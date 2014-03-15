/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.service;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.NoSuchElementException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import com.google.common.base.Objects;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;


/**
 *
 */
public class ComputeService {

  public ComputeMessage dispatchAction( final ComputeMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );
    if ( !Permissions.perhapsAuthorized( PolicySpec.VENDOR_EC2, getIamActionByMessageType( request ), user ) ) {
      throw new ComputeServiceAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    try {
      final BaseMessage backendRequest = BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final ComputeMessage response =
          (ComputeMessage) BaseMessages.deepCopy( backendResponse, request.getReply( ).getClass( ) );
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws BindingException {
    final Binding binding = BindingManager.getDefaultBinding( );
    return binding.getElementClass( "Eucalyptus." + request.getClass( ).getSimpleName( ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( Eucalyptus.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new ComputeServiceUnavailableException( "Service Unavailable" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final EucalyptusRemoteFault remoteFault = Exceptions.findCause( e, EucalyptusRemoteFault.class );
    if ( remoteFault != null ) {
      final HttpResponseStatus status = Objects.firstNonNull( remoteFault.getStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR );
      final String code = remoteFault.getFaultCode( );
      final String message = remoteFault.getFaultDetail( );
      switch( status.getCode( ) ) {
        case 400:
          throw new ComputeServiceClientException( code, message );
        case 403:
          throw new ComputeServiceAuthorizationException( code, message );
        case 503:
          throw new ComputeServiceUnavailableException( message );
        default:
          throw new ComputeServiceException( code, message );
      }
    }
  }
}
