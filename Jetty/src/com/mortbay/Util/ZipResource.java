// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
//import com.sun.java.util.collections.*; XXX-JDK1.1
    
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** ZIP resource.
 * Find a system resource which is treated as a zip input stream.
 * Entries are read at construction and byte arrays read on demand.
 *
 *
 * @see
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ZipResource
{
    String _responseName;
    Map _entryMap = new HashMap();
    Map _byteMap = new HashMap();
	
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param resouceName 
     * @exception IOException 
     * @exception IllegalArgumentException 
     */
    public ZipResource(String resouceName)
	throws IOException, IllegalArgumentException
    {
	if (Code.verbose(9)) Code.debug("ZipResource ",resouceName);
	
	_responseName=resouceName;

	// Check that it exists
	InputStream in = ClassLoader.getSystemResourceAsStream(resouceName);
	if (in==null)
	    throw new IllegalArgumentException("No such resource");

	// Load the index
	ZipInputStream zipin = new ZipInputStream(in);
	ZipEntry entry=null;
	while((entry=zipin.getNextEntry())!=null)
	{
	    if (Code.verbose(99)) Code.debug(entry.getName());
	    _entryMap.put(entry.getName(),entry);
	}
	in.close();
    }

    /* ------------------------------------------------------------ */
    public Set getNames()
    {
	return _entryMap.keySet();
    }
    
    /* ------------------------------------------------------------ */
    public Collection getEntries()
    {
	return _entryMap.values();
    }
    
    /* ------------------------------------------------------------ */
    public ZipEntry getEntry(String name)
    {
	return (ZipEntry)_entryMap.get(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the bytes of a ZIP entry.
     * @param name 
     * @return byte array or null if not found/
     */
    public byte[] getBytes(String name)
	throws IOException
    {
	// Is it already loaded?
	byte[] b = (byte[])_byteMap.get(name);
	if (b!=null)
	    return b;

	// Is it in the index?
	ZipEntry entry=(ZipEntry)_entryMap.get(name);
	if (entry==null)
	    return null;

	// find it in stream.
	InputStream in = ClassLoader.getSystemResourceAsStream(_responseName);
	ZipInputStream zipin = new ZipInputStream(in);
	while((entry=zipin.getNextEntry())!=null)
	{
	    if (entry.getName().equals(name))
	    {
		if (Code.verbose(9)) Code.debug("Loading ",name);

		// found the entry
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IO.copy(zipin,out);
		b = out.toByteArray();
		_byteMap.put(name,b);
		break;
	    }
	}
	return b;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @return 
     * @exception IOException 
     */
    public InputStream getInputStream(String name)
    {
	try
	{
	    byte[] b = getBytes(name);
	    if (b==null)
		return null;
	    
	    return new ByteArrayInputStream(b);
	}
	catch(IOException e)
	{
	    Code.ignore(e);
	}
	return null;
    }
    
}

    







