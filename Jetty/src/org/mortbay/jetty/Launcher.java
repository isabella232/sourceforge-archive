// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty;

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
 * !EXPERIMENTAL! Launcher class which automatically sets up
 * classpath and jetty home directory and then invokes
 * {@link org.mortbay.jetty.Server Server} with any commandline parameters.<br>
 * <br>
 * Will autodetect jetty.home if none has been provided in jetty.home system property
 * by examining the following directories in this order:
 * <ol>
 * <li> [CD] Current directory. Try to find ext subdirectory of it.
 * <li> [JAR] Parent directory of (lib) directory containing jar containing this class. [TODO]
 * <li> [CLASS] Parent directory of (classes) directory containing this .class file. [TODO]
 * </ol>
 * If one of these locations contains ext subdirectory, it will be used as jetty.home.
 * <br><br>
 * Classpath configuration: <br>
 * <br>
 * If there is classes subdirectory in ext, it will be added to classpath
 * (for individual classfiles and other resources). <br>
 * All JAR files found in ext subdirectory of java.home will be added to classpath.
 * In addition, if Sun's javac (required for Jasper) is not already in classpath,
 * JAR containing it will be looked for in ${java.home}/../lib/tools.jar and added
 * to classpath if found.
 *
 * @author Jan Hlavaty (hlavac@code.cz)
 * @version $Revision$
 */
 
 /* TODO:
 
   - finish possible jetty.home locations
   - use File.toURI.toURL() on JDK 1.4+
   - better handling of errors (i.e. when jetty.home cannot be autodetected...)
   - include entries from lib _when needed_
   - find out what's up with classpath...

 */
 
public abstract class Launcher {
    public static void main(String[] args) {
        
        //--------------------
        // detect jetty.home:
        //--------------------
        
        String jetty_home = (String)System.getProperty("jetty.home");
        
        if (jetty_home == null) {
            try {
                // test for [CD]
                File extdir = new File("ext").getCanonicalFile();
                if (extdir.isDirectory()) {
                    // alright, we have home
                    jetty_home = extdir.getParentFile().getCanonicalPath();
                }
            } catch (Exception e) { }
            
            if (jetty_home == null) {
                // test for [JAR]
                
            }
            
            if (jetty_home == null) {
                // test for [CLASS]
                
            }
            
            if (jetty_home != null) {
                // if we managed to detect jetty.home, store it in system property
                System.setProperty("jetty.home",jetty_home);
            } else {
                // if not, warn user
                System.err.println("WARNING: jetty.home cound not be autodetected, classpath will not be set.");
                System.err.flush();
            }
        }
        
        if (jetty_home != null) {
            // set up classpath:
            
            java.util.Vector classpath_urls = new java.util.Vector();   // storage for URLs for use in Jetty classloader later
            java.util.Vector classpath_dirs = new java.util.Vector();   // storage for directories/JAR filenames
            
            // prefill existing paths in classpath_dirs...
            String original_classpath = (String)System.getProperty("java.class.path");
            System.out.println("ORIG_CLASSPATH="+original_classpath);
            if (original_classpath != null) {
                StringTokenizer t = new StringTokenizer(original_classpath, File.pathSeparator);
                while (t.hasMoreTokens()) {
                    String classpath_element = (String)t.nextToken();
                    if (classpath_element.length()>0) {
                        try {
                            String existing_dir = new File(classpath_element).getCanonicalPath();
                            if (!classpath_dirs.contains(existing_dir)) {
                                classpath_dirs.add( existing_dir );
                            }
                        } catch (IOException e) {}
                    }
                }
            }
            
            File home = null;
            try {
                home = new File(jetty_home).getCanonicalFile();
            } catch (IOException e) {}
            if ((home != null) && home.isDirectory()) {
                File ext = null;
                try {
                    ext = new File(home,"ext").getCanonicalFile();
                } catch (IOException e) {}
                if ((ext != null) && ext.isDirectory()) {
                    // ok. Looks like we got ext directory.
                    File classes = null;
                    try {
                        classes = new File(ext,"classes").getCanonicalFile();
                    } catch (IOException e) {}
                    if ((classes != null) && classes.isDirectory()) {
                        try {
                            classpath_urls.add( classes.toURL() );  // TODO: this should be .toURI().toURL() in JDK1.4+
                        } catch (MalformedURLException e) {}
                        String classes_dir = classes.getPath();
                        if (!classpath_dirs.contains(classes_dir)) {
                            classpath_dirs.add(classes_dir);
                        }
                    }
                    // now find all .jar files in ext and add them to the list
                    File[] jars = ext.listFiles( new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".JAR") || name.endsWith(".ZIP");
                        }
                    } );
                    for (int i=0; i<jars.length; i++) {
                        File jarfile = null;
                        try {
                            jarfile = jars[i].getCanonicalFile();
                        } catch (IOException e) {}
                        if (jarfile != null) {
                            String jar = jarfile.getPath();
                            try {
                                classpath_urls.add( jarfile.toURL() );   // TODO: this should be .toURI().toURL() in JDK1.4+
                            } catch (MalformedURLException e) { }
                            if (!classpath_dirs.contains(jar)) {
                                classpath_dirs.add(jar);
                            }
                        }
                    }
                }
            }
            //  try to find javac and add it in classpaths
            Class javac_class = null;
            try {
                javac_class = Class.forName("sun.tools.javac.Main");
            } catch (ClassNotFoundException e) { }
            if (javac_class == null) {
                // Sun's javac not on path. Try to look for it.
                String java_home = (String)System.getProperty("java.home");
                System.out.println("java.home="+java_home);
                if (java_home != null) {
                    File jdk_home = null;
                    try {
                        jdk_home = new File(java_home).getParentFile().getCanonicalFile();
                    } catch (IOException e) {}
                    System.out.println("jdk.home="+jdk_home.getPath());
                    if (jdk_home != null) {
                        File tools_jar_file = null;
                        try {
                            tools_jar_file = new File(jdk_home, "lib"+File.separator+"tools.jar").getCanonicalFile();
                        } catch (IOException e) {}
                        System.out.println("tools.jar="+tools_jar_file);
                        
                        if ((tools_jar_file != null) && tools_jar_file.isFile()) {
                            // OK, found tools.jar in java.home/../lib
                            // add it in
                            try {
                                classpath_urls.add(tools_jar_file.toURL());     // TODO: this should be .toURI().toURL() in JDK1.4+
                            } catch (MalformedURLException e) {}
                            String tools_jar = tools_jar_file.getPath();
                            if (!classpath_dirs.contains(tools_jar)) {
                                classpath_dirs.add(tools_jar);
                            }
                        }
                    }
                }
            }
            
            // use all the classpaths we generated.
            // create new java.class.path
            StringBuffer newclasspath = new StringBuffer(1024);
            if (classpath_dirs.size() >= 1) {
                newclasspath.append( (String)classpath_dirs.elementAt(0) );
            }
            for (int i=1; i < classpath_dirs.size(); i++) {
                newclasspath.append(File.pathSeparatorChar);
                newclasspath.append( (String)classpath_dirs.elementAt(i) );
            }
            System.setProperty("java.class.path",newclasspath.toString());

            System.out.println("NEW_CLASSPATH="+newclasspath.toString());
            // create new ClassLoader
            
            ClassLoader old_classloader = Launcher.class.getClassLoader();
            URL[] urls = new URL[  classpath_urls.size() ];
            for (int i=0; i < classpath_urls.size(); i++) {
                urls[i] = (URL)classpath_urls.elementAt(i);
            }
            System.out.println("CLASSPATH_URLS:");
            for (int i=0; i<urls.length; i++) {
                System.out.println(urls[i].toString());
            }
            ClassLoader new_classloader = new URLClassLoader(urls, old_classloader);
            
            try {
                new_classloader.loadClass("sun.tools.javac.Main");
            } catch (ClassNotFoundException e) {
                System.err.println("WARN: javac not loadable");
            }
            
            // Invoke org.mortbay.jetty.Server.main(args) using new classloader.
            Class server_class = null;
            try {
                server_class = new_classloader.loadClass("org.mortbay.jetty.Server");
                Class[] method_param_types = new Class[1];
                method_param_types[0] = args.getClass();
                Method main = null;
                try {
                    main = server_class.getDeclaredMethod("main",method_param_types);
                    Object[] method_params = new Object[1];
                    method_params[0] = args;
                    try {
                        main.invoke(null,method_params);
                    } catch (IllegalAccessException e) {}
                    catch (InvocationTargetException e) {}
                } catch (NoSuchMethodException e) {
                    System.err.println("Unable to get main from org.mortbay.jetty.Server!");
                    System.err.flush();
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Unable to load org.mortbay.jetty.Server!");
                System.err.flush();
            }
        }
    }
}