// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

// a store provides 3 APIs :

// It's own start/stop methods. These will e.g. start/stop the session GC thread

// State LifeCyle methods - The Store encapsulates the LifeCycle of the State

// Session ID management methods - The session ID is a responsibility attribute of the store...

// Stores manage State, and will have to notify the Session Manager
// when they believe that this has timed-out.

public interface
  Store
{
  // Store LifeCycle
  void start();
  void stop();
  void destroy();	// corresponds to ctor

  // State LifeCycle
  State newState(String id, int maxInactiveInterval) throws Exception;
  State loadState(String id) throws Exception;
  void  storeState(State state) throws Exception;
  void  removeState(State state) throws Exception;

  // ID allocation
  String allocateId() throws Exception;
  void   deallocateId(String id) throws Exception;

  boolean isDistributed();
  void scavenge(int extraTime, int actualMaxInactiveInterval) throws Exception;

  void passivateSession(StateAdaptor sa);

  void setScavengerPeriod(int secs);
  void setScavengerExtraTime(int secs);
  void setActualMaxInactiveInterval(int secs);
}

