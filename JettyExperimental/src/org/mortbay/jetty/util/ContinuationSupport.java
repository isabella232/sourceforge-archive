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

package org.mortbay.jetty.util;

import javax.servlet.http.HttpServletRequest;
import org.mortbay.jetty.Request;

/* ------------------------------------------------------------ */
/** ContinuationSupport.
 * Conveniance class to avoid classloading visibility issues.
 * @author gregw
 *
 */
public class ContinuationSupport
{
    public static Continuation getContinuation(HttpServletRequest request, boolean create)
    {
        // TODO use reflection to make this portable
        return Request.getRequest(request).getContinuation(create);
    }
}
