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

package org.mortbay.util;

import java.util.List;

/* ------------------------------------------------------------ */
/** Container.
 * @author gregw
 *
 */
public interface Container
{
    public void add(Object component);
    public void add(int index,Object component);
    public void remove(Object component);
    public void remove(int index);
    public Object getComponent(int index);
    public List getComponents();
    public void setComponents(List components);
    public int size();
}
