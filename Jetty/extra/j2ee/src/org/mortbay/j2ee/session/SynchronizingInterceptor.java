// ========================================================================
// $Id$
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Map;

import org.jboss.logging.Logger;

//----------------------------------------

// OK - so I could just synchronise in the LocalState class - but I'm
// sure this will come in handy somewhere... - after all - if your
// were using a SingleThreaded model servlet....


// we only need to synchronize attributes that have writers as well as
// readers...

public class SynchronizingInterceptor
  extends StateInterceptor
{
  protected static final Logger _log=Logger.getLogger(SynchronizingInterceptor.class);

  protected final Object _lastAccessedTimeLock=new Object();
  public void        setLastAccessedTime(long time)          throws RemoteException {synchronized(_lastAccessedTimeLock){super.setLastAccessedTime(time);}}
  public long        getLastAccessedTime()                   throws RemoteException {synchronized(_lastAccessedTimeLock){return super.getLastAccessedTime();}}

  protected final Object _maxInactiveIntervalLock=new Object();
  public void        setMaxInactiveInterval(int interval)    throws RemoteException {synchronized(_maxInactiveIntervalLock){super.setMaxInactiveInterval(interval);}}
  public int         getMaxInactiveInterval()                throws RemoteException {synchronized(_maxInactiveIntervalLock){return super.getMaxInactiveInterval();}}

  protected final Object _attributesLock=new Object();
  public Object      getAttribute(String name)               throws RemoteException {synchronized(_attributesLock){return super.getAttribute(name);}}
  public Enumeration getAttributeNameEnumeration()           throws RemoteException {synchronized(_attributesLock){return super.getAttributeNameEnumeration();}}
  public String[]    getAttributeNameStringArray()           throws RemoteException {synchronized(_attributesLock){return super.getAttributeNameStringArray();}}
  public Object      setAttribute(String name, Object value, boolean returnValue) throws RemoteException {synchronized(_attributesLock){return super.setAttribute(name, value, returnValue);}}
  public Object      removeAttribute(String name, boolean returnValue)            throws RemoteException {synchronized(_attributesLock){return super.removeAttribute(name, returnValue);}}
  public Map         getAttributes()                         throws RemoteException {synchronized(_attributesLock){return super.getAttributes();}}
  public void        setAttributes(Map attributes)           throws RemoteException {synchronized(_attributesLock){super.setAttributes(attributes);}}

  //  public Object clone() { return null; } // Stateful
}
