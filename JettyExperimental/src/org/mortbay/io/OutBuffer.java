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
public interface OutBuffer extends Buffer
{
    boolean isClosed();
    
    /**
     * Close any backing stream associated with the buffer
     */
    void close() throws IOException;

    /**
     * Flush the buffer from the current getIndex to it's putIndex using whatever byte
     * sink is backing the buffer. The getIndex is updated with the number of bytes flushed.
     * Any mark set is cleared.
     * If the entire contents of the buffer are flushed, then an implicit empty() is done.
     * 
     * @return  the number of bytes written
     */
    int flush() throws IOException;

    /**
     * Flush the buffer from the current getIndex to it's putIndex using whatever byte
     * sink is backing the buffer. The getIndex is updated with the number of bytes flushed.
     * Any mark set is cleared.
     * If the entire contents of the buffer are flushed, then an implicit empty() is done.
     * The passed header buffer is written before the contents of this buffer. This may be done 
     * either as gather write, as a poke into this buffer or as two writes. The implementation is free to
     * select the optimal mechanism.
     * @param header A buffer to write before flushing this buffer. This buffers getIndex is updated.
     * @return the total number of bytes written.
     */
    int flush(Buffer header) throws IOException;

}