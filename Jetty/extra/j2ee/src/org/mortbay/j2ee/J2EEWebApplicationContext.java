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

package org.mortbay.j2ee;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.mortbay.j2ee.session.Manager;
import org.mortbay.jetty.servlet.WebApplicationContext;

public class
  J2EEWebApplicationContext
  extends WebApplicationContext
{
  protected static final Logger _log=Logger.getLogger(J2EEWebApplicationContext.class);

  public J2EEWebApplicationContext(String warUrl)
    throws IOException
    {
      super(warUrl);
    }

  //----------------------------------------------------------------------------
  // DistributedHttpSession support
  //----------------------------------------------------------------------------

  protected boolean _distributable=false;

  public boolean getDistributable()
    {
      return _distributable;
    }

  public void setDistributable(boolean distributable)
    {
      if (_log.isDebugEnabled()) _log.debug("setDistributable "+distributable);
      _distributable=distributable;
    }

  protected Manager _distributableSessionManager;

  public void setDistributableSessionManager(Manager manager)
    {
      //      _log.info("setDistributableSessionManager "+manager);
      _distributableSessionManager=(Manager)manager;
      _distributableSessionManager.setContext(this);
    }

  public Manager getDistributableSessionManager()
    {
      return _distributableSessionManager;
    }

  //----------------------------------------------------------------------------
  protected void doStart()
    throws Exception
  {
      if (getStopGracefully() && !getStatsOn())
	setStatsOn(true);

      super.doStart();
  }
}
