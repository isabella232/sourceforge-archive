// ========================================================================
// Copyright (c) 1996 Intelligent Switched Systems, Sydney
// $Id$
// ========================================================================

package com.mortbay.HTML;
import java.util.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** Abstract Page Factory interface
 */
public interface PageFactory
{
    /** Construct a new named page for the request */
    public Page getPage(String name,
			ServletRequest request,
			ServletResponse response);
}

