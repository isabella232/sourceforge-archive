package org.mortbay.jndi;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Util.java
 *
 *
 * Created: Tue Jul  1 18:26:17 2003
 *
 * @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
 * @version 1.0
 */
public class Util 
{
    private static Log log = LogFactory.getLog(Util.class);
    
  


    /* ------------------------------------------------------------ */
    /**
     * Bind an object to a context ensuring all subcontexts 
     * are created if necessary
     *
     * @param ctx the context into which to bind
     * @param name the name relative to context to bind
     * @param obj the object to be bound
     * @exception NamingException if an error occurs
     */
    public static void bind (Context ctx, String nameStr, Object obj)
        throws NamingException
    {
        Name name = ctx.getNameParser("").parse(nameStr);

        //no name, nothing to do 
        if (name.size() == 0)
            return;

        Context subCtx = ctx;
        
        //last component of the name will be the name to bind

        for (int i=0; i < name.size() - 1; i++)
        {
            try
            {
                subCtx = (Context)subCtx.lookup (name.get(i));
                if(log.isDebugEnabled())log.debug("Subcontext "+name.get(i)+" already exists");
            }
            catch (NameNotFoundException e)
            {
                subCtx = subCtx.createSubcontext(name.get(i));
                if(log.isDebugEnabled())log.debug("Subcontext "+name.get(i)+" created");
            }
        }

        subCtx.rebind (name.get(name.size() - 1), obj);
        if(log.isDebugEnabled())log.debug("Bound object to "+name.get(name.size() - 1));
    }   
}
