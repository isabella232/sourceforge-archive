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

package org.mortbay.http;

import org.mortbay.thread.AbstractLifeCycle;
import org.mortbay.thread.ThreadPool;

public class HttpServer extends AbstractLifeCycle
{
    private ThreadPool _threadPool = new ThreadPool();
    
    public HttpServer()
    	throws Exception
    {
    }
    
    protected void doStart() throws Exception
    {
        _threadPool.start();
    }
    
    protected void doStop() throws Exception
    {
        _threadPool.stop();
    }
    
    public void dispatch(Runnable job)
    	throws InterruptedException
    {
        if (isRunning())
            _threadPool.run(job,true);
    }
    
    public String toString()
    {
        return "HttpServer";
    }
}
