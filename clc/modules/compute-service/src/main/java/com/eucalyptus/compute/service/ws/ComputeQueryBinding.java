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
package com.eucalyptus.compute.service.ws;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ErrorResponse;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
@ComponentPart( Compute.class )
public class ComputeQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String COMPUTE_NAMESPACE_PATTERN = "http://ec2.amazonaws.com/doc/%s/";
  static final String COMPUTE_DEFAULT_VERSION = "2013-10-15";
  static final String COMPUTE_DEFAULT_NAMESPACE = String.format( COMPUTE_NAMESPACE_PATTERN, COMPUTE_DEFAULT_VERSION );

  public ComputeQueryBinding( ) {
    super( COMPUTE_NAMESPACE_PATTERN, COMPUTE_DEFAULT_VERSION, UnknownParameterStrategy.ERROR, OperationParameter.Action, OperationParameter.Operation );
  }

  @Override
  protected String getNamespaceOverride( @Nonnull final Object message, @Nullable final String namespace ) {
    if ( message instanceof ErrorResponse )  {
      return "";
    }
    return super.getNamespaceOverride( message, namespace );
  }
}
