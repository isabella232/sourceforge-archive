//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.servlet;

import org.mortbay.jetty.Handler;


public class FilterMapping
{
    private int _dispatches=Handler.REQUEST;
    private String _filterName;
    private transient FilterHolder _holder;
    private String _pathSpec;
    private String _servletName;

    /* ------------------------------------------------------------ */
    public FilterMapping()
    {}
    
    /* ------------------------------------------------------------ */
    public FilterMapping(String pathSpec, String servletName,int dispatches,String filter)
    {
        _pathSpec=pathSpec;
        _servletName=servletName;
        _filterName=filter;
        _dispatches=dispatches;
    }
    
    /* ------------------------------------------------------------ */
    /** Check if this filter applies to a path.
     * @param path The path to check.
     * @param type The type of request: __REQUEST,__FORWARD,__INCLUDE or __ERROR.
     * @return True if this filter applies
     */
    boolean appliesTo(String path, int type)
    {
       boolean b=((_dispatches&type)!=0 || (_dispatches==0 && type==Handler.REQUEST)) && (_pathSpec==null || PathMap.match(_pathSpec, path,true));
       return b;
    }

    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the dispatches.
     */
    public int getDispatches()
    {
        return _dispatches;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the filterName.
     */
    public String getFilterName()
    {
        return _filterName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the holder.
     */
    FilterHolder getFilterHolder()
    {
        return _holder;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the pathSpec.
     */
    public String getPathSpec()
    {
        return _pathSpec;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param dispatches The dispatches to set.
     */
    public void setDispatches(int dispatches)
    {
        _dispatches = dispatches;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param filterName The filterName to set.
     */
    public void setFilterName(String filterName)
    {
        _filterName = filterName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param holder The holder to set.
     */
    void setFilterHolder(FilterHolder holder)
    {
        _holder = holder;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param pathSpec The pathSpec to set.
     */
    public void setPathSpec(String pathSpec)
    {
        _pathSpec = pathSpec;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the servletName.
     */
    public String getServletName()
    {
        return _servletName;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletName The servletName to set.
     */
    public void setServletName(String servletName)
    {
        _servletName = servletName;
    }
}