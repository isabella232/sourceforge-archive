// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import org.mortbay.http.HandlerContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.util.Code;
import org.mortbay.util.JarResource;
import org.mortbay.util.Log;
import org.mortbay.util.Resource;
import org.mortbay.util.StringUtil;
import org.mortbay.xml.XmlConfiguration;
import org.mortbay.xml.XmlParser;


/* ------------------------------------------------------------ */
/** Standard web.xml configured HandlerContext.
 *
 * This specialization of HandlerContext uses the standardized web.xml
 * to describe a web application and configure the handlers for the
 * HandlerContext.
 * <P>
 * It creates and/or configures the following Handlers:<UL>
 * <LI>SecurityHandler - Implements BASIC and FORM
 * authentication. This handler is forced to be the first handler in
 * the context.
 * <LI>ServletHandler - Servlet handler for dynamic and configured
 * servlets
 * <LI>WebInfProtect - A handler to ensure the WEB-INF is protected
 * from all requests. This is always installed before the ResourceHandler
 * <LI>ResourceHandler - Serves static content and if forced to be
 * after the servlet handler within the context.
 * </UL>
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContext extends ServletHandlerContext
{
    /* ------------------------------------------------------------ */
    private String _name;
    private Resource _webApp;
    private Resource _webInf;
    private ServletHandler _servletHandler;
    private SecurityHandler _securityHandler;
    private Map _tagLibMap=new HashMap(3);
    private NotFoundHandler _notFoundHandler;
    private String _deploymentDescriptor;
    private String _defaultsDescriptor;
    private String _war;
    private boolean _extract;
    private XmlParser _xmlParser;
    private ArrayList _filters;
    private ArrayList _contextListeners;
    private ArrayList _contextAttributeListeners;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * This constructor should be used if the XmlParser needs to be
     * customized before initialization of the web application.
     * The XmlParser can be customized with the addition of observers
     * for specific tag types (eg ejb-ref).
     * @param httpServer The HttpServer for this context
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     */
    public WebApplicationContext(HttpServer httpServer,
                                 String contextPathSpec)
    {
        super(httpServer,contextPathSpec);
        _xmlParser= new XmlParser();
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer The HttpServer for this context
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     * @param webApp The Web application directory or WAR file.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @exception IOException 
     */
    WebApplicationContext(HttpServer httpServer,
                          String contextPathSpec,
                          String webApp,
                          String defaults)
        throws IOException
    {
        super(httpServer,contextPathSpec);
        _xmlParser= new XmlParser();
        initialize(webApp,defaults,false);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer The HttpServer for this context
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     * @param webApp The Web application directory or WAR file.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @param extractWar If true, WAR files are extracted to a
     * temporary directory.
     * @exception IOException 
     */
    public WebApplicationContext(HttpServer httpServer,
                                 String contextPathSpec,
                                 String webApp,
                                 String defaults,
                                 boolean extractWar)
        throws IOException
    {
        super(httpServer,contextPathSpec);
        _xmlParser= new XmlParser();
        initialize(webApp,defaults,extractWar);
    }
    
    /* ------------------------------------------------------------ */
    /** Initialize.
     * This method can be called directly if the null constructor was
     * used. This style of construction allows the XmlParser to be
     * configured with observers before initialization.
     * @param webApp The Web application directory or WAR file.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @param extractWar If true, WAR files are extracted to a
     * temporary directory.
     * @exception IOException 
     */
    public void initialize(String webApp,
                           String defaults,
                           boolean extractWar)
        throws IOException
    {
        _war=webApp;
        _defaultsDescriptor=defaults;
        _extract=extractWar;
        _webApp=null;
        resolveWebApp();
    }


    /* ------------------------------------------------------------ */
    private void resolveWebApp()
        throws IOException
    {
        if (_webApp==null)
        {
            // Set dir or WAR
            _webApp = Resource.newResource(_war);
            if (_webApp.exists() && !_webApp.isDirectory())
            {
                _webApp = Resource.newResource("jar:"+_webApp+"!/");
                if (_webApp.exists())
                    _war=_webApp.toString();
            }
            if (!_webApp.exists()) {
                Code.warning("Web application not found "+_war);
                throw new java.io.FileNotFoundException(_war);
            }
            
            // Expand
            if (_extract && _webApp instanceof JarResource)
            {
                File tempDir=File.createTempFile("Jetty-",".war");
                if (tempDir.exists())
                    tempDir.delete();
                tempDir.mkdir();
                tempDir.deleteOnExit();
                Log.event("Extract "+_war+" to "+tempDir);
                ((JarResource)_webApp).extract(tempDir,true);
                _webApp=Resource.newResource(tempDir.getCanonicalPath());
            }

            _webInf = _webApp.addPath("WEB-INF/");
            if (!_webInf.exists() || !_webInf.isDirectory())
                _webInf=null;
            
            // ResourcePath
            super.setBaseResource(_webApp);
        }
    }

    
    /* ------------------------------------------------------------ */
    /** Start the Web Application.
     * @exception IOException 
     */
    public void start()
        throws Exception
    {
        // Get parser
        XmlParser xmlParser=_xmlParser==null?(new XmlParser()):_xmlParser;
        
        Resource dtd22=Resource.newSystemResource("/javax/servlet/resources/web-app_2_2.dtd");
        Resource dtd23=Resource.newSystemResource("/javax/servlet/resources/web-app_2_3.dtd");
        xmlParser.redirectEntity("web-app_2_2.dtd",dtd22);
        xmlParser.redirectEntity("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN",dtd22);
        xmlParser.redirectEntity("web.dtd",dtd23);
        xmlParser.redirectEntity("web-app_2_3.dtd",dtd23);
        xmlParser.redirectEntity("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN",dtd22);

        // Find the webapp
        resolveWebApp();

        // add security handler first
        _securityHandler=(SecurityHandler)getHandler(SecurityHandler.class);
        if (_securityHandler==null)
            _securityHandler=new SecurityHandler();
        if (getHandlerIndex(_securityHandler)!=0)
        {
            removeHandler(_securityHandler);
            addHandler(0,_securityHandler);
        }
        
        // Add servlet Handler
        _servletHandler = (ServletHandler)getHandler(ServletHandler.class);
        if (_servletHandler==null)
        {
            _servletHandler=new ServletHandler();
            addHandler(_servletHandler);
        }
        _servletHandler.setDynamicServletPathSpec("/servlet/*");
        
        // Resource Handler
        ResourceHandler rh = (ResourceHandler)getHandler(ResourceHandler.class);
        if (rh==null)
        {
            rh=new ResourceHandler();
            rh.setPutAllowed(false);
            rh.setDelAllowed(false);
            addHandler(rh);
        }

        // Check order
        if (getHandlerIndex(rh)<getHandlerIndex(_servletHandler))
        {
            removeHandler(rh);
            addHandler(rh);
        }
        
        // Protect WEB-INF
        addHandler(getHandlerIndex(rh),new WebInfProtect());
        
        // Do the default configuration
        try
        {
            if (_defaultsDescriptor!=null && _defaultsDescriptor.length()>0)
            {
                Resource dftResource= Resource.newResource(_defaultsDescriptor);
                _defaultsDescriptor=dftResource.toString();
                XmlParser.Node defaultConfig =
                    xmlParser.parse(dftResource.getURL().toString());
                initialize(defaultConfig);
            }
        }
        catch(IOException e)
        {
            Code.warning("Parse error on "+_war,e);
            throw e;
        }	
        catch(Exception e)
        {
            Code.warning("Configuration error "+_war,e);
            throw new IOException("Parse error on "+_war+
                                  ": "+e.toString());
        }
        
        // Do we have a WEB-INF
        if (_webInf==null || !_webInf.isDirectory())
        {
            Code.warning("No WEB-INF in "+_war+". Serving files only.");
        }
        else
        {
            // Look for classes directory
            Resource classes = _webInf.addPath("classes/");
            String classPath="";
            if (classes.exists())
                super.setClassPath(classes.toString());
            
            // Look for jars
            Resource lib = _webInf.addPath("lib/");
            super.setClassPaths(lib,true);            

            // do web.xml file
            Resource web = _webInf.addPath("web.xml");
            if (!web.exists())
            {
                Code.warning("No WEB-INF/web.xml in "+_war+". Serving files and default/dynamic servlets only");
            }
            else
            {
                try
                {
                    _deploymentDescriptor=web.toString();
                    XmlParser.Node config = xmlParser.parse(web.getURL().toString());
                    initialize(config);
                }
                catch(IOException e)
                {
                    Code.warning("Parse error on "+_war,e);
                    throw e;
                }	
                catch(Exception e)
                {
                    Code.warning("Configuration error "+_war,e);
                    throw new IOException("Parse error on "+_war+
                                          ": "+e.toString());
                }
            }

            // do jetty.xml file
            Resource jetty = _webInf.addPath("web-jetty.xml");
            if (jetty.exists())
            {
                try
                {
                    Log.event("Configure: "+jetty);
                    XmlConfiguration jetty_config=new
                        XmlConfiguration(jetty.getURL());
                    jetty_config.configure(this);
                }
                catch(IOException e)
                {
                    Code.warning("Parse error on "+_war,e);
                    throw e;
                }	
                catch(Exception e)
                {
                    Code.warning("Configuration error "+_war,e);
                    throw new IOException("Parse error on "+_war+
                                          ": "+e.toString());
                }
            }
        }

        // Start filters
        if (_filters!=null)
            for (int i=0;i<_filters.size();i++)
                ((FilterHolder)_filters.get(i)).start();

        // Start handlers
        super.start();

        // Context listeners
        if (_contextListeners!=null && _servletHandler!=null)
        {
            ServletContextEvent event = new ServletContextEvent(_servletHandler.getServletContext());
            for (int i=0;i<_contextListeners.size();i++)
                ((ServletContextListener)_contextListeners.get(i))
                    .contextInitialized(event);
        }
        
        if (rh.isPutAllowed())
            Log.event("PUT allowed in "+this);
        if (rh.isDelAllowed())
            Log.event("DEL allowed in "+this);
    }

    /* ------------------------------------------------------------ */
    public void stop()
        throws  InterruptedException
    {
        if (_filters!=null)
            for (int i=0;i<_filters.size();i++)
                ((FilterHolder)_filters.get(i)).stop();

        // Context listeners
        if (_contextListeners!=null && _servletHandler!=null)
        {
            ServletContextEvent event = new ServletContextEvent(_servletHandler.getServletContext());
            super.stop();
            for (int i=0;i<_contextListeners.size();i++)
                ((ServletContextListener)_contextListeners.get(i))
                    .contextDestroyed(event);
        }
        else
            super.stop();
    }
    
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        Code.notImplemented();
    }

    /* ------------------------------------------------------------ */
    public synchronized void addEventListener(EventListener listener)
        throws IllegalArgumentException
    {
        boolean known=false;
        if (listener instanceof ServletContextListener)
        {
            known=true;
            if (_contextListeners==null)
                _contextListeners=new ArrayList(3);
            _contextListeners.add(listener);
        }
        
        if (listener instanceof ServletContextAttributeListener)
        {
            known=true;
            if (_contextAttributeListeners==null)
                _contextAttributeListeners=new ArrayList(3);
            _contextAttributeListeners.add(listener);
        }

        if (!known)
            throw new IllegalArgumentException("Unknown "+listener);
    }

    /* ------------------------------------------------------------ */
    public synchronized void removeEventListener(EventListener listener)
    {
        if ((listener instanceof ServletContextListener) &&
            _contextListeners!=null)
            _contextListeners.remove(listener);
        
        if ((listener instanceof ServletContextAttributeListener) &&
            _contextAttributeListeners!=null)
            _contextAttributeListeners.remove(listener);
    }

    /* ------------------------------------------------------------ */
    public synchronized void setAttribute(String name, Object value)
    {
        Object old = super.getAttribute(name);
        super.setAttribute(name,value);

        if (_contextAttributeListeners!=null && _servletHandler!=null)
        {
            ServletContextAttributeEvent event =
                new ServletContextAttributeEvent(_servletHandler.getServletContext(),
                                                 name,
                                                 old!=null?old:value);
            for (int i=0;i<_contextAttributeListeners.size();i++)
            {
                ServletContextAttributeListener l =
                    (ServletContextAttributeListener)
                    _contextAttributeListeners.get(i);
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
        
        if (old !=null && _contextAttributeListeners!=null && _servletHandler!=null)
        {
            ServletContextAttributeEvent event =
                new ServletContextAttributeEvent(_servletHandler.getServletContext(),
                                                 name,old);
            for (int i=0;i<_contextAttributeListeners.size();i++)
            {
                ServletContextAttributeListener l =
                    (ServletContextAttributeListener)
                    _contextAttributeListeners.get(i);
                l.attributeReplaced(event);    
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public XmlParser getXmlParser()
    {
        return _xmlParser;
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
    public void setDefaultsDescriptor(String defaults)
    {
        if (isStarted())
            throw new IllegalStateException();
        _defaultsDescriptor=defaults;
    }
    
    /* ------------------------------------------------------------ */
    public String getDefaultsDescriptor()
    {
        return _defaultsDescriptor;
    }
    
    /* ------------------------------------------------------------ */
    public void setWAR(String war)
    {
        if (isStarted())
            throw new IllegalStateException();
        _war=war;
    }
    
    /* ------------------------------------------------------------ */
    public String getWAR()
    {
        return _war;
    }
    
    /* ------------------------------------------------------------ */
    public void setExtractWAR(boolean extract)
    {
        if (isStarted())
            throw new IllegalStateException();
        _extract=extract;
    }
    
    /* ------------------------------------------------------------ */
    public boolean getExtractWAR()
    {
        return _extract;
    }

    /* ------------------------------------------------------------ */
    private void initialize(XmlParser.Node config)
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
                
                if ("display-name".equals(name))
                    initDisplayName(node);
                else if ("description".equals(name))
                {}
                else if ("distributable".equals(name))
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
                else if ("context-param".equals(name))
                    initContextParam(node);
                else if ("servlet".equals(name))
                    initServlet(node);
                else if ("servlet-mapping".equals(name))
                    initServletMapping(node);
                else if ("session-config".equals(name))
                    initSessionConfig(node);
                else if ("mime-mapping".equals(name))
                    initMimeConfig(node);
                else if ("welcome-file-list".equals(name))
                    initWelcomeFileList(node);
                else if ("error-page".equals(name))
                    initErrorPage(node);
                else if ("taglib".equals(name))
                    initTagLib(node);
                else if ("resource-ref".equals(name))
                    Code.debug("No implementation: ",node);
                else if ("security-constraint".equals(name))
                    initSecurityConstraint(node);
                else if ("login-config".equals(name))
                    initLoginConfig(node);
                else if ("security-role".equals(name))
                    initSecurityRole(node);
                else if ("env-entry".equals(name))
                    Code.warning("Not implemented: "+node);
                else if ("filter".equals(name))
                    initFilter(node);
                else if ("filter-mapping".equals(name))
                    initFilterMapping(node);
                else if ("listener".equals(name))
                    initListener(node);
                else if ("ejb-ref".equals(name))
                    Code.debug("No implementation: ",node);
                else if ("ejb-local-ref".equals(name))
                    Code.debug("No implementation: ",node);
                else
                {
                    Code.warning("UNKNOWN TAG: "+name);
                    System.err.println(node);
                }
            }
            catch(Exception e)
            {
                Code.warning("Configuration problem at "+node,e);
            }
        }
        
    }

    /* ------------------------------------------------------------ */
    private void initDisplayName(XmlParser.Node node)
    {
        _name=node.toString(false,true);
    }
    
    /* ------------------------------------------------------------ */
    private void initContextParam(XmlParser.Node node)
    {
        String name=node.getString("param-name",false,true);
        String value=node.getString("param-value",false,true);
        Code.debug("ContextParam: ",name,"=",value);

        setInitParameter(name,value); 
    }

    /* ------------------------------------------------------------ */
    private void initFilter(XmlParser.Node node)
        throws ClassNotFoundException, UnavailableException
    {
        String name=node.getString("filter-name",false,true);
        String className=node.getString("filter-class",false,true);
        
        if (className==null)
        {
            Code.warning("Missing filter-class in "+node);
            return;
        }
        if (name==null)
            name=className;
        
        FilterHolder holder =
            new FilterHolder(_servletHandler,name,className);
        
        Iterator iter= node.iterator("init-param");
        while(iter.hasNext())
        {
            XmlParser.Node paramNode=(XmlParser.Node)iter.next();
            String pname=paramNode.getString("param-name",false,true);
            String pvalue=paramNode.getString("param-value",false,true);
            holder.put(pname,pvalue);
        }

        addFilterHolder(holder);
    }
    
    /* ------------------------------------------------------------ */
    private void initFilterMapping(XmlParser.Node node)
    {
        String name=node.getString("filter-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);
        String servletName=node.getString("url-pattern",false,true);
        
        FilterHolder filterHolder = getFilterHolder(name);
        if (filterHolder==null)
            Code.warning("No such filter: "+name);
        else
        {
            if (servletName!=null)
            {
                ServletHolder holder =
                    _servletHandler.getServletHolder(name);
                System.err.println(filterHolder+" --> "+holder);
            }
            else
            {
                System.err.println(filterHolder+" --> "+pathSpec);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    private void initServlet(XmlParser.Node node)
        throws ClassNotFoundException, UnavailableException
    {
        String name=node.getString("servlet-name",false,true);
        String className=node.getString("servlet-class",false,true);
        String jspFile=null;
        
        if (className==null)
        {
            // There is no class, so look for a jsp file
            jspFile=node.getString("jsp-file",false,true);
            if (jspFile!=null)
                className="org.apache.jasper.servlet.JspServlet";
            else
            {
                Code.warning("Missing servlet-class|jsp-file in "+node);
                return;
            }
        }
        if (name==null)
            name=className;
        
        ServletHolder holder =
            new ServletHolder(_servletHandler,name,className,jspFile);
        
        Iterator iter= node.iterator("init-param");
        while(iter.hasNext())
        {
            XmlParser.Node paramNode=(XmlParser.Node)iter.next();
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
                Code.warning("Deprecated boolean load-on-startup.  Please use integer");
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
                    Code.warning("Cannot parse load-on-startup "+s+". Please use integer");
                    Code.ignore(e);
                }
                holder.setInitOrder(order);
            }
        }
    
        XmlParser.Node securityRef = node.get("security-role-ref");
        if (securityRef!=null)
        {
            String roleName=securityRef.getString("role-name",false,true);
            String roleLink=securityRef.getString("role-link",false,true);
            Code.debug("link role ",roleName," to ",roleLink," for ",this);
            holder.setUserRoleLink(roleName,roleLink);
        }

        // add default mappings
        String defaultPath="/servlet/"+name+"/*";
        Code.debug("ServletMapping: ",holder.getName(),"=",defaultPath);
        _servletHandler.addHolder(defaultPath,holder);
        if (!className.equals(name))
        {
            defaultPath="/servlet/"+className+"/*";
            Code.debug("ServletMapping: ",holder.getName(),
                       "=",defaultPath);
            _servletHandler.addHolder(defaultPath,holder);
        }
    }
    

    /* ------------------------------------------------------------ */
    private void initListener(XmlParser.Node node)
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
            Code.warning("Could not instantiate listener "+className,e);
            return;
        }

        if (!(listener instanceof EventListener))
        {
            Code.warning("Not an EventListener: "+listener);
            return;
        }

        boolean known=false;
        if ((listener instanceof ServletContextListener) ||
            (listener instanceof ServletContextAttributeListener))
        {
            known=true;
            addEventListener((EventListener)listener);
        }
        
        if((listener instanceof HttpSessionActivationListener) ||
           (listener instanceof HttpSessionAttributeListener) ||
           (listener instanceof HttpSessionBindingListener) ||
           (listener instanceof HttpSessionListener))
        {
            known=true;
            _servletHandler.addEventListener((EventListener)listener);
        }
        if (!known)
            Code.warning("Unknown: "+listener);
    }
    
    /* ------------------------------------------------------------ */
    private void initServletMapping(XmlParser.Node node)
    {
        String name=node.getString("servlet-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);

        ServletHolder holder = _servletHandler.getServletHolder(name);
        if (holder==null)
            Code.warning("No such servlet: "+name);
        else
        {
            Code.debug("ServletMapping: ",name,"=",pathSpec);
            _servletHandler.addHolder(pathSpec,holder);
        }
    }
    
    /* ------------------------------------------------------------ */
    private void initSessionConfig(XmlParser.Node node)
    {
        XmlParser.Node tNode=node.get("session-timeout");
        if(tNode!=null)
        {
            int timeout = Integer.parseInt(tNode.toString(false,true));
            _servletHandler.setSessionTimeout(timeout);
        }
    }
    
    /* ------------------------------------------------------------ */
    private void initMimeConfig(XmlParser.Node node)
    {
        String extension= node.getString("extension",false,true);
        if (extension!=null && extension.startsWith("."))
            extension=extension.substring(1);
        
        String mimeType= node.getString("mime-type",false,true);
        setMimeMapping(extension,mimeType);
    }
    
    /* ------------------------------------------------------------ */
    private void initWelcomeFileList(XmlParser.Node node)
    {
        ResourceHandler rh = getResourceHandler();
        rh.setIndexFiles(null);
        
        Iterator iter= node.iterator("welcome-file");
        while(iter.hasNext())
        {
            XmlParser.Node indexNode=(XmlParser.Node)iter.next();
            String index=indexNode.toString(false,true);
            Code.debug("Index: ",index);
            rh.addIndexFile(index);
        }
    }

    /* ------------------------------------------------------------ */
    private void initErrorPage(XmlParser.Node node)
    {
        String error= node.getString("error-code",false,true);
        if (error==null || error.length()==0)
            error= node.getString("exception-type",false,true);
        else if (_notFoundHandler==null)
        {
            _notFoundHandler=new NotFoundHandler();
            addHandler(_notFoundHandler);
        }
        
        String location= node.getString("location",false,true);
        setErrorPage(error,location);
    }
    
    /* ------------------------------------------------------------ */
    private void initTagLib(XmlParser.Node node)
    {
        String uri= node.getString("taglib-uri",false,true);
        String location= node.getString("taglib-location",false,true);
        _tagLibMap.put(uri,location);
        setResourceAlias(uri,location);
    }
    
    /* ------------------------------------------------------------ */
    private void initSecurityConstraint(XmlParser.Node node)
    {
        SecurityConstraint scBase = new SecurityConstraint();
        
        XmlParser.Node auths=node.get("auth-constraint");
        if (auths!=null)
        {
            Iterator iter= auths.iterator("role-name");
            while(iter.hasNext())
            {
                XmlParser.Node role=(XmlParser.Node)iter.next();
                scBase.addRole(role.toString(false,true));
            }
        }
        
        XmlParser.Node data=node.get("user-data-constraint");
        if (data!=null)
        {
            data=data.get("transport-guarantee");
            String guarantee = data.toString(false,true).toUpperCase();
            if (guarantee==null || guarantee.length()==0 ||
                "NONE".equals(guarantee))
                scBase.setDataConstraint(scBase.DC_NONE);
            else if ("INTEGRAL".equals(guarantee))
                scBase.setDataConstraint(scBase.DC_INTEGRAL);
            else if ("CONFIDENTIAL".equals(guarantee))
                scBase.setDataConstraint(scBase.DC_CONFIDENTIAL);
            else
            {
                Code.warning("Unknown user-data-constraint:"+guarantee);
                scBase.setDataConstraint(scBase.DC_CONFIDENTIAL);
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
    private void initLoginConfig(XmlParser.Node node)
    {
        SecurityHandler sh = getSecurityHandler();
        if (sh==null)
            return;

        XmlParser.Node method=node.get("auth-method");
        if (method!=null)
            sh.setAuthMethod(method.toString(false,true));
        XmlParser.Node name=node.get("realm-name");
        if (name!=null)
            sh.setRealmName(name.toString(false,true));

        XmlParser.Node formConfig = node.get("form-login-config");
        if(formConfig != null)
        {
          XmlParser.Node loginPage = formConfig.get("form-login-page");
          if (loginPage != null)
            sh.setLoginPage(loginPage.toString(false,true));
          XmlParser.Node errorPage = formConfig.get("form-error-page");
          if (errorPage != null)
            sh.setErrorPage(errorPage.toString(false,true));
        }
    }
    
    /* ------------------------------------------------------------ */
    private void initSecurityRole(XmlParser.Node node)
    {
        // XXX - not sure what needs to be done here.
        // Could check that the role is known to the security handler
        // but it could be initialized later?
        Log.event("Security role "+
                  node.get("role-name").toString(false,true)+
                  " defined");
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "WebApplicationContext["+getHandlerContextName()+","+
            (_name==null?_war:_name)+"]";
    }
    
    /* ------------------------------------------------------------ */
    public void setClassPath(String classPath)
    {
        Code.warning("ClassPath should not be set for WebApplication");
        super.setClassPath(classPath);
    }
    
    /* ------------------------------------------------------------ */
    public void setResourceBase(String resourceBase)
    {
        Code.warning("ResourceBase should not be set for WebApplication");
        super.setResourceBase(resourceBase);
    }
    
    /* ------------------------------------------------------------ */
    public void setBaseResource(Resource baseResource)
    {
        Code.warning("BaseResource should not be set for WebApplication");
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
    public synchronized void addFilterHolder(FilterHolder filter)
    {
        if (_filters==null)
            _filters=new ArrayList(3);
        _filters.add(filter);
        Code.debug("addFilterHolder "+filter);
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder getFilterHolder(String name)
    {
        if (_filters==null)
            return null;
        for (int i=0;i<_filters.size();i++)
        {
            FilterHolder holder=(FilterHolder)_filters.get(i);
            if (holder.getName().equals(name))
                return holder;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class WebInfProtect extends NullHandler
    {
        public void handle(String pathInContext,
                           String pathParams,
                           HttpRequest request,
                           HttpResponse response)
            throws HttpException, IOException
        {
            String path=Resource.canonicalPath(StringUtil.asciiToLowerCase(pathInContext));
            if(path.startsWith("/web-inf") || path.startsWith("/meta-inf" ))
            {
                response.sendError(HttpResponse.__403_Forbidden);
            }
        }

        public String toString()
        {
            return "WebInfProtect";
        }
    }
}
