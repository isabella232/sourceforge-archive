// ========================================================================
// $Id$
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus;


public abstract class AbstractService implements Service
{
    protected String _jndi;
    protected String _name;
    protected boolean _started = false;

    
    public void setJNDI (String registration)
    {
        _jndi = registration;
    }
    

    public String getJNDI ()
    {
        return _jndi;
    }
    


    public void setName (String name)
    {
        _name = name;
    }
    
    

    public String getName ()
    {
        return _name;
    }
    
    public void start()
        throws Exception
    {
        _started = true;
    }
    
    
    public void stop()
        throws InterruptedException
    {
        _started = false;
    }
    

    public boolean isStarted()
    {
        return _started;
    }
        
}
