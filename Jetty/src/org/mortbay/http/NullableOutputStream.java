// ========================================================================
// $Id$
// Copyright 2001-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.http;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------ */
/** Buffered Output Stream.
 * Uses ByteBufferOutputStream to allow pre and post writes.
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class NullableOutputStream
    extends FilterOutputStream
    implements HttpMessage.HeaderWriter
{
    private static Log log = LogFactory.getLog(NullableOutputStream.class);

    private ByteArrayISO8859Writer _httpMessageWriter;
    private boolean _nulled=false;
    private boolean _closed=false;
    private int _headerReserve;
    
    /* ------------------------------------------------------------ */
    public NullableOutputStream(OutputStream outputStream, int headerReserve)
    {
        super(outputStream);
        _headerReserve = headerReserve;
    }
    
    /* ------------------------------------------------------------ */
    /** Null the output.
     * All output written is discarded until the stream is reset. Used
     * for HEAD requests.
     */
    public void nullOutput()
        throws IOException
    {
        _nulled=true;
    }
    
    /* ------------------------------------------------------------ */
    /** is the output Nulled?
     */
    public boolean isNullOutput()
        throws IOException
    {
        return _nulled;
    }
    
    /* ------------------------------------------------------------ */
    public void writeHeader(HttpMessage httpMessage)
        throws IOException
    {
        if (_httpMessageWriter==null)
            _httpMessageWriter=new ByteArrayISO8859Writer(_headerReserve);
        httpMessage.writeHeader(_httpMessageWriter);
        _httpMessageWriter.writeTo(out);
        _httpMessageWriter.resetWriter();
    }
    
    /* ------------------------------------------------------------ */
    public void resetStream()
    {
        _closed=false;
        _nulled=false;
        if (_httpMessageWriter!=null)
            _httpMessageWriter.resetWriter();
    }
    
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        if (_httpMessageWriter!=null)
            _httpMessageWriter.destroy();
        _httpMessageWriter=null;
        try{out.close();} catch (Exception e){log.warn(LogSupport.EXCEPTION,e);}
    }
    
    /* ------------------------------------------------------------ */
    public void write(int b) throws IOException
    {
        if (!_nulled)
        {
            if (_closed)
                throw new IOException("closed");
            out.write(b);
        }
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[]) throws IOException
    {
        if (!_nulled)
        {
            if (_closed)
                throw new IOException("closed");
            out.write(b,0,b.length);
        }
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[], int off, int len)
        throws IOException
    {     
        if (!_nulled)
        {
            if (_closed)
                throw new IOException("closed");
            out.write(b,off,len);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
        if (!_nulled && !_closed)
            out.flush();
    }
    
    /* ------------------------------------------------------------ */
    /** Close the stream.
     * @exception IOException 
     */
    public void close()
        throws IOException
    {
        _closed=true;
    }
    
}
