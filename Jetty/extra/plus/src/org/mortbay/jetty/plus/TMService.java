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
public abstract class TMService extends AbstractService
{
    /**
     * Default value for UserTransaction JNDI binding. User can
     * change this by calling setJNDI()
     */
    public static final String  DEFAULT_USER_TX_JNDI = "javax.transaction.UserTransaction";


    /**
     * Value for the TransactionManager JNDI binding. This is not
     * changeable at runtime because other services need to know how to look it up.
     */
    protected String _transactionManagerJNDI = "javax.transaction.TransactionManager";
	

    public TMService ()
    {
        //set up the UserTransaction JNDI binding name
        setJNDI (DEFAULT_USER_TX_JNDI);
    }

    /**
     * returns a <code>TransactionManager</code> object.
     * 
     * @return TransactionManager
     */
    public abstract TransactionManager getTransactionManager();
    
    /**
     * Returns an <code>UserTransaction</code> object.
     * 
     * @return UserTransaction 
     */
    public abstract UserTransaction getUserTransaction();

    
    public String getTransactionManagerJNDI ()
    {
        return _transactionManagerJNDI;
    }
}
