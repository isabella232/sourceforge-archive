package org.mortbay.jetty.plus;

import java.util.Collection;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import org.mortbay.jetty.plus.AbstractService;
import org.mortbay.jndi.Util;
import org.mortbay.util.Code;
import org.mortbay.util.Log;

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
            Code.debug ("ObjectFactory getObjectInstance() called");

            if (obj instanceof Reference)
            {
                Reference ref = (Reference)obj;
                if (ref.getClassName().equals(Session.class.getName()))
                {
                    Object inst = _sessionMap.get(ref.get("xx"));
                    Code.debug ("Returning object: "+inst+" for reference: "+ref.get("xx"));
                  
                    return inst;
                }
            }

            Code.debug ("Returning null");
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
	_sessionProperties.put("Password",pwd);
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
            Code.debug("Mail authenticator: user="+getUser());

            // create a Session object
            Session session = Session.getInstance (_sessionProperties, authenticator);
            
            Code.debug ("Created Session="+session+" with ClassLoader="+session.getClass().getClassLoader());

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
            
            Code.debug ("Bound reference to "+getJNDI());

            //look up the Session object to test
            Object o = initialCtx.lookup (getJNDI());
            Code.debug ("Looked up Session="+o+" from classloader="+o.getClass().getClassLoader());

            super.start();
            Log.event ("Mail Service started");
        }
        else
            Log.warning ("MailService is already started");
    }
    
   
    
    public void stop()
        throws InterruptedException
    {
        super.stop();
    }
    

    
} 
