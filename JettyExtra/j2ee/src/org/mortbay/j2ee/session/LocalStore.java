// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Category;

//----------------------------------------

public class LocalStore
  implements Store
{
  Category _log=Category.getInstance(getClass().getName());
  Map _sessions=new HashMap();

  public
    LocalStore(Manager manager)
  {
  }

  // Store LifeCycle
  public void
    start()
  {
  }

  public void
    stop()
  {
  }

  public void
    destroy()
  {
  }

  // State LifeCycle
  public State
    newState(String id, int maxInactiveInterval)
  {
    return new LocalState(id, maxInactiveInterval);
  }

  public State
    loadState(String id)
  {
    synchronized (_sessions)
    {
      return (State)_sessions.get(id);
    }
  }

  public void
    storeState(State state)
  {
    try
    {
      synchronized (_sessions)
      {
	_sessions.put(state.getId(), state);
      }
    }
    catch (Exception e)
    {
      _log.warn("could not store session");
    }
  }

  public void
    removeState(State state)
  {
    try
    {
      synchronized (_sessions)
      {
	_sessions.remove(state.getId());
      }
    }
    catch (Exception e)
    {
      _log.error("could not remove session", e);
    }
  }

  protected GUIDGenerator _guidGenerator=new GUIDGenerator();

  public String
    allocateId()
  {
    return _guidGenerator.generateSessionId();
  }

  public void
    deallocateId(String id)
  {
  }

  public boolean
    isDistributed()
  {
    return false;
  }

  public void
    scavenge(int extraTime, int actualMaxInactiveInterval)
  {
    // Java's GC will do it for us !
    // NYI - TODO
  }

  public void
    passivateSession(StateAdaptor sa)
  {
    // we don't do that !
    sa.invalidate();
  }

  // TODO
  public void setScavengerPeriod(int secs) {}
  public void setScavengerExtraTime(int secs) {}
  public void setActualMaxInactiveInterval(int secs) {}
}
