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

package org.mortbay.io;

import junit.framework.TestCase;

import org.mortbay.io.nio.NIOBuffer;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BufferTest extends TestCase
{
    Buffer[] buffer;
    
    public static void main(String[] args)
    {
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        buffer=new Buffer[]{
          new ByteArrayBuffer(10),
          new NIOBuffer(10,false),
          new NIOBuffer(10,true)
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    /*
     * 
     */
    public void testBuffer()
        throws Exception
    {
        for (int i=0;i<buffer.length;i++)
        {
            System.err.println(i);
            String t="t"+i;
            Buffer b = buffer[i];
            
            assertEquals(t,0,b.length());
            assertEquals(t,10,b.capacity());
            assertEquals(t,10,b.space());
            
            b.put((byte)0);
            b.put((byte)1);
            b.put((byte)2);
            assertEquals(t,3,b.length());
            assertEquals(t,10,b.capacity());
            assertEquals(t,7,b.space());
            
            assertEquals(t,0,b.get());
            assertEquals(t,1,b.get());
            assertEquals(t,1,b.length());
            assertEquals(t,10,b.capacity());
            assertEquals(t,7,b.space());
            b.compact();
            assertEquals(t,9,b.space());
            
            byte[] ba = { (byte)-1, (byte)3,(byte)4,(byte)5,(byte)6 };
            
            b.put(ba,1,3);
            assertEquals(t,4,b.length());
            assertEquals(t,6,b.space());
            
            byte[] bg = new byte[4];
            b.get(bg,1,2);
            assertEquals(t,2,bg[1]);
            assertEquals(t,3,bg[2]);
            
            
            
            
            
        }
    }

}
