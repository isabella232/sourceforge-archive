package com.mortbay.Util;

import java.io.*;
import java.util.*;
import java.net.*;
import com.mortbay.Util.*;
import java.security.*;


/* ------------------------------------------------------------ */
/**
 * Class that represents a resource accessible through the file or URL 
 * <p>
 *
 * @version $Id$
 * @author Nuno Preguiça 
 * @author Greg Wilkins (gregw)
 */
public class Resource 
{
    private URL _url;
    private URLConnection _connection;
    private InputStream _in=null;

    /* ------------------------------------------------------------ */
    /** 
     * @param url 
     * @return 
     */
    public static Resource newResource(URL url)
	throws IOException
    {
	if (url==null)
	    return null;
	
	URLConnection connection=url.openConnection();
	if( url.toExternalForm().startsWith( "file:"))
	{
	    Permission perm = connection.getPermission();
	    if (perm instanceof java.io.FilePermission)
	    {
		File file =new File(perm.getName());
		if (file.isDirectory() && !url.getFile().endsWith("/"))
		    url=new URL(url.toString()+"/");
		return new FileResource(url,connection,file);
	    }
	}
	return new Resource(url,connection);
    }

    /* ------------------------------------------------------------ */
    /** Construct a resource from a string.
     * If the string is not a URL, it is treated as an absolute or
     * relative file path.
     * @param resource. 
     * @return 
     */
    public static Resource newResource(String resource)
	throws MalformedURLException, IOException
    {
	URL url=null;
	try
	{
	    // Try to format as a URL?
	    url = new URL(resource);
	}
	catch(MalformedURLException e)
	{
	    // Nope - try it as a file
	    url = new File(resource).toURL();
	}
	
	return newResource(url);
    }
    
    /* ------------------------------------------------------------ */
    /** Construct a resource for extracting files from a compressed resource.
     * Creates a temporary file for compressed files inside not file resources
     * @param resource
     * @return
     */
    public static Resource newCompressedResource( Resource resource)
        throws IOException
    {
        if( resource.getFile() == null) {
            InputStream is = resource.getInputStream();
            File file = File.createTempFile( "uncompressed", ".jar");
            FileOutputStream os = new FileOutputStream( file);
            byte[] arr = new byte[1024];
            int len;
            while( (len = is.read( arr, 0, 1024)) > 0) {
                os.write( arr, 0, len);
            }
            os.close();
            URL url = new URL( "jar:" + file.toURL().toExternalForm() + "!/");
            URLConnection connection = url.openConnection();
            return new CompressedResource( url, connection, file);
        } else {
            URL url = new URL( "jar:" + resource.getURL().toExternalForm() + "!/");
            URLConnection connection = url.openConnection();
            return new Resource( url, connection);
        }
    }

    
    /* ------------------------------------------------------------ */
    private Resource(URL url, URLConnection connection)
    {
	_url = url;
	_connection=connection;
    }

    /* ------------------------------------------------------------ */
    private boolean checkConnection()
    {
	if (_connection==null)
	{
	    try{
		_connection=_url.openConnection();
	    }
	    catch(IOException e)
	    {
		Code.ignore(e);
	    }
	}
	return _connection!=null;
    }

    /* ------------------------------------------------------------ */
    protected void finalize()
    {
	release();
    }
    
    /* ------------------------------------------------------------ */
    /** Release any resources held by the resource 
     */
    public void release()
    {
	if (_in!=null)
	{
	    try{_in.close();}catch(IOException e){Code.ignore(e);}
	    _in=null;
	}
	
	if (_connection!=null)
	{
	    _connection=null;
	}
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists()
    {
	try
	{
	    if (checkConnection())
		_in = _connection.getInputStream();
	}
	catch (IOException e)
	{
	    Code.ignore(e);
	}
	return _in!=null;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource is a container/directory.
     * If the resource is not a file, resources ending with "/" are
     * considered directories.
     */
    public boolean isDirectory()
    {
	return _url.getFile().endsWith("/");
    }

    
    /* ------------------------------------------------------------ */
    /**
     * Returns the last modified time
     */
    public long lastModified()
    {
	if (checkConnection())
	    return _connection.getLastModified();
	return -1;
    }


    /* ------------------------------------------------------------ */
    /**
     * Return the length of the resource
     */
    public long length()
    {
	if (checkConnection())  
	    return _connection.getContentLength();
	return -1;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns an URL representing the given resource
     */
    public URL getURL()
    {
	return _url;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns an File representing the given resource or NULL if this
     * is not possible.
     */
    public File getFile()
    {
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the name of the resource
     */
    public String getName()
    {
	return _url.getFile();
    }
	
    /* ------------------------------------------------------------ */
    /**
     * Returns an input stream to the resource
     */
    public synchronized java.io.InputStream getInputStream()
	throws java.io.IOException
    {
	if (!checkConnection())  
	    throw new IOException( "Invalid resource");

	if( _in != null)
	{
	    InputStream in = _in;
	    _in=null;
	    return in;
	}

	return _connection.getInputStream();
    }

    
    /* ------------------------------------------------------------ */
    /**
     * Returns an output stream to the resource
     */
    public OutputStream getOutputStream()
	throws java.io.IOException, SecurityException
    {
	throw new IOException( "Output not supported");
    }

    /* ------------------------------------------------------------ */
    /**
     * Deletes the given resource
     */
    public boolean delete()
	throws SecurityException
    {
	throw new SecurityException( "Delete not supported");
    }

    /* ------------------------------------------------------------ */
    /**
     * Rename the given resource
     */
    public boolean renameTo( Resource dest)
	throws SecurityException
    {
	throw new SecurityException( "RenameTo not supported");
    }

    
    /* ------------------------------------------------------------ */
    /**
     * Returns a list of resource names contained in the given resource
     */
    public synchronized String[] list()
    {
	return null;
    }

    
    /* ------------------------------------------------------------ */
    /**
     * Returns the resource contained inside the current resource with the
     * given name
     */
    public Resource relative(String name)
	throws IOException,MalformedURLException
    {
// XXX - it seems not to work with files, jar files, ...
//	return newResource(new URL(_url,name));
        String resourceBase = _url.toExternalForm();
        if( name.startsWith( "./"))
            name = name.substring( 2);
        if( resourceBase.endsWith( "/"))
            if( name.startsWith( "/"))
                name = resourceBase + name.substring( 1);
            else
                name = resourceBase + name;
        else
            if( name.startsWith( "/"))
                name = resourceBase + name;
            else
                name = resourceBase + "/" + name;
        return newResource(new URL(name));
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String toString()
    {
	return _url.toString();
    }
    
	
    /* ------------------------------------------------------------ */
    /** 
     * @param o
     * @return 
     */
    public boolean equals( Object o)
    {
	return o instanceof Resource &&
	    _url.equals(((Resource)o)._url);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class FileResource extends Resource
    {
	File _file;
	
	/* -------------------------------------------------------- */
	private FileResource(URL url, URLConnection connection, File file)
	{
	    super(url,connection);
	    _file=file;
	}


	/* -------------------------------------------------------- */
	/**
	 * Returns true if the respresenetd resource exists.
	 */
	public boolean exists()
	{
	    return _file.exists();
	}
	
	/* -------------------------------------------------------- */
	/**
	 * Returns the last modified time
	 */
	public long lastModified()
	{
	    return _file.lastModified();
	}

	/* -------------------------------------------------------- */
	/**
	 * Returns true if the respresenetd resource is a container/directory.
	 */
	public boolean isDirectory()
	{
	    return _file.isDirectory();
	}

	/* --------------------------------------------------------- */
	/**
	 * Return the length of the resource
	 */
	public long length()
	{
	    return _file.length();
	}
	

	/* --------------------------------------------------------- */
	/**
	 * Returns the name of the resource
	 */
	public String getName()
	{
	    return _file.getName();
	}
	
	/* ------------------------------------------------------------ */
	/**
	 * Returns an File representing the given resource or NULL if this
	 * is not possible.
	 */
	public File getFile()
	{
	    return _file;
	}
	
	/* --------------------------------------------------------- */
	/**
	 * Returns an input stream to the resource
	 */
	public InputStream getInputStream() throws IOException
	{
	    return new FileInputStream(_file);
	}
	
	/* --------------------------------------------------------- */
	/**
	 * Returns an output stream to the resource
	 */
	public OutputStream getOutputStream()
	    throws java.io.IOException, SecurityException
	{
	    return new FileOutputStream(_file);
	}
	
	/* --------------------------------------------------------- */
	/**
	 * Deletes the given resource
	 */
	public boolean delete()
	    throws SecurityException
	{
	    return _file.delete();
	}

	/* --------------------------------------------------------- */
	/**
	 * Rename the given resource
	 */
	public boolean renameTo( Resource dest)
	    throws SecurityException
	{
	    if( dest instanceof FileResource)
		return _file.renameTo( ((FileResource)dest)._file);
	    else
		return false;
	}

	/* --------------------------------------------------------- */
	/**
	 * Returns a list of resources contained in the given resource
	 */
	public String[] list()
	{
	    return _file.list();
	}
	
	/* ------------------------------------------------------------ */
	/** 
	 * @param o
	 * @return 
	 */
 	public boolean equals( Object o)
	{
	    return o instanceof FileResource &&
		_file.equals(((FileResource)o)._file);
	}
    }

    private static class CompressedResource extends Resource
    {
	File _file;
	
	/* -------------------------------------------------------- */
	private CompressedResource(URL url, URLConnection connection, File file)
	{
	    super(url,connection);
	    _file=file;
	}

        protected void finalize()
        {
    	    super.finalize();
            _file.delete();
        }
    }

}



