package org.mortbay.jetty.plus;

import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jndi.Util;
import org.mortbay.xml.XmlParser;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.TypeUtil;

/* ------------------------------------------------------------ */
public class PlusWebAppContext extends WebApplicationContext
{

    private InitialContext _initialCtx = null;
 


    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception IOException 
     */
    public PlusWebAppContext(
    )
    {
       super();
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param webApp The Web application directory or WAR file.
     * @exception IOException 
     */
    public PlusWebAppContext(
       String webApp
    )
    {
        super(webApp);
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        _initialCtx = new InitialContext();
        super.start();
    }
        
    /* ------------------------------------------------------------ */
    protected void initWebXmlElement(String element, XmlParser.Node node)
        throws Exception
    {           
        // this is ugly - should be dispatched through a hash-table or introspection...
        Context envCtx = (Context)_initialCtx.lookup("java:comp/env");

        if ("env-entry".equals(element))
        {
            String name=node.getString("env-entry-name",false,true);
            Object value= TypeUtil.valueOf(node.getString("env-entry-type",false,true),
                                           node.getString("env-entry-value",false,true));
            Util.bind (envCtx, name, value);
        }
        else if ("resource-ref".equals(element))
        {
            //resource-ref entries are ONLY for connection factories
            //the resource-ref says how the app will reference the jndi lookup relative 
            //to java:comp/env, but it is up to the deployer to map this reference to
            //a real resource in the environment. At the moment, we insist that the 
            //jetty.xml file name of the resource has to be exactly the same as the 
            //name in web.xml deployment descriptor, but it shouldn't have to be
            
            // Lookup the name in the global environment, if found
            // bind it to the local context
            String name=node.getString("res-ref-name",false,true);
            
            Code.debug ("Linking resource-ref java:comp/env/"+name+" to global "+name);
            
            Object o = _initialCtx.lookup (name);
            
            Code.debug ("Found Object in global namespace: "+o.toString());
            Util.bind (envCtx, name,  new LinkRef(name));
        }
        else if ("resource-env-ref".equals(element))
        {
            //resource-env-ref elements are a non-connection factory type of resource
            //the app looks them up relative to java:comp/env
            //again, need a way for deployer to link up app naming to real naming.
            //Again, we insist now that the name of the resource in jetty.xml is
            //the same as web.xml
            
            // Lookup the name in the global environment, if found
            // bind it to the local context
            String name=node.getString("resource-env-ref-name",false,true);
            
            Code.debug ("Linking resource-env-ref java:comp/env/"+name +" to global "+name);
            Util.bind (envCtx, name, new LinkRef(name));
        }
        else if ("ejb-ref".equals(element) ||
                 "ejb-local-ref".equals(element) ||
                 "security-domain".equals(element))
        {
            Code.warning("Entry " + element+" => "+node+" is not supported yet");
        }
        else
        {
            super.initWebXmlElement(element, node);
        }
    }

    
    /* ------------------------------------------------------------ */
    public boolean handle(HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        return super.handle(request,response);
    }    


    /* ------------------------------------------------------------ */    
    protected void initialize ()
        throws Exception
    { 
        //create ENC for this webapp 
        Context compCtx =  (Context)_initialCtx.lookup ("java:comp");
        Context envCtx = compCtx.createSubcontext("env");

        //bind UserTransaction
        compCtx.rebind ("UserTransaction", new LinkRef ("javax.transaction.UserTransaction"));
        Code.debug ("Bound ref to javax.transaction.UserTransaction to java:comp/UserTransaction");   
    }



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
    /*
      public void bind (Context ctx, String nameStr, Object obj)
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
                Code.debug ("Subcontext "+name.get(i)+" already exists");
            }
            catch (NameNotFoundException e)
            {
                subCtx = subCtx.createSubcontext(name.get(i));
                Code.debug ("Subcontext "+name.get(i)+" created");
            }
        }

        subCtx.rebind (name.get(name.size() - 1), obj);
        Code.debug ("Bound object to "+name.get(name.size() - 1));
    }
    */
}
