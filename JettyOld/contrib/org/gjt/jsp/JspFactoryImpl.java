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
	Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp;

import java.io.*;
import javax.servlet.*;
import javax.servlet.jsp.*;


class JspFactoryImpl extends JspFactory {
    // Statically define our engine info.  Since JspEngineInfo is 
    // such a simple class, we just make an anonymous inner class.
    private static JspEngineInfo engineInfo = new JspEngineInfo() {
	public String getImplementationVersion() {
	    return "1.0";
	}
    };

    public PageContext getPageContext(Servlet         servlet,
				      ServletRequest  request,
				      ServletResponse response,
				      String          errorPageURL,
				      boolean         needsSession,
				      int             bufferSize,
				      boolean         autoflush)
    {
	PageContextImpl context = new PageContextImpl ();
	
	context.initialize (servlet, request, response, errorPageURL, 
			    needsSession, bufferSize, autoflush);

	return context;
    }
    
    public void releasePageContext (PageContext pc) {
	pc.release ();
    }
    
    public JspEngineInfo getEngineInfo() {
	return engineInfo;
    }
}
