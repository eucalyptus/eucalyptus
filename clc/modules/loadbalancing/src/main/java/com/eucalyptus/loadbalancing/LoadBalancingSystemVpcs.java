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
package com.eucalyptus.loadbalancing;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.*;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancingSystemVpcs {
    private static Logger LOG = Logger.getLogger(LoadBalancingSystemVpcs.class);

    static Supplier<Map<String, Set<String>>> systemVpcPublicSubnetBlocksSupplier =
            () -> {
                final Map<String, Set<String>> val = splitPublicSubnetBlocks();
                systemVpcPublicSubnetBlocksSupplier = () -> val;
                return val;
            };

    static Map<String, Set<String>> SystemVpcPublicSubnetBlocks() {
        return systemVpcPublicSubnetBlocksSupplier.get();
    }

    static Supplier<Map<String, Set<String>>> systemVpcPrivateSubnetBlocksSupplier =
            () -> {
                final Map<String, Set<String>> val = splitPrivateSubnetBlocks();
                systemVpcPrivateSubnetBlocksSupplier = () -> val;
                return val;
            };

    static Map<String, Set<String>> SystemVpcPrivateSubnetBlocks() {
        return systemVpcPrivateSubnetBlocksSupplier.get();
    }

    private static Supplier<Boolean> cloudVpcTest = () -> {
        try {
            final AccountFullName elbSystemAccount = AccountFullName.getInstance(Accounts.lookupAccountIdByAlias(
                    AccountIdentifiers.ELB_SYSTEM_ACCOUNT
            ));
            final Boolean result;
            if (EucalyptusActivityTasks.getInstance().defaultVpc(elbSystemAccount).isPresent())
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

    private static Set<String> systemVpcs() {
        return systemVpcs.get();
    }

    private static Supplier<Set<String>> systemVpcs = () -> {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
        final List<VpcType> vpcs = client.describeSystemVpcs(null);
        final Set<String> result = vpcs.stream()
                .filter(vpc -> SystemVpcCidrBlocks.contains(vpc.getCidrBlock()))
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
                // any inteface that's not in system VPC is ELB's data interface
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

        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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
                final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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

    private static Function<String, String> userSubnetTosystemVpcPrivateSubnet =
            (userSubnetId) -> {
                final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();

                // describe user subnet
                final Optional<SubnetType> optUserSubnet =
                        client.describeSubnets(Lists.newArrayList(userSubnetId)).stream()
                        .findFirst();
                if(!optUserSubnet.isPresent())
                    throw Exceptions.toUndeclared("No such user subnet is found: " + userSubnetId);
                final SubnetType userSubnet = optUserSubnet.get();
                final String az = userSubnet.getAvailabilityZone();
                final String userVpcId = userSubnet.getVpcId();

                final Optional<VpcType> userVpc =
                        client.describeSystemVpcs(Lists.newArrayList(userVpcId)).stream()
                                .findAny();
                if(! userVpc.isPresent())
                    throw Exceptions.toUndeclared("No user VPC is found: "+userVpcId);

                final String userVpcCidrBlock = userVpc.get().getCidrBlock();
                // find system VPC with no-overlapping cidr block
                final String systemCidrBlock = SystemVpcCidrBlocks.stream().filter((systemCidr ->
                        ! cidrBlockInclusive.test(systemCidr, userVpcCidrBlock)
                )).findFirst().get();

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

                return privateSubnets.get(az);
            };

    final static LoadingCache<String, Set<String>> controlInterfaceCache =   CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES) /// control interface address shouldn't change
            .build(new CacheLoader<String, Set<String>>() {
        @Override
        public Set<String> load(final String instanceId) throws Exception {
            final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();

            final Optional<RunningInstancesItemType> vmInstanceOpt =
                    client.describeSystemInstances(Lists.newArrayList(instanceId)).stream()
                            .filter(vm -> "running".equals(vm.getStateName()))
                            .findFirst();
            if(! vmInstanceOpt.isPresent())
                throw Exceptions.toUndeclared("No running instance is found with ID " + instanceId);
            final RunningInstancesItemType vmInstance = vmInstanceOpt.get();
            try{
                return systemVpcInterfaceAddress.apply(vmInstance);
            }catch(final Exception ex) {
                throw Exceptions.toUndeclared("Failed to lookup ELB's system VPC interface address: "
                        + vmInstance.getInstanceId(),  ex);
            }
        }
    });

    // given the user vpc's subnet ID, return the corresponding system vpc's subnet ID
    // to which the control interface will be attached
    public static String getSystemVpcSubnetId(final String userSubnetId) {
        return userSubnetTosystemVpcPrivateSubnet.apply(userSubnetId);
    }

    // given the system vpc's subnet ID, return the security group ID for the VPC.
    public static String getSecurityGroupId(final String systemSubnetId) {
        return systemSubnetToSecurityGroupId.apply(systemSubnetId);
    }

    public static Optional<InstanceNetworkInterfaceSetItemType> getUserVpcInterface(final String instanceId) {
        if (!isCloudVpc().isPresent() || !isCloudVpc().get())
            return Optional.empty();

        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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

    // list[0]: public IP, list[1]
    public static List<Optional<String>> getUserVpcInterfaceIps(final String instanceId) {
        if (!isCloudVpc().isPresent() || !isCloudVpc().get())
            return null;

        final Optional<InstanceNetworkInterfaceSetItemType> userVpcEni =
                getUserVpcInterface(instanceId);
        if(! userVpcEni.isPresent())
            return null;

        final InstanceNetworkInterfaceSetItemType eni = userVpcEni.get();
        final String privateIp = eni.getPrivateIpAddress();
        String publicIp = null;
        if (eni.getAssociation()!=null) {
            publicIp = eni.getAssociation().getPublicIp();
        }

        final Optional<String> optPublicIp = publicIp!=null ? Optional.of(publicIp)  : Optional.empty();
        final Optional<String> optPrivateIp = privateIp!=null ? Optional.of(privateIp) : Optional.empty();
        return ImmutableList.of(optPublicIp, optPrivateIp);
    }

    // if the primary interface is for system VPC, the secondary interface is attached to user VPC
    public static void setupUserVpcInterface(final String instanceId) {
        if(!isCloudVpc().isPresent() || !isCloudVpc().get())
            return;
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
        final Optional<RunningInstancesItemType> vmInstanceOpt =
                client.describeSystemInstances(Lists.newArrayList(instanceId)).stream()
                        .filter(vm -> "running".equals(vm.getStateName()))
                        .findFirst();
        if(! vmInstanceOpt.isPresent())
            throw Exceptions.toUndeclared("No running instance is found with ID " + instanceId);
        final RunningInstancesItemType vmInstance = vmInstanceOpt.get();
        if(! instanceAttachedToUserVpc.test(vmInstance) ) {

            LoadBalancerServoInstance instance = null;
            try {
                instance = LoadBalancers.lookupServoInstance(instanceId);
            }catch(final Exception ex) {
                throw Exceptions.toUndeclared("Faild to lookup loadbalancer VM named: " + instanceId);
            }

            final LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView
                    autoscaleGroupView = instance.getAutoScalingGroup();
            // 1. find out this servo VM's user subnet ID
            final String userSubnetId = autoscaleGroupView.getUserSubnetId();
            if(userSubnetId == null)
                throw Exceptions.toUndeclared("User subnet ID of the loadbalancer instance is null");

            // 2. also find out the ELB's security group ID
            final LoadBalancerAutoScalingGroup autoscaleGroup =
                    LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupEntityTransform.INSTANCE.apply(autoscaleGroupView);
            final LoadBalancer.LoadBalancerCoreView lbView = autoscaleGroup.getLoadBalancer();
            final Set<String> userSecurityGroupIds = lbView.getSecurityGroupIdsToNames().keySet();

            // 3. create ENI from the subnet ID, associated with the security group ID
            //    the creation of ENI out of user subnet is EUCA-only exception
            NetworkInterfaceType attachedENI = null;
            final int MAX_RETRY = 5;
            int i = 1;
            do {
                NetworkInterfaceType availableInteface = null;
                try {
                    availableInteface =
                            client.describeSystemNetworkInterfaces(userSubnetId).stream()
                                    .filter(n -> "available".equals(n.getStatus()))
                                    .findAny()
                                    .orElseGet(() -> client.createNetworkInterface(userSubnetId,
                                            Lists.newArrayList(userSecurityGroupIds)));
                }catch(final Exception ex) {
                    throw Exceptions.toUndeclared("Failed to create network interface to subnet " + userSubnetId, ex);
                }

                // 3. attach ENI to the instance
                try {
                    // in case attach conflicts
                    client.attachNetworkInterface(vmInstance.getInstanceId(),
                            availableInteface.getNetworkInterfaceId(), 1);
                    attachedENI = availableInteface;

                    // re-load the interface address for source ip check
                    controlInterfaceCache.invalidate(instance.getInstanceId());
                }catch(final Exception ex) {
                    LOG.warn("Failed to attach user vpc interface; will retry", ex);
                    attachedENI = null;
                }
            }while (attachedENI == null && i++ < MAX_RETRY);
            if(i>=MAX_RETRY) {
                LOG.error("Failed to attach user VPC interface to ELB instance + " + vmInstance.getInstanceId());
            }else {
                LOG.debug(String.format("ELB user VPC interface %s is attached to %s",
                        attachedENI, vmInstance.getInstanceId()));
            }

            // 4. for non-internal ELB, allocate and associate EIP to the secondary interface
            if( attachedENI!=null) {
                if (lbView.getScheme() != LoadBalancer.Scheme.Internal) {
                    final String allocationId = client.describeSystemAddresses(true).stream()
                            .filter(addr -> addr.getAssociationId() == null
                                    && addr.getInstanceId() == null
                                    && addr.getNetworkInterfaceId() == null)
                            .map(addr -> addr.getAllocationId())
                            .findAny()
                            .orElseGet(() -> client.allocateSystemVpcAddress().getAllocationId());
                    if (allocationId == null)
                        throw Exceptions.toUndeclared("Failed to allocate EIP address to associate with ELB instances");

                    client.associateSystemVpcAddress(allocationId, attachedENI.getNetworkInterfaceId());
                } else {
                    // if previously this ENI has the associated EIP, disassociate it
                    try {
                        if (attachedENI.getAssociation() != null && attachedENI.getAssociation().getPublicIp() != null) {
                            client.disassociateSystemVpcAddress(attachedENI.getAssociation().getPublicIp());
                        }
                    }catch(final Exception ex) {
                        LOG.warn("Failed to disassociate elastic IP from internal ELB's interface", ex);
                    }
                }

                // 5. turn on deleteOnTerminate flag on the attached ENI
                try{
                    attachedENI = client.describeSystemNetworkInterfaces(
                            Lists.newArrayList(attachedENI.getNetworkInterfaceId())
                    ).get(0);
                    client.modifyNetworkInterfaceDeleteOnTerminate(attachedENI.getNetworkInterfaceId(),
                            attachedENI.getAttachment().getAttachmentId(),
                            true);
                }catch(final Exception ex) {
                    LOG.warn("Failed to set deleteOnTerminate flag for attached user VPC ENI", ex);
                }
            }
        }
    }

    static synchronized boolean prepareSystemVpc() {
        if (! Topology.isEnabled(Compute.class) )
            return false;

        try {
            // 1. Look for the existing VPCs or create new VPCs
            final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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
                if (SystemVpcCidrBlocks.contains(vpc.getCidrBlock()))
                    cidrToVpc.put(vpc.getCidrBlock(), vpc);
            }
            for (final String cidrBlock : SystemVpcCidrBlocks) {
                if (!cidrToVpc.containsKey(cidrBlock)) {
                    final String vpcId = client.createSystemVpc(cidrBlock);
                    final List<VpcType> result = client.describeSystemVpcs(Lists.newArrayList(vpcId));
                    final VpcType vpc = result.get(0);
                    cidrToVpc.put(cidrBlock, vpc);
                }
            }
            if (SystemVpcCidrBlocks.size() != cidrToVpc.size()) {
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
            LOG.error("Failed to prepare system VPC for loadbalancing service", ex);
            return false;
        }
        return true;
    }

    private static String getNatGateway(final String subnetId, final String eipAllocationId) {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
        final List<NatGatewayType> gateways = client.describeSystemNatGateway(subnetId);
        for (final NatGatewayType gateway : gateways) {
            final String gwState = gateway.getState();
            if ("available".equals(gwState) || "pending".equals(gwState))
                return gateway.getNatGatewayId();
            else {
                LOG.warn("Nat gateway for ELB system account is in invalid state: " +
                        gateway.getNatGatewayId() + ":" + gateway.getState());
            }
        }
        return client.createSystemNatGateway(subnetId, eipAllocationId);
    }

    private static String getElasticIp(final Set<String> exclude) {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();
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

    private static void updateDefaultSecurityGroup(final SecurityGroupItemType group) {
        final EucalyptusActivityTasks client = EucalyptusActivityTasks.getInstance();

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
        if (! egressRules.stream().filter(ip -> "tcp".equals(ip.getIpProtocol()) && servicePort == ip.getFromPort()).findAny().isPresent())
            client.authorizeSystemSecurityGroupEgressRule(groupId, "tcp",  servicePort, servicePort, "0.0.0.0/0");

        final int dnsPort = 53;
        if (! egressRules.stream().filter(ip -> "udp".equals(ip.getIpProtocol()) && dnsPort == ip.getFromPort()).findAny().isPresent())
            client.authorizeSystemSecurityGroupEgressRule(groupId, "udp", dnsPort, dnsPort, "0.0.0.0/0");

        final int ntpPort = 123;
        if (! egressRules.stream().filter(ip -> "udp".equals(ip.getIpProtocol()) && ntpPort == ip.getFromPort()).findAny().isPresent())
            client.authorizeSystemSecurityGroupEgressRule(groupId, "udp", ntpPort, ntpPort, "0.0.0.0/0");

        if (! egressRules.stream().filter(ip -> "icmp".equals(ip.getIpProtocol())).findAny().isPresent())
            client.authorizeSystemSecurityGroupEgressRule(groupId, "icmp", -1, -1, "0.0.0.0/0");
    }

    private static Set<String> KnownAvailabilityZones = Sets.newHashSet();
    /// When there is a new AZ enabled later, system VPC setup should run again
    public static class AvailabilityZoneChecker implements EventListener<ClockTick> {
        private static int CHECK_INTERVAL_SEC = 120;
        private static Date lastCheckTime = new Date(System.currentTimeMillis());

        public static void register() {
            Listeners.register(ClockTick.class, new AvailabilityZoneChecker());
        }

        @SuppressWarnings( { "finally", "ContinueOrBreakFromFinallyBlock" } )
        @Override
        public void fireEvent(ClockTick event) {
            if (Bootstrap.isOperational() &&
                    Topology.isEnabledLocally(LoadBalancing.class) &&
                    Topology.isEnabled(Compute.class) &&
                    isCloudVpc().isPresent() && isCloudVpc().get()) {
                final Date now = new Date(System.currentTimeMillis());
                final int elapsedSec = (int) ((now.getTime() - lastCheckTime.getTime()) / 1000.0);
                if (elapsedSec < CHECK_INTERVAL_SEC)
                    return;
                lastCheckTime = now;

                final List<String> availabilityZones =
                        EucalyptusActivityTasks.getInstance().describeAvailabilityZones().stream()
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
