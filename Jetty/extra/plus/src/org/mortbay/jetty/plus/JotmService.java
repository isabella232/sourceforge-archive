package org.mortbay.jetty.plus;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import javax.sql.XADataSource;
import org.mortbay.jndi.Util;
import org.mortbay.util.Code;
import org.mortbay.util.Log;


import org.enhydra.jdbc.standard.StandardXADataSource;
import org.enhydra.jdbc.pool.StandardXAPoolDataSource;

/**
 * Implementation of TMService for Objectweb JOTM (www.objectweb.org)
 * 
 * @author mhalas
 */
public class JotmService extends TMService
{
   
    public static final String DEFAULT_SERVICE_NAME = "JotmService";

   /**
     * Instance of JOTM transaction manager. 
     */
    protected org.objectweb.transaction.jta.TMService m_tm;

    /**
     * Global data sources specified in server.xml
     */
    protected Map m_mpDataSources;
    
    public JotmService()
    {
       m_tm = null;
       // We can use HashMap because it will be only read
       // since all global data sources are created on startup
       m_mpDataSources = new HashMap();

       setName (DEFAULT_SERVICE_NAME);
    }
		 
    /**
     * returns a <code>TransactionManager</code> object.
     * 
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager()
    {
       if (m_tm == null)
       {
          return null;
       }
       else
       {
          return m_tm.getTransactionManager();
       }
    }
    
    /**
     * Returns an <code>UserTransaction</code> object.
     * 
     * @return UserTransaction 
     */
    public UserTransaction getUserTransaction()
    {
       if (m_tm == null)
       {
          return null;
       }
       else
       {
          return m_tm.getUserTransaction();
       }
    }
    
    /* ------------------------------------------------------------ */
    /** Start the LifeCycle.
     * @exception Exception An arbitrary exception may be thrown.
     */
    public void start()
        throws Exception
    {
        if (!isStarted())
        {
            
            Log.event("Starting JoTM transaction manager.");
            
            // Start the transaction manager
            try
            {
                if (m_tm == null)
                    m_tm = new org.objectweb.jotm.Jotm(true, true);
            }
            catch(Exception eExc)
            {
                Code.warning(eExc);
                throw new IOException("Failed to start JoTM: " + eExc);
            }

            //  Register the user transaction and transaction mgr objects in JNDI
            Context ictx = null;

            
            try 
            {
                ictx = new InitialContext();
                Code.debug("InitialContext instanceof "+ictx.getClass().getName());
                Code.debug("java.naming.factory.initial="+System.getProperty("java.naming.factory.initial"));
            } 
            catch (NamingException e) 
            {
                Code.warning(e);
                throw new IOException("No initial context: "+e);
            }
            
            try 
            {
                Util.bind(ictx, getJNDI(), m_tm.getUserTransaction());
                Code.debug("UserTransaction object bound in JNDI with name " + getJNDI());
            }
            catch (NamingException e) 
            {
                Code.warning(e);
                throw new IOException("UserTransaction rebind failed :" + e.getExplanation());
            }
 
            try
            {
                Util.bind(ictx, getTransactionManagerJNDI(), m_tm.getTransactionManager());
                Code.debug("TransactionManager object bound in JNDI with name " + getTransactionManagerJNDI());
            } 
            catch (NamingException e) 
            {
                Code.warning(e);
                throw new IOException("TransactionManager rebind failed :" + e.getExplanation());
            }
            
            
            // Now take any existing data sources and register them with JNDI
            // NB: it won't be necessary to call setTransactionManager() on the DataSource
            // or Pool because they both do a JNDI lookup to find a TransactionManager with
            // which to enrol their transactions when necessary 
            XADataSource xadsDataSource;
            Iterator             itrDataSources;
            TransactionManager   tmManager;
            Map.Entry            meDataSource;
            String               strDataSourceName;
            
            tmManager = m_tm.getTransactionManager();
            for (itrDataSources = m_mpDataSources.entrySet().iterator();
                 itrDataSources.hasNext();)
            {
                meDataSource = (Map.Entry)itrDataSources.next();
                strDataSourceName = (String)meDataSource.getKey();
                Object o = meDataSource.getValue();

                if (o instanceof StandardXAPoolDataSource)
                {
                    StandardXAPoolDataSource xapdsPoolDataSource = (StandardXAPoolDataSource)meDataSource.getValue();
                    xadsDataSource = xapdsPoolDataSource.getDataSource();
                
		    if (m_tm != null)
		    {
                        xapdsPoolDataSource.setTransactionManager(m_tm.getTransactionManager());
			((StandardXADataSource)xadsDataSource).setTransactionManager(m_tm.getTransactionManager());
		    }

                    //bind both the Pool and the DataSource
                    try 
                    {
                        Util.bind(ictx, "XA"+ strDataSourceName, xadsDataSource);
                        Code.debug("XA Data source bound in JNDI with name XA" + strDataSourceName);
                        Util.bind(ictx, strDataSourceName, xapdsPoolDataSource);
                        Code.debug("Data Source Pool bound in JNDI with name " + strDataSourceName);
                    } 
                    catch (NamingException e) 
                    {
                        Code.debug("Data source rebind failed :" + e.getExplanation());
                        Code.warning(e);
                        throw e;
                    }
                }
                else if (o instanceof StandardXADataSource)
                {
                    //bind only the DataSource
                    xadsDataSource = (StandardXADataSource)o; 
                    
		    if (m_tm != null)
		    {
                        ((StandardXADataSource)xadsDataSource).setTransactionManager(m_tm.getTransactionManager());
		    }

                    try
                    {
                        Util.bind(ictx, strDataSourceName, xadsDataSource);
                        Code.debug("Data Source bound in JNDI with name "+ strDataSourceName);
                    }
                    catch (NamingException e)
                    {
                        Code.debug("Data source rebind failed : "+e.getExplanation());
                        Code.warning(e);
                        throw e;
                    }
                }
                else
                    throw new IllegalStateException (o + " is not a StandardDataSource");
            }

            super.start();
            
            Log.event("JoTM is running.");
        }
        else
            Log.event ("JoTM is already running");
    }



    
    /* ------------------------------------------------------------ */
    /** Stop the LifeCycle.
     * The LifeCycle may wait for current activities to complete
     * normally, but it can be interrupted.
     * @exception InterruptedException Stopping a lifecycle is rarely atomic
     * and may be interrupted by another thread.  If this happens
     * InterruptedException is throw and the component will be in an
     * indeterminant state and should probably be discarded.
     */
    public void stop(
    ) throws InterruptedException
    {
       if (!isStarted())
       {
          Log.event("Stopping JoTM...");
          m_tm.stop();
          super.stop();
          Log.event("JoTM is stopped.");
       }
       else
       {
          Code.warning("No JoTM to stop.");
       }
    }



   

    /* ------------------------------------------------------------ */
    /**
     * Add a datasource and a pool for it to the Transaction Mgr
     *
     * @param jndiName client lookup jndi of DataSource
     * @param xaDataSource the DataSource
     * @param xaPool the Pool
     * @exception SQLException if an error occurs
     * @exception NamingException if an error occurs
     */
    public void addDataSource (String dsJNDIName, 
                               StandardXADataSource xaDataSource, 
                               StandardXAPoolDataSource xaPool)
        throws SQLException, NamingException
    {
        // set up username and password for pool
        xaPool.setUser(xaDataSource.getUser());
        xaPool.setPassword(xaDataSource.getPassword());
        
        // set up the JNDI name of the datasource in the pool
        xaPool.setDataSourceName ("XA"+dsJNDIName);

        // add the datasource to the pool
        xaPool.setDataSource (xaDataSource);

        // add to map of datasources
        m_mpDataSources.put(dsJNDIName, xaPool);

        Log.event("Pooled data source: " +dsJNDIName+" configured");
    }



    /* ------------------------------------------------------------ */
    /**
     * Add a DataSource that does not have an associated pool.
     * You should only use this if the driver for the datasource does it's
     * own pooling.
     *
     * @param dsJNDIName a <code>String</code> value
     * @param xaDataSource a <code>StandardXADataSource</code> value
     */
    public  void addDataSource (String dsJNDIName,
                                StandardXADataSource xaDataSource)
    {  
        // add to map of datasources
        m_mpDataSources.put(dsJNDIName, xaDataSource);
        Log.event("Data source: " +dsJNDIName+" configured");

    }


}
