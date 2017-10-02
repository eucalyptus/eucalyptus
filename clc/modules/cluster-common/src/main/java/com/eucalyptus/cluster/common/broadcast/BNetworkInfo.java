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
package com.eucalyptus.cluster.common.broadcast;

import org.immutables.value.Value.Immutable;
import com.eucalyptus.cluster.common.broadcast.impl.ImmutableNetworkInfoStyle;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.vavr.collection.Array;
import io.vavr.control.Option;

/**
 *
 */
@Immutable
@ImmutableNetworkInfoStyle
@JacksonXmlRootElement( localName = "network-data" )
public interface BNetworkInfo {

  @JacksonXmlProperty( isAttribute = true )
  Option<String> version( );

  @JacksonXmlProperty( isAttribute = true, localName = "applied-version" )
  Option<String> appliedVersion( );

  @JacksonXmlProperty( isAttribute = true, localName = "applied-time" )
  Option<String> appliedTime( );

  BNIConfiguration configuration( );

  @JacksonXmlElementWrapper( localName = "vpcs" )
  @JacksonXmlProperty( localName = "vpc" )
  Array<BNIVpc> vpcs( );

  @JacksonXmlElementWrapper( localName = "instances" )
  @JacksonXmlProperty( localName = "instance" )
  Array<BNIInstance> instances( );

  @JacksonXmlElementWrapper( localName = "dhcpOptionSets" )
  @JacksonXmlProperty( localName = "dhcpOptionSet" )
  Array<BNIDhcpOptionSet> dhcpOptionSets( );

  @JacksonXmlElementWrapper( localName = "internetGateways" )
  @JacksonXmlProperty( localName = "internetGateway" )
  Array<BNIInternetGateway> internetGateways( );

  @JacksonXmlElementWrapper( localName = "securityGroups" )
  @JacksonXmlProperty( localName = "securityGroup" )
  Array<BNISecurityGroup> securityGroups( );
}
