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
 * @version $Id$
 * @author Juancarlo Añez <juancarlo@modelistica.com>
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class ThreadPool
{
    private String _name="PoolThread";
    private int _minThreads=0;
    private int _maxThreads=0;
    private int _threadID=0;
    private int _nThreads=0;
    private BlockingQueue jobs;
    private int _maxIdleTimeMs=0;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ThreadPool()
    {
	this(0);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param maxThreads Maximum number of handler threads.
     */
    public ThreadPool(int maxThreads)
    {
	_minThreads=0;
	_maxThreads=maxThreads;
	jobs=new BlockingQueue(_maxThreads);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param maxThreads Maximum number of handler threads.
     * @param name Name of the thread.
     */
    public ThreadPool(int maxThreads, String name)
    {
	_minThreads=0;
	_maxThreads=maxThreads;
	_name=name;
	jobs=new BlockingQueue(_maxThreads);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param maxThreads Maximum number of handler threads.
     * @param name  Name of the thread.
     * @param maxIdleTimeMs Idle time in milliseconds before a handler thread dies.
     */
    public ThreadPool(int maxThreads, String name, int maxIdleTimeMs)
    {
	_minThreads=0;
	_maxThreads=maxThreads;
	_maxIdleTimeMs=maxIdleTimeMs;
	if (name!=null)
	    _name=name;
	jobs=new BlockingQueue(_maxThreads);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param minThreads  Minimum number of handler threads.
     * @param maxThreads Maximum number of handler threads.
     * @param name Name of the thread
     * @param maxIdleTimeMs Idle time in milliseconds before a handler thread dies.
     */
    public ThreadPool(int minThreads,
		      int maxThreads,
		      String name,
		      int maxIdleTimeMs)
    {
	_minThreads=minThreads;
	_maxThreads=maxThreads;
	_maxIdleTimeMs=maxIdleTimeMs;
	if (name!=null)
	    _name=name;
	jobs=new BlockingQueue(_maxThreads);
	for (int i=_minThreads;i-->0;)
	{
	    _nThreads++;
	    new PoolThread();
	}
    }

    /* ------------------------------------------------------------ */
    public int getSize()
    {
	return _nThreads;
    }
    
    /* ------------------------------------------------------------ */
    public int getMinSize()
    {
	return _minThreads;
    }
    
    /* ------------------------------------------------------------ */
    public int getMaxSize()
    {
	return _maxThreads;
    }
    
    /* ------------------------------------------------------------ */
    /** Run job 
     * @param job.  If the job is derived from Runnable, the run method
     * is called, otherwise it is passed as the argument to the handle
     * method.
     */
    public void run(Object job)
	throws InterruptedException
    {
	// Place job on job queue
	jobs.put(job);

	// Give the pool a chance to consume the job
	Thread.yield();
	
	// If there are jobs in the queue and we are not at our
	// maximum number of threads, create a new thread
	if (jobs.size()>0 && (_maxThreads==0 || _nThreads<_maxThreads))
	{
	    synchronized(this)
	    {
		if (jobs.size()>0 && (_maxThreads==0 || _nThreads<_maxThreads))
		{
		    _nThreads++;
		    new PoolThread();
		}
	    }
	}
    }

    /* ------------------------------------------------------------ */
    /** Handle a job.
     * This method must be specialized by a derived class if non
     * Runnable jobs are given to the threads in the pool.
     * @param job 
     */
    protected void handle(Object job)
    {
	throw new Error("handle must be overridden");
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
	    _nameN=_name+"-"+(_threadID++);
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
		Code.debug("Running PoolThread: ",this);

		int runs=0;
		while(true)
		{
		    Object job =(_maxIdleTimeMs>0)
			?jobs.get(_maxIdleTimeMs)
			:jobs.get();
		    
		    if (job == null)
		    {
			if (_nThreads>_minThreads)
			    break;
			continue;
		    }

		    if (Code.debug())
		    {
			super.setName(_nameN+"/"+runs++);
			if (Code.verbose())
			    Code.debug("Thread: ",this," Handling ",job);
		    }

		    if (job instanceof Runnable)
			((Runnable)job).run();
		    else
			ThreadPool.this.handle(job);
		    Thread.yield();
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



