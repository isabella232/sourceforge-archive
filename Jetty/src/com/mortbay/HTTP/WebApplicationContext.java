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
    private Context _context;
    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     * @param webApp 
     * @param defaults 
     * @exception IOException 
     */
    WebApplicationContext(HttpServer httpServer,
                          String contextPath,
                          String webApp,
                          String defaults)
        throws IOException
    {
        super(httpServer,contextPath);

        // Get parser
        XmlParser xmlParser=new XmlParser();
        xmlParser.redirectEntity
            ("web-app_2_2.dtd",
             Resource.newSystemResource("com/mortbay/HTTP/web.dtd"));

        if (webApp.endsWith(".war"))
        {
            Resource warFile = Resource.newResource(webApp);
            webApp="jar:"+warFile+"!/";
        }
        
        Resource _webApp = Resource.newResource(webApp);
        Resource _webInf = _webApp.addPath("WEB-INF/");
        if (!_webInf.exists() || !_webInf.isDirectory())
            throw new IllegalArgumentException("No such directory: "+_webInf);
        _webAppName=_webApp.toString();
        

        // Check web.xml file
        Resource web = _webApp.addPath("WEB-INF/web.xml");
        if (!web.exists())
            throw new IllegalArgumentException("No web file: "+web);

        try
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

            // add security handler first
            addHandler(new SecurityHandler());
            
            // Set the classpath
            if (classPath.length()>0)
                super.setClassPath(classPath);
            
            // Add servlet Handler
            addHandler(new ServletHandler());
            _servletHandler = getServletHandler();
            _context=_servletHandler.getContext();
            
            // ResourcePath
            super.setResourceBase(_webApp);
            setServingResources(true);
            ResourceHandler rh = getResourceHandler();
            rh.setDirAllowed(true);
            rh.setPutAllowed(true);
            rh.setDelAllowed(true);
            
            if (defaults!=null)
            {
                Resource dftResource= Resource.newResource(defaults);
                XmlParser.Node defaultConfig =
                    xmlParser.parse(dftResource.getURL().toString());
                initialize(defaultConfig);
            }
            
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


    /* ------------------------------------------------------------ */
    private void initialize(XmlParser.Node config)
        throws ClassNotFoundException,UnavailableException
    {
        Iterator iter=config.iterator();
        while (iter.hasNext())
        {
            Object o = iter.next();
            if (!(o instanceof XmlParser.Node))
                continue;
            
            XmlParser.Node node=(XmlParser.Node)o;
            String name=node.getTag();

            if ("display-name".equals(name))
                initDisplayName(node);
            else if ("description".equals(name))
            {
                Code.warning("Not implemented: "+name);
                System.err.println(node);
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
            {
                Code.warning("Not implemented: "+name);
                System.err.println(node);
            }
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
                Code.warning("Not implemented: "+node);
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
    }

    /* ------------------------------------------------------------ */
    private void initDisplayName(XmlParser.Node node)
    {
        _name=node.toString(false);
    }
    
    /* ------------------------------------------------------------ */
    private void initContextParam(XmlParser.Node node)
    {
        String name=node.get("param-name").toString(false);
        String value=node.get("param-value").toString(false);
        Code.debug("ContextParam: ",name,"=",value);

        // XXX - This should not be in the attribute space
        setAttribute(name,value); 
    }

    /* ------------------------------------------------------------ */
    private void initServlet(XmlParser.Node node)
        throws ClassNotFoundException, UnavailableException
    {
        String name=node.get("servlet-name").toString(false);
        String className=node.get("servlet-class").toString(false);
        
        ServletHolder holder = _servletHandler.newServletHolder(className);
        holder.setServletName(name);
        
        Iterator iter= node.iterator("init-param");
        while(iter.hasNext())
        {
            XmlParser.Node paramNode=(XmlParser.Node)iter.next();
            String pname=paramNode.get("param-name").toString(false);
            String pvalue=paramNode.get("param-value").toString(false);
            holder.put(pname,pvalue);
        }

        XmlParser.Node startup = node.get("load-on-startup");
        if (startup!=null)
        {
            String s=startup.toString(false).trim().toLowerCase();
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
            // XXX - If you know what to do with this, please tell me
            Code.warning("Not Implemented: "+securityRef+" in servlet "+name);
        }
    }

    /* ------------------------------------------------------------ */
    private void initServletMapping(XmlParser.Node node)
    {
        String name=node.get("servlet-name").toString(false);
        String pathSpec=node.get("url-pattern").toString(false);

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
            int timeout = Integer.parseInt(tNode.toString(false));
            _context.setSessionTimeout(timeout);
        }
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
            String index=indexNode.toString(false);
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
            scBase.addRole(role.toString(false));
        }
        XmlParser.Node data=node.get("user-data-constraint");
        if (data!=null)
        {
            String guarantee = data.toString(false).trim().toUpperCase();
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
            String name=collection.get("web-resource-name").toString(false);
            SecurityConstraint sc = (SecurityConstraint)scBase.clone();
            sc.setName(name);
            
            Iterator iter2= collection.iterator("http-method");
            while(iter2.hasNext())
                sc.addMethod(((XmlParser.Node)iter2.next()).toString(false));

            iter2= collection.iterator("url-pattern");
            while(iter2.hasNext())
            {
                String url=
                    ((XmlParser.Node)iter2.next()).toString(false).trim();
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
            sh.setAuthMethod(method.toString(false));
        XmlParser.Node name=node.get("realm-name");
        if (name!=null)
            sh.setAuthRealm(name.toString(false));
    }
    

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String toString()
    {
        if (_name!=null)
            return _name+"@"+_webAppName;
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
