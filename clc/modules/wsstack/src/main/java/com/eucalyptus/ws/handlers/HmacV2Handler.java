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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.auth.User;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.auth.util.Hashes;
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
  @SuppressWarnings( "deprecation" )
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String, String> parameters = httpRequest.getParameters( );
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      httpRequest.getContent( ).readBytes( bos, httpRequest.getContent( ).readableBytes( ) );
      String blah = bos.toString( );
      if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
      if ( !parameters.containsKey( SecurityParameter.Signature.toString( ) ) ) throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.Signature );
      // :: note we remove the sig :://
      String sig = parameters.remove( SecurityParameter.Signature.toString( ) );
      String queryId = doAdmin?CredentialProvider.getQueryId( "admin" ):parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
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
        secretKey = CredentialProvider.getSecretKey( queryId );
      } catch ( Exception e ) {
        throw new AuthenticationException( "User authentication failed." );
      }
      String sigVersionString = parameters.get( RequiredQueryParams.SignatureVersion.toString( ) );
      if ( sigVersionString != null ) {// really, it should never be...
        int sigVersion = Integer.parseInt( sigVersionString );
        if ( sigVersion == 1 ) {
          String canonicalString = HmacUtils.makeSubjectString( parameters );
          LOG.debug( "VERSION1-STRING:        " + canonicalString );
          String computedSig = HmacUtils.getSignature( secretKey, canonicalString, Hashes.Mac.HmacSHA1 );
          LOG.debug( "VERSION1-SHA1:        " + computedSig + " -- " + sig );
          String decodedSig = URLDecoder.decode( sig ).replaceAll( "=", "" );
          if ( !computedSig.equals( sig.replaceAll( "=", "" ) ) && !computedSig.equals( decodedSig ) && !computedSig.equals( sig ) ) {
            throw new AuthenticationException( "User authentication failed." );
          }
        } else if ( sigVersion == 2 ) {
          String canonicalString = HmacUtils.makeV2SubjectString( verb, headerHost, addr, parameters );
          String canonicalStringWithPort = HmacUtils.makeV2SubjectString( verb, headerHost + ":" + headerPort, addr, parameters );
          String computedSig = HmacUtils.getSignature( secretKey, canonicalString, Hashes.Mac.HmacSHA256 );
          String computedSigWithPort = HmacUtils.getSignature( secretKey, canonicalStringWithPort, Hashes.Mac.HmacSHA256 );
          LOG.debug( "VERSION2-STRING:        " + canonicalString );
          LOG.debug( "VERSION2-SHA256:        " + computedSig + " -- " + sig );
          LOG.debug( "VERSION2-STRING-PORT:        " + canonicalString );
          LOG.debug( "VERSION2-SHA256-PORT: " + computedSigWithPort + " -- " + sig );
          if ( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) {
            sig = URLDecoder.decode( sig ).replaceAll("=","");
            computedSig = HmacUtils.getSignature( secretKey, canonicalString.replaceAll("\\+","%20"), Hashes.Mac.HmacSHA256 ).replaceAll("\\+"," ");
            computedSigWithPort = HmacUtils.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\+","%20"), Hashes.Mac.HmacSHA256 ).replaceAll("\\+"," ");
            if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) ) {
              throw new AuthenticationException( "User authentication failed." );              
            }
          }
        }
      }
      String userName = CredentialProvider.getUserName( queryId );
      User user = CredentialProvider.getUser( userName );
      httpRequest.setUser( user );
      parameters.remove( RequiredQueryParams.SignatureVersion.toString( ) );
      parameters.remove( "SignatureMethod" );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
  }

}
