// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.LifeCycle;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class OutputStreamLogSinkMBean extends LogSinkMBean
{
    private LogSink _logSink;
    private boolean _formatOptions;
    private String  _domain="com.mortbay";

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param sink 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public OutputStreamLogSinkMBean(LogSink sink)
        throws MBeanException, InstanceNotFoundException
    {
        this(sink,true);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param sink 
     * @param formatOptions 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public OutputStreamLogSinkMBean(LogSink sink, boolean formatOptions)
        throws MBeanException, InstanceNotFoundException
    {
        super(null);
        _logSink=sink;
        _formatOptions=formatOptions;
        try{setManagedResource(sink,"objectReference");}
        catch(InvalidTargetObjectTypeException e)
        {
            Code.warning(e);
            throw new MBeanException(e);
        }
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        if (_formatOptions)
        {
            defineAttribute("logDateFormat");
            defineAttribute("logTimezone");
            defineAttribute("logTimeStamps");
            defineAttribute("logLabels");
            defineAttribute("logTags");
            defineAttribute("logStackSize");
            defineAttribute("logStackTrace");
            defineAttribute("logOneLine");
        }
        
        defineAttribute("append");
        defineAttribute("outputStream");
        defineAttribute("filename");
        defineAttribute("retainDays");
        defineAttribute("flushOn");
    }
    
    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        return uniqueObjectName(server,getJettyDomain()+":name=Log");
    }
}


