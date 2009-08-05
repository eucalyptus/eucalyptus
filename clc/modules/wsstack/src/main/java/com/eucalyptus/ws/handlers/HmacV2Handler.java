package com.eucalyptus.ws.handlers;

import java.util.Calendar;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.OperationParameter;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;
import com.eucalyptus.ws.util.HmacUtils;

@ChannelPipelineCoverage("one")
public class HmacV2Handler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( HmacV2Handler.class );
  public enum SecurityParameter {
    AWSAccessKeyId,
    Timestamp,
    Expires,
    Signature,
    Authorization,
    Date,
    Content_MD5,
    Content_Type
  }

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String,String> parameters = httpRequest.getParameters( );
      if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
      if ( !parameters.containsKey( SecurityParameter.Signature.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.Signature );
      
      //:: note we remove the sig :://
      String sig = parameters.remove( SecurityParameter.Signature.toString() );
      String queryId = parameters.get( SecurityParameter.AWSAccessKeyId.toString() );
      String verb = httpRequest.getMethod( ).getName( );
      String addr = httpRequest.getServicePath( );
      String headerHost = httpRequest.getHeader( "Host" );
      String headerPort = "8773";
      if( headerHost != null && headerHost.contains( ":" ) ) {
        String[] hostTokens = headerHost.split( ":" );
        headerHost = hostTokens[0];
        if( hostTokens.length > 1 && hostTokens[1] != null && !"".equals( hostTokens[1] ) ) {
          headerPort = hostTokens[1];
        }
      }
      String canonicalString = HmacUtils.makeV2SubjectString( verb, headerHost, addr, parameters );
      String canonicalStringWithPort = HmacUtils.makeV2SubjectString( verb, headerHost+":"+headerPort, addr, parameters );
      
      //TODO: hook in user key lookup here
      String queryKey = "xhqe5UOv5b_Eplr_anLQ0cdBgwoL96U_IDdzeQ";

      String authv2sha256 = HmacUtils.checkSignature256( queryKey, canonicalString );
      String authv2sha256port = HmacUtils.checkSignature256( queryKey, canonicalStringWithPort );
      LOG.info( "VERSION2-SHA256:        " + authv2sha256 + " -- " + sig );
      LOG.info( "VERSION2-SHA256-HEADER: " + authv2sha256port + " -- " + sig );

      //if ( !authv2sha256.equals( sig ) && !authv2sha256port.equals( sig ) )
       // throw new AuthenticationException( "User authentication failed." );


      parameters.remove( RequiredQueryParams.SignatureVersion.toString() );
      parameters.remove( "SignatureMethod" );

      //:: find user, remove query key to prepare for marshalling :://
      parameters.remove( SecurityParameter.AWSAccessKeyId.toString() );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    // TODO Auto-generated method stub

  }

}
