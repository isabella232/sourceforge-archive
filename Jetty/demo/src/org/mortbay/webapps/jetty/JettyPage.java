// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;

import org.mortbay.html.Block;
import org.mortbay.html.Composite;
import org.mortbay.html.Element;
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
            if (InetAddress.getLocalHost().getHostName().indexOf(".mortbay.com")>0)
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
    static void initialize(String context)  
    {
        // This only works for 1 context.
        Log.event("Loading JettyPage Index");
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
    
    /* ------------------------------------------------------------ */
    private Section _section ;
    private Links _links ;
    public Section getSection() {return _section;}
    
    /* ------------------------------------------------------------ */
    public JettyPage(String context,String path)
    {
        if (__section==null)
            initialize(context);
        
        _path=path;
        if (context==null)
            context="";        

        addHeader
            ("<link REL=\"STYLESHEET\" TYPE=\"text/css\" HREF=\""+
             context+"/jetty.css\">");

        addHeader("<link REL=\"icon\" HREF=\""+context+"/images/jicon.png\" TYPE=\"image/png\">");
        
        addLinkHeader("Author",context,"http://www.mortbay.com");
        addLinkHeader("Copyright",context,"LICENSE.html");
        
        _links = (Links)__linkMap.match(_path);
        _section = (Section)__pathMap.match(_path);
        if (_section==null)
        {
            if("/".equals(_path))
                _section=__section[0][0];
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
        attribute("link","#606CC0");
        attribute("vlink","#606CC0");
        attribute("alink","#606CC0");
        //        attribute("background",context+"/images/jettybg.png");
        attribute("MARGINWIDTH","0");
        attribute("MARGINHEIGHT","0");
        attribute("LEFTMARGIN","0");
        attribute("RIGHTMARGIN","0");
        attribute("TOPMARGIN","0");
        
        
        title("Jetty: "+_section._key);
        _home=false;
        if (__section[0][0].equals(_section))
        {
            _home=true;
            title("Jetty Java HTTP Servlet Server");
            addHeader("<META NAME=\"description\" CONTENT=\"Jetty Java HTTP Servlet Server\"><META NAME=\"keywords\" CONTENT=\"Jetty Java HTTP Servlet Server\">");
        }
        
        _table = new Table(0);
        nest(_table);
        _table.cellPadding(0);
        _table.cellSpacing(0);
        _table.newRow();
        _table.newCell();
        _table.cell().bgColor("#E8ECF8");
        _table.cell().top();
        _table.cell().center();

        _table.add("<A HREF=http://jetty.mortbay.org><IMG SRC=\""+context+"/images/powered.png\" WIDTH=140 HEIGHT=58 BORDER=0 ALT=\"Powered by Jetty\"></A>\n");

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
            {
                if (_links==null)
                {    
                    if (section>0)
                        addLinkHeader("prev",context,__section[section-1][0]._uri);
                    if ((section+1)<__section.length)
                        addLinkHeader("next",context,__section[section+1][0]._uri);
                }
                
                _table.add("<FONT SIZE=+1><B>"+
                           __section[section][0]._section+
                           "</B></FONT>");
            }
            else
                _table.add(__section[section][0]._link);
            
            
            if (__section[section].length>1 &&
                _section._section.equals(__section[section][0]._section))
            {
                for (int sub=1;sub<__section[section].length;sub++)
                {
                    _table.add("<BR>");
                    if(_section.equals(__section[section][sub]))
                    {
                        if (_links==null)
                        {
                            addLinkHeader("up",context,__section[section][0]._uri);
                            if (sub>0)
                                addLinkHeader("prev",context,__section[section][sub-1]._uri);
                            if ((sub+1)<__section[section].length)
                                addLinkHeader("next",context,__section[section][sub+1]._uri);
                        }
                        _table.add("<FONT SIZE=-1><B>"+
                                   __section[section][sub]._subSection+
                                   "</B></FONT>");
                    }
                    else
                        _table.add(__section[section][sub]._link);
                }
                para=true;
            }
        }
        
        _table.add("&nbsp;<P>");
        _table.add("&nbsp;<P>");

        // home logos
        _table.add("<A HREF=\"http://www.mortbay.com\"><IMG SRC=\""+context+"/images/mbLogoBar.png\" WIDTH=120 HEIGHT=75 BORDER=0 ALT=\"Mort Bay\"></A><P>\n");
        _table.add("<A HREF=\"http://www.inetu.com\"><IMG SRC=\""+context+"/images/inetu.png\" WIDTH=121 HEIGHT=52 BORDER=0 ALT=\"InetU\"></A><P>\n");


        _table.add("<A HREF=\"http://sourceforge.net/projects/jetty\">");

        if (__realSite)
            _table.add("<IMG src=\"http://sourceforge.net/sflogo.php?group_id=7322\" width=\"88\" height=\"31\" border=\"0\" alt=\"SourceForge\">");
        else
            _table.add("<IMG SRC=\""+context+"/images/sourceforge.png\" WIDTH=88 HEIGHT=31 BORDER=\"0\" alt=\"SourceForge\"></A><P>\n");
        _table.add("</A><P>\n");

        _table.add("<A HREF=\""+context+"/freesoftware.html\"><IMG SRC=\""+context+"/images/effbr.png\" WIDTH=88 HEIGHT=32 BORDER=0 ALT=\"EFF\"></A><P>\n");
        
        _table.add("&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P>&nbsp;<P><font size=-4 color=\"#606CC0\">Copyright 2001<BR>Mort Bay Consulting.</FONT>");
        
        _table.newCell();
        _table.cell().width(3);
        _table.cell().bgColor("#606C90");
        _table.add("<BR>");
        _table.newCell();
        _table.cell().width(10);
        _table.add("&nbsp;&nbsp;");
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
            Code.assertTrue(tok.hasMoreTokens(),"No URI");
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
                if (_uri.startsWith("///"))
                    font.add(new Link(_uri.substring(2),_subSection));
                else if (_uri.startsWith("/"))
                    font.add(new Link(context+_uri,_subSection));
                else
                    font.add(new Link(_uri,_subSection));
                _link=font.toString();
            }
            else
            {
                _section=_key;
                _subSection=null;
                Font font=new Font(1,true);
                if (_uri.startsWith("///"))
                    font.add(new Link(_uri.substring(2),_section));
                else if (_uri.startsWith("/"))
                    font.add(new Link(context+_uri,_section));
                else
                    font.add(new Link(_uri,_section));
                _link=font.toString();
            }
        }
        
        public String toString()
        {
            return _key+", "+_uri+", "+(_pathSpec==null?"":_pathSpec);
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





