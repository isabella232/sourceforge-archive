// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpSession;

//----------------------------------------

// Superclass for the interceptors that will make up the container,
// simply delegates every call to it's State. Subclasses can choose
// which methods to override...

public
  class StateInterceptor
  implements State
{
  final HttpSession _session;	// TODO - lose session from state - pass in in context
  State _state;

  StateInterceptor(HttpSession session, State state)
  {
    _session=session;
    _state=state;
  }

  protected State getState() {return _state;}
  protected void  setState(State state) {_state=state;}

  protected HttpSession getSession() {return _session;}

  public long        getCreationTime()                       throws RemoteException {return getState().getCreationTime();}
  public String      getId()                                 throws RemoteException {return getState().getId();}
  public void        setLastAccessedTime(long time)          throws RemoteException {getState().setLastAccessedTime(time);}
  public long        getLastAccessedTime()                   throws RemoteException {return getState().getLastAccessedTime();}
  public void        setMaxInactiveInterval(int interval)    throws RemoteException {getState().setMaxInactiveInterval(interval);}
  public int         getMaxInactiveInterval()                throws RemoteException {return getState().getMaxInactiveInterval();}
  public Object      getAttribute(String name)               throws RemoteException {return getState().getAttribute(name);}
  public Enumeration getAttributeNameEnumeration()           throws RemoteException {return getState().getAttributeNameEnumeration();}
  public String[]    getAttributeNameStringArray()           throws RemoteException {return getState().getAttributeNameStringArray();}
  public Object      setAttribute(String name, Object value, boolean returnValue) throws RemoteException {return getState().setAttribute(name, value, returnValue);}
  public Object      removeAttribute(String name, boolean returnValue)            throws RemoteException {return getState().removeAttribute(name, returnValue);}
  public Map         getAttributes()                         throws RemoteException {return getState().getAttributes();}
  public void        setAttributes(Map attributes)           throws RemoteException {getState().setAttributes(attributes);}

  public String toString() {return "<"+getClass()+"->"+getState()+">";}

  public void start() {}
  public void stop() {}
}

