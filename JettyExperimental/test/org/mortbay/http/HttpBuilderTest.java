//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.http;


import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.View;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class HttpBuilderTest extends TestCase
{
    public final static String CONTENT="The quick brown fox jumped\nover the lazy dog\n";
    public final static String[] connect={null,"keep-alive","close"};

    public HttpBuilderTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HttpBuilderTest.class);
    }

    
    public void testHTTP()
    	throws Exception
    {
        Buffer b=new ByteArrayBuffer(4096);
        HttpBuilder hb = new HttpBuilder(b,null);
        Handler handler = new Handler();
        HttpParser parser= new HttpParser(b,null, handler);
        
        for (int v=9;v<=11;v++)
        {
            for (int r=0;r<tr.length;r++)
            {
                for (int ch=1;ch<=3;ch++)
                {
                    for (int c=0;c<connect.length;c++)
                    {
                        String t="v="+v+",r="+r+",ch="+ch+",c="+c;
                        
                        b.clear();
                        hb.reset();
                        tr[r].build(v,hb,null,connect[c],null,ch);
                        System.out.println("RESPONSE: "+t+"\n"+b.toString()+(hb.isPersistent()?"...":"XXX"));
                        
                        if (v==9)
                        {
                            assertFalse(t,hb.isPersistent());
                            if (tr[r].content!=null)
                                assertEquals(t,tr[r].content, b.toString());
                            continue;
                        }
                        
                        parser.reset();
                        parser.parse();
                        
                        if (tr[r].content!=null)
                            assertEquals(t,tr[r].content, this.content);
                        assertTrue(t,hb.isPersistent() || v==10 && tr[r].values[1]==null || c==2);
                        assertTrue(t,tr[r].values[1]==null || content.length()==Integer.parseInt(tr[r].values[1]));
                    }
                }
            }
        }
    }

    

    static final String[] headers= { "Content-Type","Content-Length","Connection","Transfer-Encoding","Other"};
    class TR
    {
        int code;
        String[] values=new String[headers.length];
        String content;
        
        TR(int code,String ct, String cl ,String content)
        {
            this.code=code;
            values[0]=ct;
            values[1]=cl;
            values[4]="value";
            this.content=content;
        }
        
        void build(int version,HttpBuilder hb,String reason, String connection, String te, int chunks)
        	throws Exception
        {
            values[2]=connection;
            values[3]=te;
            hb.buildResponse(version,code,reason);
            
            for (int i=0;i<headers.length;i++)
            {
                if (values[i]==null)	
                    continue;
                hb.header(new ByteArrayBuffer(headers[i]),new ByteArrayBuffer(values[i]));
            }
            
            
            if (content!=null)
            {
                int inc=1+content.length()/chunks;
                Buffer buf=new ByteArrayBuffer(content);
                View view = new View(buf);
                for (int i=1;i<chunks;i++)
                {
                    view.setPutIndex(i*inc);
                    view.setGetIndex((i-1)*inc);
                    hb.content(view, false);
                }
                view.setPutIndex(buf.putIndex());
                view.setGetIndex((chunks-1)*inc);
                hb.content(view, true);
            }
            else
                hb.complete();
        }
    }
    
    private TR[] tr =
    {
       new TR(200,null,null,null),
       new TR(200,null,null,CONTENT),
       new TR(200,null,""+CONTENT.length(),null),
       new TR(200,null,""+CONTENT.length(),CONTENT),
       new TR(200,"text/html",null,null),
       new TR(200,"text/html",null,CONTENT),
       new TR(200,"text/html",""+CONTENT.length(),null),
       new TR(200,"text/html",""+CONTENT.length(),CONTENT),
             
    };
    
    

    String content;
    String f0;
    String f1;
    String f2;
    String[] hdr;
    String[] val;
    int h;
    
    class Handler extends HttpParser.Handler
    {   
        public void content(int index, Buffer ref)
        {
            if (index == 0)
                content= "";
            content= content.substring(0, index) + ref;
        }


        public void startRequest(Buffer tok0, Buffer tok1, Buffer tok2)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= tok0.toString();
            f1= tok1.toString();
            if (tok2!=null)
                f2= tok2.toString();
            else
                f2=null;
            
            // System.out.println(f0+" "+f1+" "+f2);
        }


        /* (non-Javadoc)
         * @see org.mortbay.http.HttpHandler#startResponse(org.mortbay.io.Buffer, int, org.mortbay.io.Buffer)
         */
        public void startResponse(Buffer version, int status, Buffer reason)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= version.toString();
            f1= ""+status;
            if (reason!=null)
                f2= reason.toString();
            else
                f2=null;
        }

        public void parsedHeader(Buffer name,Buffer value)
        {
            hdr[++h]= name.toString();
            val[h]= value.toString();
        }

        public void headerComplete()
        {
            content= null;
        }

        public void messageComplete(int contentLength)
        {
        }


    }

}
