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

import java.util.Hashtable;
import java.util.Vector;
import java.io.Writer;
import java.io.IOException;

class JspNode {
    // Type designations
    public static final int TEMPLATE_TEXT = 0;
    public static final int JSP_BODY = 1;
    public static final int JSP_EXPRESSION = 2;
    public static final int JSP_SCRIPTLET = 3;
    public static final int JSP_INCLUDE = 4;
    public static final int JSP_FORWARD = 5;
    public static final int JSP_USEBEAN = 6;
    public static final int JSP_GETPROPERTY = 7;
    public static final int JSP_SETPROPERTY = 8;

    // Tag names corresponding to types
    public static final String[] tagNames = new String[] {
	null, "body", "expression", "scriptlet", "include",
	"forward", "useBean", "getProperty", "setProperty" 
    };

    public static final int typeFor(String tag) {
	for (int i = 1; i < tagNames.length; i++) {
	    if (tagNames[i].equals(tag)) return i;
	}
	return TEMPLATE_TEXT;
    }

    private Hashtable attributes;
    private int type;
    private JspNode firstChild;
    private JspNode nextSibling;
    private JspNode parent;
    /**
     * The position, where the data of this node
     * started in source file. 
     */
    private Pos pos = null;

    public JspNode(int type) {
	this(type, null);
    }

    public JspNode(int type, JspNode parent) {
	attributes = new Hashtable();
	this.type = type;
	this.parent = parent;
    }

    public int getType() {
	return type;
    }
    /**
     * The position, where the data of this node
     * started in source file. 
     */
    public void setPos(Pos pos) {
	this.pos = pos;
    }
    /**
     * The position, where the data of this node
     * started in source file. 
     */
    public Pos getPos() { return pos; }

    public JspNode getParent() { return parent; }

    public JspNode getFirstChild() { return firstChild; }

    public JspNode getNextSibling() { return nextSibling; }

    public void addChild(JspNode n) {
	if (firstChild == null) firstChild = n;
	else {
	    JspNode lastChild = firstChild;
	    JspNode next;
	    while ((next = lastChild.getNextSibling()) != null) {
		lastChild = next;
	    }
	    lastChild.nextSibling = n;
	}
    }

    public String getAttribute(String name) {
	return (String) attributes.get(name);
    }

    public String getPageAttribute(String name) {
	if (parent == null) return getAttribute(name);
	else return parent.getPageAttribute(name);
    }

    public String getAttribute(String name, String aDefault) {
	String s = (String) attributes.get(name);
	return ((s == null) ? aDefault : s);
    }

    public void setAttribute(String name, String attribute) {
	if (attribute == null) attributes.remove(name);
	else attributes.put(name, attribute);
    }

    public void setPageAttribute(String name, String attribute) {
	if (parent == null) setAttribute(name, attribute);
	else parent.setPageAttribute(name, attribute);
    }
}
