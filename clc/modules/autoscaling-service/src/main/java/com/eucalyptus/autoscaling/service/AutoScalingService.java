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

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.autoscaling.common.AutoScalingBackend;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingBackendMessage;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.ws.Role;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
public class AutoScalingService {
  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    mapper.getSerializationConfig().addMixInAnnotations( BaseMessage.class, BaseMessageMixIn.class);
    mapper.getDeserializationConfig().addMixInAnnotations( BaseMessage.class, BaseMessageMixIn.class);
  }

  public AutoScalingMessage dispatchAction( final AutoScalingMessage message ) throws EucalyptusCloudException {
    final User user = Contexts.lookup().getUser();

    // Authorization check
    if ( !Permissions.isAuthorized( PolicySpec.VENDOR_AUTOSCALING, PolicySpec.ALL_RESOURCE, "", null, getIamActionByMessageType( message ), user ) ) {
      throw new AutoScalingException( "UnauthorizedOperation", Role.Sender, "You are not authorized to perform this operation." ); //TODO:STEVE: find the right error code/text
    }

    // Validation
    final Map<String,String> validationErrorsByField = message.validate();
    if ( !validationErrorsByField.isEmpty() ) {
      throw new ClientScalingException( "ValidationError", validationErrorsByField.values().iterator().next() );
    }

    // Dispatch
    try {
      final AutoScalingBackendMessage out = (AutoScalingBackendMessage) mapper.readValue( mapper.valueToTree( message ), Class.forName( message.getClass().getName().replace( ".common.msgs.", ".common.backend.msgs." ) ) );
      final BaseMessage result = AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( AutoScalingBackend.class ), out );
      final AutoScalingMessage response = (AutoScalingMessage) mapper.readValue( mapper.valueToTree( result ), message.getReply( ).getClass( ) );
      response.setCorrelationId( message.getCorrelationId( ) );
      return response;
    } catch ( Exception e ) {
      //TODO:STEVE: Handle errors from remote components
      throw new EucalyptusCloudException( e );
    }
  }

  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId" } )
  private static final class BaseMessageMixIn { }
}
