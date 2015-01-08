/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import static com.eucalyptus.auth.login.HmacCredentials.QueryIdCredential;
import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.HmacCredentials;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class HmacHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( HmacHandler.class );
  private final Set<TemporaryKeyType> allowedTemporaryKeyTypes;

  public HmacHandler( final Set<TemporaryKeyType> allowedTemporaryKeyTypes ) {
    this.allowedTemporaryKeyTypes = allowedTemporaryKeyTypes;
  }
  
  @Override
  @SuppressWarnings( "deprecation" )
  public void incomingMessage( MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      final Map<String,String> parameters = httpRequest.getParameters();
      final ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      httpRequest.getContent( ).markReaderIndex( );
      httpRequest.getContent( ).readBytes( bos, httpRequest.getContent( ).readableBytes( ) );
      httpRequest.getContent( ).resetReaderIndex( );
      final String body = bos.toString();
      bos.close( );
      final Function<String,List<String>> headerLookup = SignatureHandlerUtils.headerLookup( httpRequest );
      final Function<String,List<String>> parameterLookup = SignatureHandlerUtils.parameterLookup( httpRequest );
      final HmacUtils.SignatureVariant variant = HmacUtils.detectSignatureVariant( headerLookup, parameterLookup );
      final Map<String,List<String>> headers = Maps.newHashMap();
      for ( final String header : httpRequest.getHeaderNames() ) {
        headers.put( header.toLowerCase(), httpRequest.getHeaders( header ) );
      }
      if ( variant.getVersion().value() <= 2 ) {
          if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.parameter() ) ) {
            throw new AuthenticationException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
          }
      }

      final HmacCredentials credentials = new HmacCredentials( 
          httpRequest.getCorrelationId(), 
          variant,
          processParametersForVariant( httpRequest, variant ),
          headers,
          httpRequest.getMethod( ).getName( ),
          httpRequest.getServicePath( ),
          body );

      SecurityContext.getLoginContext( credentials ).login( );

      final Subject subject = Contexts.lookup( httpRequest.getCorrelationId( ) ).getSubject( );
      final QueryIdCredential credential =
          Iterables.getFirst( subject.getPublicCredentials( QueryIdCredential.class ), null );
      if ( credential == null ||
          ( credential.getType( ).isPresent( ) && !allowedTemporaryKeyTypes.contains( credential.getType( ).get( ) )) ) {
        throw new AuthenticationException( "Temporary credentials forbidden for service" );
      }

      parameters.keySet().removeAll( variant.getParametersToRemove() );
      parameters.remove( SecurityParameter.SecurityToken.parameter() );
    }
  }

  private Map<String,List<String>> processParametersForVariant( final MappingHttpRequest httpRequest,
                                                                final HmacUtils.SignatureVariant variant ) {
    Map<String,String> result = httpRequest.getParameters();
    if ( variant.getVersion().value() > 2 ) {
      result = Maps.newHashMap();
      for ( final Map.Entry<String,String> entry : httpRequest.getParameters().entrySet() ) {
        if ( httpRequest.isQueryParameter( entry.getKey() ) ) {
          result.put( entry.getKey(), entry.getValue() );
        }
      }
    }
    return Maps.transformValues( result, CollectionUtils.<String>listUnit() );
  }
}
