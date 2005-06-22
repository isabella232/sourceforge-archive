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

import java.io.IOException;

import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.UserRealm;

/* ------------------------------------------------------------ */
/** BasicAuthenticator.
 * @author gregw
 *
 */
public class BasicAuthenticator extends org.mortbay.http.BasicAuthenticator
{

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.http.BasicAuthenticator#sendChallenge(org.mortbay.http.UserRealm, org.mortbay.http.HttpResponse)
     */
    public void sendChallenge(UserRealm realm, HttpResponse response) throws IOException
    {
        response.setField(HttpFields.__WwwAuthenticate,"basic realm=\""+realm.getName()+'"');

        ServletHttpResponse sresponse = (ServletHttpResponse) response.getWrapper();
        if (sresponse!=null)
            sresponse.sendError(HttpResponse.__401_Unauthorized);
        else
            response.sendError(HttpResponse.__401_Unauthorized);
    }
}
