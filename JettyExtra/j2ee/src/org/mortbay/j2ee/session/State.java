// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;

//----------------------------------------

// The API around the isolated state encapsulated by an HttpSession -
// NOT quite the same as an HttpSession interface...

// It would be much cheaper to have set/removeAttribute return a
// boolean or void - but we HAVE TO HAVE the old binding to use in
// ValueUnbound events...

//----------------------------------------

public interface
  State
{
  long        getCreationTime()                       throws RemoteException;
  String      getId()                                 throws RemoteException;
  void        setLastAccessedTime(long time)          throws RemoteException;
  long        getLastAccessedTime()                   throws RemoteException;
  void        setMaxInactiveInterval(int interval)    throws RemoteException;
  int         getMaxInactiveInterval()                throws RemoteException;
  Object      setAttribute(String name, Object value) throws RemoteException; // returns old binding
  Object      getAttribute(String name)               throws RemoteException;
  Object      removeAttribute(String name)            throws RemoteException; // returns old binding
  Enumeration getAttributeNameEnumeration()           throws RemoteException;
  String[]    getAttributeNameStringArray()           throws RemoteException;

  Map         getAttributes()                         throws RemoteException;
  void        setAttributes(Map attributes)           throws RemoteException;
}

