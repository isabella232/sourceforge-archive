// ========================================================================
// Copyright (c) 1997-2003 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package org.mortbay.util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*-----------------------------------------------------------------------*/
/** Log 
 */
public class LogSupport 
{
    private static Log log = LogFactory.getLog(LogSupport.class);
    
    static
    {
        log.info("Log instance is "+log.getClass());
        log.trace("Log from "+log.getClass().getClassLoader());
    }
    
    public final static String IGNORED= "ignored ";
    public final static String EXCEPTION= "exception ";
    public final static String NOT_IMPLEMENTED= "exception ";
}

