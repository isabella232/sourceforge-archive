// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.xml;

import java.io.File;
import java.io.FilePermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.HashMap;
import org.mortbay.util.Code;
import org.mortbay.util.Resource;
import org.mortbay.util.TestCase;



/* ------------------------------------------------------------ */
/** Util meta TestHarness.
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
            if (!__userURL.toString().endsWith("/xml/"))
            {
                __userURL=new URL(__userURL.toString()+
                                  "test/src/org/mortbay/xml/");
                FilePermission perm = (FilePermission)
                    __userURL.openConnection().getPermission();
                __userDir=new File(perm.getName()).getCanonicalPath();
                __relDir="test/src/org/mortbay/xml/".replace('/',
                                                             File.separatorChar);
            }                
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }    

    
    /* ------------------------------------------------------------ */
    public static void testXmlParser()
    {
        TestCase t = new TestCase("org.mortbay.xml.XmlParser");
        try
        {
            XmlParser parser = new XmlParser();

            
            Resource config11Resource=Resource.newSystemResource
                ("org/mortbay/xml/configure_1_1.dtd");    
            
            parser.redirectEntity
                ("configure.dtd",config11Resource);   
            
            parser.redirectEntity
                ("configure_1_1.dtd",
                 config11Resource);
            parser.redirectEntity
                ("http://jetty.mortbay.org/configure_1_1.dtd",
                 config11Resource);
            parser.redirectEntity
                ("-//Mort Bay Consulting//DTD Configure 1.1//EN",
                 config11Resource);
            
            String url = __userURL+"TestData/configure.xml";
            XmlParser.Node testDoc = parser.parse(url);
            String testDocStr = testDoc.toString().trim();
            Code.debug(testDocStr);
            
            t.check(testDocStr.startsWith("<Configure"),"Parsed");
            t.check(testDocStr.endsWith("</Configure>"),"Parsed");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    public static void testXmlConfiguration()
    {
        TestCase t = new TestCase("org.mortbay.xml.XmlConfiguration");
        try
        {
            String url = __userURL+"TestData/configure.xml";
            XmlConfiguration configuration =
                new XmlConfiguration(new URL(url));
            TestConfiguration tc = new TestConfiguration();
            configuration.configure(tc);

            t.checkEquals(tc.testObject,"SetValue","Set String");
            t.checkEquals(tc.testInt,2,"Set Type");

            t.checkEquals(tc.get("Test"),"PutValue","Put");
            t.checkEquals(tc.get("TestDft"),"2","Put dft");
            t.checkEquals(tc.get("TestInt"),new Integer(2),"Put type");
            
            t.checkEquals(tc.get("Trim"),"PutValue","Trim");
            t.checkEquals(tc.get("Null"),null,"Null");
            t.checkEquals(tc.get("NullTrim"),null,"NullTrim");
            
            t.checkEquals(tc.get("ObjectTrim"),
                          new Double(1.2345),
                          "ObjectTrim");
            t.checkEquals(tc.get("Objects"),
                          "-1String",
                          "Objects");
            t.checkEquals(tc.get("ObjectsTrim"),
                          "-1String",
                          "ObjectsTrim");
            t.checkEquals(tc.get("String"),
                          "\n    PutValue\n  ",
                          "String");
            t.checkEquals(tc.get("NullString"),
                          "",
                          "NullString");
            t.checkEquals(tc.get("WhiteSpace"),
                          "\n  ",
                          "WhateSpace");
            t.checkEquals(tc.get("ObjectString"),
                          "\n    1.2345\n  ",
                          "ObjectString");
            t.checkEquals(tc.get("ObjectsString"),
                          "-1String",
                          "ObjectsString");
            t.checkEquals(tc.get("ObjectsWhiteString"),
                          "-1\n  String",
                          "ObjectsWhiteString");

            t.checkEquals(tc.get("Property"),
                          System.getProperty("user.dir")+"/stuff",
                          "Property");
            
            t.checkEquals(tc.get("Called"),
                          "Yes",
                          "Called");

            t.check(tc.called,"Static called");

            
            TestConfiguration tc2=tc.nested;
            t.check(tc2!=null,"Called(bool)");
            t.checkEquals(tc2.get("Arg"),
                          new Boolean(true),
                          "Called(bool)");

            t.checkEquals(tc.get("Arg"),null,"nested config");
            t.checkEquals(tc2.get("Arg"),new Boolean(true),"nested config");
            
            t.checkEquals(tc2.testObject,"Call1","nested config");
            t.checkEquals(tc2.testInt,4,"nested config");
            t.checkEquals(tc2.url.toString(),
                          "http://www.mortbay.com/",
                          "nested call");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    
    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
        try
        {
       	    testXmlParser();
       	    testXmlConfiguration();
        }
        catch(Throwable th)
        {
            Code.warning(th);
            TestCase t = new TestCase("org.mortbay.xml.TestHarness");
            t.check(false,th.toString());
        }
        finally
        {
            TestCase.report();
        }
    }
}
