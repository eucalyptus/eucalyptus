/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
@GroovyAddClassUUID
package com.eucalyptus.compute.common.network

import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
class NetworkingMessage extends EucalyptusMessage {
  public <TYPE extends BaseMessage> TYPE reply( final TYPE response ) {
    super.reply( response )
  }
}

abstract class NetworkResource extends EucalyptusData {
  abstract String getType( )
  String ownerId
  String value
  ArrayList<NetworkResource> resources = Lists.newArrayList( )
}

@Canonical class SecurityGroupResource extends NetworkResource {
  @Override String getType( ){ "security-group" }

  static SecurityGroupResource forId( String id ) {
    new SecurityGroupResource( value: id )
  }
}

@Canonical class PrivateIPResource extends NetworkResource {
  @Override String getType( ){ "private-ip" }
  String mac
}

@Canonical class PublicIPResource extends NetworkResource {
  @Override String getType( ){ "public-ip" }
}

@Canonical class VpcNetworkInterfaceResource extends NetworkResource {
  @Override String getType( ){ "network-interface" }
  Integer device
  String mac
  String vpc
  String subnet
  String privateIp
  String description
  Boolean deleteOnTerminate
  ArrayList<String> networkGroupIds
  String attachmentId
}

abstract class NetworkFeature extends EucalyptusData {}

class DnsHostNamesFeature extends NetworkFeature { }

class PrepareNetworkResourcesType extends NetworkingMessage {
  String availabilityZone
  String vpc
  String subnet
  ArrayList<NetworkResource> resources = Lists.newArrayList( )
  ArrayList<NetworkFeature> features = Lists.newArrayList( )
}

class PrepareNetworkResourcesResultType extends EucalyptusData {
  ArrayList<NetworkResource> resources = Lists.newArrayList( )
}

class PrepareNetworkResourcesResponseType extends NetworkingMessage {
  PrepareNetworkResourcesResultType prepareNetworkResourcesResultType = new PrepareNetworkResourcesResultType( )
}

class ReleaseNetworkResourcesType extends NetworkingMessage {
  String vpc
  ArrayList<NetworkResource> resources = Lists.newArrayList( )
}

class ReleaseNetworkResourcesResponseType extends NetworkingMessage {}

class DescribeNetworkingFeaturesType extends NetworkingMessage {}

class DescribeNetworkingFeaturesResult extends EucalyptusData {
  ArrayList<NetworkingFeature> networkingFeatures = Lists.newArrayList( )
}

class DescribeNetworkingFeaturesResponseType extends NetworkingMessage {
  DescribeNetworkingFeaturesResult describeNetworkingFeaturesResult = new DescribeNetworkingFeaturesResult( )
}

class UpdateInstanceResourcesType extends NetworkingMessage {
  String partition
  InstanceResourceReportType resources
}

class UpdateInstanceResourcesResponseType extends NetworkingMessage {
  Boolean updated
}

class InstanceResourceReportType extends EucalyptusData {
  ArrayList<String> publicIps = Lists.newArrayList( )
  ArrayList<String> privateIps = Lists.newArrayList( )
  ArrayList<String> macs = Lists.newArrayList( )
}
