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


public class ServletMapping
{
    private String _pathSpec;
    private String _servletName;

    /* ------------------------------------------------------------ */
    public ServletMapping()
    {
    }
    
    /* ------------------------------------------------------------ */
    public ServletMapping(String pathSpec,String servlet)
    {
        _pathSpec=pathSpec;
        _servletName=servlet;
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
     * @return Returns the servletName.
     */
    public String getServletName()
    {
        return _servletName;
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
     * @param servletName The servletName to set.
     */
    public void setServletName(String servletName)
    {
        _servletName = servletName;
    }
}