// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.http.bio;


import java.io.IOException;
import java.io.OutputStream;

import org.mortbay.http.temp.HttpHeader;
import org.mortbay.http.temp.HttpOutput;
import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;

/* ------------------------------------------------------------------------------- */
/**
 * HTTP OutputStream. Based on HttpOutput and assumes a blocking buffer implementation.
 */
public class HttpOutputStream extends OutputStream
{
    private byte[] _b1=new byte[0];
    private HttpOutput _out;

    /**
     * 
     */
    public HttpOutputStream(Buffer buffer,EndPoint endp,HttpHeader header, int headerReserve)
    {
        _out=new HttpOutput(buffer,endp,header, headerReserve);
    }

    /*
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        if(_out.isClosed())
            return;
        _out.close();
        while(_out.isFlushing())
            _out.flush();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * destroy.
     */
    public void destroy()
    {
        if(_out!=null)
            _out.destroy();
        _out=null;
        _b1=null;
    }

    /*
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException
    {
        while(!_out.flush());
    }

    public Buffer getBuffer()
    {
        return _out.getBuffer();
    }

    public HttpHeader getHttpHeader()
    {
        return _out.getHttpHeader();
    }

    /**
     * @return True if header has been committed.
     */
    public boolean isCommitted()
    {
        return _out.isCommitted();
    }

    public boolean isPersistent()
    {
        return _out.isPersitent();
    }

    public void resetBuffer()
    {
        _out.resetBuffer();
    }

    public void resetStream()
    {
        _out.reset();
    }

    public void sendContinue()
    throws IOException
    {
        _out.sendContinue();
    }

    public void setHeadResponse(boolean h)
    {
        _out.setHeadResponse(h);
    }

    /**
     * Set the output version. For responses, the version may differ from that indicated by the
     * header.
     * 
     * @param ordinal HttpVersions ordinal
     */
    public void setVersionOrdinal(int ordinal)
    {
        _out.setVersionOrdinal(ordinal);
    }

    /*
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b,int offset,int length) throws IOException
    {
        if(_out.isFlushing())
            while(!_out.flush());
        while(length>0)
        {
            int len=_out.write(b,offset,length);
            if(len<length)
            {
                length-=len;
                offset+=len;
                while(!_out.flush());
            }
            else
            {
                length-=len;
                offset+=len;
            }
        }
    }

    /*
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        _b1[0]=(byte)b;
        write(_b1,0,1);
    }
    
}
