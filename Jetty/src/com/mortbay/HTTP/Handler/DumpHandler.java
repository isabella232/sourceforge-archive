// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.util.*;
import java.text.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Dump request handler.
 *
 * Usefule for testing and debugging.
 * @see
 * @version 1.0 Mon Oct 11 1999
 * @author Greg Wilkins (gregw)
 */
public class DumpHandler extends NullHandler
{
    /* ----------------------------------------------------------------- */
    public String realPath(String pathSpec, String path)
    {
        return "";
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathSpec,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if (!isStarted())
            return;        

        response.setField(HttpFields.__ContentType,
                          HttpFields.__TextHtml);
        ChunkableOutputStream out = response.getOutputStream();

        out.println("<HTML><H1>HTTP Request Dump</H1>");
        out.println("<PRE>path="+request.getPath()+
                    "\nmatch="+
                    PathMap.pathMatch(pathSpec,request.getPath())+
                    "\ninfo="+
                    PathMap.pathInfo(pathSpec,request.getPath())+
                    "</PRE>");
        out.println("<H3>Header:</H3><PRE>");
        out.print(request.toString());
        out.println("</PRE><H3>Parameters:</H3><PRE>");
        Set names=request.getParameterNames();
        Iterator iter = names.iterator();
        while(iter.hasNext())
        {
            String name=iter.next().toString();
            List values=request.getParameterValues(name);
            if (values==null || values.size()==0)
            {
                out.print(name);
                out.println("=");
            }
            else if (values.size()==1)
            {
                out.print(name);
                out.print("=");
                out.println(values.get(0));
            }
            else
            {
                for (int i=0; i<values.size(); i++)
                {
                    out.print(name);
                    out.print("["+i+"]=");
                    out.println(values.get(i));
                }
            }
        }
            
        out.println("</PRE><H3>Content:</H3><PRE>");
        byte[] buf= new byte[4096];
        int len;
        try{
            InputStream in=request.getInputStream();
            while((len=in.read(buf))>=0)
                out.write(buf,0,len);
        }
        catch(IOException e)
        {
            Code.ignore(e);
            out.println(e);
        }
        
        out.println("</PRE><H3>Response:</H3><PRE>");
        out.print(response.toString());
        out.println("</PRE></HTML>");

        // You wouldn't normally set a trailer like this, but
        // we don't want to commit the output to force trailers as
        // it makes test harness messy
        request.getAcceptableTransferCodings();
        if (response.acceptTrailer())
            response.getTrailer().put("TestTrailer","Value");

        request.setHandled(true);
    }
}





