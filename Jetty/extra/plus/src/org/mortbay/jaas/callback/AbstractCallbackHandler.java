// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;


public abstract class AbstractCallbackHandler implements CallbackHandler
{
    protected String _userName;
    protected Object _credential;

    public void setUserName (String userName)
    {
        _userName = userName;
    }

    public String getUserName ()
    {
        return _userName;
    }
    

    public void setCredential (Object credential)
    {
        _credential = credential;
    }

    public Object getCredential ()
    {
        return _credential;
    }
    
    public  void handle (Callback[] callbacks)
        throws IOException, UnsupportedCallbackException
    {
    }
    
    
}
