/*
 * $Id$
 * 
 * Copyright (c) 1995-1998 Sun Microsystems, Inc. All Rights Reserved.
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


/**
 * This exception is thrown to indicate a servlet problem.
 *
 */

public class ServletException extends Exception {

    private Throwable rootCause;

    /**
     * Constructs a new ServletException.
     */

    public ServletException() {
	super();
    }

    /**
     * Constructs a new ServletException with the specified message.
     *
     * @param message Message of exception
     */

    public ServletException(String message) {
	super(message);
    }

    /**
     * Constructs a new ServletException with the specified message
     * and root cause.
     *
     * @param message Message of exception
     * @param rootCause Exception that caused this exception to be raised
     */
    
    public ServletException(String message, Throwable rootCause) {
	super(message);
	this.rootCause = rootCause;
    }

    /**
     * Constructs a new ServletException with the specified message
     * and root cause.
     *
     * @param rootCause Exception that caused this exception to be raised
     */

    public ServletException(Throwable rootCause) {
	super(rootCause.getLocalizedMessage());
	this.rootCause = rootCause;
    }
    
    /**
     * Returns the root cause of this exception.
     *
     * @return Throwable
     */
    
    public Throwable getRootCause() {
	return rootCause;
    }
}





