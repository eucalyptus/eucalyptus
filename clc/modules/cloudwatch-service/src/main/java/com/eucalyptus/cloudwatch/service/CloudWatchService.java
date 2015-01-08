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
package com.eucalyptus.cloudwatch.service;

import com.eucalyptus.auth.AuthContextSupplier;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.NoSuchElementException;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.component.ComponentException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.backend.msgs.CloudWatchBackendMessage;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
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
public class CloudWatchService {

  public CloudWatchMessage dispatchAction( final CloudWatchMessage request ) throws EucalyptusCloudException {
    final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );
    if ( !Permissions.perhapsAuthorized( PolicySpec.VENDOR_CLOUDWATCH, getIamActionByMessageType( request ), user ) ) {
      throw new CloudWatchAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    try {
      final CloudWatchBackendMessage backendRequest = (CloudWatchBackendMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final CloudWatchMessage response = (CloudWatchMessage) BaseMessages.deepCopy( backendResponse, request.getReply().getClass() );
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
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( CloudWatchBackend.class ), request );
    } catch ( NoSuchElementException e ) {
      throw new CloudWatchUnavailableException( "Service Unavailable" );
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
          new CloudWatchException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
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
          throw new CloudWatchClientException( code, message );
        case 403:
          throw new CloudWatchAuthorizationException( code, message );
        case 404:
          throw new CloudWatchNotFoundException( code, message );
        case 503:
          throw new CloudWatchUnavailableException( message );
        default:
          throw new CloudWatchException( code, Role.Receiver, message );
      }
    }
  }
}
