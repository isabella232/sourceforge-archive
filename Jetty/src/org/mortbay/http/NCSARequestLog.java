// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import org.mortbay.util.Code;
import java.util.TimeZone;
import org.mortbay.util.RolloverFileOutputStream;
import org.mortbay.util.DateCache;
import org.mortbay.util.ByteArrayISO8859Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;


/* ------------------------------------------------------------ */
/** NCSA HTTP Request Log.
 * NCSA common or NCSA extended (combined) request log.
 * @version $Id$
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public class NCSARequestLog implements RequestLog
{
    private String _filename;
    private DateCache _logDateCache;
    private boolean _extended;
    private boolean _append;
    private int _retainDays;
    private OutputStream _fileOut;
    private boolean _closeFileOut;
    private ByteArrayISO8859Writer _buf=new ByteArrayISO8859Writer();
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @exception IOException
     */
    public NCSARequestLog()
        throws IOException
    {
        _logDateCache=new DateCache("dd/MMM/yyyy:HH:mm:ss ZZZ",Locale.US);
        _logDateCache.setTimeZoneID(TimeZone.getDefault().getID());
        _extended=true;
        _append=true;
        _retainDays=31;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param filename Filename, which can be in
     * rolloverFileOutputStream format
     * @see org.mortbay.util.RolloverFileOutputStream
     * @exception IOException 
     */
    public NCSARequestLog(String filename)
        throws IOException
    {
        this();
        setFilename(filename);
    }


    /* ------------------------------------------------------------ */
    public void setFilename(String filename)
    {
        if (filename!=null)
        {
            filename=filename.trim();
            if (filename.length()==0)
                filename=null;
        }
        _filename=filename;        
    }

    /* ------------------------------------------------------------ */
    public String getFilename()
    {
        return _filename;
    }

    /* ------------------------------------------------------------ */
    public String getDatedFilename()
    {
        if (_fileOut instanceof RolloverFileOutputStream)
            return ((RolloverFileOutputStream)_fileOut).getDatedFilename();
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public void setLogDateFormat(String format)
    {
        TimeZone tz=_logDateCache.getTimeZone();
        _logDateCache=new DateCache(format);
        _logDateCache.setTimeZone(tz);
    }

    /* ------------------------------------------------------------ */
    public String getLogDateFormat()
    {
        return _logDateCache.getFormatString();
    }
    
    /* ------------------------------------------------------------ */
    public void setLogTimeZone(String tz)
    {
        _logDateCache.setTimeZone(TimeZone.getTimeZone(tz));
    }

    /* ------------------------------------------------------------ */
    public String getLogTimeZone()
    {
        return _logDateCache.getTimeZone().getID();
    }
    
    /* ------------------------------------------------------------ */
    public int getRetainDays()
    {
        return _retainDays;
    }

    /* ------------------------------------------------------------ */
    public void setRetainDays(int retainDays)
    {
        _retainDays = retainDays;
    }

    /* ------------------------------------------------------------ */
    public boolean isExtended()
    {
        return _extended;
    }

    /* ------------------------------------------------------------ */
    public void setExtended(boolean e)
    {
        _extended=e;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isAppend()
    {
        return _append;
    }

    /* ------------------------------------------------------------ */
    public void setAppend(boolean a)
    {
        _append=a;
    }
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        if (_filename != null)
        {
            _fileOut=new RolloverFileOutputStream(_filename,_append,_retainDays);
            _closeFileOut=true;
        }
        else
            _fileOut=System.err;
    }

    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _fileOut!=null;
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
    {
        if (_fileOut!=null && _closeFileOut)
            try{_fileOut.close();}catch(IOException e){Code.ignore(e);}
        _fileOut=null;
        _closeFileOut=false;
    }
    
    /* ------------------------------------------------------------ */
    public void log(HttpRequest request,
                    HttpResponse response,
                    int responseLength)
    {
        try{
            synchronized(_buf.getLock())
            {
                if (_fileOut==null)
                    return;
                
                _buf.write(request.getRemoteAddr());
                _buf.write(" - ");
                String user = request.getAuthUser();
                _buf.write((user==null)?"-":user);
                _buf.write(" [");
                _buf.write(_logDateCache.format(request.getTimeStamp()));
                _buf.write("] \"");
                request.writeRequestLine(_buf);
                _buf.write("\" ");
                int status=response.getStatus();    
                _buf.write('0'+((status/100)%10));
                _buf.write('0'+((status/10)%10));
                _buf.write('0'+(status%10));
                if (responseLength>=0)
                {
                    _buf.write(' ');
                    if (responseLength>99999)
                        _buf.write(Integer.toString(responseLength));
                    else
                    {
                        if (responseLength>9999) 
                            _buf.write('0'+((responseLength/10000)%10));
                        if (responseLength>999) 
                            _buf.write('0'+((responseLength/1000)%10));
                        if (responseLength>99) 
                            _buf.write('0'+((responseLength/100)%10));
                        if (responseLength>9) 
                            _buf.write('0'+((responseLength/10)%10));
                        _buf.write('0'+(responseLength%10));
                    }
                    _buf.write(' ');
                }
                else
                    _buf.write(" - ");
                
                if (_extended)
                {
                    String referer = request.getField(HttpFields.__Referer);
                    if(referer==null)
                        _buf.write("\"-\" ");
                    else
                    {
                        _buf.write('"');
                        _buf.write(referer);
                        _buf.write("\" ");
                    }
                    
                    String agent = request.getField(HttpFields.__UserAgent);
                    if(agent==null)
                    _buf.write("\"-\"");
                    else
                    {
                        _buf.write('"');
                        _buf.write(agent);
                        _buf.write('"');
                    }
                }
                _buf.write("\n");
                _buf.flush();
                _buf.writeTo(_fileOut);
                _buf.reset();
            }
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
    }
}

