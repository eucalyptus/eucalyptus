package com.eucalyptus.ws.client;

import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.MuleException;

public class NioMessageDispatcherFactory implements MessageDispatcherFactory {
  @Override
  public boolean isCreateDispatcherPerRequest() {
    return false;
  }

  @Override
  public MessageDispatcher create( final OutboundEndpoint outboundEndpoint ) throws MuleException {
    return new NioMessageDispatcher( outboundEndpoint );
  }

  @Override
  public void activate( final OutboundEndpoint outboundEndpoint, final MessageDispatcher messageDispatcher ) throws MuleException {
    ((NioMessageDispatcher)messageDispatcher).doActivate( outboundEndpoint );
  }

  @Override
  public boolean validate( final OutboundEndpoint outboundEndpoint, final MessageDispatcher messageDispatcher ) {
    return true;
  }

  @Override
  public void passivate( final OutboundEndpoint outboundEndpoint, final MessageDispatcher messageDispatcher ) {
    
  }

  @Override
  public void destroy( final OutboundEndpoint outboundEndpoint, final MessageDispatcher messageDispatcher ) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
