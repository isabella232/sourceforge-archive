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


public abstract class PushPopInterceptor
  extends StateInterceptor
{
  public
    PushPopInterceptor(HttpSession session, State state)
  {
    super(session, state);
  }

  protected abstract void pushContext();
  protected abstract void popContext();

  public long
    getCreationTime()
    throws RemoteException
  {
    long tmp=0;

    pushContext();
    try
    {
      tmp=super.getCreationTime();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public String
    getId()
    throws RemoteException
  {
    String tmp=null;

    pushContext();
    try
    {
      tmp=super.getId();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public void
    setLastAccessedTime(long time)
    throws RemoteException
  {
    pushContext();
    try
    {
      super.setLastAccessedTime(time);
    }
    finally
    {
      popContext();
    }
  }

  public long
    getLastAccessedTime()
    throws RemoteException
  {
    long tmp=0;

    pushContext();
    try
    {
      tmp=super.getLastAccessedTime();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public void
    setMaxInactiveInterval(int interval)
    throws RemoteException
  {
    pushContext();
    try
    {
      super.setMaxInactiveInterval(interval);
    }
    finally
    {
      popContext();
    }
  }

  public int
    getMaxInactiveInterval()
    throws RemoteException
  {
    int tmp=0;

    pushContext();
    try
    {
      tmp=super.getMaxInactiveInterval();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public Object
    getAttribute(String name)
    throws RemoteException
  {
    Object tmp=null;

    pushContext();
    try
    {
      tmp=super.getAttribute(name);
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public Enumeration
    getAttributeNameEnumeration()
    throws RemoteException
  {
    Enumeration tmp=null;

    pushContext();
    try
    {
      tmp=super.getAttributeNameEnumeration();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public String[]
    getAttributeNameStringArray()
    throws RemoteException
  {
    String[] tmp=null;

    pushContext();
    try
    {
      tmp=super.getAttributeNameStringArray();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public Object
    setAttribute(String name, Object value)
    throws RemoteException
  {
    Object tmp=null;

    pushContext();
    try
    {
      tmp=super.setAttribute(name, value);
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public Object
    removeAttribute(String name)
    throws RemoteException
  {
    Object tmp=null;

    pushContext();
    try
    {
      tmp=super.removeAttribute(name);
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public Map
    getAttributes()
    throws RemoteException
  {
    Map tmp=null;

    pushContext();
    try
    {
      tmp=super.getAttributes();
    }
    finally
    {
      popContext();
    }

    return tmp;
  }

  public void
    setAttributes(Map attributes)
    throws RemoteException
  {
    pushContext();
    try
    {
      super.setAttributes(attributes);
    }
    finally
    {
      popContext();
    }
  }
}
