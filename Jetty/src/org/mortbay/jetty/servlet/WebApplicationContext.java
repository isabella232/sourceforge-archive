// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PermissionCollection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.UnavailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.ClientCertAuthenticator;
import org.mortbay.http.DigestAuthenticator;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.UserRealm;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.util.JarResource;
import org.mortbay.util.LazyList;
import org.mortbay.util.LogSupport;
import org.mortbay.util.MultiException;
import org.mortbay.util.Resource;
import org.mortbay.xml.XmlConfiguration;
import org.mortbay.xml.XmlParser;


/* ------------------------------------------------------------ */
/** Standard web.xml configured HttpContext.
 *
 * This specialization of HttpContext uses the standardized web.xml
 * to describe a web application and configure the handlers for the
 * HttpContext.
 *
 * If a file named web-jetty.xml or jetty-web.xml is found in the
 * WEB-INF directory it is applied to the context using the
 * XmlConfiguration format.
 *
 * A single WebApplicationHandler instance is used to provide
 * security, filter, sevlet and resource handling.
 *
 * @see org.mortbay.jetty.servlet.WebApplicationHandler
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContext
    extends ServletHttpContext
    implements Externalizable
{
    private static Log log = LogFactory.getLog(WebApplicationContext.class);

    /* ------------------------------------------------------------ */
    private String _deploymentDescriptor;
    private String _defaultsDescriptor="org/mortbay/jetty/servlet/webdefault.xml";
    private String _war;
    private boolean _extract;
    private boolean _ignorewebjetty;

    private transient String _name;
    private transient FormAuthenticator _formAuthenticator;
    private transient Map _resourceAliases;
    private transient Resource _webApp;
    private transient Resource _webInf;
    private transient Set _warnings;
    private transient WebApplicationHandler _webAppHandler;
    private transient Map _tagLibMap;
    private transient Object _contextListeners;
    private transient Object _contextAttributeListeners;
    private transient Map _errorPages;

    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public WebApplicationContext()
    {}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param webApp The Web application directory or WAR file.
     */
    public WebApplicationContext(String webApp)
    {
        _war=webApp;
    }
    
    /* ------------------------------------------------------------ */
    public void writeExternal(java.io.ObjectOutput out)
        throws java.io.IOException
    {
        out.writeObject(getContextPath());
        out.writeObject(getVirtualHosts());
        HttpHandler[] handlers = getHandlers();
        for (int i=0;i<handlers.length;i++)
        {
            if (handlers[i] instanceof WebApplicationHandler)
                break;
            out.writeObject(handlers[i]);
        }
        out.writeObject(getAttributes());
        out.writeBoolean(isRedirectNullPath());
        out.writeInt(getMaxCachedFileSize());
        out.writeInt(getMaxCacheSize());
        out.writeBoolean(getStatsOn());
        out.writeObject(getPermissions());
        out.writeBoolean(isClassLoaderJava2Compliant());
        
        out.writeObject(_deploymentDescriptor);
        out.writeObject(_defaultsDescriptor);
        out.writeObject(_war);
        out.writeBoolean(_extract);
        out.writeBoolean(_ignorewebjetty);
    }
    
    /* ------------------------------------------------------------ */
    public void readExternal(java.io.ObjectInput in)
        throws java.io.IOException, ClassNotFoundException
    {
        setContextPath((String)in.readObject());
        setVirtualHosts((String[])in.readObject());
        Object o = in.readObject();
        
        while(o instanceof HttpHandler)
        {
            addHandler((HttpHandler)o);
            o = in.readObject();
        }
        setAttributes((Map)o);
        setRedirectNullPath(in.readBoolean());
        setMaxCachedFileSize(in.readInt());
        setMaxCacheSize(in.readInt());
        setStatsOn(in.readBoolean());
        setPermissions((PermissionCollection)in.readObject());
        setClassLoaderJava2Compliant(in.readBoolean());
        
        _deploymentDescriptor=(String)in.readObject();
        _defaultsDescriptor=(String)in.readObject();
        _war=(String)in.readObject();
        _extract=in.readBoolean();
        _ignorewebjetty=in.readBoolean();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param war Filename or URL of the web application directory or WAR file. 
     */
    public void setWAR(String war)
    {
        _war=war;
    }
    
    /* ------------------------------------------------------------ */
    public String getWAR()
    {
        return _war;
    }
    
    /* ------------------------------------------------------------ */
    public WebApplicationHandler getWebApplicationHandler()
    {
        if (_webAppHandler==null)
            getServletHandler();
        return _webAppHandler;
    }
    
    /* ------------------------------------------------------------ */
    private void resolveWebApp()
        throws IOException
    {
        if (_webApp==null && _war!=null && _war.length()>0)
        {
            // Set dir or WAR
            _webApp = Resource.newResource(_war);


            // Accept aliases for WAR files
            if (_webApp.getAlias()!=null)
            {
                log.info(_webApp+" anti-aliased to "+_webApp.getAlias());
                _webApp= Resource.newResource(_webApp.getAlias());
            }

            if(log.isDebugEnabled())log.debug("Try webapp="+_webApp+
                                              ", exists="+_webApp.exists()+
                                              ", directory="+_webApp.isDirectory());
            
            // Is the WAR usable directly?
            if (_webApp.exists() &&
                !_webApp.isDirectory() &&
                !_webApp.toString().startsWith("jar:"))
            {
                // No - then lets see if it can be turned into a jar URL.
                Resource jarWebApp = Resource.newResource("jar:"+_webApp+"!/");
                if (jarWebApp.exists() && jarWebApp.isDirectory())
                {
                    _webApp=jarWebApp;
                    _war=_webApp.toString();
                    if(log.isDebugEnabled())log.debug("Try webapp="+_webApp+
                                                      ", exists="+_webApp.exists()+
                                                      ", directory="+_webApp.isDirectory());
                }
            }
            
            // If we should extract or the URL is still not usable
            if (_webApp.exists() &&
                (!_webApp.isDirectory() || 
                 ( _extract && _webApp.getFile()==null) ||
                 ( _extract && _webApp.getFile()!=null && !_webApp.getFile().isDirectory())))
            {
                // Then extract it.
                File tempDir=new File(getTempDirectory(),"webapp");
                if (tempDir.exists())
                    tempDir.delete();
                tempDir.mkdir();
                tempDir.deleteOnExit();
                log.info("Extract "+_war+" to "+tempDir);
                JarResource.extract(_webApp,tempDir,true);
                _webApp=Resource.newResource(tempDir.getCanonicalPath());
                
                if(log.isDebugEnabled())log.debug("Try webapp="+_webApp+
                                                  ", exists="+_webApp.exists()+
                                                  ", directory="+_webApp.isDirectory());
            }
            
            // Now do we have something usable?
            if (!_webApp.exists() || !_webApp.isDirectory())
            {
                log.warn("Web application not found "+_war);
                throw new java.io.FileNotFoundException(_war);
            }

            if(log.isDebugEnabled())log.debug("webapp="+_webApp);

            // Iw there a WEB-INF directory?
            _webInf = _webApp.addPath("WEB-INF/");
            if (!_webInf.exists() || !_webInf.isDirectory())
                _webInf=null;
            
            // ResourcePath
            super.setBaseResource(_webApp);
        }
    }

    /* ------------------------------------------------------------ */
    /** Get the context ServletHandler.
     * Conveniance method. If no ServletHandler exists, a new one is added to
     * the context.  This derivation of the method creates a
     * WebApplicationHandler extension of ServletHandler.
     * @return WebApplicationHandler
     */
    public synchronized ServletHandler getServletHandler()
    {
        if (_webAppHandler==null)
        {
            _webAppHandler=(WebApplicationHandler)getHandler(WebApplicationHandler.class);
            if (_webAppHandler==null)
            {
                if (getHandler(ServletHandler.class)!=null)
                    throw new IllegalStateException("Cannot have ServletHandler in WebApplicationContext");
                _webAppHandler=new WebApplicationHandler();
                addHandler(_webAppHandler);
            }
        }
        return _webAppHandler;
    }
    
    /* ------------------------------------------------------------ */
    public void setPermissions(PermissionCollection permissions)
    {
        if (!_ignorewebjetty)
            log.warn("Permissions set with web-jetty.xml enabled");
        super.setPermissions(permissions);
    }
    
    /* ------------------------------------------------------------ */
    public boolean isIgnoreWebJetty()
    {
        return _ignorewebjetty;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param b If TRUE, web-jetty.xml and jetty-web.xml configuration
     * files are ignored. 
     */
    public void setIgnoreWebJetty(boolean b)
    {
        _ignorewebjetty=b;
        if (b && getPermissions()!=null)
            log.warn("Permissions set with web-jetty.xml enabled");
    }
    
    /* ------------------------------------------------------------ */
    /** Start the Web Application.
     * @exception IOException 
     */
    public void start()
        throws Exception
    {
        if (isStarted())
            return;
        
        setWelcomeFiles(null);
        _tagLibMap=new HashMap(3);
        
        // save context classloader
        Thread thread = Thread.currentThread();
        ClassLoader lastContextLoader=thread.getContextClassLoader();

        MultiException mex=null;
        try
        {            
            // Get parser
            XmlParser xmlParser=new XmlParser();
            
            //set up cache of DTDs and schemas locally
            URL dtd22=WebApplicationContext.class
                .getResource("/javax/servlet/resources/web-app_2_2.dtd");
            URL dtd23=WebApplicationContext.class
                .getResource("/javax/servlet/resources/web-app_2_3.dtd");
            URL jsp20xsd = WebApplicationContext.class
                .getResource("/javax/servlet/resources/jsp_2_0.xsd");
            URL j2ee14xsd = WebApplicationContext.class
                .getResource("/javax/servlet/resources/j2ee_1_4.xsd");
            URL webapp24xsd = WebApplicationContext.class
                .getResource ("/javax/servlet/resources/web-app_2_4.xsd");
            URL schemadtd = WebApplicationContext.class
                .getResource ("/javax/servlet/resources/XMLSchema.dtd");
            URL xmlxsd = WebApplicationContext.class
                .getResource ("/javax/servlet/resources/xml.xsd");
            URL webservice11xsd = WebApplicationContext.class
                .getResource ("/javax/servlet/resources/j2ee_web_services_client_1_1.xsd");
            URL datatypesdtd = WebApplicationContext.class
                .getResource ("/javax/servlet/resources/datatypes.dtd");

            xmlParser.redirectEntity("web-app_2_2.dtd",dtd22);
            xmlParser.redirectEntity("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN",dtd22);
            
            xmlParser.redirectEntity("web.dtd",dtd23);
            xmlParser.redirectEntity("web-app_2_3.dtd",dtd23);
            xmlParser.redirectEntity("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN",dtd23);
            
            xmlParser.redirectEntity("XMLSchema.dtd", schemadtd);
            xmlParser.redirectEntity("http://www.w3.org/2001/XMLSchema.dtd", schemadtd);
            xmlParser.redirectEntity("-//W3C//DTD XMLSCHEMA 200102//EN", schemadtd);
            
            xmlParser.redirectEntity("jsp_2_0.xsd", jsp20xsd);
            xmlParser.redirectEntity("http://java.sun.com/xml/ns/j2ee/jsp_2_0.xsd", jsp20xsd);
            xmlParser.redirectEntity("j2ee_1_4.xsd", j2ee14xsd);
            xmlParser.redirectEntity("http://java.sun.com/xml/ns/j2ee/j2ee_1_4.xsd", j2ee14xsd);
            xmlParser.redirectEntity("web-app_2_4.xsd",webapp24xsd);
            xmlParser.redirectEntity("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", webapp24xsd);
            xmlParser.redirectEntity("xml.xsd", xmlxsd);
            xmlParser.redirectEntity("http://www.w3.org/2001/xml.xsd", xmlxsd);
            xmlParser.redirectEntity("datatypes.dtd", datatypesdtd);
            xmlParser.redirectEntity("http://www.w3.org/2001/datatypes.dtd", datatypesdtd);
            xmlParser.redirectEntity("j2ee_web_services_client_1_1.xsd", webservice11xsd);
            xmlParser.redirectEntity("http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd", webservice11xsd);

            // Find the webapp
            resolveWebApp();
            
            // Get the handler
            getServletHandler();
            
            // Add WEB-INF classes and lib classpaths
            if (_webInf!=null && _webInf.isDirectory())
            {
                // Look for classes directory
                Resource classes = _webInf.addPath("classes/");
                if (classes.exists())
                    super.setClassPath(classes.toString());
                else
                    super.setClassPath(null);
                    
                // Look for jars
                Resource lib = _webInf.addPath("lib/");
                super.setClassPaths(lib,true);
            }
          
            // initialize the classloader
            initClassLoader(true);
            thread.setContextClassLoader(getClassLoader());
            initialize();

            // Do the default configuration
            if (_defaultsDescriptor!=null && _defaultsDescriptor.length()>0)
            {
                Resource dftResource= Resource.newSystemResource(_defaultsDescriptor);
                if (dftResource==null)
                    dftResource= Resource.newResource(_defaultsDescriptor);
                
                XmlParser.Node defaultConfig =
                    xmlParser.parse(dftResource.getURL().toString());
                initialize(defaultConfig);
            }

            // handle any WEB-INF descriptors
            if (_webInf!=null && _webInf.isDirectory())
            {
                // do web.xml file
                Resource web = _webInf.addPath("web.xml");
                if (!web.exists())
                {
                    log.info("No WEB-INF/web.xml in "+_war+". Serving files and default/dynamic servlets only");
                }
                else
                {
                    XmlParser.Node config=null;
                    _deploymentDescriptor=web.toString();
                    config = xmlParser.parse(web.getURL().toString());
                    initialize(config);
                }
                
                // do jetty.xml file
                Resource jetty = _webInf.addPath("web-jetty.xml");
                if (!jetty.exists())
                    jetty = _webInf.addPath("jetty-web.xml");
                if (!_ignorewebjetty && jetty.exists())
                {
                    if(log.isDebugEnabled())log.debug("Configure: "+jetty);
                    XmlConfiguration jetty_config=new
                        XmlConfiguration(jetty.getURL());
                    jetty_config.configure(this);
                }
            }
            
            // Set classpath for Jasper.
            Map.Entry entry = _webAppHandler.getHolderEntry("test.jsp");
            if (entry!=null)
            {
                ServletHolder jspHolder = (ServletHolder)entry.getValue();
                if (jspHolder!=null && jspHolder.getInitParameter("classpath")==null)
                {
                    String fileClassPath=getFileClassPath();
                    jspHolder.setInitParameter("classpath",fileClassPath);
                    if(log.isDebugEnabled())log.debug("Set classpath="+fileClassPath+" for "+jspHolder);
                }
            }
            
            // If we have servlets, don't init them yet
            _webAppHandler.setAutoInitializeServlets(false);
            
            // Start handlers
            super.start();

            mex = new MultiException();
            // If it actually started
            if (super.isStarted())
            {            
                // Context listeners
                if (_contextListeners!=null && _webAppHandler!=null)
                {
                    ServletContextEvent event = new ServletContextEvent(getServletContext());
                    for (int i=0;i<LazyList.size(_contextListeners);i++)
                        try{((ServletContextListener)LazyList.get(_contextListeners,i))
                                .contextInitialized(event);}
                        catch(Exception ex) { mex.add(ex); }
                }
            }
            
            // OK to Initialize servlets now
            if (_webAppHandler!=null && _webAppHandler.isStarted())
            {
                try{
                    _webAppHandler.initializeServlets();
                }
                catch(Exception ex) { mex.add(ex); }
            }
        }	
        catch(Exception e)
        {
            log.warn("Configuration error on "+_war,e);
            throw e;
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
        }
        
        if (mex!=null)
            mex.ifExceptionThrow();
    }

    
    /* ------------------------------------------------------------ */
    /** Stop the web application.
     * Handlers for resource, servlet, filter and security are removed
     * as they are recreated and configured by any subsequent call to start().
     * @exception InterruptedException 
     */
    public void stop()
        throws  InterruptedException
    {
        // Context listeners
        if (_contextListeners!=null)
        {
            if (_webAppHandler!=null)
            {
                ServletContextEvent event = new ServletContextEvent(getServletContext());
                for (int i=LazyList.size(_contextListeners);i-->0;)
                    ((ServletContextListener)LazyList.get(_contextListeners,i))
                        .contextDestroyed(event);
            }
        }

        _contextListeners=null;
        _contextAttributeListeners=null;
        
        // Stop the context
        super.stop();

        // clean up
        if (_webAppHandler!=null)
            removeHandler(_webAppHandler);
        _webAppHandler=null;

        if (_errorPages!=null)
            _errorPages.clear();
        _errorPages=null;

    }
    
    /* ------------------------------------------------------------ */
    public boolean handle(String pathInContext,
                          String pathParams,
                          HttpRequest httpRequest,
                          HttpResponse httpResponse)
        throws HttpException, IOException
    {
        if (!isStarted())
            return false;
        try
        {
            super.handle(pathInContext,pathParams,httpRequest,httpResponse);
        }
        finally
        {
            if (!httpRequest.isHandled())
                httpResponse.sendError(HttpResponse.__404_Not_Found);            
            httpRequest.setHandled(true);
            if (!httpResponse.isCommitted())
                httpResponse.commit();
        }
        return true;
    }
    
    
    /* ------------------------------------------------------------ */
    public synchronized void addEventListener(EventListener listener)
        throws IllegalArgumentException
    {
        boolean known=false;
        if (listener instanceof ServletContextListener)
        {
            known=true;
            _contextListeners=LazyList.add(_contextListeners,listener);
        }
        
        if (listener instanceof ServletContextAttributeListener)
        {
            known=true;
            _contextAttributeListeners=LazyList.add(_contextAttributeListeners,listener);
        }
        

        if (!known)
            throw new IllegalArgumentException("Unknown "+listener);
    }

    /* ------------------------------------------------------------ */
    public synchronized void removeEventListener(EventListener listener)
    {
        _contextListeners=LazyList.remove(_contextListeners,listener);
        _contextAttributeListeners=LazyList.remove(_contextAttributeListeners,listener);
    }

    /* ------------------------------------------------------------ */
    public synchronized void setAttribute(String name, Object value)
    {
        Object old = super.getAttribute(name);
        super.setAttribute(name,value);

        if (_contextAttributeListeners!=null && _webAppHandler!=null)
        {
            ServletContextAttributeEvent event =
                new ServletContextAttributeEvent(getServletContext(),
                                                 name,
                                                 old!=null?old:value);
            for (int i=0;i<LazyList.size(_contextAttributeListeners);i++)
            {
                ServletContextAttributeListener l =
                    (ServletContextAttributeListener)
                    LazyList.get(_contextAttributeListeners,i);
                if (old==null)
                    l.attributeAdded(event);
                else if (value==null)
                    l.attributeRemoved(event);
                else
                    l.attributeReplaced(event);    
            }
        }
    }
    

    /* ------------------------------------------------------------ */
    public synchronized void removeAttribute(String name)
    {
        Object old = super.getAttribute(name);
        super.removeAttribute(name);
        
        if (old !=null &&
            _contextAttributeListeners!=null &&
            _webAppHandler!=null)
        {
            ServletContextAttributeEvent event =
                new ServletContextAttributeEvent(getServletContext(),
                                                 name,old);
            for (int i=0;i<LazyList.size(_contextAttributeListeners);i++)
            {
                ServletContextAttributeListener l =
                    (ServletContextAttributeListener)
                    LazyList.get(_contextAttributeListeners,i);
                l.attributeRemoved(event);    
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public String getDisplayName()
    {
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    public String getDeploymentDescriptor()
    {
        return _deploymentDescriptor;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Set the defaults web.xml file.
     * The default web.xml is used to configure all webapplications
     * before the WEB-INF/web.xml file is applied.  By default the
     * org/mortbay/jetty/servlet/webdefault.xml resource from the
     * org.mortbay.jetty.jar is used.
     * @param defaults File, Resource, URL or null.
     */
    public void setDefaultsDescriptor(String defaults)
    {
        _defaultsDescriptor=defaults;
    }
    
    /* ------------------------------------------------------------ */
    public String getDefaultsDescriptor()
    {
        return _defaultsDescriptor;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param extract If true, a WAR is extracted to a temporary
     * directory before being deployed. 
     */
    public void setExtractWAR(boolean extract)
    {
        _extract=extract;
    }
    
    /* ------------------------------------------------------------ */
    public boolean getExtractWAR()
    {
        return _extract;
    }

    /* ------------------------------------------------------------ */
    /**
     * Initialize is called by the start method after the contexts classloader
     * has been initialied, but before the defaults descriptor has been applied.
     * The default implementation does nothing.
     *
     * @exception Exception if an error occurs
     */
    protected void initialize()
        throws Exception
    {
    }

    /* ------------------------------------------------------------ */
    protected void initialize(XmlParser.Node config)
        throws ClassNotFoundException,UnavailableException
    {
        Iterator iter=config.iterator();
        XmlParser.Node node=null;
        while (iter.hasNext())
        {
            try
            {
                Object o = iter.next();
                if (!(o instanceof XmlParser.Node))
                    continue;
                
                node=(XmlParser.Node)o;
                String name=node.getTag();

                initWebXmlElement(name,node);
            }
            catch(ClassNotFoundException e)
            {
                throw e;
            }
            catch(Exception e)
            {
                log.warn("Configuration problem at "+node,e);
                throw new UnavailableException("Configuration problem");
            }
        }
        
    }

    /* ------------------------------------------------------------ */
    /** Handle web.xml element.
     * This method is called for each top level element within the
     * web.xml file.  It may be specialized by derived
     * WebApplicationContexts to provide additional configuration and handling.
     * @param element The element name
     * @param node The node containing the element.
     */
    protected void initWebXmlElement(String element, XmlParser.Node node)
        throws Exception
    {
        if ("display-name".equals(element))
            initDisplayName(node);
        else if ("description".equals(element))
        {}
        else if ("context-param".equals(element))
            initContextParam(node);
        else if ("servlet".equals(element))
            initServlet(node);
        else if ("servlet-mapping".equals(element))
            initServletMapping(node);
        else if ("session-config".equals(element))
            initSessionConfig(node);
        else if ("mime-mapping".equals(element))
            initMimeConfig(node);
        else if ("welcome-file-list".equals(element))
            initWelcomeFileList(node);
        else if ("locale-encoding-mapping-list".equals(element))
            initLocaleEncodingList(node);
        else if ("error-page".equals(element))
            initErrorPage(node);
        else if ("taglib".equals(element))
            initTagLib(node);
        else if ("resource-ref".equals(element))
        {
            if(log.isDebugEnabled())log.debug("No implementation: "+node);
        }
        else if ("security-constraint".equals(element))
            initSecurityConstraint(node);
        else if ("login-config".equals(element))
            initLoginConfig(node);
        else if ("security-role".equals(element))
            initSecurityRole(node);
        else if ("filter".equals(element))
            initFilter(node);
        else if ("filter-mapping".equals(element))
            initFilterMapping(node);
        else if ("listener".equals(element))
            initListener(node);
        else
        {                
            if (_warnings==null)
                _warnings=new HashSet(3);
            
            if (_warnings.contains(element))
            {
                if(log.isDebugEnabled())log.debug("Not Implemented: "+node);
            }
            else
            {
                _warnings.add(element);
                if(log.isDebugEnabled())
                {
                    log.debug("Element "+element+" not handled in "+this);
                    log.debug(node);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initDisplayName(XmlParser.Node node)
    {
        _name=node.toString(false,true);
    }
    
    /* ------------------------------------------------------------ */
    protected void initContextParam(XmlParser.Node node)
    {
        String name=node.getString("param-name",false,true);
        String value=node.getString("param-value",false,true);
        if(log.isDebugEnabled())log.debug("ContextParam: "+name+"="+value);

        setInitParameter(name,value); 
    }

    /* ------------------------------------------------------------ */
    protected void initFilter(XmlParser.Node node)
        throws ClassNotFoundException, UnavailableException
    {
        String name=node.getString("filter-name",false,true);
        String className=node.getString("filter-class",false,true);
        
        if (className==null)
        {
            log.warn("Missing filter-class in "+node);
            return;
        }
        if (name==null)
            name=className;
        
        FilterHolder holder = _webAppHandler.defineFilter(name,className);
        holder.addAppliesTo("REQUEST");
        Iterator iter= node.iterator("init-param");
        while(iter.hasNext())
        {
            XmlParser.Node paramNode=(XmlParser.Node)iter.next();
            String pname=paramNode.getString("param-name",false,true);
            String pvalue=paramNode.getString("param-value",false,true);
            holder.put(pname,pvalue);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initFilterMapping(XmlParser.Node node)
    {
        String filterName=node.getString("filter-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);
        String servletName=node.getString("servlet-name",false,true);


        FilterHolder holder = (servletName!=null)
            ?_webAppHandler.mapServletToFilter(servletName,filterName)
            :_webAppHandler.mapPathToFilter(pathSpec,filterName);
        
        Iterator iter= node.iterator("dispatcher");
        while(iter.hasNext())
        {
            String dispatcher=((XmlParser.Node)iter.next())
                .toString(false,true);
            holder.addAppliesTo(dispatcher);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initServlet(XmlParser.Node node)
        throws ClassNotFoundException,
               UnavailableException,
               IOException,
               MalformedURLException
    {
        String name=node.getString("servlet-name",false,true);
        String className=node.getString("servlet-class",false,true);
        String jspFile=null;
        
        if (className==null)
        {
            // There is no class, so look for a jsp file
            jspFile=node.getString("jsp-file",false,true);
            if (jspFile!=null)
            {
                Map.Entry entry = _webAppHandler.getHolderEntry(jspFile);
                if (entry!=null)
                    className=((ServletHolder)entry.getValue()).getClassName();
            }

            if (className==null)
            {
                log.warn("Missing servlet-class|jsp-file in "+node);
                return;
            }
        }
        if (name==null)
            name=className;
        
        ServletHolder holder = _webAppHandler.newServletHolder(name,className,jspFile);

        // handle JSP classpath
        if (jspFile!=null)
        {
            initClassLoader(true);
            holder.setInitParameter("classpath",getFileClassPath());
        }
        
        Iterator iParamsIter= node.iterator("init-param");
        while(iParamsIter.hasNext())
        {
            XmlParser.Node paramNode=(XmlParser.Node)iParamsIter.next();
            String pname=paramNode.getString("param-name",false,true);
            String pvalue=paramNode.getString("param-value",false,true);
            holder.put(pname,pvalue);
        }

        XmlParser.Node startup = node.get("load-on-startup");
        if (startup!=null)
        {
            String s=startup.toString(false,true).toLowerCase();
            if (s.startsWith("t"))
            {
                log.warn("Deprecated boolean load-on-startup.  Please use integer");
                holder.setInitOrder(1);
            }
            else
            {
                int order=0;
                try
                {
                    if (s!=null && s.trim().length()>0)
                        order=Integer.parseInt(s);
                }
                catch(Exception e)
                {
                    log.warn("Cannot parse load-on-startup "+s+". Please use integer");
                    LogSupport.ignore(log,e);
                }
                holder.setInitOrder(order);
            }
        }

        Iterator sRefsIter= node.iterator("security-role-ref");
        while(sRefsIter.hasNext())
        {
            XmlParser.Node securityRef=(XmlParser.Node)sRefsIter.next();
            String roleName=securityRef.getString("role-name",false,true);
            String roleLink=securityRef.getString("role-link",false,true);
            if (roleName!=null && roleName.length()>0
                    && roleLink!=null && roleLink.length()>0)
            {
                if(log.isDebugEnabled())log.debug("link role "+roleName+" to "+roleLink+" for "+this);
                holder.setUserRoleLink(roleName,roleLink);
            }
            else
            {
                log.warn("Ignored invalid security-role-ref element: "
                        +"servlet-name="+name+", "+securityRef);
            }
        }

        XmlParser.Node run_as = node.get("run-as");
        if (run_as!=null)
        {
            String roleName=run_as.getString("role-name",false,true);
            if (roleName!=null)
                holder.setRunAs(roleName);
        }   
    }
    
    /* ------------------------------------------------------------ */
    protected void initServletMapping(XmlParser.Node node)
    {
        String name=node.getString("servlet-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);

        _webAppHandler.mapPathToServlet(pathSpec,name);
    }

    /* ------------------------------------------------------------ */
    protected void initListener(XmlParser.Node node)
    {
        String className=node.getString("listener-class",false,true);
        Object listener =null;
        try
        {
            Class listenerClass=loadClass(className);
            listener=listenerClass.newInstance();
        }
        catch(Exception e)
        {
            log.warn("Could not instantiate listener "+className,e);
            return;
        }

        if (!(listener instanceof EventListener))
        {
            log.warn("Not an EventListener: "+listener);
            return;
        }

        boolean known=false;
        try
        {
            addEventListener((EventListener)listener);
            known=true;
        }
        catch(Exception e)
        {
            LogSupport.ignore(log,e);
        }

        try
        {
            _webAppHandler.addEventListener((EventListener)listener);
            known=true;
        }
        catch(Exception e)
        {
            LogSupport.ignore(log,e);
        }
        
        if (!known)
            log.warn("Unknown: "+listener);
    }
    
    
    /* ------------------------------------------------------------ */
    protected void initSessionConfig(XmlParser.Node node)
    {
        XmlParser.Node tNode=node.get("session-timeout");
        if(tNode!=null)
        {
            int timeout = Integer.parseInt(tNode.toString(false,true));
            _webAppHandler.setSessionInactiveInterval(timeout*60);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initMimeConfig(XmlParser.Node node)
    {
        String extension= node.getString("extension",false,true);
        if (extension!=null && extension.startsWith("."))
            extension=extension.substring(1);
        
        String mimeType= node.getString("mime-type",false,true);
        setMimeMapping(extension,mimeType);
    }
    
    /* ------------------------------------------------------------ */
    protected void initWelcomeFileList(XmlParser.Node node)
    {
        Iterator iter= node.iterator("welcome-file");
        while(iter.hasNext())
        {
            XmlParser.Node indexNode=(XmlParser.Node)iter.next();
            String index=indexNode.toString(false,true);
            if(log.isDebugEnabled())log.debug("Index: "+index);
            addWelcomeFile(index);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initLocaleEncodingList(XmlParser.Node node)
    {
        Iterator iter= node.iterator("locale-encoding-mapping");
        while(iter.hasNext())
        {
            XmlParser.Node mapping=(XmlParser.Node)iter.next();
            String locale= mapping.getString("locale",false,true);
            String encoding= mapping.getString("encoding",false,true);
            addLocaleEncoding(locale,encoding);
        }
    }

    /* ------------------------------------------------------------ */
    protected void initErrorPage(XmlParser.Node node)
    {
        String error= node.getString("error-code",false,true);
        if (error==null || error.length()==0)
            error= node.getString("exception-type",false,true);
        
        String location= node.getString("location",false,true);
        setErrorPage(error,location);
    }
    
    /* ------------------------------------------------------------ */
    protected void initTagLib(XmlParser.Node node)
    {
        String uri= node.getString("taglib-uri",false,true);
        String location= node.getString("taglib-location",false,true);
        _tagLibMap.put(uri,location);
        setResourceAlias(uri,location);
    }
    
    /* ------------------------------------------------------------ */
    protected void initSecurityConstraint(XmlParser.Node node)
    {
        SecurityConstraint scBase = new SecurityConstraint();
        
        XmlParser.Node auths=node.get("auth-constraint");
        if (auths!=null)
        {
            scBase.setAuthenticate(true);
            // auth-constraint
            Iterator iter= auths.iterator("role-name");
            while(iter.hasNext())
            {
                String role=((XmlParser.Node)iter.next()).toString(false,true);
                scBase.addRole(role);
            }
        }
        
        XmlParser.Node data=node.get("user-data-constraint");
        if (data!=null)
        {
            data=data.get("transport-guarantee");
            String guarantee = data.toString(false,true).toUpperCase();
            if (guarantee==null || guarantee.length()==0 ||
                "NONE".equals(guarantee))
                scBase.setDataConstraint(SecurityConstraint.DC_NONE);
            else if ("INTEGRAL".equals(guarantee))
                scBase.setDataConstraint(SecurityConstraint.DC_INTEGRAL);
            else if ("CONFIDENTIAL".equals(guarantee))
                scBase.setDataConstraint(SecurityConstraint.DC_CONFIDENTIAL);
            else
            {
                log.warn("Unknown user-data-constraint:"+guarantee);
                scBase.setDataConstraint(SecurityConstraint.DC_CONFIDENTIAL);
            }
        }

        Iterator iter= node.iterator("web-resource-collection");
        while(iter.hasNext())
        {
            XmlParser.Node collection=(XmlParser.Node)iter.next();
            String name=collection.getString("web-resource-name",false,true);
            SecurityConstraint sc = (SecurityConstraint)scBase.clone();
            sc.setName(name);
            
            Iterator iter2= collection.iterator("http-method");
            while(iter2.hasNext())
                sc.addMethod(((XmlParser.Node)iter2.next())
                             .toString(false,true));

            iter2= collection.iterator("url-pattern");
            while(iter2.hasNext())
            {
                String url=
                    ((XmlParser.Node)iter2.next()).toString(false,true);
                addSecurityConstraint(url,sc);
            }
        }
    }
                                      
    /* ------------------------------------------------------------ */
    protected void initLoginConfig(XmlParser.Node node)
    {
        XmlParser.Node method=node.get("auth-method");
        if (method!=null)
        {
            Authenticator authenticator=null;
            String m=method.toString(false,true);
            
            if (SecurityConstraint.__FORM_AUTH.equals(m))
                authenticator=_formAuthenticator=new FormAuthenticator();
            else if (SecurityConstraint.__BASIC_AUTH.equals(m))
                authenticator=new BasicAuthenticator();
            else if (SecurityConstraint.__DIGEST_AUTH.equals(m))
                authenticator=new DigestAuthenticator();
            else if (SecurityConstraint.__CERT_AUTH.equals(m))
                authenticator=new ClientCertAuthenticator();
            else if (SecurityConstraint.__CERT_AUTH2.equals(m))
                authenticator=new ClientCertAuthenticator();
            else
                log.warn("UNKNOWN AUTH METHOD: "+m);

            setAuthenticator(authenticator);
        }
        
        XmlParser.Node name=node.get("realm-name");
        if (name!=null)
            setRealmName(name.toString(false,true));

        XmlParser.Node formConfig = node.get("form-login-config");
        if(formConfig != null)
        {
            if (_formAuthenticator==null)
                log.warn("FORM Authentication miss-configured");
            else
            {
                XmlParser.Node loginPage = formConfig.get("form-login-page");
                if (loginPage != null)
                    _formAuthenticator.setLoginPage(loginPage.toString(false,true));
                XmlParser.Node errorPage = formConfig.get("form-error-page");
                if (errorPage != null)
                {
                    String ep=errorPage.toString(false,true);
                    _formAuthenticator.setErrorPage(ep);
                    if (getErrorPage("403")==null)
                        setErrorPage("403",ep);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void initSecurityRole(XmlParser.Node node)
    {
    }

    /* ------------------------------------------------------------ */
    protected UserRealm getUserRealm(String name)
    {
        return getHttpServer().getRealm(name);
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "WebApplicationContext["+getHttpContextName()+","+
            (_name==null?_war:_name)+"]";
    }
    
    /* ------------------------------------------------------------ */
    public void setClassPath(String classPath)
    {
        log.warn("ClassPath should not be set for WebApplication");
        super.setClassPath(classPath);
    }
    
    /* ------------------------------------------------------------ */
    public void setResourceBase(String resourceBase)
    {
        log.warn("ResourceBase should not be set for WebApplication");
        super.setResourceBase(resourceBase);
    }
    
    /* ------------------------------------------------------------ */
    public void setBaseResource(Resource baseResource)
    {
        log.warn("BaseResource should not be set for WebApplication");
        super.setBaseResource(baseResource);
    }

    /* ------------------------------------------------------------ */
    /** Get the taglib map. 
     * @return A map of uri to location for tag libraries.
     */
    public Map getTagLibMap()
    {
        return _tagLibMap;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Set Resource Alias.
     * Resource aliases map resource uri's within a context.
     * They may optionally be used by a handler when looking for
     * a resource.  
     * @param alias 
     * @param uri 
     */
    public void setResourceAlias(String alias,String uri)
    {
        if (_resourceAliases==null)
            _resourceAliases=new HashMap(5);
        _resourceAliases.put(alias,uri);
    }
    
    /* ------------------------------------------------------------ */
    public String getResourceAlias(String alias)
    {
        if (_resourceAliases==null)
            return null;
       return (String) _resourceAliases.get(alias);
    }
    
    /* ------------------------------------------------------------ */
    public String removeResourceAlias(String alias)
    {
        if (_resourceAliases==null)
            return null;
       return (String) _resourceAliases.remove(alias);
    }

    
    /* ------------------------------------------------------------ */
    public Resource getResource(String uriInContext)
        throws IOException
    {
        IOException ioe=null;
        Resource resource=null;
        try
        {
            resource=super.getResource(uriInContext);
            if (resource!=null && resource.exists())
                return resource;
        }
        catch (IOException e)
        {
            ioe=e;
        }

        String aliasedUri=getResourceAlias(uriInContext);
        if (aliasedUri!=null)
            return super.getResource(aliasedUri);

        if (ioe!=null)
            throw ioe;

        return resource;
    }


    /* ------------------------------------------------------------ */
    /** set error page URI.
     * @param error A string representing an error code or a
     * exception classname
     * @param uriInContext
     */
    void setErrorPage(String error,String uriInContext)
    {
        if (_errorPages==null)
            _errorPages=new HashMap();
        _errorPages.put(error,uriInContext);
    }

    /* ------------------------------------------------------------ */
    /** get error page URI.
     * @param error A string representing an error code or a
     * exception classname
     * @return URI within context
     */
    String getErrorPage(String error)
    {
        if (_errorPages==null)
            return null;
       return (String) _errorPages.get(error);
    }


    /* ------------------------------------------------------------ */
    String removeErrorPage(String error)
    {
        if (_errorPages==null)
            return null;
       return (String) _errorPages.remove(error);
    }
    
}
