/*
 * Created on Jun 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.mortbay.jetty.plus.jmx;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.plus.Service;
import org.mortbay.util.LogSupport;
import org.mortbay.util.jmx.LifeCycleMBean;

/**
 * @author janb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AbstractServiceMBean extends LifeCycleMBean
{
    private static final Log log = LogFactory.getLog(AbstractServiceMBean.class);
    private Service _service = null;
    
    public AbstractServiceMBean ()
    throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("name");
        defineAttribute("JNDI");
        _service = (Service)getManagedResource();
    }
    
    /* ------------------------------------------------------------ */
    protected ObjectName newObjectName(MBeanServer server)
    {
        ObjectName oName=super.newObjectName(server);
        String sName =_service.getName();
        if (sName==null || sName.length()==0)
            sName="";
        
        
        try{oName=new ObjectName(oName+",service="+sName);}
        catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
        return oName;
    }

}
