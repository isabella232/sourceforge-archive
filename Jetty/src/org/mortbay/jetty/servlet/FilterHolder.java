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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.mortbay.http.HttpHandler;
import org.mortbay.http.PathMap;
import org.mortbay.util.LazyList;
import org.mortbay.util.TypeUtil;

/* --------------------------------------------------------------------- */
/** 
 * @version $Id$
 * @author Greg Wilkins
 */
public class FilterHolder
    extends Holder
{
    /* ------------------------------------------------------------ */
    public static final int
        __DEFAULT=0,
        __REQUEST=1,
        __FORWARD=2,
        __INCLUDE=4,
        __ERROR=8,
        __ALL=15;
    

    public static int type(String type)
    {
        if ("request".equalsIgnoreCase(type))
            return __REQUEST;
        if ("forward".equalsIgnoreCase(type))
            return __FORWARD;
        if ("include".equalsIgnoreCase(type))
            return __INCLUDE;
        if ("error".equalsIgnoreCase(type))
            return __ERROR;
        throw new IllegalArgumentException(type);
    }
    
    /* ------------------------------------------------------------ */
    private PathMap _pathSpecs;
    private Map _servlets;

    private transient Filter _filter;
    private transient Config _config;
        
    /* ---------------------------------------------------------------- */
    /** Constructor for Serialization.
     */
    public FilterHolder()
    {}
    
    /* ---------------------------------------------------------------- */
    public FilterHolder(HttpHandler httpHandler,
                        String name,
                        String className)
    {
        super(httpHandler,name,className);
    }


    /* ------------------------------------------------------------ */
    /** Add a type that this filter applies to.
     * @param type "REQUEST", "FORWARD", "INCLUDE" or "ERROR"
     */
    public void addDispatchesToServlet(String name, String type)
    {
        if (_servlets==null || !_servlets.containsKey(name))
            throw new IllegalStateException();
        _servlets.put(name,TypeUtil.newInteger(((Integer)_servlets.get(name)).intValue()|type(type)));
    }
    
    /* ------------------------------------------------------------ */
    /** Add a type that this filter applies to.
     * @param type "REQUEST", "FORWARD", "INCLUDE" or "ERROR"
     */
    public void addDispatchesToPathSpec(String pathSpec, String type)
    {
        if (_pathSpecs==null || !_pathSpecs.containsKey(pathSpec))
            throw new IllegalStateException();
        _pathSpecs.put(pathSpec,TypeUtil.newInteger(((Integer)_pathSpecs.get(pathSpec)).intValue()|type(type)));
    }
    
    /* ------------------------------------------------------------ */
    /** Add A servlet that this filter applies to.
     * @param servlet 
     */
    public void addServlet(String servlet)
    {
        if (_servlets==null)
            _servlets=new HashMap();
        _servlets.put(servlet,new Integer(__DEFAULT));
    }
    
    /* ------------------------------------------------------------ */
    /** Add A path spec that this filter applies to.
     * @param pathSpec 
     */
    public void addPathSpec(String pathSpec)
    {
        if (_pathSpecs==null)
            _pathSpecs=new PathMap(true);
        _pathSpecs.put(pathSpec,TypeUtil.newInteger(__DEFAULT));
    }
    
    /* ------------------------------------------------------------ */
    public boolean isMappedToPath()
    {
        return _pathSpecs!=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Check if this filter applies to a path.
     * @param path The path to check.
     * @param type The type of request: __REQUEST,__FORWARD,__INCLUDE or __ERROR.
     * @return True if this filter applies
     */
    public boolean appliesToPath(String path, int type)
    {
        if (_pathSpecs==null)
            return false;
        Integer t=(Integer)_pathSpecs.match(path);
        if (t==null)
            return false;
        return (t.intValue()==0 && type==__REQUEST) || (t.intValue()&type)!=__DEFAULT;
    }
    
    /* ------------------------------------------------------------ */
    /** Check if this filter applies to a servlet.
     * @param path The path to check.
     * @param type The type of request: __REQUEST,__FORWARD,__INCLUDE or __ERROR.
     * @return True if this filter applies
     */
    public boolean appliesToServlet(String name, int type)
    {
        if (_servlets==null)
            return false;
        Integer t=(Integer)_servlets.get(name);
        if (t==null)
            return false;
        return (t.intValue()==0 && type==__REQUEST) || (t.intValue()&type)!=0;
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        super.start();
        
        if (!javax.servlet.Filter.class
            .isAssignableFrom(_class))
        {
            super.stop();
            throw new IllegalStateException(_class+" is not a javax.servlet.Filter");
        }

        _filter=(Filter)newInstance();
        _config=new Config();
        _filter.init(_config);
    }

    /* ------------------------------------------------------------ */
    public void stop()
    {
        if (_filter!=null)
            _filter.destroy();
        _filter=null;
        _config=null;
        super.stop();   
    }
    
    /* ------------------------------------------------------------ */
    public Filter getFilter()
    {
        return _filter;
    }

    /* ------------------------------------------------------------ */
    public String[] getPaths()
    {
        if (_pathSpecs==null)
            return null;
        int s = _pathSpecs.keySet().size();
        return (String[]) _pathSpecs.keySet().toArray(new String[s]);
    }
    
    /* ------------------------------------------------------------ */
    public String[] getServlets()
    {
        if (_servlets==null)
            return null;
        int s = LazyList.size(_servlets);
        return (String[])LazyList.getList(_servlets).toArray(new String[s]);
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return getName();
    }
    
    /* ------------------------------------------------------------ */
    public String dump()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getName());
        buf.append('[');
        buf.append(getClassName());
        for (int i=0;i<LazyList.size(_servlets);i++)
        {
            buf.append(',');
            buf.append(LazyList.get(_servlets,i));
        }
        if (_pathSpecs!=null)
        {
            Iterator iter = _pathSpecs.keySet().iterator();
            while (iter.hasNext())
            {
                buf.append(',');
                buf.append(iter.next());
            }
        }
        buf.append(']');
        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config implements FilterConfig
    {
        /* ------------------------------------------------------------ */
        public String getFilterName()
        {
            return FilterHolder.this.getName();
        }

        /* ------------------------------------------------------------ */
        public ServletContext getServletContext()
        {
            return ((WebApplicationHandler)_httpHandler).getServletContext();
        }
        
        /* -------------------------------------------------------- */
        public String getInitParameter(String param)
        {
            return FilterHolder.this.getInitParameter(param);
        }
    
        /* -------------------------------------------------------- */
        public Enumeration getInitParameterNames()
        {
            return FilterHolder.this.getInitParameterNames();
        }
    }
    
}





