// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;
import java.util.HashMap;

/* ------------------------------------------------------------ */
/**
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ContextLoader extends URLClassLoader
{
    private static HashMap __infoMap = new HashMap(3);
    private PathInfo _info;
    private String _path;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param servletClassPath Coma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     * @param quiet If true, non existant paths are not reported
     * @exception IOException
     */
    public ContextLoader(String classpath,
                         ClassLoader parent)
    {
        super(decodePath(classpath),parent);
        _path=classpath;
        _info=(PathInfo)__infoMap.get(classpath);

        Code.debug("ContextLoader: ",classpath,",",parent,
                   " == ",_info._fileClassPath);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static URL[] decodePath(String classpath)
    {
        synchronized(__infoMap)
        {
            PathInfo info=(PathInfo)__infoMap.get(classpath);
            if (info!=null && info._urls!=null)
                return info._urls;

            info = new PathInfo();
            
            try{
                StringTokenizer tokenizer =
                    new StringTokenizer(classpath,",;");
                info._urls = new URL[tokenizer.countTokens()];
                int i=0;
                while (tokenizer.hasMoreTokens())
                {
                    Resource resource =
                        Resource.newResource(tokenizer.nextToken());

                    // Resolve file path if possible
                    File pfile=resource.getFile();
                    if (pfile==null)
                    {
                        info._unresolved=true;
                        if (info._fileClassPath==null)
                            info._fileClassPath=resource.toString();
                        else
                            info._fileClassPath=info._fileClassPath+
                                File.pathSeparator+
                                resource.toString(); 
                    }
                    else
                    {
                        if (info._fileClassPath==null)
                            info._fileClassPath=pfile.getAbsolutePath();
                        else
                            info._fileClassPath=info._fileClassPath+
                                File.pathSeparator+
                                pfile.getAbsolutePath();                    
                    }
                    
                    // Add resource or expand jar/
                    if (resource.isDirectory() || pfile!=null)
                        info._urls[i++]=resource.getURL();
                    else
                    {
                        // XXX - this is a jar in a jar, so we must
                        // extract it - probably should be to an in memory
                        // structure, but this will do for now.
                        // XXX - Need to do better with the temp dir
                        InputStream in =resource.getInputStream();
                        File file=File.createTempFile("Jetty",".zip");
                        file.deleteOnExit();
                        Code.debug("Extract ",resource," to ",file);
                        FileOutputStream out = new FileOutputStream(file);
                        IO.copy(in,out);
                        out.close();
                        info._urls[i++]=file.toURL();
                    }
                }
            }
            catch(Exception e){Code.warning(e);info=null;}
            catch(Error e){Code.warning(e);info=null;}
            
            if (info==null)
                info=new PathInfo();
            if (info._urls==null)
                info._urls=new URL[0];
            __infoMap.put(classpath,info);
            return info._urls;        
        }
    }
    
    /* ------------------------------------------------------------ */
    protected Class findClass(String name)
        throws ClassNotFoundException
    {
        if (Code.verbose())
            Code.debug("findClass(",name,") from ",_path);
        return super.findClass(name);
    }

    /* ------------------------------------------------------------ */
    protected synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (Code.verbose())
            Code.debug("loadClass(",name,","+resolve,") from ",_path);
        return super.loadClass(name,resolve);
    }

    /* ------------------------------------------------------------ */
    public String getFileClassPath()
    {
        if (_info._unresolved)
            Code.warning("Could not resolve to file path for JSP:"+_path+
                         "\nUsing "+_info._fileClassPath+
                         "\nTry unpacking the war file.");
        
        return _info._fileClassPath;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "com.mortbay.HTTP.ContextLoader("+
            _path+") / "+getParent();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class PathInfo
    {
        URL[] _urls=null;
        String _fileClassPath=null;
        boolean _unresolved=false;
    }
    
}


