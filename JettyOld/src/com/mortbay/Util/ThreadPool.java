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
 * @author Greg Wilkins <gregw@mortbay.com>
 * @author Juancarlo Añez <juancarlo@modelistica.com>
 */
public class ThreadPool
{
    private String _name="PoolThread";
    private int _maxThreads=0;
    private int _nThreads=0;
    private BlockingQueue jobs = new BlockingQueue();
    private int _maxIdleTimeMs=0;
    
    /* ------------------------------------------------------------ */
    public ThreadPool()
    {}
    
    /* ------------------------------------------------------------ */
    public ThreadPool(int maxThreads)
    {
	_maxThreads=maxThreads;
    }
    
    /* ------------------------------------------------------------ */
    public ThreadPool(int maxThreads, String name)
    {
	_maxThreads=maxThreads;
	_name=name;
    }
    
    /* ------------------------------------------------------------ */
    public ThreadPool(int maxThreads, String name, int maxIdleTimeMs)
    {
	_maxThreads=maxThreads;
	_maxIdleTimeMs=maxIdleTimeMs;
	_name=name;
    }

    /* ------------------------------------------------------------ */
    public int getSize()
    {
	return _nThreads;
    }
    
    /* ------------------------------------------------------------ */
    public int getMaxSize()
    {
	return _maxThreads;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param runnable 
     */
    public synchronized void run(Runnable runnable)
    {
	// Place job on job queue
	jobs.put(runnable);

	// Give the pool a chance to consume the job
	Thread.yield();
	
	// If there are jobs in the queue and we are not at our
	// maximum number of threads, create a new thread
	if (jobs.size()>0 && _maxThreads==0 || _nThreads<_maxThreads)
	{
	    new PoolThread();
	}
    }
	
    
    /* ------------------------------------------------------------ */
    /** A Thread in the pool
     */
    class PoolThread extends Thread
    {
	String _nameN;
	
	/* ------------------------------------------------------------ */
	public PoolThread()
	{
	    super();
	    _nameN=_name+"-"+_nThreads+"/";
	    super.setName(_nameN);
	    Code.debug("New ",this);
	    setDaemon(true);
	    this.start();
	}

	/* ------------------------------------------------------------ */
	/** Run a pool Thread 
	 */
 	public void run()
	{
	    try
	    {
		synchronized(ThreadPool.this)
		{
		    Code.debug("Starting PoolThread: ",this);
		    _nThreads++;
		}

		int runs=0;
		while(true)
		{
		    Runnable job =(_maxIdleTimeMs>0)
			?((Runnable) jobs.get(_maxIdleTimeMs))
			:((Runnable) jobs.get());
		    if (job == null)
			break;
		    
		    super.setName(_nameN+runs++);
		    if (Code.verbose())
			Code.debug("Thread: ",this," Handling ",job);
		    job.run();
		}
	    }
	    catch(InterruptedException e)
	    {
		Code.debug(e);
	    }
	    finally
	    {
		synchronized(ThreadPool.this)
		{
		    _nThreads--;
		    Code.debug("Exiting PoolThread: ",this);;
		}
	    }
	}
    }
};
