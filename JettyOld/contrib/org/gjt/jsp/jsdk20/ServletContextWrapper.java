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

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspWriter; // jsp:include support
import org.gjt.jsp.JspServlet;

/**
 * This class is used to allow
 * setAttribute() method in application scope
 *
 * @author Serge P. Nekoval
 */
public class ServletContextWrapper implements ServletContext {
    // We need someone to execute forward/include
    // for us if it's jsp pages. (alph)
    private JspServlet jspEngine = null;

  /** An original ServletContext */
  private ServletContext sc;

  /** Attribute storage */
  private Hashtable attrs = new Hashtable();

  /** Create wrapper for the JSP 'application' object */
  public ServletContextWrapper (ServletContext sc, JspServlet jspEngine) {
    this.sc = sc;
    this.jspEngine = jspEngine;
  }

  /**
   * Note: a servlet context's own attributes are merged
   *       with the setAttribute()-supplied.
   *       First we're looking in the hastable, and only THEN
   *       we're trying to use getAttribute().
   */
  public Object getAttribute(String name) {
      Object attr = null;

      synchronized(attrs) {
	  attr = attrs.get(name);
      }
      return attr == null ? sc.getAttribute(name) : attr;
  }

  /**
   * Set the specified attribute.
   * Note: we're under JS API 2.0, so sc.setAttribute() doesn't exists!
   */
  public void setAttribute(String name,
                           Object object) {
      synchronized(attrs) {
	  attrs.put(name, object);
      }
  }

  /*
   * Removes the attribute with the given name from the servlet context. If you remove an attribute, and then use
   * getAttribute(java.lang.String) to retrieve the attribute's value, getAttribute returns null.
   *
   * @param name a String specifying the name of the attribute to be removed
   */
  public void removeAttribute(String name) {
      synchronized(attrs) {
	  attrs.remove(name);
      }
  }

  /**
   * In order to move closer to JS API 2.1 I'm providing
   *  this method. It is related to the attribute support, too.
   * Note: it is not working correctly, because all
   *       attribute names from underlying ServletContext
   *       will be missed. I hope context doesn't have
   *       any vital attributes, though.
   */
  public Enumeration getAttributeNames() {
      synchronized(attrs) {
	  return attrs.keys();
      }
  }

    /**
     * Under JSDK 2.0, there is no way to get access to any other
     * servlet contexts, so this merely returns a self-reference.
     */
    public ServletContext getContext(String path) {
	return this;
    }

    public int getMajorVersion() {
	return 2;
    }

    /** This is a lie. */
    public int getMinorVersion() {
	return 1;
    }

    public URL getResource(String path) throws MalformedURLException {
	return new URL("file:" + getRealPath(path));
    }

    public InputStream getResourceAsStream(String path) {
	try {
	    return getResource(path).openStream();
	} catch (MalformedURLException mue) { 
	} catch (IOException ioe) {
	}
	return null;
    }

    /**
     * To get this to work, you'll need RequestDispatcher.class from
     * the 2.1 JSDK, EVEN IF YOUR SERVLET ENGINE ONLY SUPPORTS 2.0!
     */
    public RequestDispatcher getRequestDispatcher(String path) {
	// FIXME: this won't work, we don't habe enough info here!
	// IMHO we need a pointer to the JspServlet somewhere. (alph)
	return new RequestDispatcherImpl(sc, path);
    }

    /**
     * To get this to work, you'll need RequestDispatcher.class from
     * the 2.1 JSDK, EVEN IF YOUR SERVLET ENGINE ONLY SUPPORTS 2.0!
     * I didn't find a clever way to get the out stream of current
     * page for support of include under jsdk 2.0 so a new method
     * is born (alph)
     */
    public RequestDispatcher getRequestDispatcher(String path, JspWriter jspWriter) {
	return new RequestDispatcherImpl(sc, path, jspWriter, jspEngine);
    }

    /**
     * This will only work if the Throwable is actually an Exception.
     */
    public void log(String message, Throwable throwable) {
	if (throwable instanceof Exception) {
	    log((Exception) throwable, message);
	}
    }

  /**************************************************************
   *           The following methods are redirected to the      *
   *                       wrapped context                      *
   **************************************************************/

  public Servlet getServlet(String name) throws ServletException {
      return sc.getServlet(name); // We know this is deprecated
  }

  public Enumeration getServlets() {
      return sc.getServlets(); // We know this is deprecated
  }

  public Enumeration getServletNames() {
    return sc.getServletNames(); // We know this is deprecated
  }

  public void log(String msg) {
    sc.log(msg);
  }

  public void log(Exception exception, String msg) {
      sc.log(exception, msg); // We know this is deprecated.
  }

  /**
   * Applies alias rules to the specified virtual path and returns the
   * corresponding real path.  For example, in an HTTP servlet,
   * this method would resolve the path against the HTTP service's
   * docroot.  Returns null if virtual paths are not supported, or if the
   * translation could not be performed for any reason.
   * @param path the virtual path to be translated into a real path
   */
  public String getRealPath(String path) {
    return sc.getRealPath(path);
  }

  /**
   * Returns the mime type of the specified file, or null if not known.
   * @param file name of the file whose mime type is required
   */
  public String getMimeType(String file) {
    return sc.getMimeType(file);
  }

  /**
   * Returns the name and version of the network service under which
   * the servlet is running. For example, if the network service was
   * an HTTP service, then this would be the same as the CGI variable
   * SERVER_SOFTWARE.
   */
  public String getServerInfo() {
    return sc.getServerInfo()+"/ GNUJSP 1.0 wrapper";
  }
}
