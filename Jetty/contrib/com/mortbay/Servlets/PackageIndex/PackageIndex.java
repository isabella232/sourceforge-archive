// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets.PackageIndex;

import com.mortbay.Util.Code;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.ResourceBundle;

import com.mortbay.Util.Servlet.DispatchServlet;
import com.mortbay.Util.Servlet.ServletDispatch;
import com.mortbay.Util.Converter.ConverterSet;
import com.mortbay.Util.Converter.ObjectConverter;
import com.mortbay.Util.Converter.ArrayConverter;
import com.mortbay.Util.PropertyTree;
import com.mortbay.HTML.Page;
import com.mortbay.HTML.Heading;
import com.mortbay.HTML.Block;
import com.mortbay.HTML.List;
import com.mortbay.HTML.Link;
import com.mortbay.HTML.Form;
import com.mortbay.HTML.Select;
import com.mortbay.HTML.Input;
import com.mortbay.HTML.Table;
import com.mortbay.HTML.Break;
import com.mortbay.HTML.FrameSet;
import com.mortbay.HTML.Element;

/** Servlet for serving a documentation index.
 * <p> This servlet can be configured with paths etc for different packages
 * (optionally versioned packages) and this servlet will search the local
 * hard disk throught the paths given looking for documentation
 * references. These are then compiled and presented to the user in a number
 * of different formats, the most usefule being a configuration screen where
 * the user can select which packages (and which versions of those packages)
 * to display in an index format for easy selection of the required
 * documentation.
 *
 * <p> Documentation can be divided into arbitrary sections, each package on
 * the local hard disk having documentation in one or more sections. The
 * properties are read using the mortbay PropertyTree object, so that
 * individual packages can easily be configured using global defaults if
 * appropriate.
 *
 * <p><h4>Usage</h4>
 * See the enclosed property file PackageIndex.prp on examples of how to
 * configure this servlet.
 *
 * @see
 * @version 1.0 Fri Jun  2 2000
 * @author Matthew Watson (mattw)
 */
public class PackageIndex extends DispatchServlet
{
    /* ------------------------------------------------------------ */
    public PackageIndex(){
	super("Documentation Index Servlet");
    }
    /* ------------------------------------------------------------ */
    protected Vector packages = new Vector();
    protected boolean mapToFile = false;
    protected String httpIndexPrefix = null;
    protected String httpIndexTruncate = null;
    protected Dictionary docTypes = null;
    protected int tocTableBorderWidth = 0;
    /* ------------------------------------------------------------ */
    public static class ServletArgs
    {
	public boolean mapToFile = false;
        public String httpIndexPrefix = null;
        public String httpIndexTruncate = null;
	public int tocTableBorderWidth = 0;
    }
    public static class PackageSpecArgs
    {
	public String description = "???";
	public String paths[] = null;
	public int versioning = 1;
	public String ignore[] = null;
    }
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
	 throws ServletException
    {
	super.init(config);

        // create a property tree of the config
	PropertyTree sconfig = new PropertyTree();
	Enumeration initE = null;
        ResourceBundle props = null;
        
        String resource = config.getInitParameter("PackageProperties");
        if (resource != null){
            props = ResourceBundle.getBundle(resource);
            initE = props.getKeys();
        } else {
            initE = config.getInitParameterNames();
        }
	while (initE.hasMoreElements()){
	    String key = (String)initE.nextElement();
	    if (!key.equals(Page.PageType)){
                String val = (props == null ?
                              config.getInitParameter(key) :
                              props.getString(key));
		sconfig.put(key, val);
            }
        }

	// A converter for parsing args
	ConverterSet cs = new ConverterSet();
	cs.registerPrimitiveConverters();
	cs.register(new ObjectConverter());
	cs.register(new ArrayConverter(":;,"));

	// Get the docTypes and barf if it is not there
	docTypes = sconfig.getTree("sections");
	if (docTypes == null)
	    throw new ServletException("No doco sections defined");

	// The servlet args
	ServletArgs sargs = (ServletArgs)
	    cs.convert(sconfig, ServletArgs.class, cs);
	mapToFile = sargs.mapToFile;
	tocTableBorderWidth = sargs.tocTableBorderWidth;
        httpIndexPrefix = sargs.httpIndexPrefix; 
        httpIndexTruncate = sargs.httpIndexTruncate;
        
	// The package specs...
	PropertyTree packconfig = sconfig.getTree("pkgs");
	for (Enumeration enum = packconfig.getRealNodes();
	     enum.hasMoreElements();)
	{
	    String packName = (String)enum.nextElement();

	    // watch out for defaults!
	    if (packName.equals("*")) continue;
	    
	    PropertyTree packageArgs = packconfig.getTree(packName);
	    PackageSpecArgs args = (PackageSpecArgs)
		cs.convert(packageArgs, PackageSpecArgs.class, cs);
	    if (args.paths != null && (args.paths.length > 0)){
		PackageSpec spec =
		    new PackageSpec(packName,
				    args.description,
				    args.paths[0] + "/",
				    args.versioning);
		for (int i = 1; i < args.paths.length; i++)
		    spec.addBasePath(args.paths[i]);
		for (int i = 0; i < args.ignore.length; i++)
		    spec.addIgnoreDir(args.ignore[i]);
		boolean pathAdded = false;
		for (Enumeration types = docTypes.keys();
		     types.hasMoreElements();)
		{
		    String type = (String)types.nextElement();
		    String paths = (String)packageArgs.get(type);
		    if (type != null){
			String pathsAr[] = new String[1];
			pathsAr = (String[])
			    cs.convert(paths, pathsAr.getClass(), cs);
			for (int i = 0; i < pathsAr.length; i++){
			    spec.addDocPath(type, pathsAr[i]);
			    pathAdded = true;
			}
		    }
		}
		if (pathAdded) packages.addElement(spec);
	    }
	}
    }
    /* ------------------------------------------------------------ */
    /* Servlet public methods */
    /* ------------------------------------------------------------ */
    public Page checkPaths(HttpServletRequest req, HttpServletResponse res){
	for (Enumeration enum = packages.elements(); enum.hasMoreElements();){
	    PackageSpec spec = (PackageSpec)enum.nextElement();
	    spec.checkPaths();
	}
	Page page = getPage(pageType, req, res);
	page.title("Paths Re-Checked OK...");
	return page;
    }
    /* ------------------------------------------------------------ */
    // Returns a default page if no path gives, with a full list of all doco
    // and a link to the makeIndex page. If given a doco type, will display
    // a page of all the doco of that type, otherwise, an error page...
    public Object defaultDispatch(String method,
				  ServletDispatch dispatch,
				  Object context,
				  HttpServletRequest req,
				  HttpServletResponse res)
    {
	Page page = getPage(pageType, req, res);
	page.setBase(req.getParameter("target") , null);
	// Print all packages, or only ones with doco?
	boolean all = ServletDispatch.parseBooleanArg(false, "all", req);
	String type = req.getParameter("type");

	page.title("Documentation Index");
	if (method != null){
	    if (docTypes.get(method) != null){
		page.add(new Heading(3, docTypes.get(method)));
		page.add(packageList(new ProdPrint(all, method)));
	    } else {
		page.title("Unknown Method");
		page.add(new Heading(2, "Bad URL Path: \"" + method + "\""));
	    }
	} else {
	    boolean merge =
		ServletDispatch.parseBooleanArg(false, "merge", req);
	    if (type == null || "all".equals(type))
		for (Enumeration types = docTypes.keys();
		     types.hasMoreElements();)
		{
		    String typeName = (String)types.nextElement();
		    page.add(new Heading(3, docTypes.get(typeName)));
		    page.add(packageList(new ProdPrint(all, typeName)));
		}
	    else {
		page.add(new Heading(3, docTypes.get(type)));
		page.add(packageList(new ProdPrint(all, type)));
	    }
	}
	page.add(new Link(req.getServletPath() + "makeIndex", "Make Index"));
	page.add(Break.line);
	Form form = new Form(req.getServletPath());
	Table table = new Table(0);
	page.add(form);
	form.add(table);
	table.addHeading("View");
	Select sel = new Select("type", false);
	table.addCell(sel);
	sel.add("All", false, "all");
	for (Enumeration types = docTypes.keys(); types.hasMoreElements();)
	{
	    String typeName = (String)types.nextElement();
	    sel.add(docTypes.get(typeName), typeName.equals(type), typeName);
	}
	sel = new Select("all", false);
	table.addCell(sel);
	sel.add("Versions with doco only", !all, "false");
	sel.add("All versions", all, "true");
	table.add(new Input(Input.Submit, "View", "View"));
	return page;
    }
    /* ------------------------------------------------------------ */
    public Page makeIndex(HttpServletRequest req,
			  HttpServletResponse res)
    {
	Page page = getPage(pageType, req, res);
	page.title("Documentation Index Builder");
	page.add("This page allows you to generate a tailor-made page of the available Documentation. Select the versions of the products you would like to have on your index and then hit \"Make Index\". The index will appear on the left-hand side of a framed page, and you can specify the width of the index frame (or resize it later). You can bookmark the resulting index for later use.");
	Form form = new Form(req.getServletPath() + "cfgIndex");
	form.method("GET");
	page.nest(form);
	Table table = new Table(0);
	page.add(table);
	table.newRow();
	table.addHeading("Index Frame Width");
	table.addCell(new Input(Input.Text, "frameWidth", "25%"));
	table.newRow();
	for (Enumeration types = docTypes.keys(); types.hasMoreElements();)
	{
	    String type = (String)types.nextElement();
	    table.addCell(new Heading(3, docTypes.get(type)), "COLSPAN=2");
	    addPackageSelect(req, table, new ProdPrint(false, type), type);
	    table.newRow();
	}
	table.addCell(new Input(Input.Submit, "submit", "Make Index"),
		      "COLSPAN=2").cell().center();
	return page;
    }
    /* ------------------------------------------------------------ */
    public Page cfgIndex(HttpServletRequest req,
			 HttpServletResponse res)
    {
	String frameWidth = req.getParameter("frameWidth");
	if (frameWidth == null) frameWidth = "25%";
	FrameSet fs = new FrameSet("Documentation Index", frameWidth +
				   ",*", null);
	String url = req.getServletPath()
	    + "cfgIndexToc?" + req.getQueryString();
	fs.frame(0,0).name("toc", url);
	fs.frame(1,0).name("display", url);
	return fs;
    }
    /* ------------------------------------------------------------ */
    public void cfgIndexToc(HttpServletRequest req,
                            HttpServletResponse res)
        throws java.io.IOException
    {
        addCfgIndexToc(req, res, false);
    }
    /* ------------------------------------------------------------ */
    public void cfgIndexToc2(HttpServletRequest req,
                             HttpServletResponse res)
        throws java.io.IOException
    {
        addCfgIndexToc(req, res, true);
    }
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private void addCfgIndexToc(HttpServletRequest req,
                                HttpServletResponse res,
                                boolean smallIndex)
	throws java.io.IOException
    {
	Page page = getPage(pageType, req, res);
	page.title("Documentation Index");
	page.setBase("display", null);
	for (Enumeration types = docTypes.keys(); types.hasMoreElements();){
	    String type = (String)types.nextElement();
	    Table table = new Table(tocTableBorderWidth);
	    table.width("100%");
	    table.center();
	    page.add(table);
	    table.addHeading(docTypes.get(type));
	    table.newRow();
	    for (Enumeration enum = packages.elements();
		 enum.hasMoreElements();){
		PackageSpec spec = (PackageSpec)enum.nextElement();
		String ver = req.getParameter(type + spec.getName());
		if (ver == null) continue;
		Package prod = spec.getPackage(ver);
		if (prod != null){
		    Link link = new Link(makeIndexURL(prod.getDoc(type)),
					 spec.getDescription() +
					 " (" + ver + ")");
		    table.addCell(link).cell().center();
		    table.newRow();
		}
	    }
	}

	page.add(Break.para);
        if (!smallIndex){
            Link link = new Link(req.getServletPath() + "cfgIndexToc2?" +
                                 req.getQueryString(),
                                 "Small Index");
            link.attribute("onClick=\"window.open('" +
                           req.getServletPath() + "cfgIndexToc2?" +
                           req.getQueryString() +
                           "','tocWindow','toolbar=no,scrollbars=no,menu=no,resizable=yes,height=300,width=200');\"");
            link.target("_parent");
            page.add(link);
            page.add(Break.line);
            link = new Link(req.getServletPath() + "makeIndex?" +
                            req.getQueryString(),
                            "Edit Index").target("_parent");
            page.add(link);
        }

	res.setContentType("text/html");
	OutputStream out = res.getOutputStream();
	PrintWriter pout = new PrintWriter(out);
	page.write(pout, Page.Content, true);
	pout.flush();
    }
    /* ------------------------------------------------------------ */
    private String makeIndexURL(String index){
        Code.debug("1:",index);
        if (mapToFile)
            return "file:" + index;
        else {
            if (httpIndexTruncate != null &&
                index.startsWith(httpIndexTruncate))
                index = index.substring(httpIndexTruncate.length());
            Code.debug("2:", httpIndexPrefix, ":", index);
            if (httpIndexPrefix != null)
                return httpIndexPrefix + index;
            else
                return index;
        }
    }
    /* ------------------------------------------------------------ */
    private abstract class ProdPrinter {
	public abstract boolean print(Package p);
	public abstract String path(Package p);
	public String href(Package p){
	    String pt = path(p);
	    if (pt == null) return null;
	    return makeIndexURL(pt);
	}
    }
    /* ------------------------------------------------------------ */
    public class ProdPrint extends ProdPrinter{
	boolean all;
	String type;
	public ProdPrint(boolean all, String type){
	    this.all = all;
	    this.type = type;
	}
	public boolean print(Package p){
	    return all || path(p) != null;
	}
	public String path(Package p){
	    return p.getDoc(type);
	}
    }
    /* ------------------------------------------------------------ */
    private Element packageList(ProdPrinter pp){
	Table table = new Table(tocTableBorderWidth);
	table.center();
	for (Enumeration specs = packages.elements();
	     specs.hasMoreElements();)
	{
	    PackageSpec spec = (PackageSpec)specs.nextElement();
	    List list = null;
	    for (Iterator enum = spec.getPackages(); enum.hasNext();)
	    {
		Package p = (Package)enum.next();
		if (pp.print(p)){
		    if (list == null){
			// There is something to show
			table.addHeading(spec.getDescription());
			table.addCell(list = new List(List.Unordered));
		    }
		    String version = p.getVersion();
		    String minorVersion = p.getMinorVersion();
		    if (minorVersion != null)
			version = version + "/" + minorVersion;
		    String href = pp.href(p);
		    if (version == null)
		    {
			// There is no version - just put in the link with no
			// list, if it exists.
			if (href != null)
			    list.add(new Link(href, "unversioned"));
			else
			    list.add("unversioned");
		    } else {
			if (href != null)
			    list.add(new Link(href, version));
			else
			    list.add(version);
		    }
		}
	    }
	    table.newRow();
	}
	return table;
    }
    /* ------------------------------------------------------------ */
    private void addPackageSelect(HttpServletRequest req,
				  Table table, ProdPrinter pp,
				  String type) {
	for (Enumeration specs = packages.elements();
	     specs.hasMoreElements();)
	{
	    PackageSpec spec = (PackageSpec)specs.nextElement();
	    Select sel = null;
	    boolean selected = false;
	    Package latest = null;
	    String oldVal =
		req.getParameter(type + spec.getName());
	    for (Iterator enum = spec.getPackages(); enum.hasNext();)
	    {
		Package p = (Package)enum.next();
		if (pp.print(p)){
		    if (sel == null) {
			sel = new Select(type + spec.getName(), false);
			table.newRow();
			table.addCell(spec.getDescription());
			table.addCell(sel);
		    }
		    String version = p.getVersion();
		    String minorVersion = p.getMinorVersion();
		    String key = (version == null ? "-" : version);
		    if (minorVersion != null)
			key = key + "/" + minorVersion;
		    String name = key;
		    if (key.equals("-"))
			name = "Show";
		    boolean select = key.equals(oldVal);
		    selected = selected || select;
		    sel.add(name, select, key);
		    if (!select && p.isNewest())
			latest = p;
		}
	    }
	    if (sel != null){
		sel.add("None", !selected, "");
	    }
	    if (selected){
		if (latest != null)
		    table.addCell("Latest: "+ latest.getVersion());
		else
		    table.newCell();
	    }
	}	
    }
    /* ------------------------------------------------------------ */
}
