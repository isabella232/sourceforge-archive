// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlet;
import com.mortbay.HTML.Page;
import com.mortbay.HTML.TableForm;
import com.mortbay.HTML.Font;
import com.mortbay.HTML.Break;
import com.mortbay.HTML.Block;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.StringUtil;
import com.mortbay.Util.WriterLogSink;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Writer;


/* ------------------------------------------------------------ */
// Don't  write servlets like this one :-)
public class Debug extends HttpServlet
{
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) 
        throws ServletException, IOException
    {
        Page page= new Page();
        page.title(getServletInfo());
        page.attribute("text","#000000");
        page.attribute(Page.BGCOLOR,"#FFFFFF");
        page.attribute("link","#606CC0");
        page.attribute("vlink","#606CC0");
        page.attribute("alink","#606CC0");

        Log log = Log.instance();
        
        TableForm tf = new TableForm(request.getRequestURI());
        page.add(tf);
        tf.table().newRow().addCell(new Block(Block.Bold)
            .add(new Font(3,true).add(getServletInfo()))).cell().attribute("COLSPAN","2");
        tf.table().add(Break.rule);
        
        tf.addCheckbox("D","Debug On",Code.getDebug());
        tf.addTextField("V","Verbosity Level",6,""+Code.getVerbose());
        tf.addTextField("P","Debug Patterns",40,Code.getDebugPatterns());
        tf.addTextField("T","Debug Triggers",40,Code.getDebugTriggers());
        tf.addCheckbox("W","Suppress Warnings",Code.getSuppressWarnings());
        tf.addCheckbox("S","Suppress Stacks",Code.getSuppressStack());

        
        LogSink[] sinks = log.getLogSinks();
        for (int s=0;sinks!=null && s<sinks.length;s++)
        {
            if (sinks[s]==null)
                continue;

            tf.table().newRow().addCell(Break.rule).cell().attribute("COLSPAN","2");
            tf.table().newRow().addCell("<B><font size=\"+1\">Log Sink "+s+":</font></B").right();
            tf.table().addCell(sinks[s].getClass().getName()).left();

            tf.addCheckbox("LSS"+s,"Started",sinks[s].isStarted());
            
            String logOptions=sinks[s].getOptions();
            if (sinks[s] instanceof WriterLogSink)
            {
                tf.addCheckbox("Lt"+s,"Timestamp",logOptions.indexOf("t")>=0);
                logOptions=StringUtil.replace(logOptions,"t","");
                tf.addCheckbox("LT"+s,"Tag",logOptions.indexOf("T")>=0);
                logOptions=StringUtil.replace(logOptions,"T","");
                tf.addCheckbox("LL"+s,"Label",logOptions.indexOf("L")>=0);
                logOptions=StringUtil.replace(logOptions,"L","");
                tf.addCheckbox("Ls"+s,"Stack Size",logOptions.indexOf("s")>=0);
                logOptions=StringUtil.replace(logOptions,"s","");
                tf.addCheckbox("LS"+s,"Stack Trace",logOptions.indexOf("S")>=0);
                logOptions=StringUtil.replace(logOptions,"S","");
                tf.addCheckbox("SL"+s,"Single Line",logOptions.indexOf("O")>=0);
                logOptions=StringUtil.replace(logOptions,"O","");
                tf.addTextField("LO"+s,"Other Options",10,logOptions);
                
                String filename=((WriterLogSink)sinks[s]).getFilename();
                tf.addTextField("LF"+s,"Log File Name",40,filename);
            }
            else
                tf.addTextField("LO"+s,"Options",10,logOptions);
        }
        
        tf.table().newRow().addCell(Break.rule).cell().attribute("COLSPAN","2");
        
        tf.addTextField("LSC","Add LogSink Class",40,"com.mortbay.Util.WriterLogSink");
        
        tf.addButtonArea();
        tf.addButton("Action","Set Options");
        tf.addButton("Action","Add LogSink");
        tf.addButton("Action","Delete Stopped Sinks");
        tf.table().newRow().addCell(Break.rule).cell().attribute("COLSPAN","2");
        
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
        Log log = Log.instance();
        String action=request.getParameter("Action");
        
        if ("Set Options".equals(action))
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


            LogSink[] sinks = log.getLogSinks();
            for (int s=0;sinks!=null && s<sinks.length;s++)
            {
                if (sinks[s]==null)
                    continue;
                
                if ("on".equals(request.getParameter("LSS"+s)))
                {
                    if(!sinks[s].isStarted())
                        try{sinks[s].start();}catch(Exception e){Code.warning(e);}
                }
                else
                {
                    if(sinks[s].isStarted())
                        try{sinks[s].stop();}catch(InterruptedException e){}
                }

                String options=request.getParameter("LO"+s);
                if (options==null)
                    options="";
                
                if (sinks[s] instanceof WriterLogSink)
                {
                    if ("on".equals(request.getParameter("Lt"+s)))options+="t";
                    if ("on".equals(request.getParameter("LL"+s)))options+="L";
                    if ("on".equals(request.getParameter("LT"+s)))options+="T";
                    if ("on".equals(request.getParameter("Ls"+s)))options+="s";
                    if ("on".equals(request.getParameter("LS"+s)))options+="S";
                    if ("on".equals(request.getParameter("SL"+s)))options+="O";

                    String filename = request.getParameter("LF"+s);
                    ((WriterLogSink)sinks[s]).setFilename(filename);
                }
                
                sinks[s].setOptions(options);
            }
        }
        else if ("Add LogSink".equals(action))
        {
            try
            {
                Class logSinkClass =
                    Class.forName(request.getParameter("LSC"));
                LogSink logSink = (LogSink)logSinkClass.newInstance();
                log.add(logSink);
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
        else if ("Delete Stopped Sinks".equals(action))
        {
            log.deleteStoppedLogSinks();
        }
        
        response.sendRedirect(request.getContextPath()+
                              request.getServletPath()+"/"+
                              Long.toString(System.currentTimeMillis(),36)+
                              (target!=null?("#"+target):""));
    }
    
    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Debug And  Log Options";
    }
}










