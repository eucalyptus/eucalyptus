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
package com.eucalyptus.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.vavr.encodings.VavrEncodingEnabled;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.network.NetworkView.ImmutableNetworkViewStyle;
import io.vavr.collection.Array;
import io.vavr.control.Option;

/**
 *
 */
@Enclosing
@ImmutableNetworkViewStyle
public interface NetworkView {

  interface VersionedNetworkView<V extends VersionedNetworkView<V>> extends Comparable<V> {
    String getId( );
    int getVersion( );

    default int compareTo( @Nonnull final V other ) {
      return this.getId( ).compareTo( other.getId( ) );
    }
  }

  @Immutable
  interface VmInstanceNetworkView extends VersionedNetworkView<VmInstanceNetworkView> {
    VmInstance.VmState state();
    Boolean omit();
    String ownerAccountNumber();
    Option<String> vpcId();
    Option<String> subnetId();
    Option<String> macAddress();
    Option<String> privateAddress();
    Option<String> publicAddress();
    String partition();
    Option<String> node();
    Array<String> securityGroupIds();
  }

  @Immutable
  interface IPPermissionNetworkView {
    Integer protocol();
    Option<Integer> fromPort();
    Option<Integer> toPort();
    Option<Integer> icmpType();
    Option<Integer> icmpCode();
    Option<String> groupId();
    Option<String> groupOwnerAccountNumber();
    Option<String> cidr();
  }

  @Immutable
  interface NetworkGroupNetworkView extends VersionedNetworkView<NetworkGroupNetworkView> {
    String ownerAccountNumber();
    Option<String> vpcId();
    Array<IPPermissionNetworkView> ingressPermissions();
    Array<IPPermissionNetworkView> egressPermissions();
  }

  @Immutable
  interface VpcNetworkView extends VersionedNetworkView<VpcNetworkView> {
    String ownerAccountNumber();
    String cidr();
    Option<String> dhcpOptionSetId();
  }

  @Immutable
  interface SubnetNetworkView extends VersionedNetworkView<SubnetNetworkView> {
    String ownerAccountNumber();
    String vpcId();
    String cidr();
    String availabilityZone();
    Option<String> networkAcl();
  }

  @Immutable
  interface DhcpOptionSetNetworkView extends VersionedNetworkView<DhcpOptionSetNetworkView> {
    String ownerAccountNumber();
    Array<DhcpOptionNetworkView> options();
  }

  @Immutable
  interface DhcpOptionNetworkView {
    String key();
    Array<String> values();
  }

  @Immutable
  interface NetworkAclNetworkView extends VersionedNetworkView<NetworkAclNetworkView> {
    String ownerAccountNumber();
    String vpcId();
    Array<NetworkAclEntryNetworkView> ingressRules();
    Array<NetworkAclEntryNetworkView> egressRules();
  }

  @Immutable
  interface NetworkAclEntryNetworkView {
    Integer number();
    Integer protocol();
    String action();
    String cidr();
    Option<Integer> icmpCode();
    Option<Integer> icmpType();
    Option<Integer> portRangeFrom();
    Option<Integer> portRangeTo();
  }

  @Immutable
  interface RouteTableNetworkView extends VersionedNetworkView<RouteTableNetworkView> {
    String ownerAccountNumber();
    String vpcId();
    boolean main();
    Array<String> subnetIds(); // associated subnets
    Array<RouteNetworkView> routes();
  }

  @Immutable
  interface RouteNetworkView {
    boolean active();
    String routeTableId();
    String destinationCidr();
    Option<String> gatewayId();
    Option<String> natGatewayId();
    Option<String> networkInterfaceId();
    Option<String> instanceId();
  }

  @Immutable
  interface InternetGatewayNetworkView extends VersionedNetworkView<InternetGatewayNetworkView> {
    String ownerAccountNumber();
    Option<String> vpcId();
  }

  @Immutable
  interface NetworkInterfaceNetworkView extends VersionedNetworkView<NetworkInterfaceNetworkView> {
    NetworkInterface.State state();
    NetworkInterfaceAttachment.Status attachmentStatus();
    String ownerAccountNumber();
    Option<String> instanceId();
    Option<String> attachmentId();
    Option<Integer> deviceIndex();
    Option<String> macAddress();
    Option<String> privateIp();
    Option<String> publicIp();
    Boolean sourceDestCheck();
    String vpcId();
    String subnetId();
    Array<String> securityGroupIds();
  }

  @Immutable
  interface NatGatewayNetworkView extends VersionedNetworkView<NatGatewayNetworkView> {
    NatGateway.State state();
    String ownerAccountNumber();
    Option<String> macAddress();
    Option<String> privateIp();
    Option<String> publicIp();
    String vpcId();
    String subnetId();
  }

  @Target( ElementType.TYPE )
  @Retention( RetentionPolicy.CLASS )
  @Style( add = "", build = "o", depluralize = true, defaults = @Immutable() )
  @VavrEncodingEnabled
  @interface ImmutableNetworkViewStyle { }
}
