// ========================================================================
// Copyright (c) 1999, 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.tools.servlet;

import javax.servlet.http.HttpServletRequest;
import java.util.Vector;
import java.util.Enumeration;

/** Class to aid in handling trees of Objects handling recursive servlet dispatching.
 * <p> This class can be derived from by objects that handle recursive
 * servlet dispatching to aid with generation of servlet urls.
 *
 * <p> Each node can add the "method" name used to access this object to
 * their address and then use the getUrl methods to generate urls to objects
 * relative to this one in the hierarchy.
 *
 * @see org.mortbay.util.ServletDispatch
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
    public String getPath()
    {
        if (urlPath == null)
            urlPath = (address != null) ? getRelativeUrlPath(0) : "";
        return urlPath;
    }
    
    /* ------------------------------------------------------------ */
    public String getUrlPath(HttpServletRequest req)
    {
        if (urlPath == null)
            urlPath = (address != null) ? getRelativeUrlPath(0) : "";
        return req.getServletPath() + urlPath;
    }
    
    /* ------------------------------------------------------------ */
    public String getParentUrlPath(HttpServletRequest req, int level)
    {
        return req.getServletPath() + getRelativeUrlPath(level);
    }
    
    /* ------------------------------------------------------------ */
    public String getBaseName()
    {
        if (address==null || address.size()==0)
            return null;
        return address.lastElement().toString();
    }
    
    /* ------------------------------------------------------------ */
    private String getRelativeUrlPath(int level)
    {
        StringBuffer sb = new StringBuffer(32);
        int depth = address.size() - level;
        for (int i=0;i<depth;i++)
        {
            sb.append("/");
            sb.append(address.elementAt(i));
        }
        return sb.toString();
    }
    
    /* ------------------------------------------------------------ */
}
