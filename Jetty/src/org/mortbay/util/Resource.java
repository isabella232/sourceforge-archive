// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------
package org.mortbay.util;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.lang.reflect.Method;

/* ------------------------------------------------------------ */
/** Abstract resource class.
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
    public static Resource newResource(URL url)
        throws IOException
    {
        if (url==null)
            return null;

        String urls=url.toExternalForm();
        if( urls.startsWith( "file:"))
        {
            try
            {
                URLConnection connection=url.openConnection();
                Permission perm = connection.getPermission();
                if (perm instanceof java.io.FilePermission)
                {
                    File file =new File(perm.getName());
                    FileResource fileResource= new FileResource(url,connection,file);
                    if (fileResource.getAlias()!=null)
                        return fileResource.getAlias();
                    return fileResource;
                }
                Code.warning("File resource without FilePermission:"+url);
            }
            catch(Exception e)
            {
                Code.debug(e);
                return new BadResource(url,e.toString());
            }
        }
        else if( urls.startsWith( "jar:file:"))
        {
            return new JarFileResource(url);
        }
        else if( urls.startsWith( "jar:"))
        {
            return new JarResource(url);
        }

        return new Resource(url,null);
    }

    /* ------------------------------------------------------------ */
    /** Construct a resource from a string.
     * @param resource A URL or filename.
     * @return A Resource object.
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
            if(!resource.startsWith("ftp:") &&
               !resource.startsWith("file:") &&
               !resource.startsWith("jar:"))
            {
                try
                {
                    // It's a file.
                    if (resource.startsWith("./"))
                        resource=resource.substring(2);
                    
                    File file=new File(resource);
                    if (resource.indexOf("..")>=0)
                        file=new File(file.getCanonicalPath());
                    url=file.toURL();
                    URLConnection connection=url.openConnection();
                    FileResource fileResource= new FileResource(url,connection,file);
                    if (fileResource.getAlias()!=null)
                        return fileResource.getAlias();
                    return fileResource;
                }
                catch(Exception e2)
                {
                    Code.debug(e2);
                    throw e;
                }
            }
            else
            {
                Code.warning("Bad Resource: "+resource);
                throw e;
            }
        }

        String nurl=url.toString();
        if (nurl.length()>0 &&
            nurl.charAt(nurl.length()-1)!=
            resource.charAt(resource.length()-1))
        {
            if ((nurl.charAt(nurl.length()-1)!='/' ||
                 nurl.charAt(nurl.length()-2)!=resource.charAt(resource.length()-1))
                &&
                (resource.charAt(resource.length()-1)!='/' ||
                 resource.charAt(resource.length()-2)!=nurl.charAt(nurl.length()-1)
                 ))
            {
                return new BadResource(url,"Trailing special characters stripped by URL in "+resource);
            }
        }
        return newResource(url);
    }

    /* ------------------------------------------------------------ */
    /** Construct a system resource from a string.
     * The resource is tried as classloader resource before being
     * treated as a normal resource.
     */
    public static Resource newSystemResource(String resource)
        throws IOException
    {
        URL url=null;
        // Try to format as a URL?
        ClassLoader
            loader=Thread.currentThread().getContextClassLoader();
        if (loader!=null)
        {
            url=loader.getResource(resource);
            if (url==null && resource.startsWith("/"))
                url=loader.getResource(resource.substring(1));
        }
        if (url==null)
        {
            loader=Resource.class.getClassLoader();
            if (loader!=null)
            {
                url=loader.getResource(resource);
                if (url==null && resource.startsWith("/"))
                    url=loader.getResource(resource.substring(1));
            }
        }
        
        if (url==null)
        {
            url=ClassLoader.getSystemResource(resource);
            if (url==null && resource.startsWith("/"))
                url=loader.getResource(resource.substring(1));
        }
        
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
    protected synchronized boolean checkConnection()
    {
        if (_connection==null)
        {
            try{
                _connection=_url.openConnection();
            }
            catch(IOException e)
            {
                e.printStackTrace();
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
    /** Release any resources held by the resource.
     */
    public synchronized void release()
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

        try
        {    
            if( _in != null)
            {
                InputStream in = _in;
                _in=null;
                return in;
            }
            return _connection.getInputStream();
        }
        finally
        {
            _connection=null;
        }
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

        path = canonicalPath(path);

        return newResource(URI.addPaths(_url.toExternalForm(),path));
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return _urlString;
    }

    /* ------------------------------------------------------------ */
    public int hashCode()
    {
        return _url.hashCode();
    }
    
    /* ------------------------------------------------------------ */
    public boolean equals( Object o)
    {
        return o instanceof Resource &&
            _url.equals(((Resource)o)._url);
    }

    /* ------------------------------------------------------------ */
    /** Convert a path to a cananonical form.
     * All instances of "//", "." and ".." are factored out.  Null is returned
     * if the path tries to .. above it's root.
     * @param path 
     * @return path or null.
     */
    public static String canonicalPath(String path)
    {
        return URI.canonicalPath(path);
    }
    
}
