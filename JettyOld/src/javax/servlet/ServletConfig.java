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

import java.util.Enumeration;

/**
 * Defines an object that a servlet engine generates to pass
 * configuration information to a servlet when such servlet
 * is initialized. The configuration information that this servlet
 * will have access to is a set of name/value pairs that
 * describe initialization parameters and the <tt>ServletContext</tt>
 * object which describes the context within which the servlet
 * will be running.
 */

public interface ServletConfig {

    /**
     * Returns the <tt>ServletContext</tt> for this servlet.
     */

    public ServletContext getServletContext();

    /**
     * Returns a string containing the value of the named
     * initialization parameter of the servlet, or null if the
     * parameter does not exist.  Init parameters have a single string
     * value; it is the responsibility of the servlet writer to
     * interpret the string.
     *
     * @param name the name of the parameter whose value is requested
     */

    public String getInitParameter(String name);

    /**
     * Returns the names of the servlet's initialization parameters
     * as an enumeration of strings, or an empty enumeration if there
     * are no initialization parameters.
     */

    public Enumeration getInitParameterNames();
    
}








