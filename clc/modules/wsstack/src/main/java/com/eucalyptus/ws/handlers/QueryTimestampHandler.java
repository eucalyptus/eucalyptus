package com.eucalyptus.ws.handlers;

import java.util.Calendar;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.handlers.HmacV2Handler.SecurityParameter;
import com.eucalyptus.ws.util.HmacUtils;

@ChannelPipelineCoverage("one")
public class QueryTimestampHandler extends MessageStackHandler {

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String, String> parameters = httpRequest.getParameters( );
      if ( !parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) && !parameters.containsKey( SecurityParameter.Expires.toString( ) ) ) { throw new AuthenticationException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp + " OR "
          + SecurityParameter.Expires ); }
      // :: check the timestamp :://
      Calendar now = Calendar.getInstance( );
      Calendar expires = null;
      if ( parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) ) {
        String timestamp = parameters.remove( SecurityParameter.Timestamp.toString( ) );
        expires = HmacUtils.parseTimestamp( timestamp );
        expires.add( Calendar.MINUTE, 5 );
      } else {
        String exp = parameters.remove( SecurityParameter.Expires.toString( ) );
        expires = HmacUtils.parseTimestamp( exp );
      }
      if ( now.after( expires ) ) throw new AuthenticationException( "Message has expired." );

    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
  }

}
