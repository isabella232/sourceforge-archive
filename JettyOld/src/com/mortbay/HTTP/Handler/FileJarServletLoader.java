// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================


package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Servlet Class Loader.
 * 
 *
 * <h4>Notes</h4>
 * The load search order is:<NL>
 * <LI>Loader cache.
 * <LI>If the class starts with a systemClass package name,
 *     load from builtin loader or fail.
 * <LI>Try the loader path.
 * <LI>Try the builtin loader. Classes found here may not be reloaded.
 *
 * @version 1.0 Tue May  4 1999
 * @author Greg Wilkins (gregw)
 */
public class FileJarServletLoader extends ServletLoader
{
    /* ------------------------------------------------------------ */
    /** Classes from packages starting with these strings can only
     * be loaded from the builtin loader.
     */
    private final static String[] systemClasses =
    {
        "java.",
        "javax."
    };
        
    /* ------------------------------------------------------------ */
    /* Is the class a system class.
     * Check if the class is from java, javax or the com.mortbay
     * packages.
     * @param name The name of the class or package.
     * @return true if the class name is in a system package.
     */
    private static boolean isSystemClass(String name)
    {
        for (int i=systemClasses.length;i-->0;)
            if (name.startsWith(systemClasses[i]))
                return true;
        return false;
    }

    /* ------------------------------------------------------------ */
    static class LoadedFile
    {
        File file;
        long lastModified;
    }
    
    /* ------------------------------------------------------------ */
    private Hashtable cache = new Hashtable();
    Vector paths=new Vector();
    Vector loaded=new Vector();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public FileJarServletLoader(String servletClassPath)
        throws IOException
    {
        StringTokenizer tokenizer =
            new StringTokenizer(servletClassPath,File.pathSeparator);
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
                    // add to the list if it is a directory.
                    paths.addElement(file.getCanonicalPath());
                else
                {
                    // Assume it is a zip file
                    paths.addElement(new ZipFile(file));
                    LoadedFile lc=new LoadedFile();
                    lc.file=file;
                    lc.lastModified=System.currentTimeMillis();
                    loaded.addElement(lc);
                }
            }
            catch(IOException e)
            {
                Code.warning("Problem with "+file,e);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     */
    private InputStream getStreamFromPath(String filename)
    {
        Code.debug("Load ",filename);
        InputStream in=null;
        int length=0;
        
        // For each potential path
        for (int p=0;p<paths.size();p++)
        {
            try
            {
                Object source = paths.elementAt(p);
                if (source instanceof java.util.zip.ZipFile)
                {
                    ZipFile zip = (ZipFile)source;

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
                        Code.debug("Loading ",entry," from ",zip.getName());
                        in = zip.getInputStream(entry);
                        length=(int)entry.getSize();
                        break;
                    }
                }
                else
                {
                    String dir = (String)source;
                    File file = new File(dir+File.separator+filename);
                    if (file.exists())
                    {
                        Code.debug("Loading ",filename);
                        in = new FileInputStream(file);
                        length=(int)file.length();
                        
                        LoadedFile lc=new LoadedFile();
                        lc.file=file;
                        lc.lastModified=file.lastModified();
                        loaded.addElement(lc);
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
    /** 
     */
    private Class loadClassFromPath(String name)
    {
        String filename = name.replace('.',File.separatorChar)+".class";

        // Try for class
        InputStream in=getStreamFromPath(filename);
        if (in!=null)
        {
            try{
                ByteArrayOutputStream out =
                    new ByteArrayOutputStream(8192);
                com.mortbay.Util.IO.copy(in,out);
                
                byte data[] = out.toByteArray();
                return defineClass(name,data,0,data.length);
            }
            catch(Exception e)
            {
                if (Code.verbose(9))
                    Code.ignore(e);
            }
            finally
            {
                try{in.close();}
                catch(Exception e){Code.ignore(e);}
            }
        }
        
        return null;
    }


    /* ------------------------------------------------------------ */
    /** Get a resource as a stream.
     * Return the raw input stream from a file in the classpath
     * @param filename The filename of the resource
     * @return An InputStream to the resource or null
     */
    public InputStream getResourceAsStream(String filename)
    {
        return getStreamFromPath(filename);
    }
    

    
    /* ------------------------------------------------------------ */
    /** Load a class
     * @param name 
     * @param resolve 
     * @return 
     * @exception ClassNotFoundException 
     */
    synchronized
    public Class loadClass(String name)
        throws ClassNotFoundException
    {
        Class c;
        
        //remove the .class
        if (name.endsWith(".class"))
            name = name.substring(0,name.lastIndexOf('.'));
        
        // Look for a cached class
        c=(Class)cache.get(name);
        if (c!=null)
            return c;

        try
        {
            // get the package name
            String packageName="";
            if (name.indexOf(".")>=0)
                packageName=name.substring(0,name.lastIndexOf('.'));
            
            // Check permission
            SecurityManager sm=System.getSecurityManager();
            if (sm!=null)
                sm.checkPackageAccess(packageName);
            
            // Look for a system class
            if (isSystemClass(name))
                // If it is a system class it must be found here or fail
                return findSystemClass(name);
            
            // Load & define
            c=loadClassFromPath(name);
            
            // Try the system class for additional CLASSPATH entries.
            // classes found here can't be reloaded.
            if (c==null)
                return findSystemClass(name);
        }
        finally
        {
            if (c!=null)
                cache.put(name,c);
        }
        
        return c;
    }


    /* ------------------------------------------------------------ */
    /** Return true a class is modified.
     * @return true if any of the classes loaded by this loader have been
     * modified since their load time.
     */
    synchronized public boolean isModified()
    {
        for (int f=loaded.size();f-->0;)
        {
            LoadedFile lf = (LoadedFile)loaded.elementAt(f);        
            if (!lf.file.exists() || lf.file.lastModified()>lf.lastModified)
                return true;
        }
        return false;   
    }
    
};
