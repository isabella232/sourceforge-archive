// ========================================================================
// $Id$
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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
