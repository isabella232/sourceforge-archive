// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.HTML.Include;
import com.mortbay.Util.Code;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * This is an example of a simple Servlet
 */
public class JettyServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {	
        String pathInfo=request.getPathInfo();

        File file=(File)
            request.getAttribute("JettyFile");
        if (file==null && "/".equals(pathInfo))
        {
            pathInfo="/index.html";
            String index=getServletContext().getRealPath("/index.html");
            if (index!=null)
                file=new File(index);
        }
        
        if (file==null)
        {    
            if (!pathInfo.endsWith(".html") && !pathInfo.endsWith(".txt") )
                return;
            
            file = new File(request.getPathTranslated());
        }

        if (file==null || !file.exists())
            return;
        
        Code.debug("FILE="+file);
        
        JettyPage page = new JettyPage(pathInfo);
        if (page.getSection()==null)
            return;
        
        page.add(new Include(file));
        
        PrintWriter pout = response.getWriter();
        page.write(pout);
        pout.flush();
    }
    

    /* ------------------------------------------------------------ */
    public long getLastModified(HttpServletRequest request)
    {
        long lm=-1;
        try{
            String pathInfo=request.getPathInfo();
            File file=null;

            if ("/".equals(pathInfo))
            {
                pathInfo="/index.html";
                String filename=request.getPathTranslated()+"index.html";
                file = new File(filename);
            }
            else if (pathInfo.endsWith(".html"))
            {
                file = new File(request.getPathTranslated());
            }
            
            if (file!=null && file.exists())
            {
                request.setAttribute("JettyFile",file);
                lm=file.lastModified();
            }
        }
        catch(Exception e)
        {
            Code.ignore(e);
        }
        return lm;
    }
    
    
}



