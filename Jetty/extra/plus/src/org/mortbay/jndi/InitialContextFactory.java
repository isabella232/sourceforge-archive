package org.mortbay.jndi;


import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import org.mortbay.util.Code;


/*------------------------------------------------*/    
/**
 * InitialContextFactory.java
 *
 * Factory for the default InitialContext.
 * Created: Tue Jul  1 19:08:08 2003
 *
 * @author <a href="mailto:janb@mortbay.com">Jan Bartel</a>
 * @version 1.0
 */
public class InitialContextFactory implements javax.naming.spi.InitialContextFactory
{
    private static final Hashtable _roots = new Hashtable();

    public static class DefaultParser implements NameParser
    { 
        static Properties syntax = new Properties();   
        static 
        {
            syntax.put("jndi.syntax.direction", "left_to_right");
            syntax.put("jndi.syntax.separator", "/");
            syntax.put("jndi.syntax.ignorecase", "false");
        }
        public Name parse (String name)
            throws NamingException
        {
            return new CompoundName (name, syntax);
        }
    };
    


    /*------------------------------------------------*/    
    /**
     * Get Context that has access to default Namespace.
     * This method won't be called if a name URL beginning
     * with java: is passed to a Context.
     *
     * @see org.mortbay.jndi.java.javaURLContextFactory
     * @param env a <code>Hashtable</code> value
     * @return a <code>Context</code> value
     */
    public Context getInitialContext(Hashtable env) 
    {
        Code.debug("InitialContextFactory.getInitialContext()");

        Context ctx = (Context)_roots.get(env);
        
        Code.debug ("Returning context root: "+ctx);

        if (ctx == null)
        {
            ctx = new NamingContext (env);
            ((NamingContext)ctx).setNameParser(new DefaultParser());
            _roots.put (env, ctx);
            Code.debug("Created new root context:"+ctx);
        }

        return ctx;
    }
} 
