// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** XXX Place holder for something a bit better...
 *
 * @see
 * @version 1.0 Thu Oct  7 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpServer
{
    /* ------------------------------------------------------------ */
    com.mortbay.HTTP.Handler.FileHandler _fileHandler;
    
    /* ------------------------------------------------------------ */
    /** XXX 
     * @param request 
     * @param response 
     * @exception IOException 
     * @exception HttpException 
     */
    public void service(HttpRequest request,HttpResponse response)
        throws IOException, HttpException
    {
        if (request.getQuery()!=null)
        {
            if (request.getQuery().indexOf("gzip")>=0)
            {
                response.setField(HttpFields.__TransferEncoding,"gzip");
                response.addField(HttpFields.__TransferEncoding,"chunked");
            }
            if (request.getQuery().indexOf("deflate")>=0)
            {
                response.setField(HttpFields.__TransferEncoding,"deflate");
                response.addField(HttpFields.__TransferEncoding,"chunked");
            }
        }
        

        if (_fileHandler!=null)
            _fileHandler.handle(request,response);
        if (response.getState()==response.__MSG_EDITABLE)
            dump(request,response);
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception IOException 
     * @exception HttpException 
     */
    public void dump(HttpRequest request,HttpResponse response)
        throws IOException, HttpException
    {
        response.setField(HttpFields.__ContentType,
                          HttpFields.__TextHtml);
        ChunkableOutputStream out = response.getOutputStream();

        out.println("<HTML><H1>HTTP Request Dump</H1>");
        out.println("<H3>Header:</H3><PRE>");
        out.print(request.toString());
        out.println("</PRE><H3>Parameters:</H3><PRE>");
        Set names=request.getParameterNames();
        Iterator iter = names.iterator();
        while(iter.hasNext())
        {
            String name=iter.next().toString();
            List values=request.getParameterValues(name);
            if (values==null || values.size()==0)
            {
                out.print(name);
                out.println("=");
            }
            else if (values.size()==1)
            {
                out.print(name);
                out.print("=");
                out.println(values.get(0));
            }
            else
            {
                for (int i=0; i<values.size(); i++)
                {
                    out.print(name);
                    out.print("["+i+"]=");
                    out.println(values.get(i));
                }
            }
        }
            
        out.println("</PRE><H3>Content:</H3><PRE>");
        byte[] buf= new byte[4096];
        int len;
        InputStream in=request.getInputStream();
        while((len=in.read(buf))>=0)
            out.write(buf,0,len);
        out.println("</PRE><H3>Response:</H3><PRE>");
        out.print(response.toString());
        out.println("</PRE></HTML>");

        // You wouldn't normally set a trailer like this, but
        // we don't want to commit the output to force trailers as
        // it makes test harness messy
        request.getAcceptableTransferCodings();
        if (response.acceptTrailer())
            response.getTrailer().put("TestTrailer","Value");
    }

    /* ------------------------------------------------------------ */
    /** XXX 
     * @param in 
     * @param coding 
     * @param parameters 
     * @exception HttpException 
     */
    public void enableEncoding(ChunkableInputStream in,
                               String coding,
                               Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "gzip parameters");
                in.insertFilter(java.util.zip.GZIPInputStream.class
                                .getConstructor(ChunkableInputStream.__filterArg),
                                null);
            }
            else if ("deflate".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "deflate parameters");
                in.insertFilter(java.util.zip.InflaterInputStream.class
                                .getConstructor(ChunkableInputStream.__filterArg),
                                null);
            }
            else if (!HttpFields.__Identity.equals(coding))
                throw new HttpException(HttpResponse.__501_Not_Implemented);
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** XXX 
     * @param in 
     * @param coding 
     * @param parameters 
     * @exception HttpException 
     */
    public void enableEncoding(ChunkableOutputStream out,
                               String coding,
                               Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "gzip parameters");
                out.insertFilter(java.util.zip.GZIPOutputStream.class
                                 .getConstructor(ChunkableOutputStream.__filterArg),
                                 null);
            }
            else if ("deflate".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "deflate parameters");
                out.insertFilter(java.util.zip.DeflaterOutputStream.class
                                 .getConstructor(ChunkableOutputStream.__filterArg),
                                null);
            }
            else if (!HttpFields.__Identity.equals(coding))
                throw new HttpException(HttpResponse.__501_Not_Implemented);
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
    }


    /* ------------------------------------------------------------ */
    /** 
     * @param args 
     */
    public static void main(String[] args)
    {
        try{
            InetAddrPort address = new InetAddrPort(8080);
            SocketListener listener = new SocketListener(address);
            listener.getServer()._fileHandler =
                new com.mortbay.HTTP.Handler.FileHandler(".",
                                                         "index.html",
                                                         true,
                                                         true,
                                                         true);
            listener.start();
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
};






