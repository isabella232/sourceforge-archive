// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.LifeCycle;
import java.net.UnknownHostException;
import java.net.ServerSocket;


/* ------------------------------------------------------------ */
/** HTTP Listener.
 *
 * @see HttpConnection
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface HttpListener extends LifeCycle
{
    public abstract void setHttpServer(HttpServer server);
    public abstract HttpServer getHttpServer();
    public abstract void setHost(String host)
        throws UnknownHostException;
    public abstract String getHost();
    public abstract void setPort(int port);
    public abstract int getPort();

    public abstract void customizeRequest(HttpConnection connection,
                                          HttpRequest request);
    public abstract String getDefaultScheme();
    public abstract ServerSocket getServerSocket();
}











