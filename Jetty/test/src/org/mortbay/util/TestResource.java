// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import junit.framework.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.Permission;

public class TestResource extends junit.framework.TestCase
{

    public static String __userDir = System.getProperty("user.dir",".");
    public static URL __userURL=null;
    private static String __relDir="";
    private static File tmpFile;

    private static final boolean DIR=true;
    private static final boolean EXISTS=true;
    
    class Data
    {
        Resource resource;
        String test;
        boolean exists;
        boolean dir;
        String content;
        
        Data(Data data,String path,boolean exists, boolean dir)
            throws Exception
        {
            this.test=data.resource+"+"+path;
            resource=data.resource.addPath(path);
            this.exists=exists;
            this.dir=dir;
        }
        
        Data(Data data,String path,boolean exists, boolean dir, String content)
            throws Exception
        {
            this.test=data.resource+"+"+path;
            resource=data.resource.addPath(path);
            this.exists=exists;
            this.dir=dir;
            this.content=content;
        }
        
        Data(URL url,boolean exists, boolean dir)
            throws Exception
        {
            this.test=url.toString();
            this.exists=exists;
            this.dir=dir;
            resource=Resource.newResource(url);
        }
        
        Data(String url,boolean exists, boolean dir)
            throws Exception
        {
            this.test=url;
            this.exists=exists;
            this.dir=dir;
            resource=Resource.newResource(url);
        }
        
        Data(String url,boolean exists, boolean dir, String content)
            throws Exception
        {
            this.test=url;
            this.exists=exists;
            this.dir=dir;
            this.content=content;
            resource=Resource.newResource(url);
        }
    }

    public static Data[] data;
    
    public TestResource(String name)
    {
        super(name);
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
    
    /* ------------------------------------------------------------ */
    public static junit.framework.Test suite()
    {
        return new TestSuite(TestResource.class);
    }

    /* ------------------------------------------------------------ */
    protected void setUp()
        throws Exception
    {
        if (data!=null)
            return;
        
        File file = new File(__userDir);
        file=new File(file.getCanonicalPath());
        __userURL=file.toURL();
        if (__userURL.toString().endsWith("/test/") ||
            __userURL.toString().endsWith("/test"))
        {
            __userURL=new URL(__userURL.toString()+
                              "src/org/mortbay/util/");
            FilePermission perm = (FilePermission)
                __userURL.openConnection().getPermission();
            __userDir=new File(perm.getName()).getCanonicalPath()+File.separatorChar;
            __relDir="src/org/mortbay/util/"
                .replace('/',File.separatorChar);
        }
        else
        {
            __userURL=new URL(__userURL.toString()+
                              "test/src/org/mortbay/util/");
            FilePermission perm = (FilePermission)
                __userURL.openConnection().getPermission();
            __userDir=new File(perm.getName()).getCanonicalPath()+File.separatorChar;
            __relDir="test/src/org/mortbay/util/"
                .replace('/',File.separatorChar);
        }
        
        System.err.println("User Dir="+__userDir);
        System.err.println("Rel  Dir="+__relDir);
        System.err.println("User URL="+__userURL);

        tmpFile=File.createTempFile("test",null);
        tmpFile.deleteOnExit();
        
        data = new Data[50];
        int i=0;

        data[i++]=new Data(tmpFile.toString(),EXISTS,!DIR);
        
        int rt=i;
        data[i++]=new Data(__userURL,EXISTS,DIR);
        data[i++]=new Data(__userDir,EXISTS,DIR);
        data[i++]=new Data(__relDir,EXISTS,DIR);
        data[i++]=new Data(__userURL+"TestResource.java",EXISTS,!DIR);
        data[i++]=new Data(__userDir+"TestResource.java",EXISTS,!DIR);
        data[i++]=new Data(__relDir+"TestResource.java",EXISTS,!DIR);
        data[i++]=new Data(__userURL+"NoName.txt",!EXISTS,!DIR);
        data[i++]=new Data(__userDir+"NoName.txt",!EXISTS,!DIR);
        data[i++]=new Data(__relDir+"NoName.txt",!EXISTS,!DIR);

        data[i++]=new Data(data[rt],"TestResource.java",EXISTS,!DIR);
        data[i++]=new Data(data[rt],"/TestResource.java",EXISTS,!DIR);
        data[i++]=new Data(data[rt],"NoName.txt",!EXISTS,!DIR);
        data[i++]=new Data(data[rt],"/NoName.txt",!EXISTS,!DIR);
        
        int td=i;
        data[i++]=new Data(data[rt],"TestData",EXISTS,DIR);
        data[i++]=new Data(data[rt],"TestData/",EXISTS,DIR);
        data[i++]=new Data(data[td],"alphabet.txt",EXISTS,!DIR,"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        
        data[i++]=new Data("jar:file:/somejar.jar!/content/",!EXISTS,!DIR);
        data[i++]=new Data("jar:file:/somejar.jar!/",!EXISTS,!DIR);

        int tj=i;
        data[i++]=new Data("jar:"+__userURL+"TestData/test.zip!/",EXISTS,DIR);
        data[i++]=new Data(data[tj],"Unkown",!EXISTS,!DIR);
        data[i++]=new Data(data[tj],"/Unkown/",!EXISTS,!DIR);
        
        data[i++]=new Data(data[tj],"subdir",!EXISTS,!DIR);
        data[i++]=new Data(data[tj],"/subdir/",EXISTS,DIR);
        data[i++]=new Data(data[tj],"alphabet",EXISTS,!DIR,
                           "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        data[i++]=new Data(data[tj],"/subdir/alphabet",EXISTS,!DIR,
                           "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    
    /* ------------------------------------------------------------ */
    protected void tearDown()
        throws Exception
    {
    }
    

    /* ------------------------------------------------------------ */
    public void testResourceExists()
    {
        for (int i=0;i<data.length;i++)
        {
            if (data[i]==null)
                continue;

            assertEquals(""+i+":"+data[i].test,data[i].dir,data[i].resource.isDirectory());
        }
    }
    
    /* ------------------------------------------------------------ */
    public void testResourceDir()
    {
        for (int i=0;i<data.length;i++)
        {
            if (data[i]==null)
                continue;

            assertEquals(""+i+":"+data[i].test,data[i].dir,data[i].resource.isDirectory());
        }
    }
    
    /* ------------------------------------------------------------ */
    public void testResourceContent()
        throws Exception
    {
        for (int i=0;i<data.length;i++)
        {
            if (data[i]==null || data[i].content==null)
                continue;

            InputStream in = data[i].resource.getInputStream();
            String c=IO.toString(in);
            assertTrue(""+i+":"+data[i].test,c.startsWith(data[i].content));
        }
    }
    
    /* ------------------------------------------------------------ */
    public void testResourceCache()
        throws Exception
    {
        for (int i=0;i<data.length;i++)
        {
            if (data[i]==null || data[i].content==null)
                continue;

            CachedResource cache=data[i].resource.cache();
            InputStream in = cache.getInputStream();
            String c=IO.toString(in);
            assertTrue("a"+i+":"+data[i].test,c.startsWith(data[i].content));

            ByteArrayOutputStream2 bout=new ByteArrayOutputStream2();
            cache.writeTo(bout,4,5);
            String s=new String(bout.toByteArray());
            assertEquals("b"+i+":"+data[i].test,data[i].content.substring(4,9),s);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void testResourceCacheUpdate()
        throws Exception
    {
        Writer w = new FileWriter(tmpFile);
        w.write("NowIsTheTimeForAllGoodMen");
        w.flush();
        w.close();
        
        Resource r=Resource.newResource(tmpFile.toString());
        CachedResource c=r.cache();

        assertEquals("cached content",
                     IO.toString(r.getInputStream()),
                     IO.toString(c.getInputStream()));

        ByteArrayOutputStream2 bout=new ByteArrayOutputStream2();
        c.writeTo(bout,3,2);
        String s=new String(bout.toByteArray());
        assertEquals("range1","Is",s);
        
        long rlm=r.lastModified();
        long clm=c.lastModified();
        assertEquals("last Modified1",rlm,clm);
        assertTrue("modified",rlm>0);
        assertTrue("modified",clm>0);

        c.update();
        clm=c.lastModified();
        assertEquals("last Modified2",rlm,clm);
        bout=new ByteArrayOutputStream2();
        c.writeTo(bout,3,2);
        s=new String(bout.toByteArray());
        assertEquals("range2","Is",s);

        tmpFile.delete();
        Thread.sleep(5000);
        
        w = new FileWriter(tmpFile);
        w.write("Blah Blah Blah");
        w.flush();
        w.close();

        
        rlm=r.lastModified();
        assertEquals("last Modified3",clm,c.lastModified());
        
        assertTrue("new file",rlm>clm);
                   
        bout=new ByteArrayOutputStream2();
        c.writeTo(bout,3,2);
        s=new String(bout.toByteArray());
        assertEquals("range2","Is",s);

        c.update();
        clm=c.lastModified();
        assertEquals("last Modified4",rlm,clm);
        assertEquals("cached content",
                     IO.toString(r.getInputStream()),
                     IO.toString(c.getInputStream()));
        bout=new ByteArrayOutputStream2();
        c.writeTo(bout,3,2);
        s=new String(bout.toByteArray());
        assertEquals("range2","h ",s);
        
    }
    
}
