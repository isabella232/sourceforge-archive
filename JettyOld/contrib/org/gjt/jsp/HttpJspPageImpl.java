/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

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
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, =
  USA.
*/
package org.gjt.jsp;


import javax.servlet.jsp.HttpJspPage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;

/**
 * This is the default superclass of the generated servlet.
 * If you want your generated code to have a different
 * parent, make sure that parent extends this class.
 */

public abstract class HttpJspPageImpl extends HttpServlet
    implements HttpJspPage
{
    // You don't need to override these accessors; they are generated
    // by JavaEmitter.

    public abstract long _gnujspGetTimestamp();
    public abstract String[] _gnujspGetDeps();
    public abstract long _gnujspGetCompilerVersion();

    // hidden and moved to jspService() as per JSP specifiction
    private ServletConfig  config;   

    public HttpJspPageImpl () {
    }
   
    public void jspInit () { }
   
    public void jspDestroy () { }

    public void init (ServletConfig config) throws ServletException {
	this.config      = config;
	super.init (this.config);

	jspInit ();
    }

    public ServletConfig getServletConfig() {
	return config;
    }

    public void destroy () {
	jspDestroy ();
    }

    public void service (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	    _jspService (req, res);
    }

}
