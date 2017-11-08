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
package com.eucalyptus.cloudformation.resources;

import java.util.Objects;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.google.common.base.MoreObjects;

public class UserIdGroupPairTypeWithEquals {


  private String sourceUserId;
  private String sourceGroupName;
  private String sourceGroupId;

  public UserIdGroupPairTypeWithEquals( UserIdGroupPairType userIdGroupPairType ) {
    this.sourceUserId = userIdGroupPairType.getSourceUserId( );
    this.sourceGroupName = userIdGroupPairType.getSourceGroupName( );
    this.sourceGroupId = userIdGroupPairType.getSourceGroupId( );
  }

  public UserIdGroupPairType getUserIdGroupPairType( ) {
    UserIdGroupPairType userIdGroupPairType = new UserIdGroupPairType( );
    userIdGroupPairType.setSourceUserId( sourceUserId );
    userIdGroupPairType.setSourceGroupName( sourceGroupName );
    userIdGroupPairType.setSourceGroupId( sourceGroupId );
    return userIdGroupPairType;
  }

  public String getSourceUserId( ) {
    return sourceUserId;
  }

  public void setSourceUserId( String sourceUserId ) {
    this.sourceUserId = sourceUserId;
  }

  public String getSourceGroupName( ) {
    return sourceGroupName;
  }

  public void setSourceGroupName( String sourceGroupName ) {
    this.sourceGroupName = sourceGroupName;
  }

  public String getSourceGroupId( ) {
    return sourceGroupId;
  }

  public void setSourceGroupId( String sourceGroupId ) {
    this.sourceGroupId = sourceGroupId;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final UserIdGroupPairTypeWithEquals that = (UserIdGroupPairTypeWithEquals) o;
    return Objects.equals( getSourceUserId( ), that.getSourceUserId( ) ) &&
        Objects.equals( getSourceGroupName( ), that.getSourceGroupName( ) ) &&
        Objects.equals( getSourceGroupId( ), that.getSourceGroupId( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getSourceUserId( ), getSourceGroupName( ), getSourceGroupId( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "sourceUserId", sourceUserId )
        .add( "sourceGroupName", sourceGroupName )
        .add( "sourceGroupId", sourceGroupId )
        .toString( );
  }
}
