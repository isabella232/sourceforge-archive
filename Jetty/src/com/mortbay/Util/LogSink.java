// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.util.*;
import java.io.*;
import java.text.*;


/* ------------------------------------------------------------ */
/** A Log sink.
 * This class represents both a concrete or abstract sink of
 * Log data.  The default implementation logs to a PrintWriter, but
 * derived implementations may log to files, syslog, or other
 * logging APIs.
 *
 * @see
 * @version 1.0 Sun Apr 23 2000
 * @author Greg Wilkins (gregw)
 */
public class LogSink 
{
    /*-------------------------------------------------------------------*/
    protected DateCache _dateFormat=null;
    protected boolean _logTimeStamps=true;
    protected boolean _logLabels=true;
    protected boolean _logTags=true;
    protected boolean _logStackSize=true;
    protected boolean _logStackTrace=false;
    protected boolean _logOneLine=false;
    
    /*-------------------------------------------------------------------*/
    private PrintWriter _out=null;
    
    /*-------------------------------------------------------------------*/
    private StringBuffer __stringBuffer = new StringBuffer(512);
    
    /* ------------------------------------------------------------ */
    private static final String __lineSeparator =
	System.getProperty("line.separator");
    private static final String __indentBase = 
    "  ";
    private static final String __indentSeparator =
	__lineSeparator+__indentBase;
    private static final int __lineSeparatorLen =
	__lineSeparator.length();
    private static String __indent =
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";

    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public LogSink()
    {
	_out=new PrintWriter(System.err);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param out 
     */
    public LogSink(PrintWriter out)
    {
	_out=out;
    }
    
    /*-------------------------------------------------------------------*/
    /** Set the log options
     *
     * @param logOptions A string of characters as defined for the
     * LOG_OPTIONS system parameter.
     */
    public void setOptions(String dateFormat,
                           String timezone,
			   boolean logTimeStamps,
			   boolean logLabels,
			   boolean logTags,
			   boolean logStackSize,
			   boolean logStackTrace,
			   boolean logOneLine)
    {
	dateFormat=dateFormat.replace('+',' ');
	_dateFormat = new DateCache(dateFormat);
	_dateFormat.getFormat().setTimeZone(TimeZone.getTimeZone(timezone));
        
	_logTimeStamps      = logTimeStamps;
	_logLabels          = logLabels;
	_logTags            = logTags;
	_logStackSize       = logStackSize;
	_logStackTrace      = logStackTrace;
	_logOneLine         = logOneLine;
    }    


    /* ------------------------------------------------------------ */
    /** Log a message.
     * This method formats the log information as a string and calls
     * log(String).  It should only be specialized by a derived
     * implementation if the format of the logged messages is to be changed.
     *
     * @param tag Tag for type of log
     * @param msg The message
     * @param frame The frame that generated the message.
     * @param time The time stamp of the message.
     */
    public void log(String tag,
		    String msg,
		    Frame frame,
		    long time)
    {
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
            if (_logLabels && frame != null)
            {
                __stringBuffer.append(frame.toString());
                __stringBuffer.append(':');
            }
            
            // Log the tag
            if (_logTags)
                __stringBuffer.append(tag);

            
            // Determine the indent string for the message and append it
            // to the buffer. Only put a newline in the buffer if the first
            // line is not blank
            String indent = "";
            String indentSeparator = _logOneLine?"\\n ":__lineSeparator;
            if (__stringBuffer.length() > 0)
            	__stringBuffer.append(indentSeparator);
            __stringBuffer.append(__indentBase);
            
            if (_logStackSize && frame != null) {
            	indent = __indent.substring(0,frame._depth)+" ";
	            __stringBuffer.append(indent);
            }
            indent = indentSeparator + __indentBase + indent;
            
            // Add stack frame to message
            if (_logStackTrace && frame != null)
                msg = msg + __lineSeparator + frame._stack;

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

	    log(__stringBuffer.toString());
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Log a message.
     * The formatted log string is written to the log sink. The default
     * implementation writes the message to a PrintWriter.
     * @param formattedLog 
     */
    public void log(String formattedLog)
    {
	synchronized(_out)
	{
	    _out.println(formattedLog);
	    _out.flush();
	}
    }

    /* ------------------------------------------------------------ */
    protected void setWriter(PrintWriter out)
    {
	_out=out;
    }

    /* ------------------------------------------------------------ */
    /** Stop a log sink.
     * An opportunity for subclasses to clean up. The default
     * implementation does nothing 
     */
    public void stop()
    {
    }
};








