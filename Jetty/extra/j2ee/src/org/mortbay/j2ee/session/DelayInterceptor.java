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


//----------------------------------------

// We need to ensure that calls to the HttpSession implementation are
// made in Jetty's and not the User's Transaction Context. Otherwise
// if their transaction is rolledback, our state is lost and
// vice-versa...

public class DelayInterceptor
  extends AroundInterceptor
{
  protected void
    before()
  {
    try
    {
      Thread.sleep(1000);
    }
    catch (Exception e)
    {
    }
  }

  protected void
    after()
  {
  }

  //  public Object clone() { return this; } // Stateless
}
