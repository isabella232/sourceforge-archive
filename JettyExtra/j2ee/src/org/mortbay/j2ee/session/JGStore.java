// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.j2ee.session;

//----------------------------------------
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.apache.log4j.Category;
import org.javagroups.Address;
import org.javagroups.Channel;
import org.javagroups.JChannel;
import org.javagroups.MembershipListener; // we are notified of changes to membership list
import org.javagroups.Message;
import org.javagroups.MessageListener; // we are notified of changes to other state
import org.javagroups.View;
import org.javagroups.blocks.GroupRequest;
import org.javagroups.blocks.MessageDispatcher;
import org.javagroups.blocks.MethodCall;
import org.javagroups.blocks.RpcDispatcher;
import org.javagroups.util.Util;

//----------------------------------------

// what happens if a member drops away for a while then comes back -
// can we deal with it ?

// quite a lot left to do:

// how do we bring ourselves or others up to date on startup whilst
// not missing any updates ? - talk to Bela

//how do we avoid the deserialisation cost like Sacha - store updates
//serialised and deserialise lazily (we would need a custom class so
//we don't get confused by a user storing their own Serialised objects
//?

// Talk to Sacha...

// It will be VERY important that nodes using this Store have their clocks synched...

/**
 * publish changes to our state, receive and dispatch notification of
 * changes in other states, initialise our state from other members,
 * allow other members to initialise their state from us - all via
 * JavaGroups...
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public class
  JGStore
  extends AbstractReplicatedStore
  implements MessageListener, MembershipListener
{
  protected String _protocolStack=""+
    "UDP(mcast_addr=228.8.8.8;mcast_port=45566;ip_ttl=32;" +
    "ucast_recv_buf_size=16000;ucast_send_buf_size=16000;" +
    "mcast_send_buf_size=32000;mcast_recv_buf_size=64000;loopback=true):"+
    "PING(timeout=2000;num_initial_members=3):"+
    "MERGE2(min_interval=5000;max_interval=10000):"+
    "FD_SOCK:"+
    "VERIFY_SUSPECT(timeout=1500):"+
    "pbcast.STABLE(desired_avg_gossip=20000):"+
    "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):"+
    "UNICAST(timeout=2000):"+
    "FRAG(frag_size=8192;down_thread=false;up_thread=false):"+
    "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true):"+
    "pbcast.STATE_TRANSFER";
  public String getProtocolStack() {return _protocolStack;}
  public void setProtocolStack(String protocolStack) {_protocolStack=protocolStack;}

  protected String _subClusterName="DefaultSubCluster";
  public String getSubClusterName() {return _subClusterName;}
  public void setSubClusterName(String subClusterName) {_subClusterName=subClusterName;}

  protected int _retrievalTimeOut=20000;
  public int getRetrievalTimeOut() {return _retrievalTimeOut;}
  public void setRetrievalTimeOut(int retrievalTimeOut) {_retrievalTimeOut=retrievalTimeOut;}

  protected int _distributionModeInternal=GroupRequest.GET_ALL; // synchronous
  protected int getDistributionModeInternal() {return _distributionModeInternal;}
  protected void
    setDistributionModeInternal(String distributionMode)
    {
      try
      {
	_distributionModeInternal=GroupRequest.class.getDeclaredField(distributionMode).getInt(GroupRequest.class);
      }
      catch (Exception e)
      {
	_log.error("could not convert "+distributionMode+" to GroupRequest field", e);
      }
      _log.debug("GroupRequest:"+distributionMode+"="+_distributionModeInternal);
    }

  protected String _distributionMode="GET_ALL";
  public String getDistributionMode() {return _distributionMode;}
  public void
    setDistributionMode(String distributionMode)
    {
      _distributionMode=distributionMode;
      setDistributionModeInternal(_distributionMode);
    }

  protected int _distributionTimeOut=0;
  public int getDistributionTimeOut() {return _distributionTimeOut;}
  public void setDistributionTimeOut(int distributionTimeOut) {_distributionTimeOut=distributionTimeOut;}

  public Object
    clone()
    {
      JGStore jgs=(JGStore)super.clone();
      jgs.setProtocolStack(getProtocolStack());
      jgs.setSubClusterName(getSubClusterName());
      jgs.setRetrievalTimeOut(getRetrievalTimeOut());
      jgs.setDistributionMode(getDistributionMode());
      jgs.setDistributionTimeOut(getDistributionTimeOut());

      return jgs;
    }

  //----------------------------------------

  protected Channel       _channel;
  protected RpcDispatcher _dispatcher;
  protected Vector        _members;

  //----------------------------------------
  // Store API - Store LifeCycle

  public
    JGStore()
    {
      super();

      try
      {
	// start up our channel...
	_channel=new JChannel(_protocolStack); // channel should be JBoss or new Jetty channel

	MessageListener messageListener=this;
	MembershipListener membershipListener=null; //this - later
	Object serverObject=this;
	_dispatcher=new RpcDispatcher(_channel, messageListener, membershipListener, serverObject);

	_channel.setOpt(Channel.GET_STATE_EVENTS, new Boolean(true));
	_members=(Vector)_channel.getView().getMembers().clone();
	_members.remove(_channel.getLocalAddress());
      }
      catch (Exception e)
      {
	_log.error("could not initialise JavaGroups Channel and Dispatcher");
      }
    }

  public String
    getChannelName()
    {
      return "JETTY_HTTPSESSION_DISTRIBUTION:"+getContextPath()+"-"+getSubClusterName();
    }

  public void
    start()
    throws Exception
    {
      super.start();

      String channelName=getChannelName();
      _log.debug("starting ("+channelName+")....");

      _channel.connect(channelName); // group should be on a per-context basis
      _dispatcher.start();

      if (!_channel.getState(null, getRetrievalTimeOut()))
	_log.info("could not retrieve current sessions from JavaGroups - I must be first up");

      _log.debug("started ("+channelName+")....");
    }

  public void
    stop()
    {
      _dispatcher.stop();
      _channel.disconnect();

      super.stop();
    }

  public void
    destroy()
    {
      _dispatcher=null;
      _channel=null;

      super.destroy();
    }

  //----------------------------------------
  // AbstractReplicatedStore API

  protected void
    publish(String methodName, Class[] argClasses, Object[] argInstances)
    {
      //      _log.info("publishing: "+methodName);
      // we also need to dispatch this on ourselves synchronously,
      // since we are excluded from members list...
      super.dispatch(methodName, argClasses, argInstances);

      // hack... - awkward - we can't use the current
      // MarshallingInterceptor because we need the original reference
      // of the object given us preserved all the way to the cache, so
      // that subsequent requests from this cache yield the same
      // reference, rather than value...

	for (int i=0; i<argInstances.length; i++)
	{
	  try
	  {
	    argInstances[i]=MarshallingInterceptor.marshal(argInstances[i]);
	  }
	  catch(IOException e)
	  {
	    _log.error("could not marshal arg for publication", e);
	  }
	}

      try
      {
	Class[] tmp={String.class, Class[].class, Object[].class};
	MethodCall method = new MethodCall(getClass().getMethod("dispatch",tmp));
	method.addArg(methodName);
	method.addArg(argClasses);
	method.addArg(argInstances);

	_dispatcher.callRemoteMethods(_members,
				      method,
				      getDistributionModeInternal(),
				      getDistributionTimeOut());
      }
      catch(Exception e)
      {
	_log.error("problem publishing change in state over JavaGroups", e);
      }

    }

  // JG doesn't find this method in our superclass ...
  public void
    dispatch(String methodName, Class[] argClasses, Object[] argInstances)
    {
      //      _log.info("dispatching: "+methodName);

      // unhack...
      for (int i=0; i<argInstances.length; i++)
	{
	  try
	  {
	    argInstances[i]=MarshallingInterceptor.demarshal((byte[])argInstances[i]);
	  }
	  catch(Exception e)
	  {
	    _log.error("could not demarshal arg from publication", e);
	  }
	}

      super.dispatch(methodName, argClasses, argInstances);
    }

  //----------------------------------------
  // 'MessageListener' API

  /**
   * receive notification of someone else's change in state
   *
   * @param msg a <code>Message</code> value
   */
  public void
    receive(Message msg)
    {
      //      _log.info("**************** RECEIVE CALLED *********************");
      byte[] buf=msg.getBuffer();
    }

  /**
   * copy our state to be used to initialise another store...
   *
   * @return an <code>Object</code> value
   */
  public byte[]
    getState()
    {
      _log.info("initialising another store from our current state");

      // this is a bit problematic - since we really need to freeze
      // every session before we can dump them... - TODO
      LocalState[] state;
      synchronized (_sessions)
      {
	_log.info("sending "+_sessions.size()+" sessions");

	state=new LocalState[_sessions.size()];
	int j=0;
	for (Iterator i=_sessions.values().iterator(); i.hasNext();)
	  state[j++]=((ReplicatedState)i.next()).getLocalState();
      }

      Object[] data={new Long(System.currentTimeMillis()), state};
      try
      {
	return MarshallingInterceptor.marshal(data);
      }
      catch (Exception e)
      {
	_log.error ("Unable to getState from JavaGroups: ", e);
	return null;
      }
    }

  /**
   * initialise ourself from the current state of another store...
   *
   * @param new_state an <code>Object</code> value
   */
  public void
    setState (byte[] tmp)
    {
      if (tmp!=null)
      {
	_log.info("initialising our state from another Store");

	Object[] data = null;
	try
	{
	  // TODO - this needs to be loaded into webapps ClassLoader,
	  // then we can lose the MarshallingInterceptor...
	  data=(Object[])MarshallingInterceptor.demarshal(tmp);
	}
	catch (Exception e)
	{
	  _log.error ("Unable to setState from JavaGroups: ", e);
	}

	long remoteTime=((Long)data[0]).longValue();
	long localTime=System.currentTimeMillis();
	long disparity=(localTime-remoteTime)/1000;
	_log.info("time disparity: "+disparity+" secs");

	LocalState[] state=(LocalState[])data[1];
	_log.info("receiving "+state.length+" sessions...");

	for (int i=0; i<state.length; i++)
	{
	  LocalState ls=state[i];
	  _sessions.put(ls.getId(), new ReplicatedState(this, ls));
	}
      }
    }

  //----------------------------------------
  // 'MembershipListener' API

  // Block sending and receiving of messages until viewAccepted() is called
  public void
    block()
    {
      _log.info("block()");
    }

  // Called when a member is suspected
  public void
    suspect(Address suspected_mbr)
    {
      _log.info("suspect("+suspected_mbr+")");
    }

  // Called when channel membership changes
  public void
    viewAccepted(View new_view)
    {
      _log.info("viewAccepted("+new_view+")");

      Vector new_mbrs=new_view.getMembers();

      if (new_mbrs != null)
      {
 	_members.clear();
 	_members.addAll(new_mbrs);
	_members.remove(_channel.getLocalAddress());
      }
    }
}
