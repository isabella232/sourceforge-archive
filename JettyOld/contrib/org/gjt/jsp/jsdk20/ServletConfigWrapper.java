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

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.gjt.jsp.JspServlet;

/**
 * This class is provided for JSDK 2.0 engines in order to provide
 * a useful implementation of ServletContext.
 *
 * @author Wes Biggs <wes@gjt.org>
 * @see ServletContextWrapper
 */
public class ServletConfigWrapper implements ServletConfig {
    private ServletConfig source;
    private ServletContext wrappedContext;

    public ServletConfigWrapper(ServletConfig source, ServletContextWrapper context) {
	this.source = source;
	this.wrappedContext = context;
    }

    public ServletContext getServletContext() {
	return wrappedContext;
    }

    public String getInitParameter(String name) {
	return source.getInitParameter(name);
    }

    public Enumeration getInitParameterNames() {
	return source.getInitParameterNames();
    }
}

    
