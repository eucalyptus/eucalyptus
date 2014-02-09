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
package com.eucalyptus.autoscaling.service.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
@ComponentPart( AutoScaling.class )
public class AutoScalingQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String SCALING_NAMESPACE_PATTERN = "http://autoscaling.amazonaws.com/doc/%s/";
  static final String SCALING_DEFAULT_VERSION = "2011-01-01";
  static final String SCALING_DEFAULT_NAMESPACE = String.format( SCALING_NAMESPACE_PATTERN, SCALING_DEFAULT_VERSION );

  public AutoScalingQueryBinding() {
    super( SCALING_NAMESPACE_PATTERN, SCALING_DEFAULT_VERSION, OperationParameter.Action, OperationParameter.Operation );
  }
}
