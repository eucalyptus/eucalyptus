package com.eucalyptus.ws.handlers;

import org.jboss.netty.channel.ChannelHandler;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface ResponseHandler<TYPE extends BaseMessage,RTYPE extends BaseMessage> extends ChannelHandler {

  public TYPE getRequest( );
  
  public RTYPE getResponse( ) throws Exception;
  
  public void waitForResponse( );
  
}