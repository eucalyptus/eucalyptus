/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.util.Map;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.HmacCredentials;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.protocol.RequiredQueryParams;

@ChannelPipelineCoverage( "one" )
public class HmacHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( HmacHandler.class );
  private boolean internal = false;
  public HmacHandler( boolean b ) {
    this.internal = b;
  }
  
  @Override
  @SuppressWarnings( "deprecation" )
  public void incomingMessage( MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String, String> parameters = httpRequest.getParameters( );
      ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      httpRequest.getContent( ).readBytes( bos, httpRequest.getContent( ).readableBytes( ) );
      String blah = bos.toString( );
      bos.close( );
      if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString( ) ) && !internal ) {
        throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
      } else if ( internal ) {
        // YE TODO: can we just pick any active secret key of admin?
        parameters.put( SecurityParameter.AWSAccessKeyId.toString( ), Accounts.getFirstActiveAccessKeyId( Accounts.lookupSystemAdmin( ) ) );
      }
      if ( !parameters.containsKey( SecurityParameter.Signature.toString( ) ) ) {
        throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.Signature );
      }
      // :: note we remove the sig :://
      String sig = parameters.remove( SecurityParameter.Signature.toString( ) );
      String sigVersion = parameters.get( RequiredQueryParams.SignatureVersion.toString( ) );
      String sigMethod = parameters.get( SecurityParameter.SignatureMethod.toString( ) );
      String verb = httpRequest.getMethod( ).getName( );
      sigMethod = ( ( sigMethod == null ) ? "HMACSHA1" : sigMethod );
      Hmac hmac = Hmac.valueOf( "HmacSHA" + sigMethod.substring( 7 ) );
      String headerHost = httpRequest.getHeader( "Host" );
      String servicePath = httpRequest.getServicePath( );
      SecurityContext.getLoginContext( new HmacCredentials( httpRequest.getCorrelationId( ), sig, parameters, verb, servicePath, headerHost, Integer.valueOf( sigVersion ), hmac ) ).login( );
      parameters.remove( RequiredQueryParams.SignatureVersion.toString( ) );
      parameters.remove( SecurityParameter.SignatureMethod.toString( ) );
      parameters.remove( SecurityParameter.AWSAccessKeyId.toString( ) );
    }
  }
  
  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {}
  
}
