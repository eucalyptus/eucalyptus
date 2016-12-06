/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded;

public class UnimplementedMessage extends ComputeMessage {
  
  public UnimplementedMessage( ) {
    super( );
  }
  
  public UnimplementedMessage( ComputeMessage msg ) {
    super( msg );
  }
  
  public UnimplementedMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class ReservedInstanceMessage extends ComputeMessage {}
public class DescribeReservedInstancesOfferingsType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesOfferingId = new ArrayList<String>();
  String availabilityZone;
  String instanceTenancy;
  String instanceType;
  String offeringType
  String productDescription;
  Integer maxInstanceCount
  Boolean includeMarketplace
  Integer maxResults
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeReservedInstancesOfferingsResponseType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesOfferingsSet = new ArrayList<String>();
}
public class DescribeReservedInstancesType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesId = new ArrayList<String>();
  String offeringType
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeReservedInstancesResponseType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesSet = new ArrayList<String>();
}

public class DescribeReservedInstancesListingsType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesListingId = new ArrayList<String>()
  ArrayList<String> reservedInstancesId = new ArrayList<String>()
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeReservedInstancesListingsResponseType extends ReservedInstanceMessage {}

public class DescribeReservedInstancesModificationsType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesModificationId = new ArrayList<String>()
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeReservedInstancesModificationsResponseType extends ReservedInstanceMessage {}

public class PriceScheduleRequestSetItemType extends EucalyptusData {
  Long term
  Double price
  String currencyCode
}

public class CreateReservedInstancesListingType extends ReservedInstanceMessage {
  String reservedInstancesId
  Integer instanceCount
  @HttpEmbedded( multiple = true )
  ArrayList<PriceScheduleRequestSetItemType> priceSchedules
  String clientToken
}

public class CreateReservedInstancesListingResponseType extends ReservedInstanceMessage {}

public class CancelReservedInstancesListingType extends ReservedInstanceMessage {
  String reservedInstancesListingId
}

public class CancelReservedInstancesListingResponseType extends ReservedInstanceMessage {}

public class ReservedInstancesConfigurationSetItemType extends EucalyptusData {
  String availabilityZone
  String platform
  Integer instanceCount
  String instanceType
}

public class ModifyReservedInstancesType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesId
  String clientToken
  @HttpEmbedded(multiple=true)
  ArrayList<ReservedInstancesConfigurationSetItemType> reservedInstancesConfigurationSetItemType
}

public class ModifyReservedInstancesResponseType extends ReservedInstanceMessage {}

public class ReservedInstanceLimitPriceType extends EucalyptusData {
  Double amount
  String currencyCode
}

public class PurchaseReservedInstancesOfferingType extends ReservedInstanceMessage {
  String reservedInstancesOfferingId
  Integer instanceCount
  ReservedInstanceLimitPriceType limitPrice
}

public class PurchaseReservedInstancesOfferingResponseType extends ReservedInstanceMessage {}

class TargetConfigurationRequestType {
  Integer instanceCount
  String offeringId
}

class GetReservedInstancesExchangeQuoteType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstanceId
  ArrayList<TargetConfigurationRequestType> targetConfiguration
}

class GetReservedInstancesExchangeQuoteResponseType extends ReservedInstanceMessage {}

class AcceptReservedInstancesExchangeQuoteType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstanceId
  ArrayList<TargetConfigurationRequestType> targetConfiguration
}

class AcceptReservedInstancesExchangeQuoteResponseType extends ReservedInstanceMessage {}


/** *******************************************************************************/
public class SpotInstanceMessage extends ComputeMessage {}

public class DescribeSpotPriceHistoryType extends SpotInstanceMessage {
  Date startTime
  Date endTime
  String availabilityZone
  ArrayList<String> instanceType
  ArrayList<String> productDescription
  Integer maxResults
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeSpotPriceHistoryResponseType extends SpotInstanceMessage {}

public class DescribeSpotInstanceRequestsType extends SpotInstanceMessage {
  ArrayList<String> spotInstanceRequestId
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeSpotInstanceRequestsResponseType extends SpotInstanceMessage {}

public class CancelSpotInstanceRequestsType extends SpotInstanceMessage {
  ArrayList<String> spotInstanceRequestId
}

public class CancelSpotInstanceRequestsResponseType extends SpotInstanceMessage {}

public class CreateSpotDatafeedSubscriptionType extends SpotInstanceMessage {
  String bucket
  String prefix
}

public class CreateSpotDatafeedSubscriptionResponseType extends SpotInstanceMessage {}

public class DeleteSpotDatafeedSubscriptionType extends SpotInstanceMessage {}

public class DeleteSpotDatafeedSubscriptionResponseType extends SpotInstanceMessage {}

public class DescribeSpotDatafeedSubscriptionType extends SpotInstanceMessage {}

public class DescribeSpotDatafeedSubscriptionResponseType extends SpotInstanceMessage {}

public class LaunchSpecificationRequestType extends EucalyptusData {
  String imageId
  String keyName
  ArrayList<String> securityGroupId
  ArrayList<String> securityGroup
  String userData
  String instanceType
  @HttpParameterMapping (parameter = "Placement.AvailabilityZone")
  String availabilityZone
  @HttpParameterMapping (parameter = "Placement.GroupName")
  String groupName
  String kernelId
  String ramdiskId
  @HttpEmbedded (multiple = true)
  ArrayList<BlockDeviceMappingItemType> blockDeviceMapping
  @HttpParameterMapping (parameter = "Monitoring.Enabled")
  Boolean monitoringEnabled
  String subnetId
  @HttpEmbedded (multiple = true)
  ArrayList<InstanceNetworkInterfaceSetItemRequestType> networkInterface
  @HttpParameterMapping (parameter = "IamInstanceProfile.Arn")
  String instanceProfileArn
  @HttpParameterMapping (parameter = "IamInstanceProfile.Name")
  String instanceProfileName
  Boolean ebsOptimized
}

public class RequestSpotInstancesType extends SpotInstanceMessage {
  String spotPrice
  Integer instanceCount
  String type // one-time | persistent
  Date validFrom
  Date validUntil
  String launchGroup
  String AvailabilityZoneGroup
  LaunchSpecificationRequestType launchSpecification
}

public class RequestSpotInstancesResponseType extends SpotInstanceMessage {}

/** *******************************************************************************/

class CancelSpotFleetRequestsType extends SpotInstanceMessage {
  ArrayList<String> spotFleetRequestId
  Boolean terminateInstances
}

class CancelSpotFleetRequestsResponseType extends SpotInstanceMessage {
}

class DescribeSpotFleetInstancesType extends SpotInstanceMessage {
  Integer maxResults
  String nextToken
  ArrayList<String> spotFleetRequestId
}

class DescribeSpotFleetInstancesResponseType extends SpotInstanceMessage {
}

class DescribeSpotFleetRequestHistoryType extends SpotInstanceMessage {
  String eventType
  Integer maxResults
  String nextToken
  String spotFleetRequestId
  String startTime
}

class DescribeSpotFleetRequestHistoryResponseType extends SpotInstanceMessage {
}

class DescribeSpotFleetRequestsType extends SpotInstanceMessage {
  Integer maxResults
  String nextToken
  ArrayList<String> spotFleetRequestId
}

class DescribeSpotFleetRequestsResponseType extends SpotInstanceMessage {
}

class RequestSpotFleetType extends SpotInstanceMessage {
  SpotFleetRequestConfigType spotFleetRequestConfig
}

class RequestSpotFleetResponseType extends SpotInstanceMessage {

}

class SpotFleetRequestConfigType extends EucalyptusData {
  String clientToken
  String iamFleetRole
  @HttpEmbedded( multiple = true )
  ArrayList<LaunchSpecificationRequestType> launchSpecifications
  String spotPrice
  Integer targetCapacity
  Boolean terminateInstancesWithExpiration
  String validFrom
  String validUntil
}

class ModifySpotFleetRequestType extends SpotInstanceMessage {
  String excessCapacityTerminationPolicy
  String spotFleetRequestId
  Integer targetCapacity
}

class ModifySpotFleetRequestResponseType extends SpotInstanceMessage {
}

/** *******************************************************************************/

public class CreatePlacementGroupType extends VmPlacementMessage {
  String groupName;
  String strategy;
  public CreatePlacementGroupType() {
  }
}
public class CreatePlacementGroupResponseType extends VmPlacementMessage {
  public CreatePlacementGroupResponseType() {
  }
}

public class DeletePlacementGroupType extends VmPlacementMessage {
  String groupName;
  public DeletePlacementGroupType() {
  }
}
public class DeletePlacementGroupResponseType extends VmPlacementMessage {
  public DeletePlacementGroupResponseType() {
  }
}
public class PlacementGroupInfo extends EucalyptusData {
  String groupName;
  String strategy;
  String state;
  public PlacementGroupInfoType() {
  }
}

public class DescribePlacementGroupsType extends VmPlacementMessage {
  @HttpParameterMapping (parameter = "GroupName")
  ArrayList<String> placementGroupSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  public DescribePlacementGroupsType() {
  }
}
public class DescribePlacementGroupsResponseType extends VmPlacementMessage {
  ArrayList<PlacementGroupInfo> placementGroupSet = new ArrayList<PlacementGroupInfo>();

  public DescribePlacementGroupsResponseType() {
  }
}

class ModifyInstancePlacementType extends VmPlacementMessage {
  String affinity
  String hostId
  String instanceId
  String tenancy
}

class ModifyInstancePlacementResponseType extends VmPlacementMessage {
}


/****** flow logs *****/

class FlowLogMessage extends ComputeMessage {

  public FlowLogMessage( ) {
    super( );
  }

  public FlowLogMessage( FlowLogMessage msg ) {
    super( msg );
  }

  public FlowLogMessage( String userId ) {
    super( userId );
  }
}

class CreateFlowLogsType extends FlowLogMessage {
  ArrayList<String> resourceId = Lists.newArrayList( )
  String resourceType
  String trafficType
  String logGroupName
  String deliverLogsPermissionArn
  String clientToken
}

class CreateFlowLogsResponseType extends FlowLogMessage {
}

class DeleteFlowLogsType extends FlowLogMessage {
  ArrayList<String> flowLogId = Lists.newArrayList( )
}

class DeleteFlowLogsResponseType extends FlowLogMessage {
}

class DescribeFlowLogsType extends FlowLogMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = Lists.newArrayList( )
  ArrayList<String> flowLogId = Lists.newArrayList( )
  Integer maxResults
  String nextToken
}

class DescribeFlowLogsResponseType extends FlowLogMessage {
}

