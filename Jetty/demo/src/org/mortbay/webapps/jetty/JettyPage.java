// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;

import org.mortbay.html.Block;
import org.mortbay.html.Font;
import org.mortbay.html.Link;
import org.mortbay.html.Page;
import org.mortbay.html.Table;
import org.mortbay.http.PathMap;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.net.InetAddress;

/* ================================================================ */
public class JettyPage extends Page
{
    private static  Section[][] __section;
    private static final PathMap __pathMap = new PathMap();
    private static final PathMap __linkMap = new PathMap();

    private static boolean __realSite;
    static
    {
        try
        {
            if (InetAddress.getLocalHost().getHostName().indexOf("jetty")>=0)
            {
                Log.event("Real Jetty Site");
                __realSite=true;
            }
        }
        catch(Exception e) {Code.ignore(e);}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param context 
     */
    static synchronized void initialize(String context)  
    {
        if (__section!=null)
	    return;

        // This only works for 1 context.
        Code.debug("Loading JettyPage Index");
        int i=0;
        int j=0;
        ArrayList major=new ArrayList(10);
        try
        {
            ResourceBundle index =
                ResourceBundle.getBundle("org.mortbay.webapps.jetty.JettyIndex");
            
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
                        Section section=new Section(context,value);
                        Code.debug(key," = ",section);
                        minor.add(section);
                        if (section._pathSpec!=null)
                        {
                            __pathMap.put(section._pathSpec,section);

                            try{
                                String links=index.getString(section._pathSpec);
                                if (links!=null)
                                    __linkMap.put(section._pathSpec,new Links(links)); }
                            catch(MissingResourceException e)
                            {
                                Code.ignore(e);
                            }
                        }
                        
                        j++;
                        key=i+"."+j;
                        value=index.getString(key);
                    }
                    while (value!=null);
                }
                catch(MissingResourceException e)
                {
                    if (Code.verbose(99999))
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

        Code.assertTrue(major.size()>0 && ((ArrayList)(major.get(0))).size()>0,
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
    private String _context;
    
    /* ------------------------------------------------------------ */
    private Section _selectedSection ;
    private Links _links ;
    public Section getSection() {return _selectedSection;}
    
    /* ------------------------------------------------------------ */
    public JettyPage(String context,String path)
    {
        if (__section==null)
            initialize(context);
        
        _path=path;
        if (context==null)
            context="";        

        _context=context;
        
        addHeader
            ("<link REL=\"STYLESHEET\" TYPE=\"text/css\" HREF=\""+
             context+"/jetty.css\">");

        addHeader("<link REL=\"icon\" HREF=\""+context+"/images/jicon.gif\" TYPE=\"image/gif\">");
        
        addLinkHeader("Author",context,"http://www.mortbay.com");
        addLinkHeader("Copyright",context,"LICENSE.html");
        
        _links = (Links)__linkMap.match(_path);
        _selectedSection = (Section)__pathMap.match(_path);
        if (_selectedSection==null)
        {
            if("/".equals(_path))
                _selectedSection=__section[0][0];
            else
                return;
        }

        
        if (_links!=null)
        {
            if (path.equals(_links._up))
            {
                addLinkHeader("top",context,"/");
                addLinkHeader("up",context,_links._top);
                addLinkHeader("next",context,_links.get(0));
            }
            else
            {
                addLinkHeader("top",context,"/");
                addLinkHeader("up",context,_links._up);
            }

            if (_links.size()>0)
            {
                addLinkHeader("first",context,_links.get(0));
                addLinkHeader("last",context,_links.get(_links.size()-1));
                for (int i=0;i<_links.size();i++)
                {
                    if (path.equals(_links.get(i)))
                    {
                        if (i>0)
                            addLinkHeader("prev",context,_links.get(i-1));
                        if (i+1<_links.size())
                            addLinkHeader("next",context,_links.get(i+1));
                    }
                }
            }
        }
        else
        {
            addLinkHeader("top",context,"/");
        }
        

        attribute("text","#000000");
        attribute(BGCOLOR,"#FFFFFF");
        // attribute("link","#606CC0");
        // attribute("vlink","#606CC0");
        // attribute("alink","#606CC0");
        attribute("MARGINWIDTH","0");
        attribute("MARGINHEIGHT","0");
        attribute("LEFTMARGIN","0");
        attribute("RIGHTMARGIN","0");
        attribute("TOPMARGIN","0");
        
        
        title("Jetty: "+_selectedSection._key);
        _home=false;
        if (__section[0][0].equals(_selectedSection))
        {
            _home=true;
            title("Jetty Java HTTP Servlet Server");
            addHeader("<META NAME=\"description\" CONTENT=\"Jetty Java HTTP Servlet Server\"><META NAME=\"keywords\" CONTENT=\"Jetty Java HTTP Servlet Server\">");
        }
        
        _table = new Table(0);
        
        nest(_table);
        _table.cellPadding(5);
        _table.cellSpacing(5);
        _table.newRow();
        _table.newCell();
        _table.cell().top();
        _table.cell().center();
        _table.cell().width("150");
        
        _table.add("<A HREF=http://jetty.mortbay.org><IMG SRC=\""+context+"/images/powered.gif\" WIDTH=140 HEIGHT=58 BORDER=0 ALT=\"Powered by Jetty\"></A>\n");


     
        
        Table menu = new Table(0,"bgcolor=#FFFFFF");
        menu.width("90%");
        menu.cellPadding(2);
        menu.cellSpacing(5);
        
        _table.add(menu);
        

        
        boolean para=true;
        // navigation - iterate over all sections
        for (int i=0;i<__section.length;i++)
        {
             for (int j=0; j < __section[i].length; j++)
             {
                 // this is the section header,make a new row
                 if (j==0)
                 {
                     menu.newRow();
                     menu.newCell(" bgcolor=#3333ff");
                     menu.cell().middle();
                     menu.cell().right();

                     if (__section[i][0]._link != null)
                     {
                         Code.debug ("Section "+__section[i][0]._section+" has link "+__section[i][0]._link);
                         
                         if (_selectedSection._section.equals(__section[i][0]._section))
                             menu.add ("<a class=selhdr href="+__section[i][0]._link+">"+__section[i][0]._section+"</a>");
                         else
                             menu.add ("<a class=hdr href="+__section[i][0]._link+">"+__section[i][0]._section+"</a>");
                     }
                     else
                     {
                         Code.debug ("Section has no link: "+__section[i][0]._section);
                         menu.add ("<font color=#ffffff><b>"+__section[i][0]._section+"</b></font>");
                     }
                 }
                 else
                 {
                     //if this is first of the subsections, make
                     //a new row
                     if (j==1)
                     {
                         menu.newRow();
                         menu.newCell();
                         menu.cell().top();
                         menu.cell().right();
                     }

                     if ((_selectedSection._subSection != null)
                         &&
                         _selectedSection._subSection.equals(__section[i][j]._subSection))
                         menu.cell().add ("<a class=selmenu href="+__section[i][j]._link+">"+__section[i][j]._subSection+"</a>");
                     else
                         menu.cell().add ("<a class=menu href="+__section[i][j]._link+">"+__section[i][j]._subSection+"</a>");
                 }
                 
                 menu.cell().add("<BR>");
             }
        }

        _table.newCell();
        _table.cell().top();
        _table.cell().left();
        
        if (path.endsWith(".txt"))
            _table.nest(new Block(Block.Pre));
        
    }

    /* ------------------------------------------------------------ */
    private void addLinkHeader(String link, String context, String uri)
    {
        addHeader(
            "<link REL=\""+
            link+
            "\" HREF=\""+
            (uri.startsWith("/")?(context+uri):uri)+
            "\" >");
    }

    /* ------------------------------------------------------------ */
    public void completeSections()
    {
        if ("/index.html".equals(_path))
        {
            _table.newCell();
            _table.cell().top();
            _table.cell().center();
            
            _table.add("<A HREF=\"http://www.mortbay.com\"><IMG SRC=\""+_context+"/images/mbLogoBar.gif\" WIDTH=120 HEIGHT=75 BORDER=0 ALT=\"Mort Bay\"></A><P>\n");


            _table.add("<FORM method=GET action=http://www.google.com/custom><small><A HREF=http://www.google.com/search><IMG SRC=http://www.google.com/logos/Logo_40wht.gif border=0 ALT=Google align=middle></A><BR><INPUT TYPE=text name=q size=14 maxlength=255 value=\"\"><BR><INPUT type=hidden name=cof VALUE=\"LW:468;L:http://jetty.mortbay.org/jetty/images/jetty_banner.gif;LH:60;AH:center;S:http://jetty.mortbay.org;AWFID:1e76608d706e7dfc;\"><input type=hidden name=domains value=\"mortbay.com;mortbay.org\"><input type=radio name=sitesearch value=\"mortbay.org\" checked> mortbay.org<br><input type=radio name=sitesearch value=\"mortbay.com\"> mortbay.com<br><INPUT type=submit name=sa VALUE=\"Google Search\"><BR></small></form>");

            
            _table.add("<A HREF=\"http://www.coredevelopers.net\"><IMG SRC=\""+_context+"/images/coredev.gif\" WIDTH=81 HEIGHT=81 BORDER=0 ALT=\"CoreDev\"></A><P>\n");
            _table.add("<A HREF=\"http://sourceforge.net/projects/jboss/\"><IMG SRC=\""+_context+"/images/jboss.gif\" WIDTH=134 HEIGHT=60 BORDER=0 ALT=\"JBoss\"></A><P>\n");
            
            _table.add("<A HREF=\"http://www.inetu.com\"><IMG SRC=\""+_context+"/images/inetu.gif\" WIDTH=121 HEIGHT=52 BORDER=0 ALT=\"InetU\"></A><P>\n");
            _table.add("<A HREF=\"http://sourceforge.net/projects/jetty/\">");
            if (__realSite)
                _table.add("<IMG src=\"http://sourceforge.net/sflogo.php?group_id=7322\" width=\"88\" height=\"31\" border=\"0\" alt=\"SourceForge\">");
            else
                _table.add("<IMG SRC=\""+_context+"/images/sourceforge.gif\" WIDTH=88 HEIGHT=31 BORDER=\"0\" alt=\"SourceForge\"></A><P>\n");
            _table.add("</A><P>\n");
            
            _table.add("<A HREF=\""+_context+"/freesoftware.html\"><IMG SRC=\""+_context+"/images/effbr.gif\" WIDTH=88 HEIGHT=32 BORDER=0 ALT=\"EFF\"></A><P>\n");    
        }
        
        unnest();
        
        add("&nbsp;<P>&nbsp;<P><Center><font size=-4 color=\"#606CC0\">Copyright 2003 Mort Bay Consulting.</FONT></Center>");
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class Section
    {
        String _uri;
        String _pathSpec;
        String _key;
        String _section;
        String _subSection;
        String _link;

        Section(String context, String value)
        {
            StringTokenizer tok = new StringTokenizer(value,"\t ");
            Code.assertTrue(tok.hasMoreTokens(),"No name");
            _key=tok.nextToken();
            //Code.assertTrue(tok.hasMoreTokens(),"No URI");
            if (tok.hasMoreTokens())
                _uri=tok.nextToken();
            
            if (tok.hasMoreTokens())
                _pathSpec=tok.nextToken();
            _key=_key.replace('+',' ');
            int c=_key.indexOf(':');
            if (c>0)
            {
                _section=_key.substring(0,c);
                _subSection=_key.substring(c+1);
                if (_uri != null)
                {
                    if (_uri.startsWith("///"))
                        _link = _uri.substring(2);
                    else if (_uri.startsWith("/"))
                        _link = context+_uri;
                    else
                        _link = _uri;
                }
            }
            else
            {
                _section=_key;
                _subSection=null;
                if (_uri != null)
                {
                    if (_uri.startsWith("///"))
                        _link = _uri.substring(2);
                    else if (_uri.startsWith("/"))
                        _link = context+_uri;
                    else
                        _link = _uri;
                }
            }
            
        }
        
        public String toString()
        {
            return _key+", "+(_uri==null?"":_uri)+", "+(_pathSpec==null?"":_pathSpec);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class Links
    {
        String _top;
        String _up;
        String[] _links;
        
        Links(String l)
        {
            StringTokenizer tok=new StringTokenizer(l,", ");
            if (tok.hasMoreTokens())
                _top=tok.nextToken();
            if (tok.hasMoreTokens())
                _up=tok.nextToken();
            _links=new String[tok.countTokens()];
            int i=0;
            while (tok.hasMoreTokens())
                _links[i++]=tok.nextToken();
        }

        int size()
        {
            if (_links==null)
                return 0;
            return _links.length;
        }

        String get(int i)
        {
            if (_links==null)
                return null;
            return _links[i];
        }
        
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Links[up=");
            buf.append(_up);
            buf.append(",links=(");
            for (int i=0;i<_links.length;i++)
            {
                if (i>0)
                    buf.append(',');
                buf.append(_links[i]);
            }
            buf.append(")]");
            return buf.toString();
        }
        
    }
}





