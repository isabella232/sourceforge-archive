// ========================================================================
// Copyright (c) 1999-2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpTunnel;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.StringMap;
import org.mortbay.util.URI;

/* ------------------------------------------------------------ */
/** Proxy request handler.
 * A HTTP/1.1 Proxy.  This implementation uses the JVMs URL implementation to
 * make proxy requests.
 * <P>The HttpTunnel mechanism is also used to implement the CONNECT method.
 *
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ProxyHandler extends AbstractHttpHandler
{
    protected Set _proxyHostsWhiteList;
    protected Set _proxyHostsBlackList;    
    
    /* ------------------------------------------------------------ */
    /** Map of leg by leg headers (not end to end).
     * Should be a set, but more efficient string map is used instead.
     */
    protected StringMap _DontProxyHeaders = new StringMap();
    {
        Object o = new Object();
        _DontProxyHeaders.setIgnoreCase(true);
        _DontProxyHeaders.put(HttpFields.__ProxyConnection,o);
        _DontProxyHeaders.put(HttpFields.__Connection,o);
        _DontProxyHeaders.put(HttpFields.__KeepAlive,o);
        _DontProxyHeaders.put(HttpFields.__TransferEncoding,o);
        _DontProxyHeaders.put(HttpFields.__TE,o);
        _DontProxyHeaders.put(HttpFields.__Trailer,o);
        _DontProxyHeaders.put(HttpFields.__ProxyAuthorization,o);
        _DontProxyHeaders.put(HttpFields.__ProxyAuthenticate,o);
        _DontProxyHeaders.put(HttpFields.__Upgrade,o);
    }
    
    /* ------------------------------------------------------------ */
    /**  Map of allows schemes to proxy
     * Should be a set, but more efficient string map is used instead.
     */
    protected StringMap _ProxySchemes = new StringMap();
    {
        Object o = new Object();
        _ProxySchemes.setIgnoreCase(true);
        _ProxySchemes.put(HttpMessage.__SCHEME,o);
        _ProxySchemes.put(HttpMessage.__SSL_SCHEME,o);
        _ProxySchemes.put("ftp",o);
    }
    
    /* ------------------------------------------------------------ */
    /** Set of allowed CONNECT ports.
     */
    protected HashSet _allowedConnectPorts = new HashSet();
    {
        _allowedConnectPorts.add(new Integer(80));
        _allowedConnectPorts.add(new Integer(8000));
        _allowedConnectPorts.add(new Integer(8080));
        _allowedConnectPorts.add(new Integer(8888));
        _allowedConnectPorts.add(new Integer(443));
        _allowedConnectPorts.add(new Integer(8443));
    }

    /* ------------------------------------------------------------ */
    /** Get proxy host white list.
     * @return Array of hostnames and IPs that are proxied,
     * or an empty array if all hosts are proxied.
     */
    public String[] getProxyHostsWhiteList()
    {
        if (_proxyHostsWhiteList==null||_proxyHostsWhiteList.size()==0)
            return new String[0];
        
        String [] hosts = new String[_proxyHostsWhiteList.size()];
        hosts=(String[])_proxyHostsWhiteList.toArray(hosts);
        return hosts;
    }
    
    /* ------------------------------------------------------------ */
    /** Set proxy host white list.
     * @param hosts Array of hostnames and IPs that are proxied, 
     * or null if all hosts are proxied.
     */
    public void setProxyHostsWhiteList(String[] hosts)
    {
        if (hosts==null || hosts.length==0)
            _proxyHostsWhiteList=null;
        else
        {
            _proxyHostsWhiteList=new HashSet();
            for (int i=0;i<hosts.length;i++)
                if (hosts[i]!=null && hosts[i].trim().length()>0)
                    _proxyHostsWhiteList.add(hosts[i]);
        }
    }

    /* ------------------------------------------------------------ */
    /** Get proxy host black list.
     * @return Array of hostnames and IPs that are NOT proxied.
     */
    public String[] getProxyHostsBlackList()
    {
        if (_proxyHostsBlackList==null||_proxyHostsBlackList.size()==0)
            return new String[0];
        
        String [] hosts = new String[_proxyHostsBlackList.size()];
        hosts=(String[])_proxyHostsBlackList.toArray(hosts);
        return hosts;
    }
    
    /* ------------------------------------------------------------ */
    /** Set proxy host black list.
     * @param hosts Array of hostnames and IPs that are NOT proxied. 
     */
    public void setProxyHostsBlackList(String[] hosts)
    {
        if (hosts==null || hosts.length==0)
            _proxyHostsBlackList=null;
        else
        {
            _proxyHostsBlackList=new HashSet();
            for (int i=0;i<hosts.length;i++)
                if (hosts[i]!=null && hosts[i].trim().length()>0)
                    _proxyHostsBlackList.add(hosts[i]);
        }
    }

    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        URI uri = request.getURI();
        
        // Is this a CONNECT request?
        if (HttpRequest.__CONNECT.equalsIgnoreCase(request.getMethod()))
        {
            handleConnect(pathInContext,pathParams,request,response);
            return;
        }
        
        try
        {
            // Do we proxy this?
            URL url=isProxied(uri);
            if (url==null)
            {
                if (isForbidden(uri))
                    sendForbid(request,response,uri);
                return;
            }
            
            Code.debug("PROXY URL=",url);

            URLConnection connection = url.openConnection();
            connection.setAllowUserInteraction(false);
            
            // Set method
            HttpURLConnection http = null;
            if (connection instanceof HttpURLConnection)
            {
                http = (HttpURLConnection)connection;
                http.setRequestMethod(request.getMethod());
                http.setInstanceFollowRedirects(false);
            }

            // check connection header
            String connectionHdr = request.getField(HttpFields.__Connection);
            if (connectionHdr!=null &&
                (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive)||
                 connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
                connectionHdr=null;

            // copy headers
            boolean xForwardedFor=false;
            boolean hasContent=false;
            Enumeration enum = request.getFieldNames();
            while (enum.hasMoreElements())
            {
                // XXX could be better than this!
                String hdr=(String)enum.nextElement();

                if (_DontProxyHeaders.containsKey(hdr))
                    continue;
                if (connectionHdr!=null && connectionHdr.indexOf(hdr)>=0)
                    continue;

                if (HttpFields.__ContentType.equals(hdr))
                    hasContent=true;


                Enumeration vals = request.getFieldValues(hdr);
                while (vals.hasMoreElements())
                {
                    String val = (String)vals.nextElement();
                    if (val!=null)
                    {
                        connection.addRequestProperty(hdr,val);
                        xForwardedFor|=HttpFields.__XForwardedFor.equalsIgnoreCase(hdr);
                    }
                }
            }

            // Proxy headers
            connection.setRequestProperty("Via","1.1 (jetty)");
            if (!xForwardedFor)
                connection.addRequestProperty(HttpFields.__XForwardedFor,
                                              request.getRemoteAddr());

            // a little bit of cache control
            String cache_control = request.getField(HttpFields.__CacheControl);
            if (cache_control!=null &&
                (cache_control.indexOf("no-cache")>=0 ||
                 cache_control.indexOf("no-store")>=0))
                connection.setUseCaches(false);

            // customize Connection
            customizeConnection(pathInContext,pathParams,request,connection);
            
            try
            {    
                connection.setDoInput(true);
                
                // do input thang!
                InputStream in=request.getInputStream();
                if (hasContent)
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
            int code=HttpResponse.__500_Internal_Server_Error;
            if (http!=null)
            {
                proxy_in = http.getErrorStream();
                
                code=http.getResponseCode();
                response.setStatus(code);
                response.setReason(http.getResponseMessage());
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
            
            // clear response defaults.
            response.removeField(HttpFields.__Date);
            response.removeField(HttpFields.__Server);
            
            // set response headers
            int h=0;
            String hdr=connection.getHeaderFieldKey(h);
            String val=connection.getHeaderField(h);
            while(hdr!=null || val!=null)
            {
                if (hdr!=null && val!=null && !_DontProxyHeaders.containsKey(hdr))
                    response.addField(hdr,val);
                h++;
                hdr=connection.getHeaderFieldKey(h);
                val=connection.getHeaderField(h);
            }
            response.setField("Via","1.1 (jetty)");

            // Handled
            request.setHandled(true);
            if (proxy_in!=null)
                IO.copy(proxy_in,response.getOutputStream());
            
        }
        catch (Exception e)
        {
            Code.warning(e.toString());
            Code.ignore(e);
            if (!response.isCommitted())
                response.sendError(HttpResponse.__400_Bad_Request);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void handleConnect(String pathInContext,
                              String pathParams,
                              HttpRequest request,
                              HttpResponse response)
        throws HttpException, IOException
    {
        URI uri = request.getURI();
        
        try
        {
            Code.debug("CONNECT: ",uri);
            InetAddrPort addrPort=new InetAddrPort(uri.toString());
            
            Integer port = new Integer(addrPort.getPort());
            String host=addrPort.getHost();
            if (!_allowedConnectPorts.contains(port) ||
                _proxyHostsWhiteList!=null && !_proxyHostsWhiteList.contains(host) ||
                _proxyHostsBlackList!=null && _proxyHostsBlackList.contains(host))
            {
                sendForbid(request,response,uri);
            }
            else
            {
                Socket socket = new Socket(addrPort.getInetAddress(),addrPort.getPort());
                request.getHttpConnection().setHttpTunnel(new HttpTunnel(socket));
                response.setStatus(HttpResponse.__200_OK);
                response.setContentLength(0);
                request.setHandled(true);
            }
        }
        catch (Exception e)
        {
            Code.ignore(e);
            response.sendError(HttpResponse.__500_Internal_Server_Error);
        }
    }
    
        
    /* ------------------------------------------------------------ */
    /** Customize proxy connection.
     * Method to allow derived handlers to customize the connection.
     */
    protected void customizeConnection(String pathInContext,
                                       String pathParams,
                                       HttpRequest request,
                                       URLConnection connection)
        throws IOException
    {
    }
    
    
    /* ------------------------------------------------------------ */
    /** Is URL Proxied.
     * Method to allow derived handlers to select which URIs are proxied and
     * to where.
     * @param uri The requested URI, which should include a scheme, host and port.
     * @return The URL to proxy to, or null if the passed URI should not be proxied.
     * The default implementation returns the passed uri if isForbidden() returns true.
     */
    protected URL isProxied(URI uri)
        throws MalformedURLException
    {
        // Is this a proxy request?
        if (isForbidden(uri))
            return null;
        
        // OK return URI as untransformed URL.
        return new URL(uri.toString());
    }
    

    /* ------------------------------------------------------------ */
    /** Is URL Forbidden.
     * 
     * @return True if the URL is not forbidden: if it has a schema
     * that is in the _ProxySchemes map. If a proxy host black list is set,
     * the URI host must not be in the list. If aproxy host white list is
     * set, then the URI host must be in the list.
     * The port is also checked that it is either 80, 443, >1024 or one of the 
     * allowed CONNECT ports.
     */
    protected boolean isForbidden(URI uri)
    {
        // Is this a proxy request?
        String host=uri.getHost();

        // check port
        int port = uri.getPort();
        if (port>0 && port <=1024 && port!=80 && port!=443)
        {
            Integer p = new Integer(port);
            if (!_allowedConnectPorts.contains(p))
                return true;
        }

        // Must be a scheme that can be proxied.
        String scheme=uri.getScheme();
        if (scheme==null || !_ProxySchemes.containsKey(scheme))
            return true;

        // Must be in any defined white list
        if (_proxyHostsWhiteList!=null && !_proxyHostsWhiteList.contains(host))
            return true;

        // Must not be in any defined black list
        if (_proxyHostsBlackList!=null && _proxyHostsBlackList.contains(host))
            return true;

        return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Send Forbidden.
     * Method called to send forbidden response. Default implementation calls
     * sendError(403)
     */
    protected void sendForbid(HttpRequest request, HttpResponse response, URI uri)
        throws IOException
    {
        response.sendError(HttpResponse.__403_Forbidden,"Forbidden for Proxy");
    }
}
