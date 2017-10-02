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
