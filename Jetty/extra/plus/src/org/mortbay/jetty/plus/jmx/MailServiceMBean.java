/*
 * Created on Jun 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.mortbay.jetty.plus.jmx;

import javax.management.MBeanException;

import org.mortbay.jetty.plus.MailService;

/**
 * @author janb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MailServiceMBean extends AbstractServiceMBean
{
    private MailService _mailService;
    
    public MailServiceMBean ()
    throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute ("user");
        defineAttribute ("password");
        defineOperation ("get", new String[]{STRING}, IMPACT_INFO, ON_MBEAN);
        defineOperation ("put", new String[]{STRING, STRING}, IMPACT_ACTION, ON_MBEAN);
        defineOperation ("remove", new String[]{STRING}, IMPACT_ACTION, ON_MBEAN);
        
        _mailService = (MailService)getManagedResource();
    }
    
    public String get (String key)
    {
        return (String)_mailService.get(key);
    }
    
    public void put (String key, String value)
    {
        _mailService.put(key,value);
    }
    
    public void remove (String key)
    {
        _mailService.remove(key);
    }
}
