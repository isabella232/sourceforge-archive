// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================
package org.mortbay.http.handler;
import java.io.IOException;
import java.io.Writer;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.StringUtil;
/* ------------------------------------------------------------ */
/** Handler for Error pages
 * A handler that is registered at the org.mortbay.http.ErrorHandler
 * context attributed and called by the HttpResponse.sendError method to write a
 * error page.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ErrorPageHandler extends AbstractHttpHandler
{
    /* ------------------------------------------------------------ */
    public void handle(
        String pathInContext,
        String pathParams,
        HttpRequest request,
        HttpResponse response)
        throws HttpException, IOException
    {
        response.setContentType(HttpFields.__TextHtml);
        ByteArrayISO8859Writer writer= new ByteArrayISO8859Writer(2048);
        writeErrorPage(request, writer, response.getStatus(), response.getReason());
        writer.flush();
        response.setContentLength(writer.size());
        writer.writeTo(response.getOutputStream());
        writer.destroy();
    }
    
    /* ------------------------------------------------------------ */
    protected void writeErrorPage(HttpRequest request, Writer writer, int code, String message)
        throws IOException
    {
        if (message != null)
        {
            message= StringUtil.replace(message, "<", "&lt;");
            message= StringUtil.replace(message, ">", "&gt;");
        }
        String uri= request.getPath();
        uri= StringUtil.replace(uri, "<", "&lt;");
        uri= StringUtil.replace(uri, ">", "&gt;");
        writer.write("<html>\n<head>\n<title>Error ");
        writer.write(Integer.toString(code));
        writer.write(' ');
        writer.write(message);
        writer.write("</title>\n<body>\n<h2>HTTP ERROR: ");
        writer.write(Integer.toString(code));
        writer.write(' ');
        writer.write(message);
        writer.write("</h2>\n");
        writer.write("<p>RequestURI=");
        writer.write(uri);
        writer.write(
            "</p>\n<p><i><small><a href=\"http://jetty.mortbay.org\">Powered by Jetty://</a></small></i></p>");
        for (int i= 0; i < 20; i++)
            writer.write("\n                                                ");
        writer.write("\n</body>\n</html>\n");
    }
}
