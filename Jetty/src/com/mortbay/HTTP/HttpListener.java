// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** XXX
 *
 * @see HttpConnection
 * @version 1.0 Sat Oct  2 1999
 * @author Greg Wilkins (gregw)
 */
public interface HttpListener 
{
    public abstract HttpServer getServer();
    public abstract String getDefaultProtocol();
    public abstract String getHost();
    public abstract int getPort();
};










