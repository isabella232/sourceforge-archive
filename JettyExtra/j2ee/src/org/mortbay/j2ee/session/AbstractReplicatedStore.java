// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Category;
import org.javagroups.util.Util;
import org.javagroups.Message;
import org.javagroups.blocks.MessageDispatcher;

//----------------------------------------

// implement scavenging
// implement setMaxInactiveInterval
// look for NYI/TODO

// this infrastructure could probably be used across JMS aswell -
// think about it...

/**
 * Maintain synchronisation with other States representing the same
 * session by publishing changes made to ourself and updating ourself
 * according to notifications published by the other State objects.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */

abstract public class
  AbstractReplicatedStore
  implements Store
{
  protected final Category _log=Category.getInstance(getClass().getName());
  protected Map            _sessions=new HashMap();
  protected GUIDGenerator  _guidGenerator=new GUIDGenerator();
  protected Manager        _manager;
  protected int            _actualMaxInactiveInterval;

  //----------------------------------------
  // Store API - Store LifeCycle

  public
    AbstractReplicatedStore(Manager manager)
    {
      _manager=manager;
    }

  public void
    start()
    throws Exception
    {
      // nothing to do
    }

  public void
    stop()
    {
      // nothing to do
    }

  public void
    destroy()			// corresponds to ctor
    {
      _sessions.clear();
      _sessions=null;

      _guidGenerator=null;

      _manager=null;
    }

  //----------------------------------------
  // Store API - State LifeCycle

  public State
    newState(String id, int maxInactiveInterval)
    throws Exception
    {
      long creationTime=System.currentTimeMillis();
      Class[]  argClasses   = {String.class, String.class, Long.class, Integer.class};
      Object[] argInstances = {getContextPath(), id, new Long(creationTime), new Integer(maxInactiveInterval)};
      publish("create", argClasses, argInstances);

      // if we get one - all we have to do is loadState - because we
      // will have just created it...

      return loadState(id);
    }

  public State
    loadState(String id)
    {
      // pull it out of our cache - if it is not there, it doesn't
      // exist/hasn't been distributed...

      synchronized (_sessions)
      {
	return (State) _sessions.get(id);
      }
    }

  public void
    storeState(State state)
    {
      // no need - it has already been distributed...
    }

  public void
    removeState(State state)
    throws Exception
    {
      Class[]  argClasses   = {String.class, String.class};
      Object[] argInstances = {getContextPath(), state.getId()};
      publish("destroy", argClasses, argInstances);
    }

  //----------------------------------------
  // Store API - GUID management

  public String
    allocateId()
  {
    return _guidGenerator.generateSessionId();
  }

  public void
    deallocateId(String id)
  {
  }

  //----------------------------------------
  // Store API - garbage collection - NYI/TODO

  public void scavenge(int extraTime, int actualMaxInactiveInterval) throws Exception {}
  public void setScavengerPeriod(int secs) {}
  public void setScavengerExtraTime(int secs) {}

  public void
    setActualMaxInactiveInterval(int secs)
    {
      _actualMaxInactiveInterval=secs;
    }

  //----------------------------------------
  // Store API - hacks... - NYI/TODO

  public void passivateSession(StateAdaptor sa) {}
  public boolean isDistributed() {return true;}

  //----------------------------------------
  // utils

  public String
    getContextPath()
    {
      return _manager.getContextPath();
    }

  //----------------------------------------
  // change notification API

  abstract protected void publish(String methodName, Class[] argClasses, Object[] argInstances);

  protected void
    dispatch(String methodName, Class[] argClasses, Object[] argInstances)
    {
      // only stuff meant for our context will be dispatched to us

      String tmp="(";
      for (int i=2; i<argInstances.length; i++)
	tmp=tmp+argInstances[i]+((i<argInstances.length-1)?", ":"");
      tmp=tmp+")";

      _log.info("dispatching call: "+methodName+tmp);

      String id=(String)argInstances[1];

      // either this is a class method
      if (methodName.equals("create"))
      {
	long creationTime=((Long)argInstances[2]).longValue();
	int maxInactiveInterval=((Integer)argInstances[3]).intValue();

	State state=new ReplicatedState(this, id, creationTime, maxInactiveInterval);

	synchronized(_sessions)
	{
	  _sessions.put(id, state);
	}
      }
      else if (methodName.equals("destroy"))
      {
	synchronized(_sessions)
	{
	  _sessions.remove(id);
	}
      }
      else
      {
	// or an instance method..
	((ReplicatedState)_sessions.get(id)).dispatch(methodName, argClasses, argInstances);
      }
    }
}
