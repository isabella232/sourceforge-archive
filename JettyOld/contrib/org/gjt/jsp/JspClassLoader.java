/*
	GNUJSP - a free JSP implementation
	Copyright (C) 1998-1999, Vincent Partington <vinny@klomp.org>
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

class JspClassLoader extends ClassLoader {

    File repository;
    

    public JspClassLoader (File repository) {
	this.repository = repository;
    }

    protected Class loadClass (String name, boolean resolve) 
	throws ClassNotFoundException
    {
	Class  c = findLoadedClass (name);
	
	if (c == null) {
	    try {
		c = findSystemClass (name);
	    } catch (ClassNotFoundException e) {
		
		byte[] bits = loadClassData (name);
		
		if (bits == null) {
		    	
		    ClassLoader cl = JspServlet.class.getClassLoader(); 

		    if (cl != null) 
			c = cl.loadClass (name);

		} else {

		    c = defineClass (name, bits, 0,bits.length);

		    if (resolve)

			resolveClass (c);
		    }
		}
	    
	    if (c == null)
		throw new ClassNotFoundException (name);
	}

	return c;
    }
    
    private byte[] loadClassData (String className) {
	File             file = JspServlet.getFileName (repository, className, ".class", false);
	byte[]           buf  = null;
	FileInputStream	 in   = null;
	int              n    = 0;
	int              pos  = 0;
	
	if (file.exists ()) {

	    buf = new byte [(int) file.length ()];
	    
	    try {
		in = new FileInputStream (file);
		
		while (pos < buf.length &&
		       (n = in.read (buf, pos, buf.length - pos)) != -1) {
		    pos += n;
		}
	    } catch (IOException e) {
		buf = null;
	    } finally {
		if (in != null) {
		    try { in.close(); }
		    catch (IOException e) { }
		}
	    }
	}

	return buf;
    }
    
}
