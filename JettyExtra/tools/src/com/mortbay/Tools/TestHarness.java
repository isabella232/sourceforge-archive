// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Tools;

import com.mortbay.Tools.DataClassTest.T1;
import com.mortbay.Tools.DataClassTest.T2;
import com.mortbay.Util.Code;
import com.mortbay.Util.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;


/* ------------------------------------------------------------ */
/** 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestHarness
{
    public final static String __CRLF = "\015\012";
    public static String __userDir =
        System.getProperty("user.dir",".");
    public static URL __userURL=null;
    private static String __relDir="";
    static
    {
        try{
            File file = new File(__userDir);
            __userURL=file.toURL();
            if (!__userURL.toString().endsWith("/Tools/"))
            {
                __userURL=new URL(__userURL.toString()+
                                  "src/com/mortbay/Tools/");
                FilePermission perm = (FilePermission)
                    __userURL.openConnection().getPermission();
                __userDir=new File(perm.getName()).getCanonicalPath();
                __relDir="src/com/mortbay/Tools/".replace('/',
                                                         File.separatorChar);
            }                
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }    

    
    
    /* ------------------------------------------------------------ */
    public static void testDataHelper()
    {
        Test t = new Test("com.mortbay.Tools.DataHelper");
        try{
            T2 t2 = (com.mortbay.Tools.DataClassTest.T2)
                DataClass.emptyInstance(com.mortbay.Tools.DataClassTest.T2.class);

            t.check(t2!=null,"empty t2 constructed");
            t.checkEquals(t2.a.length,0,"empty array");
            t.check(t2.t1!=null,"empty t1 constructed");
            t.checkEquals(t2.t1.s,"","empty string");
            t.checkEquals(t2.t1.i,0,"zero int");
            t.checkEquals(t2.t1.I.intValue(),0,"zero Integer");

            t2.t1.i=42;
            t2.t1.s="check";
            t2.a=new T1[2];
            t2.a[0]=(com.mortbay.Tools.DataClassTest.T1)
                DataClass.emptyInstance(com.mortbay.Tools.DataClassTest.T1.class);

            String out = DataClass.toString(t2);
            if (Code.debug())
                System.out.println(out);

            t.checkContains(out,"i = 42;","toString int");
            t.checkContains(out,"check","toString string");
            t.checkContains(out,"[","toString null array open");
            t.checkContains(out,"]","toString null array close");
            t.checkContains(out,"null","toString null array element");
            
        }
        catch(Exception e){
            t.check(false,"Exception: "+e);
        }
    }

    /* ------------------------------------------------------------ */
    /** main
     */
    public static void main(String[] args)
    {
        try
        {
       	    testDataHelper();
       	    PropertyTreeTest.test();
        }
        catch(Throwable th)
        {
            Code.warning(th);
            Test t = new Test("com.mortbay.Tools.TestHarness");
            t.check(false,th.toString());
        }
        finally
        {
            Test.report();
        }
    }
}
