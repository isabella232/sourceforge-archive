package org.mortbay.util;

import java.io.InterruptedIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* ------------------------------------------------------------ */
/** Base Thread class implementing LifeCycle.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public abstract class LifeCycleThread implements LifeCycle, Runnable
{
    private static Log log = LogFactory.getLog(LifeCycleThread.class);

    private boolean _running;
    private boolean _daemon ;
    private Thread _thread;
    
    /* ------------------------------------------------------------ */
    public boolean isDaemon()
    {
        return _daemon;
    }
    
    /* ------------------------------------------------------------ */
    public void setDaemon(boolean d)
    {
        _daemon = d;
    }
    
    /* ------------------------------------------------------------ */
    public Thread getThread()
    {
        return _thread;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _running;
    }

    /* ------------------------------------------------------------ */
    public synchronized void start()
        throws Exception
    {
        if (_running)
        {
            log.debug("Already started");
            return;
        }
        _running=true;
        if (_thread==null)
        {
            _thread=new Thread(this);
            _thread.setDaemon(_daemon);
        }
        _thread.start();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     */
    public synchronized void stop()
        throws InterruptedException
    {
        _running=false;
        if (_thread!=null)
        {
            _thread.interrupt();
            _thread.join();
        }
    }
    

    /* ------------------------------------------------------------ */
    /** 
     */
    public final void run()
    {
        try
        {
            while(_running)
            {
                try
                {
                    loop();
                }
                catch(InterruptedException e)
                {
                    LogSupport.ignore(log,e);
                }
                catch(InterruptedIOException e)
                {
                    LogSupport.ignore(log,e);
                }
                catch(Exception e)
                {
                    if (exception(e))
                        break;
                } 
                catch(Error e)
                {
                    if (error(e))
                        break;
                }   
            }
        }
        finally
        {
            _running=false;
        }
    }

    /* ------------------------------------------------------------ */
    /** Handle exception from loop.
     * @param e The exception
     * @return True of the loop should continue;
     */
    public boolean exception(Exception e)
    {
        log.warn(LogSupport.EXCEPTION,e);
        return true;
    }
    
    /* ------------------------------------------------------------ */
    /** Handle error from loop.
     * @param e The exception
     * @return True of the loop should continue;
     */
    public boolean error(Error e)
    {
        log.warn(LogSupport.EXCEPTION,e);
        return true;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @exception InterruptedException 
     * @exception InterruptedIOException 
     */
    public abstract void loop()
    throws InterruptedException,
           InterruptedIOException,
           Exception;
    
}
