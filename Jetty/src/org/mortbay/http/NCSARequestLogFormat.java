// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;
import org.mortbay.util.DateCache;
import java.util.TimeZone;

/* ------------------------------------------------------------ */
/** NCSA HTTP Request Log format.
 * Formate an NCSA common or NCSA extended (combined) log entries.
 * @version $Id$
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public class NCSARequestLogFormat implements RequestLogFormat
{
    private DateCache _logDateCache;
    private boolean _extended;

    /* ------------------------------------------------------------ */
    public NCSARequestLogFormat()
    {
        this("dd/MMM/yyyy:HH:mm:ss ZZZ",
             TimeZone.getDefault().getID(),
             true);
    }
    
    /* ------------------------------------------------------------ */
    public NCSARequestLogFormat(String logDateFormat,
                                String timeZoneID,
                                boolean extended)
    {
        _logDateCache=new DateCache(logDateFormat);
        _logDateCache.setTimeZoneID(timeZoneID);
        _extended=extended;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @param responseLength 
     * @return 
     */
    public String format(HttpRequest request,
                         HttpResponse response,
                         int responseLength)
    {
        StringBuffer buf = new StringBuffer(256);
        synchronized(buf)
        {
            buf.append(request.getRemoteAddr());
            buf.append(" - ");
            String user = (String)request.getAttribute(HttpRequest.__AuthUser);
            buf.append((user==null)?"-":user);
            buf.append(" [");
            _logDateCache.format(request.getTimeStamp(),buf);
            buf.append("] \"");
            request.appendRequestLine(buf);
            buf.append("\" ");
            buf.append(response.getStatus());
            if (responseLength>=0)
            {
                buf.append(' ');
                buf.append(responseLength);
                buf.append(' ');
            }
            else
                buf.append(" - ");

            if (_extended)
            {
                String referer = request.getField(HttpFields.__Referer);
                if(referer==null)
                    buf.append("- ");
                else
                {
                    buf.append('"');
                    buf.append(referer);
                    buf.append("\" ");
                }
                
                String agent = request.getField(HttpFields.__UserAgent);
                
                if(agent==null)
                    buf.append('-');
                else
                {
                    buf.append('"');
                    buf.append(agent);
                    buf.append('"');
                }
            }
            return buf.toString();
        }
    }
}

