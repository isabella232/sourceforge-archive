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

package org.mortbay.thread;
import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;

/**
 * @author gregw
 */
public abstract class AbstractLifeCycle implements LifeCycle
{
    private final static ULogger log = LoggerFactory.getLogger(AbstractLifeCycle.class);
    private final int FAILED=-1,STOPPED=0,STARTING=1,STARTED=2,STOPPING=3;
    private transient int _state=0;
    protected void doStart() throws Exception {};
    protected void doStop() throws Exception {};

    public final void start() throws Exception
    {
        try
        {
            if (_state==STARTED)
                return;
            _state=STARTING;
            doStart();
            log.info("started "+this);
            _state=STARTED;
        }
        catch (Exception e)
        {
            _state=FAILED;
            throw e;
        }
        catch(Error e)
        {
            _state=FAILED;
            throw e;
        }
    }
    
    public final void stop() throws Exception
    {
        try
        {
            if (_state<STARTING)
                return;
            _state=STOPPING;
            doStop();
            log.info("stopped "+this);
            _state=STOPPED;
        }
        catch (Exception e)
        {
            _state=FAILED;
            throw e;
        }
        catch(Error e)
        {
            _state=FAILED;
            throw e;
        }
    }

    public boolean isRunning()
    {
        return _state==STARTED || _state==STARTING;
    }
    
    public boolean isStarted()
    {
        return _state==STARTED;
    }

    public boolean isStarting()
    {
        return _state==STARTING;
    }

    public boolean isStopping()
    {
        return _state==STOPPING;
    }

    public boolean isFailed()
    {
        return _state==FAILED;
    }
}
