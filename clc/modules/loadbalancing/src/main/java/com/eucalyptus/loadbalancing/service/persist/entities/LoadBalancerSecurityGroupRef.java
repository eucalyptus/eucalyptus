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
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerSecurityGroupRefView;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;

/**
 * Reference to a VPC security group.
 */
@Embeddable
public class LoadBalancerSecurityGroupRef implements LoadBalancerSecurityGroupRefView, Serializable {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_group_id", nullable = false, updatable = false )
  private String groupId;

  @Column( name = "metadata_group_name", nullable = false, updatable = false )
  private String groupName;

  public LoadBalancerSecurityGroupRef( ) {
  }

  public LoadBalancerSecurityGroupRef( final String groupId,
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

    final LoadBalancerSecurityGroupRef that = (LoadBalancerSecurityGroupRef) o;

    if ( !groupId.equals( that.groupId ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return groupId.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper( this )
        .add( "groupId", getGroupId( ) )
        .add( "groupName", getGroupName( ) )
        .toString();

  }

  public static NonNullFunction<LoadBalancerSecurityGroupRef,String> groupId( ) {
    return StringPropertyFunctions.GroupId;
  }

  public static NonNullFunction<LoadBalancerSecurityGroupRef,String> groupName( ) {
    return StringPropertyFunctions.GroupName;
  }

  private static enum StringPropertyFunctions implements NonNullFunction<LoadBalancerSecurityGroupRef,String> {
    GroupId {
      @Nonnull
      @Override
      public String apply( final LoadBalancerSecurityGroupRef loadBalancerSecurityGroupRef ) {
        return loadBalancerSecurityGroupRef.getGroupId( );
      }
    },
    GroupName {
      @Nonnull
      @Override
      public String apply( final LoadBalancerSecurityGroupRef loadBalancerSecurityGroupRef ) {
        return loadBalancerSecurityGroupRef.getGroupName();
      }
    },
  }

  @TypeMapper
  public enum SecurityGroupItemTypeToLoadBalancerSecurityGroupRefTransform
      implements Function<SecurityGroupItemType,LoadBalancerSecurityGroupRef> {
    INSTANCE;

    @Nullable
    @Override
    public LoadBalancerSecurityGroupRef apply( @Nullable final SecurityGroupItemType securityGroupItemType ) {
      return securityGroupItemType == null ?
          null :
          new LoadBalancerSecurityGroupRef( securityGroupItemType.getGroupId( ), securityGroupItemType.getGroupName( ) );
    }
  }
}