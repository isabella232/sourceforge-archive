// ========================================================================
// $Id$
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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


package org.mortbay.http.handler;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.mortbay.log.LogFactory;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

/**
 * Handler to force MSIE SSL connections to not be persistent to
 * work around MSIE5 bug.
 *  
 * @author gregw
 * @author chris haynes
 *
 */
public class MsieSslHandler extends AbstractHttpHandler
{
    private static Log log = LogFactory.getLog(MsieSslHandler.class);
    
    private String userAgentSubString="MSIE 5";
    
    /* 
     * @see org.mortbay.http.HttpHandler#handle(java.lang.String, java.lang.String, org.mortbay.http.HttpRequest, org.mortbay.http.HttpResponse)
     */
    public void handle(
        String pathInContext,
        String pathParams,
        HttpRequest request,
        HttpResponse response)
        throws HttpException, IOException
    {
        String userAgent = request.getField(HttpFields.__UserAgent);
        
        if(userAgent != null &&  
           userAgent.indexOf( userAgentSubString)>=0 &&
           HttpRequest.__SSL_SCHEME.equalsIgnoreCase(request.getScheme()))
        {
            if (log.isDebugEnabled())
                log.debug("Force close");
            response.setField(HttpFields.__Connection, HttpFields.__Close);
            request.getHttpConnection().forceClose();
        }
    }
    
    /**
     * @return The substring to match against the User-Agent field
     */
    public String getUserAgentSubString()
    {
        return userAgentSubString;
    }

    /**
     * @param string The substring to match against the User-Agent field
     */
    public void setUserAgentSubString(String string)
    {
        userAgentSubString= string;
    }

}
