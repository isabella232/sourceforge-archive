// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.Base.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/* ================================================================ */
public class JettyLaF extends Page
{
    /* ------------------------------------------------------------ */
    public static boolean alternate;

    /* ------------------------------------------------------------ */
    public static Composite contact = new Composite();
    public static Composite alternateContact = new Composite();
    public static PathMap sectionMap = new PathMap();
    public static Hashtable headingMap = new Hashtable();
    static
    {
	alternate=false;
	contact.add(new Link("http://www.mortbay.com",
	     "<B>Jetty by Mort Bay Consulting</B>"));
	contact.add("<BR>AU: Pty. Ltd. ACN 069204815");
	contact.add("<BR>GB: Ltd. No 3396994");
	contact.add("<BR>");
	contact.add(new Link("http://www.mortbay.com",
		          "http://www.mortbay.com"));
	contact.add("<BR>");
	contact.add(new Link("mailto:mortbay@mortbay.com",
			     "mortbay@mortbay.com"));

	alternateContact.add(new Link("mailto:mortbay@mortbay.com",
	     "<B>Mort Bay Consulting</B>"));
	alternateContact.add( " AU: Pty. Ltd. ACN 069204815");
	alternateContact.add( " GB: Ltd. No 3396994 ");
	alternateContact.add(new Link("http://www.mortbay.com",
				      "http://www.mortbay.com"));

	sectionMap.put("/Jetty/Info","Info");
	sectionMap.put("/Jetty/Demo","Demo");
	sectionMap.put("/Jetty/Program","Program");
	sectionMap.put("/Jetty/Config","Config");
	sectionMap.put("/Jetty/Home","Home");

	headingMap.put("Info","Jetty Information");
	headingMap.put("Demo","Jetty Demonstrations");
	headingMap.put("Program","Jetty Development");
	headingMap.put("Config","Jetty Configuration");
    }
    
    /* ------------------------------------------------------------ */
    static Element mbImage=null;
    static Element logo=null;

    /* ------------------------------------------------------------ */
    private static final String[][] navigation =
    {
	{"Up",null},
	{"Home","/"},
	{"Info","/Jetty/Info"},
	{"Demo","/Jetty/Demo"},
	{"Program","/Jetty/Program"},
	{"Config","/Jetty/Config"},
	{"Help",null},
	{"Back",null}
    };    
    

    /* ------------------------------------------------------------ */
    Composite header=new Composite();
    Composite content=new Composite();
    Composite footer=new Composite();
    
    /* ------------------------------------------------------------ */
    public JettyLaF()
    {
	Code.debug("Construct JettyLaF");
	properties.put(Home,"/");
	setBackGroundColor("white");
	setBase("_top",null);

	setSection(Header,header);
	setSection(Content,content);
	setSection(Footer,footer);
	
	add(header);
	nest(content);
	if (!alternate)
	    nest(new Block(Block.Center));
    }
    
    /* ------------------------------------------------------------ */
    /** Complete Header and footer setion
     * Called just before the page is output (after all content added).
     */
    public void completeSections()
    {
	// Finish adding to content
	unnest();
	add(footer);
	
	// Determine Section
	String section = (String)properties().get(Section);
	String sectionPath = null;
	
	HttpServletRequest request =
	    (HttpServletRequest)properties().get(Request);

	if (request!=null)
	{
	    sectionPath = sectionMap.longestMatch(request.getServletPath()+
						  request.getPathInfo());
	    
	    if (section==null && sectionPath!=null)
		section = (String)sectionMap.get(sectionPath);

	    if ("Yes".equals(request.getParameter("ToggleLaF")))
	    {
		Code.warning("Toggle LAF");
		alternate=!alternate;
	    }
	}
			
	// check heading
	String heading = (String)properties().get(Heading);
	if (heading==null ||
	    heading.length()==0 ||
	    heading.equals(Page.NoTitle))
	{
	    if (section!=null)
		heading = (String)headingMap.get(section);
	    if (heading==null)
		heading = "Jetty Demo";
	}

	if (alternate)
	    alternateLaF(section,heading);
	else
	{
	    // check up
	    String up = (String)properties().get(Up);
	    if (up==null && sectionPath!=null)
		properties().put(Up,sectionPath);
	
	    // check images and applets 
	    if (mbImage==null)
	    {
		mbImage = new Image("FileBase","/Images/mbLogoSmall.gif")
		    .border(0);
		mbImage = new Link("/",mbImage);
	    }
	    if (logo==null)
	    {
		//logo = new Applet("MortBayLogo.class")
		//	.codeBase("/")
		//	.setDimensions(150,150);
		//logo.add(mbImage);
		logo=mbImage;
	    }

	    // Build header
	    Table grid = new Table(0);
	    grid.width("100%");
	    grid.newRow();
	    grid.newCell().cell().add("Home".equals(section)?
				      (Element)logo:
				      (Element)mbImage)
		.left().width("30%").bottom();
	    grid.newCell().cell()
		.nest(new Block(Block.Bold))
		.center().width("40%").bottom();
	
	    if ("Home".equals(section))
	    {
		grid.cell()
		    .add(new Font(3,true).face("Helvetica")
			 .add("Mort Bay Consulting's<P>"))
		    .add(new Font(4,true).face("Helvetica")
			 .add("JETTY<BR>Java HTTP Server"));
	    }
	    else
		grid.cell()
		    .add(new Font(4,true).face("Helvetica")
			 .add(heading));

	    grid.newCell().cell()
		.nest(new Font(2).face("Helvetica"))
		.right().width("30%").bottom();

	    Composite topRight = new Composite();
	    if ("Home".equals(section))
		grid.cell().add(contact);
	    else
		grid.cell().add(topRight);
	    
	    header.add(grid);
	    header.add(Break.rule);

	    // Get referer
	    String referer = request.getHeader(HttpHeader.Referer);
	    if (referer!=null && referer.length()>0)
		properties.put("Back",referer);
	    
	    // build footer & finish top right header
	    footer.add(Break.rule);
	    footer.nest(new Block(Block.Center));

	    for (int i =0;i<navigation.length;i++)
	    {
		String label = navigation[i][0];
		String url = navigation[i][1];
		if (url==null)
		    url=(String)properties().get(label);
		if (url!=null && url.length()>0)
		{
		    if (label.equals(section))
		    {
			footer.add("<B>");
			topRight.add("<B>");
		    }
		
		    footer.add(new Link(url,label));
		    topRight.add(new Link(url,label));

		    if (label.equals(section))
		    {
			footer.add("</B>");
			topRight.add("</B>");
		    }
		
		    footer.add("&nbsp;");
		    topRight.add("<BR>");
		}
	    }
	}	
    }

    private void alternateLaF(String section, String heading)
    {
	// Build header
	Table grid = new Table(0);
	header.add(grid);
	grid.cellPadding(0);
	grid.cellSpacing(0);
	grid.width("100%");
	
	grid.newRow();

	Table tabs = new Table(0);
	grid.addCell(tabs);
	tabs.width("100%");
	tabs.cellPadding(2);
	tabs.cellSpacing(0);
	tabs.newRow();

	int sn = -1;
	
	for (int s=1; (s+1)<navigation.length ; s++)
	{
	    tabs.newCell().cell().center();
	    tabs.nest(new Font(4,true).face("Helvetica"));
	    if (navigation[s][0].equals(section))
	    {
		sn = s;
		tabs.cell().bgColor("#c0c0c0");
		tabs.add(new Link(navigation[s][1],
				  "<B>"+navigation[s][0]+"</B>"));
	    }
	    else
	    {
		tabs.add(new Link(navigation[s][1],
				  navigation[s][0]));
	    }
	    tabs.cell().unnest();
	}
	
	grid.newRow();
	grid.newCell().cell().center().bgColor("#c0c0c0");
	Table t = new Table(0);
	t.cellPadding(6);
	t.cellSpacing(0);
	t.newRow();
	t.newCell();
	t.nest(new Block(Block.Center));
	t.add(new Font(4,true).face("Helvetica").add(heading));
	t.center();
	grid.add(t);
	

	// build footer & finish top right header
	footer.add(Break.line);
	Table foot = new Table(0);
	footer.add(foot);
	foot.cellPadding(5);
	foot.cellSpacing(0);
	foot.width("100%");
	
	foot.newRow();
	foot.newCell().cell().center().bgColor("#c0c0c0");
	foot.add(alternateContact);
    }
    
    
    /* ------------------------------------------------------------ */
    public FrameSet frameSet()
    {
	FrameSet frameSet=new FrameSet("JettyLaF.Frames",
				       null,"150,*,70");
	frameSet.nameFrame(Page.Header,0,0)
	    .scrolling(false);
	frameSet.nameFrame(Page.Content,0,1);
	frameSet.nameFrame(Page.Footer,0,2)
	    .scrolling(false);
	frameSet.border(false,0,null);

	return frameSet;
    }
}

