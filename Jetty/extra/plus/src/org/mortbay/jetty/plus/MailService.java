// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jndi.Util;

/**
 * MailService.java
 *
 *
 * Created: Fri May 30 09:25:47 2003
 *
 * @author <a href="mailto:janb@wafer">Jan Bartel</a>
 * @version 1.0
 */
public class MailService extends AbstractService implements Map
{
    static Log log = LogFactory.getLog(MailService.class);

    public static final String DEFAULT_MAIL_JNDI = "mail/Session";
    protected Properties _sessionProperties;
    protected String _user;
    protected String _password;
    protected ObjectFactory _objectFactory;
   


    public class MailAuthenticator extends Authenticator
    {
        PasswordAuthentication _passwordAuthentication;

        public MailAuthenticator(String user, String password)
        {
            _passwordAuthentication = new PasswordAuthentication (user, password);
        }

        public PasswordAuthentication getPasswordAuthentication()
        {
            return _passwordAuthentication;
        }
        
    };


   
    public static class SessionObjectFactory implements ObjectFactory 
    {
        protected static HashMap _sessionMap = new HashMap();

       

        public static void addSession (Session session, StringRefAddr ref)
        {
            _sessionMap.put (ref, session);
        }

        public Object getObjectInstance(Object obj,
                                        Name name,
                                        Context nameCtx,
                                        Hashtable environment)
            throws Exception
        {
            if(log.isDebugEnabled())log.debug("ObjectFactory getObjectInstance() called");

            if (obj instanceof Reference)
            {
                Reference ref = (Reference)obj;
                if (ref.getClassName().equals(Session.class.getName()))
                {
                    Object inst = _sessionMap.get(ref.get("xx"));
                    if(log.isDebugEnabled())log.debug("Returning object: "+inst+" for reference: "+ref.get("xx"));
                  
                    return inst;
                }
            }

            if(log.isDebugEnabled())log.debug("Returning null");
            return null;
        }
    };


    public MailService() 
    {    
        setJNDI (DEFAULT_MAIL_JNDI);
        _sessionProperties = new Properties();
    }




    public void setUser (String user)
    {
        _user = user;
        _sessionProperties.put("User", user);
    }

    public String getUser ()
    {
        return _user;
    }

    public void setPassword (String pwd)
    {
        _password = pwd;
        _sessionProperties.put("Password", pwd);
    }
    
    protected String getPassword ()
    {
        return _password;
    }

    public void clear ()
    {
        _sessionProperties.clear();
    }

    public int size()
    {
        return _sessionProperties.size();
    }

    public boolean isEmpty()
    {
        return _sessionProperties.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return _sessionProperties.containsKey(key);
    }
    
    public boolean containsValue(Object value)
    {
        return _sessionProperties.containsValue(value);
    }

    public Object get(Object key)
    {
        return _sessionProperties.get(key);
    }

    public Object put (Object key, Object value)
    {
        return _sessionProperties.put (key, value);
    }
    
    public Object remove(Object key)
    {
        return _sessionProperties.remove (key);
    }

    public void putAll(Map t)
    {
        _sessionProperties.putAll(t);
    }

    public Set keySet()
    {
        return _sessionProperties.keySet();
    }

    public Collection values()
    {
        return _sessionProperties.values();
    }

    public Set entrySet()
    {
        return _sessionProperties.entrySet();
    }

   
    public boolean equals(Object o)
    {
        return _sessionProperties.equals(o);
    }

    public int hashCode()
    {
        return _sessionProperties.hashCode();
    }


    /**
     * Create a Session and bind to JNDI
     *
     * @exception Exception 
     */
    public void start()
        throws Exception
    {
        if (!isStarted())
        {

            MailAuthenticator authenticator = new MailAuthenticator (getUser(), getPassword());            
            if(log.isDebugEnabled())log.debug("Mail authenticator: user="+getUser());

            // create a Session object
            Session session = Session.getInstance (_sessionProperties, authenticator);
            
            if(log.isDebugEnabled())log.debug("Created Session="+session+" with ClassLoader="+session.getClass().getClassLoader());

            // create an ObjectFactory for Session as Session isn't serializable            
            StringRefAddr refAddr = new StringRefAddr ("xx", getJNDI());
            SessionObjectFactory.addSession (session, refAddr);
            Reference reference = new Reference (session.getClass().getName(), 
                                                 refAddr,
                                                 SessionObjectFactory.class.getName(),
                                                 null);
            // bind to JNDI
            InitialContext initialCtx = new InitialContext();
           
            Util.bind(initialCtx, getJNDI(), reference);
            
            if(log.isDebugEnabled())log.debug("Bound reference to "+getJNDI());

            //look up the Session object to test
            Object o = initialCtx.lookup (getJNDI());
            if(log.isDebugEnabled())log.debug("Looked up Session="+o+" from classloader="+o.getClass().getClassLoader());

            super.start();
            log.info ("Mail Service started");
        }
        else
            log.warn ("MailService is already started");
    }
    
   
    
    public void stop()
        throws InterruptedException
    {
        super.stop();
    }
    

    
} 
