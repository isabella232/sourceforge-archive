// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;
import com.mortbay.Base.*;

/* ------------------------------------------------------------ */
/** Set User ID
 *
 * This Unix only class, calls a native method to set the user ID
 *
 * Used to change to a non priviledged user after listening on
 * priviledged IP ports.
 *
 * @see Server
 * @version 1.0 Mon Sep 27 1999
 * @author Greg Wilkins (gregw)
 */
public class SetUID
{
    static
    {
        System.loadLibrary("SetUID");
    }
    private static native void doSetUID(int uid);
    
    /* ------------------------------------------------------------ */
    /** 
     * @param uid 
     */
    public static void setUID(int uid)
    {
        try
        {    
            Log.event("Set User ID = "+uid);
            doSetUID(uid);
        }
        catch(Exception e)
        {
            Code.fail(e);
        }        
    }
};
