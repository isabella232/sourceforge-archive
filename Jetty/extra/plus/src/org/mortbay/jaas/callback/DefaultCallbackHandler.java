// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.mortbay.util.Password;



/* ---------------------------------------------------- */
/** DefaultUsernameCredentialCallbackHandler
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class DefaultCallbackHandler extends AbstractCallbackHandler
{
     
    public void handle (Callback[] callbacks)
        throws IOException, UnsupportedCallbackException
    {
        for (int i=0; i < callbacks.length; i++)
        {
            if (callbacks[i] instanceof NameCallback)
            {
                ((NameCallback)callbacks[i]).setName(getUserName());
            }
            else if (callbacks[i] instanceof ObjectCallback)
            {
                ((ObjectCallback)callbacks[i]).setObject(getCredential());
            }
            else if (callbacks[i] instanceof PasswordCallback)
            {
                if (getCredential() instanceof Password)
                    ((PasswordCallback)callbacks[i]).setPassword (((Password)getCredential()).toString().toCharArray());
                else if (getCredential() instanceof String)
                {
                    ((PasswordCallback)callbacks[i]).setPassword (((String)getCredential()).toCharArray());
                }
                else
                    throw new UnsupportedCallbackException (callbacks[i], "User supplied credentials cannot be converted to char[] for PasswordCallback: try using an ObjectCallback instead");
            }
            else
                throw new UnsupportedCallbackException(callbacks[i]);
        }
        
    }
    
}
        
