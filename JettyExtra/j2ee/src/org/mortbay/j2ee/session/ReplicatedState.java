// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpSessionBindingListener;
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
    ReplicatedState(AbstractReplicatedStore store, String id, long creationTime, int maxInactiveInterval, int actualMaxInactiveInterval)
    {
      _store=store;
      _context=_store.getContextPath();
      _id=id;

      // need to pass through creation time...
      _state=new LocalState(id, maxInactiveInterval, actualMaxInactiveInterval);
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

  public int
    getActualMaxInactiveInterval()
    {
      return _state.getActualMaxInactiveInterval();
    }

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

  public boolean
    isValid()
    {
      return _state.isValid();
    }

  // hacky...
  public boolean
    isValid(int extraTime)
    {
      return _state.isValid(extraTime);
    }

  //----------------------------------------
  // writers - wrap-publish-n-delegate - these should be moved into a
  // ReplicatingInterceptor...

  public void
    setLastAccessedTime(long time)
    {
//       if (!AbstractReplicatedStore.getReplicating())
//       {
// 	Class[] argClasses={Long.TYPE};
// 	Object[] argInstances={new Long(time)};
// 	_store.publish(_id, "setLastAccessedTime", argClasses, argInstances);
//       }

      _state.setLastAccessedTime(time);
    }

  public void
    setMaxInactiveInterval(int interval)
    {
//       if (!AbstractReplicatedStore.getReplicating())
//       {
// 	Class[] argClasses={Integer.TYPE};
// 	Object[] argInstances={new Integer(interval)};
// 	_store.publish(_id, "setMaxInactiveInterval", argClasses, argInstances);
//       }

      _state.setMaxInactiveInterval(interval);
    }

  public Object
    setAttribute(String name, Object value, boolean returnValue)
    {
//       if (!AbstractReplicatedStore.getReplicating())
//       {
// 	// special case to allow double marshalling - works around current limitation in RpcDispatcher...
// 	Class[] argClasses={String.class, Object.class, Boolean.TYPE, Boolean.TYPE};
//
// 	byte[] tmp=null;
// 	try
// 	{
// 	  tmp=MarshallingInterceptor.marshal(value);
// 	}
// 	catch(IOException e)
// 	{
// 	  _log.error("could not marshal arg for publication", e);
// 	}
//
// 	Object[] argInstances={name, tmp, returnValue?Boolean.TRUE:Boolean.FALSE, Boolean.TRUE};
// 	_store.publish(_id, "setAttribute", argClasses, argInstances);
//       }

      return _state.setAttribute(name, value, returnValue);
    }

  public void
    setAttributes(Map attributes)
    {
//       if (!AbstractReplicatedStore.getReplicating())
//       {
// 	// special case to allow double marshalling - works around current limitation in RpcDispatcher...
// 	Class[] argClasses={Map.class, Boolean.TYPE};
//
// 	// marshall all attribute values (into new Map)
// 	Map tmp=new HashMap(attributes.size());
// 	for (Iterator i=attributes.entrySet().iterator(); i.hasNext();)
// 	{
// 	  Map.Entry entry=(Map.Entry)i.next();
// 	  String key=(String)entry.getKey();
// 	  Object val=entry.getValue();
// 	  try
// 	  {
// 	    tmp.put(key,MarshallingInterceptor.marshal(val));
// 	  }
// 	  catch(IOException e)
// 	  {
// 	    _log.error("could not marshal arg ("+key+") for publication", e);
// 	  }
// 	}
//
// 	Object[] argInstances={tmp, Boolean.TRUE};
// 	_store.publish(_id, "setAttributes", argClasses, argInstances);
//       }

      _state.setAttributes(attributes);
    }

  public Object
    removeAttribute(String name, boolean returnValue)
    {
//       if (!AbstractReplicatedStore.getReplicating())
//       {
// 	Class[] argClasses={String.class, Boolean.TYPE};
// 	Object[] argInstances={name, returnValue?Boolean.TRUE:Boolean.FALSE};
// 	_store.publish(_id, "removeAttribute", argClasses, argInstances);
//       }

      return _state.removeAttribute(name, returnValue);
    }

  //----------------------------------------

  public Object
    setAttribute(String name, Object value, boolean returnValue, boolean dummy)
    {
      // deserialise attribute here...
      Object tmp=null;
      try
      {
	tmp=MarshallingInterceptor.demarshal((byte[])value);
      }
      catch(Exception e)
      {
	_log.error("could not demarshal arg for dispatch", e);
      }

      return _state.setAttribute(name, tmp, returnValue);
    }

  public void
    setAttributes(Map attributes, boolean dummy)
    {
      // demarshall all attribute values (back into given Map)
      for (Iterator i=attributes.entrySet().iterator(); i.hasNext();)
      {
	Map.Entry entry=(Map.Entry)i.next();
	String key=(String)entry.getKey();
	Object val=entry.getValue();
	try
	{
	  attributes.put(key, MarshallingInterceptor.demarshal((byte[])val));
	}
	catch(Exception e)
	{
	  _log.error("could not demarshal arg ("+key+") for publication", e);
	}
      }

      _state.setAttributes(attributes);
    }
}
