// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/* ------------------------------------------------------------ */
/**  Date Format Cache.
 * Computes String representations of Dates and caches
 * the results
 * so that subsequent requests within the same minute will be fast.
 *
 * Only format strings that contain either "ss" or "ss.SSS" are
 * handled.
 *
 * If consecutive calls are frequently very different, then this
 * may be a little slower than a normal DateFormat.
 *
 * @version $Id$
 * @author Kent Johnson <KJohnson@transparent.com>
 * @author Greg Wilkins (gregw)
 */

public class DateCache  
{
    private String formatString;
    private SimpleDateFormat minuteFormat;
    private SimpleDateFormat format;
    private boolean millis=false;
    private long lastMinutes = -1;
    private long lastSeconds = -1;
    private String lastResult = null;
        
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Make a DateCache that will use a default format. The default format
     * generates the same results as Date.toString().
     */
    public DateCache()
    {
        this("EEE MMM dd HH:mm:ss zzz yyyy");
        getFormat().setTimeZone(TimeZone.getDefault());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Make a DateCache that will use the given format
     */
    public DateCache(String format)
    {
        formatString=format;
        this.minuteFormat=new SimpleDateFormat(mFormat(format));
    }
    
    /* ------------------------------------------------------------ */
    public DateCache(String format,Locale l)
    {
        formatString=format;
        this.minuteFormat=new SimpleDateFormat(mFormat(format),l);
    }
    
    /* ------------------------------------------------------------ */
    public DateCache(String format,DateFormatSymbols s)
    {
        formatString=format;
        this.minuteFormat=new SimpleDateFormat(mFormat(format),s);
    }

    /* ------------------------------------------------------------ */
    private String mFormat(String format)
    {
        int i = format.indexOf("ss.SSS");
        int l = 6;
        if (i>=0)
            millis=true;
        else
        {
            l=2;
            i = format.indexOf("ss");
        }
        
        Code.assert(i>=0,"No seconds in format");

        // Build a formatter that formats a second format string
        // Have to replace @ with ' later due to bug in SimpleDateFormat
        String ss1=format.substring(0,i);
        String ss2=format.substring(i+l);
        String mFormat =
            (i>0?("@"+ss1+"@"):"")+
            (l==2?"'ss'":"'ss.SSS'")+
            (ss2.length()>0?("@"+ss2+"@"):"");
        return mFormat;
    }

    /* ------------------------------------------------------------ */
    /** Format a date according to our stored formatter.
     * @param inDate 
     * @return Formatted date
     */
    public synchronized String format(Date inDate)
    {
        return format(inDate.getTime());
    }
    
    /* ------------------------------------------------------------ */
    /** Format a date according to our stored formatter.
     * @param inDate 
     * @return Formatted date
     */
    public synchronized String format(long inDate)
    {
        
        // Check if we are in the same second
        // and don't care about millis
        long seconds = inDate / 1000;
        if (lastSeconds==seconds && !millis)
            return lastResult;

        Date d = new Date(inDate);
        
        // Check if we need a new format string
        long minutes = seconds/60;
        if (lastMinutes != minutes)
        {
            format=new SimpleDateFormat(minuteFormat.format(d)
                                        .replace('@','\''));
            lastMinutes = minutes;
        }

        // Always format if we get here
        lastSeconds = seconds;
        lastResult = format.format(d);
                
        return lastResult;
    }

    /* ------------------------------------------------------------ */
    /** Format to string buffer. 
     * @param inDate Date the format
     * @param buffer StringBuffer
     */
    public void format(long inDate, StringBuffer buffer)
    {
        buffer.append(format(inDate));
    }
    
    /* ------------------------------------------------------------ */
    /** Get the format.
     */
    public SimpleDateFormat getFormat()
    {
        return minuteFormat;
    }

    /* ------------------------------------------------------------ */
    public String getFormatString()
    {
        return formatString;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the timezone.
     * @param tz TimeZone
     */
    public void setTimeZone(TimeZone tz)
    {
        minuteFormat.setTimeZone(tz);
        if (format!=null)
            format.setTimeZone(tz);
    }
}       


