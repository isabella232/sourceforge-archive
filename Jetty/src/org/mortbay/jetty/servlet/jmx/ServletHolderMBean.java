// ========================================================================
// $Id$
// Copyright 200-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanException;

import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ServletHolderMBean extends HolderMBean 
{
    /* ------------------------------------------------------------ */
    private ServletHolder _holder;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public ServletHolderMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("initOrder");
        defineAttribute("paths",READ_ONLY,ON_MBEAN);

        _holder=(ServletHolder)getManagedResource();
    }

    /* ------------------------------------------------------------ */
    public String[] getPaths()
    {
        ServletHandler handler = (ServletHandler)_holder.getHttpHandler();
        Map servletMap = handler.getServletMap();
        ArrayList paths = new ArrayList(servletMap.size());
        Iterator iter = servletMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry =(Map.Entry)iter.next();
            if (entry.getValue()==_holder)
                paths.add(entry.getKey());
        }
        return (String[])paths.toArray(new String[paths.size()]);
    }
    
    

}
