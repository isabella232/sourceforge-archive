// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.HTTP.Handler.ResourceHandler;
import com.mortbay.HTTP.Handler.SecurityHandler;
import com.mortbay.HTTP.Handler.Servlet.Context;
import com.mortbay.HTTP.Handler.Servlet.ServletHandler;
import com.mortbay.HTTP.Handler.Servlet.ServletHolder;
import com.mortbay.Util.Code;
import com.mortbay.Util.Resource;
import com.mortbay.Util.XmlParser;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Iterator;
import javax.servlet.UnavailableException;


/* ------------------------------------------------------------ */
/**
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContext extends HandlerContext
{
    /* ------------------------------------------------------------ */
    private String _name;
    private Resource _webApp;
    private String _webAppName;
    private ServletHandler _servletHandler;
    private SecurityHandler _securityHandler;
    private Context _context;
    
    
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

        // Get parser
        XmlParser xmlParser=new XmlParser();
        xmlParser.redirectEntity
            ("web-app_2_2.dtd",
             Resource.newSystemResource("com/mortbay/HTTP/web.dtd"));

        // Set dir or WAR
        Resource _webApp = Resource.newResource(webApp);
        if (!_webApp.isDirectory())
        {
            webApp="jar:"+_webApp+"!/";
            _webApp = Resource.newResource(webApp);
        }
        _webAppName=_webApp.toString();

        // add security handler first
        _securityHandler=new SecurityHandler();
        addHandler(_securityHandler);
            
        // Add servlet Handler
        _servletHandler = getServletHandler();
        _context=_servletHandler.getContext();
            
        // ResourcePath
        super.setResourceBase(_webApp);

        // Resource Handler
        setServingResources(true);
        ResourceHandler rh = getResourceHandler();
        rh.setDirAllowed(true);
        rh.setPutAllowed(false);
        rh.setDelAllowed(false);

        // Do the default configuration
        try
        {
            if (defaults!=null && defaults.length()>0)
            {
                Resource dftResource= Resource.newResource(defaults);
                XmlParser.Node defaultConfig =
                    xmlParser.parse(dftResource.getURL().toString());
                initialize(defaultConfig);
            }
        }
        catch(IOException e)
        {
            Code.warning("Parse error on "+_webAppName,e);
            throw e;
        }	
        catch(Exception e)
        {
            Code.warning("Configuration error "+_webAppName,e);
            throw new IOException("Parse error on "+_webAppName+
                                  ": "+e.toString());
        }

        
        // Do we have a WEB-INF
        Resource _webInf = _webApp.addPath("WEB-INF/");
        if (_webInf.exists() && _webInf.isDirectory())
        {
            // Look for classes directory
            Resource classes = _webApp.addPath("WEB-INF/classes/");
            String classPath="";
            if (classes.exists())
                classPath=classes.toString();

            // Look for jars
            Resource lib = _webApp.addPath("WEB-INF/lib/");
            if (lib.exists() && lib.isDirectory())
            {
                String[] files=lib.list();
                for (int f=0;files!=null && f<files.length;f++)
                {
                    Resource fn=lib.addPath(files[f]);
                    String fnlc=fn.getName().toLowerCase();
                    if (fnlc.endsWith(".jar") || fnlc.endsWith(".zip"))
                    {
                        classPath+=(classPath.length()>0?",":"")+
                            fn.toString();
                    }
                }
            }

            // Set the classpath
            if (classPath.length()>0)
                super.setClassPath(classPath);

            // do web.xml file
            Resource web = _webApp.addPath("WEB-INF/web.xml");
            if (web.exists())
            {
                try
                {
                    XmlParser.Node config = xmlParser.parse(web.getURL().toString());
                    initialize(config);
                }
                catch(IOException e)
                {
                    Code.warning("Parse error on "+_webAppName,e);
                    throw e;
                }	
                catch(Exception e)
                {
                    Code.warning("Configuration error "+_webAppName,e);
                    throw new IOException("Parse error on "+_webAppName+
                                          ": "+e.toString());
                }
            }
        }
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
                {                   
                }
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
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
                else if ("taglib".equals(name))
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
                else if ("resource-ref".equals(name))
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
                else if ("security-constraint".equals(name))
                    initSecurityConstraint(node);
                else if ("login-config".equals(name))
                    initLoginConfig(node);
                else if ("security-role".equals(name))
                    initSecurityRole(node);
                else if ("env-entry".equals(name))
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
                else if ("ejb-ref".equals(name))
                {
                    Code.warning("Not implemented: "+name);
                    System.err.println(node);
                }
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
        if (className==null)
        {
            Code.warning("Missing servlet-class in "+node);
            return;
        }
        if (name==null)
            name=className;
        
        ServletHolder holder = _servletHandler.newServletHolder(className);
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
                holder.setInitOnStartup(true);
            else
            {
                try
                {
                    int order=Integer.parseInt(s);
                    if (order>0)
                    {
                        holder.setInitOnStartup(true);
                        if (order>1)
                            Code.warning("Startup ordering not implemented");
                    }
                }
                catch(Exception e)
                {
                    Code.ignore(e);
                }
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
    private void initSecurityConstraint(XmlParser.Node node)
    {
        SecurityConstraint scBase = new SecurityConstraint();
        
        XmlParser.Node auths=node.get("auth-constraint");
        Iterator iter= auths.iterator("role-name");
        while(iter.hasNext())
        {
            XmlParser.Node role=(XmlParser.Node)iter.next();
            scBase.addRole(role.toString(false,true));
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

        iter= node.iterator("web-resource-collection");
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
            sh.setRealm(name.toString(false,true));
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
        if (_name!=null)
            return "WebApp:"+_name+"@"+_webAppName;
        return _webAppName;
    }
    
    /* ------------------------------------------------------------ */
    public void setClassPath(String classPath)
    {
        Code.warning("ClassPath should not be set for WebApplication");
        super.setClassPath(classPath);
    }
    
    /* ------------------------------------------------------------ */
    public void setResourceBase(Resource resourceBase)
    {
        Code.warning("ResourceBase should not be set for WebApplication");
        super.setResourceBase(resourceBase);
    }   
}
