// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package org.mortbay.util;

import java.rmi.RemoteException;


public class NullLogSink implements LogSink
{
    private boolean started;
    public void setOptions(String options){}
    public String getOptions(){return null;}
    public void log(String tag,Object msg,Frame frame,long time){}
    public void log(String formattedLog){}
    public void start(){started=true;}
    public void stop(){started=false;}
    public boolean isStarted(){return started;}
}
