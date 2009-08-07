package com.eucalyptus.ws.handlers;

import java.util.Calendar;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.util.Credentials;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.OperationParameter;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;
import com.eucalyptus.ws.util.Hashes;
import com.eucalyptus.ws.util.HmacUtils;

@ChannelPipelineCoverage( "one" )
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
      Map<String, String> parameters = httpRequest.getParameters( );
      if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
      if ( !parameters.containsKey( SecurityParameter.Signature.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.Signature );
      // :: note we remove the sig :://
      String sig = parameters.remove( SecurityParameter.Signature.toString( ) );
      String queryId = parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
      String verb = httpRequest.getMethod( ).getName( );
      String addr = httpRequest.getServicePath( );
      String headerHost = httpRequest.getHeader( "Host" );
      String headerPort = "8773";
      if ( headerHost != null && headerHost.contains( ":" ) ) {
        String[] hostTokens = headerHost.split( ":" );
        headerHost = hostTokens[0];
        if ( hostTokens.length > 1 && hostTokens[1] != null && !"".equals( hostTokens[1] ) ) {
          headerPort = hostTokens[1];
        }
      }
      // TODO: hook in user key lookup here
      String secretKey;
      try {
        secretKey = Credentials.Users.getSecretKey( queryId );
      } catch ( Exception e ) {
        throw new AuthenticationException( "User authentication failed." );
      }
      String sigVersionString = parameters.get( RequiredQueryParams.SignatureVersion.toString( ) );
      if ( sigVersionString != null ) {// really, it should never be...
        int sigVersion = Integer.parseInt( sigVersionString );
        if ( sigVersion == 1 ) {
          String canonicalString = HmacUtils.makeSubjectString( parameters );
          LOG.info( "VERSION1-STRING:        " + canonicalString );
          String computedSig = HmacUtils.getSignature( secretKey, canonicalString, Hashes.Mac.HmacSHA1 );
          LOG.info( "VERSION1-SHA1:        " + computedSig + " -- " + sig );
          if ( !computedSig.equals( sig ) ) throw new AuthenticationException( "User authentication failed." );
        } else if ( sigVersion == 2 ) {
          String canonicalString = HmacUtils.makeV2SubjectString( verb, headerHost, addr, parameters );
          String canonicalStringWithPort = HmacUtils.makeV2SubjectString( verb, headerHost + ":" + headerPort, addr, parameters );
          String computedSig = HmacUtils.getSignature( secretKey, canonicalString, Hashes.Mac.HmacSHA256 );
          String computedSigWithPort = HmacUtils.getSignature( secretKey, canonicalStringWithPort, Hashes.Mac.HmacSHA256 );
          LOG.info( "VERSION2-STRING:        " + canonicalString );
          LOG.info( "VERSION2-SHA256:        " + computedSig + " -- " + sig );
          LOG.info( "VERSION2-STRING-PORT:        " + canonicalString );
          LOG.info( "VERSION2-SHA256-PORT: " + computedSigWithPort + " -- " + sig );
         // if ( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) throw new AuthenticationException( "User authentication failed." );
        }
      }
      parameters.remove( RequiredQueryParams.SignatureVersion.toString( ) );
      parameters.remove( "SignatureMethod" );
      // :: find user, remove query key to prepare for marshalling :://
      parameters.remove( SecurityParameter.AWSAccessKeyId.toString( ) );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
  }

}
