// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

//
// this code is based on the published examples from log4j.
//
package org.mortbay.log4j;

import java.util.Hashtable;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootCategory;

public class CRS implements RepositorySelector 
{
    private static Hashtable __repositoryMap = new Hashtable();
    
    public synchronized LoggerRepository getLoggerRepository() 
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
        Hierarchy hierarchy = (Hierarchy) __repositoryMap.get(cl);
        if(hierarchy == null) 
        {
            
            hierarchy = new Hierarchy(new RootCategory((Level) Level.DEBUG));
            __repositoryMap.put(cl, hierarchy);
        } 
        return hierarchy;
    }
    
    public static void remove(ClassLoader cl) 
    {
        if (cl!=null)
            __repositoryMap.remove(cl); 
    } 
}

