// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ------------------------------------------------------------------------

package org.mortbay.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;



/* ================================================================ */
/** Handle a multipart MIME response.
 *
 *
 * @version $Id$
 * @author Greg Wilkins
 * @author Jim Crossley
*/
public class MultiPartResponse extends org.mortbay.http.MultiPartResponse
{
    /* ------------------------------------------------------------ */
    /** MultiPartResponse constructor.
     * @param response The ServletResponse to which this multipart
     *                 response will be sent.
     */
    public MultiPartResponse(HttpServletResponse response)
         throws IOException
    {
        super(response.getOutputStream());
        response.setContentType("multipart/mixed;boundary="+getBoundary());
    }
    
};




