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

import javax.naming.Context;
import javax.naming.InitialContext;

// utility for unambiguously shipping Contexts from node to node..

// Aarrgh ! - I thought this would work - but no luck - looking up a
// Context returns a different Object each time, and I cannot find a
// decent way to figure out whether they represent the same Context or
// not...

// it looks like this will need proprietary info - the J2EE API does
// not give us enough...

public class
  SerializableContext
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

  protected
    SerializableContext()
    throws java.rmi.RemoteException
    {
    }

  SerializableContext(Context context)
    throws java.rmi.RemoteException
    {

      log_warn("distribution of Contexts is NYI - assuming java:comp/env");

      //       Context tmp=null;
      //       try
      //       {
      // 	tmp=(Context)new javax.naming.InitialContext().lookup("java:comp/env");
      // 	System.err.println(new javax.naming.InitialContext().lookup("java:comp/env")+"!="+new javax.naming.InitialContext().lookup("java:comp/env"));
      //       }
      //       catch (Exception e)
      //       {
      // 	System.err.println("could not distribute Context"+context);
      // 	e.printStackTrace(System.err);
      //       }
      //
      //       if (context!=tmp)
      //       {
      // 	System.err.println(context+"!="+tmp);
      // 	throw new IllegalArgumentException("only java:comp/env Context may be distributed");
      //       }
    }

  Context
    toContext()
    throws java.rmi.RemoteException
    {
      try
      {
	// optimise - TODO
	return (Context)new InitialContext().lookup("java:comp/env");
      }
      catch (Exception e)
      {
	log_error("could not lookup Context: "+"java:comp/env", e);
	return null;
      }
    }
}
