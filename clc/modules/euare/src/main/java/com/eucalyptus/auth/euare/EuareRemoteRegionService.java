/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare;

import java.io.StringWriter;
import java.net.URI;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.common.identity.IdentityMessage;
import com.eucalyptus.auth.euare.common.identity.TunnelActionResponseType;
import com.eucalyptus.auth.euare.common.identity.TunnelActionType;
import com.eucalyptus.auth.euare.identity.region.RegionInfo;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.EphemeralConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
@ComponentNamed
public class EuareRemoteRegionService {
  private static final Logger logger = Logger.getLogger( EuareRemoteRegionService.class );

  public EuareMessage callRemote( final EuareMessage euareRequest ) throws EucalyptusCloudException {
    final Optional<RegionInfo> regionInfo = EuareRemoteRegionFilter.getRegion( euareRequest );
    final String previousUserId = euareRequest.getUserId( );
    try {
      euareRequest.setUserId( Contexts.lookup( ).getUser( ).getAuthenticatedId( ) );
      final StringWriter writer = new StringWriter();
      try ( final LockResource lock = LockResource.lock( HoldMe.canHas ) ) {
        final OMElement message = BaseMessages.toOm( euareRequest );
        message.addAttribute( "type", euareRequest.getClass( ).getName( ), null );
        final OMOutputFormat format = new OMOutputFormat();
        format.setIgnoreXMLDeclaration( true );
        message.serialize( writer );
      }

      final TunnelActionType tunnelAction = new TunnelActionType( );
      tunnelAction.setContent( writer.toString( ) );
      final TunnelActionResponseType tunnelActionResponse = send( regionInfo, tunnelAction );
      final String responseContent = tunnelActionResponse.getTunnelActionResult( ).getContent( );

      final EuareMessage euareResponse;  //TODO:STEVE: move this message marshalling to a helper (BaseMessages? also IdentityService#tunnelAction)
      try ( final LockResource lock = LockResource.lock( HoldMe.canHas ) ) {
        final StAXOMBuilder omBuilder = HoldMe.getStAXOMBuilder( HoldMe.getXMLStreamReader( responseContent ) );
        final OMElement message = omBuilder.getDocumentElement( );
        final String messageTypeName = message.getAttributeValue( new QName( "type" ) ); 
        final Class<?> messageType = getClass( ).getClassLoader( ).loadClass( messageTypeName );
        if ( !EuareMessage.class.isAssignableFrom( messageType ) ) {
          throw new IllegalArgumentException( "Unsupported type: " + messageTypeName );
        }
        euareResponse = (EuareMessage) BaseMessages.fromOm( message, messageType );
      }
      euareResponse.setCorrelationId( euareRequest.getCorrelationId( ) );
      return euareResponse;
    } catch ( Exception e ) {
      final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
      if ( error.isPresent( ) ) {
        final AsyncWebServiceError webServiceError = error.get( );
        throw new EuareException( HttpResponseStatus.valueOf( webServiceError.getHttpErrorCode( ) ), webServiceError.getCode( ), webServiceError.getMessage( ) );
      }
      throw new EucalyptusCloudException( e );
    } finally {
      euareRequest.setUserId( previousUserId );
    }
  }


  private <R extends IdentityMessage> R send(  //TODO:STEVE: move send to a helper (also RemoteIdentityProvider#send)
      final Optional<RegionInfo> region,
      final IdentityMessage request ) throws Exception {
    final Optional<Set<String>> endpoints = region.transform( RegionInfo.serviceEndpoints( "identity" ) );
    final ServiceConfiguration config = new EphemeralConfiguration(
        ComponentIds.lookup( Identity.class ),
        "identity",
        "identity",
        URI.create( Iterables.get( endpoints.get( ), 0 ) ) ); //TODO:STEVE: endpoint handling
    return AsyncRequests.sendSync( config, request );
  }

}


