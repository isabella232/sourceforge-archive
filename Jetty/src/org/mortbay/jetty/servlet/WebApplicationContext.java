// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import org.mortbay.http.HttpServer;
import org.mortbay.http.HandlerContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpException;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.Resource;
import org.mortbay.util.JarResource;
import org.mortbay.util.StringUtil;
import org.mortbay.xml.XmlParser;
import org.mortbay.xml.XmlConfiguration;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.UnavailableException;


/* ------------------------------------------------------------ */
/** Standard web.xml configured HandlerContext.
 *
 * This specialization of HandlerContext uses the standardized web.xml
 * to describe a web application and configure the handlers for the
 * HandlerContext.
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
    private Context _context;
    private Map _tagLibMap=new HashMap(3);
    private NotFoundHandler _notFoundHandler;
    private String _deploymentDescriptor;
    private String _defaultsDescriptor;
    private String _war;
    private boolean _extract;
    private XmlParser _xmlParser;

    
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
        Resource dtd=Resource.newSystemResource("org/mortbay/jetty/servlet/web.dtd");
        xmlParser.redirectEntity("web.dtd",dtd);
        xmlParser.redirectEntity("web-app_2_2.dtd",dtd);
        xmlParser.redirectEntity("-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN",dtd);

        // Find the webapp
        resolveWebApp();

        // add security handler first
        _securityHandler=new SecurityHandler();
        addHandler(_securityHandler);
            
        // Add servlet Handler
        _servletHandler = (ServletHandler)
            getHandler(org.mortbay.jetty.servlet.ServletHandler.class);
        if (_servletHandler==null)
        {
            _servletHandler=new ServletHandler();
            addHandler(_servletHandler);
        }
        _servletHandler.setDynamicServletPathSpec("/servlet/*");
        _context=_servletHandler.getContext();

        // Protect WEB-INF
        addHandler(new WebInfProtect());
        
        // Resource Handler
        setServingResources(true);
        ResourceHandler rh = getResourceHandler();
        rh.setPutAllowed(false);
        rh.setDelAllowed(false);

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
                    if (_defaultsDescriptor!=null && _defaultsDescriptor.length()>0)
                    {
                        rh.setPutAllowed(true);
                        rh.setDelAllowed(true);
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
        super.start();
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
                    Code.debug("No implementation: ",node);
                else if ("ejb-ref".equals(name))
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
                className=_servletHandler.getJSPClassName();
            else
            {
                Code.warning("Missing servlet-class|jsp-file in "+node);
                return;
            }
        }
        if (name==null)
            name=className;
        
        ServletHolder holder =
            _servletHandler.newServletHolder(className,jspFile);
        holder.setServletName(name);
        
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
        Code.debug("ServletMapping: ",holder.getServletName(),"=",defaultPath);
        _servletHandler.addHolder(defaultPath,holder);
        if (!className.equals(name))
        {
            defaultPath="/servlet/"+className+"/*";
            Code.debug("ServletMapping: ",holder.getServletName(),
                       "=",defaultPath);
            _servletHandler.addHolder(defaultPath,holder);
        }
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
            _context.setSessionTimeout(timeout);
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
