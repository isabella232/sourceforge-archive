// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;


import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.Socket;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
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


            // XXX need to do loop detection here - is the host me???
            
            // XXX associate this socket with the connection so
            // that it may be persistent.
            socket = new Socket(host,port);
            socket.setSoTimeout(5000);
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
            
            
            // XXX yuck!
            String req=
                request.getMethod()+" "+path+" "+request.getVersion()+"\015\012"+
                header;
            System.err.println("\nreq=\n"+req);
            sout.write(req.getBytes(StringUtil.__ISO_8859_1));

            
            // XXX If expect 100-continue flush the header now!
            // XXX cache http versions and do 417
            
            // XXX To to copy content with content length or chunked.


            LineInput lin = new LineInput(socket.getInputStream());
            System.err.println("lin="+lin);

            // XXX need to do something about timeouts here
            String resLine = lin.readLine();
            System.err.println("resLine="+resLine);
            // XXX handle/forward 100 responses

            request.setHandled(true);
            response.setState(HttpMessage.__MSG_SENT);
            
            HttpFields res = new HttpFields();
            res.read(lin);
            res.add("Via","Via: 1.1 host (Jetty/4.x)");
            String resHeader = resLine+"\015\012"+res;
            
            System.err.println("\nres=\n"+resHeader);

            OutputStream out=response.getOutputStream().getRawStream();
            
            out.write(resHeader.getBytes(StringUtil.__ISO_8859_1));
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
