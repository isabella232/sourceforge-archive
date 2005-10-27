//========================================================================
//$Id$
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.plugin;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.NotFoundHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.JettyWebXmlConfiguration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebAppClassLoader;

/**
 * 
 * 
 * Mojo to run Jetty in-situ from a Maven webapp project without necessitating
 * building an actual war or exploded war.
 * 
 * @goal run
 * @description Runs jetty6 directly from a maven project
 *
 */
public class JettyMojo extends AbstractMojo 
{
    
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * The location of the web.xml file. If not
     * set then it is assumed it is in ${basedir}/src/main/webapp/WEB-INF
     * 
     * @parameter expression="${maven.war.webxml}"
     */
    private String webXml;
    
    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;
    
    /**
     * Root directory for all html/jsp etc files
     *
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private File webAppSourceDirectory;
    
    
    /**
     * List of connectors to use. If none are configured
     * then we use a single SelectChannelConnector at port 8080
     * 
     * @parameter 
     */
    private Connector[] connectors;
    
    
    /**
     * List of other contexts to set up. Optional.
     * @parameter
     */
    private ContextHandler[] contextHandlers;
    
    
    /**
     * List of security realms to set up. Optional.
     * @parameter
     */
    private UserRealm[] userRealms;
    
    
    /**
     * The context path for the webapp. Defaults to thes
     * name of the webapp's artifact.
     * 
     * @parameter expression="/${project.artifactId}"
     * @required
     */
    private String contextPath;
    
    
    
    /**
     * The interval in seconds to scan the webapp for changes 
     * and restart the context if necessary. Disabled by default.
     * 
     * @parameter expression="0"
     * @required
     */
    private int scanIntervalSeconds;
    
    
    /**
     * The webapp
     */
    private WebAppContext webAppHandler;
   
    public static SelectChannelConnector DEFAULT_CONNECTOR = new SelectChannelConnector();
    public static int DEFAULT_PORT = 8080;
    public static long DEFAULT_MAX_IDLE_TIME = 30000L;
  

    
    public MavenProject getProject()
    {
        return project;
    }

    public void setProject(MavenProject project)
    {
        this.project = project;
    }
    
    public String getWebXml ()
    {
        return this.webXml;
    }
    public void setWebXml (String webXml)
    {
        this.webXml = webXml;
    }
    
    public File getClassesDirectory ()
    {
        return this.classesDirectory;
    }
    
    public void setClassesDirectory (File classesDirectory)
    {
        this.classesDirectory = classesDirectory;
    }
    
    public File getWebAppSourceDirectory ()
    {
        return this.webAppSourceDirectory;
    }
    public void setWebAppSourceDirectory(File webAppSourceDir)
    {
        this.webAppSourceDirectory = webAppSourceDir;
    }


    /**
	 * @return Returns the connectors.
	 */
	public Connector[] getConnectors()
	{
		return connectors;
	}

	/**
	 * @param connectors The connectors to set.
	 */
	public void setConnectors(Connector[] connectors)
	{
		this.connectors = connectors;
	}
    
    /**
	 * @return Returns the contextPath.
	 */
    public String getContextPath() 
    {
        return contextPath;
    }

    /**
     * @param contextPath The contextPath to set.
     */
    public void setContextPath(String contextPath) 
    {
        this.contextPath = contextPath;
    }

    
    /**
	 * @return Returns the contextHandlers.
	 */
	public ContextHandler[] getContextHandlers()
	{
		return this.contextHandlers;
	}

	/**
	 * @param contextHandlers The contextHandlers to set.
	 */
	public void setContextHandlers(ContextHandler[] contextHandlers)
	{
		this.contextHandlers = contextHandlers;
	}

	/**
	 * @return Returns the scanIntervalSeconds.
	 */
	public int getScanIntervalSeconds()
	{
		return this.scanIntervalSeconds;
	}

	/**
	 * @param scanIntervalSeconds The scanIntervalSeconds to set.
	 */
	public void setScanIntervalSeconds(int scanIntervalSeconds)
	{
		this.scanIntervalSeconds = scanIntervalSeconds;
	}

	/**
	 * @return Returns the realms.
	 */
	public UserRealm[] getUserRealms()
	{
		return this.userRealms;
	}

	/**
	 * @param realms The realms to set.
	 */
	
	public void setUserRealms(UserRealm[] realms)
	{
		this.userRealms = realms;
	}
	
    
	
	public Handler getWebApplication ()
	{
		if (this.webAppHandler==null)
			this.webAppHandler = new WebAppContext();
		
		return this.webAppHandler;
	}

    /** 
     * Execute the Mojo
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Configuring Jetty for project: " + getProject().getName());
        
        //check the location of the static content/jsps etc
        try
        {
            if ((getWebAppSourceDirectory() == null) || !getWebAppSourceDirectory().exists())
                throw new MojoExecutionException ("Webapp source directory "
                                                 + (getWebAppSourceDirectory()==null?"null":getWebAppSourceDirectory().getCanonicalPath())
                                                 + " does not exist");
            else
                getLog().info("Webapp source directory is: "+getWebAppSourceDirectory().getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Webapp source directory does not exist", e);
        }
        
        File webXmlFile;        
        //get the web.xml file if one has been provided, otherwise assume it is 
        //in the webapp src directory
        if (getWebXml() == null || (getWebXml().trim().equals("")))
            webXmlFile = new File (new File(getWebAppSourceDirectory(), "WEB-INF"), "web.xml");
        else
            webXmlFile = new File(getWebXml());
        
        try
        {
            if (!webXmlFile.exists())
                throw new MojoExecutionException("web.xml does not exist at location "+webXmlFile.getCanonicalPath());
            else
                getLog().info("web.xml file located at: "+webXmlFile.getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException ("web.xml does not exist", e);
        }
        
        //check the classes to form a classpath with
        try
        {
            if (getClassesDirectory() == null)
                throw new MojoExecutionException ("Location of classesDirectory is not set");
            if (!getClassesDirectory().exists())
                throw new MojoExecutionException("Location "+getClassesDirectory().getCanonicalPath()+" classesDirectory does not exist");
            
            getLog().info("Classes located at: "+getClassesDirectory().getCanonicalPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Location of classesDirectory does not exist");
        }
  
        
        startJetty (webXmlFile);
    }

    
    
    
    /** Configure and start a Jetty server.
     * 
     * @param webXmlFile location of web.xml file in the Maven project
     * @throws MojoExecutionException
     */
    public void startJetty (final File webXmlFile)
    throws MojoExecutionException
    {
        
        try
        {
            getLog().info("Starting Jetty Server ...");
            Server server = new Server();
            server.setStopAtShutdown(true);
            
            //if the user hasn't configured their project's pom to use a different set of connectors,
            //use the default
            if (getConnectors()==null || getConnectors().length==0)
            {                
            	getLog().info("No connectors configured, using defaults: "+DEFAULT_CONNECTOR.getClass().getName()+" listening on "+DEFAULT_PORT+" with maxIdleTime "+DEFAULT_MAX_IDLE_TIME);
                DEFAULT_CONNECTOR.setPort(DEFAULT_PORT);
                DEFAULT_CONNECTOR.setMaxIdleTime(DEFAULT_MAX_IDLE_TIME);
                setConnectors (new Connector[]{DEFAULT_CONNECTOR});
            }          
            server.setConnectors(getConnectors());
            
            WebAppContext webapp = (WebAppContext)configureWebApplication(webXmlFile);
            webapp.setServer(server);
            
            
            //include any other ContextHandlers that the user has configured in their project's pom
            Handler[] handlers = new Handler[(getContextHandlers()!=null?getContextHandlers().length:0)+2];   
            handlers[0] = webapp;
            for (int i=0; (getContextHandlers()!=null && i < getContextHandlers().length); i++)
            	handlers[1+i] = getContextHandlers()[i];

            handlers[handlers.length-1]=new NotFoundHandler();   
            ((NotFoundHandler)handlers[handlers.length-1]).setServer(server);
            server.setHandlers(handlers);
            
            //set up security realms
            for (int i=0;(getUserRealms()!=null)&&i<getUserRealms().length;i++)
            getLog().debug(getUserRealms()[i].getClass().getName()+ ": "+getUserRealms()[i].toString());
            
            server.setUserRealms(getUserRealms());
            
            //start Jetty
            server.start();
            
            //start the scanner thread (if necessary) on the main webapp
            ArrayList scanList = new ArrayList ();
        	scanList.add(webXmlFile);
        	scanList.add(getProject().getFile());
        	scanList.add(getClassesDirectory());
        	ArrayList listeners = new ArrayList();
        	listeners.add(new Scanner.Listener()
        	{
        		public void changeDetected ()
        		{
        			try
        			{
        				getLog().info("Stopping webapp ...");
        				getWebApplication().stop();
        				getLog().info("Reconfiguring webapp ...");
        				configureWebApplication(webXmlFile);
        				getLog().info("Restarting webapp ...");
        				getWebApplication().start();
        				getLog().info("Restart completed.");
        			}
        			catch (Exception e)
        			{
        				getLog().error("Error reconfiguring/restarting webapp after change in watched files", e);
        			}
        		}
        	});
            startScanner(getScanIntervalSeconds(), scanList, listeners);
            
            //keep the thread going
            server.getThreadPool().join();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException ("Failure",e);
        }
        finally 
        {
            getLog().info("Jetty server exiting.");
        }
    }



    
    /**
     * Run a scanner thread on the given list of files and directories,
     * calling stop/start on the given list of LifeCycle objects if any
     * of the watched files change.
     * 
     * @param scanList the list of files and directories to watch
     * @param scanListeners list of listeners for the watched files
     */
    private void startScanner (int scanInterval, List scanList, List scanListeners)
    {
    	//check if scanning is enabled
    	if (scanInterval <= 0)
    		return;
    	
    	Scanner scanner = new Scanner ();
    	scanner.setScanInterval(scanInterval);
    	scanner.setRoots(scanList);
    	
    	scanner.setListeners(scanListeners);
    	scanner.setLog(getLog());
    	getLog().info("Starting scanner at interval of "+scanInterval+" seconds.");
    	scanner.start();
    }
    
    
    
    private Handler configureWebApplication (File webXmlFile)
    throws Exception
    { 	  
        //make a webapp handler and set the context
        WebAppContext webapp = (WebAppContext)getWebApplication();
        
        String contextPath = getContextPath();
        if (!contextPath.startsWith("/"))
            contextPath = "/"+contextPath;
        getLog().info("Context path = "+contextPath);
        webapp.setContextPath(contextPath);
        getLog().info("Webapp directory = "+getWebAppSourceDirectory().getCanonicalPath());
        webapp.setWar(getWebAppSourceDirectory().getCanonicalPath());
        
        //do special configuration of classpaths and web.xml etc in Jetty startup
        JettyMavenConfiguration mavenConfig = new JettyMavenConfiguration();
        mavenConfig.setClassPathConfiguration (getWebAppSourceDirectory(), getClassesDirectory(), getTldFiles(), getLibFiles());
        mavenConfig.setWebXml (webXmlFile);
        mavenConfig.setLog (getLog());           
        webapp.setConfigurations(new Configuration[]{mavenConfig, new JettyWebXmlConfiguration()});
        return webapp;
    }
    
    
    private List getLibFiles ()
    {
    	List libFiles = new ArrayList();
    	for ( Iterator iter = project.getArtifacts().iterator(); iter.hasNext(); )
    	{
    		Artifact artifact = (Artifact) iter.next();
    		
    		// Include runtime and compile time libraries
    		if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) &&
    				!Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
    		{
    			String type = artifact.getType();
    			if ( "jar".equals( type ) )
    			{
    				libFiles.add(artifact.getFile());
    			}
    			else
    			{
    				getLog().debug( "Skipping artifact of type " + type + " for WEB-INF" );
    			}
    		}
    	}
    	return libFiles;	
    }
    
    private List getTldFiles ()
    {
    	List tldFiles = new ArrayList();
    	for ( Iterator iter = getProject().getArtifacts().iterator(); iter.hasNext(); )
    	{
    		Artifact artifact = (Artifact) iter.next();
    		
    		// Include runtime and compile time libraries
    		if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) &&
    				!Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
    		{
    			String type = artifact.getType();
    			if ( "tld".equals( type ) )
    			{
    				tldFiles.add(artifact.getFile());
    			}
    			else
    			{
    				getLog().debug( "Skipping artifact of type " + type + " for WEB-INF" );
    			}
    		}
    	}
    	return tldFiles;
    }

    
    
}
