// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.start;

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
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;




/*-------------------------------------------*/
/**
 * @author Jan Hlavaty (hlavac@code.cz)
 * @version $Revision$

  TODO:
   - finish possible jetty.home locations
   - use File.toURI.toURL() on JDK 1.4+
   - better handling of errors (i.e. when jetty.home cannot be autodetected...)
   - include entries from lib _when needed_
 */
 
public class Main
{
    private String _classname = null;
    private File _home_dir = null;
    private Classpath _classpath = new Classpath();
    private boolean _debug = Boolean.getBoolean("org.mortbay.start.debug");
    private ArrayList _xml = new ArrayList();
        
    public static void main(String[] args)
    {
        try
        {
            new Main().run(args);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    static File getDirectory(String name)
    {
        try
        {
            if (name != null)
            {
                File dir = new File(name).getCanonicalFile();
                if (dir.isDirectory())
                {
                    return dir;
                }
            }
        } catch (IOException e) { }
        return null;
    }
    

    boolean isAvailable(String classname)
    {
        try
        {
            Class check = Class.forName(classname);
            return true;
        }
        catch (ClassNotFoundException e)
        {}
        
        ClassLoader loader=_classpath.getClassLoader();
        try
        {
            Class check = loader.loadClass(classname);
            return true;
        }
        catch (ClassNotFoundException e)
        {}

        return false;
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
    
    
    void configureClasspath(String home,
                            Classpath classpath,
                            InputStream config,
                            String[] args)
    {
        try
        {
            BufferedReader cfg = new BufferedReader(new InputStreamReader(config,"ISO-8859-1"));
            Version java_version = new Version(System.getProperty("java.version"));
            Version ver = new Version();
            
            // JAR's already processed
            java.util.Hashtable done = new Hashtable();
            String line = cfg.readLine();
            while (line != null)
            {
                try
                {
                    if ((line.length() > 0) && (!line.startsWith("#")))
                    {
                        if (_debug) System.err.println(">"+line);
                        StringTokenizer st = new StringTokenizer(line);
                        String subject = st.nextToken();
                        boolean include_subject = true;
                        String condition=null;
                        while (include_subject && st.hasMoreTokens())
                        {
                            condition = st.nextToken();
                            if (condition.equals("never"))
                            {
                                include_subject = false;
                            }
                            else if (condition.equals("always"))
                            {
                            }
                            else if (condition.equals("available"))
                            {
                                String class_to_check = st.nextToken();
                                include_subject &= isAvailable(class_to_check);
                            }
                            else if (condition.equals("!available"))
                            {
                                String class_to_check = st.nextToken();
                                include_subject &=!isAvailable(class_to_check);
                            }
                            else if (condition.equals("java"))
                            {
                                String operator = st.nextToken();
                                String version = st.nextToken();
                                ver.parse(version);
                                include_subject &=
                                    (operator.equals("<") && java_version.compare(ver)<0) ||
                                    (operator.equals(">") && java_version.compare(ver)>0) ||
                                    (operator.equals("<=") && java_version.compare(ver)<=0) ||
                                    (operator.equals("=<") && java_version.compare(ver)<=0) ||
                                    (operator.equals("=>") && java_version.compare(ver)>=0) ||
                                    (operator.equals(">=") && java_version.compare(ver)>=0) ||
                                    (operator.equals("==") && java_version.compare(ver)==0) ||
                                    (operator.equals("!=") && java_version.compare(ver)!=0);
                            }
                            else if (condition.equals("nargs"))
                            {
                                String operator = st.nextToken();
                                int number = Integer.parseInt(st.nextToken());
                                include_subject &=
                                    (operator.equals("<") && args.length<number) ||
                                    (operator.equals(">") && args.length>number) ||
                                    (operator.equals("<=") && args.length<=number) ||
                                    (operator.equals("=<") && args.length<=number) ||
                                    (operator.equals("=>") && args.length>=number) ||
                                    (operator.equals(">=") && args.length>=number) ||
                                    (operator.equals("==") && args.length==number) ||
                                    (operator.equals("!=") && args.length!=number);
                            }
                            else
                            {
                                System.err.println("ERROR: Unknown condition: "+condition);
                            }
                        }
                        
                        String file=subject.startsWith("/")
                            ?(subject.replace('/',File.separatorChar))
                            :(home+File.separatorChar+subject.replace('/',File.separatorChar));
                        
                        if (_debug)
                            System.err.println("subject="+subject+
                                               " file="+file+
                                               " condition="+condition+
                                               " include_subject="+include_subject);
                        
                        // ok, should we include?
                        if (subject.endsWith("/*"))
                        {
                            // directory of JAR files
                            File extdir = new File(file.substring(0,file.length()-1));
                            File[] jars = extdir.listFiles(new FilenameFilter()
                                {
                                    public boolean accept(File dir, String name)
                                    {
                                        String namelc = name.toLowerCase();
                                        return namelc.endsWith(".jar") || name.endsWith(".zip");
                                        
                                    }
                                } );
                            
                            
                            for (int i=0; i<jars.length; i++)
                            {
                                String jar = jars[i].getCanonicalPath();
                                if (!done.containsKey(jar))
                                {
                                    done.put(jar,jar);
                                    if (include_subject)
                                    {
                                        if (classpath.addComponent(jar) && _debug)
                                            System.err.println("Adding JAR from directory: "+jar);
                                    }
                                }
                            }
                        }
                        else if (subject.endsWith("/"))
                        {
                            // class directory
                            File cd = new File(file);
                            String d = cd.getCanonicalPath();
                            if (!done.containsKey(d))
                            {
                                done.put(d,d);
                                if (include_subject)
                                {
                                    if (classpath.addComponent(d) && _debug)
                                        System.err.println("Adding directory: "+d);
                                }
                            }
                        }
                        else if (subject.toLowerCase().endsWith(".xml"))
                        {
                            // Config file
                            File f = new File(file);                        
                            if (f.exists() && include_subject)
                                _xml.add(f.getCanonicalPath());
                        }
                        else if (subject.toLowerCase().endsWith(".class"))
                        {
                            // Class
                            _classname = subject.substring(0,subject.length()-6);
                        }
                        else
                        {
                            // single JAR file
                            File f = new File(file);                        
                            String d = f.getCanonicalPath();
                            if (!done.containsKey(d))
                            {
                                done.put(d,d);
                                if (include_subject)
                                {
                                    if (classpath.addComponent(d) &&_debug)
                                        System.err.println("Adding single JAR: "+d);
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    if (_debug)
                    {
                        System.err.println(line);
                        e.printStackTrace();
                    }
                }
                line = cfg.readLine();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void run(String[] args)
    {        
        //--------------------
        // detect jetty.home:
        //--------------------
        _home_dir = getDirectory(System.getProperty("jetty.home"));
        if (_home_dir == null)
        {
            // test for [CD]
            File extdir = getDirectory("ext");
            if (extdir != null)
            {
                // alright, we have home
                _home_dir = extdir.getParentFile();
            }
        }

        if (_home_dir == null)
        {    
            // test for parent
            File extdir = getDirectory(".."+File.separator+"ext");
            if (extdir != null)
            {
                // alright, we have home
                try
                {
                    _home_dir = extdir.getParentFile().getCanonicalFile();
                } catch (IOException e) { }
            }
        }
        //TODO: more attempts here...


        if (_home_dir != null)
        {
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
            
            try
            {
                InputStream cpcfg = null;
                try
                {
                    cpcfg = new java.io.FileInputStream(_home_dir.getPath()+File.separatorChar+"etc"+File.separatorChar+"start.config");
                }
                catch (java.io.FileNotFoundException e)
                {
                    cpcfg = null;
                }
                if (cpcfg == null)
                {
                    if (_debug) System.err.println("Configuring classpath from default resource");
                    cpcfg = getClass().getClassLoader().getResourceAsStream("org/mortbay/start/start.config");
                }
                else
                {
                    if (_debug) System.err.println("Configuring classpath from etc/start.config");
                }
                configureClasspath(_home_dir.getPath(), _classpath, cpcfg,args);
                cpcfg.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            
            // try to find javac and add it in classpaths
            String java_home = System.getProperty("java.home");
            if (java_home != null)
            {
                File jdk_home = null;
                try
                {
                    jdk_home = new File(java_home).getParentFile().getCanonicalFile();
                }
                catch (IOException e)
                {}
                if (jdk_home != null)
                {
                    File tools_jar_file = null;
                    try
                    {
                        tools_jar_file = new File(jdk_home, "lib"+File.separator+"tools.jar").getCanonicalFile();
                    }
                    catch (IOException e) {}
                    
                    if ((tools_jar_file != null) && tools_jar_file.isFile())
                    {
                        // OK, found tools.jar in java.home/../lib
                        // add it in
                        _classpath.addComponent(tools_jar_file);
                        if (_debug) System.err.println("JAVAC="+tools_jar_file);
                    }
                }
            }
            
            // okay, classpath complete.
            System.setProperty("java.class.path",_classpath.toString());
            ClassLoader cl = _classpath.getClassLoader();

            // clean up tempdir for Jetty...
            try
            {
                File tmpdir = new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
                if (tmpdir.isDirectory())
                {
                    System.setProperty("java.io.tmpdir",tmpdir.getPath());    
                }
            }
            catch (IOException e) {}

            if (_debug) System.err.println("JETTY_HOME="+System.getProperty("jetty.home"));
            if (_debug) System.err.println("TEMPDIR="+System.getProperty("java.io.tmpdir"));
            if (_debug) System.err.println("CLASSPATH="+_classpath.toString());

            // Invoke org.mortbay.jetty.Server.main(args) using new classloader.
            Thread.currentThread().setContextClassLoader(cl);
            
            try
            {
                if (_xml.size()>0)
                {
                    for (int i=0;i<args.length;i++)
                        _xml.add(args[i]);
                    args=(String[])_xml.toArray(args);
                }
                
                invokeMain(cl,_classname,args);

                boolean demo=false;
                for (int i=0;i<args.length;i++)
                    demo|=args[i].indexOf("demo.xml")>=0;
                
                // for demo, try to invoke web browser on Windows
                if (demo && System.getProperty("os.name").indexOf("Windows")!=-1)
                {
                    Process p = Runtime.getRuntime().exec( new String[] {"C:\\Program Files\\Internet Explorer\\iexplore.exe","http://localhost:8080/"});
                    if (p==null)
                    {
                        System.err.println("ERROR: Failed to start browser.");
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            } 
        }
        else
        {
            // if not, warn user
            System.err.println("ERROR: jetty.home cound not be autodetected, bailing out.");
            System.err.flush();
        }
    }
}
