// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
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
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import org.mortbay.http.ContextLoader;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
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
/** Standard web.xml configured HttpContext.
 *
 * This specialization of HttpContext uses the standardized web.xml
 * to describe a web application and configure the handlers for the
 * HttpContext.
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
public class WebApplicationContext extends ServletHttpContext
{
    /* ------------------------------------------------------------ */
    private String _name;
    private Resource _webApp;
    private Resource _webInf;
    private NotFoundHandler _notFoundHandler;
    private HttpHandler _webInfHandler;
    private FilterHandler _filterHandler;
    private ServletHandler _servletHandler;
    private SecurityHandler _securityHandler;
    private ResourceHandler _resourceHandler;
    private Map _tagLibMap=new HashMap(3);
    private String _deploymentDescriptor;
    private String _defaultsDescriptor;
    private String _war;
    private boolean _extract;
    private XmlParser _xmlParser;
    private ArrayList _contextListeners;
    private ArrayList _contextAttributeListeners;
    private Set _warnings;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * This constructor should be used if the XmlParser needs to be
     * customized before initialization of the web application.
     * The XmlParser can be customized with the addition of observers
     * for specific tag types (eg ejb-ref).
     * @param httpServer The HttpServer for this context
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/* 
     * @deprecated                      
     */
    protected WebApplicationContext(HttpServer httpServer,
                                 String contextPathSpec)
    {
        super(httpServer,contextPathSpec);
        _xmlParser= new XmlParser();
        Code.warning("DEPRECATED CONSTRUCTOR");
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
            if (_webApp.exists() &&
                !_webApp.isDirectory() &&
                !_webApp.toString().startsWith("jar:"))
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
                File tempDir=new File(getTempDirectory(),"webapp");
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
        _securityHandler=(SecurityHandler)getHttpHandler(SecurityHandler.class);
        if (_securityHandler==null)
            _securityHandler=new SecurityHandler();
        if (getHttpHandlerIndex(_securityHandler)!=0)
        {
            removeHttpHandler(_securityHandler);
            addHttpHandler(0,_securityHandler);
        }        
        
        // Protect WEB-INF
        _webInfHandler=new WebInfProtect();
        addHttpHandler(1,_webInfHandler);
        
        // Add filter Handler
        _filterHandler = (FilterHandler)getHttpHandler(FilterHandler.class);
        if (_filterHandler==null)
        {
            _filterHandler=new FilterHandler();
            addHttpHandler(_filterHandler);
        }
        
        // Add servlet Handler
        _servletHandler = (ServletHandler)getHttpHandler(ServletHandler.class);
        if (_servletHandler==null)
        {
            _servletHandler=new ServletHandler();
            addHttpHandler(_servletHandler);
        }
        _servletHandler.setDynamicServletPathSpec("/servlet/*");
        
        // Check order
        if (getHttpHandlerIndex(_servletHandler)<getHttpHandlerIndex(_filterHandler))
        {
            removeHttpHandler(_servletHandler);
            addHttpHandler(_servletHandler);
        }
        
        // Resource Handler
        _resourceHandler = (ResourceHandler)getHttpHandler(ResourceHandler.class);
        if (_resourceHandler==null)
        {
            _resourceHandler=new ResourceHandler();
            _resourceHandler.setPutAllowed(false);
            _resourceHandler.setDelAllowed(false);
            addHttpHandler(_resourceHandler);
        }

        // Check order
        if (_servletHandler!=null &&
            getHttpHandlerIndex(_resourceHandler)<getHttpHandlerIndex(_servletHandler))
        {
            removeHttpHandler(_resourceHandler);
            addHttpHandler(_resourceHandler);
        }
        
        // NotFoundHandler
        _notFoundHandler=(NotFoundHandler)getHttpHandler(NotFoundHandler.class);
        if (_notFoundHandler==null)
            _notFoundHandler=new NotFoundHandler();
        else
            removeHttpHandler(_notFoundHandler);
        addHttpHandler(_notFoundHandler);
        
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
        if (_webInf!=null && _webInf.isDirectory())
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

        // initialize the classloader (if it has not been so already)
        initClassLoader();
        
        // Set classpath for Jasper.
        if (_servletHandler!=null)
        {
            Map.Entry entry = _servletHandler.getHolderEntry("test.jsp");
            if (entry!=null)
            {
                ServletHolder jspHolder = (ServletHolder)entry.getValue();
                if (jspHolder!=null && jspHolder.getInitParameter("classpath")==null)
                {
                    String fileClassPath=getFileClassPath();
                    jspHolder.setInitParameter("classpath",fileClassPath);
                    Code.debug("Set classpath=",fileClassPath," for ",jspHolder);
                }
            }
        }
        
        // Start handlers
        Exception ex=null;
        try {super.start();} catch(Exception e){ex=e;}

        // Handle context init.
        if (ex==null || super.isStarted())
        {
            // Context listeners
            if (_contextListeners!=null && _servletHandler!=null)
            {
                ServletContextEvent event = new ServletContextEvent(getServletContext());
                for (int i=0;i<_contextListeners.size();i++)
                    ((ServletContextListener)_contextListeners.get(i))
                        .contextInitialized(event);
            }
        
            if (_resourceHandler.isPutAllowed())
                Log.event("PUT allowed in "+this);
            if (_resourceHandler.isDelAllowed())
                Log.event("DEL allowed in "+this);
        }
        
        if (ex!=null)
            throw ex;
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
        if (_contextListeners!=null && _servletHandler!=null)
        {
            ServletContextEvent event = new ServletContextEvent(getServletContext());
            super.stop();
            for (int i=0;i<_contextListeners.size();i++)
                ((ServletContextListener)_contextListeners.get(i))
                    .contextDestroyed(event);
        }
        else
            super.stop();

        if (_resourceHandler!=null)
            removeHttpHandler(_resourceHandler);
        _resourceHandler=null;
        
        if (_servletHandler!=null)
            removeHttpHandler(_servletHandler);
        _servletHandler=null;
        
        if (_filterHandler!=null)
            removeHttpHandler(_filterHandler);
        _filterHandler=null;
        
        if (_webInfHandler!=null)
            removeHttpHandler(_webInfHandler);
        _webInfHandler=null;
        
        if (_securityHandler!=null)
            removeHttpHandler(_securityHandler);
        _securityHandler=null;
        
        if (_notFoundHandler!=null)
            removeHttpHandler(_notFoundHandler);
        _notFoundHandler=null;
    }

    /* ------------------------------------------------------------ */
    /** Get the complete file classpath of webapplication.
     * This method makes a best effort to return a complete file
     * classpath for the context.  The default implementation returns
     * <PRE>
     *  ((ContextLoader)getClassLoader()).getFileClassPath()+
     *       System.getProperty("path.separator")+
     *       System.getProperty("java.class.path");
     * </PRE>
     * The default implementation requires the classloader to be
     * initialized before it is called. It will not include any
     * classpaths used by a non-system parent classloader.
     * <P>
     * The main user of this method is the start() method.  If a JSP
     * servlet is detected, the string returned from this method is
     * used as the default value for the "classpath" init parameter.
     * <P>
     * Derivations may replace this method with a more accurate or
     * specialized version.
     * @return Path of files and directories for loading classes.
     * @exception IllegalStateException HttpContext.initClassLoader
     * has not been called.
     */
    public String getFileClassPath()
        throws IllegalStateException
    {
        ClassLoader loader = getClassLoader();
        if (loader==null)
            throw new IllegalStateException("Context classloader not initialized");
        String fileClassPath =
            ((loader instanceof ContextLoader)
             ? ((ContextLoader)loader).getFileClassPath()
             : getClassPath())+
            System.getProperty("path.separator")+
            System.getProperty("java.class.path");
        return fileClassPath;
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
                new ServletContextAttributeEvent(getServletContext(),
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
                new ServletContextAttributeEvent(getServletContext(),
                                                 name,old);
            for (int i=0;i<_contextAttributeListeners.size();i++)
            {
                ServletContextAttributeListener l =
                    (ServletContextAttributeListener)
                    _contextAttributeListeners.get(i);
                l.attributeRemoved(event);    
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

                initWebXmlElement(name,node);
            }
            catch(ClassNotFoundException e)
            {
                throw e;
            }
            catch(Exception e)
            {
                Code.warning("Configuration problem at "+node,e);
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
        else if ("error-page".equals(element))
            initErrorPage(node);
        else if ("taglib".equals(element))
            initTagLib(node);
        else if ("resource-ref".equals(element))
            Code.debug("No implementation: ",node);
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
                Code.debug("Not Implemented: ",node);
            else
            {
                _warnings.add(element);
                Code.warning("Element "+element+" not handled in "+this);
                Code.debug(node);
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
        
        FilterHolder holder = _filterHandler.newFilterHolder(name,className);
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
    private void initFilterMapping(XmlParser.Node node)
    {
        String filterName=node.getString("filter-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);
        String servletName=node.getString("servlet-name",false,true);
        
        if (servletName!=null)
            _filterHandler.mapServletToFilter(servletName,filterName);
        else
            _filterHandler.mapPathToFilter(pathSpec,filterName);
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
        
        ServletHolder holder = _servletHandler.newServletHolder(name,className,jspFile);
        
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
        _servletHandler.addServletHolder(defaultPath,holder);
        if (!className.equals(name))
        {
            defaultPath="/servlet/"+className+"/*";
            Code.debug("ServletMapping: ",holder.getName(),
                       "=",defaultPath);
            _servletHandler.addServletHolder(defaultPath,holder);
        }
    }
    
    /* ------------------------------------------------------------ */
    private void initServletMapping(XmlParser.Node node)
    {
        String name=node.getString("servlet-name",false,true);
        String pathSpec=node.getString("url-pattern",false,true);

        _servletHandler.mapPathToServlet(pathSpec,name);
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
    private void initSessionConfig(XmlParser.Node node)
    {
        XmlParser.Node tNode=node.get("session-timeout");
        if(tNode!=null)
        {
            int timeout = Integer.parseInt(tNode.toString(false,true));
            _servletHandler.setSessionInactiveInterval(timeout*60);
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
        _resourceHandler.setIndexFiles(null);
        Iterator iter= node.iterator("welcome-file");
        while(iter.hasNext())
        {
            XmlParser.Node indexNode=(XmlParser.Node)iter.next();
            String index=indexNode.toString(false,true);
            Code.debug("Index: ",index);
            _resourceHandler.addIndexFile(index);
        }
    }

    /* ------------------------------------------------------------ */
    private void initErrorPage(XmlParser.Node node)
    {
        String error= node.getString("error-code",false,true);
        if (error==null || error.length()==0)
            error= node.getString("exception-type",false,true);
        
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
        return "WebApplicationContext["+getHttpContextName()+","+
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
