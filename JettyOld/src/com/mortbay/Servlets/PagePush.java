// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.Code;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.mortbay.HTTP.MultiPartResponse;
import com.mortbay.HTML.Page;

import java.io.PrintWriter;
import java.util.Date;

/** Class to handle pushing of new pages upon changes within the parameters
 * of a maximum and minimum time period.
 * <p> The user can specify a minimum and/or maximum time for the makePage
 * method to be called within. Both can be -1, meaning that this is not
 * checked for. The user can call the <code>markChange</code> method to
 * indicate that the Model being viewed has changed. If within the minimum
 * time since the page was last sent, nothing happens until the minimum time
 * has expired, whereupon the <code>fillPage</code> method is called. If no
 * change happens and the maximum time is reached, the <code>fillPage</code>
 * method is called regardless.
 *
 * @version 1.0 Thu Jul 16 1998
 * @author Matthew Watson (watsonm)
 */
public abstract class PagePush
{
    /* ------------------------------------------------------------ */
    protected long minTime;
    protected long maxTime;
    protected String lookAndFeelName;
    protected boolean header;
    protected boolean footer;
    /** Is the multi-page push finished? */
    protected boolean finished = false;
    private boolean change = false;
    private boolean inMinTime = false;
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param minTime Minimum time between page refreshes 
     * @param maxTime Maximum time between page refreshes
     * @param lookAndFeelName Type of Page to create
     * @param header Should the Page header be printed?
     * @param footer Should the Page footer be printed?
     */
    protected PagePush(long minTime, long maxTime,
                       String lookAndFeelName,
                       boolean header, boolean footer)
    {
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.lookAndFeelName = lookAndFeelName;
        this.header = header;
        this.footer = footer;
    }
    /* ------------------------------------------------------------ */
    /** Notify the object that something has changed */
    protected synchronized void markChange(){
        change = true;
        if (!inMinTime) notify();
    }
    /* ------------------------------------------------------------ */
    /** Called by the user to initiate the pushing of pages.
     * This calls fillPage immediately before going into a timing pattern.
     */
    public synchronized void serve(HttpServletRequest req,
                                   HttpServletResponse res)
        throws Exception
    {
        MultiPartResponse multi = new MultiPartResponse(req,res);
        while (!finished){
            multi.startNextPart("text/html");
            PrintWriter pout = new PrintWriter(multi.out);
            Page page = Page.getPage(lookAndFeelName, req, res);
            fillPage(req, page);
            if (header && footer)
                page.write(pout);
            else {
                if (header)
                    page.write(pout, Page.Header, true);
                page.write(pout, Page.Content, true);
                if (footer)
                    page.write(pout, Page.Footer, true);
            }
            pout.flush();
            if (!finished){
                multi.endPart();
                change = false;
                if (minTime > 0){
                    inMinTime = true;
                    wait(minTime);
                    inMinTime = false;
                }
                if (!change && maxTime > 0)
                    wait(maxTime);
                else if (!change)
                    wait();
            }
        }
        multi.endLastPart();
    }
    /* ------------------------------------------------------------ */
    public abstract void fillPage(HttpServletRequest req, Page page);
    /* ------------------------------------------------------------ */
};
