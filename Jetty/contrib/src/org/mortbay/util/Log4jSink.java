// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package org.mortbay.util;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.BasicConfigurator;


public class Log4jSink implements LogSink
{
    private String _options;
    private transient boolean _started;
    
    /* ------------------------------------------------------------ */
    public void setOptions(String filename)
    {
        _options=filename;
    }
    
    /* ------------------------------------------------------------ */
    public String getOptions()
    {
        return _options;
    }
    
    /* ------------------------------------------------------------ */
    public  void start()
        throws Exception
    {
        if (_options==null)
            BasicConfigurator.configure();
        else
            DOMConfigurator.configure(Resource.newResource(_options).getURL());
        _started=true;
    }
    
    /* ------------------------------------------------------------ */
    public  void stop()
    {
        _started=false;
    }

    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _started;
    }
    
    /* ------------------------------------------------------------ */
    public  void log(String tag,
                     Object msg,
                     Frame frame,
                     long time)
    {
        String method=frame.getMethod();
        int lb=method.indexOf('(');
        int ld = (lb>0)
            ?method.lastIndexOf('.',lb)
            :method.lastIndexOf('.');
        if (ld<0) ld=lb;
        String class_name = (ld>0)?method.substring(0,ld):method;
        
        Logger log = Logger.getLogger(class_name);

        Priority priority=Priority.INFO;

        if (Log.DEBUG.equals(tag))
            priority=Priority.DEBUG;
        else if (Log.WARN.equals(tag) || Log.ASSERT.equals(tag))
            priority=Priority.ERROR;
        else if (Log.FAIL.equals(tag))
            priority=Priority.FATAL;
        
        if (!log.isEnabledFor(priority))
            return;

        log.log("org.mortbay.util.Log4jSink",
                priority,
                msg.toString(),
                null);
    }

    /* ------------------------------------------------------------ */
    public  synchronized void log(String s)
    {
        Logger.getRootLogger().log("org.mortbay.util.Log4jSink",
                                   Priority.INFO,
                                   s,
                                   null);
    }
    
}
