// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Servlet Class Loader.
 * 
 *
 * <h4>Notes</h4>
 * The load search order is:<NL>
 * <LI>Loader cache.
 * <LI>If the class starts with a systemClass package name,
 *     load from builtin loader or fail.
 * <LI>Try the loader path.
 * <LI>Try the builtin loader. Classes found here may not be reloaded.
 *
 * @version 1.0 Tue May  4 1999
 * @author Greg Wilkins (gregw)
 */
public abstract class ServletLoader extends ClassLoader
{
    /* ------------------------------------------------------------ */
    /** Load a class.
     * @param name Class name (without ".class");
     * @param resolve True if the class should be resolved when loaded. 
     * @return The Class instance
     * @exception ClassNotFoundException 
     */
    abstract public Class loadClass(String name)
	throws ClassNotFoundException;
    

    /* ------------------------------------------------------------ */
    /** Load a class.
     * @param name Class name (without ".class");
     * @param resolve True if the class should be resolved when loaded. 
     * @return The Class instance
     * @exception ClassNotFoundException 
     */
    public Class loadClass(String name, boolean resolve)
	throws ClassNotFoundException
    {
	Class c = loadClass(name);
	
	// resolve
	if (resolve)
	    resolveClass(c);
	return c;
    }

    /* ------------------------------------------------------------ */
    /** Return true a class is modified.
     * @return true if any of the classes loaded by this loader have been
     * modified since their load time.
     */
    abstract public boolean isModified();
    
};






