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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.StreamUtil;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.HasTags;

public class RunInstancesType extends VmControlMessage implements HasTags {

  private String imageId;
  private String reservationId;
  private int minCount;
  private int maxCount;
  private String keyName;
  private ArrayList<String> instanceIds = Lists.newArrayList( );
  @HttpParameterMapping( parameter = "SecurityGroup" )
  private ArrayList<String> groupSet = Lists.newArrayList( );
  @HttpParameterMapping( parameter = "SecurityGroupId" )
  private ArrayList<String> groupIdSet = Lists.newArrayList( );
  private ArrayList<GroupItemType> securityGroups = Lists.newArrayList( );
  private String additionalInfo;
  private String userData;
  private String version;
  private String encoding;
  private String addressingType;
  private String instanceType = "m1.small";
  private String kernelId;
  private String ramdiskId;
  @HttpParameterMapping( parameter = "Placement.AvailabilityZone" )
  private String availabilityZone;
  @HttpParameterMapping( parameter = "Placement.GroupName" )
  private String placementGroup = "default";
  @HttpParameterMapping( parameter = "Placement.Tenancy" )
  private String placementTenancy = "default";
  @HttpEmbedded( multiple = true )
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMapping = Lists.newArrayList( );
  @HttpParameterMapping( parameter = "Monitoring.Enabled" )
  private Boolean monitoring = false;
  private String subnetId;
  @HttpParameterMapping( parameter = "DisableApiTermination" )
  private Boolean disableTerminate;
  @HttpParameterMapping( parameter = "InstanceInitiatedShutdownBehavior" )
  private String shutdownAction = "stop";
  /**
   * InstanceLicenseRequest license;
   **/
  private String privateIpAddress;
  private String clientToken;
  @HttpEmbedded
  private InstanceNetworkInterfaceSetRequestType networkInterfaceSet;
  @HttpParameterMapping( parameter = "IamInstanceProfile.Arn" )
  private String iamInstanceProfileArn;
  @HttpParameterMapping( parameter = "IamInstanceProfile.Name" )
  private String iamInstanceProfileName;
  private ArrayList<Integer> networkIndexList = Lists.newArrayList( );
  private String privateMacBase;
  private String publicMacBase;
  private int macLimit;
  private int vlan;
  private Boolean ebsOptimized = Boolean.FALSE;
  @HttpEmbedded( multiple = true )
  private ArrayList<ResourceTagSpecification> tagSpecification = new ArrayList<ResourceTagSpecification>( );

  public Set<String> securityGroupNames( ) {
    Set<String> names = Sets.newLinkedHashSet( );
    names.addAll( groupSet );
    names.addAll( StreamUtil.ofAll( groupIdSet ).filter( Strings.startsWith( "sg-" ).negate( ) ).toJavaList( ) ); // ID was historically the name
    names.addAll( StreamUtil.ofAll( securityGroups ).map( GroupItemType.groupName( ) ).filter( CollectionUtils.isNotNull( ) ).toJavaList( ) );
    names.addAll( StreamUtil.ofAll( securityGroups ).map( GroupItemType.groupId( ) ).filter( Strings.startsWith( "sg-" ).negate( ).and( CollectionUtils.isNotNull( ) ) ).toJavaList( ) );
    return names;
  }

  public Set<String> securityGroupsIds( ) {
    Set<String> names = Sets.newLinkedHashSet( );
    names.addAll( StreamUtil.ofAll( groupIdSet ).filter( Strings.startsWith( "sg-" ) ).toJavaList( ) ); // ID was historically the name
    names.addAll( StreamUtil.ofAll( securityGroups ).map( GroupItemType.groupId( ) ).filter( Strings.startsWith( "sg-" ) ).toJavaList( ) );
    return names;
  }

  @SuppressWarnings( "unchecked" )
  public Object clone( ) throws CloneNotSupportedException {
    RunInstancesType c = (RunInstancesType) super.clone( );
    c.instanceIds = (ArrayList<String>) this.instanceIds.clone( );
    c.groupSet = (ArrayList<String>) this.groupSet.clone( );
    c.groupIdSet = (ArrayList<String>) this.groupIdSet.clone( );
    c.securityGroups = Lists.newArrayList( );
    if ( this.securityGroups != null ) for ( GroupItemType groupItemType : this.securityGroups )
      c.securityGroups.add( (GroupItemType) groupItemType.clone( ) );
    c.blockDeviceMapping = Lists.newArrayList( );
    if ( this.blockDeviceMapping != null ) for ( BlockDeviceMappingItemType b : this.blockDeviceMapping )
      c.blockDeviceMapping.add( (BlockDeviceMappingItemType) b.clone( ) );
    return c;
  }

  public void setInstanceProfileNameOrArn( String nameOrArn ) {
    if ( nameOrArn.startsWith( "arn:" ) ) {
      this.iamInstanceProfileArn = nameOrArn;
    } else {
      this.iamInstanceProfileName = nameOrArn;
    }

  }

  public InstanceNetworkInterfaceSetItemRequestType primaryNetworkInterface( boolean create ) {
    if ( networkInterfaceSet == null ) {
      networkInterfaceSet = new InstanceNetworkInterfaceSetRequestType( );
    }

    InstanceNetworkInterfaceSetItemRequestType primary = null;
    for ( InstanceNetworkInterfaceSetItemRequestType item : networkInterfaceSet.getItem( ) ) {
      if ( item.getDeviceIndex( ) == 0 ) {
        primary = item;
        break;
      }

    }

    if ( primary == null && create ) {
      primary = new InstanceNetworkInterfaceSetItemRequestType( 0 );
      networkInterfaceSet.getItem( ).add( primary );
    }

    return primary;
  }

  @Override
  public Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    return getTagKeys( tagSpecification, resourceType, resourceId );
  }

  @Override
  public String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull String tagKey ) {
    return getTagValue( tagSpecification, resourceType, resourceId, tagKey );
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getReservationId( ) {
    return reservationId;
  }

  public void setReservationId( String reservationId ) {
    this.reservationId = reservationId;
  }

  public int getMinCount( ) {
    return minCount;
  }

  public void setMinCount( int minCount ) {
    this.minCount = minCount;
  }

  public int getMaxCount( ) {
    return maxCount;
  }

  public void setMaxCount( int maxCount ) {
    this.maxCount = maxCount;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public ArrayList<String> getInstanceIds( ) {
    return instanceIds;
  }

  public void setInstanceIds( ArrayList<String> instanceIds ) {
    this.instanceIds = instanceIds;
  }

  public ArrayList<String> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<String> groupSet ) {
    this.groupSet = groupSet;
  }

  public ArrayList<String> getGroupIdSet( ) {
    return groupIdSet;
  }

  public void setGroupIdSet( ArrayList<String> groupIdSet ) {
    this.groupIdSet = groupIdSet;
  }

  public ArrayList<GroupItemType> getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( ArrayList<GroupItemType> securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public String getAdditionalInfo( ) {
    return additionalInfo;
  }

  public void setAdditionalInfo( String additionalInfo ) {
    this.additionalInfo = additionalInfo;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }

  public String getVersion( ) {
    return version;
  }

  public void setVersion( String version ) {
    this.version = version;
  }

  public String getEncoding( ) {
    return encoding;
  }

  public void setEncoding( String encoding ) {
    this.encoding = encoding;
  }

  public String getAddressingType( ) {
    return addressingType;
  }

  public void setAddressingType( String addressingType ) {
    this.addressingType = addressingType;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKernelId( ) {
    return kernelId;
  }

  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId( ) {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getPlacementGroup( ) {
    return placementGroup;
  }

  public void setPlacementGroup( String placementGroup ) {
    this.placementGroup = placementGroup;
  }

  public String getPlacementTenancy( ) {
    return placementTenancy;
  }

  public void setPlacementTenancy( String placementTenancy ) {
    this.placementTenancy = placementTenancy;
  }

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMapping( ) {
    return blockDeviceMapping;
  }

  public void setBlockDeviceMapping( ArrayList<BlockDeviceMappingItemType> blockDeviceMapping ) {
    this.blockDeviceMapping = blockDeviceMapping;
  }

  public Boolean getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( Boolean monitoring ) {
    this.monitoring = monitoring;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public Boolean getDisableTerminate( ) {
    return disableTerminate;
  }

  public void setDisableTerminate( Boolean disableTerminate ) {
    this.disableTerminate = disableTerminate;
  }

  public String getShutdownAction( ) {
    return shutdownAction;
  }

  public void setShutdownAction( String shutdownAction ) {
    this.shutdownAction = shutdownAction;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public InstanceNetworkInterfaceSetRequestType getNetworkInterfaceSet( ) {
    return networkInterfaceSet;
  }

  public void setNetworkInterfaceSet( InstanceNetworkInterfaceSetRequestType networkInterfaceSet ) {
    this.networkInterfaceSet = networkInterfaceSet;
  }

  public String getIamInstanceProfileArn( ) {
    return iamInstanceProfileArn;
  }

  public void setIamInstanceProfileArn( String iamInstanceProfileArn ) {
    this.iamInstanceProfileArn = iamInstanceProfileArn;
  }

  public String getIamInstanceProfileName( ) {
    return iamInstanceProfileName;
  }

  public void setIamInstanceProfileName( String iamInstanceProfileName ) {
    this.iamInstanceProfileName = iamInstanceProfileName;
  }

  public ArrayList<Integer> getNetworkIndexList( ) {
    return networkIndexList;
  }

  public void setNetworkIndexList( ArrayList<Integer> networkIndexList ) {
    this.networkIndexList = networkIndexList;
  }

  public String getPrivateMacBase( ) {
    return privateMacBase;
  }

  public void setPrivateMacBase( String privateMacBase ) {
    this.privateMacBase = privateMacBase;
  }

  public String getPublicMacBase( ) {
    return publicMacBase;
  }

  public void setPublicMacBase( String publicMacBase ) {
    this.publicMacBase = publicMacBase;
  }

  public int getMacLimit( ) {
    return macLimit;
  }

  public void setMacLimit( int macLimit ) {
    this.macLimit = macLimit;
  }

  public int getVlan( ) {
    return vlan;
  }

  public void setVlan( int vlan ) {
    this.vlan = vlan;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public ArrayList<ResourceTagSpecification> getTagSpecification( ) {
    return tagSpecification;
  }

  public void setTagSpecification( ArrayList<ResourceTagSpecification> tagSpecification ) {
    this.tagSpecification = tagSpecification;
  }
}
