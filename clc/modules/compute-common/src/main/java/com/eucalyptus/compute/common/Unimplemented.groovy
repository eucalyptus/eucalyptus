/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

import java.util.ArrayList;
import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.policy.PolicySpec
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
  //:: reservedInstancesOfferingsSet/item/reservedInstancesOfferingId :://
  ArrayList<String> instanceIds = new ArrayList<String>();
  String instanceType;
  String availabilityZone;
  String productDescription;
}
public class DescribeReservedInstancesOfferingsResponseType extends ReservedInstanceMessage {
  ArrayList<String> reservedInstancesOfferingsSet = new ArrayList<String>();
}
public class DescribeReservedInstancesType extends ReservedInstanceMessage {
  //:: reservedInstancesSet/item/reservedInstancesId :://
  ArrayList<String> instanceIds = new ArrayList<String>();
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
  ArrayList<String> reservedInstancesModificationIds = new ArrayList<String>()
  String nextToken
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeReservedInstancesModificationsResponseType extends ReservedInstanceMessage {}

/** *******************************************************************************/
public class SpotInstanceMessage extends ComputeMessage {}
public class DescribeSpotPriceHistoryType extends SpotInstanceMessage {}
public class DescribeSpotPriceHistoryResponseType extends SpotInstanceMessage {}
public class DescribeSpotInstanceRequestsType extends SpotInstanceMessage {}
public class DescribeSpotInstanceRequestsResponseType extends SpotInstanceMessage {}
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
  ArrayList<String> placementGroupSet = new ArrayList<String>();
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  public DescribePlacementGroupsType() {
  }
}
public class DescribePlacementGroupsResponseType extends VmPlacementMessage {
  ArrayList<PlacementGroupInfo> placementGroupSet = new ArrayList<PlacementGroupInfo>();
  public DescribePlacementGroupsResponseType() {
  }
}
