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

import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.LifeCycle;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.enhydra.jdbc.pool.StandardXAPoolDataSource;

/**
 * Implementation of TMService for Objectweb JOTM (www.objectweb.org)
 * 
 * @author mhalas
 */
public class JotmService implements TMService
{
    /**
     * Instance of JOTM transaction manager. 
     */
    protected org.objectweb.transaction.jta.TMService m_tm;

    /**
     * Global data sources specified in server.xml
     */
    protected Map m_mpDataSources;
    
    public JotmService(
    )
    {
       m_tm = null;
       // We can use HashMap because it will be only read
       // since all global data sources are created on startup
       m_mpDataSources = new HashMap();
    }
		 
    /**
     * returns a <code>TransactionManager</code> object.
     * 
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager(
    )
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
    public UserTransaction getUserTransaction(
    )
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
    public void start(
    ) throws Exception
    {
       Log.event("Starting JoTM transaction manager.");

       // Start the transaction manager
       try
       {
          m_tm = new org.objectweb.jotm.Jotm(true, true);
       }
       catch(Exception eExc)
       {
          Code.warning(eExc);
          throw new IOException("Failed to start JoTM: " + eExc);
       }

       //  Register the user transaction object in JNDI
       // TODO: This needs to be redone, email from Jeff:
       // For example, a J2EE app server requires the UserTransaction 
       // to be available through JNDI with the name 
       // "java:comp/env/UserTransaction" (ENC requirements). 
       // What's more, using resource factories, you 
       // don't even bind UserTransaction object 
       // (you create instead an object inheriting from 
       // javax.naming/spi/ObjectFactory which'll return an UserTransaction object).
       // On the other hand, if an application is not supporting 
       // ENC, UserTransaction can be available with 
       // the name "javax.transaction.UserTransaction" (see the JDBC example of JOTM)
       // I prefer not to mandate a specific use of JOTM so that it can fit into several use cases.

       Context ictx = null;
       String  userTransactionName = "javax.transaction.UserTransaction";
       String  transactionManagerName = "javax.transaction.TransactionManager";

       try 
       {
          ictx = new InitialContext();
       } 
       catch (NamingException e) 
       {
          Code.warning(e);
          throw new IOException("No initial context: "+e);
       }

       try 
       {
          if (userTransactionName != null) 
          {
             ictx.rebind(userTransactionName, m_tm.getUserTransaction());
             Code.debug("UserTransaction object bound in JNDI with name " + userTransactionName);
          }
       }
       catch (NamingException e) 
       {
          Code.warning(e);
          throw new IOException("UserTransaction rebind failed :" + e.getExplanation());
       }
 
       try
       {
          if (transactionManagerName != null) 
          {
             ictx.rebind(transactionManagerName, m_tm.getTransactionManager());
             Code.debug("TransactionManager object bound in JNDI with name " + transactionManagerName);
          }
       } 
       catch (NamingException e) 
       {
          Code.warning(e);
          throw new IOException("TransactionManager rebind failed :" + e.getExplanation());
       }


       // Now take any existing data sources and register them with this
       // transaction manager and JNDI
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
          StandardXAPoolDataSource xapdsPoolDataSource = (StandardXAPoolDataSource)meDataSource.getValue();
          xadsDataSource = xapdsPoolDataSource.getDataSource();

          try 
          {
             ictx.rebind("XA"+ strDataSourceName, xadsDataSource);
             Code.debug("XA Data source bound in JNDI with name XA" + strDataSourceName);
             ictx.rebind(strDataSourceName, xapdsPoolDataSource);
             Code.debug("Data source bound in JNDI with name " + strDataSourceName);
          } 
          catch (NamingException e) 
          {
             Code.debug("Data source rebind failed :" + e.getExplanation());
             Code.warning(e);
             throw e;
          }
       }

       Log.event("JoTM is running.");
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
       if (m_tm != null)
       {
          Log.event("Stopping JoTM...");
          m_tm.stop();
          Log.event("JoTM is stopped.");
       }
       else
       {
          Code.warning("No JoTM to stop.");
       }
    }
   
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the LifeCycle has been started. 
     */
    public boolean isStarted(
    )
    {
       return (m_tm != null);
    }

    /**
     * Add data source to be managed by this transaction manager.
     */
    public void addDataSource(
       String  strDataSourceName,
       String  strDriverName,
       String  strUrl,
       String  strUser,
       String  strPassword,
       String  strInitialConnectionCount,
       String  strMinimalPoolSize,
       String  strMaximalPoolSize
    ) throws SQLException,
             NamingException
    {
       // create an XA pool datasource with a minimum of 4 objects
       StandardXAPoolDataSource xapdsPoolDataSource = new StandardXAPoolDataSource(Integer.parseInt(strInitialConnectionCount));
       try
       {
          xapdsPoolDataSource.setMinSize(Integer.parseInt(strMinimalPoolSize));
          xapdsPoolDataSource.setMaxSize(Integer.parseInt(strMaximalPoolSize));
       }
       catch(Exception eExc)
       {
          Code.warning(eExc);
       }
       xapdsPoolDataSource.setUser(strUser);
       xapdsPoolDataSource.setPassword(strPassword);

       StandardXADataSource xadsDataSource;

       xadsDataSource = new StandardXADataSource();
       xadsDataSource.setDriverName(strDriverName);
       xadsDataSource.setUrl(strUrl);
       xadsDataSource.setUser(strUser);
       xadsDataSource.setPassword(strPassword);

       // Transaction manager was already created so attach it with DS
       xapdsPoolDataSource.setDataSourceName("XA"+strDataSourceName);
       xapdsPoolDataSource.setDataSource(xadsDataSource);

       m_mpDataSources.put(strDataSourceName, xapdsPoolDataSource);

       Code.debug("DataSource " + strDataSourceName + " registered.");
    }
}
