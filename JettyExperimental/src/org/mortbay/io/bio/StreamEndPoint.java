//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================


package org.mortbay.io.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StreamEndPoint implements EndPoint
{
	InputStream _in;
    OutputStream _out;

    /**
     * 
     */
    public StreamEndPoint(InputStream in, OutputStream out)
    	throws IOException	
    {
        _in=in;
        _out=out;
    }
    
    public boolean isBlocking()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#isClosed()
     */
    public boolean isClosed()
    {
        return _in==null || _out==null;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#close()
     */
    public void close() throws IOException
    {
        if (_in!=null)
            _in.close();
        _in=null;
        if (_out!=null)
            _out.close();
        _out=null;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#fill(org.mortbay.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        // TODO handle null array()
        if (_in==null)
            return 0;
            
    	int space=buffer.space();
    	if (space<=0)
    	{
    	    if (buffer.hasContent())
    	        return 0;
    	    Portable.throwIO("FULL");
    	}
        
	    byte[] bytes = buffer.array();
		int n=_in.read(bytes,buffer.putIndex(),space);
		if (n>=0)
		{
		  	buffer.setPutIndex(buffer.putIndex()+n);
			return n;
    	}
    	
    	return -1;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#flush(org.mortbay.io.Buffer)
     */
    public int flush(Buffer buffer) throws IOException
    {
        // TODO handle null array()
        if (_out==null)
            return -1;
        int length=buffer.length();
        if (length>0)
            _out.write(buffer.array(),buffer.getIndex(),length);
        buffer.clear();
        return length;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#flush(org.mortbay.io.Buffer, org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        // TODO lots of efficiency stuff here to avoid double write
        // TODO handle null array()

        int total=0;
        
        // See if the header buffer will fit in front of buffer content.
        int length=header==null?0:header.length();
        if (length>0 && length<=buffer.getIndex())
        {
            int pi=buffer.putIndex();
            buffer.setGetIndex(buffer.getIndex()-length);
            buffer.setPutIndex(buffer.getIndex());
            buffer.put(header);
            buffer.setPutIndex(pi);
        }
        else if (length>0)
        {
            _out.write(header.array(),header.getIndex(),length);
            total=length;
        }
        // TODO
        header.clear();

        // See if the trailer buffer will fit in front of buffer content.
        length=trailer==null?0:trailer.length();
        if (length>0 && length<=buffer.space())
        {
            buffer.put(trailer); 
            trailer.clear();
        }
        
        length=buffer.length();
        if (length>0)
            _out.write(buffer.array(),buffer.getIndex(),length);
       
        total+=length;
        if (!buffer.isImmutable())
            buffer.clear();
        
        // write trailer if it was not stuffed in the buffer.
        length=trailer==null?0:trailer.length();
        if (length>0 && length<=buffer.space())
        {
            _out.write(trailer.array(),trailer.getIndex(),length);
            total+=length;
            trailer.clear();
        }
        
        return total;
    }

}
