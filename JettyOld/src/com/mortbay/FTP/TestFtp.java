// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.FTP;

import com.mortbay.Base.*;
import java.io.*;

public class TestFtp
{
    /* ------------------------------------------------------------------ */
    public static void main(String[] args)
    {	
	CmdReply.test();
	CmdReplyStream.test();
	Ftp.test();
	
	Test.report();
    }

	
}


