// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.ftp;

public class FtpCmdStreamException extends FtpException
{
    /* ------------------------------------------------------------------ */
    public String input=null;
    
    
    /* ------------------------------------------------------------------ */
    FtpCmdStreamException()
    {
        super("Unexpected close of FTP command channel");
    }
    
    /* ------------------------------------------------------------------ */
    FtpCmdStreamException(String message, String input)
    {
        super(message);
        this.input=input;
    }
}
