// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------
package com.mortbay.Util;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.ArrayList;

/* ------------------------------------------------------------ */
class JarFileResource extends Resource
{
    JarURLConnection _jarConnection;
    JarFile _jarFile;
    File _file;
    String[] _list;
    JarEntry _entry;
    
    /* -------------------------------------------------------- */
    JarFileResource(URL url)
    {
        super(url,null);
    }

    /* ------------------------------------------------------------ */
    protected boolean checkConnection()
    {
        boolean check=super.checkConnection();
        try{
            if (_jarConnection!=_connection)
            {
                _entry=null;
                _file=null;
                _jarFile=null;
                _jarConnection=(JarURLConnection)_connection;
                _jarFile=_jarConnection.getJarFile();
                _file=new File(_jarFile.getName());
            }
        }
        catch(IOException e)
        {
            Code.ignore(e);
            _entry=null;
            _file=null;
            _jarFile=null;
            _jarConnection=null;    
        }
        
        return _jarFile!=null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists()
    {
        if (_urlString.endsWith("!/"))
            return checkConnection();
        else if (checkConnection())
        {
            if (_entry==null)
            {
                Enumeration e=_jarFile.entries();
                String path=_urlString.substring(_urlString.indexOf("!/")+2);
                while(e.hasMoreElements())
                {
                    JarEntry entry = (JarEntry) e.nextElement();
                    String name=entry.getName().replace('\\','/');
                    if (name.equals(path))
                    {
                        _entry=entry;
                        break;
                    }
                }
            }
        }
        return _entry!=null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the last modified time
     */
    public long lastModified()
    {
        if (checkConnection())
            return _file.lastModified();
        return -1;
    }

    /* ------------------------------------------------------------ */
    public String[] list()
    {
        if(isDirectory() && _list==null)
        {
            Enumeration e=_jarFile.entries();
            String dir=_urlString.substring(_urlString.indexOf("!/")+2);
            ArrayList list = new ArrayList(10);
            while(e.hasMoreElements())
            {
                JarEntry entry = (JarEntry) e.nextElement();
                String name=entry.getName().replace('\\','/');
                if(!name.startsWith(dir) || name.length()==dir.length())
                    continue;
                String listName=name.substring(dir.length());
                int dash=listName.indexOf('/');
                if (dash>=0 && dash!=(listName.length()-1))
                    continue;

                list.add(listName);
            }

            _list=new String[list.size()];
            list.toArray(_list);
        }
        return _list;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Return the length of the resource
     */
    public long length()
    {
        if (isDirectory())
            return -1;

        if (_entry!=null)
            return _entry.getSize();
        
        return -1;
    }
}








