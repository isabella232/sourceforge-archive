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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.XMLConfiguration;
import org.mortbay.jndi.Util;
import org.mortbay.util.LogSupport;
import org.mortbay.util.TypeUtil;
import org.mortbay.xml.XmlParser;

/* ------------------------------------------------------------ */
public class PlusWebAppContext extends WebApplicationContext
{
    private static Log log = LogFactory.getLog(PlusWebAppContext.class);
    private InitialContext _initialCtx = null;
    private HashMap _envMap = null;
    private ClassLoader _removeClassLoader=null;
    private boolean _webXmlEnvEntryOverride = true;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception IOException 
     */
    public PlusWebAppContext()
    {
       super();
       setConfiguration(new Configuration(this));
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param webApp The Web application directory or WAR file.
     * @exception IOException 
     */
    public PlusWebAppContext(
       String webApp
    )
    {
        super(webApp);
        setConfiguration(new Configuration(this));
    }

    public void setWebXmlEnvEntryOverride (boolean value)
    {
        _webXmlEnvEntryOverride = value;
    }
    
    public boolean getWebXmlEnvEntryOverride()
    {
        return _webXmlEnvEntryOverride;
    }

    /* ------------------------------------------------------------ */
    /** Add a java:comp/env entry.
     *  Values must be serializable to be stored!
     */
    public void addEnvEntry (String name, Object value)
    {
        if (_envMap == null)
             _envMap = new HashMap();

        if (name == null)
            log.warn ("Name for java:comp/env is null. Ignoring.");
        if (value == null)
            log.warn ("Value for java:comp/env is null. Ignoring.");

        _envMap.put (name, value);
    }

    public Object getEnvEntry (String name)
    {
        if (_envMap == null)
            return null;
        
        return _envMap.get(name);
    }
    
    
    
    /* ------------------------------------------------------------ */
    /**
     * Get a mapping of java:comp/env bindings for this webapp context.
     * @return flattened map of java:comp/env names to bound objects
     * @throws NamingException
     */
    public Map getENC()
    throws NamingException
    {
        // save context classloader
        Thread thread= Thread.currentThread();
        ClassLoader lastContextLoader= thread.getContextClassLoader();
        
        //set the classloader up as this webapp's loader
        thread.setContextClassLoader(getClassLoader());
        Map map = null;
        try
        {
            map = Util.flattenBindings ((Context)_initialCtx.lookup("java:comp"), "env");
        }
        finally
        {
            //replace the classloader
            thread.setContextClassLoader(lastContextLoader);
        }
        
        return map;
    }
    
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        _initialCtx = new InitialContext();
        super.start();
    }
        
    
    /* ------------------------------------------------------------ */
    public void handle(HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        super.handle(request,response);
    }    


    /* ------------------------------------------------------------ */    
    protected void initialize ()
        throws Exception
    { 
        //create ENC for this webapp 
        Context compCtx =  (Context)_initialCtx.lookup ("java:comp");
        Context envCtx = compCtx.createSubcontext("env");
        if(LogSupport.isTraceEnabled(log))log.trace(envCtx);

        //bind UserTransaction
        compCtx.rebind ("UserTransaction", new LinkRef ("javax.transaction.UserTransaction"));
        if(log.isDebugEnabled())log.debug("Bound ref to javax.transaction.UserTransaction to java:comp/UserTransaction");   

        //set up any env entries defined in config file
        if (_envMap != null)
        {
            Iterator it = _envMap.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry)it.next();
                Util.bind (envCtx, (String)entry.getKey(), entry.getValue());
                if (log.isDebugEnabled())log.debug("Bound java:comp/env/"+entry.getKey()+" to "+entry.getValue());	
            }
        }
    }



   


    /* ------------------------------------------------------------ */
    protected void initClassLoader(boolean forceContextLoader)
        throws MalformedURLException, IOException
    {
        // detect if own classloader is created.  If so, make sure it is
        // removed from log4j CRS
        ClassLoader cl=getClassLoader();
        super.initClassLoader(forceContextLoader);
        if (cl==null || getClassLoader()!=cl)
            _removeClassLoader=getClassLoader();
    }

    
    /* ------------------------------------------------------------ */
    /* Removes context classloader from log4j repository
     */
    public void stop()
        throws InterruptedException
    {
        try { super.stop(); }
        finally
        {
            org.mortbay.log4j.CRS.remove(_removeClassLoader);
            _removeClassLoader=null;
        }
    }
    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class Configuration extends XMLConfiguration
    {
        public Configuration(WebApplicationContext context)
        {
            super(context);
        }

        public PlusWebAppContext getPlusWebAppContext()
        {
            return (PlusWebAppContext)getWebApplicationContext();
        }
        

        /* ------------------------------------------------------------ */
        protected void initWebXmlElement(String element, XmlParser.Node node)
            throws Exception
        {           
            // this is ugly - should be dispatched through a hash-table or introspection...
            Context envCtx = (Context)getPlusWebAppContext()._initialCtx.lookup("java:comp/env");

            if ("env-entry".equals(element))
            {
                String name=node.getString("env-entry-name",false,true);
                Object value= TypeUtil.valueOf(node.getString("env-entry-type",false,true),
                                               node.getString("env-entry-value",false,true));
                
                try
                {
                    Object o = envCtx.lookup (name);
                    //an object must already exist, check if we should override with web xml env-entry
                    if (((PlusWebAppContext)getWebApplicationContext()).getWebXmlEnvEntryOverride())
                    {
                        Util.bind (envCtx, name, value);
                    }                       
                }
                catch (NameNotFoundException e)
                {
                    //bind it anyway
                    Util.bind (envCtx, name, value);
                }

            }
            else if ("resource-ref".equals(element))
            {
                //resource-ref entries are ONLY for connection factories
                //the resource-ref says how the app will reference the jndi lookup relative 
                //to java:comp/env, but it is up to the deployer to map this reference to
                //a real resource in the environment. At the moment, we insist that the 
                //jetty.xml file name of the resource has to be exactly the same as the 
                //name in web.xml deployment descriptor, but it shouldn't have to be
                
                // Lookup the name in the global environment, if found
                // bind it to the local context
                String name=node.getString("res-ref-name",false,true);
                
                if(log.isDebugEnabled())log.debug("Linking resource-ref java:comp/env/"+name+" to global "+name);
                
                Object o = getPlusWebAppContext()._initialCtx.lookup (name);
                
                if(log.isDebugEnabled())log.debug("Found Object in global namespace: "+o.toString());
                Util.bind (envCtx, name,  new LinkRef(name));
            }
            else if ("resource-env-ref".equals(element))
            {
                //resource-env-ref elements are a non-connection factory type of resource
                //the app looks them up relative to java:comp/env
                //again, need a way for deployer to link up app naming to real naming.
                //Again, we insist now that the name of the resource in jetty.xml is
                //the same as web.xml
                
                // Lookup the name in the global environment, if found
                // bind it to the local context
                String name=node.getString("resource-env-ref-name",false,true);
                
                if(log.isDebugEnabled())log.debug("Linking resource-env-ref java:comp/env/"+name +" to global "+name);
                Util.bind (envCtx, name, new LinkRef(name));
            }
            else if ("ejb-ref".equals(element) ||
                     "ejb-local-ref".equals(element) ||
                     "security-domain".equals(element))
            {
                log.warn("Entry " + element+" => "+node+" is not supported yet");
            }
            else
            {
                super.initWebXmlElement(element, node);
            }
        }

    }
}
