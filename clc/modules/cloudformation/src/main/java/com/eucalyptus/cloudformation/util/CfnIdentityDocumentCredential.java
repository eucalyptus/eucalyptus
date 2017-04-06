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
package com.eucalyptus.cloudformation.util;

import javax.annotation.Nonnull;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class CfnIdentityDocumentCredential {

  @Nonnull
  private final String instanceId;


  private CfnIdentityDocumentCredential( @Nonnull final String instanceId ) {
    this.instanceId = Assert.notNull( instanceId, "instanceId" );
  }

  public static CfnIdentityDocumentCredential of( final String instanceId ) {
    return new CfnIdentityDocumentCredential( instanceId );
  }

  @Nonnull
  public String getInstanceId( ) {
    return instanceId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper( this )
        .add( "instanceId", instanceId )
        .toString( );
  }
}
