// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.util.*;
import java.text.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Dump request handler.
 * Dumps GET and POST requests.
 * Useful for testing and debugging.
 * @see
 * @version 1.0 Mon Oct 11 1999
 * @author Greg Wilkins (gregw)
 */
public class DumpHandler extends NullHandler
{
    /* ----------------------------------------------------------------- */
    public String realPath(String pathSpec, String path)
    {
        return "";
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathSpec,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if (!isStarted())
            return;
	
        // Only handle GET, HEAD and POST
        if (!request.__GET.equals(request.getMethod()) &&
            !request.__HEAD.equals(request.getMethod()) &&
            !request.__POST.equals(request.getMethod()))
            return;

	Code.debug("Dump");
	
        response.setField(HttpFields.__ContentType,
                          HttpFields.__TextHtml);
        ChunkableOutputStream out = response.getOutputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
        Writer writer = new OutputStreamWriter(buf,"ISO-8859-1");
        writer.write("<HTML><H1>HTTP Request Dump</H1>");
        writer.write("<PRE>\npath="+request.getPath()+
                    "\nmatch="+
                    PathMap.pathMatch(pathSpec,request.getPath())+
                    "\ninfo="+
                    PathMap.pathInfo(pathSpec,request.getPath())+
                    "\n</PRE>\n");
        writer.write("<H3>Header:</H3><PRE>");
        writer.write(request.toString());
        writer.write("</PRE>\n<H3>Parameters:</H3>\n<PRE>");
        Set names=request.getParameterNames();
        Iterator iter = names.iterator();
        while(iter.hasNext())
        {
            String name=iter.next().toString();
            List values=request.getParameterValues(name);
            if (values==null || values.size()==0)
            {
                writer.write(name);
                writer.write("=\n");
            }
            else if (values.size()==1)
            {
                writer.write(name);
                writer.write("=");
                writer.write((String)values.get(0));
                writer.write("\n");
            }
            else
            {
                for (int i=0; i<values.size(); i++)
                {
                    writer.write(name);
                    writer.write("["+i+"]=");
                    writer.write((String)values.get(i));
                    writer.write("\n");
                }
            }
        }
        
        String set_cookie=request.getParameter("CookieName");
        if (set_cookie!=null && set_cookie.trim().length()>0)
        {
            try{
                set_cookie=set_cookie.trim();
                String cv=request.getParameter("CookieVal");
                response.addSetCookie(set_cookie,cv,null,"/",60*60*1000,false);
            }
            catch(IllegalArgumentException e)
            {
                writer.write("</PRE>\n<H3>BAD Set-Cookie:</H3>\n<PRE>");
                writer.write(e.toString());
                Code.ignore(e);
            }
        }
        
        
        Map cookies=request.getCookies();
        if (cookies!=null && cookies.size()>0)
        {
            writer.write("</PRE>\n<H3>Cookies:</H3>\n<PRE>");
            Iterator c=cookies.keySet().iterator();
            while(c.hasNext())
            {
                String cookie=c.next().toString();
                writer.write(cookie);
                writer.write("=");
                writer.write(cookies.get(cookie).toString());
                writer.write("\n");
            }
        }
        
        Collection attributes=request.getAttributeNames();
        if (attributes!=null && attributes.size()>0)
        {
            writer.write("</PRE>\n<H3>Attributes:</H3>\n<PRE>");
            Iterator a=attributes.iterator();
            while(a.hasNext())
            {
                String attr=a.next().toString();
                writer.write(attr);
                writer.write("=");
                writer.write(request.getAttribute(attr).toString());
                writer.write("\n");
            }
        }
        
        writer.write("</PRE>\n<H3>Content:</H3>\n<PRE>");
        byte[] content= new byte[4096];
        int len;
        try{
            InputStream in=request.getInputStream();
            while((len=in.read(content))>=0)
                writer.write(new String(content,0,len));
        }
        catch(IOException e)
        {
            Code.ignore(e);
            writer.write(e.toString());
        }
        
        writer.write("</PRE>\n<H3>Response:</H3>\n<PRE>");
        writer.write(response.toString());
        writer.write("</PRE></HTML>");
        writer.flush();
        response.setIntField(HttpFields.__ContentLength,buf.size());
        buf.writeTo(out);

        // You wouldn't normally set a trailer like this, but
        // we don't want to commit the output to force trailers as
        // it makes test harness messy
        request.getAcceptableTransferCodings();
        if (response.acceptTrailer())
            response.getTrailer().put("TestTrailer","Value");

        request.setHandled(true);
    }
}





