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
    private final String _jobLock="l1";
    private final String _clearLock="l2";
    private String _name="PoolThread";
    private int _minThreads=0;
    private int _maxThreads=0;
    private int _threadID=0;
    private int _nThreads=0;
    private int _blockedThreads=0;
    private Object _job=null;
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
    public synchronized void run(Object job)
        throws InterruptedException
    {
        if (job==null)
        {
            Code.warning("Null Job");
            return;
        }

        // Wait for last job to be consumed
        if(_job!=null)
        {
            synchronized(_clearLock)
            {
                while(_job!=null)
                {
                    _clearLock.wait();
                }
            }
        }
        
        // Setup job for collection
        synchronized(_jobLock)
        {
            _job=job;
            
            if (_blockedThreads==0 &&
                (_maxThreads==0 || _nThreads<_maxThreads))
                new PoolThread();
            else
                _jobLock.notify();
        }
    }

    /* ------------------------------------------------------------ */
    /** Handle a job.
     * Unless the job is an instance of Runnable, then
     * this method must be specialized by a derived class.
     * @param job 
     */
    protected void handle(Object job)
    {
        if (job!=null && job instanceof Runnable)
            ((Runnable)job).run();
        else
            Code.warning("Invalid job: "+job);
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
            synchronized(ThreadPool.this)
            {
                _nThreads++;
                _nameN=_name+"-"+(_threadID++);
            }
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
            Object myJob=null;
            try
            {
                Code.debug("Running PoolThread: ",this);

                int runs=0;
                while(true)
                {
                    // Sync on the lock to get a job
                    synchronized(_jobLock)
                    {
                        // If no job ready...
                        while (_job==null)
                        {
                            // Wait for one
                            try{
                                _blockedThreads++;
                                if (Code.verbose())
                                    Code.debug("Thread: ",this,
                                               " waiting "+_maxIdleTimeMs);

                                // Die if we are idle
                                if (_maxIdleTimeMs>0)
                                {
                                    _jobLock.wait(_maxIdleTimeMs);
                                    if (_job==null && _nThreads>_minThreads)
                                        return;
                                }
                                else
                                    _jobLock.wait();
                            }
                            finally
                            {
                                _blockedThreads--;
                            }
                        }
                        myJob=_job;
                        synchronized(_clearLock)
                        {
                            _job=null;
                            _clearLock.notify();
                        }
                        
                        // name the thread
                        if (Code.debug())
                        {
                            super.setName(_nameN+"/"+runs++);
                            if (Code.verbose())
                                Code.debug("Thread: ",this,
                                           " Handling ",myJob);
                        }
                    }

                    // Handle the job
                    ThreadPool.this.handle(myJob);
                    myJob=null;
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



