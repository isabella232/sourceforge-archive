package org.mortbay.jetty.plus;

import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;

import org.mortbay.util.LifeCycle;

/**
 * A <code>TMService</code> represents a JTA Service and is used to acces JTA
 * main interfaces (<code>UserTransaction</code> and
 * <code>TransactionManager</code>).
 * 
 * @author mhalas
 */
public interface TMService extends LifeCycle
{
	
    /**
     * returns a <code>TransactionManager</code> object.
     * 
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager();
    
    /**
     * Returns an <code>UserTransaction</code> object.
     * 
     * @return UserTransaction 
     */
    public UserTransaction getUserTransaction();
    
    /* ------------------------------------------------------------ */
    /** Start the LifeCycle.
     * @exception Exception An arbitrary exception may be thrown.
     */
    public void start()
        throws Exception;
    
    /* ------------------------------------------------------------ */
    /** Stop the LifeCycle.
     * The LifeCycle may wait for current activities to complete
     * normally, but it can be interrupted.
     * @exception InterruptedException Stopping a lifecycle is rarely atomic
     * and may be interrupted by another thread.  If this happens
     * InterruptedException is throw and the component will be in an
     * indeterminant state and should probably be discarded.
     */
    public void stop()
        throws InterruptedException;
   
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the LifeCycle has been started. 
     */
    public boolean isStarted();
}