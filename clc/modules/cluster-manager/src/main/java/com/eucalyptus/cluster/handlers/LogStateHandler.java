package com.eucalyptus.cluster.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.binding.BindingException;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.Event;

public class LogStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  public LogStateHandler( Cluster cluster ) throws BindingException {
    super( cluster, false );
  }

  @Override
  public void trigger( ) {}

  @Override
  public void fireEvent( Event event ) {}


  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
  }

  @Override
  public void advertiseEvent( Event event ) {}

}