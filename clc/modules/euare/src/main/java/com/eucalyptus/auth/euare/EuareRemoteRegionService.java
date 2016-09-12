/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare;

import java.io.StringWriter;
import java.net.URI;
import java.util.Set;
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
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingManager;
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

/**
 *
 */
@ComponentNamed
public class EuareRemoteRegionService {
  private static final Logger logger = Logger.getLogger( EuareRemoteRegionService.class );

  public EuareMessage callRemote( final EuareMessage euareRequest ) throws EucalyptusCloudException {
    final Optional<RegionInfo> regionInfo = EuareRemoteRegionFilter.getRegion( euareRequest );
    final String previousUserId = euareRequest.getUserId( );
    final Binding binding = BindingManager.getDefaultBinding( );
    try {
      euareRequest.setUserId( Contexts.lookup( ).getUser( ).getAuthenticatedId( ) );
      final StringWriter writer = new StringWriter();
      try ( final LockResource lock = LockResource.lock( HoldMe.canHas ) ) {
        final OMElement message = binding.toOM( euareRequest );
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
        final Class<?> messageType = binding.getElementClass( message.getLocalName( ) );
        euareResponse = (EuareMessage) binding.fromOM( message, messageType ); //TODO:STEVE: allow for (subminor?) version differences
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


