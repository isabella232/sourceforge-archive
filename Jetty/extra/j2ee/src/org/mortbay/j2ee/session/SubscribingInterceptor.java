// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;

import org.jboss.logging.Logger;

//----------------------------------------


// hook SubscribingInterceptor to AbstractReplicatedStore
// lose ReplicatedState

public class SubscribingInterceptor
  extends StateInterceptor
{
  protected static final Logger _log=Logger.getLogger(SubscribingInterceptor.class);

  protected AbstractReplicatedStore
    getStore()
  {
    AbstractReplicatedStore store=null;
    try
    {
      store=(AbstractReplicatedStore)getManager().getStore();
    }
    catch (Exception e)
    {
      _log.error("could not get AbstractReplicatedStore");
    }

    return store;
  }

  //----------------------------------------

  // this Interceptor is stateful - it is the dispatch point for
  // change notifications targeted at the session that it wraps.

  public void
    start()
  {
    try
    {
      getStore().subscribe(getId(), this);
    }
    catch (RemoteException e)
    {
      _log.error("could not get my ID", e);
    }
  }

  public void
    stop()
  {
    try
    {
      getStore().unsubscribe(getId());
    }
    catch (RemoteException e)
    {
      _log.error("could not get my ID", e);
    }
  }
}
