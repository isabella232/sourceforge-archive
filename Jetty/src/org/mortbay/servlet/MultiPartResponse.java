// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ------------------------------------------------------------------------

package org.mortbay.servlet;

import org.mortbay.util.Code;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletResponse;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;



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
    public MultiPartResponse(HttpServletRequest request,
                             HttpServletResponse response)
         throws IOException
    {
        super(response.getOutputStream());
        response.setContentType("multipart/mixed;boundary="+getBoundary());
    }
    
};




