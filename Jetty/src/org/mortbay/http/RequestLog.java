// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import org.mortbay.util.LifeCycle;

/* ------------------------------------------------------------ */
/** Abstract HTTP Request Log format
 * @version $Id$
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public interface RequestLog extends LifeCycle
{
    public void log(HttpRequest request,
                    HttpResponse response,
                    int responseLength);
}

