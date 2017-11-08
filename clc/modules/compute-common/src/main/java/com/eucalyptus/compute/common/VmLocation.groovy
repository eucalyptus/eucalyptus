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

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.google.common.base.Function
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
  public ClusterInfoType(String zoneName, String zoneState, String regionName=''){this.zoneName = zoneName; this.zoneState = zoneState; this.regionName=regionName;}
  String zoneName;
  String zoneState;
  String regionName;
  ArrayList<String> messageSet = new ArrayList<String>();

  static Function<ClusterInfoType,String> zoneName( ) {
    return { ClusterInfoType clusterInfoType -> clusterInfoType.zoneName } as Function<ClusterInfoType,String>
  }
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
