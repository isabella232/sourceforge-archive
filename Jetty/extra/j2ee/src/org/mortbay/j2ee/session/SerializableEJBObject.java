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

// utility for unambiguously shipping EJBObjects from node to node...

public class
  SerializableEJBObject
  implements java.io.Serializable
{
  javax.ejb.Handle _handle=null;

  protected
    SerializableEJBObject()
    throws java.rmi.RemoteException
    {
    }

  SerializableEJBObject(javax.ejb.EJBObject ejb)
    throws java.rmi.RemoteException
    {
      _handle=ejb.getHandle();
    }

  javax.ejb.EJBObject
    toEJBObject()
    throws java.rmi.RemoteException
    {
      return _handle.getEJBObject();
    }
}
