// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session.ejb;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import org.apache.log4j.Category;
import org.mortbay.j2ee.session.interfaces.CMPState;
import org.mortbay.j2ee.session.interfaces.CMPStateHome;
import org.mortbay.j2ee.session.interfaces.CMPStatePK;

/**
 * The Entity bean represents an HttpSession.
 *
 * @author jules@mortbay.com
 * @version $Revision$
 *
 *   @ejb:bean name="CMPState" type="CMP" view-type="remote" jndi-name="jetty/CMPState" reentrant="true" cmp-version="2.x"
 *   @ejb:interface remote-class="org.mortbay.j2ee.session.interfaces.CMPState" extends="javax.ejb.EJBObject, org.mortbay.j2ee.session.State"
 *   @ejb:pk
 *   @ejb:finder
 *        signature="java.util.Collection findTimedOut(long currentTime, int extraTime, int actualMaxInactiveInterval)"
 *        query="SELECT OBJECT(o) FROM CMPState o WHERE (o.maxInactiveIntervalInternal>0 AND o.creationTimeInternal < (?1-(1000*(o.maxInactiveIntervalInternal+?2)))) OR (o.maxInactiveIntervalInternal<1 AND o.creationTimeInternal < (?1-(1000*(?3+?2))))"
 *
 *   @jboss:table-name "JETTY_HTTPSESSION_CMPState"
 *   @jboss:create-table create="true"
 *   @jboss:remove-table remove="true"
 *   @jboss:container-configuration name="Sharing Standard CMP 2.x EntityBean"
 *
 */

public abstract class CMPStateBean
  implements EntityBean, org.mortbay.j2ee.session.State
{
  Category _log=Category.getInstance(getClass().getName());

  //----------------------------------------
  // Home
  //----------------------------------------

  /**
   * This removes sessions that timed out and, for some reason, have
   * not been removed by the webapp that created them (Perhaps it died
   * and has never been redeployed). For this reason, they cannot be
   * loaded back into a VM and invalidated with all the attendant
   * notifications etc (since the classes that they deserialise into
   * are probably not in the caller's ClassLoader and half of the
   * Listeners will have disappeared), so we just remove them directly
   * from the DB.
   *
   * @ejb:home-method
   */
  public void
    ejbHomeScavenge(int extraTime, int actualMaxInactiveInterval)
  {
    try
    {
      // this may not be the best way to call a home method from an
      // instance - but it is the only one that I have found so far...
      CMPStateHome home = (CMPStateHome)_entityContext.getEJBObject().getEJBHome();
      Collection c=(Collection)home.findTimedOut(System.currentTimeMillis(), extraTime, actualMaxInactiveInterval);
      _log.debug("distributed scavenging: "+c);

      // this is not working - what is the class of the Objects returned in the Collection ?
      for (Iterator i=c.iterator(); i.hasNext();)
      {
	//	home.remove((CMPState)i.next()); // doesn't work - WHY?
	((CMPState)i.next()).remove();
      }
      c.clear();
      c=null;
    }
    catch (Exception e)
    {
      _log.warn("could not scavenge dead sessions: ", e);
    }
  }

  //----------------------------------------
  // Lifecycle
  //----------------------------------------

  /**
   * Create httpSession.
   *
   * @ejb:create-method
   */
  public CMPStatePK ejbCreate(String context, String id, int maxInactiveInterval, int actualMaxInactiveInterval)
    throws CreateException
  {
    _log.debug("ejbCreate("+context+":"+id+")");

    setContextInternal(context);
    setIdInternal(id);
    setMaxInactiveIntervalInternal(maxInactiveInterval);
    setActualMaxInactiveIntervalInternal(actualMaxInactiveInterval);

    long time=System.currentTimeMillis();
    setCreationTimeInternal(time);
    setLastAccessedTimeInternal(time);
    setAttributesInternal(new HashMap());

    return null;
  }

  /**
   * Create httpSession.
   *
   */
  public void ejbPostCreate(String context, String id, int maxInactiveInterval, int actualMaxInactiveInterval)
    throws CreateException
  {
    //    _log.info("ejbPostCreate("+id+")");
  }

  private EntityContext _entityContext;

  public void
    setEntityContext(EntityContext entityContext)
  {
    //    _log.info("setEntityContext("+ctx+")");
    _entityContext=entityContext;
  }

  /**
   */
  public void
    ejbRemove()
    throws RemoveException
  {
    _log.debug("ejbRemove("+getContextInternal()+":"+getIdInternal()+")");
  }

  //----------------------------------------
  // Accessors
  //----------------------------------------
  // Context

  /**
   * @ejb:pk-field
   * @ejb:persistent-field
   */
  public abstract String getContextInternal();

  /**
   * @ejb:pk-field
   * @ejb:persistent-field
   */
  public abstract void setContextInternal(String context);

  //----------------------------------------
  // Id

  /**
   * @ejb:pk-field
   * @ejb:persistent-field
   */
  public abstract String getIdInternal();

  /**
   * @ejb:pk-field
   * @ejb:persistent-field
   */
  public abstract void setIdInternal(String id);

  /**
   * @ejb:interface-method
   */
  public String
    getId()
    throws IllegalStateException
  {
    checkValid();
    return getIdInternal();
  }

  //----------------------------------------
  /**
   * @ejb:persistent-field
   */
  public abstract long getCreationTimeInternal();

  /**
   * @ejb:persistent-field
   */
  public abstract void setCreationTimeInternal(long time);

  /**
   * @ejb:interface-method
   */
  public long
    getCreationTime()
    throws IllegalStateException
  {
    checkValid();
    return getCreationTimeInternal();
  }

  //----------------------------------------
  /**
   * @ejb:persistent-field
   */
  public abstract long getLastAccessedTimeInternal();

  /**
   * @ejb:persistent-field
   */
  public abstract void setLastAccessedTimeInternal(long time);

  /**
   * @ejb:interface-method
   */
  public long
    getLastAccessedTime()
    throws IllegalStateException
  {
    checkValid();
    return getLastAccessedTimeInternal();
  }

  /**
   * @ejb:interface-method
   */
  public void
    setLastAccessedTime(long time)
    throws IllegalStateException
  {
    checkValid();
    setLastAccessedTimeInternal(time);
  }

  //----------------------------------------
  /**
   * @ejb:persistent-field
   */
  public abstract int getMaxInactiveIntervalInternal();

  /**
   * @ejb:persistent-field
   */
  public abstract void setMaxInactiveIntervalInternal(int interval);

  /**
   * @ejb:interface-method
   */
  public int
    getMaxInactiveInterval()
    throws IllegalStateException
  {
    checkValid();
    return getMaxInactiveIntervalInternal();
  }

  /**
   * @ejb:interface-method
   */
  public void
    setMaxInactiveInterval(int interval)
    throws IllegalStateException
  {
    checkValid();
    setMaxInactiveIntervalInternal(interval);
  }

  //----------------------------------------
  // Attributes

  /**
   * @ejb:persistent-field
   */
  public abstract Map getAttributesInternal();

  /**
   * @ejb:persistent-field
   */
  public abstract void setAttributesInternal(Map attributes);

  /**
   * @ejb:interface-method
   */
  public Map
    getAttributes()
    throws IllegalStateException
  {
    checkValid();
    return getAttributesInternal();
  }

  /**
   * @ejb:interface-method
   */
  public void
    setAttributes(Map attributes)
    throws IllegalStateException
  {
    checkValid();
    setAttributesInternal(attributes);
  }

  /**
   * @ejb:interface-method
   */
  public Object
    getAttribute(String name)
    throws IllegalStateException
  {
    checkValid();

    //    _log.info(getIdInternal()+": get attribute - "+name);
    return getAttributesInternal().get(name);
  }

  /**
   * @ejb:interface-method
   */
  public Object
    setAttribute(String name, Object value, boolean returnValue)
    throws IllegalStateException
  {
    checkValid();

    Map attrs=getAttributesInternal();
    Object tmp=attrs.put(name, value);
    setAttributesInternal(null);
    setAttributesInternal(attrs);

    //    _log.info(getContextInternal()+":"+getIdInternal()+": set attribute - "+name+":"+value);

    return returnValue?tmp:null;
  }

  /**
   * @ejb:interface-method
   */
  public Object
    removeAttribute(String name, boolean returnValue)
    throws IllegalStateException
  {
    checkValid();

    Map attrs=getAttributesInternal();
    Object tmp=attrs.remove(name);

    if (tmp!=null)
    {
      setAttributesInternal(null);	// belt-n-braces - TODO
      setAttributesInternal(attrs);
    }

    //    _log.info(getContextInternal()+":"+getIdInternal()+": remove attribute - "+name);

    return returnValue?tmp:null;
  }

  /**
   * @ejb:interface-method
   */
  public Enumeration
    getAttributeNameEnumeration()
    throws IllegalStateException
  {
    checkValid();

    return Collections.enumeration(getAttributesInternal().keySet());
  }

  /**
   * @ejb:interface-method
   */
  public String[]
    getAttributeNameStringArray()
    throws IllegalStateException
  {
    checkValid();

    Map attrs=getAttributesInternal();
    return (String[])attrs.keySet().toArray(new String[attrs.size()]);
  }

  //----------------------------------------
  /**
   * @ejb:persistent-field
   */
  public abstract int getActualMaxInactiveIntervalInternal();

  /**
   * @ejb:persistent-field
   */
  public abstract void setActualMaxInactiveIntervalInternal(int interval);

  //----------------------------------------
  // new stuff - for server sider validity checking...

  protected long
    getRealMaxInactiveValue()
  {
    long maxInactiveInterval=getMaxInactiveIntervalInternal();
    return maxInactiveInterval<1?getActualMaxInactiveIntervalInternal()*1000:maxInactiveInterval;
  }

  protected boolean
    isValid()
  {
    return (getLastAccessedTimeInternal()+getRealMaxInactiveValue())>System.currentTimeMillis();
  }

  protected void
    checkValid()
    throws IllegalStateException
  {
    if (!isValid())
      throw new IllegalStateException("HttpSession timed out");
  }
}
