// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import mx4j.adaptor.http.HttpAdaptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MX4JHttpAdaptor extends HttpAdaptor
{
    private static Log log = LogFactory.getLog(MX4JHttpAdaptor.class);

    public MX4JHttpAdaptor()
    {
        super();
    }
    
    public MX4JHttpAdaptor(int port)
    {
        super(port);
    }
    
    public MX4JHttpAdaptor(int port, String host)
    {
        super(port,host);
    }
    
    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
        throws Exception
    {
        name=super.preRegister(server,name);
        ObjectName processorName = new ObjectName(name+",processor=XSLT");
        server.createMBean("mx4j.adaptor.http.XSLTProcessor", processorName, null);
        setProcessorName(processorName);
        return name;
    }

    public void postRegister(Boolean done)
    {
        super.postRegister(done);
        if (done.booleanValue())
        {
            try{start();} catch(Exception e){e.printStackTrace();}
            log.info("Started MX4J HTTP Adaptor on : "+this.getPort());
        }
    }
}


