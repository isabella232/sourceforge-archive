// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
//import com.sun.java.util.collections.*; XXX-JDK1.1
import com.mortbay.Util.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.util.*;

/* ================================================================ */
public class JettyPage extends Page
{
    private static  Section[][] __section;
    private static final PathMap __pathMap = new PathMap();
    static   
    {
	System.err.println("Loading Index");
	int i=0;
	int j=0;
	ArrayList major=new ArrayList(10);
	try
	{
	    ResourceBundle index =
		ResourceBundle.getBundle("com.mortbay.Jetty.JettyIndex");
	    
	    String key=i+"."+j;
	    String value=index.getString(key);    
	    while (value!=null)
	    {
		try
		{
		    ArrayList minor=new ArrayList(5);
		    major.add(minor);
		    do
		    {
			Section section=new Section(value);
			System.err.println(key+" = "+section);
			minor.add(section);
			if (section._pathSpec!=null)
			    __pathMap.put(section._pathSpec,section);
			
			j++;
			key=i+"."+j;
			value=index.getString(key);
		    }
		    while (value!=null);
		}
		catch(MissingResourceException e)
		{
		    Code.ignore(e);
		}
		finally
		{
		    i++;
		    j=0;
		    key=i+"."+j;
		    value=index.getString(key);  
		}
	    }
	}
	catch(MissingResourceException e)
	{
	    Code.ignore(e);
	}
	catch(Throwable th)
	{
	    Code.warning(th);
	}

	Code.assert(major.size()>0 && ((ArrayList)(major.get(0))).size()>0,
		    "No index");
	
	__section=new Section[major.size()][];
	for (i=0;i<major.size();i++)
	{
	    ArrayList minor = (ArrayList)major.get(i);
	    __section[i]=new Section[minor.size()];
	    __section[i]=(Section[])minor.toArray(__section[i]);
	}
    };
    
    
    /* ------------------------------------------------------------ */
    private String _path;
    private Table _table;
    private boolean _home;
    
    /* ------------------------------------------------------------ */
    private Section _section ;
    public Section getSection() {return _section;}
    
    /* ------------------------------------------------------------ */
    public JettyPage(String path)
    {
	_path=path;
	
	String root = ""; // XXX may be ../ or ../../ etc.
	
	addHeader
	    ("<link REL=\"STYLESHEET\" TYPE=\"text/css\" HREF=\""+
	     root+"jetty.css\">");
	attribute("text","#000000");
	attribute(BGCOLOR,"#FFFFFF");
	attribute("link","#606CC0");
	attribute("vlink","#606CC0");
	attribute("alink","#606CC0");
	attribute("background",root+"images/jettybg.png");
	attribute("MARGINWIDTH","0");
	attribute("MARGINHEIGHT","0");
	attribute("LEFTMARGIN","0");
	attribute("RIGHTMARGIN","0");
	attribute("TOPMARGIN","0");
	
	_section = (Section)__pathMap.match(_path);
	if (_section==null)
	{
	    if("/".equals(_path))
		_section=__section[0][0];
	    else
		return;
	}
	
	title("Mort Bay: "+_section._key);
	_home=false;
	if (__section[0][0].equals(_section))
	{
	    _home=true;
	    title("Mort Bay Consulting");
	    addHeader("<META NAME=\"description\" CONTENT=\"Jetty Java HTTP Servlet Server\"><META NAME=\"keywords\" CONTENT=\"Jetty Java HTTP Servlet Server\">");
	}
	
	_table = new Table();
	nest(_table);
	_table.cellPadding(7);
	_table.cellSpacing(0);
	_table.newRow();
	_table.newCell();
	_table.cell().bgColor("#E8ECF8");
	_table.cell().top();
	_table.cell().center();

	_table.add("<A HREF=http://jetty.mortbay.com><IMG SRC=\""+root+"images/powered.png\" BORDER=0></A>\n");

	boolean para=true;
	// navigation
	for (int section=0;section<__section.length;section++)
	{
	    if (_section._section.equals(__section[section][0]._section)&&
		__section[section].length>1 )
		para=true;
	    _table.add(para?"<P>":"<BR>");
	    para=false;
	    if(_section.equals(__section[section][0]))
		_table.add("<FONT SIZE=+1><B>"+
			   __section[section][0]._section+
			   "</B></FONT>");
	    else
		_table.add(__section[section][0]._link);
	    
	    
	    if (__section[section].length>1 &&
		_section._section.equals(__section[section][0]._section))
	    {
		for (int sub=1;sub<__section[section].length;sub++)
		{
		    _table.add("<BR>");
		    if(_section.equals(__section[section][sub]))
			_table.add("<FONT SIZE=-1><B>"+
				   __section[section][sub]._subSection+
				   "</B></FONT>");
		    else
			_table.add(__section[section][sub]._link);
		}
		para=true;
	    }
	}
	
	_table.add("&nbsp;<P>");
	_table.add("&nbsp;<P>");

	// home logos
	_table.add("<A HREF=\"http://www.mortbay.com\"><IMG SRC=\""+root+"images/mbLogoBar.png\" BORDER=0></A><P>\n");
	_table.add("<A HREF=\"http://www.inetu.com\"><IMG SRC=\""+root+"images/inetu.png\" BORDER=0></A><P>\n");
	_table.add("<A HREF=\"http://sourceforge.net/projects/jetty\"><IMG SRC=\""+root+"images/sourceforge.png\" BORDER=0></A><P>\n");
	
	
	_table.newCell();
	_table.add("<IMG SRC=\""+root+"images/blank.png\">");
	_table.newCell();
	_table.cell().top();
	_table.cell().left();
	if (path.endsWith(".txt"))
	    _table.nest(new Block(Block.Pre));
	
    }
    
    /* ------------------------------------------------------------ */
    public void completeSections()
    {
	
    }


    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	try{
	    Page page = args.length>0
		? new JettyPage(args[0])
		    : new JettyPage("/index.html");

	    page.add("Stuff");
	    
	    page.write(System.out);
	    
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }

    /* ------------------------------------------------------------ */
    public static class Section
    {
	String _uri;
	String _pathSpec;
	String _key;
	String _section;
	String _subSection;
	String _link;

	Section(String value)
	{
	    StringTokenizer tok = new StringTokenizer(value,"\t ");
	    Code.assert(tok.hasMoreTokens(),"No name");
	    _key=tok.nextToken();
	    Code.assert(tok.hasMoreTokens(),"No URI");
	    _uri=tok.nextToken();
	    if (tok.hasMoreTokens())
		_pathSpec=tok.nextToken();
	    _key=_key.replace('+',' ');
	    int c=_key.indexOf(':');
	    if (c>0)
	    {
		_section=_key.substring(0,c);
		_subSection=_key.substring(c+1);
		Font font=new Font(-1,true);
		font.add(new Link(_uri,_subSection));
		_link=font.toString();
	    }
	    else
	    {
		_section=_key;
		_subSection=null;
		Font font=new Font(1,true);
		font.add(new Link(_uri,_section));
		_link=font.toString();
	    }
	}
	
	public String toString()
	{
	    return _key+", "+_uri+", "+(_pathSpec==null?"":_pathSpec);
	}
	
    }

    
}





