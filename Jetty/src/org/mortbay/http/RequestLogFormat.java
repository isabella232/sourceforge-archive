// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

/* ------------------------------------------------------------ */
/** Abstract HTTP Request Log format
 * @version $Id$
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public interface RequestLogFormat
{
    public String format(HttpRequest request,
                         HttpResponse response,
                         int responseLength);
}

