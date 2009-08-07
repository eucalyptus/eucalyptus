package com.eucalyptus.ws.handlers.wssecurity;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.handlers.MessageStackHandler;

public class UserWsSecHandler extends MessageStackHandler implements ChannelHandler {

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
  }

}
