// ========================================================================
// Copyright (c) 1996 Intelligent Switched Systems, Sydney
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTML.*;
import java.io.*;
import java.util.*;


/* ------------------------------------------------------------------------ */
/** Servlet for presenting a hierarchy of web server index pages
 * <p> Provides configurable index and hierarchy for web pages on a web
 * server.
 *
 * <p><h4>Notes</h4>
 * <p> The config requires a global setting "HelpUri" for the help that is
 * given for each of the generated pages. This can be overridden in each pages
 * specific section.
 *
 * <p> The index is described in a nested Hashtable of the form:<PRE>
 * 
 * 
 * </PRE>
 * @see Class.ThisShouldHaveBeenChanged
 * @version $Id$
 * @author Greg Wilkins
*/
public class IndexServlet extends HttpServlet 
{
    /* ------------------------------------------------------------ */
    String pageType;
    PathMap index = null;
    String helpUri = "";

    /* ------------------------------------------------------------ */
    public IndexServlet()
    {}

    /* ------------------------------------------------------------ */
    protected IndexServlet(PathMap index)
    {
        this.index=index;
    }
    
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);

        if (index==null)
        {
            String indexName = getInitParameter("indexAttr");
            Code.assert(indexName!=null,"indexAttr not set in init Params");
            index = (PathMap)getServletContext().getAttribute(indexName);
        }
        
        Code.assert(index!=null,"Index not set in constructor or server attributes");
        Code.debug("IndexServlet configured with ",index);
        
        pageType = getInitParameter("PageType");
        if (pageType ==null)
            pageType=Page.getDefaultPageType();
    }

    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
         throws ServletException, IOException
    {
        try{
            OutputStream out = response.getOutputStream();
            PrintWriter pout = new PrintWriter(out);

            String address = request.getPathInfo()!=null
                ?(request.getServletPath()+request.getPathInfo())
                :(request.getServletPath());

            Code.debug("Looking for " + address);
            Hashtable pageConfig = (Hashtable)index.getLongestMatch(address);
            
            Page page = Page.getPage(pageType,request,response);
            page.nest(new Block(Block.Quote));
            page.nest(new Block(Block.Quote));
            page.nest(new Block(Block.Quote));
            
            if (pageConfig==null)
                return;

            // Set defaults
            page.properties().put(Page.Help,helpUri);
            int lastSlash = address.lastIndexOf("/");
            if (lastSlash==address.length()-1)
                lastSlash=address.substring(0,lastSlash).indexOf("/");
            if (lastSlash>0)
                page.properties().put(Page.Up,address.substring(0,lastSlash));
            String back = request.getHeader(HttpHeader.Referer);
            if (back != null)
                page.properties().put(Page.Back, back);

            // Move pageConfig to page properties
            Enumeration k = pageConfig.keys();
            while(k.hasMoreElements())
            {
                Object property = k.nextElement();
                Object value = pageConfig.get(property);
                page.properties().put(property,value);
            }
            
            page.add(Break.para);
            page.add((String)pageConfig.get("Text"));
            page.add(Break.para);
        
            String[][] items = (String[][])pageConfig.get("Items");
            for (int i=0;i<items.length;i++)
            {
                if (items[i][1]==null && items[i][2]==null)
                {
                    page.add(Break.line);
                    page.add(new Heading(2,items[i][0]));
                }
                else
                {
                    Block block = new Block(Block.Bold);
                    block.add(new Link(response.encodeURL(items[i][1]),items[i][0]));
                    page.add(block);
                    page.add(Break.line);
                    page.add(items[i][2]);
                    page.add(Break.para);
                }
            }
            page.add(Break.para);
        
            page.write(pout);
            pout.flush();
        }
        catch(RuntimeException e){
            throw e;
        }
        catch(IOException e){
            throw e;
        }
        catch(Exception e){
            Code.debug("rethrow",e);
            throw new ServletException(e.toString());
        }
    }
};
