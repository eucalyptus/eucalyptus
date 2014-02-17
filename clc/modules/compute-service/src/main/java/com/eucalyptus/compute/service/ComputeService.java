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
import java.util.UUID;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;


/**
 *
 */
public class ComputeService {

  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    mapper.getSerializationConfig().addMixInAnnotations( BaseMessage.class, BaseMessageMixIn.class);
    mapper.getDeserializationConfig().addMixInAnnotations( BaseMessage.class, BaseMessageMixIn.class);
    mapper.getSerializationConfig().set( SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false );
  }

  public ComputeMessage dispatchAction( final ComputeMessage message ) throws EucalyptusCloudException {
    final User user = Contexts.lookup().getUser();
    if ( !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.ALL_RESOURCE, "", null, getIamActionByMessageType( message ), user ) ) {
      throw new ComputeServiceAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    try {
      final Binding binding = BindingManager.getDefaultBinding( );
      final Class eucaClass = binding.getElementClass( "Eucalyptus." + message.getClass( ).getSimpleName( ) );
      final BaseMessage eucaMessage = (BaseMessage) mapper.readValue( mapper.valueToTree( message ),  eucaClass );
      final BaseMessage result = AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( Eucalyptus.class ), eucaMessage );
      final ComputeMessage response = (ComputeMessage) mapper.readValue( mapper.valueToTree( result ), message.getReply().getClass() );
      response.setCorrelationId( message.getCorrelationId() );
      return response;
    } catch ( Exception e ) {
      //TODO:STEVE: Handle errors from remote components
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId" } )
  private static final class BaseMessageMixIn { }
}
