/*
 * $Id$
 * 
 * Copyright (c) 1997-1999 Sun Microsystems, Inc. All Rights Reserved.
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
 */


package javax.servlet.http;

import java.util.Enumeration;

/**
 * A HttpSessionContext is a grouping of HttpSessions associated with a single
 * entity. This interface gives servlets access to  
 * methods for listing the IDs and for retrieving a session based on its ID.
 *
 * <p>Servlets get the HttpSessionContext object by calling the
 * getSessionContext() method of HttpSession.
 *
 * @see HttpSession
 * @deprecated The HttpSessionContext class has been deprecated for security
 * reasons. It will be removed in a future version of the Servlet API.
 */

public interface HttpSessionContext {

    /**
     * This method is deprecated and retained only for binary compatibility.
     * It must always return null.
     *
     * @deprecated This method has been deprecated for security reasons.
     * It will be removed in a future version of the Servlet API.
     */

    public HttpSession getSession (String sessionId);
  
    /**
     * This method is deprecated and retained only for binary compatibility.
     * It must always return an empty enumeration.
     *
     * @deprecated This method has been deprecated for security reasons.
     * It will be removed in a future version of the Servlet API.
     */

    public Enumeration getIds ();
}





