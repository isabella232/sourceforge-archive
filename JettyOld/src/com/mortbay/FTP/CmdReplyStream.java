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
/** FTP Command Reply Stream
 * <p> 
 * <p><h4>Notes</h4>
 * <p> notes...
 *
 * <p><h4>Usage</h4>
 * <pre>
 * example();
 * </pre>
 *
 * @see 
 * @version $Id$
 * @author Greg Wilkins
*/
class CmdReplyStream
{
    /* ------------------------------------------------------------ */
    /** The actual input stream.
     */
    private LineInput in;

    /* ------------------------------------------------------------ */
    /** Constructor
     */
    CmdReplyStream(InputStream in)
    {
        this.in = new LineInput(in);
    }
    
    /* ------------------------------------------------------------ */
    /** Read command reply
     * Block until can read a single line or complete multi line
     * command from the command input stream.
     * @return The CmdReply received or null if the port has closed
     */
    CmdReply readReply()
         throws FtpException,IOException
    {
        String line;
        CmdReply reply = new CmdReply();
        boolean multiLine=false;
        
        while ((line=in.readLine())!=null)
        {
            if (multiLine)
            {
                if (line.length()>3 && line.startsWith(reply.code))
                {
                    
                    if (line.charAt(3)==' ')
                    {
                        reply.text += "\n" + line.substring(4);
                        Code.debug("Reply="+reply);
                        return reply;
                    }
                    else if (line.charAt(3)=='-')
                        reply.text += "\n" + line.substring(4);
                    else
                        reply.text += "\n" + line;
                }
                else
                {
                    reply.text += "\n" + line;
                }
            }
            else
            {
                if (line.length()>3 &&
                    line.charAt(0)>='0' && line.charAt(0)<='9' &&
                    line.charAt(1)>='0' && line.charAt(1)<='9' &&
                    line.charAt(2)>='0' && line.charAt(2)<='9')
                {
                    reply.code=line.substring(0,3);
                    reply.text=line.substring(4);
                    if (line.charAt(3)==' ')
                    {
                        Code.debug("Reply="+reply);
                        return reply;
                    }
                    if (line.charAt(3)=='-')
                        multiLine=true;
                    else
                        throw new FtpCmdStreamException
                            ("Bad code separator",line);
                }
                else
                    throw new FtpCmdStreamException
                        ("Bad reply format",line);
            }
        }

        return null;
    }

    /* ------------------------------------------------------------------ */
    CmdReply waitForReply(String code)
         throws FtpException, IOException
    {
        CmdReply reply = readReply();
        if (reply==null )
            throw new FtpCmdStreamException();

        if (!reply.code.equals(code))
            throw new FtpReplyException(reply);
        return reply;
    }
    
    /* ------------------------------------------------------------------ */
    CmdReply waitForPreliminaryOK()
         throws FtpException, IOException
    {
        CmdReply reply = readReply();
        if (reply==null)
            throw new FtpCmdStreamException();

        if (!reply.preliminary() || !reply.positive())
            throw new FtpReplyException(reply);
        return reply;
    }
    
    /* ------------------------------------------------------------------ */
    CmdReply waitForCompleteOK()
         throws FtpException, IOException
    {
        CmdReply reply;
        
        while ((reply=readReply())!=null)
        {
            if (!reply.positive())
                throw new FtpReplyException(reply);
            if (reply.transferComplete())
                break;
        }
        if (reply==null)
            throw new FtpCmdStreamException();

        return reply;
    }
    
    /* ------------------------------------------------------------------ */
    CmdReply waitForIntermediateOK()
         throws FtpException, IOException
    {
        CmdReply reply = readReply();
        if (reply==null )
            throw new FtpCmdStreamException();

        if (reply.transferComplete() ||
            reply.preliminary() ||
            !reply.positive())
            throw new FtpReplyException(reply);
        return reply;
    }
    
    
    /* ------------------------------------------------------------------ */
    /** Test Harness
     */
    static void test()
    {
        Test t = new Test("class CmdReplyStream");

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
};




