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
package com.eucalyptus.cloudformation.policy;

import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec;

/**
 *
 */
public class CloudFormationResourceName extends Ern {
  private String type;
  private String id;

  public CloudFormationResourceName( String type, String id ) {
    this.type = type;
    this.id = id;
    this.vendor = CloudFormationPolicySpec.VENDOR_CLOUDFORMATION;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( ARN_PREFIX ).append( this.getVendor( ) ).append( ":::" ).append( this.type ).append( '/' ).append( this.id );
    return sb.toString( );
  }

  @Override
  public String getResourceType( ) {
    return this.vendor + ":" + this.type;
  }

  @Override
  public String getResourceName( ) {
    return this.id;
  }
}
