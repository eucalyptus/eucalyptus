package com.eucalyptus.ws.handlers.wssecurity;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.ws.handlers.MessageStackHandler;

public class BrokerWsSecHandler extends MessageStackHandler implements ChannelHandler {

  @Override
  public void incomingMessage( MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      //FIXME: temporary work around for ws-security issues in the broker
      User admin = Accounts.lookupSystemAdmin( ); 
      Contexts.lookup( ( ( MappingHttpMessage ) o ).getCorrelationId( ) ).setUser( admin );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {}

}
