// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package org.mortbay.servlets.packageindex;

import java.util.Hashtable;

/** An installed instance of a Package on the local Hard Disk
 *
 * @see PackageIndex
 * @version 1.0 Fri Jun  2 2000
 * @author Matthew Watson (mattw)
 */
public class Package
{
    /* ------------------------------------------------------------ */
    protected String name = null;
    protected String version = null;
    protected String minorVersion = null;
    protected Hashtable doc = new Hashtable();
    protected String path = null;
    protected boolean newest = false;
    /* ------------------------------------------------------------ */
    public Package(String name, String version,
		   String minorVersion, String path)
    {
	this.name = name;
	this.version = version;
	this.minorVersion = minorVersion;
	this.path = path;
    }
    /* ------------------------------------------------------------ */
    public void addDoc(String type, String path){
	doc.put(type, path);
    }
    /* ------------------------------------------------------------ */
    public boolean isNewest()
    {
	return newest;
    }
    public void setNewest(boolean newest)
    {
	this.newest = newest;
    }
    /* ------------------------------------------------------------ */
    public String getName(){
	return name;
    }
    /* ------------------------------------------------------------ */
    public  String getVersion()
    {
	return version;
    }
    /* ------------------------------------------------------------ */
    public String getMinorVersion()
    {
	return minorVersion;
    }
    /* ------------------------------------------------------------ */
    public String getDoc(String type)
    {
	return (String)doc.get(type);
    }
    /* ------------------------------------------------------------ */
    public String getpath()
    {
	return path;
    }
    /* ------------------------------------------------------------ */
    public String toString(){
	return name + "(" + version + "(" + minorVersion + "):" + doc + ")";
    }
    /* ------------------------------------------------------------ */
}
