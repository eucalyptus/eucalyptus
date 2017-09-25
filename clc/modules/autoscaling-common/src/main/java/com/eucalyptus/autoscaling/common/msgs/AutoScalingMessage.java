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
package com.eucalyptus.autoscaling.common.msgs;

import java.lang.reflect.Method;
import java.util.Map;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.util.MessageValidation;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( AutoScaling.class )
public class AutoScalingMessage extends BaseMessage {

  @Override
  public <TYPE extends BaseMessage> TYPE getReply( ) {
    TYPE type = super.getReply( );
    final ResponseMetadata responseMetadata = getResponseMetadata( type );
    if ( responseMetadata != null ) {
      responseMetadata.setRequestId( type.getCorrelationId( ) );
    }
    return type;
  }

  public static ResponseMetadata getResponseMetadata( final BaseMessage message ) {
    try {
      Method responseMetadataMethod = message.getClass( ).getMethod( "getResponseMetadata" );
      return ( (ResponseMetadata) responseMetadataMethod.invoke( message ) );
    } catch ( Exception e ) {
    }

    return null;
  }

  public Map<String, String> validate( ) {
    return MessageValidation.validateRecursively( Maps.newTreeMap( ), new AutoScalingMessageValidation.AutoScalingMessageValidationAssistant( ), "", this );
  }

}
