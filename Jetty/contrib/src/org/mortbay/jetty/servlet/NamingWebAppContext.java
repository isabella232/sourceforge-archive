// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.xml.XmlParser;
import tyrex.naming.MemoryContext;
import tyrex.tm.RuntimeContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.TypeUtil;

/* ------------------------------------------------------------ */
public class NamingWebAppContext extends WebApplicationContext
{
    MemoryContext _root;
    Context _rootComp;
    Context _rootCompEnv;
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        Hashtable hashtable = new Hashtable();
        
        _root = new MemoryContext(hashtable);
        _rootComp = _root.createSubcontext("comp");
        _rootCompEnv = _rootComp.createSubcontext("env");
        super.start();
    }
        
    /* ------------------------------------------------------------ */
    protected void initWebXmlElement(String element, XmlParser.Node node)
        throws Exception
    {
        // this is ugly - should be dispatched through a hash-table or introspection...

        // these are handled by AbstractWebContainer
        if ("env-entry".equals(element))
        {
            String name=node.getString("env-entry-name",false,true);
            Object value=
                TypeUtil.valueOf(node.getString("env-entry-type",false,true),
                                  node.getString("env-entry-value",false,true));
            bind(_rootCompEnv,name,value);
        }
        else if ("resource-ref".equals(element) ||
                 "resource-env-ref".equals(element) ||
                 "ejb-ref".equals(element) ||
                 "ejb-local-ref".equals(element) ||
                 "security-domain".equals(element))
        {
          System.err.println(element+" => "+node);
        }
        else
            super.initWebXmlElement(element, node);
    }

    
    /* ------------------------------------------------------------ */
    public boolean handle(HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        try
        {
            // Associate the memory context with a new
            // runtime context and associate the runtime context
            // with the current thread
            RuntimeContext runCtx = RuntimeContext.newRuntimeContext( _root, null );
            RuntimeContext.setRuntimeContext(runCtx);
            
            System.err.println("runtime="+runCtx);
            
            return super.handle(request,response);
        }
        catch (NamingException e)
        {
            Code.warning(e);
            throw new HttpException(500);
        }
        finally
        {
            // Dissociate the runtime context from the thread
            RuntimeContext.unsetRuntimeContext();
        }
    }    


    /* ------------------------------------------------------------ */
    private void bind(Context ctx, String name, Object value)
        throws NamingException
    {
        Code.debug("bind in ",ctx," ",name,"=",value);
        System.err.println("bind in "+ctx+" "+name+"="+value);
        Name n = ctx.getNameParser("").parse(name);
        int subs=n.size()-1;
        for (int i=0;i<subs;i++)
        {
            String sub=n.get(i);
            Context subCtx = null;
            try {subCtx = (Context)ctx.lookup(sub);}
            catch(NameNotFoundException e)
            {subCtx = ctx.createSubcontext(sub);}
            ctx=subCtx;
        }
        ctx.bind(n.get(subs),value);
    }
    
}
