package org.mortbay.jndi;


import java.util.Hashtable;
import java.util.WeakHashMap;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * CompContextFactory.java
 *
 *
 * Created: Fri Jun 27 09:26:40 2003
 *
 * @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
 * @version 1.0
 */
public class ContextFactory implements ObjectFactory
{
    private static Log log = LogFactory.getLog(ContextFactory.class);


    //map of classloaders to contexts
    private static WeakHashMap _contextMap;

    private static NameParser _parser;


    static
    {
        _contextMap = new WeakHashMap();
    }
    
    public static void setNameParser (NameParser parser)
    {
        _parser = parser;
    }

    public Object getObjectInstance (Object obj,
                                     Name name,
                                     Context nameCtx,
                                     Hashtable env)
        throws Exception
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Context ctx = (Context)_contextMap.get(loader);
        
        //the map does not contain an entry for this classloader
        if (ctx == null)
        {
            //check if a parent classloader has created the context
            ctx = getParentClassLoaderContext(loader);

            if (ctx == null)
            {
                ctx = new NamingContext (env,
                                         name.get(0),
                                         nameCtx,
                                         _parser);
                if(log.isDebugEnabled())log.debug("No entry for classloader: "+loader);
                _contextMap.put (loader, ctx);
            }
        }

        return ctx;
    }

    public Context getParentClassLoaderContext (ClassLoader loader)
    {
        Context ctx = null;
        ClassLoader cl = loader;
        for (cl = cl.getParent(); (cl != null) && (ctx == null); cl = cl.getParent())
        {
            ctx = (Context)_contextMap.get(cl);
        }

        return ctx;
    }
} 
