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

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class CloudTopologyMessage extends ComputeMessage{

  public CloudTopologyMessage( ) {
    super( );
  }

  public CloudTopologyMessage( ComputeMessage msg ) {
    super( msg );
  }

  public CloudTopologyMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class MigrateInstancesType extends CloudTopologyMessage {
  String sourceHost;
  String instanceId;
  @HttpParameterMapping (parameter = "DestinationHost")
  ArrayList<String> destinationHosts = new ArrayList<String>( );
  Boolean allowHosts = false;
}
public class MigrateInstancesResponseType extends CloudTopologyMessage {}
/** *******************************************************************************/
public class DescribeAvailabilityZonesType extends CloudTopologyMessage {
  //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "ZoneName")
  ArrayList<String> availabilityZoneSet = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeAvailabilityZonesResponseType extends CloudTopologyMessage {
  //** added 2008-02-01  **/
  ArrayList<ClusterInfoType> availabilityZoneInfo = new ArrayList<ClusterInfoType>();
}
/** *******************************************************************************/
public class DescribeRegionsType extends CloudTopologyMessage {
  //** added 2008-12-01  **/
  @HttpParameterMapping (parameter = "RegionName")
  ArrayList<String> regions = new ArrayList<String>();
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}
public class DescribeRegionsResponseType extends CloudTopologyMessage {
  //** added 2008-12-01  **/
  ArrayList<RegionInfoType> regionInfo = new ArrayList<RegionInfoType>();
}
/** *******************************************************************************/
public class ClusterInfoType extends EucalyptusData {
  //** added 2008-02-01  **/
  public ClusterInfoType(){}
  public ClusterInfoType(String zoneName, String zoneState){this.zoneName = zoneName; this.zoneState = zoneState; this.regionName="";}
  String zoneName;
  String zoneState;
  String regionName;
  ArrayList<String> messageSet = new ArrayList<String>();
}
public class RegionInfoType extends EucalyptusData {  //** added 2008-12-01  **/
  public RegionInfoType(){}
  public RegionInfoType(final String regionName, final String regionEndpoint) {
    this.regionName = regionName;
    this.regionEndpoint = regionEndpoint;
  }
  String regionName;
  String regionEndpoint;
}
