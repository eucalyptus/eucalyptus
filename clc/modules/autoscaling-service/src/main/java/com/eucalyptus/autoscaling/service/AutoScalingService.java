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
package com.eucalyptus.autoscaling.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Map;
import java.util.NoSuchElementException;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.component.ComponentException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.autoscaling.common.AutoScalingBackend;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingBackendMessage;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import com.google.common.base.Objects;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
public class AutoScalingService {

  public AutoScalingMessage dispatchAction( final AutoScalingMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );

    // Authorization check
    if ( !Permissions.perhapsAuthorized( PolicySpec.VENDOR_AUTOSCALING, getIamActionByMessageType( request ), user ) ) {
      throw new AutoScalingAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    // Validation
    final Map<String,String> validationErrorsByField = request.validate();
    if ( !validationErrorsByField.isEmpty() ) {
      throw new AutoScalingClientException( "ValidationError", validationErrorsByField.values().iterator().next() );
    }

    // Dispatch
    try {
      final AutoScalingBackendMessage backendRequest = (AutoScalingBackendMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final AutoScalingMessage response = (AutoScalingMessage) BaseMessages.deepCopy( backendResponse, request.getReply().getClass() );
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws ClassNotFoundException {
    return Class.forName( request.getClass( ).getName( ).replace( ".common.msgs.", ".common.backend.msgs." ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( AutoScalingBackend.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingUnavailableException( "Service Unavailable" );
    } catch ( ServiceDispatchException e ) {
      final ComponentException componentException = Exceptions.findCause( e, ComponentException.class );
      if ( componentException != null && componentException.getCause( ) instanceof Exception ) {
        throw (Exception) componentException.getCause( );
      }
      throw e;
    } catch ( final FailedRequestException e ) {
      if ( request.getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new AutoScalingException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final EucalyptusRemoteFault remoteFault = Exceptions.findCause( e, EucalyptusRemoteFault.class );
    if ( remoteFault != null ) {
      final HttpResponseStatus status = Objects.firstNonNull( remoteFault.getStatus( ), HttpResponseStatus.INTERNAL_SERVER_ERROR );
      final String code = remoteFault.getFaultCode( );
      final String message = remoteFault.getFaultDetail( );
      switch( status.getCode( ) ) {
        case 400:
          throw new AutoScalingClientException( code, message );
        case 403:
          throw new AutoScalingAuthorizationException( code, message );
        case 503:
          throw new AutoScalingUnavailableException( message );
        default:
          throw new AutoScalingException( code, Role.Receiver, message );
      }
    }
  }
}
