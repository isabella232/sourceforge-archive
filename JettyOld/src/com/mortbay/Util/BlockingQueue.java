// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import java.util.Vector;


/* ------------------------------------------------------------ */
/** Blocking queue
 *
 * XXX temp implementation while waiting for java2 containers.
 * Implemented as circular buffer in a Vector. Sync is on the vector
 * to avoid double synchronization.
 *
 * @version 1.0 Fri May 28 1999
 * @author Greg Wilkins (gregw)
 */
public class BlockingQueue
{
    private Vector elements = new Vector();
    int size=0;
    int head=0;
    int tail=0;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public BlockingQueue()
    {
	elements.addElement("");
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public int size()
    {
	synchronized(elements)
	{
	    return size;
	}
    }
  
    /* ------------------------------------------------------------ */
    /** 
     * @param o 
     */
    public void put(Object o)
    {
	synchronized(elements)
	{
	    int esize=elements.size();
	    if (size==esize)
	    {
		elements.insertElementAt(o,++tail);
		if(head>=tail)
		    head++;
	    }
	    else
	    {
		if(++tail==esize)
		    tail=0;
		elements.setElementAt(o,tail);
	    }
	    size++;
	    elements.notify();
	}
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public Object get()
	throws InterruptedException
    {
	synchronized(elements)
	{
	    while (size==0)
	    {
		elements.wait();
	    }
	    Object o = elements.elementAt(head++);
	    if(head==elements.size())
		head=0;
	    size--;
	    return o;
	}
    }
    
	
    /* ------------------------------------------------------------ */
    /** 
     * @param timeout 
     * @return 
     */
    public Object get(long timeout)
	throws InterruptedException
    {
	synchronized(elements)
	{
	    if (size==0)
		elements.wait(timeout);
	    if (size==0)
		return null;
	    
	    Object o = elements.elementAt(head++);
	    if(head==elements.size())
		head=0;
	    size--;
	    return o;
	}
    }
}








