// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.io.IOException;

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
        __FIRST_WRITE=0,
        __RESET_BUFFER=1,
        __COMMITING=2,
        __COMMITED=3,
        __CLOSING=4,
        __CLOSED=5;
    
    /* ------------------------------------------------------------ */
    /** XXX 
     * @param out 
     * @param action 
     */
    void outputNotify(ChunkableOutputStream out, int action)
        throws IOException;
}
