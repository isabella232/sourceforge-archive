// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/* ------------------------------------------------------------ */
/** Resource Path.
 *
 * Access files from a path of URLs, directories and Jar files.
 *
 * XXX - This class needs a BIG TIDY UP and a test harness
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ResourcePath
{
    /* ------------------------------------------------------------ */
    private List _paths=new ArrayList();
    private List _loaded=new ArrayList();
        
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public ResourcePath(String path)
        throws IOException
    {
        this(path,false);
    }
        
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public ResourcePath(String path, boolean quiet)
        throws IOException
    { 
        StringTokenizer tokenizer = new StringTokenizer(path,",;");
         
        while (tokenizer.hasMoreTokens())
        {
            String token=tokenizer.nextToken();
            try
            {
                Resource resource = Resource.newResource(token);
                if( !resource.exists())
                    resource = Resource.newResource( token +"/");
                if( !resource.exists())
                    throw new FileNotFoundException(token);

                File file=resource.getFile();

                if (resource.isDirectory())
                    _paths.add(resource);
                else if (file!=null)
                {
                    // Assume it is a zip file
                    _paths.add(new ZipFile(file));
                    LoadedFile lc=new LoadedFile();
                    lc.resource=resource;
                    lc.lastModified=System.currentTimeMillis();
                    _loaded.add(lc);
                    if (Code.verbose(100))
                        Code.debug("Jar ",file);
                }
                else
                {
                    Set filenames = new HashSet();
                    InputStream in =resource.getInputStream();

                    // XXX -Lets extract it to a temp file for now
                    file=File.createTempFile("Jetty",".zip");
                    file.deleteOnExit();
                    FileOutputStream out = new FileOutputStream(file);
                    IO.copy(in,out);
                    out.close();
                    
                    _paths.add(new ZipFile(file));
                    LoadedFile lc=new LoadedFile();
                    lc.resource=resource;
                    lc.lastModified=System.currentTimeMillis();
                    _loaded.add(lc);
                    if (Code.verbose(100))
                        Code.debug("Jar ",file," from ",resource);
                }
            }
            catch(IOException e)
            {
                if (quiet)
                    Code.ignore(e);
                else
                    Code.warning("Problem with "+token,e);
            }    
        }
    }
    
    
    
    /* ------------------------------------------------------------ */
    /** 
     */
    public InputStream getInputStream(String filename)
    {
        if (Code.verbose()) Code.debug("get ",filename);
         
        // XXX Maybe it should get here with / instead of \.
        filename = filename.replace( File.separatorChar, '/');
        InputStream in=null;
        int length=0;

        // For each potential path
        for (int p=0;p<_paths.size();p++)
        {
            Resource resource=null;
            try
            {
                Object source = _paths.get(p);
                if (source instanceof ZipFile)
                {
                    ZipFile zip = (ZipFile)source;

                    if (Code.verbose(1000))
                        Code.debug("Look in jar ",zip,
                                   " for ",filename);
                    
                    ZipEntry entry = zip.getEntry(filename);
                    if (entry==null)
                    {
                        // Try alternate file separator for jars prepared
                        // on other architectures.
                        String alt=null;
                        if (File.separatorChar=='/')
                            alt=filename.replace('/','\\');
                        else
                            alt=filename.replace(File.separatorChar,'/');
                        entry = zip.getEntry(alt);
                    }
                    
                    if (entry!=null)
                    {
                        in = zip.getInputStream(entry);
                        length=(int)entry.getSize();
                        if (Code.verbose()) Code.debug("Found ",entry," in ",zip.getName());
                        break;
                    }
                }
                else if (source instanceof Resource)
                {   
                    Resource resourceBase = (Resource)_paths.get(p);
                    resource = resourceBase.addPath(filename);
                    if (resource.exists())
                    {
                        if (Code.verbose(1000))
                            Code.debug("Look in dir ",resourceBase,
                                       " for ",filename);
                        
                        in = resource.getInputStream();
                        length=(int)resource.length();
                        
                        LoadedFile lc=new LoadedFile();
                        lc.resource=resource;
                        lc.lastModified=resource.lastModified();
                        lc.resource.release();
                        if (lc.lastModified>0)
                            _loaded.add(lc);
                        if (Code.verbose()) Code.debug("Found ",filename);
                        break;
                    }
                }
                else
                {
                    Code.warning("Not Implemented for: "+source);
                }
            }
            catch(Exception e)
            {
                if (Code.verbose())
                    Code.debug(e);
            }
            finally
            {
                if(resource!=null)
                    resource.release();
            }
        }
         
        return in;
    }

    
    
    /* ------------------------------------------------------------ */
    /** 
     */
    public Resource getResource(String filename)
    {
        if (Code.verbose()) Code.debug("get ",filename);
         
        // XXX Maybe it should get here with / instead of \.
        filename = filename.replace( File.separatorChar, '/');
        int length=0;
        
        Resource resource=null;

        // For each potential path
        for (int p=0;p<_paths.size();p++)
        {
            try
            {
                Object source = _paths.get(p);
                if (source instanceof ZipFile)
                {
                    ZipFile zip = (ZipFile)source;

                    if (Code.verbose(1000))
                        Code.debug("Look in jar ",zip,
                                   " for ",filename);
                    
                    ZipEntry entry = zip.getEntry(filename);
                    if (entry==null)
                    {
                        // Try alternate file separator for jars prepared
                        // on other architectures.
                        String alt=null;
                        if (File.separatorChar=='/')
                            alt=filename.replace('/','\\');
                        else
                            alt=filename.replace(File.separatorChar,'/');
                        entry = zip.getEntry(alt);
                    }
                    
                    if (entry!=null)
                    {
                        resource=Resource.newResource
                            ("jar:"+
                             new File(zip.getName()).toURL().toString()+
                             (filename.startsWith("/")?"!":"!/")+
                             filename);  
                        break;
                    }
                }
                else if (source instanceof Resource)
                {   
                    Resource resourceBase = (Resource)_paths.get(p);
                    resource = resourceBase.addPath(filename);
                    if (resource.exists())
                        break;
                    resource=null;
                }
                else
                {
                    Code.warning("Not Implemented for: "+source);
                }
            }
            catch(Exception e)
            {
                if (Code.verbose())
                    Code.debug(e);
            }
        }
         
        return resource;
    }

    
    /* ------------------------------------------------------------ */
    /** Return true a loaded file is modified.
     * @return true if any of the files loaded have been
     * modified since their load time.
     * 
     */
    synchronized public boolean isModified()
    {
        for (int f=_loaded.size();f-->0;)
        {
            LoadedFile lf = (LoadedFile)_loaded.get(f);
            if (!lf.resource.exists() || lf.resource.lastModified()>lf.lastModified)
                return true;
        }
        return false; 
    }

    /* ------------------------------------------------------------ */
    static class LoadedFile
    {
        Resource resource;
        long lastModified;
    }
}


