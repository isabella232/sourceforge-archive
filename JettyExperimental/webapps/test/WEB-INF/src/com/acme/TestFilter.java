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

package com.acme;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/* ------------------------------------------------------------ */
/** TestFilter.
 * @author gregw
 *
 */
public class TestFilter implements Filter
{
    private ServletContext _context;
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        _context= filterConfig.getServletContext();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        long start = System.currentTimeMillis();
        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            HttpServletRequest srequest = (HttpServletRequest)request;
            _context.log((System.currentTimeMillis()-start)+"ms handling "+srequest.getRequestURI()+(srequest.getQueryString()==null?"":("?"+srequest.getQueryString())));
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
        // TODO Auto-generated method stub

    }

}
