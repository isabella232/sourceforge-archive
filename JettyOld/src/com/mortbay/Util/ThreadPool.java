// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.util.*;
import com.mortbay.Base.Code;

/* ------------------------------------------------------------ */
/** A pool of threads.
 * <p>
 * Avoids the expense of thread creation by pooling threads after
 * their run methods exit for reuse.
 * <p>
 * If the maximum pool size is reached, jobs wait for a free thread.
 * By default there is no maximum pool size.
 * <p>
 * <p><h4>Usage</h4>
 * Works well with inner classes:
 * <pre>
 * final ArgClass myArg = new ArgClass(...);
 * threadPool.run(new Runnable() { public void run() { myHandler(myArg); }} );
 * </pre>
 *
 * @version 1.0 Sun Feb 21 1999
 * @author Greg Wilkins (gregw)
 */
public class ThreadPool
{
    private int _size=0;
    private boolean waitForThread=true;
    private Stack _threads = new Stack();
    private int _nthreads=0;
    
    /* ------------------------------------------------------------ */
    public ThreadPool()
    {
	_size=0;
    }
    
    /* ------------------------------------------------------------ */
    public ThreadPool(int size)
    {
	_size=size;
    }

    /* ------------------------------------------------------------ */
    public int getSize()
    {
	return _size;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param runnable 
     */
    public void run(Runnable runnable)
    {
	synchronized(_threads)
	{
	    try
	    {
		while (true)
		{
		    // Try to use an idle thread
		    if (!_threads.empty())
		    {
			try {
			    PoolThread thread=(PoolThread)_threads.pop();
			    if (thread!=null)
				thread.run(runnable);
			    break;
			}
			catch (EmptyStackException ese)
			{Code.warning(ese);}
		    }
		    // Else try to use a new thread 
		    else if(_size==0 || _nthreads<_size)
		    {
			new PoolThread(runnable);
			break;
		    }
		    // Wait for an idle thread
		    else
			_threads.wait();
		}
	    }
	    catch (InterruptedException ie)
	    {Code.ignore(ie);}
	}
    }
    
    /* ------------------------------------------------------------ */
    /** A Thread in the pool
     */
    class PoolThread extends Thread
    {
	private Runnable _runnable=null;

	/* ------------------------------------------------------------ */
	public PoolThread(Runnable runnable)
	{
	    super("PoolThread");
	    _runnable=runnable;
	    Code.debug("New ",_runnable);
	    start();
	}
	
	/* ------------------------------------------------------------ */
	public PoolThread()
	{
	    super("PoolThread");
	    Code.debug("New ");
	    start();
	}

	/* ------------------------------------------------------------ */
	public void run(Runnable runnable)
	{
	    Code.debug("Run ",runnable);
	    synchronized(this)
	    {
		_runnable=runnable;
		notify();
	    }
	}

	/* ------------------------------------------------------------ */
	/** Run a pool Thread 
	 */
 	public void run()
	{
	    try
	    {
		// Increment count of total threads
		synchronized(_threads)
		{
		    Code.debug("New thread ",this);
		    _nthreads++;
		}
		synchronized(this)
		{
		    while (true)
		    {
			// wait for a job
			while (_runnable==null)
			{
			    Code.debug("Thread ",this," Waiting for job ...");
			    wait();
			}

			// do the job
			Code.debug("Thread: ",this," Handling ",_runnable);
			_runnable.run();
			_runnable=null;

			// place ourselves on stack of idle threads
			synchronized(_threads)
			{
			    _threads.push(this);
			    _threads.notify();
			}
		    }
		}
	    }
	    catch (InterruptedException e)
	    {
		Code.ignore(e);
	    }
	    finally
	    {
		// Decrement total thread count
		synchronized(_threads)
		{
		    Code.debug("Ending thread ",this);
		    _nthreads--;
		    _threads.notify();
		} 
	    }
	}
    }
};
