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

package org.mortbay.util.ajax;

public class WaitingContinuation implements org.mortbay.util.ajax.Continuation
{
    Object _object;
    Object _event;
    boolean _waited;
    boolean _new=true;
    boolean _pending=true;
    
    public void resume(Object object)
    {
        synchronized (this)
        {
            _event=object==null?this:object;
            _pending=false;
            notify();
        }
    }

    public boolean isNew()
    {
        return _new;
    }

    public Object getEvent(long timeout)
    {
        if (timeout < 0)
            throw new IllegalArgumentException();
        
        synchronized (this)
        {
            _new=false;
            try
            {
                if (!_waited && _event==null && timeout>0)
                {
                    _waited=true;
                    wait(timeout);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            _pending=false;
        }
        return _event;
    }
    
    public boolean isPending()
    {
        return _pending;
    }

    public Object getObject()
    {
        return _object;
    }

    public void setObject(Object object)
    {
        _object = object;
    }

}