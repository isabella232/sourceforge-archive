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

package org.mortbay.io.nio;

import java.io.IOException;
import java.nio.channels.ByteChannel;

import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;


/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelEndPoint implements EndPoint
{
    ByteChannel _channel;
    
    /**
     * 
     */
    public ChannelEndPoint(ByteChannel chanel)
    {
        super();
        this._channel = chanel;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#isClosed()
     */
    public boolean isClosed()
    {
        return !_channel.isOpen();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#close()
     */
    public void close() throws IOException
    {
        _channel.close();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#fill(org.mortbay.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        int l= _channel.read(null);
        return 0;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer)
     */
    public int flush(Buffer buffer) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer, org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
