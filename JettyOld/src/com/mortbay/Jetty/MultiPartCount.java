
// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.mortbay.Base.*;

/** 
 * @version $Id$
 * @author Greg Wilkins
*/
public class MultiPartCount extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void init()
    {}
    
    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
         throws ServletException, IOException
    {
        try{
            MultiPartResponse multi=new MultiPartResponse(req,res);
            Page page=null;
            Date now=null;

            while (true)
            {
                Code.debug("Loop...");
                multi.startNextPart("text/plain");
                now = new Date();
                multi.out.write(now+"\n\nOne as plain text\n");     
                multi.out.write("\n\nWait until server sends the second part...\n");        
                multi.endPart();

                Thread.sleep(4000);         

                multi.startNextPart("text/html");
                now = new Date();
                page = new Page("Two");
                page.add("<PRE>"+now.toString()+"</PRE><P>");
                page.add(new Heading(1,"This is Two HTML heading"));
                page.add("Wait until server sends the third part...");      
                page.write(multi.out);
                multi.endPart();

                Thread.sleep(4000);
            
                multi.startNextPart("text/html");
                now = new Date();
                page = new Page("Two");
                page.add("<PRE>"+now+"</PRE><P>");
                page.add(new Image("/Images/powered.gif",124,49,0));
                page.add("<P>Wait until server sends the first part again..."); 
                page.write(multi.out);
                multi.endPart();
            
                Thread.sleep(4000);
            }
        }
        catch(Throwable th){
            Code.debug("MultiPartCount ended with " +th);
        }
        finally{
            Code.debug("MultiPartCount complete");
        }
    }    
}



