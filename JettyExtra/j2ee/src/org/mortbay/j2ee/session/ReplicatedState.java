// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;
import org.apache.log4j.Category;
//----------------------------------------

// Should this be some sort of interceptor ?

// we could optimise this by defining methods a,b,c,... and publishing
// using those method names...

/**
 * Maintain synchronisation with other States representing the same
 * session by publishing changes made to ourself and updating ourself
 * according to notifications published by the other State objects.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public class
  ReplicatedState
  implements State
{
  protected final Category _log=Category.getInstance(getClass().getName());
  protected final AbstractReplicatedStore _store;
  protected final String _context;
  protected final String _id;

  protected LocalState _state;

  public
    ReplicatedState(AbstractReplicatedStore store, String id, long creationTime, int maxInactiveInterval)
    {
      _store=store;
      _context=_store.getContextPath();
      _id=id;

      // need to pass through creation time...
      _state=new LocalState(id, maxInactiveInterval);
    }

  ReplicatedState(AbstractReplicatedStore store, LocalState state)
    {
      _store=store;
      _context=_store.getContextPath();
      _state=state;		// we are taking ownership...
      _id=_state.getId();
    }


  //----------------------------------------

  LocalState
    getLocalState()
    {
      return _state;
    }

  //----------------------------------------
  // readers - simply wrap-n-delegate

  public long
    getCreationTime()
    {
      return _state.getCreationTime();
    }

  public String
    getId()
    {
      return _state.getId();
    }

  public long
    getLastAccessedTime()
    {
      return _state.getLastAccessedTime();
    }

  public int
    getMaxInactiveInterval()
    {
      return _state.getMaxInactiveInterval();
    }

  public Object
    getAttribute(String name)
    {
      return _state.getAttribute(name);
    }

  public Enumeration
    getAttributeNameEnumeration()
    {
      return _state.getAttributeNameEnumeration();
    }

  public String[]
    getAttributeNameStringArray()
    {
      return _state.getAttributeNameStringArray();
    }

  public Map
    getAttributes()
    {
      return _state.getAttributes();
    }

  //----------------------------------------
  // writers - wrap-n-publish

  public void
    setLastAccessedTime(long time)
    {
      Class[] argClasses={String.class, String.class, Long.class};
      Object[] argInstances={_context, _id, new Long(time)};
      _store.publish("setLastAccessedTime", argClasses, argInstances);
    }

  public void
    setMaxInactiveInterval(int interval)
    {
      Class[] argClasses={String.class, String.class, Integer.class};
      Object[] argInstances={_context, _id, new Integer(interval)};
      _store.publish("setMaxInactiveInterval", argClasses, argInstances);
    }

  public Object
    setAttribute(String name, Object value)
    {
      Class[] argClasses={String.class, String.class, String.class, Object.class};
      Object[] argInstances={_context, _id, name, value};

      Object oldValue=_state.getAttribute(name);
      _store.publish("setAttribute", argClasses, argInstances);

      return oldValue;
    }

  public void
    setAttributes(Map attributes)
    {
      Class[] argClasses={String.class, String.class, Map.class};
      Object[] argInstances={_context, _id, attributes};
      _store.publish("setAttributes", argClasses, argInstances);
    }

  public Object
    removeAttribute(String name)
    {
      Class[] argClasses={String.class, String.class, String.class};
      Object[] argInstances={_context, _id, name};

      Object oldValue=_state.getAttribute(name);
      _store.publish("removeAttribute", argClasses, argInstances);

      return oldValue;
    }

  //----------------------------------------

  void
    dispatch(String methodName, Class[] argClasses, Object[] argInstances)
    {
      // only stuff meant for our session will be dispatched to us..
      try
      {
	getClass().getMethod(methodName, argClasses).invoke(this, argInstances);
      }
      catch (Exception e)
      {
	_log.error("this should never happen - code version mismatch ?", e);
      }
    }

  // yeughhhhh! - but cheaper than reformatting args

  // writers - receive-n-delegate

  public void
    setLastAccessedTime(String context, String id, Long time)
    {
      _state.setLastAccessedTime(time.longValue());
    }

  public void
    setMaxInactiveInterval(String context, String id, Integer interval)
    {
      _state.setMaxInactiveInterval(interval.intValue());
    }

  public Object
    setAttribute(String context, String id, String name, Object value)
    {
      return _state.setAttribute(name, value);
    }

  public void
    setAttributes(String context, String id, Map attributes)
    {
      _state.setAttributes(attributes);
    }

  public Object
    removeAttribute(String context, String id, String name)
    {
      return _state.removeAttribute(name);
    }
}
