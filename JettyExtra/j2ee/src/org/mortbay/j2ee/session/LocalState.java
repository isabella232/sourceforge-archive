// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

//----------------------------------------

// a simple implementation used to hold the State of an HttpSession
// that is Local (not Distributed).

class LocalState
  implements State
{
  protected final String _id;

  protected   long   _creationTime     =System.currentTimeMillis();
  protected   long   _lastAccessedTime =_creationTime;
  protected   Map    _attributes       =new HashMap();

  protected   int    _maxInactiveInterval;

  public
    LocalState(String id, int maxInactiveInterval)
  {
    _id=id;
    _maxInactiveInterval=maxInactiveInterval;
  }

  public long        getCreationTime()                       {return _creationTime;}
  public String      getId()                                 {return _id;}
  public void        setLastAccessedTime(long time)          {_lastAccessedTime=time;};
  public long        getLastAccessedTime()                   {return _lastAccessedTime;}
  public void        setMaxInactiveInterval(int interval)    {_maxInactiveInterval=interval;};
  public int         getMaxInactiveInterval()                {return _maxInactiveInterval;}
  public Object      getAttribute(String name)               {return _attributes.get(name);}
  public Enumeration getAttributeNameEnumeration()           {return Collections.enumeration(_attributes.keySet());}
  public String[]    getAttributeNameStringArray()           {return (String[])_attributes.keySet().toArray(new String[_attributes.size()]);}
  public Object      setAttribute(String name, Object value) {return _attributes.put(name, value);}
  public Object      removeAttribute(String name)            {return _attributes.remove(name);}

  public Map         getAttributes() {return Collections.unmodifiableMap(_attributes);}
  public void        setAttributes(Map attributes) {_attributes.clear();_attributes.putAll(attributes);}
}

