// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Enumeration;
import org.mortbay.http.HttpOutputStream;
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
import org.mortbay.util.StringMap;

/* ------------------------------------------------------------ */
/** Proxy request handler.
 * Skeleton of a HTTP/1.1 Proxy
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ProxyHandler extends AbstractHttpHandler
{
    private static final StringMap __DontProxyHeaders = new StringMap();
    static
    {
        Object o = new Object();
        __DontProxyHeaders.setIgnoreCase(true);
        __DontProxyHeaders.put("Proxy-Connection",o);
        __DontProxyHeaders.put(HttpFields.__Connection,o);
        __DontProxyHeaders.put(HttpFields.__KeepAlive,o);
        __DontProxyHeaders.put(HttpFields.__TransferEncoding,o);
        __DontProxyHeaders.put(HttpFields.__TE,o);
        __DontProxyHeaders.put(HttpFields.__Trailer,o);
        __DontProxyHeaders.put(HttpFields.__ProxyAuthorization,o);
        __DontProxyHeaders.put(HttpFields.__ProxyAuthenticate,o);
        __DontProxyHeaders.put(HttpFields.__Upgrade,o);
    }
    
    private static final StringMap __ProxySchemes = new StringMap();
    static
    {
        Object o = new Object();
        __ProxySchemes.setIgnoreCase(true);
        __ProxySchemes.put(HttpMessage.__SCHEME,o);
        __ProxySchemes.put(HttpMessage.__SSL_SCHEME,o);
        __ProxySchemes.put("ftp",o);
    }
    
    /* ------------------------------------------------------------ */
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        // Is this a proxy request?
        
        URI uri = request.getURI();
        String scheme=uri.getScheme();
        if (scheme==null || !__ProxySchemes.containsKey(scheme))
            return;

        // Do we proxy this?
        if (!isProxied(uri))
            return;
        
        try
        {
            URL url = new URL(uri.toString());
            Code.debug("PROXY URL=",url);

            URLConnection connection = url.openConnection();

            // Set method
            HttpURLConnection http = null;
            if (connection instanceof HttpURLConnection)
            {
                http = (HttpURLConnection)connection;
                http.setRequestMethod(request.getMethod());
            }

            // check connection header
            connection.setRequestProperty("Via","1.1 (jetty)");
            String connectionHdr = request.getField(HttpFields.__Connection);
            if (connectionHdr!=null &&
                (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
                 connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
                connectionHdr=null;

            // copy headers
            Enumeration enum = request.getFieldNames();
            while (enum.hasMoreElements())
            {
                // XXX could be better than this!
                String hdr=(String)enum.nextElement();
                if (__DontProxyHeaders.containsKey(hdr))
                    continue;
                if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0)
                    continue;
                
                String val=request.getField(hdr);
                connection.setRequestProperty(hdr,val);
            }           

            // a little bit of cache control
            String cache_control = request.getField(HttpFields.__CacheControl);
            if (cache_control!=null &&
                (cache_control.indexOf("no-cache")>=0 ||
                 cache_control.indexOf("no-store")>=0))
                connection.setUseCaches(false);
            
            try
            {    
                connection.setDoInput(true);
                
                // do input thang!
                InputStream in=request.getInputStream();
                if (in.available()>0) // XXX need better tests than this
                {
                    connection.setDoOutput(true);
                    IO.copy(in,connection.getOutputStream());
                }
                
                // Connect
                connection.connect();    
            }
            catch (Exception e)
            {
                Code.ignore(e);
            }
            
            InputStream proxy_in = null;

            // handler status codes etc.
            if (http!=null)
            {
                proxy_in = http.getErrorStream();
                int code=500;
                
                code=http.getResponseCode();
                System.err.println("code="+code);
                response.setStatus(code);
                response.setReason(http.getResponseMessage());
                System.err.println("reason");
            }
            
            if (proxy_in==null)
            {
                try {proxy_in=connection.getInputStream();}
                catch (Exception e)
                {
                    Code.ignore(e);
                    proxy_in = http.getErrorStream();
                }
            }
            System.err.println("in");
            
            // set response headers
            int h=0;
            String hdr=connection.getHeaderFieldKey(h);
            String val=connection.getHeaderField(h);
            
            while(hdr!=null || val!=null)
            {
                if (hdr!=null && val!=null && !__DontProxyHeaders.containsKey(hdr))
                    response.setField(hdr,val);
                h++;
                hdr=connection.getHeaderFieldKey(h);
                val=connection.getHeaderField(h);
            }
            response.setField("Via","1.1 (jetty)");
            
            request.setHandled(true);
            if (proxy_in!=null)
                IO.copy(proxy_in,response.getOutputStream());
            
        }
        catch (Exception e)
        {
            Code.warning("??? ",e);
        }
        
    }
    
    protected boolean isProxied(URI uri)
    {
        return true;
    }    
}
