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

import java.util.Map;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.HmacCredentials;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.server.EucalyptusQueryPipeline.RequiredQueryParams;

@ChannelPipelineCoverage( "one" )
public class HmacHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( HmacHandler.class );
  private boolean internal = false;
  public HmacHandler( boolean b ) {
    this.internal = b;
  }
  
  @Override
  @SuppressWarnings( "deprecation" )
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
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
        parameters.put( SecurityParameter.AWSAccessKeyId.toString( ), Users.lookupUser( "admin" ).getQueryId( ) );
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
