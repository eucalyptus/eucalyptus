/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.NatGatewayAddressSetItemType;
import com.eucalyptus.compute.common.NatGatewayType;
import com.eucalyptus.compute.common.RouteTableAssociationType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RouteType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.service.activities.RdsActivityTasks;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;

/**
 *
 */
@ConfigurableClass(root = "services.rds", description = "Parameters controlling rds")
public class RdsSystemVpcs {
  private static final Logger LOG = Logger.getLogger(RdsSystemVpcs.class);

  private static final Supplier<Map<String, Set<String>>> systemVpcPublicSubnetBlocksSupplier =
      CompatSupplier.of(Suppliers.memoize(RdsSystemVpcs::splitPublicSubnetBlocks));

  private static Map<String, Set<String>> SystemVpcPublicSubnetBlocks() {
    return systemVpcPublicSubnetBlocksSupplier.get();
  }

  static Supplier<Map<String, Set<String>>> systemVpcPrivateSubnetBlocksSupplier =
      CompatSupplier.of(Suppliers.memoize(RdsSystemVpcs::splitPrivateSubnetBlocks));

  static Map<String, Set<String>> SystemVpcPrivateSubnetBlocks() {
    return systemVpcPrivateSubnetBlocksSupplier.get();
  }

  private static Supplier<Boolean> cloudVpcTest = () -> {
    try {
      final AccountFullName serviceAccount = AccountFullName.getInstance(Accounts.lookupAccountIdByAlias(
          RdsSystemAccountProvider.RDS_SYSTEM_ACCOUNT
      ));
      final Boolean result;
      if (RdsActivityTasks.getInstance().defaultVpc(serviceAccount).isPresent())
        result = true;
      else
        result = false;
      cloudVpcTest = () -> result;
      return result;
    } catch (final Exception ex) {
      return false;
    }
  };

  public static Optional<Boolean> isCloudVpc() {
    if (Topology.isEnabled(Compute.class)) {
      return Optional.of(cloudVpcTest.get());
    } else {
      return Optional.empty();
    }
  }

  @ConfigurableField(displayName = "number_of_hosts_in_vpc_subnet",
      description = "number of hosts per rds system vpc subnet",
      initial = "4096",
      changeListener = NumHostsChangeListener.class
  )
  public static String HOSTS_PER_SYSTEM_SUBNET = "4096";

  public static int NumberOfHostsPerSystemSubnet() {
    return Integer.parseInt(HOSTS_PER_SYSTEM_SUBNET);
  }

  static double log2(double x) {
    return Math.log(x) / Math.log(2.0d);
  }

  public static class NumHostsChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
      try {
        Integer numHosts = Integer.parseInt((String) newValue);
        if (numHosts > 32768)
          throw new Exception("Number of subnet hosts cannot exceed 32768");
        if (numHosts < 256)
          throw new Exception("Number of subnet hosts should be larger than 256");
        if (numHosts % 256 != 0)
          throw new Exception("Number of subnet hosts should be multiplication of 256");
      } catch (final Exception ex) {
        LOG.error("Failed to verify number of hosts for system VPC subnet", ex);
        throw new ConfigurablePropertyException(ex);
      }
    }
  }

  @ConfigurableField( description = "Comma separated list of CIDRs for use with rds vpcs",
      displayName = "vpc_cidrs",
      initial = "10.254.0.0/16, 192.168.0.0/16",
      changeListener = SystemVpcCidrListChangeListener.class )
  public static volatile String vpc_cidrs = "10.254.0.0/16, 192.168.0.0/16";

  private static List<String> systemVpcCidrBlocks() {
    return Lists.newArrayList( LIST_SPLITTER.split( vpc_cidrs ) );
  }

  private static List<Cidr> systemVpcCidrs( ) {
    //noinspection StaticPseudoFunctionalStyleMethod
    return Lists.newArrayList( com.google.common.base.Optional.presentInstances(
        Iterables.transform( systemVpcCidrBlocks(), Cidr.parse( ) ) ) );
  }

  private static final Splitter LIST_SPLITTER =
      Splitter.on( CharMatcher.anyOf(", ;:") ).trimResults( ).omitEmptyStrings( );

  public static class SystemVpcCidrListChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty t, final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        for ( final String cidrStr : LIST_SPLITTER.split( newValue ) ) {
          final Cidr cidr = Cidr.parse( ).apply( cidrStr ).orNull( );
          if ( cidr == null ) {
            throw new ConfigurablePropertyException( "Invalid cidr: " + cidr );
          }
          if ( 16 != cidr.getPrefix( ) ) {
            throw new ConfigurablePropertyException( "Invalid cidr range (/16): " + newValue );
          }
        }
      }
    }
  }

  // public subnets: "10.254.0.0/16" -> ["10.254.0.0/28", "10.254.0.16/28", ..., "10.254.1.240/28"]
  //                 256 public subnets = Max # of AZs
  //
  private static Map<String, Set<String>> splitPublicSubnetBlocks() {
    return buildVpcSubnetMap( 256 );
  }

  // private subnets: "10.254.0.0/16" -> ["10.254.16.0/20", "10.254.32.0/20", ..., ]
  private static Map<String, Set<String>> splitPrivateSubnetBlocks() {
    final int numSubnetHosts = NumberOfHostsPerSystemSubnet();
    return buildVpcSubnetMap(65536 / numSubnetHosts );
  }

  private static Map<String, Set<String>> buildVpcSubnetMap( final int subnetParts ) {
    final Map<String, Set<String>> subnetCidrBlocks = Maps.newHashMap();
    for (final Cidr vpcCidr : systemVpcCidrs() ) {
      final String vpcCidrBlock = vpcCidr.toString( );
      subnetCidrBlocks.put(vpcCidrBlock, Sets.newHashSet());

      for (final Cidr subnetCidr : vpcCidr.split( subnetParts )) {
        subnetCidrBlocks.get(vpcCidrBlock).add(subnetCidr.toString());
      }
    }
    return subnetCidrBlocks;
  }

  private static Set<String> systemVpcs() {
    return systemVpcs.get();
  }

  private static Supplier<Set<String>> systemVpcs = () -> {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final List<VpcType> vpcs = client.describeSystemVpcs(null);
    final Set<String> result = vpcs.stream()
        .filter(vpc -> systemVpcCidrBlocks().contains(vpc.getCidrBlock()))
        .map(vpc -> vpc.getVpcId())
        .collect(Collectors.toSet());
    systemVpcs = () -> result;
    return result;
  };

  private static Predicate<RunningInstancesItemType> instanceAttachedToUserVpc = (instance) -> {
    final Optional<InstanceNetworkInterfaceSetItemType> userIf =
        instance.getNetworkInterfaceSet().getItem().stream()
            .filter(netif -> !systemVpcs().contains(netif.getVpcId()))
            .findAny();
    return userIf.isPresent();
  };

  private static Function<RunningInstancesItemType, Set<String>> systemVpcInterfaceAddress = (instance) -> {
    final Set<String> addresses = Sets.newHashSet();
    /// find instance's data interface address
    instance.getNetworkInterfaceSet().getItem().stream()
        .filter(iff -> ! systemVpcs().contains(iff.getVpcId()))
        // any inteface that's not in system VPC is the users account interface
        .forEach( iff -> {
              if (iff.getPrivateIpAddress() != null)
                addresses.add(iff.getPrivateIpAddress());
              if (iff.getAssociation() != null && iff.getAssociation().getPublicIp() != null)
                addresses.add(iff.getAssociation().getPublicIp());
            }
        );

    /// find nat gateway's interface address
    final Optional<InstanceNetworkInterfaceSetItemType> controlIf =
        instance.getNetworkInterfaceSet().getItem().stream()
            .filter(iff -> systemVpcs().contains(iff.getVpcId()))
            .findAny();
    if(!controlIf.isPresent())
      return addresses; // silently return only data interfaces?
    final InstanceNetworkInterfaceSetItemType netIf = controlIf.get();
    final String subnetId = netIf.getSubnetId();

    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final String natGatewayId;
    // find the route for the subnet
    try {
      final Optional<RouteTableType> routeTable = client.describeSystemRouteTables().stream()
          .filter(rtb -> {
            return rtb.getAssociationSet().getItem().stream()
                .anyMatch(assoc -> subnetId.equals(assoc.getSubnetId()));
          })
          .filter(rtb -> {
            return rtb.getRouteSet().getItem().stream()
                .anyMatch(route -> route.getNatGatewayId() != null);
          })
          .findFirst();

      if (!routeTable.isPresent())
        throw Exceptions.toUndeclared("Failed to lookup route table associated with subnet: " + subnetId);

      natGatewayId = routeTable.get().getRouteSet().getItem().stream()
          .filter(rt -> rt.getNatGatewayId() != null)
          .findFirst()
          .get().getNatGatewayId();
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to lookup NAT gateway ID associated with subnet: " + subnetId, ex);
    }

    // find the NAT gateway connected to the same subnet
    try {
      final NatGatewayType gateway =
          client.describeSystemNatGateway(null).stream()
              .filter( gw -> natGatewayId.equals(gw.getNatGatewayId()))
              .findAny()
              .get();
      final NatGatewayAddressSetItemType natAddress = gateway.getNatGatewayAddressSet().getItem().stream()
          .findFirst()
          .get();
      if(natAddress.getPublicIp()!=null)
        addresses.add(natAddress.getPublicIp());
      if(natAddress.getPrivateIp()!=null)
        addresses.add(natAddress.getPrivateIp());
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to lookup NAT gateway's interface address", ex);
    }

    return addresses;
  };

  private static BiPredicate<String, String> cidrBlockInclusive =
      (systemVpcCidr, userCidr) -> {
        final String[] systemCidrTokens = systemVpcCidr.split("\\.");
        final String[] userCidrTokens = userCidr.split("\\.");
        return systemCidrTokens[0].equals(userCidrTokens[0]) &&
            systemCidrTokens[1].equals(userCidrTokens[1]);
      };

  private static Function<String, String> systemSubnetToSecurityGroupId =
      (subnetId) -> {
        final RdsActivityTasks client = RdsActivityTasks.getInstance();
        final Optional<String> optVpcId = client.describeSubnets(Lists.newArrayList(subnetId)).stream()
            .map( subnet -> subnet.getVpcId())
            .findFirst();
        if(! optVpcId.isPresent())
          throw Exceptions.toUndeclared("No VPC ID for the requested subnet is found: " +subnetId);
        final String vpcId = optVpcId.get();
        final Optional<String> optSgroupId =
            client.describeSystemSecurityGroups(null).stream()
                .filter(g -> vpcId.equals(g.getVpcId()))
                .map(g -> g.getGroupId())
                .findFirst();   // assuming there's only one security group for system VPCs
        if (! optSgroupId.isPresent())
          throw Exceptions.toUndeclared("No security group is found for VPC: " + vpcId);
        return optSgroupId.get();
      };

  private static Function<Option<String>, Tuple3<String,String,String>> userSubnetTosystemVpcPrivateSubnet =
      (userSubnetIdOption) -> {
        final RdsActivityTasks client = RdsActivityTasks.getInstance();

        final String systemCidrBlock;
        final String az;
        if (userSubnetIdOption.isDefined()) {
          final String userSubnetId = userSubnetIdOption.get();

          // describe user subnet
          final Optional<SubnetType> optUserSubnet =
              client.describeSubnets(Lists.newArrayList(userSubnetId)).stream()
                  .findFirst();
          if (!optUserSubnet.isPresent())
            throw Exceptions.toUndeclared("No such user subnet is found: " + userSubnetId);
          final SubnetType userSubnet = optUserSubnet.get();
          final String userVpcId = userSubnet.getVpcId();

          final Optional<VpcType> userVpc =
              client.describeSystemVpcs(Lists.newArrayList(userVpcId)).stream()
                  .findAny();
          if (!userVpc.isPresent())
            throw Exceptions.toUndeclared("No user VPC is found: " + userVpcId);

          final String userVpcCidrBlock = userVpc.get().getCidrBlock();
          // find system VPC with no-overlapping cidr block
          systemCidrBlock = systemVpcCidrBlocks().stream().filter((systemCidr ->
              !cidrBlockInclusive.test(systemCidr, userVpcCidrBlock)
          )).findFirst().get();
          az = userSubnet.getAvailabilityZone();
        } else {
          systemCidrBlock = systemVpcCidrBlocks().stream().findFirst().get();
          az = Stream.ofAll(client.describeAvailabilityZones())
              .map(ClusterInfoType::getZoneName).head();
        }

        final Optional<String> vpcId =
            client.describeSystemVpcs(null).stream()
                .filter(vpc -> systemCidrBlock.equals(vpc.getCidrBlock()))
                .map(vpc -> vpc.getVpcId())
                .findAny();
        if(! vpcId.isPresent())
          throw Exceptions.toUndeclared("No system VPC with cidr block " + systemCidrBlock +" is found");

        final Map<String, String> privateSubnets = getSubnets(vpcId.get(),
            SystemVpcPrivateSubnetBlocks().get(systemCidrBlock),
            Lists.newArrayList(az));
        if(! privateSubnets.containsKey(az))
          throw Exceptions.toUndeclared("Failed to lookup system VPC's private subnet");

        return Tuple.of(vpcId.get(), privateSubnets.get(az), az);
      };

  public static Tuple3<String,String,String> getSystemVpcd() {
    return userSubnetTosystemVpcPrivateSubnet.apply(Option.none());
  }

  /**
   * Map the users vpc subnet identifier to the corresponding system vpc information
   *
   * @returns tuple of vpc identifer, subnet identifier and availability zone
   */
  public static Tuple3<String,String,String> getSystemVpcSubnetId(final String userSubnetId) {
    return userSubnetTosystemVpcPrivateSubnet.apply(Option.some(userSubnetId));
  }

  // given the system vpc's subnet ID, return the security group ID for the VPC.
  public static String getSecurityGroupId(final String systemSubnetId) {
    return systemSubnetToSecurityGroupId.apply(systemSubnetId);
  }

  public static Optional<InstanceNetworkInterfaceSetItemType> getUserVpcInterface(final String instanceId) {
    if (!isCloudVpc().isPresent() || !isCloudVpc().get())
      return Optional.empty();

    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final Optional<RunningInstancesItemType> vmInstanceOpt =
        client.describeSystemInstances(Lists.newArrayList(instanceId)).stream()
            .filter(vm -> "running".equals(vm.getStateName()))
            .findFirst();
    if (!vmInstanceOpt.isPresent())
      throw Exceptions.toUndeclared("No running instance is found with ID " + instanceId);
    final RunningInstancesItemType vmInstance = vmInstanceOpt.get();
    final Optional<InstanceNetworkInterfaceSetItemType> userVpcEni =
        vmInstance.getNetworkInterfaceSet().getItem().stream()
            .filter(netif -> !systemVpcs().contains(netif.getVpcId()))
            .findAny();
    return userVpcEni;
  }

  static synchronized boolean prepareSystemVpc() {
    if (! Topology.isEnabled(Compute.class) )
      return false;

    try {
      // 1. Look for the existing VPCs or create new VPCs
      final RdsActivityTasks client = RdsActivityTasks.getInstance();
      final List<String> availabilityZones =
          Lists.newArrayList(client.describeAvailabilityZones().stream()
              .map(az -> az.getZoneName())
              .collect(Collectors.toList())
          );
      if (availabilityZones.size() > (int) (65536 / (double) NumberOfHostsPerSystemSubnet()) - 1) {
        throw Exceptions.toUndeclared("Number of possible subnets is less than availability zones. Property HOSTS_PER_SYSTEM_SUBNET should be reduced");
      }
      final List<VpcType> vpcs = client.describeSystemVpcs(null);
      final Map<String, VpcType> cidrToVpc = Maps.newHashMap();
      final Map<String, String> vpcToCidr = Maps.newHashMap();
      for (final VpcType vpc : vpcs) {
        if (systemVpcCidrBlocks().contains(vpc.getCidrBlock()))
          cidrToVpc.put(vpc.getCidrBlock(), vpc);
      }
      for (final String cidrBlock : systemVpcCidrBlocks()) {
        if (!cidrToVpc.containsKey(cidrBlock)) {
          final String vpcId = client.createSystemVpc(cidrBlock);
          final List<VpcType> result = client.describeSystemVpcs(Lists.newArrayList(vpcId));
          final VpcType vpc = result.get(0);
          cidrToVpc.put(cidrBlock, vpc);
        }
      }
      if (systemVpcCidrBlocks().size() != cidrToVpc.size()) {
        throw new Exception("Could not find some system VPCs");
      }

      final List<String> systemVpcIds = cidrToVpc.values().stream()
          .map(vpc -> vpc.getVpcId())
          .collect(Collectors.toList());

      for (final String cidr : cidrToVpc.keySet()) {
        final VpcType vpc = cidrToVpc.get(cidr);
        vpcToCidr.put(vpc.getVpcId(), cidr);
      }

      final Map<String, String> azToNatGateway = Maps.newHashMap();
      final Set<String> eipAllocated = Sets.newHashSet();
      for (final String vpcId : systemVpcIds) {
        // 2. Internet gateway
        final String internetGatewayId = getInternetGateway(vpcId);

        // 3. public subnet (to place nat gateway)
        final Map<String, String> publicSubnets = getSubnets(vpcId,
            SystemVpcPublicSubnetBlocks().get(vpcToCidr.get(vpcId)),
            availabilityZones);
        // 4. create a route table for the public subnet
        for (final String az : publicSubnets.keySet()) {
          final String subnetId = publicSubnets.get(az);
          final String routeTableId = getRouteTable(vpcId, subnetId);
          addRouteToGateway(routeTableId, internetGatewayId, null);

          // 5. elastic IP to be assigned to Nat gateway
          final String eipAllocationId = getElasticIp(eipAllocated);
          eipAllocated.add(eipAllocationId);

          // 6. Nat gateway placed in the public subnet
          final String natGatewayId = getNatGateway(subnetId, eipAllocationId);
          azToNatGateway.put(az, natGatewayId);
        }

        // 7. private subnet
        final Map<String, String> privateSubnets = getSubnets(vpcId,
            SystemVpcPrivateSubnetBlocks().get(vpcToCidr.get(vpcId)),
            availabilityZones);

        // 8. route table with route to NAT gateway
        for (final String az : privateSubnets.keySet()) {
          final String subnetId = privateSubnets.get(az);
          final String routeTableId = getRouteTable(vpcId, subnetId);
          if (!azToNatGateway.containsKey(az))
            throw Exceptions.toUndeclared("No NAT gateway is found for AZ: " + az);
          addRouteToGateway(routeTableId, null, azToNatGateway.get(az));
        }

        // 9. restrict egress ports for default group
        final List<SecurityGroupItemType> groups =
            client.describeSystemSecurityGroupsByVpc(vpcId);
        final Optional<SecurityGroupItemType> defaultGroup =
            groups.stream().filter( g -> "default".equals(g.getGroupName()))
                .findFirst();
        if (defaultGroup.isPresent())
          updateDefaultSecurityGroup(defaultGroup.get());
      }
      KnownAvailabilityZones.addAll(availabilityZones);
    } catch (final Exception ex) {
      LOG.error("Failed to prepare system VPC for rds service", ex);
      return false;
    }
    return true;
  }

  private static String getNatGateway(final String subnetId, final String eipAllocationId) {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final List<NatGatewayType> gateways = client.describeSystemNatGateway(subnetId);
    for (final NatGatewayType gateway : gateways) {
      final String gwState = gateway.getState();
      if ("available".equals(gwState) || "pending".equals(gwState))
        return gateway.getNatGatewayId();
      else {
        LOG.warn("Nat gateway for system account is in invalid state: " +
            gateway.getNatGatewayId() + ":" + gateway.getState());
      }
    }
    return client.createSystemNatGateway(subnetId, eipAllocationId);
  }

  private static String getElasticIp(final Set<String> exclude) {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    List<AddressInfoType> addresses =
        client.describeSystemAddresses(true);
    String allocationId = null;
    for (final AddressInfoType address : addresses) {
      if (exclude.contains(address.getAllocationId()))
        continue;
      if (address.getAssociationId() != null || address.getInstanceId() != null)
        continue;
      if (address.getPublicIp() != null) {
        allocationId = address.getAllocationId();
        break;
      }
    }
    if (allocationId == null) {
      allocationId = client.allocateSystemVpcAddress().getAllocationId();
    }

    return allocationId;
  }

  private static void addRouteToGateway(final String routeTableId,
                                        final String internetGatewayId,
                                        final String natGatewayId) {
    if (internetGatewayId != null && natGatewayId != null)
      throw Exceptions.toUndeclared("Only one type of gateway id can be specified");

    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final List<RouteTableType> routeTables = client.describeSystemRouteTables(routeTableId, null);
    if (routeTables == null || routeTables.size() <= 0)
      throw Exceptions.toUndeclared("No route table is found - " + routeTableId);
    final RouteTableType table = routeTables.get(0);
    RouteType routeFound = null;
    boolean deleteRoute = false;
    for (final RouteType route : table.getRouteSet().getItem()) {
      if ("0.0.0.0/0".equals(route.getDestinationCidrBlock())) {
        if (route.getGatewayId() != null &&
            route.getGatewayId().equals(internetGatewayId)) {
          routeFound = route;
          break;
        } else if (route.getNatGatewayId() != null &&
            route.getNatGatewayId().equals(natGatewayId)) {
          routeFound = route;
          break;
        } else {
          routeFound = route;
          deleteRoute = true;
        }
      }
    }

    if (deleteRoute && routeFound != null) {
      client.deleteSystemRoute(routeTableId, "0.0.0.0/0");
      routeFound = null;
    }

    if (routeFound == null) {
      if (internetGatewayId != null) {
        client.createSystemRouteToInternetGateway(routeTableId, "0.0.0.0/0", internetGatewayId);
      } else if (natGatewayId != null) {
        client.createSystemRouteToNatGateway(routeTableId, "0.0.0.0/0", natGatewayId);
      }
    }
  }

  private static String getRouteTable(final String vpcId, final String subnetId) {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    List<RouteTableType> routeTables = client.describeSystemRouteTables(null, vpcId);
    String routeTableId = null;

    for (final RouteTableType table : routeTables) {
      for (final RouteTableAssociationType association : table.getAssociationSet().getItem()) {
        if (subnetId.equals(association.getSubnetId())) {
          routeTableId = table.getRouteTableId();
          break;
        }
      }
      if (routeTableId != null)
        break;
    }

    if (routeTableId == null) {
      // if VPC has the non-associated route table, use it
      for(final RouteTableType table : routeTables) {
        if(table.getAssociationSet().getItem()==null ||
            table.getAssociationSet().getItem().size()<=0) {
          routeTableId = table.getRouteTableId();
          break;
        }
      }
      if(routeTableId == null)
        routeTableId = client.createSystemRouteTable(vpcId);
      client.associateSystemRouteTable(subnetId, routeTableId);
    }

    routeTables = client.describeSystemRouteTables(routeTableId, vpcId);
    if (routeTables == null || routeTables.size() <= 0)
      throw Exceptions.toUndeclared("No route table is found associated with subnet id: " + subnetId);
    final RouteTableType routeTable = routeTables.get(0);

    return routeTable.getRouteTableId();
  }

  private static Map<String, String> getSubnets(final String vpcId, final Set<String> subnetBlocks,
                                                final List<String> availabilityZones) {
    final Queue<String> availableBlocks =
        new LinkedList<String>(subnetBlocks);

    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final List<SubnetType> subnets =
        client.describeSubnetsByZone(vpcId, null, availabilityZones);
    final Map<String, String> azToSubnet = Maps.newHashMap();
    for (final String az : availabilityZones) {
      azToSubnet.put(az, null);
    }
    for (final SubnetType subnet : subnets) {
      final String cidrBlock = subnet.getCidrBlock();
      if (subnetBlocks.contains(cidrBlock)) {
        azToSubnet.put(subnet.getAvailabilityZone(), subnet.getSubnetId());
        availableBlocks.remove(cidrBlock);
      }
    }

    for (final String az : azToSubnet.keySet()) {
      if (azToSubnet.get(az) == null) {
        final String cidr = availableBlocks.remove();
        final String subnet = client.createSystemSubnet(vpcId, az, cidr);
        azToSubnet.put(az, subnet);
      }
    }
    return azToSubnet;
  }

  private static String getInternetGateway(final String vpcId) {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();
    final List<InternetGatewayType> gateways =
        client.describeInternetGateways(Lists.newArrayList(vpcId));
    if (gateways.size() <= 0) {
      final String gatewayId = client.createSystemInternetGateway();
      client.attachSystemInternetGateway(vpcId, gatewayId);
      return gatewayId;
    } else {
      if (gateways.size() > 1)
        LOG.warn("More than 1 internet gateway is found for vpc: " + vpcId);
      return gateways.get(0).getInternetGatewayId();
    }
  }

  private static void updateDefaultSecurityGroup(final SecurityGroupItemType group) {
    final RdsActivityTasks client = RdsActivityTasks.getInstance();

    final String groupId = group.getGroupId();

    // revoke ingress permission from the same group
    client.revokePermissionFromOtherGroup(groupId, group.getAccountId(), groupId, "-1");

    final List<IpPermissionType> egressRules = group.getIpPermissionsEgress();
    if (egressRules.stream().filter(ip -> "-1".equals(ip.getIpProtocol()))
        .findAny().isPresent()) {
      // revoke all
      client.revokeSystemSecurityGroupEgressRules(groupId);
    }

    final int servicePort = StackConfiguration.PORT;
    @SuppressWarnings("unchecked")
    final Collection<Tuple2<String,Integer>> egressProtocolAndPorts = Sets.newLinkedHashSet(Lists.newArrayList(
        Tuple.of( "tcp", servicePort ), // cloud services
        Tuple.of( "tcp", 8773 ), // cloud services cluster port
        Tuple.of( "tcp", 53 ),  // dns
        Tuple.of( "udp", 53 ),  // dns
        Tuple.of( "udp", 123 ), // ntp
        Tuple.of( "icmp", -1 )
    ));
    for ( Tuple2<String,Integer> protocolAndPort : egressProtocolAndPorts ) {
      if (!egressRules.stream().anyMatch(ip -> protocolAndPort._1().equals(ip.getIpProtocol()) && servicePort == ip.getFromPort())) {
        client.authorizeSystemSecurityGroupEgressRule(
            groupId, protocolAndPort._1(), protocolAndPort._2(), protocolAndPort._2(), "0.0.0.0/0");
      }
    }
  }

  private static Set<String> KnownAvailabilityZones = Sets.newHashSet();
  /// When there is a new AZ enabled later, system VPC setup should run again
  public static class RdsAvailabilityZoneChecker implements EventListener<ClockTick> {
    private static int CHECK_INTERVAL_SEC = 120;
    private static Date lastCheckTime = new Date(System.currentTimeMillis());

    public static void register() {
      Listeners.register(ClockTick.class, new RdsAvailabilityZoneChecker());
    }

    @SuppressWarnings( { "finally", "ContinueOrBreakFromFinallyBlock" } )
    @Override
    public void fireEvent(ClockTick event) {
      if (Bootstrap.isOperational() &&
          Topology.isEnabledLocally(Rds.class) &&
          Topology.isEnabled(Compute.class) &&
          isCloudVpc().isPresent() && isCloudVpc().get()) {
        final Date now = new Date(System.currentTimeMillis());
        final int elapsedSec = (int) ((now.getTime() - lastCheckTime.getTime()) / 1000.0);
        if (elapsedSec < CHECK_INTERVAL_SEC)
          return;
        lastCheckTime = now;

        final List<String> availabilityZones =
            RdsActivityTasks.getInstance().describeAvailabilityZones().stream()
                .map(az -> az.getZoneName())
                .collect(Collectors.toList());

        for(final String az: availabilityZones) {
          if(! KnownAvailabilityZones.contains(az)) {
            try{
              LOG.info("Trying to prepare system VPC for a new AZ: " + az);
              prepareSystemVpc();
            }catch(final Exception ex) {
              LOG.error("System VPC setup failed", ex);
            }finally{
              KnownAvailabilityZones.add(az); // try only once
              break;
            }
          }
        }
      }
    }
  }
}

