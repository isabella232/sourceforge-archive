// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;
import org.mortbay.util.Log;

/* ------------------------------------------------------------ */
/** Jetty version.
 *
 * This class sets the version data returned in the Server and
 * Servlet-Container headers.   If the
 * java.org.mortbay.http.Version.paranoid System property is set to
 * true, then this information is suppressed.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Version
{
    public static boolean __paranoid = 
        Boolean.getBoolean("java.org.mortbay.http.Version.paranoid");
    
    public static String __Version="Jetty/3";
    public static String __VersionImpl=__Version;
    public static String __VersionDetail="Unknown";
    public static String __ServletEngine="Unknown (Servlet 2.2; JSP 1.1)";

    static
    {
        Package p = Version.class.getPackage();
        if (p!=null)
        {
            __Version="Jetty/"+p.getSpecificationVersion();
            __VersionImpl="Jetty/"+p.getImplementationVersion();
        }
        
        if (!__paranoid)
        {
            __VersionDetail=__VersionImpl+" ("+
                System.getProperty("os.name")+" "+
                System.getProperty("os.version")+" "+
                System.getProperty("os.arch")+")";

            __ServletEngine=__Version+" (JSP 1.1; Servlet 2.2; java "+
                System.getProperty("java.version")+")";
        }

    }
}

