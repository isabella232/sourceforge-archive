// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.util.*;
import java.io.*;
import java.text.*;

/*-----------------------------------------------------------------------*/
/** Log formatted and tagged messages.
 * Multiple LogSinks instances can be configured, but by default a
 * System.err sink is created.
 * <p>
 * The Log log format is controlled by the LOG_OPTIONS property
 * supplied to the VM. 
 * <p>If LOG_OPTIONS is set, then the default output format is controlled
 * by the option characters in the string:
 * <PRE>
 * t Timestamp log output
 * T Show the log tag name
 * L Show log label (thread, method and file names).
 * s Show indication of stack depth
 * S Stack trace for each output line (VERY VERBOSE)
 * O Place each log one One line of output
 * </PRE>
 *
 * <p> If the property LOG_FILE is set, this class uses it as the alternate
 * destination for Log output rather than standard error.
 * <p> If the property LOG_DATE_FORMAT is set, then it is interpreted
 * as a format string for java.text.SimpleDateFormat and used to
 * format the log timestamps. Note: The character '+' is replaced with
 * space in the date format string.  If LOG_TIMEZONE is set, it is used
 * to set the timezone of the log date format, otherwise GMT is used.
 */
public class Log 
{
    /*-------------------------------------------------------------------*/
    public final static String EVENT="LOG.EVENT";
    public final static String WARN="LOG.WARN";
    public final static String CODE_ASSERT="CODE.ASSERT";
    public final static String CODE_WARN="CODE.WARN";
    public final static String CODE_FAIL="CODE.FAIL";
    public final static String CODE_DEBUG="CODE.DEBUG";

    /*-------------------------------------------------------------------*/
    public static char TIMESTAMP = 't';
    public static char LABEL = 'L';
    public static char TAG = 'T';
    public static char STACKSIZE = 's';
    public static char STACKTRACE = 'S';
    public static char ONELINE = 'O';

    
    /*-------------------------------------------------------------------*/
    public LogSink[] _sinks = null;
    public String _logOptions=null;

    /*-------------------------------------------------------------------*/
    private static Log __instance=null;

    /*-------------------------------------------------------------------*/
    /** Shared static instances, reduces object creation at expense
     * of lock contention in multi threaded debugging */
    private static StringBuffer __stringBuffer = new StringBuffer(512);
    
    /*-------------------------------------------------------------------*/
    public static Log instance()
    {    
        if (__instance==null)
        {
            synchronized(com.mortbay.Util.Log.class)
            {
                if (__instance==null)
		{
                    __instance = new Log();
		    
		    String logOptions = "tLTs";
		    String logFile = null;
		    String dateFormat = "yyyyMMdd HHmmss.SSS zzz ";
		    String timezone = "GMT";
		    try {
			logOptions = System.getProperty("LOG_OPTIONS","tLTs");
			logFile = System.getProperty("LOG_FILE");
			dateFormat = System.getProperty("LOG_DATE_FORMAT",
							"yyyyMMdd HHmmss.SSS zzz ");
			timezone = System.getProperty("LOG_TIMEZONE","GMT");
		    }
		    catch (Exception ex){
			System.err.println("Exception from getProperty - probably running in applet\nUse Log.initParamsFromApplet or Log.setOptions to control debug output.");
		    }
		    
		    LogSink sink= null;
		    try
		    {
			if(logFile==null)
			    sink=new LogSink();
			else
			    sink=new FileLogSink(logFile);
		    }
		    catch(IOException e)
		    {
			e.printStackTrace();
			sink=new LogSink();
		    }
		    
		    sink.setOptions(dateFormat,timezone,
				    (logOptions.indexOf(TIMESTAMP) >= 0),
				    (logOptions.indexOf(LABEL) >= 0),
				    (logOptions.indexOf(TAG) >= 0),
				    (logOptions.indexOf(STACKSIZE) >= 0),
				    (logOptions.indexOf(STACKTRACE) >= 0),
				    (logOptions.indexOf(ONELINE) >= 0));
		    
		    __instance.add(sink);
		}
            }
        }
        return __instance;
    }

    
    /* ------------------------------------------------------------ */
    /** Initialize default behaviour from applet parameters
     *
     * Initializes the default instance from  applet parameters of the
     * same name as the system properties used to config Log
     * @param appl Applet
     */
    public static void initParamsFromApplet(java.applet.Applet appl)
    {
	synchronized(com.mortbay.Util.Log.class)
	{
	    if (__instance==null)
		__instance = new Log();
		
	    String logOptions = appl.getParameter("LOG_OPTIONS");
	    String logFile = appl.getParameter("LOG_FILE");
	    String dateFormat = appl.getParameter("LOG_DATE_FORMAT");
	    String timezone = appl.getParameter("LOG_TIMEZONE");

	    if (logOptions==null)
		logOptions="tLTs";
	    
	    LogSink sink= null;
	    try
	    {
		if(logFile==null)
		    sink=new LogSink();
		else
		    sink=new FileLogSink(logFile);
	    }
	    catch(IOException e)
	    {
		e.printStackTrace();
		sink=new LogSink();
	    }
	
	    sink.setOptions((dateFormat!=null)
			    ?dateFormat:"yyyyMMdd HHmmss.SSS zzz ",
			    (timezone!=null)?timezone:"GMT",
			    (logOptions.indexOf(TIMESTAMP) >= 0),
			    (logOptions.indexOf(LABEL) >= 0),
			    (logOptions.indexOf(TAG) >= 0),
			    (logOptions.indexOf(STACKSIZE) >= 0),
			    (logOptions.indexOf(STACKTRACE) >= 0),
			    (logOptions.indexOf(ONELINE) >= 0));
	    
	    __instance.add(sink);
	}
    }

    /*-------------------------------------------------------------------*/
    /** Construct the shared instance of Log that decodes the
     * options setup in the environments properties.
     */
    private Log()
    {}

    /* ------------------------------------------------------------ */
    /** Add a Log Sink.
     * @param logSink 
     */
    public synchronized void add(LogSink logSink)
    {
	if (_sinks==null)
	{
	    _sinks=new LogSink[1];
	    _sinks[0]=logSink;
	}
	else
	{
	    LogSink[] ns = new LogSink[_sinks.length+1];
	    for (int i=_sinks.length;i-->0;)
		ns[i]=_sinks[i];
	    ns[_sinks.length]=logSink;
	    _sinks=ns;
	}
    }

    
    /* ------------------------------------------------------------ */
    /** No logging.
     * All log sinks are removed.
     */
    public void disableLog()
    {
        _sinks=null;
    }

    
    /*-------------------------------------------------------------------*/
    public static synchronized void message(String tag,
                                            String msg,
                                            Frame frame)
    {
        long time = System.currentTimeMillis();
        instance().message(tag,msg,frame,time);
    }
    
    /* ------------------------------------------------------------ */
    /** Log a message
     * @param tag Tag for type of log
     * @param msg The message
     * @param frame The frame that generated the message.
     * @param time The time stamp of the message.
     */
    public void message(String tag,
                        String msg,
                        Frame frame,
                        long time)
    {
	if (_sinks==null)
	    return;
	for (int s=_sinks.length;s-->0;)
	    _sinks[s].log(tag,msg,frame,time);
    }

    /* ------------------------------------------------------------ */
    /** Log an event
     */
    public static void event(String message, int stackDepth)
    {
        Log.message(Log.EVENT,message,new Frame(stackDepth));
    }
    
    /* ------------------------------------------------------------ */
    /** Log an event
     */
    public static void event(String message)
    {
        Log.message(Log.EVENT,message,new Frame(1));
    }
    
    /* ------------------------------------------------------------ */
    /** Log an warning
     */
    public static void warning(String message, int stackDepth)
    {
        Log.message(Log.WARN,message,new Frame(1));
    }
    
    /* ------------------------------------------------------------ */
    /** Log an warning
     */
    public static void warning(String message)
    {
        Log.message(Log.WARN,message,new Frame(1));
    }
}

