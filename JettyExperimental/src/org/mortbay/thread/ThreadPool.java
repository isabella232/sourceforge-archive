// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.thread;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/* ------------------------------------------------------------ */
/** A pool of threads.
 * <p>
 * Avoids the expense of thread creation by pooling threads after
 * their run methods exit for reuse.
 * <p>
 * If the maximum pool size is reached, jobs wait for a free thread.
 * By default there is no maximum pool size.  Idle threads timeout
 * and terminate until the minimum number of threads are running.
 * <p>
 * This implementation uses the run(Object) method to place a
 * job on a queue, which is read by the getJob(timeout) method.
 * Derived implementations may specialize getJob(timeout) to
 * obtain jobs from other sources without queing overheads.
 *
 * @version $Id$
 * @author Juancarlo A�ez <juancarlo@modelistica.com>
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class ThreadPool implements LifeCycle, Serializable
{
    private static int __id;

    private final String _jobLock = "JOB";
    private final String _joinLock = "JOIN";
    
    private boolean _daemon;
    private int _maxIdleTimeMs=10000;
    private int _maxThreads=255;
    private int _minThreads=1;
    private String _name;
    int _priority= Thread.NORM_PRIORITY;
    private boolean _queue;
    private int _blockMs=10000;

    private transient int _id;
    private transient int _idle;
    private transient List _blocked;
    private transient List _jobs;
    private transient boolean _started;
    private transient Set _threads;

    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadPool()
    {
        _name= this.getClass().getName();
        int dot= _name.lastIndexOf('.');
        if (dot >= 0)
            _name= _name.substring(dot + 1);
        _name=_name+__id++;
    }

    /* ------------------------------------------------------------ */
    /** Get the number of idle threads in the pool.
     * @see #getThreads
     * @return Number of threads
     */
    public int getIdleThreads()
    {
        return _idle;
    }

    /* ------------------------------------------------------------ */
    /** Get the maximum thread idle time.
     * Delegated to the named or anonymous Pool.
     * @see #setMaxIdleTimeMs
     * @return Max idle time in ms.
     */
    public int getMaxIdleTimeMs()
    {
        return _maxIdleTimeMs;
    }

    /* ------------------------------------------------------------ */
    /** Set the maximum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #setMaxThreads
     * @return maximum number of threads.
     */
    public int getMaxThreads()
    {
        return _maxThreads;
    }

    /* ------------------------------------------------------------ */
    /** Get the minimum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #setMinThreads
     * @return minimum number of threads.
     */
    public int getMinThreads()
    {
        return _minThreads;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return The name of the ThreadPool.
     */
    public String getName()
    {
        return _name;
    }

    /* ------------------------------------------------------------ */
    /** Get the number of threads in the pool.
     * @see #getIdleThreads
     * @return Number of threads
     */
    public int getThreads()
    {
        return _threads.size();
    }

    /* ------------------------------------------------------------ */
    /** Get the priority of the pool threads.
     *  @return the priority of the pool threads.
     */
    public int getThreadsPriority()
    {
        return _priority;
    }

    /* ------------------------------------------------------------ */
    /** Handle a job.
     * Called by the allocated thread to handle a job. If the job is a
     * Runnable, it's run method is called. Otherwise this method needs to be
     * specialized by a derived class to provide specific handling.
     * @param job The job to execute.
     * @exception InterruptedException 
     */
    protected void handle(Object job) throws InterruptedException
    {
        if (job != null && job instanceof Runnable)
             ((Runnable)job).run();
    }

    /* ------------------------------------------------------------ */
    /** 
     * Delegated to the named or anonymous Pool.
     */
    public boolean isDaemon()
    {
        return _daemon;
    }

    /* ------------------------------------------------------------ */
    /** Is the pool running jobs.
     * @return True if start() has been called.
     */
    public boolean isStarted()
    {
        return _started;
    }

    /* ------------------------------------------------------------ */
    public void join() throws InterruptedException
    {
        synchronized (_joinLock)
        {
            while (isStarted())
                _joinLock.wait(getMaxIdleTimeMs());
        }
    }

    protected void newThread()
    {
        synchronized(_jobLock)
        {
            Thread thread =new PoolThread();
            _threads.add(thread);
            _idle++;
            thread.setName(_name+"-"+_id++);
            thread.start();   
        }
    }

    /* ------------------------------------------------------------ */
    /** Run job.
     * Give a job to the pool. 
     * @param job  If the job is derived from Runnable, the run method
     * is called, otherwise it is passed as the argument to the handle
     * method.
     * @param waitMs
     * @return true if the job was given to a thread, false if no thread was
     * available.
     */
    public boolean run(Object job) 
    {
        boolean queued=false;
        synchronized(_jobLock)
        {	
            if (!_started)
                return false;
            
            int blockMs = _blockMs;
            
            // Wait for an idle thread!
            while (_idle-_jobs.size()<=0)
            {
                // Are we at max size?
                if (_threads.size()<_maxThreads)
                {    
                    newThread();
                    break;
                }
                 
                // Can we queue?
                if (_queue)
                    break;
                
                // pool is full
                if (blockMs<0)
                    return false;
                    
                // Block waiting
                try
                {
                    _blocked.add(Thread.currentThread());
                    _jobLock.wait(blockMs);
                    blockMs=-1;
                }
                catch (InterruptedException ie)
                {}
            }

            _jobs.add(job);
            queued=true;
            _jobLock.notify();
            Thread.yield(); 
        }
        
        return queued;
    }

    /* ------------------------------------------------------------ */
    /** 
     * Delegated to the named or anonymous Pool.
     */
    public void setDaemon(boolean daemon)
    {
        _daemon=daemon;
    }

    /* ------------------------------------------------------------ */
    /** Set the maximum thread idle time.
     * Threads that are idle for longer than this period may be
     * stopped.
     * Delegated to the named or anonymous Pool.
     * @see #getMaxIdleTimeMs
     * @param maxIdleTimeMs Max idle time in ms.
     */
    public void setMaxIdleTimeMs(int maxIdleTimeMs)
    {
        _maxIdleTimeMs=maxIdleTimeMs;
    }

    /* ------------------------------------------------------------ */
    /** Set the maximum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #getMaxThreads
     * @param maxThreads maximum number of threads.
     */
    public void setMaxThreads(int maxThreads)
    {
        _maxThreads=maxThreads;
    }

    /* ------------------------------------------------------------ */
    /** Set the minimum number of threads.
     * Delegated to the named or anonymous Pool.
     * @see #getMinThreads
     * @param minThreads minimum number of threads
     */
    public void setMinThreads(int minThreads)
    {
        _minThreads=minThreads;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name Name of the ThreadPool to use when naming Threads.
     */
    public void setName(String name)
    {
        _name= name;
    }

    /* ------------------------------------------------------------ */
    /** Set the priority of the pool threads.
     *  @param priority the new thread priority.
     */
    public void setThreadsPriority(int priority)
    {
        _priority=priority;
    }

    /* ------------------------------------------------------------ */
    /* Start the ThreadPool.
     * Construct the minimum number of threads.
     */
    public void start() throws Exception
    {
        _started= true;
        _threads=new HashSet();
        _jobs=new LinkedList();
        _blocked=new LinkedList();
        _idle=0;
        
        for (int i=0;i<_minThreads;i++)
        {
            newThread();
        }
        
    }

    /* ------------------------------------------------------------ */
    /** Stop the ThreadPool.
     * New jobs are no longer accepted,idle threads are interrupted
     * and stopJob is called on active threads.
     * The method then waits 
     * min(getMaxStopTimeMs(),getMaxIdleTimeMs()), for all jobs to
     * stop, at which time killJob is called.
     */
    public void stop() throws InterruptedException
    {
        _started= false;
        
        // TODO STOP!
        
        synchronized (_joinLock)
        {
            _joinLock.notifyAll();
        }
    }

    /* ------------------------------------------------------------ */
    /** Stop a Job.
     * This method is called by the Pool if a job needs to be stopped.
     * The default implementation does nothing and should be extended by a
     * derived thread pool class if special action is required.
     * @param thread The thread allocated to the job, or null if no thread allocated.
     * @param job The job object passed to run.
     */
    protected void stopJob(Thread thread, Object job)
    {
        thread.interrupt();
    }
    
    

    /* ------------------------------------------------------------ */
    /** Pool Thread class.
     * The PoolThread allows the threads job to be
     * retrieved and active status to be indicated.
     */
    public class PoolThread extends Thread 
    {
        Object _job=null;
        
        PoolThread()
        {
            setDaemon(_daemon);
            setPriority(_priority);
        }
        
        /* ------------------------------------------------------------ */
        /** ThreadPool run.
         * Loop getting jobs and handling them until idle or stopped.
         */
        public void run()
        {
            try
            {
                while (_started)
                {
                    _job=null;
                    
                    try
                    {
                        synchronized (_jobLock)
                        {
                            while(_jobs.size()==0 && _started)
                                _jobLock.wait();
                            if (_jobs.size()>0 && _started)
                                _job=_jobs.remove(0);
                            if (_job!=null)
                                _idle--;
                        }
                        
                        if (_started && _job!=null)
                            handle(_job);
                    }
                    catch (InterruptedException e) {}
                    finally
                    {
                        synchronized (_jobLock)
                        {
                            if (_job!=null)
                                _idle++;
                            _job=null;
                            if (_blocked.size()>0)
                                ((Thread)_blocked.remove(0)).interrupt();
                        }
                    }
                }
            }
            finally
            {
                synchronized (_jobLock)
                {
                    _threads.remove(this);
                }
            }
        }
    }
}
