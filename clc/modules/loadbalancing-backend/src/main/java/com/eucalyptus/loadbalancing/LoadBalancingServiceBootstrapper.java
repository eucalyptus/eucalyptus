/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.loadbalancing;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.NatGatewayType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RouteType;
import com.eucalyptus.compute.common.RouteTableAssociationType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CloudMetadatas;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;

import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.util.*;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

@Provides(LoadBalancingBackend.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(LoadBalancingBackend.class)
@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancingServiceBootstrapper extends Bootstrapper.Simple {
    private static Logger LOG = Logger.getLogger(LoadBalancingServiceBootstrapper.class);

    private static LoadBalancingServiceBootstrapper singleton;
    private static final Callable<String> imageNotConfiguredFaultRunnable =
            Faults.forComponent(LoadBalancingBackend.class).havingId(1014).logOnFirstRun();

    public static Bootstrapper getInstance() {
        synchronized (LoadBalancingServiceBootstrapper.class) {
            if (singleton == null) {
                singleton = new LoadBalancingServiceBootstrapper();
                LOG.info("Creating Load Balancing Bootstrapper instance.");
            } else {
                LOG.info("Returning Load Balancing Bootstrapper instance.");
            }
        }
        return singleton;
    }

    private static int CheckCounter = 0;
    private static boolean EmiCheckResult = true;

    @Override
    public boolean check() throws Exception {
        if (CloudMetadatas.isMachineImageIdentifier(LoadBalancerASGroupCreator.IMAGE)) {
            if (CheckCounter >= 3 && Topology.isEnabled(Compute.class)) {
                try {
                    final List<ImageDetails> emis =
                            EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(LoadBalancerASGroupCreator.IMAGE));
                    EmiCheckResult = LoadBalancerASGroupCreator.IMAGE.equals(emis.get(0).getImageId());
                    EmiCheckResult = "available".equals(emis.get(0).getImageState());
                } catch (final Exception ex) {
                    EmiCheckResult = false;
                }
                CheckCounter = 0;
            } else
                CheckCounter++;
            return EmiCheckResult;
        } else {
            try {
                //GRZE: do this bit in the way that it allows getting the information with out needing to spelunk log files.
                final ServiceConfiguration localService = Components.lookup(LoadBalancingBackend.class).getLocalServiceConfiguration();
                final Faults.CheckException ex = Faults.failure(localService, imageNotConfiguredFaultRunnable.call().split("\n")[1]);
                Faults.submit(localService, localService.lookupStateMachine().getTransitionRecord(), ex);
            } catch (Exception e) {
                LOG.debug(e);
            }
            return false;
        }
    }

    @Override
    public boolean enable() throws Exception {
        if (!super.enable())
            return false;
        try {
            LoadBalancerPolicies.initialize();
        } catch (final Exception ex) {
            LOG.error("Unable to initialize ELB policy types", ex);
            return false;
        }
        try{
            if(isCloudVpc()) {
                if (!prepareSystemVpc())
                    return false;
            }
        } catch (final Exception ex) {
            LOG.error("Failed to prepare system VPC for loadbalancing service", ex);
            return false;
        }
        return true;
    }

    private static boolean isCloudVpc() {
        try {
            final AccountFullName elbSystemAccount = AccountFullName.getInstance(Accounts.lookupAccountIdByAlias(
                    AccountIdentifiers.ELB_SYSTEM_ACCOUNT
            ));
            if (EucalyptusActivityTasks.getInstance().defaultVpc(elbSystemAccount).isPresent())
                return true;
            else
                return false;
        } catch (final Exception ex) {
            return false;
        }
    }

    @ConfigurableField(displayName = "number_of_hosts_in_vpc_subnet",
            description = "number of hosts per ELB system VPC subnet",
            initial = "4096",
            readonly = false,
            type = ConfigurableFieldType.KEYVALUE,
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

    private static final Set<String> SystemVpcCidrBlocks = Sets.newHashSet(
            "10.254.0.0/16",
            "192.168.0.0/16"
    );

    // public subnets: "10.254.0.0/16" -> ["10.254.0.0/28", "10.254.0.16/28", ..., "10.254.1.240/28"]
    //                 256 public subnets = Max # of AZs
    //
    private static Map<String, Set<String>> splitPublicSubnetBlocks() {
        final Map<String, Set<String>> subnetCidrBlocks = Maps.newHashMap();
        final int SUBNET_SIZE_MASK = 28;
        final int NUM_SUBNET_HOSTS = 16;

        for (final String vpcCidrBlock : SystemVpcCidrBlocks) {
            final String[] prefixAndMask = vpcCidrBlock.split("/");
            final String ipPrefix = prefixAndMask[0];
            final String[] ipParts = ipPrefix.split("\\.");
            if (ipParts.length != 4)
                throw Exceptions.toUndeclared("Invalid cidr format found: " + vpcCidrBlock);
            subnetCidrBlocks.put(vpcCidrBlock, Sets.<String>newHashSet());

            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 255; j += NUM_SUBNET_HOSTS) {
                    final String subnetBlock = String.format("%s.%s.%d.%d/%d",
                            ipParts[0], ipParts[1], i, j, SUBNET_SIZE_MASK);
                    subnetCidrBlocks.get(vpcCidrBlock).add(subnetBlock);
                }
            }
        }
        return subnetCidrBlocks;
    }

    // private subnets: "10.254.0.0/16" -> ["10.254.16.0/20", "10.254.32.0/20", ..., ]
    private static Map<String, Set<String>> splitPrivateSubnetBlocks() {
        final Map<String, Set<String>> subnetCidrBlocks = Maps.newHashMap();
        final int numSubnetHosts = NumberOfHostsPerSystemSubnet(); /// NOTE: MAX # of ELB VMs IN AZ
        final int SUBNET_SIZE_MASK = 32 - (int) log2(numSubnetHosts);

        for (final String vpcCidrBlock : SystemVpcCidrBlocks) {
            final String[] prefixAndMask = vpcCidrBlock.split("/");
            final String ipPrefix = prefixAndMask[0];
            final String[] ipParts = ipPrefix.split("\\.");
            if (ipParts.length != 4)
                throw Exceptions.toUndeclared("Invalid cidr format found: " + vpcCidrBlock);
            subnetCidrBlocks.put(vpcCidrBlock, Sets.<String>newHashSet());

            // numSubnetHosts = multiplication of 256
            for (int i = 0; i < 65536; i += numSubnetHosts) {
                int idx256Subnets = (int) (i / 256.0);
                if (idx256Subnets <= 1) // this block is taken for public subnet
                    continue;
                final String subnetBlock = String.format("%s.%s.%d.%d/%d",
                        ipParts[0], ipParts[1], idx256Subnets, 0, SUBNET_SIZE_MASK);
                subnetCidrBlocks.get(vpcCidrBlock).add(subnetBlock);
            }
        }
        return subnetCidrBlocks;
    }

    final static Map<String, Set<String>> SystemVpcPublicSubnetBlocks =
            splitPublicSubnetBlocks();
    final static Map<String, Set<String>> SystemVpcPrivateSubnetBlocks =
            splitPrivateSubnetBlocks();

    private static boolean prepareSystemVpc() {
        if (! Topology.isEnabled(Compute.class) )
            return false;

        try {
            // 1. Look for the existing VPCs or create new VPCs
            final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
            final List<String> availabilityZones = Lists.newArrayList(Collections2.transform(client.describeAvailabilityZones(),
                    new Function<ClusterInfoType, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable ClusterInfoType clusterInfoType) {
                            return clusterInfoType.getZoneName();
                        }
                    }));

            if (availabilityZones.size() > (int) (65536 / (double) NumberOfHostsPerSystemSubnet()) - 1) {
                throw Exceptions.toUndeclared("Number of possible subnets is less than availability zones. Property HOSTS_PER_SYSTEM_SUBNET should be reduced");
            }
            final List<VpcType> vpcs = client.describeSystemVpcs(false, null);
            final Map<String, VpcType> cidrToVpc = Maps.newHashMap();
            final Map<String, String> vpcToCidr = Maps.newHashMap();
            for (final VpcType vpc : vpcs) {
                if (SystemVpcCidrBlocks.contains(vpc.getCidrBlock()))
                    cidrToVpc.put(vpc.getCidrBlock(), vpc);
            }
            for (final String cidrBlock : SystemVpcCidrBlocks) {
                if (!cidrToVpc.containsKey(cidrBlock)) {
                    final String vpcId = client.createSystemVpc(cidrBlock);
                    final List<VpcType> result = client.describeSystemVpcs(false, Lists.newArrayList(vpcId));
                    final VpcType vpc = result.get(0);
                    cidrToVpc.put(cidrBlock, vpc);
                }
            }
            if (SystemVpcCidrBlocks.size() != cidrToVpc.size()) {
                throw new Exception("Could not find some system VPCs");
            }

            final Collection<String> systemVpcIds = Collections2.transform(cidrToVpc.values(),
                    new Function<VpcType, String>() {
                        @Override
                        public String apply(VpcType vpcType) {
                            return vpcType.getVpcId();
                        }
                    });
            for (final String cidr : cidrToVpc.keySet()) {
                final VpcType vpc = cidrToVpc.get(cidr);
                vpcToCidr.put(vpc.getVpcId(), cidr);
            }

            final Map<String, String> azToNatGateway = Maps.newHashMap();
            for (final String vpcId : systemVpcIds) {
                // 2. Internet gateway
                final String internetGatewayId = getInternetGateway(vpcId);

                // 3. public subnet (to place nat gateway)
                final Map<String, String> publicSubnets = getSubnets(vpcId,
                        SystemVpcPublicSubnetBlocks.get(vpcToCidr.get(vpcId)),
                        availabilityZones);

                // 4. create a route table for the public subnet
                for (final String az : publicSubnets.keySet()) {
                    final String subnetId = publicSubnets.get(az);
                    final String routeTableId = getRouteTable(vpcId, subnetId);
                    addRouteToGateway(routeTableId, internetGatewayId, null);

                    // 5. elastic IP to be assigned to Nat gateway
                    final String eipAllocationId = getElasticIp();

                    // 6. Nat gateway placed in the public subnet
                    final String natGatewayId = getNatGateway(subnetId, eipAllocationId);
                    azToNatGateway.put(az, natGatewayId);
                }

                // 7. private subnet
                final Map<String, String> privateSubnets = getSubnets(vpcId,
                        SystemVpcPrivateSubnetBlocks.get(vpcToCidr.get(vpcId)),
                        availabilityZones);

                // 8. route table with route to NAT gateway
                for (final String az : privateSubnets.keySet()) {
                    final String subnetId = privateSubnets.get(az);
                    final String routeTableId = getRouteTable(vpcId, subnetId);
                    if (!azToNatGateway.containsKey(az))
                        throw Exceptions.toUndeclared("No NAT gateway is found for AZ: " + az);
                    addRouteToGateway(routeTableId, null, azToNatGateway.get(az));
                }
            }
            KnownAvailabilityZones.addAll(availabilityZones);
        } catch (final Exception ex) {
            LOG.error("Failed to prepare system VPC for loadbalancing service", ex);
            return false;
        }
        return true;
    }

    private static String getNatGateway(final String subnetId, final String eipAllocationId) {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
        final List<NatGatewayType> gateways = client.describeSystemNatGateway(subnetId);
        for (final NatGatewayType gateway : gateways) {
            if ("available".equals(gateway.getState()))
                return gateway.getNatGatewayId();
            else {
                LOG.warn("Nat gateway for ELB system account is in invalid state: " +
                        gateway.getNatGatewayId() + ":" + gateway.getState());
            }
        }
        return client.createSystemNatGateway(subnetId, eipAllocationId);
    }

    private static String getElasticIp() {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
        List<AddressInfoType> addresses =
                client.describeSystemAddresses(true);
        String allocationId = null;
        for (final AddressInfoType address : addresses) {
            if (address.getAssociationId() != null || address.getInstanceId() != null)
                continue;
            if (address.getPublicIp() != null) {
                allocationId = address.getAllocationId();
                break;
            }
        }
        if (allocationId == null) {
            final String ipAddress = client.allocateSystemVpcAddress();
            addresses = client.describeSystemAddresses(true, ipAddress);
            allocationId = addresses.get(0).getAllocationId();
        }
        return allocationId;
    }

    private static void addRouteToGateway(final String routeTableId,
                                          final String internetGatewayId,
                                          final String natGatewayId) {
        if (internetGatewayId != null && natGatewayId != null)
            throw Exceptions.toUndeclared("Only one type of gateway id can be specified");

        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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

        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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

    private static Set<String> KnownAvailabilityZones = Sets.newHashSet();
    /// When there is a new AZ enabled later, system VPC setup should run again
    public static class AvailabilityZoneChecker implements EventListener<ClockTick> {
        private static int CHECK_INTERVAL_SEC = 120;
        private static Date lastCheckTime = new Date(System.currentTimeMillis());

        public static void register() {
            Listeners.register(ClockTick.class, new AvailabilityZoneChecker());
        }

        @Override
        public void fireEvent(ClockTick event) {
            if (Bootstrap.isFinished() &&
                    Topology.isEnabledLocally(LoadBalancingBackend.class) &&
                    Topology.isEnabled(Compute.class) &&
                    isCloudVpc()) {
                final Date now = new Date(System.currentTimeMillis());
                final int elapsedSec = (int) ((now.getTime() - lastCheckTime.getTime()) / 1000.0);
                if (elapsedSec < CHECK_INTERVAL_SEC)
                    return;
                lastCheckTime = now;


                final List<String> availabilityZones =
                        Lists.newArrayList(Collections2.transform(
                                EucalyptusActivityTasks.getInstance().describeAvailabilityZones(),
                                new Function<ClusterInfoType, String>() {
                                    @Nullable
                                    @Override
                                    public String apply(@Nullable ClusterInfoType clusterInfoType) {
                                        return clusterInfoType.getZoneName();
                                    }
                                }));

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