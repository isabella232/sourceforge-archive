// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

/* ------------------------------------------------------------ */
/** Jetty version.
 *
 * This class sets the version data returned in the Server and
 * Servlet-Container headers.   If the
 * java.com.mortbay.HTTP.Version.paranoid System property is set to
 * true, then this information is suppressed.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Version
{
    public static boolean __paranoid = 
        Boolean.getBoolean("java.com.mortbay.HTTP.Version.paranoid");
    
    public static String __Version="Jetty/3.0";
    public static String __VersionDetail="Unknown";
    public static String __ServletEngine="Unknown (Servlet 2.2; JSP 1.1)";

    static
    {
        if (!__paranoid)
        {
            __VersionDetail=__Version+" ("+
                System.getProperty("os.name")+" "+
                System.getProperty("os.version")+" "+
                System.getProperty("os.arch")+")";

            __ServletEngine=__Version+" (JSP 1.1; Servlet 2.2; java "+
                System.getProperty("java.version")+")";
        }
    }
}

