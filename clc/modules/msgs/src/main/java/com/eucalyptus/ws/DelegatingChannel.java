package com.eucalyptus.ws;

import java.net.SocketAddress;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

/**
 *
 */
abstract class DelegatingChannel implements Channel {

  private final Channel delegate;

  DelegatingChannel( final Channel delegate ) {
    this.delegate = delegate;
  }

  @Override
  public Integer getId( ) {
    return delegate.getId( );
  }

  @Override
  public ChannelFactory getFactory( ) {
    return delegate.getFactory( );
  }

  @Override
  public Channel getParent( ) {
    return delegate.getParent( );
  }

  @Override
  public ChannelConfig getConfig( ) {
    return delegate.getConfig( );
  }

  @Override
  public ChannelPipeline getPipeline( ) {
    return delegate.getPipeline( );
  }

  @Override
  public boolean isOpen( ) {
    return delegate.isOpen( );
  }

  @Override
  public boolean isBound( ) {
    return delegate.isBound( );
  }

  @Override
  public boolean isConnected( ) {
    return delegate.isConnected( );
  }

  @Override
  public SocketAddress getLocalAddress( ) {
    return delegate.getLocalAddress( );
  }

  @Override
  public SocketAddress getRemoteAddress( ) {
    return delegate.getRemoteAddress( );
  }

  @Override
  public ChannelFuture write( final Object message ) {
    return delegate.write( message );
  }

  @Override
  public ChannelFuture write( final Object message, final SocketAddress remoteAddress ) {
    return delegate.write( message, remoteAddress );
  }

  @Override
  public ChannelFuture bind( final SocketAddress localAddress ) {
    return delegate.bind( localAddress );
  }

  @Override
  public ChannelFuture connect( final SocketAddress remoteAddress ) {
    return delegate.connect( remoteAddress );
  }

  @Override
  public ChannelFuture disconnect( ) {
    return delegate.disconnect( );
  }

  @Override
  public ChannelFuture unbind( ) {
    return delegate.unbind( );
  }

  @Override
  public ChannelFuture close( ) {
    return delegate.close( );
  }

  @Override
  public ChannelFuture getCloseFuture( ) {
    return delegate.getCloseFuture( );
  }

  @Override
  public int getInterestOps( ) {
    return delegate.getInterestOps( );
  }

  @Override
  public boolean isReadable( ) {
    return delegate.isReadable( );
  }

  @Override
  public boolean isWritable( ) {
    return delegate.isWritable( );
  }

  @Override
  public ChannelFuture setInterestOps( final int interestOps ) {
    return delegate.setInterestOps( interestOps );
  }

  @Override
  public ChannelFuture setReadable( final boolean readable ) {
    return delegate.setReadable( readable );
  }

  @Override
  public Object getAttachment( ) {
    return delegate.getAttachment( );
  }

  @Override
  public void setAttachment( final Object attachment ) {
    delegate.setAttachment( attachment );
  }

  @Override
  public int compareTo( final Channel o ) {
    return delegate.compareTo( o );
  }
}
