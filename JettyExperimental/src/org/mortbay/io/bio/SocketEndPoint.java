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
import java.net.Socket;

import org.mortbay.io.Buffer;
import org.mortbay.io.Portable;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SocketEndPoint extends StreamEndPoint
{
    Socket _socket;

    /**
     * 
     */
    public SocketEndPoint(Socket socket)
    	throws IOException	
    {
        super(socket.getInputStream(),socket.getOutputStream());
        _socket=socket;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#isClosed()
     */
    public boolean isClosed()
    {
        return _socket==null || _socket.isClosed() || _socket.isInputShutdown() || _socket.isOutputShutdown();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.BufferIO#close()
     */
    public void close() throws IOException
    {
        _socket.close();
        _in=null;
        _out=null;
    }
}
