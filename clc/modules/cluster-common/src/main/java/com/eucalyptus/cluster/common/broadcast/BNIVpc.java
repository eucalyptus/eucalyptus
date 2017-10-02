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
import io.vavr.collection.Array;
import io.vavr.control.Option;

/**
 *
 */
@Immutable
@ImmutableNetworkInfoStyle
public interface BNIVpc extends BNIHasName {

  String ownerId( );

  String cidr( );

  Option<String> dhcpOptionSet( );

  @JacksonXmlElementWrapper( localName = "subnets" )
  @JacksonXmlProperty( localName = "subnet" )
  Array<BNIVpcSubnet> subnets( );

  @JacksonXmlElementWrapper( localName = "networkAcls" )
  @JacksonXmlProperty( localName = "networkAcl" )
  Array<BNINetworkAcl> networkAcls( );

  @JacksonXmlElementWrapper( localName = "routeTables" )
  @JacksonXmlProperty( localName = "routeTable" )
  Array<BNIRouteTable> routeTables( );

  @JacksonXmlElementWrapper( localName = "natGateways" )
  @JacksonXmlProperty( localName = "natGateway" )
  Array<BNINatGateway> natGateways( );

  @JacksonXmlElementWrapper( localName = "internetGateways" )
  @JacksonXmlProperty( localName = "value" )
  Array<String> internetGateways( );
}
