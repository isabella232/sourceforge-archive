// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.Code;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import com.mortbay.HTML.*;
import com.mortbay.Util.PropertyTree;
import com.mortbay.HTTP.MultiPartRequest;

/** Servlet to handle navigation and editing of a PropertyTree
 * <p> This servlet uses the ServletDispatch class to carry out navigation
 * over a tree of HTTP handlers that mirror the structure of my model, in this
 * case being a PropertyTree. Each node in the HTTP handling tree is a
 * ServletNode (to aid in generation of URL's) and the standard operations on
 * tree nodes (Set, Remove, Save, Load) are defined on this object as methods
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
    private String lookAndFeelName;
    private final PropertyTree tree = new PropertyTree();
    private EditorDispatcher editorDispatcher = new EditorDispatcher();


    /* ------------------------------------------------------------ */
    public static class Context
    {
	public ServletNode path=new ServletNode();
	public Page page;
	public PropertyTree subtree;
	public String key=null;
	public String value=null;
	
	public Context(Page page, PropertyTree subtree)
	{
	    this.page=page;
	    this.subtree=subtree;
	}
    }
	
    /* ------------------------------------------------------------ */
    public static class EditArgs
    {
	public String key = null;
	public String value = null;
	public String action = null;
    };
    
    /* -------------------------------------------------------- */
    public static class PruneArgs
    {
	public String keys[] = null;
    };
    
    /* ------------------------------------------------------------ */
    public class EditorDispatcher
	implements ServletDispatchHandler
    {
	String dftKey=null;
	String dftValue=null;
	
	/* -------------------------------------------------------- */
	public void edit(Context context,
			 EditArgs args,
			 HttpServletRequest req)
	{
	    addHeader(context, req, true);
	    
	    if (args.key != null)
	    {
		boolean set = "Set".equals(args.action);
		boolean get = "Get".equals(args.action);
		boolean remove = "Remove".equals(args.action);

		if (set)
		{
		    context.key=args.key;
		    context.value=args.value;
		    context.subtree.put(args.key,
					set?(Object)args.value:
					(Object)new PropertyTree());
		}
		else if (get)
		{
		    context.key = req.getParameter("key");
		    context.value = (String)context.subtree.get(context.key);
		}
		else if (remove)
		{
		    context.key=args.key;
		    context.value=null;
		    context.subtree.remove(args.key);
		}
	    }
	    
	    fillPage(context, req);
	}
	
	
	/* ------------------------------------------------------------ */
	public void save(Context context,
			 HttpServletRequest req,
			 HttpServletResponse res)
	    throws Exception
	{
	    String filename = req.getPathInfo();
	    filename=filename.substring(context.path.getPath().length());
	    if (filename.startsWith("/Save/"))
		filename=filename.substring(6);
	    else if (filename.startsWith("/"))
		filename=filename.substring(1);

	    Code.warning("Filename="+filename);
	    
	    res.setContentType("application/x-java-properties; filename="+
			       filename);
	    
	    OutputStream out = res.getOutputStream();
	    PrintWriter pout = new PrintWriter(out);
	    context.subtree.list(pout);
	    pout.flush();
	    context.page=null;
	}
	
	/* ------------------------------------------------------------ */
	public void load(Context context, HttpServletRequest req)
	    throws Exception
	{
	    addHeader(context, req, true);
	    
	    MultiPartRequest mpr = new MultiPartRequest(req);
	    if (mpr.contains("file"))
	    {
		String filename = mpr.getFilename("file");
		context.subtree.load(mpr.getInputStream("file"));
	    }
	    
	    fillPage(context, req);
	}
	
	
	/* ------------------------------------------------------------ */
	private void addHeader(Context context,
			       HttpServletRequest req,
			       boolean last)
	{
	    String name = context.path.getBaseName();
	    if (name!=null)
		context.page.add("&nbsp;.&nbsp;");
	    context.page.add(new Link(context.path.getUrlPath(req),
			      name == null ? "ROOT" : name));
	    if (last)
		context.page
		    .add("&nbsp;:</H2></TD><TD></TD></TR><TR VALIGN=TOP><TD>");
	}
	
	/* ------------------------------------------------------------ */
	private void fillPage(Context context, HttpServletRequest req)
	{
	    Page page = context.page;
	    String myUrl = context.path.getUrlPath(req);
	    String name = context.path.getBaseName();
	    
	    Table table = new Table(1);
	    page.add(table);
	    table.cellPadding(2);
	    table.newRow();
	    table.addHeading("KEY");
	    table.addHeading("VALUE");

	    // crude sort of the keys
	    Vector keys=new Vector(context.subtree.size());
	    Enumeration e = context.subtree.keys();
	    while (e.hasMoreElements())
	    {
		String key=(String)e.nextElement();
		for (int i=0;i<keys.size();i++)
		{
		    if (key.compareTo((String)keys.elementAt(i))<0)
		    {
			keys.insertElementAt(key,i);
			key=null;
			break;
		    }
		}
		if (key!=null)
		    keys.addElement(key);
	    }
	    
	    e = keys.elements();
	    while (e.hasMoreElements())
	    {
		table.newRow();
		table.newCell();

		String key=(String)e.nextElement();
		String value=context.subtree.get(key).toString();
		    
		StringTokenizer tok = new StringTokenizer(key,".");
		String path="";
		while (tok.hasMoreTokens())
		{
		    String t = tok.nextToken();
		    path += "/"+t;
		    if (tok.hasMoreTokens())
		    {
			table.add(new Link(myUrl+path,t));
			table.add(" . ");
		    }
		    else
			table.add(new Link(myUrl+"/edit?action=Get&key="+
					   path.replace('/','.')
					   .substring(1),t));
		}
		table.addCell(value);
	    }
		
	    page.add("</TD><TD WIDTH=25>&nbsp;</TD><TD>");
	    
	    TableForm form;
	    
	    // Edit
	    page.add("<HR>");
	    form = new TableForm(myUrl+"/edit");
	    form.addTextField("key","Key",30,context.key);
	    form.addTextField("value","Value",30,context.value);
	    form.addButtonArea("Action");
	    form.addButton("action", "Set");
	    form.addButton("action", "Get");
	    form.addButton("action", "Remove");
	    page.add(form);
	    
	    // Load & Save ...
	    page.add("<HR>");
	    page.add(new Heading(3, "Load Properties File"));
	    form = new TableForm(myUrl+"/load");
	    form.left();
	    form.encoding(Form.encodingMultipartForm);
	    form.addField("File",new Input(Input.File, "file"));
	    form.addButtonArea("Action");
	    form.addButton("load", "Load");
	    page.add(form);
	    page.add(new Link(myUrl+"/save/save.prp",
			      new Heading(3, "Save Tree to Disk")));
	    page.add("</TD></TR></TABLE>");
	}

	
	/* -------------------------------------------------------- */
	public Object defaultDispatch(String method,
				      ServletDispatch dispatch,
				      Object contextObj,
				      HttpServletRequest req,
				      HttpServletResponse res)
	    throws Exception
	{
	    Context context = (Context)contextObj;
	    
	    if (method != null)
	    {
		PropertyTree pt = context.subtree.getTree(method);
		if (pt != null)
		{
		    addHeader(context, req, false);
		    context.path.addAddressElement(method);
		    context.subtree=pt;
		    return dispatch.dispatch(this, context);
		}
		
		addHeader(context, req, false);
		
		// Unknown node...
		context.page = Page.getPage(lookAndFeelName, req,res);
		context.page.title("Property Tree Editor");
		context.page.add(new Heading(1, "Node "+
					     req.getPathInfo()+
					     " not found"));
		context.page.add(new Heading(3,new Link(req.getServletPath(),
							"Goto root")));
		return null;
	    }
	    else
		addHeader(context, req, true);
	    
	    fillPage(context, req);
	    
	    return null;
	}
    }
    
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
	 throws ServletException
    {
	super.init(config);

	lookAndFeelName = getInitParameter(Page.PageType);
	if (lookAndFeelName == null)
	    lookAndFeelName = Page.getDefaultPageType();

	tree.put("*","unknown");
	tree.put("animal.*.legs","4");
	tree.put("animal.insect.*.legs","6");
	tree.put("animal.spider.*.legs","8");
	tree.put("animal.bird.*.legs","2");
	tree.put("animal.fish.*.legs","0");
	tree.put("animal.*.roots","none");
	tree.put("*.spine","false");
	tree.put("animal.*.spine","true");
	tree.put("animal.insect.*.spine","false");
	tree.put("animal.mammal.mouse.furry","true");
	tree.put("animal.mammal.horse.furry","true");
	tree.put("animal.mammal.human.furry","some");
	tree.put("animal.mammal.human.legs","2");
	tree.put("animal.spider.*.furry","probably not");
	tree.put("animal.spider.redback.furry","false");
	tree.put("animal.spider.huntsman.furry","true");
	tree.put("animal.reptile.*.fur","false");
	tree.put("animal.reptile.snake.legs","0");
	tree.put("plant.*.legs","0");
	tree.put("plant.*.roots","true");
	tree.put("plant.*.furry","true");
	tree.put("plant.tree.oak.roots","deep");
	tree.put("plant.tree.willow.roots","shallow");
	
    }
    
    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException
    {
	Context context = new Context(Page.getPage(lookAndFeelName, req,res),
				      tree);
	context.page.unnest();
	
	context.page.title("Property Tree Editor");
	context.page.add("<TABLE WIDTH=95%><TR><TD COLSPAN=3><H1>Property Tree&nbsp;@&nbsp;");
	
	try
	{
	    ServletDispatch disp = new ServletDispatch(req, res);
	    disp.dispatch(editorDispatcher, context);
	}
	catch (Exception e)
	{
	    Code.warning(e);
	    throw new ServletException(e.toString());
	}
	if (context.page != null)
	{
	    res.setContentType("text/html");
	    OutputStream out = res.getOutputStream();
	    PrintWriter pout = new PrintWriter(out);
	    context.page.write(pout);
	    pout.flush();
	}
    }
    /* ------------------------------------------------------------ */
};
