// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import org.apache.log4j.Category;
import org.javagroups.Message;
import org.javagroups.blocks.MessageDispatcher;
import org.javagroups.util.Util;

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
  extends AbstractStore
{
  //----------------------------------------
  // tmp hack to prevent infinite loop
  private final static ThreadLocal _replicating=new ThreadLocal();
  public static boolean     getReplicating()                    {return _replicating.get()==Boolean.TRUE;}
  public static void        setReplicating(boolean replicating) {_replicating.set(replicating?Boolean.TRUE:Boolean.FALSE);}
  //----------------------------------------

  public Object
    clone()
    {
      return super.clone();
    }

  protected Map     _sessions=new HashMap();

  //----------------------------------------
  // Store API - Store LifeCycle

  public void
    destroy()			// corresponds to ctor
    {
      _sessions.clear();
      _sessions=null;
      setManager(null);
      super.destroy();
    }

  //----------------------------------------
  // Store API - State LifeCycle

  public State
    newState(String id, int maxInactiveInterval)
    throws Exception
    {
      long creationTime=System.currentTimeMillis();
      Class[]  argClasses   = {Long.class, Integer.class, Integer.class};
      Object[] argInstances = {new Long(creationTime), new Integer(maxInactiveInterval), new Integer(_actualMaxInactiveInterval)};

      if (!AbstractReplicatedStore.getReplicating())
	publish(id, "create", argClasses, argInstances);

      // if we get one - all we have to do is loadState - because we
      // will have just created it...

      dispatch(id, "create", argClasses, argInstances);
      return loadState(id);
    }

  public State
    loadState(String id)
    {
      // pull it out of our cache - if it is not there, it doesn't
      // exist/hasn't been distributed...

      Object tmp;
      synchronized (_sessions) {tmp=_sessions.get(id);}
      return (State)tmp;
    }

  public void
    storeState(State state)
    {
      try
      {
	String id=state.getId();
	synchronized (_sessions){_sessions.put(id, state);}
      }
      catch (Exception e)
      {
	_log.error("error storing session", e);
      }
    }

  public void
    removeState(State state)
    throws Exception
    {
      String id=state.getId();

      Class[]  argClasses   = {};
      Object[] argInstances = {};

      if (!AbstractReplicatedStore.getReplicating())
	publish(id, "destroy", argClasses, argInstances);

      dispatch(id, "destroy", argClasses, argInstances);
      synchronized (_sessions){_sessions.remove(id);}
    }

  //----------------------------------------
  // Store API - garbage collection

  public void
    scavenge()
    throws Exception
    {
      _log.info("distributed scavenging...");
      synchronized (_sessions)
      {
	for (Iterator i=_sessions.entrySet().iterator(); i.hasNext();)
 	  if (!((ReplicatedState)((Map.Entry)i.next()).getValue()).isValid(_scavengerExtraTime))
	  {
	    _log.info("scavenging state");
	    i.remove();
	  }
      }
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
      return getManager().getContextPath();
    }

  //----------------------------------------
  // change notification API

  abstract protected void publish(String id, String methodName, Class[] argClasses, Object[] argInstances);

  protected void
    dispatch(String id, String methodName, Class[] argClasses, Object[] argInstances)
    {
      try
      {
	AbstractReplicatedStore.setReplicating(true);
	// only stuff meant for our context will be dispatched to us

	//      String tmp="(";
	//      for (int i=2; i<argInstances.length; i++)
	//	tmp=tmp+argInstances[i]+((i<argInstances.length-1)?", ":"");
	//      tmp=tmp+")";

	//      _log.info("dispatching call: "+argInstances[1]+"."+methodName+tmp);

	// either this is a class method
	if (methodName.equals("create"))
	{
	  _log.debug("creating replicated session: "+id);
	  long creationTime=((Long)argInstances[0]).longValue();
	  int maxInactiveInterval=((Integer)argInstances[1]).intValue();
	  int actualMaxInactiveInterval=((Integer)argInstances[2]).intValue();
	  State state=new ReplicatedState(this, id, creationTime, maxInactiveInterval, actualMaxInactiveInterval);

	  synchronized(_sessions) {_sessions.put(id, state);}
	}
	else if (methodName.equals("destroy"))
	{
	  _log.debug("destroying replicated session: "+id);
	  synchronized(_sessions) {_sessions.remove(id);}
	}
	else
	{
	  // or an instance method..
	  ((ReplicatedState)_sessions.get(id)).dispatch(methodName, argClasses, argInstances);
	}
      }
      finally
      {
	AbstractReplicatedStore.setReplicating(false);
      }
    }
}
