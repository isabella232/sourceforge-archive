// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets.PackageIndex;

import com.mortbay.Util.Code;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Collection;
import java.util.Iterator;
import java.io.File;

/** A specification of a Package to be scanned for on the Local Hard Disk.
 * <p> This class contains the details of where a package could be located
 * etc. and has utilities for locating all the actual Packages.
 *
 * @see com.mortbay.Servlets.PackageIndex.Package
 * @version 1.0 Fri Jun  2 2000
 * @author Matthew Watson (mattw)
 */
public class PackageSpec
{
    /* ------------------------------------------------------------ */
    // Number of levels of directories before we find the inbstallation/s
    protected int versioning = 1;
    // List of packages directories
    protected Vector basePath = new Vector();
    // Map of doco type to Vecors of their search paths
    protected Hashtable docoPaths = new Hashtable();
    // Versions and directories to ignore
    protected Vector ignore = new Vector();
    // The short name of the product
    protected String name = null;
    // The short description of the product
    protected String desc = null;
    /* ------------------------------------------------------------ */
    protected TreeMap packages = null;
    /* ------------------------------------------------------------ */
    /** Build a Package Spec for a versioned product.
     * @param name The pathname name of the product
     * @param desc A longer description of the product to be used for link
     * names etc.
     * @param basePath The path to the packages directory
     */
    public PackageSpec(String name, String desc, String basePath){
	this(name, desc, basePath, 1);
    }
    /* ------------------------------------------------------------ */
    /** Build a Package Spec
     * @param name The pathname name of the product
     * @param desc A longer description of the product to be used for link
     * names etc. 
     * @param basePath The path to the packages directory
     * @param versioning Levels of directories to the installation/s
     */
    public PackageSpec(String name, String desc,
		       String basePath, int versioning)
    {
	this.name = name;
	this.desc = desc;
	this.versioning = versioning;
	this.basePath.addElement(basePath);
    }
    /* ------------------------------------------------------------ */
    public PackageSpec addBasePath(String path){
	basePath.addElement(path);
	return this;
    }
    /* ------------------------------------------------------------ */
    public PackageSpec addDocPath(String type, String path){
	Vector docPath = (Vector)docoPaths.get(type);
	if (docPath == null) {
	    docPath = new Vector();
	    docoPaths.put(type, docPath);
	}
	docPath.addElement(path);
	return this;
    }
    /* ------------------------------------------------------------ */
    public PackageSpec addIgnoreDir(String name){
	ignore.addElement(name);
	return this;
    }
    /* ------------------------------------------------------------ */
    public synchronized Package getPackage(String key){
	if (packages == null)
	    checkPaths();
	return (Package)packages.get(key);
    }
    /* ------------------------------------------------------------ */
    public synchronized Iterator getPackages(){
	if (packages == null)
	    checkPaths();
	return packages.values().iterator();
    }
    /* ------------------------------------------------------------ */
    public String getName() {
	return name;
    }
    /* ------------------------------------------------------------ */
    public String getDescription() {
	return desc;
    }
    /* ------------------------------------------------------------ */
    public synchronized void checkPaths(){
	packages = new TreeMap(new PackageVersionOrderer());
	Enumeration paths = basePath.elements();
	while (paths.hasMoreElements()){
	    String path = paths.nextElement().toString() + name;
	    checkPaths(path, null, null, versioning);
	}
    }
    /* ------------------------------------------------------------ */
    private void checkPaths(String base, String extra,
			    String basename, int levels)
    {
	// First check if we should ignore this path
	if (basename != null && ignore.contains(basename))
	    return;
	// Look for versions at the base path
	String path = (base.endsWith("/") ? base : base + "/");
	if (extra != null)
	    path = path + (extra.endsWith("/") ? extra : extra + "/");
	if (basename != null)
	    path = path + basename;
	File dir = new File(path);
	try {
	    if (!dir.isDirectory() || !dir.canRead())
		return;
	    Code.debug("Path Found:", dir.getPath());
	    if (levels == 0)
		checkVersion(base, extra, basename);
	    else {
		String[] subDirs = dir.list();
		String nextra = (extra == null ? basename :
				 extra + "/" + basename);
		// For each sub-dir (version)
		for (int i = 0; i < subDirs.length; i++)
		    checkPaths(base, nextra, subDirs[i], levels-1);
	    }
	} catch (Exception ex){
	    Code.debug(ex);
	}
    }
    /* ------------------------------------------------------------ */
    private boolean checkPath(String path){
	File pathf = new File(path);
	if (pathf.canRead() &&
	    (pathf.isFile() ||
	     (path.endsWith("/") && pathf.isDirectory())))
	{
	    Code.debug("Found: " + path);
	    return true;
	}
	return false;
    }
    /* ------------------------------------------------------------ */
    private void checkVersion(String base, String extra, String version){
	String path = (base.endsWith("/") ? base : base + "/");
	if (extra != null)
	    path = path + (extra.endsWith("/") ? extra : extra + "/");
	if (version != null)
	    path = path + version;
	File subDir = new File(path);
	if (!subDir.isDirectory() || !subDir.canRead()) return;
	Code.debug("Found version: "+ path);
			
	// Check we haven't already found one...
	String key = (version == null ? "-" : version);
	if (extra != null) key = key + "/" + extra;
	if (packages.get(key) != null){
	    Code.debug("Already found: " + name + key);
	    return;
	}
			    
	// Look for possible doco
	Package prod = new Package(name, version, extra, base);
	for (Enumeration types = docoPaths.keys(); types.hasMoreElements();)
	{
	    String type = types.nextElement().toString();
	    Vector pathsV = (Vector)docoPaths.get(type);
	    for (Enumeration paths = pathsV.elements();
		 paths.hasMoreElements();)
	    {
		String pathn = path + "/" + paths.nextElement();
		if (checkPath(pathn)){
		    prod.addDoc(type, pathn);
		    break;
		}
	    }
	}
	packages.put(key, prod);
    }
    /* ------------------------------------------------------------ */
    public String toString(){
	StringBuffer sb = new StringBuffer();
	sb.append(name);
	sb.append("(");
	sb.append(desc);
	sb.append(")\n");
	for (Iterator enum = getPackages(); enum.hasNext();){
	    sb.append("\t");
	    sb.append(enum.next().toString());
	    sb.append("\n");
	}
	return sb.toString();
    }
    /* ------------------------------------------------------------ */
}
