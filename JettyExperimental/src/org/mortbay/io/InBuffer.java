/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.io;

import java.io.IOException;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public interface InBuffer extends Buffer
{
    boolean isClosed();
    
    /**
     * Close any backing stream associated with the buffer
     */
    void close() throws IOException;

    /**
     * Fill the buffer from the current putIndex to it's capacity from whatever 
     * byte source is backing the buffer. The putIndex is increased if bytes filled.
     * The buffer may chose to do a compact before filling.
     * @return an <code>int</code> value indicating the number of bytes 
     * filled or -1 if EOF is reached.
     */
    int fill() throws IOException;

}