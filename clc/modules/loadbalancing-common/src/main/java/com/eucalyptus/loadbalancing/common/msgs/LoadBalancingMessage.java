/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.loadbalancing.common.msgs;

import java.lang.reflect.Method;
import java.util.Map;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.util.MessageValidation;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( LoadBalancing.class )
public class LoadBalancingMessage extends BaseMessage implements MessageValidation.ValidatableMessage {

  public static ResponseMetadata getResponseMetadata( final BaseMessage message ) {
    try {
      Method responseMetadataMethod = message.getClass( ).getMethod( "getResponseMetadata" );
      return ( (ResponseMetadata) responseMetadataMethod.invoke( message ) );
    } catch ( Exception e ) {
      return null;
    }

  }

  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    final TYPE type = super.getReply( );
    final ResponseMetadata responseMetadata = getResponseMetadata( type );
    if ( responseMetadata != null ) {
      responseMetadata.setRequestId( type.getCorrelationId( ) );
    }
    return type;
  }

  public Map<String, String> validate( ) {
    return MessageValidation.validateRecursively(
        Maps.newTreeMap( ),
        new LoadBalancingMessageValidation.LoadBalancingMessageValidationAssistant( ),
        "",
        this );
  }
}
