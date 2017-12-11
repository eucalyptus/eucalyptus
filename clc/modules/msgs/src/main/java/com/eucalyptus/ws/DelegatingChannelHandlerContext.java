package com.eucalyptus.ws;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;

/**
 *
 */
abstract class DelegatingChannelHandlerContext implements ChannelHandlerContext {

  private final ChannelHandlerContext delegate;

  DelegatingChannelHandlerContext( final ChannelHandlerContext delegate ) {
    this.delegate = delegate;
  }

  @Override
  public Channel getChannel( ) {
    return delegate.getChannel( );
  }

  @Override
  public ChannelPipeline getPipeline( ) {
    return delegate.getPipeline( );
  }

  @Override
  public String getName( ) {
    return delegate.getName( );
  }

  @Override
  public ChannelHandler getHandler( ) {
    return delegate.getHandler( );
  }

  @Override
  public boolean canHandleUpstream( ) {
    return delegate.canHandleUpstream( );
  }

  @Override
  public boolean canHandleDownstream( ) {
    return delegate.canHandleDownstream( );
  }

  @Override
  public void sendUpstream( final ChannelEvent e ) {
    delegate.sendUpstream( e );
  }

  @Override
  public void sendDownstream( final ChannelEvent e ) {
    delegate.sendDownstream( e );
  }

  @Override
  public Object getAttachment( ) {
    return delegate.getAttachment( );
  }

  @Override
  public void setAttachment( final Object attachment ) {
    delegate.setAttachment( attachment );
  }
}
