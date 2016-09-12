/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging.service;

import com.eucalyptus.auth.AuthContextSupplier;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.imaging.common.GetInstanceImportTaskType;
import com.eucalyptus.imaging.common.ImagingMessage;
import com.eucalyptus.imaging.common.PutInstanceImportTaskStatusType;
import com.eucalyptus.imaging.common.backend.msgs.ImagingBackendMessage;
import com.eucalyptus.imaging.common.ImagingBackend;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;
import com.google.common.base.Optional;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

/**
 *
 */
@SuppressWarnings( "UnusedDeclaration" )
@ComponentNamed
public class ImagingService {

  public ImagingMessage dispatchAction( final ImagingMessage request ) throws EucalyptusCloudException {
    // Authorization
    final Context ctx = Contexts.lookup( );
    final AuthContextSupplier user = ctx.getAuthContext( );
   
    // Dispatch
    try {
      final ImagingBackendMessage backendRequest = (ImagingBackendMessage) BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final InetSocketAddress remoteAddr = ( ( InetSocketAddress ) ctx.getChannel( ).getRemoteAddress( ) );
      if (remoteAddr != null) {
        final String remoteHost = remoteAddr.getAddress( ).getHostAddress( );
        if (request instanceof GetInstanceImportTaskType) {
          ((com.eucalyptus.imaging.common.backend.msgs.GetInstanceImportTaskType) backendRequest).setSourceIp(remoteHost);
        }else if (request instanceof PutInstanceImportTaskStatusType) {
          ((com.eucalyptus.imaging.common.backend.msgs.PutInstanceImportTaskStatusType) backendRequest).setSourceIp(remoteHost);
        }
      }
      final BaseMessage backendResponse = send( backendRequest );
      final ImagingMessage response = (ImagingMessage) BaseMessages.deepCopy( backendResponse, request.getReply( ).getClass( ) );
      response.setCorrelationId( request.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      handleRemoteException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws ClassNotFoundException {
    return Class.forName( request.getClass( ).getName( ).replace( ".common.", ".common.backend.msgs." ) );
  }

  private static BaseMessage send( final ImagingBackendMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( ImagingBackend.class ), (BaseMessage)request );
    } catch ( NoSuchElementException e ) {
      throw new ImagingUnavailableException( "Service Unavailable" );
    } catch ( final FailedRequestException e ) {
      if ( ((BaseMessage)request).getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new ImagingException( "InternalError", Role.Receiver, "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleRemoteException( final Exception e ) throws EucalyptusCloudException {
    final Optional<AsyncExceptions.AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( e );
    if ( serviceErrorOption.isPresent( ) ) {
      final AsyncExceptions.AsyncWebServiceError serviceError = serviceErrorOption.get( );
      final String code = serviceError.getCode( );
      final String message = serviceError.getMessage( );
      switch( serviceError.getHttpErrorCode( ) ) {
        case 400:
          throw new ImagingClientException( code, message );
        case 403:
          throw new ImagingAuthorizationException( code, message );
        case 409:
          throw new ImagingInvalidConfigurationException( code, message );
        case 503:
          throw new ImagingUnavailableException( message );
        default:
          throw new ImagingException( code, Role.Receiver, message );
      }
    }
  }
}
