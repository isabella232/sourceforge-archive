// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class RootNotFoundHandler extends NotFoundHandler
{
    private static Log log = LogFactory.getLog(RootNotFoundHandler.class);

    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        log.debug("Root Not Found");
        String method=request.getMethod();
        
        if (!method.equals(HttpRequest.__GET) ||
            !request.getPath().equals("/"))
        {
            // don't bother with fancy format.
            super.handle(pathInContext,pathParams,request,response);
            return;
        }

        response.setStatus(404);
        request.setHandled(true);
        response.setReason("Not Found");
        response.setContentType(HttpFields.__TextHtml);
        
        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

        String uri=request.getPath();
        uri=StringUtil.replace(uri,"<","&lt;");
        uri=StringUtil.replace(uri,">","&gt;");
        
        writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found");
        writer.write("</TITLE>\n<BODY>\n<H2>Error 404 - Not Found.</H2>\n");
        writer.write("No context on this server matched or handled this request.<BR>");
        writer.write("Contexts known to this server are: <ul>");

        HttpContext[] contexts = getHttpContext().getHttpServer().getContexts();
        
        for (int i=0;i<contexts.length;i++)
        {
            HttpContext context = contexts[i];
            writer.write("<li><a href=\"");
            writer.write(context.getContextPath());
            writer.write("/\">");
            writer.write(context.toString());
            writer.write("</a></li>\n");
        }
        
        writer.write("</ul><small><I>The links above may not work if a virtual host is configured</I></small>");

	for (int i=0;i<10;i++)
	    writer.write("\n<!-- Padding for IE                  -->");
	
        writer.write("\n</BODY>\n</HTML>\n");
        writer.flush();
        response.setContentLength(writer.size());
        OutputStream out=response.getOutputStream();
        writer.writeTo(out);
        out.close();
    }
}
