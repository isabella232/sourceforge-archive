/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Wes Biggs <wes@gjt.org>

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

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents an exception encountered while parsing a JSP file.
 *
 * @author Wes Biggs <wes@gjt.org>
 */
class ParseException extends IOException 
    implements JspMsg
{
    private Pos pos;

    ParseException() {
	super();
    }
    
    ParseException(String message) {
	this(null, message);
    }

    ParseException(Pos pos, int msgno) {
	this(pos, JspConfig.getLocalizedMsg(msgno));
    }

    ParseException(Pos pos, String message) {
	super((pos != null ? pos + ": " : "") + message);
	this.pos = pos;
    }


    /** Return an HTML-friendly message. */
    void writeHTMLMessage(PrintWriter w) {
	w.println("<HTML><HEAD><TITLE>GNUJSP Parser Exception</TITLE></HEAD><BODY><PRE>");
	if (pos != null) {
	    w.println(JspConfig.getLocalizedMsg
		    (ERR_gnujsp_error_compiling_source_file)
		    +" " + pos.getFile() + ":<br>");
	}

	String msg = getMessage();
	char ch;
	for (int i = 0; i < msg.length(); i++) {
	    ch = msg.charAt(i);
	    if (ch == '<') w.write("&lt;");
	    else if (ch == '>') w.write("&gt;");
	    else if (ch == '&') w.write("&amp;");
	    else w.write(ch);
	}
	w.println("</PRE></BODY></HTML>");
    }
}


