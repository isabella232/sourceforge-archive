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
    public static char TIMESTAMP = 't';
    public static char LABEL = 'L';
    public static char TAG = 'T';
    public static char STACKSIZE = 's';
    public static char STACKTRACE = 'S';
    public static char ONELINE = 'O';

    /*-------------------------------------------------------------------*/
    public final static String EVENT="LOG.EVENT";
    public final static String WARN="LOG.WARN";
    public final static String CODE_ASSERT="CODE.ASSERT";
    public final static String CODE_WARN="CODE.WARN";
    public final static String CODE_FAIL="CODE.FAIL";
    public final static String CODE_DEBUG="CODE.DEBUG";

    /*-------------------------------------------------------------------*/
    private static final String __lineSeparator = System.getProperty("line.separator");
    private static final int __lineSeparatorLen = __lineSeparator.length();
    
    /*-------------------------------------------------------------------*/
    public String _logOptions=null;
    public boolean _logTimeStamps=true;
    public boolean _logLabels=true;
    public boolean _logTags=true;
    public boolean _logStackSize=true;
    public boolean _logStackTrace=false;
    public boolean _logOneLine=false;
    public PrintWriter _out = null;;
    public DateCache _dateFormat=null;

    /*-------------------------------------------------------------------*/
    private static Log __instance=null;

    private static String __indent =
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";

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
                    new Log();
            }
        }
        return __instance;
    }    
    
    /*-------------------------------------------------------------------*/
    /** Construct the shared instance of Log that decodes the
     * options setup in the environments properties.
     */
    public Log()
    {
        __instance = this;
        String logOptions = null;
        String logFile = null;
        String dateFormat = null;
        String timezone = null;
        try {
            logOptions = System.getProperty("LOG_OPTIONS");
            logFile = System.getProperty("LOG_FILE");
            dateFormat = System.getProperty("LOG_DATE_FORMAT");
            timezone = System.getProperty("LOG_TIMEZONE");
        }
        catch (Exception ex){
            System.err.println("Exception from getProperty - probably running in applet\nUse Log.initParamsFromApplet or Log.setOptions to control debug output.");
        }

        setOptions(logOptions,logFile,dateFormat,timezone);
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
        String lo = appl.getParameter("LOG_OPTIONS");
        String lf = appl.getParameter("LOG_FILE");
        String df = appl.getParameter("LOG_DATE_FORMAT");
        String tz = appl.getParameter("LOG_TIMEZONE");
        instance().setOptions(lo,lf,df,tz);
    }

    /* ------------------------------------------------------------ */
    /**  Set the log options
     * @param logOptions A string of characters as defined for the
     * LOG_OPTIONS system parameter.
     * @param logFile log file name. Null for stderr.
     * @param dateFormat Simple date format string for timestamps
     * @param timezone Time zone for timestamps
     */
    public void setOptions(String logOptions,
                           String logFile,
                           String dateFormat,
                           String timezone)
    {
        setOptions(logOptions);

        if (dateFormat!=null && dateFormat.trim().length()>0)
        {
            dateFormat=dateFormat.replace('+',' ');
            _dateFormat = new DateCache(dateFormat);
            if (timezone==null || timezone.length()==0)
                timezone="GMT";
            _dateFormat.getFormat().setTimeZone(TimeZone.getTimeZone(timezone));
        }
        else
            _dateFormat=new DateCache("yyyyMMdd HHmmss.SSS zzz ");
        
        try {
            if (logFile==null)
                _out=new PrintWriter(System.err);
            else
            {
                try {
                    FileOutputStream fos = new FileOutputStream(logFile);
                    _out = new PrintWriter(fos, true);
                } catch (Exception ex){
                    Code.fail("Error writing to LOG_FILE:"+logFile, ex);
                    System.exit(1);
                }
            }
        } catch (Exception ex){
            System.err.println("Log problem!");
            ex.printStackTrace();
        }
    }


    /* ------------------------------------------------------------ */
    /** No logging.
     * Logging is disabled with this call.
     */
    public void disableLog()
    {
        _out=null;
    }
    
    /*-------------------------------------------------------------------*/
    /** Set the log options
     *
     * @param logOptions A string of characters as defined for the
     * LOG_OPTIONS system parameter.
     */
    public void setOptions(String logOptions)
    {
        _logOptions = logOptions;
        
        if (logOptions != null)
        {
            _logTimeStamps      = (logOptions.indexOf(TIMESTAMP) >= 0);
            _logLabels          = (logOptions.indexOf(LABEL) >= 0);
            _logTags            = (logOptions.indexOf(TAG) >= 0);
            _logStackSize       = (logOptions.indexOf(STACKSIZE) >= 0);
            _logStackTrace      = (logOptions.indexOf(STACKTRACE) >= 0);
            _logOneLine         = (logOptions.indexOf(ONELINE) >= 0);
        } else {
            _logTimeStamps      = true;
            _logLabels          = true;
            _logTags            = true;
            _logStackSize       = true;
            _logStackTrace      = false;
            _logOneLine         = false;
            _logOptions         = "tLTs";
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Get the current log options
     * @return the log options strings
     */
    public String getOptions()
    {
        return _logOptions;
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
        if (_out==null)
            return;
        
        // Lock static buffer
        synchronized(__stringBuffer)
        {
            __stringBuffer.setLength(0);
            
            // Log the time stamp
            if (_logTimeStamps)
            {
                if (_dateFormat!=null)
                    __stringBuffer.append(_dateFormat.format(new Date(time)));
                else
                {
                    String mSecs = "0000" + time%1000L;
                    mSecs = mSecs.substring(mSecs.length() - 3);
                    __stringBuffer.append(Long.toString(time / 1000L));
                    __stringBuffer.append('.');
                    __stringBuffer.append(mSecs);
                }
            }
        
            // Log the label
            if (_logLabels)
            {
                __stringBuffer.append(frame.toString());
                __stringBuffer.append(':');
            }
            
            // Log the tag
            if (_logTags)
                __stringBuffer.append(tag);

            
            // Determine the indent string for the message
            String indent = _logOneLine?"\\n ":"\n  ";
            if (_logStackSize)
                indent += __indent.substring(0,frame._depth)+" ";
            __stringBuffer.append(indent);
            
            // Add stack frame to message
            if (_logStackTrace)
                msg = msg + "\n" + frame._stack;

            // Log indented message
            int i=0;
            int last=0; 
            while ((i=msg.indexOf(__lineSeparator,i))>=last)
            {
                __stringBuffer.append(msg.substring(last,i));
                __stringBuffer.append(indent);
                i+=__lineSeparatorLen;
                last=i;
            }
            if (msg.length()>last)
                __stringBuffer.append(msg.substring(last));

            _out.println(__stringBuffer.toString());
            _out.flush();
        }
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

