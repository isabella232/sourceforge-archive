package org.mortbay.jndi.java;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import org.mortbay.util.Code;


/** javaURLContextFactory
 * <p>This is the URL context factory for the java: URL.
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
* </pre>
*
* @see
*
* @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
* @version 1.0
*/
public class javaURLContextFactory implements ObjectFactory 
{
    
    /**
     * Either return a new context or the resolution of a url.
     *
     * @param url an <code>Object</code> value
     * @param name a <code>Name</code> value
     * @param ctx a <code>Context</code> value
     * @param env a <code>Hashtable</code> value
     * @return a new context or the resolved object for the url
     * @exception Exception if an error occurs
     */
    public Object getObjectInstance(Object url, Name name, Context ctx, Hashtable env)
        throws Exception 
    {
        // null object means return a root context for doing resolutions
        if (url == null)
        {
            Code.debug (">>> new root context requested ");
            return new javaRootURLContext(env);
        }
        
        // return the resolution of the url
        if (url instanceof String)
        {
            Code.debug (">>> resolution of url "+url+" requested");
            Context rootctx = new javaRootURLContext (env);
            return rootctx.lookup ((String)url);
        }

        // return the resolution of at least one of the urls
        if (url instanceof String[])
        {
            Code.debug (">>> resolution of array of urls requested");
            String[] urls = (String[])url; 
            Context rootctx = new javaRootURLContext (env);
            Object object = null;
            NamingException e = null;
            for (int i=0;(i< urls.length) && (object == null); i++)
            {
                try
                {
                    object = rootctx.lookup (urls[i]);
                }
                catch (NamingException x)
                {
                    e = x;
                }
            }

            if (object == null)
                throw e;
            else
                return object;
        }

        Code.debug (">>> No idea what to do, so return a new root context anyway");
        return new javaRootURLContext (env);
    }
};
