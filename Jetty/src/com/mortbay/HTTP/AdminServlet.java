// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.HTML.Break;
import com.mortbay.HTML.Composite;
import com.mortbay.HTML.Heading;
import com.mortbay.HTML.Element;
import com.mortbay.HTML.Block;
import com.mortbay.HTML.Font;
import com.mortbay.HTML.Link;
import com.mortbay.HTML.List;
import com.mortbay.HTML.Page;
import com.mortbay.HTML.TableForm;
import com.mortbay.HTML.Target;
import com.mortbay.HTTP.Handler.Servlet.ServletHandler;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.WriterLogSink;
import com.mortbay.Util.LifeCycle;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/* ------------------------------------------------------------ */
/** Jetty Administration Servlet.
 *
 * This is a minimal start to a administration servlet that allows
 * start/stop of server components and control of debug parameters.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class AdminServlet extends HttpServlet
{
    private java.util.List _servers;
    
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
        _servers=HttpServer.getHttpServerList();
    }
    
    /* ------------------------------------------------------------ */
    private String doAction(HttpServletRequest request,
                            HttpServletResponse response) 
        throws ServletException, IOException
    {
        boolean start="Start".equals(request.getParameter("A"));
        String id=request.getParameter("ID");

        StringTokenizer tok=new StringTokenizer(id,":");
        int tokens=tok.countTokens();
        String target=null;
        
        try{
            target=tok.nextToken();
            HttpServer server=(HttpServer)
                _servers.get(Integer.parseInt(target));
            
            if (tokens==1)
            {
                // Server stop/start
                if (start) server.start();
                else server.stop();
            }
            else if (tokens==3)
            {
                // Listener stop/start
                String l=tok.nextToken()+":"+tok.nextToken();
                Collection listeners=server.getListeners();
                Iterator i2 = listeners.iterator();
                while(i2.hasNext())
                {
                    HttpListener listener = (HttpListener) i2.next();
                    if (listener.toString().indexOf(l)>=0)
                    {
                        if (start) listener.start();
                        else listener.stop();
                    }
                }
            }
            else
            {
                String host=tok.nextToken();
                if ("null".equals(host))
                    host=null;
                
                String contextPath=tok.nextToken();
                target+=":"+host+":"+contextPath;
                if (contextPath.length()>1)
                    contextPath+="/*";
                int contextIndex=Integer.parseInt(tok.nextToken());
                target+=":"+contextIndex;
                HandlerContext
                    context=server.getContext(host,contextPath,contextIndex);
                
                if (tokens==4)
                {
                    // Context stop/start
                    if (start) context.start();
                    else context.stop();
                }
                else if (tokens==5)
                {
                    // Handler stop/start
                    int handlerIndex=Integer.parseInt(tok.nextToken());
                    HttpHandler handler=context.getHandler(handlerIndex);
                    
                    if (start) handler.start();
                    else handler.stop();
                }
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        catch(Error e)
        {
            Code.warning(e);
        }
        
        return target;
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {
        if (request.getQueryString()!=null &&
            request.getQueryString().length()>0)
        {
            String target=doAction(request,response);
            response.sendRedirect(request.getContextPath()+
                                  request.getServletPath()+
                                  (request.getPathInfo()!=null
                                   ?request.getPathInfo():"")+
                                  (target!=null?("#"+target):""));
            return;
        }
        
        Page page= new Page();
        page.title(getServletInfo());
        page.addHeader("");
        page.attribute("text","#000000");
        page.attribute(Page.BGCOLOR,"#FFFFFF");
        page.attribute("link","#606CC0");
        page.attribute("vlink","#606CC0");
        page.attribute("alink","#606CC0");

        page.add(new Block(Block.Bold).add(new Font(3,true).add(getServletInfo())));
        page.add(Break.rule);
        page.add(new Link(request.getServletPath()+"/#Components","Components"));
        page.add(Break.line);
        page.add(new Link(request.getServletPath()+"/#Debug","Debug"));
        page.add(Break.rule);
        page.add(new Target("Components"));
        page.add(new Heading(3,"Components:"));

        List sList=new List(List.Ordered);
        page.add(sList);
        String id1;
        
        for(int i1=0;i1<_servers.size();i1++)
        {
            id1=""+i1;
            HttpServer server=(HttpServer)_servers.get(i1);
            Composite sItem = sList.newItem();
            sItem.add("<B>HttpServer&nbsp;");
            sItem.add(lifeCycle(request,id1,server));
            sItem.add("</B>");
            sItem.add(Break.line);
            sItem.add("<B>Listeners:</B>");
            List lList=new List(List.Unordered);
            sItem.add(lList);

            Collection listeners=server.getListeners();
            Iterator i2 = listeners.iterator();
            while(i2.hasNext())
            {
                HttpListener listener = (HttpListener) i2.next();
                String id2=id1+":"+listener;
                lList.add(lifeCycle(request,id2,listener));
            }

            Map hostMap = server.getHostMap();
            
            sItem.add("<B>Contexts:</B>");
            List hcList=new List(List.Unordered);
            sItem.add(hcList);
            i2=hostMap.entrySet().iterator();
            while(i2.hasNext())
            {
                Map.Entry hEntry=(Map.Entry)(i2.next());
                String host=(String)hEntry.getKey();

                PathMap contexts=(PathMap)hEntry.getValue();
                Iterator i3=contexts.entrySet().iterator();
                while(i3.hasNext())
                {
                    Map.Entry cEntry=(Map.Entry)(i3.next());
                    String contextPath=(String)cEntry.getKey();
                    java.util.List contextList=(java.util.List)cEntry.getValue();
                    
                    Composite hcItem = hcList.newItem();
                    if (host!=null)
                        hcItem.add("Host="+host+":");
                    hcItem.add("ContextPath="+contextPath);
                    
                    String id3=id1+":"+host+":"+
                        (contextPath.length()>2
                         ?contextPath.substring(0,contextPath.length()-2)
                         :contextPath);
                    
                    List cList=new List(List.Ordered);
                    hcItem.add(cList);
                    for (int i4=0;i4<contextList.size();i4++)
                    {
                        String id4=id3+":"+i4;
                        Composite cItem = cList.newItem();
                        HandlerContext hc=
                            (HandlerContext)contextList.get(i4);
                        cItem.add(lifeCycle(request,id4,hc));
                        cItem.add("<BR>ResourceBase="+hc.getResourceBase());
                        cItem.add("<BR>ClassPath="+hc.getClassPath());

                    
                        List hList=new List(List.Ordered);
                        cItem.add(hList);
                        for(int i5=0;i5<hc.getHandlerSize();i5++)
                        {
                            String id5=id4+":"+i5;
                            HttpHandler handler = hc.getHandler(i5);
                            Composite hItem=hList.newItem();
                            hItem.add(lifeCycle(request,
                                                id5,
                                                handler,
                                                handler.getName()));
                            if (handler instanceof ServletHandler)
                            {
                                hItem.add("<BR>"+
                                          ((ServletHandler)handler)
                                          .getServletMap());
                            }
                        }
                    }
                }
            }
            sItem.add("<P>");
        }


        page.add(Break.rule);
        page.add(new Target("Debug"));
        page.add(new Heading(3,"Debug:"));

        Log log = Log.instance();
        boolean logStackTrace=false;
        boolean logOneLine=false;
        LogSink[] sinks = log.getLogSinks();
        for (int s=0;sinks!=null && s<sinks.length;s++)
        {
            if (sinks[s]==null || !(sinks[s] instanceof WriterLogSink))
                continue;
            logStackTrace=logStackTrace||((WriterLogSink)sinks[s]).isLogStackTrace();
            logOneLine=logOneLine||((WriterLogSink)sinks[s]).isLogOneLine();
        }
        
        TableForm tf = new TableForm(request.getRequestURI());
        page.add(tf);
        
        tf.addCheckbox("D","Debug On",Code.getDebug());
        tf.addTextField("V","Verbosity Level",6,""+Code.getVerbose());
        tf.addTextField("P","Debug Patterns",40,Code.getDebugPatterns());
        tf.addTextField("T","Debug Triggers",40,Code.getDebugTriggers());
        tf.addCheckbox("LS","Log Stack",logStackTrace);
        tf.addCheckbox("OL","Log One Line",logOneLine);
        tf.addCheckbox("W","Suppress Warnings",Code.getSuppressWarnings());
        tf.addCheckbox("S","Suppress Stacks",Code.getSuppressStack());
        tf.addButton("Action","Set Debug Options");
        
        response.setContentType("text/html");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache,no-store");
        Writer writer=response.getWriter();
        page.write(writer);
        writer.flush();
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request,
                        HttpServletResponse response) 
        throws ServletException, IOException
    {
        String target=null;
        if ("Set Debug Options".equals(request.getParameter("Action")))
        {
            Code.setDebug("on".equals(request.getParameter("D")));
            Code.setSuppressWarnings("on".equals(request.getParameter("W")));
            Code.setSuppressStack("on".equals(request.getParameter("S")));
            String v=request.getParameter("V");
            if (v!=null && v.length()>0)
                Code.setVerbose(Integer.parseInt(v));
            else
                Code.setVerbose(0);
            Code.setDebugPatterns(request.getParameter("P"));
            Code.setDebugTriggers(request.getParameter("T"));



            Log log = Log.instance();
            LogSink[] sinks = log.getLogSinks();
            boolean logStackTrace="on".equals(request.getParameter("LS"));
            boolean logOneLine="on".equals(request.getParameter("OL"));
            for (int s=0;sinks!=null && s<sinks.length;s++)
            {
                if (sinks[s]==null || ! (sinks[s] instanceof WriterLogSink))
                    continue;
                ((WriterLogSink)sinks[s]).setLogStackTrace(logStackTrace);
                ((WriterLogSink)sinks[s]).setLogOneLine(logOneLine);
            }

            
            target="Debug";
        }

        response.sendRedirect(request.getContextPath()+
                              request.getServletPath()+"/"+
                              Long.toString(System.currentTimeMillis(),36)+
                              (target!=null?("#"+target):""));
    }
    
    /* ------------------------------------------------------------ */
    private Element lifeCycle(HttpServletRequest request,
                              String id,
                              LifeCycle lc)
    {
        return lifeCycle(request,id,lc,lc.toString());
    }
    
    /* ------------------------------------------------------------ */
    private Element lifeCycle(HttpServletRequest request,
                              String id,
                              LifeCycle lc,
                              String name)
    {
        Composite comp=new Composite();
        comp.add(new Target(id));
        Font font = new Font();
        comp.add(font);
        font.color(lc.isStarted()?"green":"red");
        font.add(name);

        String action=lc.isStarted()?"Stop":"Start";
        
        comp.add("&nbsp;[");
        comp.add(new Link(request.getServletPath()+
                          "?A="+action+"&ID="+id,action));
        comp.add("]");
        return comp;
    }
    
    
    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "HTTP Admin";
    }
}










