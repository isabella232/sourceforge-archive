// ========================================================================
// Copyright (c) Kent Johnson <KJohnson@transparent.com>
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/* ------------------------------------------------------------ */
/**  DateCache.
 * Computes String representations of Dates and caches
 * the results
 * so that subsequent requests within the same minute will be fast.
 *
 * @see
 * @version 1.0 Wed Mar  3 1999
 * @author Kent Johnson <KJohnson@transparent.com>
 */

public final class DateCache 
{
    private long lastSeconds = 0;
    private String lastResult = null;
    private SimpleDateFormat formatter;
	
    /** Make a DateCache that will use the given format */
    public DateCache(SimpleDateFormat formatter)
    {
	this.formatter = formatter;
    }

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Make a DateCache that will use a default format. The default format
     * generates the same results as Date.toString().
     */
    public DateCache()
    {
	this(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"));
	formatter.setTimeZone(TimeZone.getDefault());
    }

    /* ------------------------------------------------------------ */
    /** Format a date according to our stored formatter.
     * @param inDate 
     * @return Formatted date
     */
    public synchronized String format(Date inDate)
    {
	long seconds = inDate.getTime() / 1000;
	if (lastSeconds != seconds)
	{
	    lastSeconds = seconds;
	    lastResult = formatter.format(inDate);
	}
		
	return lastResult;
    }
}	
