// ===========================================================================
// Copyright (c) 1997 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.HTML;
import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import java.util.*;
import java.net.*;
import java.io.*;

/* -------------------------------------------------------------------- */
/** Embed URL HTML Element
 * @see class Element
 * @version $Id$
 * @author Greg Wilkins
*/
public class EmbedUrl extends  Element
{
    /* ----------------------------------------------------------------- */
    private URL url = null;
    private InetAddrPort proxy = null;
    private Socket socket=null;
    HttpRequest request=null;
    HttpHeader replyHeader=null;
    HttpInputStream replyStream=null;

    /* ----------------------------------------------------------------- */
    public EmbedUrl(URL url)
    {
        this.url=url;
    }

    /* ----------------------------------------------------------------- */
    public EmbedUrl(URL url,
                    InetAddrPort proxy)
    {
        this.url=url;
        this.proxy=proxy;
    }

    /* ------------------------------------------------------------ */
    /*
     * @return content encoding.
     * @exception IOException
     */
    private String skipHeader()
         throws IOException
    {
        Code.debug("Embed "+url);
        Socket socket=null;
        HttpRequest request=null;

        if (proxy==null)
        {
            int port = url.getPort();
            if (port==-1)
                port=80;
            socket= new Socket(url.getHost(),port);
        }
        else
        {
            socket= new Socket(proxy.getInetAddress(),
                               proxy.getPort());
        }

        request=new HttpRequest(null,HttpRequest.GET,url.getFile());

        request.write(socket.getOutputStream());
        Code.debug("waiting for forward reply...");

        replyHeader = new HttpHeader();
        replyStream = new HttpInputStream(socket.getInputStream());
        String replyLine=replyStream.readLine();
        Code.debug("got "+replyLine);
        replyHeader.read(replyStream);

        String s = replyHeader.getHeader(HttpHeader.ContentType);
        try {
            int i1 = s.indexOf("charset=",s.indexOf(';')) + 8;
            int i2 = s.indexOf(' ',i1);
            return (0 < i2) ? s.substring(i1,i2) : s.substring(i1);
        }
        catch (Exception e)
        {
            return "ISO8859_1";
        }
    }

    /* ----------------------------------------------------------------- */
    public void write(OutputStream out)
         throws IOException
    {
        try
        {
            skipHeader();
            IO.copy(replyStream,out);
            out.flush();
        }
        finally
        {
            if (socket!=null)
                socket.close();
            if (replyStream!=null)
                replyStream.close();
            if (replyHeader!=null)
                replyHeader.destroy();
            if (request!=null)
                request.destroy();

            socket=null;
            replyStream=null;
            replyHeader=null;
            request=null;
        }
    }

    /* ----------------------------------------------------------------- */
    public void write(Writer out)
         throws IOException
    {
        try
        {
            String encoding=skipHeader();
            try{
                IO.copy(new InputStreamReader(replyStream,encoding),out);
            }
            catch(UnsupportedEncodingException e)
            {
                IO.copy(new InputStreamReader(replyStream,"ISO8859_1"),out);
            }
            out.flush();
        }
        finally
        {
            if (socket!=null)
                socket.close();
            if (replyStream!=null)
                replyStream.close();
            if (replyHeader!=null)
                replyHeader.destroy();
            if (request!=null)
                request.destroy();

            socket=null;
            replyStream=null;
            replyHeader=null;
            request=null;
        }
    }
}





