package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;

/* ------------------------------------------------------------ */
/** Jetty Handler for ServletRunner config files
 *
 * @version 1.0 Fri Sep  3 1999
 * @author klaus spd &lt;klausspd@hotmail.com&gt;
 */
public class ServletRunnerHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap servletMap;
    PathMap dynamicMap=null;
    String dynamicClassPath=null;
    String dynamicLoader=null;
    Hashtable nameMap=null;
    boolean autoReloadDynamicServlets=true;
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param properties Passed to setProperties
     */
    public ServletRunnerHandler(Properties properties)
        throws IOException
    {
        setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct with servlet PathMap
     * @param servletMap Map of servlet path to ServletHolder instances
     */
    public ServletRunnerHandler(PathMap servletMap)
    {
        this.servletMap=servletMap;
    }

    /* ------------------------------------------------------------ */
    /**
     * Configure from Properties.
     * Properties treated as a PropertyTree with the following fields: <PRE>
     * CLASSPATH : ./servlets:        # CLASS Paths for dynamic servlet loading
     * AutoReloadDynamicServlets: True# Should dynamic servlets auto reload
     * Loader : className             # ServletLoader for dynamic servlets
     * Properties : ./etc/servlets.prp# handle Servlet-Runner style (and JRUN) properties
     * Mappings : ./etc/mappings.prp  # handle Servlet-Runner style (and JRUN) properties
     * </PRE>
     * 
     * @param properties Configuration.
     * @exception java.io.IOException
     */
    public void setProperties(Properties properties)
        throws IOException
    {
        servletMap=new PathMap();
        // nameMap now has to remember all Servlets from Beginn on
        nameMap = new Hashtable();
        
        PropertyTree tree=null;
        if (properties instanceof PropertyTree)
            tree = (PropertyTree)properties;
        else
            tree = new PropertyTree(properties);
        Code.debug(tree);

        // Setup dynamic servlets
        dynamicClassPath = tree.getProperty("CLASSPATH");
        dynamicLoader = tree.getProperty("Loader");
        autoReloadDynamicServlets = tree.getBoolean("AutoReloadDynamicServlets");
        if ( dynamicClassPath!=null && dynamicClassPath.length() > 0 )
            new FileJarServletLoader(dynamicClassPath);
        
        // check if we have Servlet-Runner Style Properties
        String propFilename = tree.getProperty("Properties");
        String mappFilename = tree.getProperty("Mappings");
        if ( propFilename == null || mappFilename == null )
        {
            Code.fail( "Can't read props files due to: no filenames specified" );
        }
 
        Properties servletProps = new Properties();
        Properties mappingsProps = new Properties();
        try
        {
            if ( propFilename != null )
                servletProps.load( new BufferedInputStream(
                                   new FileInputStream( propFilename )));
            if ( mappFilename != null )
                mappingsProps.load( new BufferedInputStream(
                                    new FileInputStream( mappFilename )));
        }
        catch( IOException ioe )
        {
            Code.fail( "Can't read props files due to: " + ioe );
        }

        servletMap = new PathMap();
        
        // get all different servlet names from properties file
        // expecting somthing like:
        // dump.code = com.mortbay.Servlets.Dump
        // dump.args = arg1=value1,arg2=value2
        // dump.preload = false
        String servletName;
        Vector allServletNames = new Vector();
        for( Enumeration enum = servletProps.propertyNames();
                enum.hasMoreElements(); )
        {
            String key = (String)enum.nextElement();
            try
            {
                servletName = key.substring(0, key.indexOf("."));
                if(!allServletNames.contains(servletName))
                    allServletNames.addElement(servletName);
            }
            catch( Exception ex )
            {
		Code.warning( "Malformed servlet property: " + key );
            }
        }

        // for all servletnames get the classnames,
        // initparameters and the preload option
        Hashtable servletParamTable = null;
        ServletHolder holder = null;

        String servletClass = null;
        String servletParam = null;
        String servletPreload = null;
        for( Enumeration enum = allServletNames.elements();
                enum.hasMoreElements(); )
        {
            servletName = (String)enum.nextElement();
            Code.debug( "reading props for servletName:" + servletName );

            // extracting classname
            servletClass = servletProps.getProperty( servletName + ".code" );
            if (servletClass == null)
                continue;
            Code.debug( "className:" + servletClass );
            
            // extracting servlet init parameters
            servletParam = servletProps.getProperty( servletName + ".args" );
            if ( servletParam != null )
            {
                servletParamTable = new Hashtable();
                for( StringTokenizer strT = new StringTokenizer(servletParam, ",", false);
                            strT.hasMoreTokens(); )
                {
                    String assignment = strT.nextToken();
                    int i = assignment.indexOf("=");
                    if(i > 0)
                    {
                        String key = assignment.substring(0, i).trim();
                        String value = assignment.substring(i + 1, assignment.length()).trim();
                        servletParamTable.put(key, value);
                    }
                }
            }
            
	    try {
                holder = new ServletHolder( servletName, servletClass, servletParamTable );
                Code.debug( "created holder:" + holder + " for servletName:" + servletName );
	    }
	    catch(ClassNotFoundException e)
	    {
		Code.warning(e);
	    }
            
            // we remember all servlets even if they don't
            // have a mapping (servletMap) they could be invoked
            // dynamically so we keep the initargs and might do a preload
            nameMap.put( servletName, holder );
            
            // preload servlet if asked for
            servletPreload = servletProps.getProperty( servletName + ".preload" );
            holder.setInitialize( servletPreload != null &&
                                    servletPreload.equalsIgnoreCase( "true" ) );
        }

        // all servlets that have a mapping in the mapping file
        // will be added to servletMap
        String path;
        for( Enumeration mappings = mappingsProps.propertyNames();
                mappings.hasMoreElements(); )
        {
            path = (String)mappings.nextElement();
            servletName = mappingsProps.getProperty(path).trim();
            Code.debug( "reading mapping:" + path + " for servletName:" + servletName );
            if ( path.startsWith( "." ) )
                path = "*" + path;
            holder = (ServletHolder)nameMap.get( servletName );
            // check if we know that servlet from the propFile
            if ( holder == null )
            {
                Code.debug( "holder is null for servletName:" + servletName );
                // we don't know a servlet with this name
                // from the properties file
                if ( servletName.equalsIgnoreCase( "invoker" ) )
                {
                    // but it is a mapping for the servlet invoker
                    // we are very strikt with the invoker mappings
                    // otherwise it gets to complicate
                    // a prefix mapping should look like this:
                    // /xxx/ -> URI: /xxx/ + servlet classname + pathinfo
                    // a suffix mapping should look like this:
                    // .yyy -> URI: /xyz/ + servlet classname + .yyy
                    if ( path.indexOf( "*" ) > 0 )
                        Code.fail( "Malformed invoker mapping:" + path );
                    else if ( ( path.startsWith( "/" ) && path.endsWith( "/" ) ) ||
                              ( path.startsWith( "*" ) && path.length() > 1 ) )
                    {
                        if ( dynamicMap == null )
                            dynamicMap = new PathMap();
                        dynamicMap.put( path, "" );
                    }
                    continue;
                }

                // we assume this mapping is for a servlet 
                // that can be found in the classpath
                // servletName = servletClass and no parameters
                try {    
                    holder = new ServletHolder( servletName, servletName );
                    nameMap.put( servletName, holder );
	        }
	        catch(ClassNotFoundException e)
	        {
		    Code.warning(e);
	        }
            }
          
            if ( holder != null )
            {
                // the class for this servlet is loaded
                // so we can remember it's name and path info
                Code.debug( "addServlet: " + servletName + " at " + path );
                servletMap.put( path, holder );
            }
        }
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws Exception
    {
        String address = request.getResourcePath();
        String pathSpec=servletMap.matchSpec(address);

        // try a known servlet
        if (pathSpec != null)
        {
            ServletHolder holder =
                (ServletHolder)servletMap.get(pathSpec);
            
            Code.debug("Pass request to servlet " + holder);

            request.setServletPath(pathSpec);
            
            // service request
            holder.service(request,response);
            return;
        }

        if (dynamicMap==null)
            return;
        
        if (Code.verbose())
            Code.debug("Looking for "+address +" in "+dynamicMap);
        
        // try a dynamic servlet
        pathSpec=dynamicMap.matchSpec(address);
        if ( pathSpec != null )
        {
            // adress is matching the servlet invoker
            if ( pathSpec.length() >= address.length() )
                return;

            // invoker path found
            // we are very strikt with the invoker mappings
            // otherwise it gets to complicate
            // a prefix mapping should look like this:
            // /xxx/ -> URI: /xxx/ + servlet-classname + "/" + pathinfo
            // a suffix mapping should look like this:
            // .yyy -> URI: /xyz/ + servlet-classname + .yyy
            // now extract servletname and find additional servlet path
            // since i am not experienced enough with using all the PathMap
            // functionality this is very ugly code
            String servletClassName = new String( address );
            String path = new String( pathSpec );
            int s;
            if ( pathSpec.startsWith( "*" ) )
            {
                String suffix=path.substring( 1 );
                // let's take the suffix of
                servletClassName = servletClassName.substring( 0,
                                                    servletClassName.lastIndexOf( suffix ) );
                if ( (s = servletClassName.lastIndexOf( "/" )) >= 0 )
                {
                    // we remember the ServletPath - from the beginning
                    // up to the end of the suffix
                    // so we keep additional pathinfo although there
                    // should be non (in this version)
                    path = servletClassName + suffix;
                    servletClassName = servletClassName.substring( s + 1 );
                }
                else
                {
                    // there is not even one beginning slash in this URI
                    servletClassName = "";
                }
            }
            else
            {
                // path should end with a slash
                // but just in case.
                // a more generic way would be to
                // extend the PathMap class with the
                // methods getMatchingPrefix and
                // getMatchingSuffix
                switch ( path.charAt( path.length() - 1 ) )
                {
                    case '|':
                    case '%':
                    case '$':
                        path = path.substring( 0, path.length() - 1 );
                        break;
                    default:
                        break;
                }
                servletClassName = servletClassName.substring( path.length() );
                if ( (s = servletClassName.indexOf( "/" )) > 0 )
                {
                    servletClassName = servletClassName.substring( 0, s );
                }
                else if ( s == 0 )
                    servletClassName = "";
                path = path + servletClassName;
            }
                
            if ( servletClassName.length() <= 0 )
                return;
            ServletHolder holder;
            // check if we know this servlet already
            // from config files or had been invoked before
            holder = (ServletHolder)nameMap.get( servletClassName );
            if ( holder != null )
            {
                request.setServletPath(path);
                holder.service(request,response);
                return;
            }
            try 
            {
                // there should be a better way to ask the
                // ServeltHolder kindly if it can get a servlet class
                holder = new ServletHolder( dynamicLoader,
                                                        servletClassName,
                                                        servletClassName,
                                                        dynamicClassPath,
                                                        null);
            } catch ( Exception e )
            { 
                Code.warning( "tried to invoke servlet:\"" + servletClassName +
                                "\" exception:" + e );
                return;
            }
            // tell the new holder about his server->ServletContext
            holder.setServer(httpServer);
            holder.setAutoReload(autoReloadDynamicServlets);
            nameMap.put( servletClassName, holder );
            request.setServletPath(path);
            holder.service(request,response);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void setServer(HttpServer server)
         throws Exception
    {
        Enumeration h = nameMap.elements();
        while (h.hasMoreElements())
        {
            ServletHolder holder = (ServletHolder)h.nextElement();
            holder.setServer(server);
        }
        super.setServer(server);
    }
 
    /* ----------------------------------------------------------------- */
    public Enumeration servletNames()
    {
        if (nameMap==null)
        {
            nameMap=new Hashtable();
            Enumeration e = servletMap.elements();
            while (e.hasMoreElements())
            {
                ServletHolder holder = (ServletHolder)e.nextElement();
                nameMap.put(holder.toString(),holder);
            }
        }
        return nameMap.keys();
    }
        
    /* ----------------------------------------------------------------- */
    public Servlet servlet(String name)
    {
        try{
            servletNames();
            ServletHolder holder = (ServletHolder)nameMap.get(name);
            if (holder==null)
                return null;
            return holder.getServlet();
        }
        catch(Exception e){
            Code.warning(e);
        }
        return null;    
    }
}


