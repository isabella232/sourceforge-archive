// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.Code;
import com.mortbay.Util.PropertyTree;
import com.mortbay.HTTP.MultiPartRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import com.mortbay.HTML.Page;
import com.mortbay.HTML.Heading;
import com.mortbay.HTML.Composite;
import com.mortbay.HTML.Table;
import com.mortbay.HTML.Block;
import com.mortbay.HTML.Form;
import com.mortbay.HTML.List;
import com.mortbay.HTML.Link;
import com.mortbay.HTML.Input;
import com.mortbay.HTML.Select;

/** Servlet to handle navigation and editing of a PropertyTree
 * <p> This servlet uses the ServletDispatch class to carry out navigation
 * over a tree of HTTP handlers that mirror the structure of my model, in this
 * case being a PropertyTree. Each node in the HTTP handling tree is a
 * ServletNode (to aid in generation of URL's) and the standard operations on
 * tree nodes (Add, Remove, Save, Load) are defined on this object as methods
 * for the Dispatcher to call. The defaultDispatch method handles navigation
 * down to sub-nodes.
 *
 * @see com.mortbay.Servlets.ServletDispatch
 * @see com.mortbay.Servlets.ServletDispatchHandler
 * @see com.mortbay.Servlets.ServletNode
 * @see com.mortbay.Util.PropertyTree
 * @version 1.0 Sun Feb 21 1999
 * @author Matthew Watson (mattw)
 */
public class PropertyTreeEditor
    extends HttpServlet
{
    /* ------------------------------------------------------------ */
    protected String lookAndFeelName;
    protected TreeNode node = new TreeNode(new PropertyTree(), null, null);
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
	 throws ServletException
    {
	super.init(config);

	lookAndFeelName = getInitParameter(Page.PageType);
	if (lookAndFeelName == null)
	    lookAndFeelName = Page.getDefaultPageType();
    }
    /* ------------------------------------------------------------ */
    public static class TreeNode
	extends ServletNode
	implements ServletDispatchHandler
    {
	/* ------------------------------------------------------------ */
	public TreeNode(PropertyTree pt, String name, Vector parentAddress){
	    if (parentAddress != null) setAddress(parentAddress);
	    if (name != null) addAddressElement(name); // in ServletNode
	    this.name = name;
	    tree = pt;
	}
	/* -------------------------------------------------------- */
	private String name;
	// This is our cache of TreeNodes that handle servlet requests on the
	// corresponding PropertyTree. Their PropertyTree is updated by their
	// parent on every request for consistency.
	private Hashtable nodes = new Hashtable();
	private PropertyTree tree;
	/* ------------------------------------------------------------ */
	public static class AddArgs {
	    public String key = null;
	    public String value = null;
	    public String add = null;
	};
	public static class RemoveArgs {
	    public String keys[] = null;
	};
	/* -------------------------------------------------------- */
	public Page Add(Page page, AddArgs args, HttpServletRequest req){
	    addHeader(page, req);
	    page.unnest();
	    if (args.key != null){
		boolean value = args.add.equals("New Value");
		if (!value){
		    PropertyTree pt = new PropertyTree();
		    tree.put(args.key, pt);
		} else {
		    tree.put(args.key, args.value);
		}
		page.add(new Heading(3, (value ? "Value " : "Node ")
				     + args.key + " added..."));
		if ("*".equals(name) && args.key.equals("*")){
		    Heading head = new Heading(3, "default value set on ");
		    head.add(new Link(getParentUrlPath(req, 1), "parent"));
		    page.add(head);
		}
	    }
	    return fillPage(page, req);
	}
	/* -------------------------------------------------------- */
	public Page Remove(Page page, RemoveArgs args, HttpServletRequest req){
	    addHeader(page, req);
	    page.unnest();
	    if (args.keys != null){
		page.add(new Heading(3, "Removing Keys"));
		List list = new List(List.Unordered);
		for (int i = 0; i < args.keys.length; i++){
		    Object val = tree.remove(args.keys[i]);
		    Composite comp = new Composite();
		    comp.add(args.keys[i]);
		    comp.add(": ");
		    Block bl = new Block(Block.Pre);
		    comp.add(bl);
		    bl.add(val);
		    list.add(comp);
		    // clean up the TreeNodes as well
		    this.nodes.remove(args.keys[i]);
		}
		page.add(list);
	    }
	    return fillPage(page, req);
	}
	/* ------------------------------------------------------------ */
	public void Save(HttpServletResponse res)
	    throws Exception
	{
	    res.setContentType("application/x-java-properties; filename=save.prp");
	    OutputStream out = res.getOutputStream();
	    PrintWriter pout = new PrintWriter(out);
	    pout.print(tree);
	    pout.flush();
	}
	/* ------------------------------------------------------------ */
	public Page Load(Page page, HttpServletRequest req)
	    throws Exception
	{
	    addHeader(page, req);
	    page.unnest();
	    MultiPartRequest mpr = new MultiPartRequest(req);
	    if (mpr.contains("file"))
	    {
		String filename = mpr.getFilename("file");
		tree.load(mpr.getInputStream("file"));
		page.add(new Heading(3, "Loaded file: " + filename));
	    }
	    return fillPage(page, req);
	}
	/* -------------------------------------------------------- */
	public Page Get(Page page, HttpServletRequest req){
	    addHeader(page, req);
	    page.unnest();
	    page.add(new Heading(3, "Get Value"));
	    Table table = new Table(1);
	    table.addHeading("Key");
	    table.addHeading("Value");
	    table.newRow();
	    String key = req.getParameter("key");
	    table.addCell(key);
	    table.addCell(tree.get(key));
	    page.add(table);
	    return fillPage(page, req);
	}
	/* -------------------------------------------------------- */
	private void setPropertyTree(PropertyTree pt){
	    tree = pt;
	}
	/* ------------------------------------------------------------ */
	private void addHeader(Page page, HttpServletRequest req){
	    page.add(new Link(getUrlPath(req), name == null ? "root" : name));
	    page.add("&nbsp;");
	}
	/* ------------------------------------------------------------ */
	private Page fillPage(Page page, HttpServletRequest req){
	    // Get out of the Location header
	    page.unnest();
	    // The Tree:
	    page.add(new Heading(2, "Tree Values"));
	    Block bl = new Block(Block.Pre);
	    bl.add(tree);
	    page.add(bl);
	    // Sub-nodes
	    page.add(new Heading(3, "Sub-Nodes of this PropertyTree:"));
	    String myUrl = getUrlPath(req);
	    List list = new List(List.Unordered);
	    for (Enumeration enum = tree.nodeNames(); enum.hasMoreElements();){
		String node = enum.nextElement().toString();
		list.add(new Link(myUrl+"/"+node, node));
	    }
	    if (!"*".equals(name))
		list.add(new Link(myUrl+"/*", "default values (*)"));
	    page.add(list);
	    // Add
	    page.add(new Heading(3, "Add Values"));
	    Form form = new Form(myUrl+"/Add");
	    Table table = new Table(0);
	    form.add(table);
	    table.newRow();
	    table.addHeading("Name");
	    table.addCell(new Input(Input.Text, "key"));
	    table.addCell(new Input(Input.Submit, "add", "New Sub-Node"));
	    table.addHeading("Value");
	    table.addCell(new Input(Input.Text, "value"));
	    table.add(new Input(Input.Submit, "add", "New Value"));
	    page.add(form);
	    // Remove
	    page.add(new Heading(3, "Remove Values"));
	    page.add("Note: Removing keys that are both node names and value s will remove both...");
	    form = new Form(myUrl+"/Remove");
	    table = new Table(0);
	    form.add(table);
	    table.addHeading("Nodes");
	    Select sel = new Select("keys", true);
	    sel.size(4);
	    sel.add(tree.nodeNames());
	    sel.add("*");
	    table.addCell(sel);
	    table.addHeading("Values");
	    sel = new Select("keys", true);
	    sel.size(4);
	    sel.add(tree.valueNames());
	    sel.add("*");
	    table.addCell(sel);
	    table.newRow();
	    table.addCell(new Input(Input.Submit, "go", "Remove"))
		.cell().attributes("COLSPAN=4").center();
	    page.add(form);
	    // Get...
	    page.add(new Heading(3, "Get Value"));
	    form = new Form(myUrl+"/Get");
	    table = new Table(0);
	    form.add(table);
	    table.addHeading("Key");
	    table.addCell(new Input(Input.Text, "key"));
	    table.addCell(new Input(Input.Submit, "go", "Get"));
	    page.add(form);
	    // Save ...
	    page.add(new Link(myUrl+"/Save",
			      new Heading(3, "Save Tree to Disk")));
	    // Load ...
	    page.add(new Heading(3, "Load Properties File"));
	    form = new Form(myUrl+"/Load");
	    form.encoding(Form.encodingMultipartForm);
	    table = new Table(0);
	    form.add(table);
	    table.newRow();
	    table.addHeading("Select File");
	    table.addCell(new Input(Input.File, "file"));
	    table.addCell(new Input(Input.Submit, "load", "Load"));
	    page.add(form);
	    return page;
	}
	/* -------------------------------------------------------- */
	public Object defaultDispatch(String method,
				      ServletDispatch dispatch,
				      Object context,
				      HttpServletRequest req,
				      HttpServletResponse res)
	    throws Exception
	{
	    if (method != null){
		PropertyTree pt = tree.getNode(method);
		if (pt != null){
		    addHeader((Page)context, req);
		    // get the tree node...
		    TreeNode tn = (TreeNode)nodes.get(method);
		    if (tn == null){
			nodes.put(method,
				  new TreeNode(pt, method, getAddress()));
			tn = (TreeNode)nodes.get(method);
		    }
		    tn.setPropertyTree(pt);
		    return dispatch.dispatch(tn, context);
		}
		addHeader((Page)context, req);
		// Unknown node...
		((Page)context).add(new Heading(3, "Node "+method+" not found"));
	    } else
		addHeader((Page)context, req);
	    return fillPage((Page)context, req);
	}
	/* -------------------------------------------------------- */
    }
    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException
    {
	Page page = Page.getPage(lookAndFeelName, req);
	page.title("Property Tree Editor");
	page.add(new Heading(1, "Property Tree Editor").center());
	Block bl = new Block(Block.Center);
	bl.nest(new Heading(3, ""));
	page.nest(bl);
	page.add("Location:&nbsp;");
	try {
	    try {
		ServletDispatch disp = new ServletDispatch(req, res);
		page = (Page)disp.dispatch(node, page);
	    } catch (java.lang.reflect.InvocationTargetException ex){
		Throwable t = ex;
		while (t instanceof
		       java.lang.reflect.InvocationTargetException){
		    t = ((java.lang.reflect.InvocationTargetException)t)
			.getTargetException();
		}
		throw t;
	    }
	} catch (Throwable e) {
	    Code.debug(e);
	    page = Page.getPage(lookAndFeelName, req);
	    page.title("Exception Occurred...");
	    page.nest(new Block(Block.Pre));
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    page.add(sw.toString());
	}
	if (page != null){
	    res.setContentType("text/html");
	    OutputStream out = res.getOutputStream();
	    PrintWriter pout = new PrintWriter(out);
	    page.write(pout);
	    pout.flush();
	}
    }
    /* ------------------------------------------------------------ */
};
