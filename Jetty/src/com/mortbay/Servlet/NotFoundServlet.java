// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

/* ------------------------------------------------------------ */
/** Not Found Servlet.
 * Utility servlet to protect a URI by always responding with 404.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class NotFoundServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException
    {
        res.sendError(404);
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException
    {
        res.sendError(404);
    }
}
