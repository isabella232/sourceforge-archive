/*
 * Created on Jun 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.mortbay.jetty.plus.jmx;

import javax.management.MBeanException;

/**
 * @author janb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TMServiceMBean extends AbstractServiceMBean
{
    public TMServiceMBean ()
    throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        
        defineAttribute ("transactionManagerJNDI");
        defineAttribute ("transactionManager");
        defineAttribute ("userTransaction");
        
    }
}
