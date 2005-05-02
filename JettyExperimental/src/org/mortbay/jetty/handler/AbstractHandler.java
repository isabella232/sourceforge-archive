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

package org.mortbay.jetty.handler;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.thread.AbstractLifeCycle;
import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;


/* ------------------------------------------------------------ */
/** AbstractHandler.
 * @author gregw
 *
 */
public abstract class AbstractHandler extends AbstractLifeCycle implements Handler
{
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public AbstractHandler()
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#start()
     */
    protected void doStart() throws Exception
    {
        log.info("start {}",this);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#stop()
     */
    protected void doStop() throws Exception
    {
        log.info("stop {}",this);
    }

}
