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

/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.mortbay.j2ee.session;

import javax.servlet.http.HttpServletRequest;

public class
  DistributableAjpIdGenerator
  extends DistributableIdGenerator
{
  public synchronized Object
    clone()
    {
      DistributableAjpIdGenerator daig=(DistributableAjpIdGenerator)super.clone();
      daig.setWorkerName(getWorkerName());
      return daig;
    }

  protected String _workerName;
  public String getWorkerName() { return _workerName; }
  public void setWorkerName(String workerName) { _workerName=workerName; }

  public String
    nextId(HttpServletRequest request)
    {
      String id=super.nextId(request);
      String s=(_workerName!=null)?_workerName:(String)request.getAttribute("org.mortbay.http.ajp.JVMRoute");
      return (s==null)?id:id+"."+s;
    }
}
