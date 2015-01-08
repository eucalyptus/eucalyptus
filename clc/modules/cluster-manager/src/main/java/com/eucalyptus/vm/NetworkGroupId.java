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
package com.eucalyptus.vm;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import com.google.common.base.Objects;

/**
 *
 */
@Embeddable
public class NetworkGroupId implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_group_id" )
  private String groupId;

  @Column( name = "metadata_group_name" )
  private String groupName;

  public NetworkGroupId( ) {
  }

  public NetworkGroupId( final String groupId,
                         final String groupName ) {
    this.groupId = groupId;
    this.groupName = groupName;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( final String groupName ) {
    this.groupName = groupName;
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( final String groupId ) {
    this.groupId = groupId;
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final NetworkGroupId that = (NetworkGroupId) o;

    if ( !groupId.equals( that.groupId ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return groupId.hashCode();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper( this )
        .add( "groupId", getGroupId( ) )
        .add( "groupName", getGroupName( ) )
        .toString();

  }
}
