// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class URITest extends junit.framework.TestCase
{
    public URITest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(URITest.class);
        return suite;                  
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
    /* ------------------------------------------------------------ */
    public void testURI()
    {
        URI uri;

        // test basic encode/decode
        StringBuffer buf = new StringBuffer();
        URI.encodeString(buf,"foo%23;,:=bar",";,=");
        assertEquals("foo%23;,:=bar",URI.decodePath(buf.toString()));


        // No host
        uri = new URI("/");
        assertEquals("root /","/", uri.getPath());

        uri = new URI("/Test/URI");
        assertEquals("no params","/Test/URI", uri.toString());

        uri = new URI("/Test/URI?");
        assertEquals("no params","/Test/URI?", uri.toString());
        uri.setPath(uri.getPath());
        assertEquals("no params","/Test/URI", uri.toString());
        
        uri = new URI("/Test/URI?a=1");
        assertEquals("one param","/Test/URI?a=1", uri.toString());
    
        uri = new URI("/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param","/Test/URI?b=2+%21", uri.toString());

        // Host but no port
        uri = new URI("http://host");
        assertEquals("root host","/", uri.getPath());
        assertEquals("root host","http://host/", uri.toString());
        
        uri = new URI("http://host/");
        assertEquals("root host/","/", uri.getPath());
        
        uri = new URI("http://host/Test/URI");
        assertEquals("no params","http://host/Test/URI", uri.toString());

        uri = new URI("http://host/Test/URI?");
        assertEquals("no params","http://host/Test/URI?", uri.toString());
        uri.setPath(uri.getPath());
        assertEquals("no params","http://host/Test/URI", uri.toString());
        
        uri = new URI("http://host/Test/URI?a=1");
        assertEquals("one param","http://host/Test/URI?a=1", uri.toString());
    
        uri = new URI("http://host/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param","http://host/Test/URI?b=2+%21", uri.toString());
    
        // Host and port and path
        uri = new URI("http://host:8080");
        assertEquals("root","/", uri.getPath());
        
        uri = new URI("http://host:8080/");
        assertEquals("root","/", uri.getPath());
        
        uri = new URI("http://host:8080/xxx");
        assertEquals("path","/xxx", uri.getPath());

        String anez=UrlEncoded.decodeString("A%F1ez");
        uri = new URI("http://host:8080/"+anez);
        assertEquals("root","/"+anez, uri.getPath());            
        
        uri = new URI("http://host:8080/Test/URI");
        assertEquals("no params","http://host:8080/Test/URI", uri.toString());

        uri = new URI("http://host:8080/Test/URI?");
        assertEquals("no params","http://host:8080/Test/URI?", uri.toString());
        uri.getParameters();
        assertEquals("no params","http://host:8080/Test/URI", uri.toString());
        
        uri = new URI("http://host:8080/Test/URI?a=1");
        assertEquals("one param","http://host:8080/Test/URI?a=1", uri.toString());
    
        uri = new URI("http://host:8080/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param","http://host:8080/Test/URI?b=2+%21", uri.toString());
    
        assertEquals("protocol","http", uri.getScheme());
        assertEquals("host","host", uri.getHost());
        assertEquals("port",8080, uri.getPort());

        uri.setScheme("ftp");
        uri.setHost("fff");
        uri.setPort(23);
        assertEquals("add param","ftp://fff:23/Test/URI?b=2+%21", uri.toString());
        
    
        uri = new URI("/Test/URI?c=1&d=2");
        uri.put("e","3");
        String s = uri.toString();
        assertTrue("merge params path", s.startsWith("/Test/URI?"));
        assertTrue("merge params c1", s.indexOf("c=1")>0);
        assertTrue("merge params d2", s.indexOf("d=2")>0);
        assertTrue("merge params e3", s.indexOf("e=3")>0);

        uri = new URI("/Test/URI?a=");
        assertEquals("null param","/Test/URI?a=", uri.toString());
        uri.getParameters();
        assertEquals("null param","/Test/URI?a", uri.toString());
        
        uri = new URI("/Test/URI?a+c=1%203");
        assertEquals("space param","/Test/URI?a+c=1%203", uri.toString());
        System.err.println(uri.getParameters());
        assertEquals("space param","1 3", uri.get("a c"));
        uri.getParameters();
        assertEquals("space param","/Test/URI?a+c=1+3", uri.toString());
        
        uri = new URI("/Test/Nasty%26%3F%20URI?c=%26&d=+%3F");
        assertEquals("nasty","/Test/Nasty&? URI", uri.getPath());
        uri.setPath("/test/nasty&? URI");
        uri.getParameters();
        assertTrue( "nasty",
                    uri.toString().equals("/test/nasty&%3F%20URI?c=%26&d=+%3F")||
                    uri.toString().equals("/test/nasty&%3F%20URI?d=+%3F&c=%26")
                    );
        uri=(URI)uri.clone();
        assertTrue("clone",
                   uri.toString().equals("/test/nasty&%3F%20URI?c=%26&d=+%3F")||
                   uri.toString().equals("/test/nasty&%3F%20URI?d=+%3F&c=%26")
                   );

        assertEquals("null+null", URI.addPaths(null,null),null);
        assertEquals("null+", URI.addPaths(null,""),null);
        assertEquals("null+bbb", URI.addPaths(null,"bbb"),"bbb");
        assertEquals("null+/", URI.addPaths(null,"/"),"/");
        assertEquals("null+/bbb", URI.addPaths(null,"/bbb"),"/bbb");
        
        assertEquals("+null", URI.addPaths("",null),"");
        assertEquals("+", URI.addPaths("",""),"");
        assertEquals("+bbb", URI.addPaths("","bbb"),"bbb");
        assertEquals("+/", URI.addPaths("","/"),"/");
        assertEquals("+/bbb", URI.addPaths("","/bbb"),"/bbb");
        
        assertEquals("aaa+null", URI.addPaths("aaa",null),"aaa");
        assertEquals("aaa+", URI.addPaths("aaa",""),"aaa");
        assertEquals("aaa+bbb", URI.addPaths("aaa","bbb"),"aaa/bbb");
        assertEquals("aaa+/", URI.addPaths("aaa","/"),"aaa/");
        assertEquals("aaa+/bbb", URI.addPaths("aaa","/bbb"),"aaa/bbb");
        
        assertEquals("/+null", URI.addPaths("/",null),"/");
        assertEquals("/+", URI.addPaths("/",""),"/");
        assertEquals("/+bbb", URI.addPaths("/","bbb"),"/bbb");
        assertEquals("/+/", URI.addPaths("/","/"),"/");
        assertEquals("/+/bbb", URI.addPaths("/","/bbb"),"/bbb");
        
        assertEquals("aaa/+null", URI.addPaths("aaa/",null),"aaa/");
        assertEquals("aaa/+", URI.addPaths("aaa/",""),"aaa/");
        assertEquals("aaa/+bbb", URI.addPaths("aaa/","bbb"),"aaa/bbb");
        assertEquals("aaa/+/", URI.addPaths("aaa/","/"),"aaa/");
        assertEquals("aaa/+/bbb", URI.addPaths("aaa/","/bbb"),"aaa/bbb");
        
        assertEquals(";JS+null", URI.addPaths(";JS",null),";JS");
        assertEquals(";JS+", URI.addPaths(";JS",""),";JS");
        assertEquals(";JS+bbb", URI.addPaths(";JS","bbb"),"bbb;JS");
        assertEquals(";JS+/", URI.addPaths(";JS","/"),"/;JS");
        assertEquals(";JS+/bbb", URI.addPaths(";JS","/bbb"),"/bbb;JS");
        
        assertEquals("aaa;JS+null", URI.addPaths("aaa;JS",null),"aaa;JS");
        assertEquals("aaa;JS+", URI.addPaths("aaa;JS",""),"aaa;JS");
        assertEquals("aaa;JS+bbb", URI.addPaths("aaa;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URI.addPaths("aaa;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URI.addPaths("aaa;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("aaa;JS+null", URI.addPaths("aaa/;JS",null),"aaa/;JS");
        assertEquals("aaa;JS+", URI.addPaths("aaa/;JS",""),"aaa/;JS");
        assertEquals("aaa;JS+bbb", URI.addPaths("aaa/;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URI.addPaths("aaa/;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URI.addPaths("aaa/;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("?A=1+null", URI.addPaths("?A=1",null),"?A=1");
        assertEquals("?A=1+", URI.addPaths("?A=1",""),"?A=1");
        assertEquals("?A=1+bbb", URI.addPaths("?A=1","bbb"),"bbb?A=1");
        assertEquals("?A=1+/", URI.addPaths("?A=1","/"),"/?A=1");
        assertEquals("?A=1+/bbb", URI.addPaths("?A=1","/bbb"),"/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URI.addPaths("aaa?A=1",null),"aaa?A=1");
        assertEquals("aaa?A=1+", URI.addPaths("aaa?A=1",""),"aaa?A=1");
        assertEquals("aaa?A=1+bbb", URI.addPaths("aaa?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URI.addPaths("aaa?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URI.addPaths("aaa?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URI.addPaths("aaa/?A=1",null),"aaa/?A=1");
        assertEquals("aaa?A=1+", URI.addPaths("aaa/?A=1",""),"aaa/?A=1");
        assertEquals("aaa?A=1+bbb", URI.addPaths("aaa/?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URI.addPaths("aaa/?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URI.addPaths("aaa/?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals(";JS?A=1+null", URI.addPaths(";JS?A=1",null),";JS?A=1");
        assertEquals(";JS?A=1+", URI.addPaths(";JS?A=1",""),";JS?A=1");
        assertEquals(";JS?A=1+bbb", URI.addPaths(";JS?A=1","bbb"),"bbb;JS?A=1");
        assertEquals(";JS?A=1+/", URI.addPaths(";JS?A=1","/"),"/;JS?A=1");
        assertEquals(";JS?A=1+/bbb", URI.addPaths(";JS?A=1","/bbb"),"/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URI.addPaths("aaa;JS?A=1",null),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+", URI.addPaths("aaa;JS?A=1",""),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URI.addPaths("aaa;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URI.addPaths("aaa;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URI.addPaths("aaa;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URI.addPaths("aaa/;JS?A=1",null),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+", URI.addPaths("aaa/;JS?A=1",""),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URI.addPaths("aaa/;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URI.addPaths("aaa/;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URI.addPaths("aaa/;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");

        assertEquals("parent /aaa/bbb/","/aaa/", URI.parentPath("/aaa/bbb/"));
        assertEquals("parent /aaa/bbb","/aaa/", URI.parentPath("/aaa/bbb"));
        assertEquals("parent /aaa/","/", URI.parentPath("/aaa/"));
        assertEquals("parent /aaa","/", URI.parentPath("/aaa"));
        assertEquals("parent /",null, URI.parentPath("/"));
        assertEquals("parent null",null, URI.parentPath(null));

        String[][] canonical = 
        {
            {"/aaa/bbb/","/aaa/bbb/"},
            {"/aaa//bbb/","/aaa/bbb/"},
            {"/aaa///bbb/","/aaa/bbb/"},
            {"/aaa/./bbb/","/aaa/bbb/"},
            {"/aaa/../bbb/","/bbb/"},
            {"/aaa/./../bbb/","/bbb/"},
            {"/aaa/bbb/ccc/../../ddd/","/aaa/ddd/"},
            {"./bbb/","bbb/"},
            {"./aaa/../bbb/","bbb/"},
            {"./",""},
            {".//",""},
            {".///",""},
            {"/.","/"},
            {"//.","/"},
            {"///.","/"},
            {"/","/"},
            {"aaa/bbb","aaa/bbb"},
            {"aaa/","aaa/"},
            {"aaa","aaa"},
            {"/aaa/bbb","/aaa/bbb"},
            {"/aaa//bbb","/aaa/bbb"},
            {"/aaa/./bbb","/aaa/bbb"},
            {"/aaa/../bbb","/bbb"},
            {"/aaa/./../bbb","/bbb"},
            {"./bbb","bbb"},
            {"./aaa/../bbb","bbb"},
            {"aaa/bbb/..","aaa/"},
            {"aaa/bbb/../","aaa/"},
            {"./",""},
            {".",""},
            {"",""},
            {"..",null},
            {"./..",null},
            {"aaa/../..",null},
            {"/foo/bar/../../..",null},
            {"/../foo",null},
            {"/foo/.","/foo/"},
            {"a","a"},
            {"a/","a/"},
            {"a/.","a/"},
            {"a/..",""},
            {"a/../..",null},
            {"/foo/../bar//","/bar/"}
        };

        for (int t=0;t<canonical.length;t++)
            assertEquals( "canonical "+canonical[t][0],
                          URI.canonicalPath(canonical[t][0]),
                          canonical[t][1]
                          );
        
    }

}
