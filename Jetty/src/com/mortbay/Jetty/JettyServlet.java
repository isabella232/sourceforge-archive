// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.HTML.Include;
import com.mortbay.Util.Code;
import com.mortbay.Util.StringUtil;
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
    static long __minModTime = System.currentTimeMillis();
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {	
        String path=request.getServletPath();        

        File file=(File)
            request.getAttribute("JettyFile");
        if (file==null)
            file = new File(request.getRealPath(path));

        if (file==null || !file.exists())
        {
            response.sendError(404);
            return;
        }
        
        Code.debug("FILE="+file);
        
        JettyPage page = new JettyPage(request.getContextPath(),path);
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
            String path=request.getServletPath();
            
            File file=null;

            file = new File(request.getRealPath(path));
            request.setAttribute("JettyFile",file);
            
            if (file!=null && file.exists())
            {
                lm=file.lastModified();
                if (lm<__minModTime)
                    lm=__minModTime;
            }
            
        }
        catch(Exception e)
        {
            Code.ignore(e);
        }
        return lm;
    }
    
    
}



