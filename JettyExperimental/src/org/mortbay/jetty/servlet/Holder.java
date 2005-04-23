// ========================================================================
// $Id$
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;
import org.mortbay.thread.AbstractLifeCycle;
import org.mortbay.util.Loader;


/* --------------------------------------------------------------------- */
/** 
 * @version $Id$
 * @author Greg Wilkins
 */
public class Holder extends AbstractLifeCycle implements Serializable
{
    private static ULogger log = LoggerFactory.getLogger(Holder.class);
    protected transient Class _class;
    protected String _className;
    protected String _displayName;
    protected Map _initParams;

    /* ---------------------------------------------------------------- */
    protected String _name;
    protected ServletHandler _servletHandler;

    protected Holder()
    {}


    /* ------------------------------------------------------------ */
    public void doStart()
        throws Exception
    {
        _class=Loader.loadClass(Holder.class, _className);
        if(log.isDebugEnabled())log.debug("Holding {}",_class);
    }

    
    /* ------------------------------------------------------------ */
    public void doStop()
    {
        _class=null;
    }
    
    /* ------------------------------------------------------------ */
    public String getClassName()
    {
        return _className;
    }
    
    /* ------------------------------------------------------------ */
    public String getDisplayName()
    {
        return _name;
    }

    /* ---------------------------------------------------------------- */
    public String getInitParameter(String param)
    {
        if (_initParams==null)
            return null;
        return (String)_initParams.get(param);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getInitParameterNames()
    {
        if (_initParams==null)
            return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(_initParams.keySet());
    }

    /* ---------------------------------------------------------------- */
    public Map getInitParameters()
    {
        return _initParams;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the initParams.
     */
    public Map getInitParams()
    {
        return _initParams;
    }
   
    
    /* ------------------------------------------------------------ */
    public String getName()
    {
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the servletHandler.
     */
    public ServletHandler getServletHandler()
    {
        return _servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    public synchronized Object newInstance()
        throws InstantiationException,
               IllegalAccessException
    {
        if (_class==null)
            throw new InstantiationException("!"+_className);
        return _class.newInstance();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param className The className to set.
     */
    public void setClassName(String className)
    {
        _className = className;
    }
    
    /* ------------------------------------------------------------ */
    public void setDisplayName(String name)
    {
        _name=name;
    }
    
    /* ------------------------------------------------------------ */
    public void setInitParameter(String param,String value)
    {
        if (_initParams==null)
            _initParams=new HashMap(3);
        _initParams.put(param,value);
    }
    
    /* ---------------------------------------------------------------- */
    public void setInitParameters(Map map)
    {
        _initParams=map;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param initParams The initParams to set.
     */
    public void setInitParams(Map initParams)
    {
        _initParams = initParams;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        _name = name;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletContext The servletHandler to set.
     */
    public void setServletHandler(ServletHandler servletHandler)
    {
        _servletHandler = servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return _name;
    }
}





