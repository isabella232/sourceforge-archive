// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;


import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.Socket;
import org.mortbay.http.ChunkableOutputStream;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.URI;
import org.mortbay.util.LineInput;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** Dump request handler.
 * Dumps GET and POST requests.
 * Useful for testing and debugging.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ProxyHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {

        // XXX we should proxy IF
        //  1) we have a configured path that we send to a configured
        //     destination (URL rewritting forbidden by rfc???)
        //  2) We have a http and the host is not what is configured
        //     as us
        //  3) We have a ftp scheme and the FTP client classes are in
        //     our classpath (should delegate to another class to
        //     avoid linking hassles).
        URI uri = request.getURI();
        if (!"http".equals(uri.getScheme()))
            return;
        
        System.err.println("\nPROXY:");
        System.err.println("pathInContext="+pathInContext);
        System.err.println("URI="+uri);

        Socket socket=null;
        try
        {
            String host=uri.getHost();
            int port =uri.getPort();
            if (port<=0)
                port=80;
            String path=uri.getPath();
            if (path==null || path.length()==0)
                path="/";
            
            System.err.println("host="+host);
            System.err.println("port="+port);
            System.err.println("path="+path);
            
            // XXX associate this socket with the connection so
            // that it may be persistent.
            
            socket = new Socket(host,port);
            socket.setSoTimeout(5000); // XXX configure this
            System.err.println("socket="+socket);
            
            OutputStream sout=socket.getOutputStream();
            System.err.println("sout="+sout);
            
            request.setState(HttpMessage.__MSG_EDITABLE);
            HttpFields header=request.getHeader();

            // XXX Lets reject range requests at this point!!!!?
            
            // XXX need to process connection header????

            // XXX Handle Max forwards - and maybe OPTIONS/TRACE???
            
            // XXX need to encode the path

            header.put("Connection","close");
            header.add("Via","Via: 1.1 host (Jetty/4.x)");

            // Convert the header to byte array.
            // Should assoc writer with connection for recycling!
            ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
            writer.write(request.getMethod());
            writer.write(' ');
            writer.write(path);
            writer.write(' ');
            writer.write(request.getDotVersion()==0
                         ?HttpMessage.__HTTP_1_0
                         :HttpMessage.__HTTP_1_1);
            writer.write('\015');
            writer.write('\012');
            header.write(writer);
            
            // Send the request to the next hop.
            System.err.println("\nreq=\n"+new String(writer.getBuf(),0,writer.length()));
            writer.writeTo(sout);
            writer.reset();
            
            // XXX If expect 100-continue flush or no body the header now!
            sout.flush();
            
            // XXX cache http versions and do 417
            
            // XXX To to copy content with content length or chunked.


            // get ready to read the results back
            LineInput lin = new LineInput(socket.getInputStream());
            System.err.println("lin="+lin);

            // XXX need to do something about timeouts here
            String resLine = lin.readLine();
            if (resLine==null)
                return; // XXX what should we do?
            
            // At this point we are committed to sending a response!!!!
            request.setHandled(true);
            response.setState(HttpMessage.__MSG_SENT);
            OutputStream out=response.getOutputStream();
            
            // Forward 100 responses
            while (resLine.startsWith("100"))
            {
                writer.write(resLine);
                writer.writeTo(out);
                out.flush();
                writer.reset();
                
                resLine = lin.readLine();
                if (resLine==null)
                    return; // XXX what should we do?
            }
            
            System.err.println("resLine="+resLine);

            // Receive the response headers
            HttpFields resHeader = response.getHeader();
            resHeader.clear();

            // Modify them
            resHeader.read(lin);
            resHeader.add("Via","Via: 1.1 host (Jetty/4.x)"); // XXX
            // XXX do the connection based stuff here!

            // return the header
            // XXX this should really be set in the 
            writer.write(resLine);
            writer.write('\015');
            writer.write('\012');
            resHeader.write(writer);
            System.err.println("\nres=\n"+resLine+"\015\012"+resHeader);
            writer.writeTo(out);

            // return the body
            // XXX need more content length options here
            // XXX need to handle prechunked 
            IO.copy(lin,out);
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        finally
        {
            request.setState(HttpMessage.__MSG_RECEIVED);
            if (socket!=null)
            {
                try{socket.close();}
                catch(Exception e){Code.warning(e);}
            }
        }
        
        
    }
}
