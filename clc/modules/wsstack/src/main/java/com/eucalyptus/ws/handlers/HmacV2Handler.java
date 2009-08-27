/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.util.Calendar;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.OperationParameter;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;
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
  private boolean doAdmin = false;
  
  public HmacV2Handler( ) {}

  public HmacV2Handler( boolean doAdmin ) {
    this.doAdmin = doAdmin;
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
      String queryId = doAdmin?UserCredentialProvider.getQueryId( "admin" ):parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
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
        secretKey = UserCredentialProvider.getSecretKey( queryId );
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
          if ( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) throw new AuthenticationException( "User authentication failed." );
        }
      }
      String userName = UserCredentialProvider.getUserName( queryId );
      User user = UserCredentialProvider.getUser( userName );
      httpRequest.setUser( user );
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
