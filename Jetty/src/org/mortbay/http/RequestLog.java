// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import org.mortbay.util.LifeCycle;
import java.io.Serializable;

/* ------------------------------------------------------------ */
/** Abstract HTTP Request Log format
 * @version $Id$
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public interface RequestLog
    extends LifeCycle,
            Serializable
{
    public void log(HttpRequest request,
                    HttpResponse response,
                    int responseLength);
}

