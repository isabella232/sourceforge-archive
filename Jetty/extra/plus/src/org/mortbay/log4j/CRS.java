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
	__repositoryMap.remove(cl); 
    } 
}

