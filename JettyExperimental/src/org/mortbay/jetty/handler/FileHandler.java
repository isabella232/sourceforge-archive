//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.IO;
import org.mortbay.io.View;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.HttpConnection;

/* ------------------------------------------------------------ */
/** FileHandler.
 * @author gregw
 *
 */
public class FileHandler extends AbstractHandler
{
    HashMap _cache = new HashMap();
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public FileHandler()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.EventHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        String uri = request.getRequestURI();
        
        
        Buffer content = (Buffer) _cache.get(uri);
        
        if (content==null)
        {
            File file = new File(".",uri);
            if (file.exists() && !file.isDirectory())
            {
                if (Boolean.getBoolean("FileNIO") &&  HttpConnection.getCurrentConnection().getConnector().getBuffer(1) instanceof NIOBuffer)
                {
                    System.err.println("FileNIO");
                    content=new NIOBuffer(file);
                }
                else
                {
                    System.err.println("FileIO");
                    content = HttpConnection.getCurrentConnection().getConnector().getBuffer((int)file.length());
                    byte buffer[] = new byte[8096];
                    int len=buffer.length;
                    InputStream in =new FileInputStream(file);
                    while (true)
                    {
                        len=in.read(buffer,0,8096);
                        if (len<0 )
                            break;
                        content.put(buffer,0,len);
                    }
                }
                _cache.put(uri, content);
            }
        }
        
        
        if (content==null)
        {
            content = new ByteArrayBuffer("<h1>Hello World: "+uri+"</h1>\n");
            response.setContentType("text/html");
        }
        else if (uri.endsWith(".html"))
            response.setContentType("text/html");
        else if (uri.endsWith(".jpg"))
            response.setContentType("image/jpeg");
        else if (uri.endsWith(".gif"))
            response.setContentType("image/gif");
        else 
            response.setContentType("text/plain");
        
        ServletOutputStream out = response.getOutputStream();
        
        if (out instanceof HttpConnection.Output)
        {
            //TODO - try new _content API ???
            View view = new View(content);
            ((HttpConnection.Output)out).sendContent(view);
        }
        else if (content.array()!=null)
            out.write(content.array(),content.getIndex(),content.length());
        else
        {
            File file = new File(".",uri);
            IO.copy(new FileInputStream(file), out);
        }
    }
}
