// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------
package com.mortbay.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;


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
    protected URL _url;
    protected String _urlString;
    protected URLConnection _connection;
    protected InputStream _in=null;

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

        String urls=url.toExternalForm();
        if( urls.startsWith( "file:"))
        {
            URLConnection connection=url.openConnection();
            Permission perm = connection.getPermission();
            if (perm instanceof java.io.FilePermission)
            {
                File file =new File(perm.getName());
		System.err.println(file.getCanonicalPath());
                if (urls.indexOf("..")>=0)
                {
                    file=new File(file.getCanonicalPath());
                    url=file.toURL();
                }
                if (file.isDirectory() && !url.getFile().endsWith("/"))
                    url=new URL(url.toString()+"/");
                return new FileResource(url,connection,file);
            }
            Code.warning("File resource without FilePermission:"+url);
        }
        else if( url.toExternalForm().startsWith( "jar:file:"))
        {
            return new JarFileResource(url);
        }
        else if( url.toExternalForm().startsWith( "jar:"))
        {
            return new JarResource(url);
        }

        return new Resource(url,null);
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
            if(resource.startsWith("."+File.separator))
            {
                // It's a local file.
                File file=new File(resource);
		System.err.println(file.getCanonicalPath());
                file =new File(file.getCanonicalPath());
                url=file.toURL();
            }
            else if (!resource.startsWith("ftp:") &&
                     !resource.startsWith("file:") &&
                     !resource.startsWith("jar:"))
            {
                // Nope - try it as a file
                url = new File(resource).toURL();
		System.err.println(new File(resource).getCanonicalPath());
            }
            else
                Code.warning(e);
        }

        return newResource(url);
    }

    /* ------------------------------------------------------------ */
    /** Construct a resource from a string.
     * If the string is not a URL, it is treated as an absolute or
     * relative file path.
     * @param resource.
     * @return
     */
    public static Resource newSystemResource(String resource)
        throws IOException
    {
        URL url=null;
        // Try to format as a URL?
        url = Resource.class.getClassLoader().getResource(resource);
        if (url==null)
            return null;
        return newResource(url);
    }


    /* ------------------------------------------------------------ */
    protected Resource(URL url, URLConnection connection)
    {
        _url = url;
        _urlString=_url.toString();
        _connection=connection;
    }

    /* ------------------------------------------------------------ */
    protected boolean checkConnection()
    {
        if (_connection==null)
        {
            synchronized(this)
            {
                // XXX - vulnerable to double null check sync problem.
                // but should not be a problem for this class.
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
    /** Release any resources held by the resource.
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
     * Returns true if the respresened resource exists.
     */
    public boolean exists()
    {
        try
        {
            if (checkConnection() && _in==null )
            {
                synchronized(this)
                {
                    if ( _in==null )
                        _in = _connection.getInputStream();
                }
            }
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
        return exists() && _url.toString().endsWith("/");
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
        return _url.toExternalForm();
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns an input stream to the resource
     */
    public InputStream getInputStream()
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
    public String[] list()
    {
        return null;
    }


    /* ------------------------------------------------------------ */
    /**
     * Returns the resource contained inside the current resource with the
     * given name
     */
    public Resource addPath(String path)
        throws IOException,MalformedURLException
    {
        if (path==null)
            return null;

        path = path.replace('\\','/');

        // XXX - need to check for ../ which might take us
        // out-side of resourcebase - or at least make sure we
        // never see that here.
        while (path.indexOf("..")>=0)
        {
            Code.notImplemented();
        }

        String resourceBase = _url.toExternalForm();
        if( path.startsWith( "./"))
            path = path.substring( 2);
        if( resourceBase.endsWith( "/"))
            if( path.startsWith( "/"))
                path = resourceBase + path.substring( 1);
            else
                path = resourceBase + path;
        else
            if( path.startsWith( "/"))
                path = resourceBase + path;
            else
                path = resourceBase + "/" + path;
        return newResource(new URL(path));
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String toString()
    {
        return _urlString;
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
}
