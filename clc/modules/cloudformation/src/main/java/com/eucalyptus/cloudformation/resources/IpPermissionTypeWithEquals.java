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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import com.eucalyptus.compute.common.CidrIpType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class IpPermissionTypeWithEquals {

  private String ipProtocol;
  private Integer fromPort;
  private Integer toPort;
  private ArrayList<UserIdGroupPairTypeWithEquals> groups = new ArrayList<UserIdGroupPairTypeWithEquals>( );
  private ArrayList<CidrIpTypeWithEquals> ipRanges = new ArrayList<CidrIpTypeWithEquals>( );

  public IpPermissionTypeWithEquals( IpPermissionType ipPermissionType ) {
    this.ipProtocol = ipPermissionType.getIpProtocol( );
    this.fromPort = ipPermissionType.getFromPort( );
    this.toPort = ipPermissionType.getToPort( );
    this.groups = convertGroups( ipPermissionType.getGroups( ) );
    this.ipRanges = convertRanges( ipPermissionType.getIpRanges( ) );
  }

  public static Collection<IpPermissionTypeWithEquals> getNonNullCollection( Collection<IpPermissionType> ipPermissionTypes ) {
    if ( ipPermissionTypes == null ) return Collections.emptyList( );
    Collection<IpPermissionTypeWithEquals> retVal = Lists.newArrayList( );
    for ( IpPermissionType ipPermissionType : ipPermissionTypes ) {
      ( (ArrayList<IpPermissionTypeWithEquals>) retVal ).add( new IpPermissionTypeWithEquals( ipPermissionType ) );
    }

    return retVal;
  }

  private ArrayList<UserIdGroupPairTypeWithEquals> convertGroups( ArrayList<UserIdGroupPairType> groups ) {
    if ( groups == null ) return null;
    ArrayList<UserIdGroupPairTypeWithEquals> newGroups = Lists.newArrayList( );
    for ( UserIdGroupPairType group : groups ) {
      newGroups.add( new UserIdGroupPairTypeWithEquals( group ) );
    }

    return newGroups;
  }

  private ArrayList<UserIdGroupPairType> convertGroupsBack( ArrayList<UserIdGroupPairTypeWithEquals> groups ) {
    if ( groups == null ) return null;
    ArrayList<UserIdGroupPairType> newGroups = Lists.newArrayList( );
    for ( UserIdGroupPairTypeWithEquals group : groups ) {
      newGroups.add( group.getUserIdGroupPairType( ) );
    }

    return newGroups;
  }

  private ArrayList<CidrIpTypeWithEquals> convertRanges( ArrayList<CidrIpType> ranges ) {
    if ( ranges == null ) return null;
    ArrayList<CidrIpTypeWithEquals> newRanges = Lists.newArrayList( );
    for ( CidrIpType group : ranges ) {
      newRanges.add( new CidrIpTypeWithEquals( group ) );
    }

    return newRanges;
  }

  private ArrayList<CidrIpType> convertRangesBack( ArrayList<CidrIpTypeWithEquals> ranges ) {
    if ( ranges == null ) return null;
    ArrayList<CidrIpType> newRanges = Lists.newArrayList( );
    for ( CidrIpTypeWithEquals group : ranges ) {
      newRanges.add( group.getCidrIpType( ) );
    }

    return newRanges;
  }

  public IpPermissionType getIpPermissionType( ) {
    IpPermissionType ipPermissionType = new IpPermissionType( );
    ipPermissionType.setIpProtocol( ipProtocol );
    ipPermissionType.setFromPort( fromPort );
    ipPermissionType.setToPort( toPort );
    ipPermissionType.setGroups( convertGroupsBack( groups ) );
    ipPermissionType.setIpRanges( convertRangesBack( ipRanges ) );
    return ipPermissionType;
  }

  public String getIpProtocol( ) {
    return ipProtocol;
  }

  public void setIpProtocol( String ipProtocol ) {
    this.ipProtocol = ipProtocol;
  }

  public Integer getFromPort( ) {
    return fromPort;
  }

  public void setFromPort( Integer fromPort ) {
    this.fromPort = fromPort;
  }

  public Integer getToPort( ) {
    return toPort;
  }

  public void setToPort( Integer toPort ) {
    this.toPort = toPort;
  }

  public ArrayList<UserIdGroupPairTypeWithEquals> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<UserIdGroupPairTypeWithEquals> groups ) {
    this.groups = groups;
  }

  public ArrayList<CidrIpTypeWithEquals> getIpRanges( ) {
    return ipRanges;
  }

  public void setIpRanges( ArrayList<CidrIpTypeWithEquals> ipRanges ) {
    this.ipRanges = ipRanges;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final IpPermissionTypeWithEquals that = (IpPermissionTypeWithEquals) o;
    return Objects.equals( getIpProtocol( ), that.getIpProtocol( ) ) &&
        Objects.equals( getFromPort( ), that.getFromPort( ) ) &&
        Objects.equals( getToPort( ), that.getToPort( ) ) &&
        Objects.equals( getGroups( ), that.getGroups( ) ) &&
        Objects.equals( getIpRanges( ), that.getIpRanges( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getIpProtocol( ), getFromPort( ), getToPort( ), getGroups( ), getIpRanges( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "ipProtocol", ipProtocol )
        .add( "fromPort", fromPort )
        .add( "toPort", toPort )
        .add( "groups", groups )
        .add( "ipRanges", ipRanges )
        .toString( );
  }
}
