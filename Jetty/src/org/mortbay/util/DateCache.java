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
 * the results so that subsequent requests within the same minute
 * will be fast.
 *
 * Only format strings that contain either "ss" or "ss.SSS" are
 * handled.
 *
 * The timezone of the date may be included as an ID with the "zzz"
 * format string or as an offset with the "ZZZ" format string.
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
    private String _formatString;
    private SimpleDateFormat _minuteFormat;
    private SimpleDateFormat _format;
    private boolean _millis=false;
    private long _lastMinutes = -1;
    private long _lastSeconds = -1;
    private String _lastResult = null;

    private Locale _locale	= null;
    private DateFormatSymbols	_dfs	= null;

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
        _formatString=format;
        this._minuteFormat=new SimpleDateFormat(mFormat(format,TimeZone.getDefault()));
    }
    
    /* ------------------------------------------------------------ */
    public DateCache(String format,Locale l)
    {
        _formatString=format;
		_locale = l;
        this._minuteFormat=new SimpleDateFormat(mFormat(format,TimeZone.getDefault()),l);
    }
    
    /* ------------------------------------------------------------ */
    public DateCache(String format,DateFormatSymbols s)
    {
        _formatString=format;
		_dfs = s;
        this._minuteFormat=new SimpleDateFormat(mFormat(format,TimeZone.getDefault()),s);
    }

    /* ------------------------------------------------------------ */
    private String getOffsetString( int msOffset )
    {
        StringBuffer sb = new StringBuffer( "'" );
        if( msOffset >= 0 )
            sb.append( '+' );
        else
        {
            msOffset *= -1;
            sb.append( '-' );
        }
        
        int raw = msOffset / (1000*60);		// Convert to seconds
        int hr = raw / 60;
        int min = raw % 60;
        
        if( hr < 10 )
            sb.append( '0' );
        
        sb.append( hr );
        sb.append( ':' );
        
        // Would this really ever happen?
        if( min < 10 )
            sb.append( '0' );
        
        sb.append( min );
        sb.append( '\'' );
        
        return sb.toString();
    }

    /* ------------------------------------------------------------ */
    private String mFormat( String format, TimeZone tz )
    {
        int zIndex = format.indexOf( "ZZZ" );
        if( zIndex >= 0 ) {
            String ss1 = format.substring( 0, zIndex );
            String ss2 = format.substring( zIndex+3 );			// Add the length of ZZZ
            format = ss1 + getOffsetString( tz.getRawOffset() ) + ss2;
        }

        int i = format.indexOf("ss.SSS");
        int l = 6;
        if (i>=0)
            _millis=true;
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
        if (_lastSeconds==seconds && !_millis)
            return _lastResult;

        Date d = new Date(inDate);
        
        // Check if we need a new format string
        long minutes = seconds/60;
        if (_lastMinutes != minutes)
        {
            _format=new SimpleDateFormat(_minuteFormat.format(d)
                                        .replace('@','\''));
            _lastMinutes = minutes;
        }

        // Always format if we get here
        _lastSeconds = seconds;
        _lastResult = _format.format(d);
                
        return _lastResult;
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
        return _minuteFormat;
    }

    /* ------------------------------------------------------------ */
    public String getFormatString()
    {
        return _formatString;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the timezone.
     * @param tz TimeZone
     */
    public void setTimeZone(TimeZone tz)
    {
        if( _locale != null ) 
            _minuteFormat = new SimpleDateFormat( mFormat( _formatString, tz ), _locale );
        else if( _dfs != null ) 
            _minuteFormat = new SimpleDateFormat( mFormat( _formatString, tz ), _dfs );
        else 
            _minuteFormat = new SimpleDateFormat( mFormat( _formatString, tz ) );

        _minuteFormat.setTimeZone(tz);
        if (_format!=null)
            _format.setTimeZone(tz);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the timezone.
     * @param tz TimeZoneId the ID of the zone as used by TimeZone.getTimeZone(id)
     */
    public void setTimeZoneID(String timeZoneId)
    {
        setTimeZone(TimeZone.getTimeZone(timeZoneId));
    }
    
}
