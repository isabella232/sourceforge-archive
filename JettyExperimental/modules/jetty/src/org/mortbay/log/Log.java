// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.log;
import java.lang.reflect.Method;



/*-----------------------------------------------------------------------*/
/** Logging.
 * This class provides a static logging interface.  If an instance of the 
 * org.slf4j.Logger class is found on the classpath, the static log methods
 * are directed to a slf4j logger for "org.mortbay.log".   Otherwise the logs
 * are directed to stderr.
 * 
 * If the system property VERBOSE is set, then ignored exceptions are logged in detail.
 * If the system property DEBUG is set, then debug logs are printed if stderr is being used.
 * 
 */
public class Log 
{    
    private static final String[] __nestedEx =
        {"getTargetException","getTargetError","getException","getRootCause"};
    /*-------------------------------------------------------------------*/
    private static final Class[] __noArgs=new Class[0];
    public final static String EXCEPTION= "EXCEPTION ";
    public final static String IGNORED= "IGNORED";
    public final static String IGNORED_FMT= "IGNORED: {}";
    public final static String NOT_IMPLEMENTED= "NOT IMPLEMENTED ";
    private static boolean debug = System.getProperty("DEBUG",null)!=null;
    private static boolean verbose = System.getProperty("VERBOSE",null)!=null;
    private static Log log=new Log();
   
    static
    {
        try
        {
            log=new Slf4jLog();
            log.doInfo("Logging to slf4j",null,null);
        }
        catch(Exception e)
        {
            if(debug&&verbose)
                e.printStackTrace();
            log.doInfo("Logging to stderr",null,null);
        }
    }
    
    public static void debug(Throwable th)
    {
        log.doDebug(EXCEPTION,th);
        unwind(th);
    }

    public static void debug(String msg)
    {
        log.doDebug(msg,null,null);
    }
    
    public static void debug(String msg,Object arg)
    {
        log.doDebug(msg,arg,null);
    }
    
    public static void debug(String msg,Object arg0, Object arg1)
    {
        log.doDebug(msg,arg0,arg1);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Ignore an exception unless trace is enabled.
     * This works around the problem that log4j does not support the trace level.
     */
    public static void ignore(Throwable th)
    {
        if (verbose)
        {
            log.doDebug(IGNORED,th);
            unwind(th);
        }
        else if (debug)
            log.doDebug(IGNORED_FMT,th.toString(),null);
    }
    
    public static void info(String msg)
    {
        log.doInfo(msg,null,null);
    }
    
    public static void info(String msg,Object arg)
    {
        log.doInfo(msg,arg,null);
    }
    
    public static void info(String msg,Object arg0, Object arg1)
    {
        log.doInfo(msg,arg0,arg1);
    }
    
    public static boolean isDebugEnabled()
    {
        return log.doDebugEnabled();
    }
    
    public static void warn(String msg)
    {
        log.doWarn(msg,null,null);
    }
    
    public static void warn(String msg,Object arg)
    {
        log.doWarn(msg,arg,null);        
    }
    
    public static void warn(String msg,Object arg0, Object arg1)
    {
        log.doWarn(msg,arg0,arg1);        
    }
    
    public static void warn(String msg, Throwable th)
    {
        log.doWarn(msg,th);
        unwind(th);
    }

    public static void warn(Throwable th)
    {
        log.doWarn(EXCEPTION,th);
        unwind(th);
    }

    private static void unwind(Throwable th)
    {
        if (th==null)
            return;
        for (int i=0;i<__nestedEx.length;i++)
        {
            try
            {
                Method get_target = th.getClass().getMethod(__nestedEx[i],__noArgs);
                Throwable th2=(Throwable)get_target.invoke(th,null);
                if (th2!=null)
                    warn("Nested in "+th+":",th2);
            }
            catch(Exception ignore){}
        }
    }
    

    boolean doDebugEnabled()
    {
        return debug;
    }
    
    void doInfo(String msg,Object arg0, Object arg1)
    {
        System.err.println("INFO:  "+format(msg,arg0,arg1));
    }
    
    void doDebug(String msg,Throwable th)
    {
        if (debug)
        {
            System.err.println("DEBUG: "+msg);
            th.printStackTrace();
        }
    }
    
    void doDebug(String msg,Object arg0, Object arg1)
    {
        if (debug)
        {
            System.err.println("DEBUG: "+format(msg,arg0,arg1));
        }
    }
    
    void doWarn(String msg,Object arg0, Object arg1)
    {
        System.err.println("WARN:  "+format(msg,arg0,arg1));
    }
    
    void doWarn(String msg, Throwable th)
    {
        System.err.println("WARN:  "+msg);
        th.printStackTrace();
    }

    private String format(String msg, Object arg0, Object arg1)
    {
        int i0=msg.indexOf("{}");
        int i1=i0<0?-1:msg.indexOf(i0, i0+2);
        
        if (arg1!=null && i1>=0)
            msg=msg.substring(0,i1)+arg1+msg.substring(i1+2);
        if (arg0!=null && i0>=0)
            msg=msg.substring(0,i0)+arg0+msg.substring(i0+2);
        return msg;
    }
    
    
    private static class Slf4jLog extends Log
    {
        private static final String LOGGER="org.slf4j.Logger";
        private static final String LOGGERFACTORY="org.slf4j.LoggerFactory";
        private Method infoSOO;
        private Method debugSOO;
        private Method debugST;
        private Method debugEnabled;
        private Method warnSOO;
        private Method warnST;
        private Object logger;
        
        Slf4jLog() throws Exception
        {
            Class slf4j = Thread.currentThread().getContextClassLoader()==null?Class.forName(LOGGER):Thread.currentThread().getContextClassLoader().loadClass(LOGGER);
            infoSOO = slf4j.getMethod("info", new Class[]{String.class,Object.class,Object.class});
            debugSOO = slf4j.getMethod("debug", new Class[]{String.class,Object.class,Object.class});
            debugST = slf4j.getMethod("debug", new Class[]{String.class,Throwable.class});
            debugEnabled = slf4j.getMethod("isDebugEnabled", new Class[]{});
            warnSOO = slf4j.getMethod("warn", new Class[]{String.class,Object.class,Object.class});
            warnST = slf4j.getMethod("warn", new Class[]{String.class,Throwable.class});
            
            Class slf4jf = Thread.currentThread().getContextClassLoader()==null?Class.forName(LOGGERFACTORY):Thread.currentThread().getContextClassLoader().loadClass(LOGGERFACTORY);
            Method getLogger = slf4jf.getMethod("getLogger", new Class[]{String.class});
            logger=getLogger.invoke(null, new Object[]{"org.mortbay.log"});
        }
        
        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doDebug(java.lang.String, java.lang.Object, java.lang.Object)
         */
        void doDebug(String msg, Object arg0, Object arg1)
        {
            try{debugSOO.invoke(logger, new Object[]{msg,arg0,arg1});}
            catch (Exception e) {e.printStackTrace();}
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doDebug(java.lang.String, java.lang.Throwable)
         */
        void doDebug(String msg, Throwable th)
        {
            try{debugST.invoke(logger, new Object[]{msg,th});}
            catch (Exception e) {e.printStackTrace();}
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doDebugEnabled()
         */
        boolean doDebugEnabled()
        {
            try{return ((Boolean)debugEnabled.invoke(logger, new Object[]{})).booleanValue();}
            catch (Exception e) {e.printStackTrace();return true;}
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doInfo(java.lang.String, java.lang.Object, java.lang.Object)
         */
        void doInfo(String msg, Object arg0, Object arg1)
        {
            try{infoSOO.invoke(logger, new Object[]{msg,arg0,arg1});}
            catch (Exception e) {e.printStackTrace();}
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doWarn(java.lang.String, java.lang.Object, java.lang.Object)
         */
        void doWarn(String msg, Object arg0, Object arg1)
        {
            try{warnSOO.invoke(logger, new Object[]{msg,arg0,arg1});}
            catch (Exception e) {e.printStackTrace();}
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see org.mortbay.log.Log#doWarn(java.lang.String, java.lang.Throwable)
         */
        void doWarn(String msg, Throwable th)
        {
            try{warnST.invoke(logger, new Object[]{msg,th});}
            catch (Exception e) {e.printStackTrace();}
        }
        
    }
}

