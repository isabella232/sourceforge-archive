// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import org.apache.log4j.Category;

//----------------------------------------

public class ActivationInterceptor
  extends StateInterceptor
{
  Category _log=Category.getInstance(getClass().getName());
  protected final HttpSessionEvent _event;

  public
    ActivationInterceptor(Manager ignore, HttpSession session, State state)
  {
    super(session, state);
    _event=new HttpSessionEvent(_session); // cache an event ready for use...
  }

  public Object
    getAttribute(String name)
    throws IllegalArgumentException, RemoteException
  {
    try
    {
      Object tmp=super.getAttribute(name);
      if (tmp!=null && tmp instanceof HttpSessionActivationListener)
 	((HttpSessionActivationListener)tmp).sessionDidActivate(_event);

      return tmp;
    }
    catch (Exception e)
    {
      _log.error("could not get Attribute: "+name, e);
      throw new IllegalArgumentException("could not get Attribute");
    }
  }

  public Object
    setAttribute(String name, Object value)
    throws IllegalArgumentException
  {
    try
    {
      Object tmp=value;
      if (tmp!=null && tmp instanceof HttpSessionActivationListener)
	((HttpSessionActivationListener)tmp).sessionWillPassivate(_event);

      tmp=super.setAttribute(name, tmp);

      if (tmp!=null && tmp instanceof HttpSessionActivationListener)
	((HttpSessionActivationListener)tmp).sessionDidActivate(_event);

      return tmp;
    }
    catch (Exception e)
    {
      _log.error("could not set Attribute: "+name+":"+value, e);
      throw new IllegalArgumentException("could not set Attribute");
    }
  }

  // should an attribute be activated before it is removed ? How do we deal with the bind/unbind events... - TODO
  public Object
    removeAttribute(String name)
    throws IllegalArgumentException
  {
    try
    {
      Object tmp=super.removeAttribute(name);

      if (tmp!=null && tmp instanceof HttpSessionActivationListener)
	((HttpSessionActivationListener)tmp).sessionDidActivate(_event);

      return tmp;
    }
    catch (Exception e)
    {
      _log.error("could not remove Attribute: "+name, e);
      throw new IllegalArgumentException("could not remove Attribute");
    }
  }
}
