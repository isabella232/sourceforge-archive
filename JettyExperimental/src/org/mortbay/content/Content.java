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

package org.mortbay.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mortbay.io.Buffer;

/**
 * @author gregw
 *
 */
public interface Content
{
    public String getMimeType();
    public String getContentEncoding();
    public int getLength();
    public long getLastModified();
    public long getExpiry();
    
    public Buffer get(long offset, long length);
    public int put(Buffer buf);
    
    public void write(OutputStream out) throws IOException;
    public void read(InputStream in) throws IOException;
}
