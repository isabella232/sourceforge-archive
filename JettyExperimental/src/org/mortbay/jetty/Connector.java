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

package org.mortbay.jetty;

import java.io.IOException;

import org.mortbay.io.Buffers;
import org.mortbay.thread.LifeCycle;
import org.mortbay.thread.ThreadPool;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface Connector extends LifeCycle, Buffers
{ 
    public void open() throws IOException;
    public void close() throws IOException;
    
    public void setThreadPool(ThreadPool pool);
    public ThreadPool getThreadPool();
    
    public void setHandler(Handler handler);
    public Handler getHandler();
    

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the headerBufferSize.
     */
    int getHeaderBufferSize();
    
    /* ------------------------------------------------------------ */
    /**
     * @param headerBufferSize The headerBufferSize to set.
     */
    void setHeaderBufferSize(int headerBufferSize);
    
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the requestBufferSize.
     */
    int getRequestBufferSize();
    
    /* ------------------------------------------------------------ */
    /**
     * @param requestBufferSize The requestBufferSize to set.
     */
    void setRequestBufferSize(int requestBufferSize);
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the responseBufferSize.
     */
    int getResponseBufferSize();
    
    /* ------------------------------------------------------------ */
    /**
     * @param responseBufferSize The responseBufferSize to set.
     */
    void setResponseBufferSize(int responseBufferSize);
}
