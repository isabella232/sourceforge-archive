/* ==============================================
* Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
* Distributed under the artistic license.
* Created on 17-Apr-2003
* $Id$
* ============================================== */

package org.mortbay.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Temporary Servler class to get things running.
 */

public class HttpServer
{
    public static void main(String[] args)
        throws Exception
    {   
        ServerSocket ss = new ServerSocket(8080);
        System.out.println("listening on "+ss);
        while(true)
        {
            final Socket socket=ss.accept();
        
            try
            {
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            HttpConnection connection=
                            new HttpConnection(socket);
                            connection.run();
                        }
                        catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                    }   
                }).start();  
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }   
}
