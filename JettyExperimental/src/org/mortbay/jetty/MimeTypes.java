// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;

/**
 * 
 * @version $Revision$
 * @author gregw
 */
public class MimeTypes extends BufferCache
{
    public final static String
        TEXT_PLAIN="text/plain",
        TEXT_HTML="text/html";

    private static int index=1;
    public final static int
        TEXT_PLAIN_ORDINAL=index++,
        TEXT_HTML_ORDINAL=index++;

    public final static MimeTypes CACHE= new MimeTypes();

    public final static Buffer 
        TEXT_PLAIN_BUFFER=CACHE.add(TEXT_PLAIN,TEXT_PLAIN_ORDINAL),
        TEXT_HTML_BUFFER=CACHE.add(TEXT_HTML,TEXT_HTML_ORDINAL);
       
}
