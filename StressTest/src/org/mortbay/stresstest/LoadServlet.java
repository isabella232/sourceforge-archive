// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.stresstest;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/* ------------------------------------------------------------ */
public class LoadServlet extends HttpServlet
{
    public static char[] __chars=new char[16384];
    public static byte[] __bytes=new byte[16384];
    static
    {
	for (int i=0;i<16384;i++)
	{
	    __chars[i]=(char)('a'+(i%28));
	    if (i%28==26)
		__chars[i]='\r';
	    if (i%28==27)
		__chars[i]='\n';
	    __bytes[i]=(byte)__chars[i];
	}
    }

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException
    {
        doGet(request,response);
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException
    {
        int lines=10;
        String l=request.getParameter("lines");
        if (l!=null)
            lines=Integer.parseInt(l);
        int block=-1;
        String b=request.getParameter("block");
        if (b!=null)
            block=Integer.parseInt(b);

        response.setContentType("text/plain");
        
	if (block>0 && lines==0)
	{
            response.getOutputStream().write(__bytes,0,block);
	}
        else
        {
            String info=request.getPathInfo();
            String encoding=request.getParameter("encoding");
            if (encoding!=null)
                request.setCharacterEncoding(encoding);
            
            // Reader r = request.getReader();
            // while (r.read()!=-1);
            
            
            PrintWriter out = response.getWriter();
            for (int i=0;i<lines;i++)
            {
                out.write("Now ");
                out.write("is the time for all good men to come to the aid of the party\n");
            }
            
            if (block>0)
                out.write(__chars,0,block);
            
            out.flush();
        }
    }
    

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Load Servlet";
    }
    
}
