/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
