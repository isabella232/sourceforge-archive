// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.html.Block;
import org.mortbay.html.Page;
import org.mortbay.html.Table;
import org.mortbay.http.PathMap;
import org.mortbay.util.LogSupport;

/* ================================================================ */
public class JettyPage extends Page
{
    private static Log log = LogFactory.getLog(JettyPage.class);

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
                log.info("Real Jetty Site");
                __realSite=true;
            }
            
            __realSite = true;
        }
        catch(Exception e) {LogSupport.ignore(log,e);}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param _context 
     */
    static synchronized void initialize(String context)  
    {
        if (__section!=null)
	    return;

        // This only works for 1 _context.
        log.debug("Loading JettyPage Index");
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
                        if(log.isDebugEnabled())log.debug(key+" = "+section);
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
                                LogSupport.ignore(log,e);
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
                    
                        LogSupport.ignore(log,e);
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
            LogSupport.ignore(log,e);
        }
        catch(Throwable th)
        {
            log.warn(LogSupport.EXCEPTION,th);
        }
        
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
    
    private Block _divLeft = null;
    private Block _divHeader = null;
    private Block _divRight = null;
    private Block _divContent = null;
    
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

        addHeader("<link REL=\"icon\" HREF=\""+_context+"/images/jicon.gif\" TYPE=\"image/gif\">");
        
        addLinkHeader("Author",context,"http://www.mortbay.com");
        addLinkHeader("Copyright",context,"LICENSE.TXT");
        
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
        
       
        
        _divLeft = new Block("div", "class=\"divLeft\"");
        _divHeader = new Block("div", "class=\"divHeader\"");
        _divRight = new Block("div", "class=\"divRight\"");
        _divContent = new Block ("div", "class=\"divContent\"");
        
        add(_divHeader);
        add(_divLeft);
        add(_divContent);
        add(_divRight);
        
         String searchString = "<FORM method=GET action=http://www.google.com/custom><div class=prova><span>Search this site with Google:</span><INPUT TYPE=text name=q size=14 maxlength=255 value=\"\"><input type=hidden name=\"sitesearch\" value=\"mortbay.org\"><INPUT type=hidden name=cof VALUE=\"LW:468;L:http://jetty.mortbay.org/jetty/images/jetty_banner.gif;LH:60;AH:center;S:http://jetty.mortbay.org;AWFID:1e76608d706e7dfc;\"><input type=hidden name=domains value=\"mortbay.org\"><INPUT class=\"go\" type=submit name=sa VALUE=\"Go\"/></div></form>";
       
        if ("/index.html".equals(_path))
          _divHeader.add("<img src=\""+_context+"/images/jetty_banner.gif\" alt=\"Jetty Java HTTP Servlet Server\" width=\"467\" height=\"60\"/>");
        else
          _divHeader.add("<img src=\""+_context+"/images/jetty_banner_still.gif\"  alt=\"Jetty Java HTTP Servlet Server\" width=\"467\" height=\"60\"/>");

        _divHeader.add(searchString);

       
        Block _divMenu = new Block("div", "class=\"menuLeft\"");
        _divLeft.add(_divMenu);

        
        Block _divMenuHead = null;//current menu header
        
        boolean para=true;
        // navigation - iterate over all sections
        for (int i=0;i<__section.length;i++)
        {
             for (int j=0; j < __section[i].length; j++)
             {
                 // this is the section header,make a new row
                 if (j==0)
                 {
                    _divMenuHead = new Block("div", "class=\"menuHead\"");
                    _divMenu.add(_divMenuHead);

                     if (__section[i][0]._link != null)
                     {
                         if(log.isDebugEnabled())log.debug("Section "+__section[i][0]._section+" has link "+__section[i][0]._link);
                         
                         if (_selectedSection._section.equals(__section[i][0]._section))
                             _divMenuHead.add ("<a class=selhdr href="+__section[i][0]._link+">"+__section[i][0]._section+"</a>");
                         else
                             _divMenuHead.add ("<a class=hdr href="+__section[i][0]._link+">"+__section[i][0]._section+"</a>");
                     }
                     else
                     {
                         if(log.isDebugEnabled())log.debug("Section has no link: "+__section[i][0]._section);
                         	_divMenuHead.add ("<span class=\"empty\">"+__section[i][0]._section+"</span>");
                     }
                 }
                 else
                 {
                    
                     Block _divMenuItem = new Block("div", "class=\"menuItem\"");
                     _divMenu.add(_divMenuItem);
                     
                     if ((_selectedSection._subSection != null)
                         &&
                         _selectedSection._subSection.equals(__section[i][j]._subSection))
                         _divMenuItem.add ("<a class=selmenu href="+__section[i][j]._link+">"+__section[i][j]._subSection+"</a>");
                     else
                         _divMenuItem.add ("<a class=menu href="+__section[i][j]._link+">"+__section[i][j]._subSection+"</a>");
                 }
             }
        }

        
        _divLeft.add ("<P class=\"copyright\">Copyright 2005 Mort Bay Consulting</P>");
        setNest(_divContent);
        if (path.endsWith(".txt"))
            _divContent.nest(new Block(Block.Pre));
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
            
            Block _resourceHead = new Block ("div", "class=menuRightHead");
           _divRight.add(_resourceHead);
            _divRight.add("<div class=\"menuRightItem\"><A HREF=\"http://www.mortbay.com\"><IMG SRC=\""+_context+"/images/mbLogoBar.gif\" WIDTH=120 HEIGHT=75 ALT=\"Mort Bay\"></A></div>");

            _divRight.add("<br/>");

            _divRight.add("<div class=\"menuRightItem\"><A HREF=\"http://sourceforge.net/projects/jetty/\">");
            if (__realSite)
                _divRight.add("<IMG src=\"http://sourceforge.net/sflogo.php?group_id=7322\" width=\"88\" height=\"31\" border=\"0\" alt=\"SourceForge\">");
            else
                _divRight.add("<IMG SRC=\""+_context+"/images/sourceforge.gif\" WIDTH=88 HEIGHT=31 BORDER=\"0\" alt=\"SourceForge\"></A>");
            _divRight.add("</A></div>");

            _divRight.add("<br/>");
            _divRight.add("<div class=\"menuRightItem\"><A HREF=\"http://www.codehaus.org\"><IMG SRC=\""+_context+"/images/codehaus-small.png\" WIDTH=140 BORDER=0 ALT=\"Codehaus\"></A></div>");
            
            _divRight.add("<br/>");
            _divRight.add("<div class=\"menuRightItem\"><A HREF=\"http://www.coredevelopers.net\"><IMG SRC=\""+_context+"/images/coredev.gif\" WIDTH=81 HEIGHT=81 BORDER=0 ALT=\"CoreDev\"></A></div>");
            
            _divRight.add("<br/>");
            _divRight.add("<div class=\"menuRightItem\"><A HREF=\"http://www.inetu.net\"><IMG SRC=\""+_context+"/images/inetu.gif\" WIDTH=121 HEIGHT=52 BORDER=0 ALT=\"InetU\"></A></div>");
            
            _divRight.add("<br/>");
            _divRight.add("<div class=\"menuRightItem\"><A HREF=\""+_context+"/freesoftware.html\"><IMG SRC=\""+_context+"/images/effbr.gif\" WIDTH=88 HEIGHT=32 BORDER=0 ALT=\"EFF\"></A><P></div>");    
                    
            _divRight.add("<br/>");
            _divRight.add("<div class=\"menuRightItem\"><A HREF=http://jetty.mortbay.org><IMG class=\"powered\" SRC=\""+_context+"/images/powered.gif\" WIDTH=140 HEIGHT=58 BORDER=0 ALT=\"Powered by Jetty\"></a></div>");
        }
        
       
       
       
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
            _key=tok.nextToken();
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





