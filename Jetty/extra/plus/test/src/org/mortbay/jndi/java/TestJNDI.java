package org.mortbay.jndi.java;


import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.HashMap;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.spi.ObjectFactory;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestJNDI extends TestCase
{
    public static class MyObjectFactory implements ObjectFactory
    {
        public static String myString = "xxx";

        public Object getObjectInstance(Object obj,
                                        Name name,
                                        Context nameCtx,
                                        Hashtable environment)
            throws Exception
            {
                return myString;
            }
    }

    public TestJNDI (String name)
    {
        super (name);
    }


    public static Test suite ()
    {
        return new TestSuite (TestJNDI.class);
    }

    public void setUp ()
        throws Exception
    {
    }


    public void tearDown ()
        throws Exception
    {
    }

    public void testIt ()
        throws Exception
    {
        //set up some classloaders
        Thread currentThread = Thread.currentThread();
        ClassLoader currentLoader = currentThread.getContextClassLoader();
        ClassLoader childLoader1 = new URLClassLoader(new URL[0], currentLoader);
        ClassLoader childLoader2 = new URLClassLoader(new URL[0], currentLoader);

        //set the current thread's classloader
        currentThread.setContextClassLoader(childLoader1);
        
        InitialContext initCtx = new InitialContext();
        Context sub0 = (Context)initCtx.lookup("java:");
        try
        {
            Context sub1 = sub0.createSubcontext ("comp");
            fail("Comp should already be bound");
        }
        catch (NameAlreadyBoundException e)
        {
            //expected exception
        }

        Context sub1 = (Context)initCtx.lookup("java:comp");

        Context sub2 = sub1.createSubcontext ("env");

        initCtx.bind ("java:comp/env/rubbish", "abc");
        assertEquals ("abc", (String)initCtx.lookup("java:comp/env/rubbish"));

        LinkRef link = new LinkRef ("java:comp/env/rubbish");
        initCtx.bind ("java:comp/env/poubelle", link);
        assertEquals ("abc", (String)initCtx.lookup("java:comp/env/poubelle"));

        StringRefAddr addr = new StringRefAddr("blah", "myReferenceable");
        Reference ref = new Reference (java.lang.String.class.getName(),
                                       addr,
                                       MyObjectFactory.class.getName(),
                                       (String)null);

        initCtx.bind ("java:comp/env/quatsch", ref);
        assertEquals (MyObjectFactory.myString, (String)initCtx.lookup("java:comp/env/quatsch"));
        
        //test binding something at java:
        Context sub3 = initCtx.createSubcontext("java:zero");
        initCtx.bind ("java:zero/one", "ONE");
        assertEquals ("ONE", initCtx.lookup("java:zero/one"));


       
        
        //change the current thread's classloader to check distinct naming                                              
        currentThread.setContextClassLoader(childLoader2);

        Context otherSub1 = (Context)initCtx.lookup("java:comp");
        assertTrue (!(sub1 == otherSub1));
        try
        {
            initCtx.lookup("java:comp/env/rubbish");
        }
        catch (NameNotFoundException e)
        {
            //expected
        }

     
        //put the thread's classloader back
        currentThread.setContextClassLoader(childLoader1);

        //test rebind with existing binding
        initCtx.rebind("java:comp/env/rubbish", "xyz");
        assertEquals ("xyz", initCtx.lookup("java:comp/env/rubbish"));

        //test rebind with no existing binding
        initCtx.rebind ("java:comp/env/mullheim", "hij");
        assertEquals ("hij", initCtx.lookup("java:comp/env/mullheim"));

        //test that the other bindings are already there       
        assertEquals ("xyz", (String)initCtx.lookup("java:comp/env/poubelle"));

        //test list Names
        NamingEnumeration nenum = initCtx.list ("java:comp/env");
        HashMap results = new HashMap();
        while (nenum.hasMore())
        {
            NameClassPair ncp = (NameClassPair)nenum.next();
            results.put (ncp.getName(), ncp.getClassName());
        }

        assertEquals (4, results.size());
 
        assertEquals ("java.lang.String", (String)results.get("rubbish"));
        assertEquals ("javax.naming.LinkRef", (String)results.get("poubelle"));
        assertEquals ("java.lang.String", (String)results.get("mullheim"));
        assertEquals ("javax.naming.Reference", (String)results.get("quatsch"));

        //test list Bindings
        NamingEnumeration benum = initCtx.list("java:comp/env");
        assertEquals (4, results.size());

        //test NameInNamespace
        assertEquals ("comp/env", sub2.getNameInNamespace());

    }
}
