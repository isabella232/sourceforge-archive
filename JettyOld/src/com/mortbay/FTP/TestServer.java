// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.FTP;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import java.util.*;


// ===========================================================================
class TestServer extends Thread
{
    Test test;
    ServerSocket listen=null;
    int port = -1;
    Socket connection;
    LineInput in;
    Writer out;

    TestServer(Test t)
    {
        this.test=t;
        start();

        synchronized(this){
            while (port==-1)
                try{wait(1000);}catch(InterruptedException e){};
        }
        
    }
    
    public void run()
    {
        try{
            listen = new ServerSocket(0);
            synchronized(this){
                port = listen.getLocalPort();
                notifyAll();
            }
            Code.debug("Test server listening");
            connection = listen.accept( );
            Code.debug("Test server connected");
            in = new LineInput(connection.getInputStream());
            out = new OutputStreamWriter(connection.getOutputStream(),"UTF8");
            out.write(CmdReply.codeServiceReady+" OK\n");
            out.flush();

            // Handle authentication
            String line = in.readLine();
            Code.debug("Test server got: "+line);
            test.checkEquals(line,"USER TestUser","Received USER");
            out.write(CmdReply.codeUsernameNeedsPassword+" Need password\n");
            out.flush();
            
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.checkEquals(line,"PASS TestPass","Received PASS");
            out.write(CmdReply.codeUserLoggedIn+" OK\n");
            out.flush();

            //Handler get file
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.check(line.startsWith("PORT"),"Received PORT");
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();

            int c = line.lastIndexOf(',');
            int dataPort = Integer.parseInt(line.substring(c+1));
            line = line.substring(0,c);
            dataPort += 256 *
                Integer.parseInt(line.substring(line.lastIndexOf(',')+1));
            Socket dataConnection = new Socket(InetAddress.getLocalHost(),
                                               dataPort);
            test.check(true,"DataPort Opened");
            
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.checkEquals(line,"RETR TestFileName","Received RETR");
            out.write(CmdReply.codeFileStatusOK+" Data port opened\n");
            out.flush();

            Writer dataOut = new
                OutputStreamWriter(dataConnection.getOutputStream(),"UTF8");

            Thread.sleep(1000);
            dataOut.write("How Now Brown Cow\n");
            dataOut.flush();
            dataOut.close();
            dataConnection.close();
            out.write(CmdReply.codeClosingData+" File transfer complete\n");
            out.flush();

            //Handler put file
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.check(line.startsWith("PORT"),"Received PORT");
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();

            c = line.lastIndexOf(',');
            dataPort = Integer.parseInt(line.substring(c+1));
            line = line.substring(0,c);
            dataPort += 256 *
                Integer.parseInt(line.substring(line.lastIndexOf(',')+1));
            dataConnection = new Socket(InetAddress.getLocalHost(),
                                               dataPort);
            test.check(true,"DataPort Opened");
            
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.checkEquals(line,"STOR TestFileName","Received STOR");
            out.write(CmdReply.codeFileStatusOK+" Data port opened\n");
            out.flush();

            LineInput dataIn = new
                LineInput(dataConnection.getInputStream());
            String input = dataIn.readLine();
            test.checkEquals(input,"How Now Brown Cow","received file");
            input = dataIn.readLine();
            test.checkEquals(input,null,"received EOF");

            out.write(CmdReply.codeClosingData+" File transfer complete\n");
            out.flush();

            //Handler abort file
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.check(line.startsWith("PORT"),"Received PORT");
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();

            c = line.lastIndexOf(',');
            dataPort = Integer.parseInt(line.substring(c+1));
            line = line.substring(0,c);
            dataPort += 256 *
                Integer.parseInt(line.substring(line.lastIndexOf(',')+1));
            dataConnection = new Socket(InetAddress.getLocalHost(),
                                               dataPort);
            test.check(true,"DataPort Opened");
            
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.checkEquals(line,"RETR TestFileName","Received RETR");
            out.write(CmdReply.codeFileStatusOK+" Data port opened\n");
            out.flush();

            dataOut = new OutputStreamWriter(dataConnection.getOutputStream(),"UTF8");
            dataOut.write("How Now Brown Cow\n");
            dataOut.flush();
            line = in.readLine();
            Code.debug("Test server got: "+line);
            test.check(line.startsWith("ABOR"),"Received ABOR");

            dataOut.close();
            dataConnection.close();
            out.write(CmdReply.codeClosingData+" File transfer aborted\n");
            out.flush();
            
            line = in.readLine();
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();
            Code.debug("Test server got: "+line);
            test.checkEquals("TYPE I",line,"Received TYPE I");
            
            line = in.readLine();
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();
            Code.debug("Test server got: "+line);
            test.checkEquals("TYPE L 8",line,"Received TYPE L 8");
            
            line = in.readLine();
            out.write(CmdReply.codeCommandOK+" OK\n");
            out.flush();
            Code.debug("Test server got: "+line);
            test.checkEquals("TYPE A C",line,"Received TYPE A C");
    
            Code.debug("Tests completed");
        }
        catch (Exception e){
            test.check(false,"Server failed: "+e);
            Test.report();
            System.exit(1);
        }       
    }
};

