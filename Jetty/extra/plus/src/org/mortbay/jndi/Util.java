// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jndi;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
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
    
    
    /**
     * Do a deep listing of the bindings for a context.
     * @param ctx the context containing the name for which to list the bindings
     * @param name the name in the context to list
     * @return map: key is fully qualified name, value is the bound object 
     * @throws NamingException
     */
    public static Map flattenBindings (Context ctx, String name)
    throws NamingException
    {
        HashMap map = new HashMap();

        //the context representation of name arg
        Context c = (Context)ctx.lookup (name);
        NameParser parser = c.getNameParser("");
        NamingEnumeration enum = ctx.listBindings(name);
        while (enum.hasMore())
        {
            Binding b = (Binding)enum.next();

            if (b.getObject() instanceof Context)
            {
                map.putAll(flattenBindings (c, b.getName()));
            }
            else
            {
                Name compoundName = parser.parse (c.getNameInNamespace());
                compoundName.add(b.getName());
                map.put (compoundName.toString(), b.getObject());
            }
            
        }
        
        return map;
    }
    
}
