/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling;

import java.util.Map;
import com.eucalyptus.autoscaling.common.AutoScalingMessage;

/**
 * Validation component invoked before service actions
 */
public class AutoScalingMessageValidator {

  public AutoScalingMessage validate( final Object object ) throws AutoScalingException {
    // Check type
    if ( !(object instanceof AutoScalingMessage) ) {
      throw new InvalidActionException();
    }

    // Run validation
    final AutoScalingMessage message = AutoScalingMessage.class.cast( object );
    final Map<String,String> validationErrorsByField = message.validate();
    if ( !validationErrorsByField.isEmpty() ) {
      throw new ValidationErrorException( validationErrorsByField.values().iterator().next() );
    }

    return message;
  }
}
