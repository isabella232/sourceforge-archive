/*
 * $Id$
 * 
 * Copyright (c) 1995-1999 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 * CopyrightVersion 1.0
 */

package javax.servlet;

import java.io.IOException;

/**
 * A Servlet is a small program that runs inside a web server. It
 * receives and responds to requests from web clients.
 *
 * <p>All servlets implement this interface. Servlet writers typically
 * do this by subclassing either GenericServlet, which implements the
 * Servlet interface, or by subclassing GenericServlet's descendent,
 * HttpServlet.
 *
 * <p>The Servlet interface defines methods to initialize a servlet,
 * to service requests, and to remove a servlet from the server. These
 * are known as life-cycle methods and are called by the network
 * service in the following manner:
 * <ol>
 * <li>Servlet is created then <b>init</b>ialized.
 * <li>Zero or more <b>service</b> calls from clients are handled
 * <li>Servlet is <b>destroy</b>ed then garbage collected and finalized
 * </ol>
 * 
 * <p>In addition to the life-cycle methods, the Servlet interface
 * provides for a method for the servlet to use to get any startup
 * information, and a method that allows the servlet to return basic
 * information about itself, such as its author, version and copyright.
 *
 * @see GenericServlet
 * @see javax.servlet.http.HttpServlet
 */

public interface Servlet {

    /**
     * Called by the web server when the Servlet is placed into
     * service. This method is called exactly once by the
     * host servlet engine after the Servlet object is instantiated
     * and must successfully complete before any requests can
     * be routed through the Servlet.
     *
     * <p>If a ServletException is thrown during the execution
     * of this method, a servlet engine may not place the servlet
     * into service. If the method does not return within a
     * server defined time-out period, the servlet engine may
     * assume that the servlet is nonfunctional and may not
     * place it into service.
     *
     * @see UnavailableException
     * @see javax.servlet.Servlet#getServletConfig()
     * @param config object containing the servlet's startup-
     * configuration and initialization parameters
     * @exception ServletException if a servlet exception has occurred
     */

    public void init(ServletConfig config) throws ServletException;

    /**
     * Returns a <tt>ServletConfig</tt> object, which contains any
     * initialization parameters and startup configuration for this
     * servlet.  This is the ServletConfig object passed to the init
     * method; the init method should have stored this object so that
     * this method could return it.
     *
     * <p>The servlet writer is responsible for storing the
     * <tt>ServletConfig</tt> object passed to the init method so
     * it may be accessed via this method. For your convience, the
     * <tt>GenericServlet</tt> implementation of this interface
     * already does this.
     *
     * @see javax.servlet.Servlet#init
     */

    public ServletConfig getServletConfig();

    /**
     * Called by the servlet engine to allow the servlet to respond
     * to a request. This method can only be called when the servlet
     * has been properly initialized. The servlet engine may block
     * pending requests to this servlet until initialization is
     * complete. Similarly, when a servlet is removed from service
     * (has its destroy method called), no more requests can be
     * serviced by this instance of the servlet.
     *
     * <p>Note that servlets typically run inside of multi threaded
     * servlet engines that can handle multiple requests simultaneously.
     * It is the servlet writer's responsibility to synchronize access
     * to any shared resources, such as network connections or
     * the servlet's class and instance variables. Information on
     * multi-threaded programming in Java can be found in <a
     * href="http://java.sun.com/Series/Tutorial/java/threads/multithreaded.html">the
     * Java tutorial on multi-threaded programming</a>.
     *
     * @param req the client's request of the servlet
     * @param res the servlet's response to the client
     * @exception ServletException if a servlet exception has occurred
     * @exception IOException if an I/O exception has occurred
     */

    public void service(ServletRequest req, ServletResponse res)
	throws ServletException, IOException;

    /**
     * Allows the servlet to provide information about itself to
     * the host servlet runner such as author, version, and
     * copyright. As this method may be called to display such
     * information in an administrative tool, the string that
     * this method returns should be plain text and not
     * composed of markup of any kind (such as HTML, XML, etc).
     *
     * @return String containing servlet information
     */

    public String getServletInfo();

    /**
     * Called by the servlet engine when the servlet is removed from
     * service. The servlet engine may not call this method until
     * all threads within in the servlet's service method have
     * exited or an engine specified timeout period has passed. After
     * this method is run, the service method may not be called
     * by the servlet engine on this instance of the servlet.
     *
     * <p>This method gives the servlet an opprotunity to clean
     * up whatever resources are being held (e.g., memory, file handles,
     * thread) and makes sure that any persistent state is
     * synchronized with the servlet's current in-memory state.
     */

    public void destroy();
}
