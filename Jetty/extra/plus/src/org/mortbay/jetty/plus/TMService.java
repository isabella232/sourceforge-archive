// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plus;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

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
