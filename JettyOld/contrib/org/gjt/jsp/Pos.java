/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Carsten Heyl <alph@gjt.org>

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

/**
 * hold a file position for parsing information
 */ 

class Pos 
    implements JspMsg
{
    private static final String javaCodePrefix = "// line:";
    private String file = null;
    private String extraInfo = null;
    private int line = 0;

    /**
     * constructor from filename and line number
     */
    public Pos(String f, int l) {
	file = f;
	line = l;
    }
    /**
     * constructor from code line
     */
    public Pos(String l) {
	if(!l.startsWith(javaCodePrefix)) {
	    throw new IllegalArgumentException
		(JspConfig.getLocalizedMsg(ERR_gnujsp_internal_error)
		 + ": "
		 + JspConfig.getLocalizedMsg(ERR_gnujsp_illegal_code_line_format));
	}
	// strip prefix
	l = l.substring(javaCodePrefix.length());
	// format is <file-name>:<line-number>
	int sep = l.lastIndexOf(":");
	file = l.substring(0,sep);

	String pastFile = l.substring(sep+1);
	sep = pastFile.indexOf("#");
	if(sep != -1) {
	    extraInfo = pastFile.substring(sep+1);
	    pastFile = pastFile.substring(0, sep);
	}
	line = Integer.valueOf(pastFile).intValue();
    }
    public String getFile() { return file; }
    public int getLine() { return line; }
    public String toString() {
	return file+":"+line;
    }
    /**
     * get a java code representation of this position
     */ 
    public String toJavaCode() {
	if(extraInfo != null) 
	    return javaCodePrefix+file+":"+line+"#"+extraInfo;
	else
	    return javaCodePrefix+file+":"+line;
    }
    /**
     * get a java code representation of this position with additional 
     * caller info
     * info '*' means: source code evtl. was more than one line but 
     *                 generated code has less lines (e.g. template text)
     * info '+' means: source code was 1 line but 
     *                 generated code has more lines (e.g. jsp:useBean)
     */ 
    public String toJavaCode(String s) {
	return javaCodePrefix+file+":"+line+"#"+s;
    }
    /**
     * get the prefix of the java code representation
     * (e.g. for line number searches)
     */ 
    public static String getCodePrefix() {
	return javaCodePrefix;
    }
    public boolean hasExtraInfo() { return extraInfo != null; }
    public String getExtraInfo() { return extraInfo; }
}
