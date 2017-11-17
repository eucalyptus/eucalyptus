/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
