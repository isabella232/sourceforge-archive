package com.mortbay.Jetty;

import java.io.*;
import java.sql.*;
import java.net.*;
//import com.sun.java.util.collections.*; XXX-JDK1.1
import java.util.*; //XXX-JDK1.2
import java.io.InputStream;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;


public class Server extends HttpServer
{

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration 
     */
    public Server(String configuration)
	throws IOException
    {
	this(Resource.newResource(configuration).getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration 
     */
    public Server(Resource configuration)
	throws IOException
    {
	this(configuration.getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration 
     */
    public Server(URL configuration)
	throws IOException
    {
	try
	{
	    XmlConfiguration config=new XmlConfiguration(configuration);
	    config.configure(this);
	}
	catch(org.xml.sax.SAXException e)
	{
	    Code.warning(e);
	    throw new IOException("Jetty configuration problem: "+e);
	}
	catch(NoSuchMethodException e)
	{
	    Code.warning(e);
	    throw new IOException("Jetty configuration problem: "+e);
	}
	catch(java.lang.reflect.InvocationTargetException e)
	{
	    Code.warning(e);
	    throw new IOException("Jetty configuration problem: "+e);
	}
	catch(ClassNotFoundException e)
	{
	    Code.warning(e);
	    throw new IOException("Jetty configuration problem: "+e);
	}
	
    }
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
	String[] dftConfig={"etc/jetty.xml"};
	
	if (arg.length==0)
	{
	    System.err.println("Using default configuration: etc/jetty.xml");
	    arg=dftConfig;
	}
	
	try
	{
	    for (int i=0;i<arg.length;i++)
		new Server(arg[i]).start();
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }
}




