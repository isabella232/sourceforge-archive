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
public class HttpServer implements Runnable
{
    int port= 8080;

    HttpServer()
    {
    }

    /**
     * @return
     */
    public int getPort()
    {
        return port;
    }

    /**
     * @param p
     */
    public void setPort(int p)
    {
        port= p;
    }

    public void run() 
    {
        ServerSocket ss=null;

        try
        {
            ss= new ServerSocket(port);

            System.out.println("listening on " + ss);
            while (true)
            {
                final Socket socket= ss.accept();

                try
                {
                    // TODO poor substitute for a thread pool
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                System.out.println("Open connection: " + socket);
                                HttpConnection connection= new HttpConnection(socket);
                                connection.run();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                System.out.println("Close connection: " + socket);
                                try
                                {
                                    socket.close();
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.println("Close Connection: "+ss);
            try{ss.close();}
            catch(IOException e){e.printStackTrace();}
        }
    }

    public static void main(String[] args) throws Exception
    {
        new HttpServer().run();
    }

}
