// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

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
