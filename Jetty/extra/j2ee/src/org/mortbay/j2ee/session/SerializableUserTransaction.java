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

import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

// utility for unambiguously shipping UserTransactions from node to node..

// NYI

// it looks like this will need proprietary info - the J2EE API does
// not give us enough... - I'm talking to Ole.

public class
  SerializableUserTransaction
  implements java.io.Serializable
{
  protected void
    log_warn(String message)
    {
      System.err.println("WARNING: "+message);
    }

  protected void
    log_error(String message, Exception e)
    {
      System.err.println("ERROR: "+message);
      e.printStackTrace(System.err);
    }

  protected Context _ctx=null;

  protected
    SerializableUserTransaction()
    throws RemoteException
    {
    }

  SerializableUserTransaction(UserTransaction userTransaction)
    throws RemoteException
    {
      log_warn("distribution of UserTransaction is NYI/Forbidden");
    }

  UserTransaction
    toUserTransaction()
    throws RemoteException
    {
      try
      {
	// optimise - TODO
	return (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
      }
      catch (Exception e)
      {
	log_error("could not lookup UserTransaction", e);
	return null;
      }
    }
}
