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

package org.gjt.jsp.jsdk20;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletResponse;

/**
 * a JspWriter capable of wrapping another JspWriter
 * for include/forward simulation on jsdk 2.0
 */
public class JspWriterImpl extends org.gjt.jsp.JspWriterImpl {

    /**
     * This is for wrapping JspWriters on include. It will ignore (and don't propagate)
     * close if told so.
     */ 
    public JspWriterImpl (JspWriter out, int bufferSize, boolean propagateClose)
    {
	// FIXME: if setting autoflush() to false something goes wrong
	super(null, bufferSize, true);
	this.response   = null;
	this.out = out;
	this.propagateClose = propagateClose;
	this.allowFlushedClear = true;
    }
}
