// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;/* ------------------------------------------------------------ */
/** A component LifeCycle.
 * Represents the life cycle interface for an abstract
 * software component. Implementations should respect
 * the following state table:<PRE>
 * State: Destroyed (initial state)
 *    start()      -> Started
 *    stop()       -> Stopped
 *    destroy()    -> Destroyed
 *
 * State: Stopped
 *    start()      -> Started
 *    stop()       -> Stopped
 *    destroy()    -> Destroyed
 *
 * State: Started
 *    start()      -> Started
 *    stop()       -> Stopped
 *    destroy()    -> Destroyed
 * </PRE>
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface LifeCycle
{
    /* ------------------------------------------------------------ */
    /** Start the LifeCycle.
     */
    public void start()
        throws Exception;
    
    /* ------------------------------------------------------------ */
    /** Stop the LifeCycle.
     * The LifeCycle may wait for current activities to complete
     * normally, but it can be interrupted.
     */
    public void stop()
        throws InterruptedException;
    
    /* ------------------------------------------------------------ */
    /** Destroy the LifeCycle.
     * Activities are terminated.
     */
    public void destroy();

    /* ------------------------------------------------------------ */
    /** 
     * @return True if the LifeCycle has been started. 
     */
    public boolean isStarted();
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the LifeCycle has been destroyed. 
     */
    public boolean isDestroyed();
    
}

