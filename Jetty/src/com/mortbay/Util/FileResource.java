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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;


/* ------------------------------------------------------------ */
/** File Resource.
 *
 * Handle resources of implied or explicit file type.
 * This class can check for aliasing in the filesystem (eg case
 * insensitivity).  By default this is turned on if the platform does
 * not have the "/" path separator, or it can be controlled with the
 * "com.mortbay.Util.FileResource.checkAliases" system parameter.
 *
 * If alias checking is turned on, then aliased resources are
 * treated as if they do not exist, nor can they be created.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
class FileResource extends Resource
{
    private static boolean __checkAliases =
        "true".equalsIgnoreCase(System.getProperty("com.mortbay.Util.FileResource.checkAliases",
                                                   File.pathSeparatorChar=='/'?"false":"true"));
    
    /* ------------------------------------------------------------ */
    private File _file;
    private boolean _isAlias;
        
    /* -------------------------------------------------------- */
    FileResource(URL url, URLConnection connection, File file)
    {
        super(url,connection);
        _file=file;

        if (__checkAliases)
        {
            try{
                String abs=_file.getAbsolutePath();
                String can=_file.getCanonicalPath();

                _isAlias=!abs.equals(can);
                if (_isAlias && Code.debug())
                {
                    Code.debug("ALIAS abs=",abs);
                    Code.debug("ALIAS can=",can);
                }
            }
            catch(IOException e)
            {
                Code.ignore(e);
            }
        }
    }
    

    /* -------------------------------------------------------- */
    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists()
    {
        return _file.exists() && !_isAlias;
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
        return _file.isDirectory() && !_isAlias;
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
        return _file.getAbsolutePath();
    }
        
    /* ------------------------------------------------------------ */
    /**
     * Returns an File representing the given resource or NULL if this
     * is not possible.
     */
    public File getFile()
    {
        if (_isAlias)
            return null;
        return _file;
    }
        
    /* --------------------------------------------------------- */
    /**
     * Returns an input stream to the resource
     */
    public InputStream getInputStream() throws IOException
    {
        if (_isAlias)
            throw new FileNotFoundException("File is alias:"+getName());
        return new FileInputStream(_file);
    }
        
    /* --------------------------------------------------------- */
    /**
     * Returns an output stream to the resource
     */
    public OutputStream getOutputStream()
        throws java.io.IOException, SecurityException
    {
        if (_isAlias)
            throw new FileNotFoundException("File is alias:"+getName());
        return new FileOutputStream(_file);
    }
        
    /* --------------------------------------------------------- */
    /**
     * Deletes the given resource
     */
    public boolean delete()
        throws SecurityException
    {
        if (_isAlias)
            throw new SecurityException("File is alias:"+getName());
        return _file.delete();
    }

    /* --------------------------------------------------------- */
    /**
     * Rename the given resource
     */
    public boolean renameTo( Resource dest)
        throws SecurityException
    {
        if (_isAlias)
            throw new SecurityException("File is alias:"+getName());
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
