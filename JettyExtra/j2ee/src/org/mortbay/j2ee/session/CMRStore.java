// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;
import javax.ejb.CreateException;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.apache.log4j.Category;
import org.mortbay.j2ee.session.interfaces.CMRState;
import org.mortbay.j2ee.session.interfaces.CMRStateHome;
import org.mortbay.j2ee.session.interfaces.CMRStatePK;
//----------------------------------------

public class CMRStore
  implements Store
{
  Category       _log=Category.getInstance(getClass().getName());
  InitialContext _jndiContext;
  CMRStateHome   _home;
  String         _name="jetty/CMRState"; // TODO - parameterise
  Manager        _manager;

  public
    CMRStore(Manager manager)
  {
    _manager=manager;
  }

  public void
    destroy()
  {
    // tidy up
  }

  // Store LifeCycle
  public void
    start()
    throws Exception
  {
    // jndi lookups here
    _jndiContext=new InitialContext();
    Object o=_jndiContext.lookup(_name);
    _home=(CMRStateHome)PortableRemoteObject.narrow(o, CMRStateHome.class);
    _log.info("Support for CMP-based Distributed HttpSessions loaded successfully: "+_home);

    synchronized (getClass())
    {
      if (_scavengerCount++==0)
      {
	boolean isDaemon=true;
	_scavenger=new Timer(isDaemon);
	long delay=Math.round(Math.random()*_scavengerPeriod);
	_log.debug("local scavenge delay is: "+delay+" seconds");
	_scavenger.scheduleAtFixedRate(new Scavenger(), delay*1000, _scavengerPeriod*1000);
	_log.debug("started local scavenger");
      }
    }
  }

  public void
    stop()
  {
    synchronized (getClass())
    {
      if (--_scavengerCount==0)
      {
	_scavenger.cancel();
	_scavenger=null;
	_log.debug("stopped local scavenger");
      }
    }
  }

  // State LifeCycle
  public State
    loadState(String id)
  {
    if (_home==null)
      throw new IllegalStateException("invalid store");

    try
    {
      return (CMRState)PortableRemoteObject.narrow(_home.findByPrimaryKey(new CMRStatePK(_manager.getContextPath(), id)), CMRState.class);
    }
    catch (Throwable e)
    {
      _log.warn("session "+id+" not found: "+e);
      return null;
    }
  }

  public State
    newState(String id, int maxInactiveInterval)
    throws RemoteException, CreateException
  {
    if (_home==null)
      throw new IllegalStateException("invalid store");

    Object tmp=_home.create(_manager.getContextPath(), id, maxInactiveInterval);
    CMRState state=(CMRState)PortableRemoteObject.narrow(tmp, CMRState.class);
    return state;
  }

  public void
    storeState(State state)
  {
    // TODO
  }

  public void
    removeState(State state)
    throws RemoteException, RemoveException
  {
    if (_home==null)
      throw new IllegalStateException("invalid store");

    ((CMRState)state).remove();
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
    // these ids are disposable
  }

  public boolean
    isDistributed()
  {
    return true;
  }

  /**
   * The period between scavenges
   */
  protected int _scavengerPeriod=60*30;	// 1/2 an hour
  /**
   * The extra time we wait before tidying up a CMRState to ensure
   * that if it loaded locally it will be scavenged locally first...
   */
  protected int _scavengerExtraTime=60*30; // 1/2 an hour
  /**
   * A maxInactiveInterval of -1 means never scavenge. The DB would
   * fill up verey quickly - so we can override -1 with a real value
   * here.
   */
  protected int _actualMaxInactiveInterval=60*60*24*28;	// 28 days

  public void setScavengerPeriod(int secs) {_scavengerPeriod=secs;}
  public void setScavengerExtraTime(int secs) {_scavengerExtraTime=secs;}
  public void setActualMaxInactiveInterval(int secs) {_actualMaxInactiveInterval=secs;}

  protected static Timer _scavenger;
  protected static int   _scavengerCount=0;

  class Scavenger
    extends TimerTask
  {
    public void
      run()
    {
      try
      {
	scavenge(_scavengerExtraTime, _actualMaxInactiveInterval);
      }
      catch (Exception e)
      {
	_log.warn("could not scavenge distributed sessions", e);
      }
    }
  }


  public void
    scavenge(int extraTime, int actualMaxInactiveInterval)
    throws RemoteException
  {
    // run a GC method EJB-side to remove all Sessions whose
    // maxInactiveInterval+extraTime has run out...

    // no events (unbind, sessionDestroyed etc) will be raised Servlet
    // side on any node, but that's OK, because we know that the
    // session does not 'belong' to any of them, or they would have
    // already GC-ed it....

    _home.scavenge(extraTime, actualMaxInactiveInterval);
  }

  public void
    passivateSession(StateAdaptor sa)
  {
    // we are already passivated...
  }
}
