// ========================================================================
// Copyright (c) 1997-2003 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package org.mortbay.util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*-----------------------------------------------------------------------*/
/** Log Support class.
 */
public class LogSupport 
{
    private static Log log = LogFactory.getLog(LogSupport.class);
    private static boolean trace = System.getProperty("TRACE",null)!=null;
    private static boolean log4j =
        log instanceof org.apache.commons.logging.impl.Log4JLogger ||
        log instanceof org.apache.commons.logging.impl.Log4JCategoryLog;
    
    static
    {
        log.info("Log instance is "+log.getClass());
        if(LogSupport.isTraceEnabled(log))log.trace("Log from "+log.getClass().getClassLoader());
    }
    
    public final static String IGNORED= "IGNORED ";
    public final static String EXCEPTION= "EXCEPTION ";
    public final static String NOT_IMPLEMENTED= "NOT IMPLEMENTED ";


    /* ------------------------------------------------------------ */
    /**
     * Ignore an exception unless trace is enabled.
     * This works around the problem that log4j does not support the trace level.
     */
    public static void ignore(Log log,Throwable th)
    {
        if (trace && log.isTraceEnabled()) log.trace(IGNORED,th);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Is Trace Enabled.
     * This works around the problem that log4j does not support the trace level.
     * @return true IFF log.isTraceEnabled() AND ( the system property TRACE is set OR not using log4j )
     */
    public static boolean isTraceEnabled(Log log)
    {
        return trace && log.isTraceEnabled();
    }
    
}

