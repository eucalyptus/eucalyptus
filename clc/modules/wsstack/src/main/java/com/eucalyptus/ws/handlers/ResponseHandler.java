package com.eucalyptus.ws.handlers;

import org.jboss.netty.channel.ChannelHandler;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface ResponseHandler<Q extends BaseMessage,R extends BaseMessage> extends ChannelHandler {

  public Q getRequest( );
  
  public R getResponse( ) throws Exception;
  
  public void waitForResponse( );
  
}