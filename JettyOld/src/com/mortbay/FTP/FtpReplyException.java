// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.FTP;
import com.mortbay.Base.*;

public class FtpReplyException extends FtpException
{
    /* ------------------------------------------------------------------ */
    public CmdReply reply = null;
    
    /* ------------------------------------------------------------------ */
    FtpReplyException(CmdReply reply)
    {
	super(replyDescription(reply));
	this.reply = reply;
    }

    /* ------------------------------------------------------------------ */
    static String replyDescription(CmdReply reply)
    {
	Code.assert(reply!=null,"Use FtpCmdStreamException for null reply");
	return reply.toString();
    }
}
