// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.sun.java.util.collections.*;
import com.mortbay.Util.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** File Jar Path.
 *
 * Access files from a path of directories and Jar files.
 *
 * @version 1.0 Tue May  4 1999
 * @author Greg Wilkins (gregw)
 */
public class FileJarPath
{
    /* ------------------------------------------------------------ */
    private List _paths=new ArrayList();
    private List _loaded=new ArrayList();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public FileJarPath(String path)
        throws IOException
    {
	this(path,false);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public FileJarPath(String path, boolean quiet)
        throws IOException
    {	
        StringTokenizer tokenizer =
            new StringTokenizer(path,File.pathSeparator);
	
        while (tokenizer.hasMoreTokens())
        {
            String filename=tokenizer.nextToken();
            File file=new File(filename);
            try
            {
                // see if the path element exists
                if (!file.exists())
                    throw new FileNotFoundException(filename);
                file = new File(file.getCanonicalPath());
                if (file.isDirectory())
		{
                    // add to the list if it is a directory.
                    _paths.add(file.getCanonicalPath());
		    if (Code.verbose(100))
			Code.debug("Directory ",file);
		}
                else
                {
                    // Assume it is a zip file
                    _paths.add(new ZipFile(file));
                    LoadedFile lc=new LoadedFile();
                    lc.file=file;
                    lc.lastModified=System.currentTimeMillis();
                    _loaded.add(lc);
		    if (Code.verbose(100))
			Code.debug("Jar ",file);
                }
            }
            catch(IOException e)
            {
		if (quiet)
		    Code.ignore(e);
		else
		    Code.warning("Problem with "+file,e);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     */
    public InputStream getInputStream(String filename)
    {
        if (Code.verbose()) Code.debug("get ",filename);
        InputStream in=null;
        int length=0;
        
        // For each potential path
        for (int p=0;p<_paths.size();p++)
        {
            try
            {
                Object source = _paths.get(p);
                if (source instanceof java.util.zip.ZipFile)
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
                else
                {
                    String dir = (String)source;
                    File file = new File(dir+File.separator+filename);
                    if (file.exists())
                    {
			if (Code.verbose(1000))
			    Code.debug("Look in dir ",file,
				       " for ",filename);

			
                        in = new FileInputStream(file);
                        length=(int)file.length();
                        
                        LoadedFile lc=new LoadedFile();
                        lc.file=file;
                        lc.lastModified=file.lastModified();
                        _loaded.add(lc);
                        if (Code.verbose()) Code.debug("Found ",filename);
                        break;
                    }
                }
            }
            catch(Exception e)
            {
                if (Code.verbose())
                    Code.debug(e);
            }
        }
        
        return in;
    }

    /* ------------------------------------------------------------ */
    /** Return true a loaded file is modified.
     * @return true if any of the files loaded have been
     * modified since their load time.
     */
    synchronized public boolean isModified()
    {
        for (int f=_loaded.size();f-->0;)
        {
            LoadedFile lf = (LoadedFile)_loaded.get(f);
            if (!lf.file.exists() || lf.file.lastModified()>lf.lastModified)
                return true;
        }
        return false;   
    }

    /* ------------------------------------------------------------ */
    static class LoadedFile
    {
        File file;
        long lastModified;
    }
}
