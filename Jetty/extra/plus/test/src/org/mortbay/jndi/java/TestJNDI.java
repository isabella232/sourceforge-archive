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
import org.mortbay.jndi.NamingContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TestJNDI extends TestCase
{
    private static Log log = LogFactory.getLog(TestJNDI.class);

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
        
        InitialContext initCtxA = new InitialContext();
        initCtxA.bind ("blah", "123");
        assertEquals ("123", initCtxA.lookup("blah"));

        
        
        
        InitialContext initCtx = new InitialContext();
        Context sub0 = (Context)initCtx.lookup("java:");

        if(log.isDebugEnabled())log.debug("------ Looked up java: --------------");

        Name n = sub0.getNameParser("").parse("/red/green/");


        if(log.isDebugEnabled())log.debug("get(0)="+n.get(0));
        if(log.isDebugEnabled())log.debug("getPrefix(1)="+n.getPrefix(1));
        n = n.getSuffix(1);
        if(log.isDebugEnabled())log.debug("getSuffix(1)="+n);
        if(log.isDebugEnabled())log.debug("get(0)="+n.get(0));
        if(log.isDebugEnabled())log.debug("getPrefix(1)="+n.getPrefix(1));
        n = n.getSuffix(1);
        if(log.isDebugEnabled())log.debug("getSuffix(1)="+n);
        if(log.isDebugEnabled())log.debug("get(0)="+n.get(0));
        if(log.isDebugEnabled())log.debug("getPrefix(1)="+n.getPrefix(1));
        n = n.getSuffix(1);
        if(log.isDebugEnabled())log.debug("getSuffix(1)="+n);

        n = sub0.getNameParser("").parse("pink/purple/");
        if(log.isDebugEnabled())log.debug("get(0)="+n.get(0));
        if(log.isDebugEnabled())log.debug("getPrefix(1)="+n.getPrefix(1));
        n = n.getSuffix(1);
        if(log.isDebugEnabled())log.debug("getSuffix(1)="+n);
        if(log.isDebugEnabled())log.debug("get(0)="+n.get(0));
        if(log.isDebugEnabled())log.debug("getPrefix(1)="+n.getPrefix(1));

        NamingContext ncontext = (NamingContext)sub0;

        Name nn = ncontext.toCanonicalName(ncontext.getNameParser("").parse("/yellow/blue/"));
        log.debug(nn);
        assertEquals (2, nn.size());

        nn = ncontext.toCanonicalName(ncontext.getNameParser("").parse("/yellow/blue"));
        log.debug(nn);
        assertEquals (2, nn.size());

        nn = ncontext.toCanonicalName(ncontext.getNameParser("").parse("/"));
        if(log.isDebugEnabled())log.debug("/ parses as: "+nn+" with size="+nn.size());
        log.debug(nn);
        assertEquals (1, nn.size());

        nn = ncontext.toCanonicalName(ncontext.getNameParser("").parse(""));
        log.debug(nn);
        assertEquals (0, nn.size());

        Context fee = ncontext.createSubcontext("fee");
        fee.bind ("fi", "88");
        assertEquals("88", initCtxA.lookup("java:/fee/fi"));
        assertEquals("88", initCtxA.lookup("java:/fee/fi/"));
        assertTrue (initCtxA.lookup("java:/fee/") instanceof javax.naming.Context);

        try
        {
            Context sub1 = sub0.createSubcontext ("comp");
            fail("Comp should already be bound");
        }
        catch (NameAlreadyBoundException e)
        {
            //expected exception
        }

       

        //check bindings at comp
        Context sub1 = (Context)initCtx.lookup("java:comp");

        Context sub2 = sub1.createSubcontext ("env");

        initCtx.bind ("java:comp/env/rubbish", "abc");
        assertEquals ("abc", (String)initCtx.lookup("java:comp/env/rubbish"));

        //check binding LinkRefs
        LinkRef link = new LinkRef ("java:comp/env/rubbish");
        initCtx.bind ("java:comp/env/poubelle", link);
        assertEquals ("abc", (String)initCtx.lookup("java:comp/env/poubelle"));

        //check binding References
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

        //test java:/comp/env/stuff
        assertEquals ("xyz", (String)initCtx.lookup("java:/comp/env/poubelle/"));

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
        
        //test close does nothing
        Context closeCtx = (Context)initCtx.lookup("java:comp/env");
        closeCtx.close();
        
        
        //test what happens when you close an initial context
        InitialContext closeInit = new InitialContext();
        closeInit.close();
        
        //test what happens when you close an initial context that was used
        initCtx.close();
        

    } 


}
