// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.ftp;

import org.mortbay.util.Code;
import org.mortbay.util.LineInput;
import java.io.InputStream;
import java.io.IOException;

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
    
    
}



