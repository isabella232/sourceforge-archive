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

package org.mortbay.io;

/* ------------------------------------------------------------ */
/** SimpleBuffers.
 * Simple implementation of Buffers holder.
 * @author gregw
 *
 */
public class SimpleBuffers implements Buffers
{
    Buffer _big;
    Buffer _small;
    boolean _bigOut;
    boolean _smallOut;
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public SimpleBuffers(Buffer small,Buffer big)
    {
        _big=big;
        _small=small;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.Buffers#getBuffer(boolean)
     */
    public Buffer getBuffer(boolean big)
    {
        if (big)
        {
            if (_bigOut)
                Portable.throwIllegalState("Big buffer in use");
            _bigOut=true;
            return _big;
        }
        else
        {
            if (_smallOut)
                Portable.throwIllegalState("Small buffer in use");
            _smallOut=true;
            return _small;
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.Buffers#returnBuffer(org.mortbay.io.Buffer)
     */
    public void returnBuffer(Buffer buffer)
    {
        if (buffer==_big)
            _bigOut=false;
        if (buffer==_small)
            _smallOut=false;
        buffer.clear();
    }
    
    public String toString()
    {
        return "[big="+_big+",small="+_small+"]";
    }

}
