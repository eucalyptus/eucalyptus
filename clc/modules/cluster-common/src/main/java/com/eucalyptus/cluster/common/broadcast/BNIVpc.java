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
