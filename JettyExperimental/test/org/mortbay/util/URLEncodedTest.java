// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class URLEncodedTest extends junit.framework.TestCase
{
    public URLEncodedTest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(URLEncodedTest.class);
        return suite;                  
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    

    /* -------------------------------------------------------------- */
    public void testUrlEncoded()
    {
          
        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals("Empty",0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("Name1=Value1");
        assertEquals("simple param size",1, url_encoded.size());
        assertEquals("simple encode","Name1=Value1", url_encoded.encode());
        assertEquals("simple get","Value1", url_encoded.getString("Name1"));
        
        url_encoded.clear();
        url_encoded.decode("Name2=");
        assertEquals("dangling param size",1, url_encoded.size());
        assertEquals("dangling encode","Name2", url_encoded.encode());
        assertEquals("dangling get","", url_encoded.getString("Name2"));
    
        url_encoded.clear();
        url_encoded.decode("Name3");
        assertEquals("noValue param size",1, url_encoded.size());
        assertEquals("noValue encode","Name3", url_encoded.encode());
        assertEquals("noValue get","", url_encoded.getString("Name3"));
    
        url_encoded.clear();
        url_encoded.decode("Name4=Value+4%21");
        assertEquals("encoded param size",1, url_encoded.size());
        assertEquals("encoded encode","Name4=Value+4%21", url_encoded.encode());
        assertEquals("encoded get","Value 4!", url_encoded.getString("Name4"));
        
        url_encoded.clear();
        url_encoded.decode("Name4=Value+4%21%20%214");
        assertEquals("encoded param size",1, url_encoded.size());
        assertEquals("encoded encode","Name4=Value+4%21+%214", url_encoded.encode());
        assertEquals("encoded get","Value 4! !4", url_encoded.getString("Name4"));

        
        url_encoded.clear();
        url_encoded.decode("Name5=aaa&Name6=bbb");
        assertEquals("multi param size",2, url_encoded.size());
        assertTrue("multi encode "+url_encoded.encode(),
                   url_encoded.encode().equals("Name5=aaa&Name6=bbb") ||
                   url_encoded.encode().equals("Name6=bbb&Name5=aaa")
                   );
        assertEquals("multi get","aaa", url_encoded.getString("Name5"));
        assertEquals("multi get","bbb", url_encoded.getString("Name6"));
    
        url_encoded.clear();
        url_encoded.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
        assertEquals("multi encode",
                         "Name7=aaa&Name7=b%2Cb&Name7=ccc"
                         ,
                        url_encoded.encode());
        assertEquals("list get all", url_encoded.getString("Name7"),"aaa,b,b,ccc");
        assertEquals("list get","aaa", url_encoded.getValues("Name7").get(0));
        assertEquals("list get", url_encoded.getValues("Name7").get(1),"b,b");
        assertEquals("list get","ccc", url_encoded.getValues("Name7").get(2));

        url_encoded.clear();
        url_encoded.decode("Name8=xx%2C++yy++%2Czz");
        assertEquals("encoded param size",1, url_encoded.size());
        assertEquals("encoded encode","Name8=xx%2C++yy++%2Czz", url_encoded.encode());
        assertEquals("encoded get", url_encoded.getString("Name8"),"xx,  yy  ,zz");

        
        byte[] b = new byte[]
            {
                (byte)'s',
                (byte)'=',
                (byte)0x83,
                (byte)'Q',
                (byte)0x81,
                (byte)0x5b,
                (byte)0x83,
                (byte)0x80
            };
        MultiMap m = new MultiMap();
        UrlEncoded.decodeTo(b,0,b.length,m,"SJIS");
        String sjis=(String)m.get("s");
        assertEquals("SJIS len",3, sjis.length());
        assertEquals("SJIS param","\u30b2\u30fc\u30e0",sjis );
        
        
    }
}
