// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import javax.servlet.http.HttpServletRequest;
import java.util.Vector;
import java.util.Enumeration;

/** Class to aid in handling trees of Objects handling recursive servlet
 * dispatching
 * <p> This class can be derived from by objects that handle recursive
 * servlet dispatching to aid with generation of servlet urls.
 *
 * <p> Each node can add the "method" name used to access this object to
 * their address and then use the getUrl methods to generate urls to objects
 * relative to this one in the hierarchy.
 *
 * @see com.mortbay.Util.ServletDispatch
 * @version $Version: $
 * @author Matthew Watson (watsonm)
 */
public class ServletNode
{
    /* ------------------------------------------------------------ */
    private Vector address = null;
    private String urlPath = null;
    /* ------------------------------------------------------------ */
    public Vector getAddress(){
	if (address == null) address = new Vector();
	return address;
    }
    /* ------------------------------------------------------------ */
    public void addAddressElement(Object elem){
	getAddress().addElement(elem);
	urlPath = null;
    }
    /* ------------------------------------------------------------ */
    public void setAddress(Vector address){
	this.address = (Vector)address.clone();
	urlPath = null;
    }
    /* ------------------------------------------------------------ */
    public String getUrlPath(HttpServletRequest req){
	if (urlPath == null)
	    urlPath = (address != null) ? getRelativeUrlPath(0) : "";
	return req.getServletPath() + urlPath;
    }
    /* ------------------------------------------------------------ */
    public String getParentUrlPath(HttpServletRequest req, int level){
	return req.getServletPath() + getRelativeUrlPath(level);
    }
    /* ------------------------------------------------------------ */
    private String getRelativeUrlPath(int level){
	StringBuffer sb = new StringBuffer();
	int depth = address.size() - level;
	int i = 0;
	for (Enumeration enum = address.elements();
	     i < depth && enum.hasMoreElements(); i++)
	{
	    sb.append("/");
	    sb.append(enum.nextElement());
	}
	return sb.toString();
    }
    /* ------------------------------------------------------------ */
};
