// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.launcher;

import java.util.StringTokenizer;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.InputStreamReader;




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
    

    boolean isAvailable(String classname) {
        try {
            Class check = Class.forName(classname);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
    
    
    void configureClasspath(String home, Classpath classpath, InputStream config) {
        
        try {
        
            BufferedReader cfg = new BufferedReader(new InputStreamReader(config,"ISO-8859-1"));
            Version java_version = new Version(System.getProperty("java.version"));
            Version ver = new Version();
            
            // JAR's already processed
            java.util.Hashtable done = new Hashtable();
            String line = cfg.readLine();
            while (line != null) {
                if ((line.length() > 0) && (!line.startsWith("#"))) {
                    System.out.println(">"+line);
                    StringTokenizer st = new StringTokenizer(line);
                    String jarfile = st.nextToken();
                    boolean include_jar = true;
                    while (include_jar && st.hasMoreTokens()) {
                        String condition = st.nextToken();
                        if (condition.equals("never")) {
                            include_jar = false;
                        } else if (condition.equals("always")) {
                            
                        } else if (condition.equals("available")) {
                            String class_to_check = st.nextToken();
                            if (!isAvailable(class_to_check)) {
                                include_jar = false;
                            }
                        } else if (condition.equals("!available")) {
                            String class_to_check = st.nextToken();
                            if (isAvailable(class_to_check)) {
                                include_jar = false;
                            }
                        } else if (condition.equals("java")) {
                            String operator = st.nextToken();
                            String version = st.nextToken();
                            ver.parse(version);
                            if (operator.equals("<")) {
                                if (java_version.compare(ver) >= 0) include_jar = false;
                            } else if (operator.equals(">")) {
                                if (java_version.compare(ver) <= 0) include_jar = false;
                            } else if (operator.equals("<=")||operator.equals("=<")) {
                                if (java_version.compare(ver) > 0) include_jar = false;
                            } else if (operator.equals(">=")||operator.equals("=>")) {
                                if (java_version.compare(ver) < 0) include_jar = false;
                            } else if (operator.equals("==")) {
                                if (java_version.compare(ver) != 0) include_jar = false;
                            } else if (operator.equals("!=")||operator.equals("<>")) {
                                if (java_version.compare(ver) == 0) include_jar = false;
                            } else {
                                // report error
                            }
                        }
                    }
                    // ok, should we include?
                    if (jarfile.endsWith("/*")) {
                        // directory of JAR files
                        File extdir = new File(home+File.separatorChar+jarfile.substring(0,jarfile.length()-1).replace('/',File.separatorChar));
                        File[] jars = extdir.listFiles( new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                String namelc = name.toLowerCase();
                                return namelc.endsWith(".jar") || name.endsWith(".zip");
                            }
                        } );
                        for (int i=0; i<jars.length; i++) {
                            String jar = jars[i].getCanonicalPath();
                            if (!done.containsKey(jar)) {
                                done.put(jar,jar);
                                if (include_jar) {
                                    System.out.println("Adding JAR from directory: "+jar);
                                    classpath.addComponent(jar);
                                }
                            }
                        }
                    } else if (jarfile.endsWith("/")) {
                        // class directory
                        File cd = new File(home+File.separatorChar+jarfile.replace('/',File.separatorChar));
                        String d = cd.getCanonicalPath();
                        if (!done.containsKey(d)) {
                            done.put(d,d);
                            if (include_jar) {
                                System.out.println("Adding directory: "+d);
                                classpath.addComponent(d);
                            }
                        }
                    } else {
                        // single JAR file
                        File f = new File(home+File.separatorChar+jarfile.replace('/',File.separatorChar));
                        String d = f.getCanonicalPath();
                        if (!done.containsKey(d)) {
                            done.put(d,d);
                            if (include_jar) {
                                System.out.println("Adding single JAR: "+d);
                                classpath.addComponent(d);
                            }
                        }
                    }
                }
                line = cfg.readLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

            /*
            // temp log            
            try {        
                java.io.PrintStream log = new java.io.PrintStream(new java.io.FileOutputStream(_home_dir.getPath()+File.separator+"demo.log"));
                System.setOut(log);
                System.setErr(log);
            } catch (IOException e) {}
            */
            
            
            // set up classpath:

            // prefill existing paths in classpath_dirs...
            _classpath.addClasspath(System.getProperty("java.class.path"));
            
            // add JARs from ext and lib
            // be smart about it
            
            try {
                InputStream cpcfg = null;
                try {
                    cpcfg = new java.io.FileInputStream(_home_dir.getPath()+File.separatorChar+"etc"+File.separatorChar+"classpath.config");
                } catch (java.io.FileNotFoundException e) {
                    cpcfg = null;
                }
                if (cpcfg == null) {
                    System.out.println("Configuring classpath from default resource");
                    cpcfg = getClass().getClassLoader().getResourceAsStream("org/mortbay/jetty/launcher/classpath.config");
                } else {
                    System.out.println("Configuring classpath from etc/classpath.config");
                }
                configureClasspath(_home_dir.getPath(), _classpath, cpcfg);
                cpcfg.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            
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
                        System.out.println("JAVAC="+tools_jar_file);
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
                   // for demo, try to invoke web browser on Windows
                   if (System.getProperty("os.name").indexOf("Windows")!=-1) {
                        System.out.println("Trying to launch IE browser...");
                        Process p = Runtime.getRuntime().exec( new String[] {"C:\\Program Files\\Internet Explorer\\iexplore.exe","http://localhost:8080/"});
                        if (p==null) {
                            System.out.println("Failed to start browser.");
                        }
                   }
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