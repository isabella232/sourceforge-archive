// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Category;

//----------------------------------------


public abstract class InvalidInterceptor
  extends StateInterceptor
{
  public
    InvalidInterceptor(HttpSession session, State state)
  {
    super(session, state);
  }

  public String
    getId()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getId();
  }

  public long
    getCreationTime()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getCreationTime();
  }

  public long
    getLastAccessedTime()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getLastAccessedTime();
  }

  public void
    setLastAccessedTime(long time)
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    getState().setLastAccessedTime(time);
  }

  public int
    getMaxInactiveInterval()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getMaxInactiveInterval();
  }

  public void
    setMaxInactiveInterval(int interval)
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    getState().setMaxInactiveInterval(interval);
  }

  public Map
    getAttributes()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getAttributes();
  }

  public void
    setAttributes(Map attributes)
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    getState().setAttributes(attributes);
  }

  public Object
    getAttribute(String name)
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getAttribute(name);
  }

  public Object
    setAttribute(String name, Object value, boolean returnValue)
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().setAttribute(name, value, returnValue);
  }

  public Enumeration
    getAttributeNameEnumeration()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getAttributeNameEnumeration();
  }

  public String[]
    getAttributeNameStringArray()
    throws RemoteException, IllegalStateException
  {
    ensureValid();
    return getState().getAttributeNameStringArray();
  }

  public Object      removeAttribute(String name, boolean returnValue)            throws RemoteException {return getState().removeAttribute(name, returnValue);}

  //----------------------------------------
  protected int
    getActualMaxInactiveInterval()
  {
    //    return getManager().getActualMaxInactiveInterval();
    return 60*60*24;
  }

  protected long
    getRealMaxInactiveValue()
    throws RemoteException
  {
    long maxInactiveInterval=getState().getMaxInactiveInterval();
    return maxInactiveInterval<1?getActualMaxInactiveInterval()*1000:maxInactiveInterval;
  }

  protected boolean
    isValid()
    throws RemoteException
  {
    return (getState().getLastAccessedTime()+getRealMaxInactiveValue())>System.currentTimeMillis();
  }

  protected void
    ensureValid()
    throws RemoteException,IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("HttpSession timed out");
  }
}
