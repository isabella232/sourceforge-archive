// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util.Servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handle an Argument Format Error
 * <p> Object that are being called by ServletDispatch.dispatch calls may
 * implement this interface if they take arbitrary object as parameters and
 * expect ServletDispatch to populate those objects from request
 * parameters. If there are errors converting the parameters into the correct
 * types of the argument object, then the ServletDispatch object will call
 * the argumentFormatError() function on the target object if it implements
 * this interface, passing in the names of the field that did not convert
 * correctly.
 *
 * <p><h4>Notes</h4>
 * <p> This call has nothing to do with whether the request parameters were
 * present or not, simply with whether those present could be parsed. If the
 * user wishes to check whether request parameters are present, it is
 * suggested they initialise their Argument object values with default values
 * that will be recognised as not having been overridden (such as null, or
 * unlikely values for number types, e.g. - MAX_INT).
 *
 * @see com.mortbay.Servlets.ServletDispatch
 * @version 1.0 Sun Jun 11 2000
 * @author Matthew Watson (mattw)
 */
public interface ServletDispatchErrorHandler 
{
    /* ------------------------------------------------------------ */
    /** Handle an Argument Format Error
     * @param method The name of the method being called.
     * @param dispatch The dispatch object
     * @param context The user context
     * @param req
     * @param res
     * @param fields An array of the names of the fields that had format errors
     * @return non-null if the request was handled
     */
    Object argumentFormatError(String method,
			       ServletDispatch dispatch,
			       Object context,
			       HttpServletRequest req,
			       HttpServletResponse res,
			       String fields[]);
    /* ------------------------------------------------------------ */
}
