// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;

/* ------------------------------------------------------------ */
/** Observer output events.
 *
 * @see ChunkableOutputStream
 * @version 1.0 Fri Oct  1 1999
 * @author Greg Wilkins (gregw)
 */
public interface OutputObserver
{
    public final static int
        FIRST_WRITE=0,
        RESET_BUFFER=1,
        COMMITING=2,
        COMMITED=3,
        CLOSING=4,
        CLOSED=5;
    void outputNotify(ChunkableOutputStream out, int action);
};
