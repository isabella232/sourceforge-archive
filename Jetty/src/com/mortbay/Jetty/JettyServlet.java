// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.HTML.Include;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import com.mortbay.Util.StringUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* ------------------------------------------------------------ */
/** Jetty Demo site servlet.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class JettyServlet extends HttpServlet
{
    public static long __minModTime = System.currentTimeMillis();
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {	
        String path=request.getServletPath();
        
        Resource resource=(Resource)request.getAttribute("JettyResource");
        if (resource==null)
            resource =
                Resource.newResource(getServletContext().getResource(path));
        
        if (resource==null || !resource.exists())
        {
            response.sendError(404);
            return;
        }
        
        Code.debug("Resource=",resource);

        JettyPage page = new JettyPage(request.getContextPath(),path);
        if (page.getSection()!=null)
        {
            response.setContentType("text/html");
            page.add(new Include(resource.getInputStream()));
            PrintWriter pout = response.getWriter();
            page.write(pout);
            pout.flush();
        }
        else
        {
            String type=getServletContext().getMimeType(resource.getName());
            if (type!=null)
                response.setContentType(type);
            if(resource.length()>0)
                response.setContentLength((int)resource.length());
            IO.copy(resource.getInputStream(),
                    response.getOutputStream());
        }
    }

    /* ------------------------------------------------------------ */
    public long getLastModified(HttpServletRequest request)
    {
        long lm=-1;
        try{
            String path=request.getServletPath();
            
            Resource resource=
                Resource.newResource(getServletContext().getResource(path));

            request.setAttribute("JettyResource",resource);
            
            if (resource==null || !resource.exists())
            {
                lm=resource.lastModified();
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
