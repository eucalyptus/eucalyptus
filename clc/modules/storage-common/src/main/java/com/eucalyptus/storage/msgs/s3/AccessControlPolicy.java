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
package com.eucalyptus.storage.msgs.s3;

import java.util.Objects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AccessControlPolicy extends EucalyptusData {

  private CanonicalUser owner;
  private AccessControlList accessControlList;

  public AccessControlPolicy( ) {
  }

  public AccessControlPolicy( CanonicalUser owner, AccessControlList acl ) {
    this.owner = owner;
    this.accessControlList = acl;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final AccessControlPolicy that = (AccessControlPolicy) o;
    return Objects.equals( getOwner( ), that.getOwner( ) ) &&
        Objects.equals( getAccessControlList( ), that.getAccessControlList( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getOwner( ), getAccessControlList( ) );
  }

  public CanonicalUser getOwner( ) {
    return owner;
  }

  public void setOwner( CanonicalUser owner ) {
    this.owner = owner;
  }

  public AccessControlList getAccessControlList( ) {
    return accessControlList;
  }

  public void setAccessControlList( AccessControlList accessControlList ) {
    this.accessControlList = accessControlList;
  }
}
