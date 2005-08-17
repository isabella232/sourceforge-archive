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

package org.mortbay.thread;

import junit.framework.TestCase;

public class TimerTest extends TestCase
{
    Timer timer = new Timer();
    Timer.Timeout[] timeout;
    
    public static void main(String[] args)
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        timer=new Timer();
        timer.setDuration(1000000);
        timeout= new Timer.Timeout[10]; 
        
        for (int i=0;i<timeout.length;i++)
        {
            timeout[i]=new Timer.Timeout();
            timer.setNow(1000+i*100);
            timer.add(timeout[i]);
        }
        timer.setNow(100);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        // TODO Auto-generated method stub
        super.tearDown();
    }
    
    
    public void testExpiry()
    {
        timer.setDuration(200);
        timer.setNow(1500);
        timer.tick();
        
        for (int i=0;i<timeout.length;i++)
        {
            assertEquals("isExpired "+i,i<3, timeout[i].isExpired());
        }
    }
    
    public void testCancel()
    {
        timer.setDuration(200);
        timer.setNow(1700);

        for (int i=0;i<timeout.length;i++)
            if (i%2==1)
                timeout[i].cancel();

        timer.tick();
        
        for (int i=0;i<timeout.length;i++)
        {
            assertEquals("isExpired "+i,i%2==0 && i<5, timeout[i].isExpired());
        }
    }

}
