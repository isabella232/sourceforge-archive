// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

public class LocalStore
  implements Store
{

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
    // if it's not in the Container's cache - it's nowhere else !
    return null;
  }

  public void
    storeState(State state)
  {
    // we don't store state - it's all in-VM and that's it.
  }

  public void
    removeState(State state)
  {
    // There is no distributed store to remove it from...
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
  }

  public void
    passivateSession(StateAdaptor sa)
  {
    // we don't do that !
    sa.invalidate();
  }

  // none of these need do anything - since there is no scavenger...
  public void setScavengerPeriod(int secs) {}
  public void setScavengerExtraTime(int secs) {}
  public void setActualMaxInactiveInterval(int secs) {}

}
