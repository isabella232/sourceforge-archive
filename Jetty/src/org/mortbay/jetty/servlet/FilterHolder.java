// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import org.mortbay.http.HandlerContext;

/* --------------------------------------------------------------------- */
/** 
 * @see org.mortbay.jetty.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class FilterHolder
    extends Holder
{
    /* ---------------------------------------------------------------- */
    public FilterHolder(HandlerContext handlerContext,
                       String name,
                       String className)
    {
        super(handlerContext,name,className);
    }
    
}





