// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import org.mortbay.util.Code;
import org.mortbay.util.Log;
import mx4j.adaptor.http.HttpAdaptor;
import javax.management.ObjectName;
import javax.management.MBeanServer;

public class MX4JHttpAdaptor extends HttpAdaptor
{
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
            Log.event("Started MX4J HTTP Adaptor on : "+this.getPort());
        }
    }
}


