package com.eucalyptus.ws.handlers.wssecurity;

import java.security.cert.X509Certificate;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.User;
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.util.WSSecurity;

@ChannelPipelineCoverage( "one" )
public class UserWsSecHandler extends MessageStackHandler implements ChannelHandler {
  private static Logger             LOG = Logger.getLogger( UserWsSecHandler.class );

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
      SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
      X509Certificate cert = WSSecurity.getVerifiedCertificate( envelope );
      String userName = UserCredentialProvider.getUserName( cert );
      User user = UserCredentialProvider.getUser( userName );
      httpRequest.setUser( user );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {

  }

}
