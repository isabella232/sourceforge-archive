/*
  GNUJSP - a free JSP1.0 implementation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp.jsdk20;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import org.gjt.jsp.JspServlet;

/**
 * Provides an implementation of RequestDispatcher as mandated by
 * Servlet API 2.1.  This is used by ServletContextWrapper in attempt
 * to provide support for jsp:include and jsp:forward when the underlying
 * servlet engine only supports the 2.0 API.
 *
 * @author Wes Biggs <wes@gjt.org>, Carsten Heyl <alph@gjt.org>
 */
public class RequestDispatcherImpl implements RequestDispatcher {
    private ServletContext sc;
    private String path;
    private JspServlet jspServletEngine = null;
    private JspWriter jspWriter = null;


    RequestDispatcherImpl(ServletContext sc, String path) {
	this.sc = sc;
	this.path = path;
    }

    RequestDispatcherImpl(ServletContext sc,
			String path,
			JspWriter jspWriter,
			JspServlet jspServletEngine 
			) {
	this.sc = sc;
	this.path = path;
	this.jspServletEngine = jspServletEngine;
	this.jspWriter = jspWriter;
    }

    public void forward(ServletRequest req, ServletResponse res) 
	throws ServletException, IOException
    {
	jspServletEngine.doForward(sc,
				   path,
				   req, 
				   res);
    }
    public void include(ServletRequest req, ServletResponse res) 
    	throws ServletException, IOException
    {
	jspServletEngine.doInclude(sc,
				   path,
				   jspWriter,
				   req, 
				   res);
    }
}
