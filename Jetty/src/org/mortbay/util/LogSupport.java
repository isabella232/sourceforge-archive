// ========================================================================
// $Id$
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.util;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

/*-----------------------------------------------------------------------*/
/** Log Support class.
 */
public class LogSupport 
{    
    public final static String IGNORED= "IGNORED ";
    public final static String EXCEPTION= "EXCEPTION ";
    public final static String NOT_IMPLEMENTED= "NOT IMPLEMENTED ";

    private static boolean trace = System.getProperty("TRACE",null)!=null;

    /* ------------------------------------------------------------ */
    /**
     * Ignore an exception unless trace is enabled.
     * This works around the problem that log4j does not support the trace level.
     */
    public static void ignore(Log log,Throwable th)
    {
        if (trace && log.isTraceEnabled()) log.trace(IGNORED,th);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Is Trace Enabled.
     * This works around the problem that log4j does not support the trace level.
     * @return true IFF log.isTraceEnabled() AND ( the system property TRACE is set OR not using log4j )
     */
    public static boolean isTraceEnabled(Log log)
    {
        return log.isTraceEnabled() && (trace || log.getClass().getName().indexOf("Log4J")>=0);
    }

    /*-------------------------------------------------------------------*/
    private static final Class[] __noArgs=new Class[0];
    private static final String[] __nestedEx =
        {"getTargetException","getTargetError","getException","getRootCause"};

    /** Log nested exceptions 
     */
    public static void warn(Log log,String msg, Throwable th)
    {
        log.warn(msg,th);

        if (th==null)
            return;
        for (int i=0;i<__nestedEx.length;i++)
        {
            try
            {
                Method get_target = th.getClass().getMethod(__nestedEx[i],__noArgs);
                Throwable th2=(Throwable)get_target.invoke(th,null);
                if (th2!=null)
                {
                    warn(log,"Nested in "+th+":",th2);
                }
            }
            catch(Exception ignore){}
        }
    }
}

