// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.launcher;

import java.util.StringTokenizer;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;



/*-------------------------------------------*/
/**
 * @author Jan Hlavaty (hlavac@code.cz)
 * @version $Revision$
 */
 
 /* TODO:
 
   - finish possible jetty.home locations
   - use File.toURI.toURL() on JDK 1.4+
   - better handling of errors (i.e. when jetty.home cannot be autodetected...)
   - include entries from lib _when needed_

 */
 
public class Launcher {
    
    private File _home_dir = null;
    private Classpath _classpath = new Classpath();
    
    public static void main(String[] args) {
        try {
            new Launcher().run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    static File getDirectory(String name) {
        try {
            if (name != null) {
                File dir = new File(name).getCanonicalFile();
                if (dir.isDirectory()) {
                    return dir;
                }
            }
        } catch (IOException e) { }
        return null;
    }
    

    public static void invokeMain(ClassLoader classloader, String classname, String[] args)
         throws IllegalAccessException,
                InvocationTargetException,
                NoSuchMethodException,
                ClassNotFoundException
    {
        Class invoked_class = null;
        invoked_class = classloader.loadClass(classname);
        Class[] method_param_types = new Class[1];
        method_param_types[0] = args.getClass();
        Method main = null;
        main = invoked_class.getDeclaredMethod("main",method_param_types);
        Object[] method_params = new Object[1];
        method_params[0] = args;
        main.invoke(null,method_params);
    }
    

    
    public void run(String[] args) {
        
        //--------------------
        // detect jetty.home:
        //--------------------
        _home_dir = getDirectory(System.getProperty("jetty.home"));
        if (_home_dir == null) {
            // test for [CD]
            File extdir = getDirectory("ext");
            if (extdir != null) {
                // alright, we have home
                _home_dir = extdir.getParentFile();
            }
        }
        if (_home_dir == null) {
            // test for parent
            File extdir = getDirectory(".."+File.separator+"ext");
            if (extdir != null) {
                // alright, we have home
                try {
                    _home_dir = extdir.getParentFile().getCanonicalFile();
                } catch (IOException e) { }
            }
        }
        //TODO: more attempts here...


        if (_home_dir != null) {
            // if we managed to detect jetty.home, store it in system property
            System.setProperty("jetty.home",_home_dir.getPath());
            System.setProperty("user.dir",_home_dir.getPath());
            
            // set up classpath:

            // prefill existing paths in classpath_dirs...
            _classpath.addClasspath(System.getProperty("java.class.path"));
            
            // add JARs from ext and lib
            _classpath.addExtdir(_home_dir,"ext");
            _classpath.addExtdir(_home_dir,"lib");
            
            // try to find javac and add it in classpaths
            String java_home = System.getProperty("java.home");
            if (java_home != null) {
                File jdk_home = null;
                try {
                    jdk_home = new File(java_home).getParentFile().getCanonicalFile();
                } catch (IOException e) {}
                if (jdk_home != null) {
                    File tools_jar_file = null;
                    try {
                        tools_jar_file = new File(jdk_home, "lib"+File.separator+"tools.jar").getCanonicalFile();
                    } catch (IOException e) {}
                    if ((tools_jar_file != null) && tools_jar_file.isFile()) {
                        // OK, found tools.jar in java.home/../lib
                        // add it in
                        _classpath.addComponent(tools_jar_file);
                    }
                }
            }
            
            // okay, classpath complete.
            System.setProperty("java.class.path",_classpath.toString());
            ClassLoader cl = _classpath.getClassLoader();

            // clean up tempdir for Jetty...
            try {
            File tmpdir = new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
            if (tmpdir.isDirectory()) {
                System.setProperty("java.io.tmpdir",tmpdir.getPath());
            }
        } catch (IOException e) {}

            System.out.println("JETTY_HOME="+System.getProperty("jetty.home"));
            System.out.println("TEMPDIR="+System.getProperty("java.io.tmpdir"));
            System.out.println("CLASSPATH="+_classpath.toString());

            // Invoke org.mortbay.jetty.Server.main(args) using new classloader.
            Thread.currentThread().setContextClassLoader(cl);
            
            try {
                if (args.length == 0) { // no args, try demo
                   invokeMain(cl,"org.mortbay.jetty.Server", new String[] { "etc"+File.separator+"demo.xml", "etc"+File.separator+"admin.xml" } );
                } else {
                   invokeMain(cl,"org.mortbay.jetty.Server",args);
                }
            } catch (Exception e) {
            }
            
        } else {
            // if not, warn user
            System.err.println("WARNING: jetty.home cound not be autodetected, bailing out.");
            System.err.flush();
        }
    }
}