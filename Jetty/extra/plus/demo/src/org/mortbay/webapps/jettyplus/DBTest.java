// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.webapps.jettyplus;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import org.mortbay.util.Log;
import org.mortbay.util.Code;


/* ---------------------------------------------------- */
/** DBTest
 * <p>Class for demo of user transactions, datasource and
 * jndi lookups.
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Sun May  4 2003
 * @author Jan Bartel (janb)
 */
public class DBTest
{
    public static final String COMMIT = "commit";
    public static final String ROLLBACK = "rollback";
    
    
    
    int foo;
    DataSource datasource = null;
    Context context = null;
    String selectString = null;
    String updateString = null;
    

    
    /* ------------------------------------------------ */
    /** Constructor. 
     */
    public DBTest()
    {
        super();
        this.foo = -1;
    }


    public void init ()
    {
        try
        {
            if (context == null)
            {
                //jndi env lookup
                context = new InitialContext();
                Log.event("<<< Retrieving environment >>>");
                selectString = (String)context.lookup("java:comp/env/select");
                updateString = (String)context.lookup("java:comp/env/update");
                Log.event("<<< Environment retrieved >>>");
                
                //get datasource
                Log.event("<<< Retrieving DataSource >>>");
                datasource = (DataSource)context.lookup("java:comp/env/jdbc/myDB");
                Log.event("<<< DataSource retrieved >>>");
            }
        }
        catch (Exception e)
        {
            Log.warning (e.getMessage());
        }
        
    }
    
    /* ------------------------------------------------ */
    /** 
     * @param action 
     */
    public void doIt(String action)
    {
        try
        {
            init();
            Log.event ("<<< Looking up UserTransaction >>>");
            UserTransaction usertransaction = (UserTransaction)context.lookup("java:comp/UserTransaction");
            Log.event ("<<< Connecting to datasource >>>");
            Connection connection = datasource.getConnection();
            Log.event ("<<< Connected >>>");
            
            //start a user transaction, get foo from db
            Log.event("<<< beginning the transaction >>>");
            usertransaction.begin();
            Statement statement = connection.createStatement();
            ResultSet resultset = statement.executeQuery(selectString);
            PreparedStatement preparedStatement;
            if ( resultset.next() )
            {
                this.foo = resultset.getInt(2);
            }
            Log.event(new StringBuffer().append("<<< ").append("foo = ").append(this.foo).append(" (before completion) >>>").toString());

            //update foo and commit or rollback
            preparedStatement = connection.prepareStatement(updateString);
            preparedStatement.setInt(1 , this.foo + 1);
            preparedStatement.executeUpdate();
            if ( (action != null) && action.equals("commit") )
            {
                Log.event("<<< committing the transaction >>>");
                usertransaction.commit();
            }
            else
            { 
                Log.event("<<< rolling back the transaction >>>");
                usertransaction.rollback();
            }
            connection.close();
            
            //get foo again (without a transaction)
            foo = readFoo();
            Log.event(new StringBuffer().append("<<< foo = ").append(this.foo).append(" (after completion) >>>").toString());
            Log.event("<<< done >>>");
        }
        catch ( Exception  exception  )
        {
            Log.warning(exception.getMessage());
        }
        return;
    }


    /* ------------------------------------------------ */
    /** Read the value of foo from the db
     * @return 
     */
    public int readFoo ()
    {
        int fooValue = -1;
        Connection connection = null;
        
        try
        {
            init();
            
            Log.event ("<<< Getting connection >>>");

            connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultset = statement.executeQuery(selectString);
            if ( resultset.next() )
            {
                fooValue = resultset.getInt(2);
            }

            Log.event ("<<< Read foo from db = "+fooValue+" >>>");
            return fooValue;
        }
        catch (Exception e)
        {
            Log.warning(e.getMessage());
            return fooValue;
        }
        finally
        {
            try
            {
                if (connection != null)
                    connection.close();
            }
            catch (Exception e)
            {
                Log.warning (e.getMessage());
            }
        }
    }

    
    /* ------------------------------------------------ */
    /** Get current value of foo
     * @return value of foo as a string
     */
    public String getFoo()
    {
        return (new StringBuffer().append("").append(this.foo).toString());
    }
}
