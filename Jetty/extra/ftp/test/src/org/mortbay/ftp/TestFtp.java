// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.ftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;

import org.mortbay.util.TestCase;

public class TestFtp
{
    /* ------------------------------------------------------------------ */
    public static void main(String[] args)
    {   
        testCmdReply();
        testCmdReplyStream();
        testFTP();
        
        TestCase.report();
    }
    
    /* ------------------------------------------------------------ */
    static void testCmdReply()
    {
        TestCase t = new TestCase("class CmdReply");

        CmdReply reply= new CmdReply();

        reply.code="100";
        t.check(reply.preliminary(),"100 preliminary");
        t.check(reply.positive(),"100 positive");
        t.check(!reply.transferComplete(),"100 not complete");
        t.check(reply.isType(CmdReply.d1Syntax),"100 is Syntax");

        reply.code="210";
        t.check(!reply.preliminary(),"210 not preliminary");
        t.check(reply.positive(),"210 positive");
        t.check(reply.transferComplete(),"210 complete");
        t.check(reply.isType(CmdReply.d1Information),"210 is Information");
        t.check(!reply.isType(CmdReply.d1Syntax),"210 is notSyntax");

        reply.code="320";
        t.check(!reply.preliminary(),"320 not preliminary");
        t.check(reply.positive(),"320 positive");
        t.check(!reply.transferComplete(),"320 not complete");
        t.check(reply.isType(CmdReply.d1Connections),"320 is Connections");
        t.check(!reply.isType(CmdReply.d1Syntax),"320 is notSyntax");

        reply.code="430";
        t.check(!reply.preliminary(),"430 not preliminary");
        t.check(!reply.positive(),"430 not positive");
        t.check(!reply.transferComplete(),"430 not complete");
        t.check(reply.isType(CmdReply.d1Authentication),"430 is Authentication");
        t.check(!reply.isType(CmdReply.d1Syntax),"430 is notSyntax");

        reply.code="540";
        t.check(!reply.preliminary(),"540 not preliminary");
        t.check(!reply.positive(),"540 not positive");
        t.check(reply.transferComplete(),"540 complete");
        t.check(reply.isType(CmdReply.d1Unspecified),"540 is Unspecified");
        t.check(!reply.isType(CmdReply.d1Syntax),"540 is notSyntax");
        
        reply.code="550";
        t.check(reply.isType(CmdReply.d1FileSystem),"550 is FileSystem");
        t.check(!reply.isType(CmdReply.d1Syntax),"550 is notSyntax");
    }

    
    /* ------------------------------------------------------------------ */
    static void testCmdReplyStream()
    {
        TestCase t = new TestCase("class CmdReplyStream");

        String inputString =
            "rubbish\n"+
            "\n"+
            "000\n"+
            "0000000\n"+
            "000 aaa\n"+
            "111 \n"+
            "222 bbb\r\n"+
            "333-ccc\n"+
            "ccc\n"+
            "333 ccc\n"+
            "444-ddd\n"+
            "444-ddd\n"+
            "444ddd\n"+
            "444\n"+
            "444 ddd\n"+
            "100 prelim\n"+
            "300 intermediate\n"+
            "200 complete\n";

        try{
            byte[] b = inputString.getBytes();
            ByteArrayInputStream bin = new ByteArrayInputStream(b);
            CmdReplyStream in = new CmdReplyStream(bin);
            CmdReply reply;

            try {
                in.readReply();
                t.check(false,"read rubbish 1");
            }
            catch(FtpCmdStreamException e){
                t.check(true,"rejected rubbish 1");
            }
            try {
                in.readReply();
                t.check(false,"read rubbish 2");
            }
            catch(FtpCmdStreamException e){
                t.check(true,"rejected rubbish 2");
            }
            try {
                in.readReply();
                t.check(false,"read rubbish 3");
            }
            catch(FtpCmdStreamException e){
                t.check(true,"rejected rubbish 3");
            }
            try {
                in.readReply();
                t.check(false,"read rubbish 4");
            }
            catch(FtpCmdStreamException e){
                t.check(true,"rejected rubbish 4");
            }
                
            reply=in.readReply();
            t.check(reply!=null,"Skipped rubbish");
            
            t.checkEquals(reply.code,"000","single code");
            t.checkEquals(reply.text,"aaa","single text");
            
            reply=in.readReply();
            t.checkEquals(reply.code,"111","empty reply");
            t.checkEquals(reply.text,"","empty text");
            
            reply=in.readReply();
            t.checkEquals(reply.code,"222","next reply");
            t.checkEquals(reply.text,"bbb","Handle CRLF");
            
            reply=in.readReply();
            t.checkEquals(reply.code,"333","multi code");
            t.checkEquals(reply.text,"ccc\nccc\nccc","multi text");
            
            reply=in.readReply();
            t.checkEquals(reply.code,"444","complex multi code");
            t.checkEquals(reply.text,"ddd\nddd\n444ddd\n444\nddd","complex multi text");
            
            reply=in.waitForCompleteOK();
            t.checkEquals(reply.code,"200","wait for completeOK");

            reply=in.readReply();
            t.checkEquals(reply,null,"End of input");
            
        }
        catch(Exception e){
            t.check(false,e.toString());
        }    
    }
    
    /* -------------------------------------------------------------------- */
    public static void testFTP()
    {
        TestCase test = null;

        try{
            TestServer server = new TestServer(test);

            ///////////////////////////////////////////
            test = server.test = new TestCase("FtpAuthenticate");;

            Ftp ftp = new Ftp(InetAddress.getByName("127.0.0.1"),
                              server.port,
                              "TestUser",
                              "TestPass");
            test.check(server.connection!=null,"Made command connection");

            ///////////////////////////////////////////
            test = server.test = new TestCase("FtpGetFile");
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ftp.startGet("TestFileName",bout);
            test.check(true,"Get started");
            test.check(!ftp.transferComplete(),"Not yet completed");

            ftp.waitUntilTransferComplete();
            test.check(true,"Get completed");
            test.check(ftp.transferComplete(),"Completed");

            test.checkEquals("How Now Brown Cow\n",bout.toString(),
                             "Get file data");

            ///////////////////////////////////////////
            test = server.test = new TestCase("FtpPutFile");
            bout = new ByteArrayOutputStream();
            Writer writeOut = new OutputStreamWriter(bout);
            writeOut.write("How Now Brown Cow\n");
            writeOut.flush();
            ByteArrayInputStream src =
                new ByteArrayInputStream(bout.toByteArray());

            ftp.startPut(src,"TestFileName");
            test.check(true,"Put started");

            Thread.sleep(2000);
            test.check(ftp.transferComplete(),"wait completed");

            ftp.waitUntilTransferComplete();
            test.check(true,"put wait completed");

            ///////////////////////////////////////////
            test = server.test = new TestCase("FtpAbort");
            bout = new ByteArrayOutputStream(256);
            ftp.startGet("TestFileName",bout);
            test.check(true,"Get started");
            ftp.abort();
            test.check(ftp.transferComplete(),"Aborted");

            ftp.setType(Ftp.BINARY);
            ftp.setType(8);
            ftp.setType(Ftp.ASCII,Ftp.CARRIAGE_CONTROL);

        }
        catch(Exception e){
            if (test==null)
                test = new TestCase("Ftp");
            test.check(false,"Exception "+e);
        }
    }
}


