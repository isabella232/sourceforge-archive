package org.gjt.jsp;

import java.util.ResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * This class contains global configuration data
 * about running GNUJSP instance.
 */

public class JspConfig {
    private static ResourceBundle rb = null;
    private static Locale locale = Locale.getDefault();
    private static boolean init = false;

    private static final String RESOURCE_NAME = 
	"org.gjt.jsp.prop.JspMsgRes";

    /**
     * Set locale for GNUJSP to argument locale.  Without calling this
     * method, GNUJSP will use the default locale of the JVM.
     */
    public static void setLocale(Locale xlocale)
    {
	locale = xlocale;
    }

    /**
     * Returns a localized message from the JspMsgRes.properties file
     * for the locale set by calling setLocale (or the JVM's default
     * locale if not explicitly set.)  If a message for this message
     * number is not available, the number itself will be returned as
     * a string.
     */
    public static String getLocalizedMsg(int msgno) {
	String msgnum = "M"+formatNum(msgno);
	try {
	    // Execute this on first run.
	    if (!init) {
		// try this once only
		init = true;
		rb = ResourceBundle.getBundle(RESOURCE_NAME, locale);
	    }
	    if(rb != null)
		return rb.getString(msgnum);
	}
	catch(MissingResourceException e)
	{
	    System.err.println(e.toString());
	    return e.toString();
	}
	return msgnum;
    }

    private static String formatNum(int n) {
	if(n < 10) return "00"+n;
	else if(n < 100) return "0"+n;
	else return ""+n;
    }
}
