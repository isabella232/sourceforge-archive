// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.apache.log4j.Category;

//----------------------------------------
// this class is responsible for presenting a State container to a
// servlet as a HttpSession - since this interface is not an ideal one
// to use for the container interceptors. It constrains all the inputs
// to this API as specified...

// Since this is the front end of the Container, maybe it should be so
// called ? or maybe I should just call it [Http]Session?

// we should cache our id locally...

public class StateAdaptor
  implements org.mortbay.jetty.servlet.SessionManager.Session
{
  final Category _log=Category.getInstance(getClass().getName());
  Manager        _manager;
  State          _state=null;
  boolean        _new=true;

  // we cache these for speed...
  final String _id;
  long _lastAccessedTime;
  int _maxInactiveInterval;


  StateAdaptor(String id, Manager manager, int maxInactiveInterval, long lastAccessedTime)
  {
    _id=id;
    _manager=manager;
    _maxInactiveInterval=maxInactiveInterval;
    _lastAccessedTime=lastAccessedTime;	// this starts not quite in synch with State
  }

  void
    setState(State state)
  {
    _state=state;
  }

  State
    getState()
  {
    return _state;
  }

  protected int getActualMaxInactiveInterval() { return _manager.getActualMaxInactiveInterval(); }
  protected int getRealMaxInactiveValue() { return _maxInactiveInterval<1?getActualMaxInactiveInterval():_maxInactiveInterval; }

  public boolean
    isValid()
  {
    if (_state==null)
      return false;

    long currentSecond=_manager.currentSecond();
    _log.debug("isValid - checking local cache");
    if ((_lastAccessedTime+(getRealMaxInactiveValue()*1000))<currentSecond)
    {
      // our local cache reckons we have timed out - confirm with our
      // actual state, which may have been changed via another route
      // (e.g.. on another box)... - and update our local cache at the
      // same time...

      try
      {
	// TODO - we need some synchronisation and maybe optionally
	// support for Transactions around this...
	_lastAccessedTime   =_state.getLastAccessedTime();
	_maxInactiveInterval=_state.getMaxInactiveInterval();
	// check again...
	_log.info("isValid - checking distributed state");
	if ((_lastAccessedTime+(getRealMaxInactiveValue()*1000))<currentSecond)
	{
	  //	  invalidate(); - watch out for this - you'll recurse to the bottom of the stack !
	  _log.info("isValid - calling _manager.destroySession...");
	  _manager.destroySession(_id);
	  return false;
	}
      }
      catch (RemoteException e)
      {
	_log.error("problem querying distributed state...", e);
	// we can't declare ourself invalid - because we don't know
	// what is happening - even if we did, we wouldn't be able to
	// invalidate() ourself because the DB is probably down...
      }
    }

    // if we got to here - we must have a valid state...
    return true;
  }

  // HttpSession API

  public long
    getCreationTime()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getCreationTime();
    }
    catch (RemoteException e)
    {
      _log.error("could not get CreationTime", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public String
    getId()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    // locally cached and invariant
    return _id;
  }

  public long
    getLastAccessedTime()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getLastAccessedTime();
    }
    catch (RemoteException e)
    {
      _log.error("could not get LastAccessedTime", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  // clarify with Tomcat, whether this is on a per Session or SessionManager basis...
  public void
    setMaxInactiveInterval(int interval)
  {
    if (!isValid())
      throw new IllegalStateException("invalid session"); // TODO - not spec

    try
    {
      _state.setMaxInactiveInterval(interval);
      _maxInactiveInterval=interval; // synchronize - TODO
    }
    catch (RemoteException e)
    {
      _log.error("could not set MaxInactiveInterval", e);
    }
  }

  public int
    getMaxInactiveInterval()
  {
    if (!isValid())
      throw new IllegalStateException("invalid session"); // TODO - not spec

    try
    {
      return _state.getMaxInactiveInterval();
    }
    catch (RemoteException e)
    {
      // Can I throw an exception of some type here - instead of
      // returning rubbish ? - TODO
      _log.error("could not get MaxInactiveInterval", e);
      return 0;
    }
  }

  public Object
    getAttribute(String name)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getAttribute(name);
    }
    catch (RemoteException e)
    {
      _log.error("could not get Attribute", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public Object
    getValue(String name)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getAttribute(name);
    }
    catch (RemoteException e)
    {
      _log.error("could not get Value", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public Enumeration
    getAttributeNames()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getAttributeNameEnumeration();
    }
    catch (RemoteException e)
    {
      _log.error("could not get AttributeNames", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public String[]
    getValueNames()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      return _state.getAttributeNameStringArray();
    }
    catch (RemoteException e)
    {
      _log.error("could not get ValueNames", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public void
    setAttribute(String name, Object value)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      if (value==null)
	_state.removeAttribute(name);
      else
      {
	if (name==null)
	  throw new IllegalArgumentException("invalid attribute name: "+name);

	_state.setAttribute(name, value);
      }
    }
    catch (RemoteException e)
    {
      _log.error("could not set Attribute", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public void
    putValue(String name, Object value)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    if (name==null)
      throw new IllegalArgumentException("invalid attribute name: "+name);

    if (value==null)
      throw new IllegalArgumentException("invalid attribute value: "+value);

    try
    {
      _state.setAttribute(name, value);
    }
    catch (RemoteException e)
    {
      _log.error("could not put Value", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public void
    removeAttribute(String name)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      _state.removeAttribute(name);
    }
    catch (RemoteException e)
    {
      _log.error("could not remove Attribute", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public void
    removeValue(String name)
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    try
    {
      _state.removeAttribute(name);
    }
    catch (RemoteException e)
    {
      _log.error("could not remove Value", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public void
    invalidate()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    //    _log.info("user invalidated session: "+getId());
    _manager.destroySession(_id);
  }

  /**
   *
   * Returns <code>true</code> if the client does not yet know about the
   * session or if the client chooses not to join the session.  For
   * example, if the server used only cookie-based sessions, and
   * the client had disabled the use of cookies, then a session would
   * be new on each request.
   *
   * @return 				<code>true</code> if the
   *					server has created a session,
   *					but the client has not yet joined
   *
   * @exception IllegalStateException	if this method is called on an
   *					already invalidated session
   *
   */

  public boolean
    isNew()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("invalid session");

    return _new;
  }

  public ServletContext
    getServletContext()
  {
    return _manager.getServletContext();
  }

  public HttpSessionContext
    getSessionContext()
  {
    return _manager.getSessionContext();
  }

  // this one's for Greg...
  public void
    access()
  {
    long time=System.currentTimeMillis(); // we could get this from Manager - less accurate
    setLastAccessedTime(time);

    _new=false;			// synchronise - TODO
  }

  public void
    setLastAccessedTime(long time)
    throws IllegalStateException
  {
    if (!isValid())		// do we need this check ?
      throw new IllegalStateException("invalid session");

    try
    {
      _state.setLastAccessedTime(time);
      _lastAccessedTime=time;	// local cache - synchronize - TODO
    }
    catch (RemoteException e)
    {
      _log.error("could not set LastAccessedTime", e);
      throw new IllegalStateException("problem with distribution layer");
    }
  }

  public String
    toString()
  {
    return "<"+getClass()+"->"+_state+">";
  }

  // I'm still not convinced that this is the correct place for this
  // method- but I can;t think of a better way - maybe in the next
  // iteration...

  MigrationInterceptor _mi=null;

  public void
    registerMigrationListener(MigrationInterceptor mi)
  {
    _mi=mi;
  }

  public void
    migrate()
  {
    if (!isValid())		// do we need this check ?
      throw new IllegalStateException("invalid session");

    if (_mi!=null)
      _mi.migrate(); // yeugh - TODO
  }
}
