// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import java.util.Observable;

/* ======================================================================== */
/** Helpful extension to Observable.
 * NotifyObservers will set a changed first.
 */
public class Observed  extends Observable
{
    public void notifyObservers(Object arg)
    {
        setChanged();
        super.notifyObservers(arg);
    }

    public void notifyObservers()
    {
        setChanged();
        super.notifyObservers(null);
    }
}
