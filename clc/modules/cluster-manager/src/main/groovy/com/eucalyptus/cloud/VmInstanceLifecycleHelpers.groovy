/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloud

import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.auth.AuthContext
import com.eucalyptus.auth.principal.AccountFullName
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.cluster.common.msgs.VmRunType.Builder as VmRunBuilder
import com.eucalyptus.cloud.run.Allocations.Allocation
import com.eucalyptus.cluster.common.ResourceToken
import com.eucalyptus.compute.common.internal.network.NoSuchGroupMetadataException
import com.eucalyptus.compute.common.internal.util.IllegalMetadataAccessException
import com.eucalyptus.compute.common.internal.util.InvalidMetadataException
import com.eucalyptus.compute.common.internal.util.InvalidParameterCombinationMetadataException
import com.eucalyptus.compute.common.internal.util.MetadataException
import com.eucalyptus.compute.common.internal.util.SecurityGroupLimitMetadataException
import com.eucalyptus.component.Partition
import com.eucalyptus.component.Partitions
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.common.CloudMetadatas
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemRequestType
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetRequestType
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType
import com.eucalyptus.compute.common.backend.RunInstancesType
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.Networking
import com.eucalyptus.compute.common.network.NetworkingFeature
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.SecurityGroupResource
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment
import com.eucalyptus.compute.vpc.NetworkInterfaceHelper
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface as VpcNetworkInterface
import com.eucalyptus.compute.vpc.NetworkInterfaceInUseMetadataException
import com.eucalyptus.compute.vpc.NoSuchNetworkInterfaceMetadataException
import com.eucalyptus.compute.vpc.NoSuchSubnetMetadataException
import com.eucalyptus.compute.common.internal.vpc.Subnet
import com.eucalyptus.compute.common.internal.vpc.Subnets
import com.eucalyptus.compute.vpc.NotEnoughPrivateAddressResourcesException
import com.eucalyptus.compute.vpc.PrivateAddressResourceAllocationException
import com.eucalyptus.compute.vpc.VpcConfiguration
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException
import com.eucalyptus.compute.vpc.VpcRequiredMetadataException
import com.eucalyptus.compute.common.internal.vpc.Vpcs
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces
import com.eucalyptus.compute.vpc.persist.PersistenceSubnets
import com.eucalyptus.compute.vpc.persist.PersistenceVpcs
import com.eucalyptus.entities.Entities
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.network.IPRange
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.network.PublicAddresses
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Callback
import com.eucalyptus.util.Cidr
import com.eucalyptus.util.CollectionUtils
import com.eucalyptus.util.LockResource
import com.eucalyptus.util.Ordered
import com.eucalyptus.auth.type.RestrictedType
import com.eucalyptus.util.Pair
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.util.TypedKey
import com.eucalyptus.util.dns.DomainNames
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState
import com.eucalyptus.vm.VmInstances.Builder as VmInstanceBuilder
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig
import com.google.common.base.Function
import com.google.common.base.MoreObjects
import com.google.common.base.Optional
import com.google.common.base.Predicates
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets

import com.eucalyptus.cluster.common.msgs.NetworkConfigType
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import org.springframework.core.OrderComparator

import javax.annotation.Nonnull
import javax.annotation.Nullable

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy as JavaProxy
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

import static com.google.common.base.MoreObjects.firstNonNull


/**
 *
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
@CompileStatic
class VmInstanceLifecycleHelpers {
  private static final DispatchingVmInstanceLifecycleHelper dispatchingHelper =
      new DispatchingVmInstanceLifecycleHelper( )

  static {
    VmInstanceLifecycleHelper.Registry.helper.set( dispatchingHelper )
  }

  static class DispatchingVmInstanceLifecycleHelper {
    private final ReadWriteLock helpersLock = new ReentrantReadWriteLock( )
    private final AtomicReference<List<VmInstanceLifecycleHelper>> helpers =
        new AtomicReference<List<VmInstanceLifecycleHelper>>( Lists.newArrayList( ) )

    @Delegate VmInstanceLifecycleHelper instance = VmInstanceLifecycleHelper.cast( JavaProxy.newProxyInstance(
        VmInstanceLifecycleHelper.getClassLoader( ),
        [ VmInstanceLifecycleHelper ] as Class<?>[],
        { Object proxy, Method method, Object[] args ->
          LockResource.withLock( helpersLock.readLock( ) ){ helpers.get( ) }.each {
            try {
              method.invoke( it, args )
            } catch ( InvocationTargetException e ) {
              throw e.targetException
            }
          }
        } as InvocationHandler
    ) )
  }

  @PackageScope
  static void register( final VmInstanceLifecycleHelper helper ) {
    LockResource.withLock( dispatchingHelper.helpersLock.writeLock( ) ) {
      List<VmInstanceLifecycleHelper> helpers = Lists.newArrayList( dispatchingHelper.helpers.get( ) )
      helpers.add( helper )
      Collections.sort( helpers, new OrderComparator( ) )
      dispatchingHelper.helpers.set( helpers )
    }
  }

  static abstract class VmInstanceLifecycleHelperSupport implements Ordered, VmInstanceLifecycleHelper {
    public static final int DEFAULT_ORDER = 0

    @Override
    int getOrder( ) {
      DEFAULT_ORDER
    }

    @Override
    void verifyAllocation(
        final Allocation allocation
    ) throws MetadataException {
    }

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
    }

    @Override
    void verifyNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesResultType prepareNetworkResourcesResultType
    ) {
    }

    @Override
    void prepareVmRunType(
        final ResourceToken resourceToken,
        final VmRunBuilder builder
    ) {
    }

    @Override
    void prepareVmInstance(
        final ResourceToken resourceToken,
        final VmInstanceBuilder builder
    ) {
    }

    @Override
    void prepareAllocation(
        final VmInstance instance,
        final Allocation allocation
    ) {
    }

    @Override
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance
    ) {
    }

    @Override
    void cleanUpInstance(
        final VmInstance instance,
        final VmState state
    ) {
    }
  }

  static abstract class NetworkResourceVmInstanceLifecycleHelper extends VmInstanceLifecycleHelperSupport {
    public static final TypedKey<Set<NetworkResource>> NetworkResourcesKey =
        TypedKey.create( "NetworkResources", { Sets.newHashSet( ) } as Supplier<Set<NetworkResource>> )
    public static final TypedKey<String> DefaultVpcIdKey =
        TypedKey.create( "DefaultVpcId", Suppliers.<String>ofInstance( null ) )

    static boolean prepareFromTokenResources(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType,
        final Class<? extends NetworkResource> resourceClass
    ) {
      Collection<NetworkResource> resources = allocation.allocationTokens.collect{ ResourceToken resourceToken ->
        ( NetworkResource ) resourceToken.getAttribute(NetworkResourcesKey).find{ NetworkResource resource ->
          resourceClass.isInstance( resource ) }
      }.findAll( )

      allocation.allocationTokens.each{ ResourceToken resourceToken ->
        resourceToken.getAttribute(NetworkResourcesKey).removeAll( resources )
      }
      prepareNetworkResourcesType.getResources( ).addAll( resources )

      !resources.isEmpty( )
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    static Address getAddress( final ResourceToken token ) {
      final PublicIPResource publicIPResource = (PublicIPResource) Iterables.find(
          token.getAttribute( NetworkResourceVmInstanceLifecycleHelper.NetworkResourcesKey ),
          Predicates.instanceOf( PublicIPResource.class ),
          null )
      return publicIPResource!=null && publicIPResource.getValue()!=null ?
          Addresses.getInstance().lookupActiveAddress( publicIPResource.value ) :
          null
    }

    static void withBatch( final Closure<?> closure ) {
      final Addresses.AddressingBatch batch = Addresses.getInstance( ).batch( )
      try {
        closure.call( )
      } finally {
        batch.close( )
      }
    }

    @Nullable
    static InstanceNetworkInterfaceSetItemRequestType getPrimaryNetworkInterface(
        final RunInstancesType runInstancesType
    ) {
      runInstancesType?.networkInterfaceSet?.item?.find{ InstanceNetworkInterfaceSetItemRequestType item ->
        item.deviceIndex == 0
      }
    }

    @Nullable
    static Iterable<InstanceNetworkInterfaceSetItemRequestType> getSecondaryNetworkInterfaces(
        final RunInstancesType runInstancesType
    ) {
      final InstanceNetworkInterfaceSetItemRequestType primary = getPrimaryNetworkInterface( runInstancesType )
      runInstancesType?.networkInterfaceSet?.item?.findAll{ InstanceNetworkInterfaceSetItemRequestType item ->
        item != primary // check against primary in case multiple interfaces with device index 0
      } ?: []
    }

    @Nullable
    static String getPrimaryPrivateIp(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface,
        final String privateIp
    ) {
      instanceNetworkInterface?.privateIpAddressesSet?.item?.find{ PrivateIpAddressesSetItemRequestType item ->
        item.primary?:false
      }?.privateIpAddress ?: instanceNetworkInterface?.privateIpAddress ?: Strings.emptyToNull( privateIp )
    }

    @Nullable
    static Set<String> getSecurityGroupIds(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface
    ) {
      normalizeIdentifiers( instanceNetworkInterface?.groupSet?.item*.groupId as Iterable<String> )
    }

    @Nullable
    static String getDefaultVpcId( final AccountFullName account, final Allocation allocation ) {
      String defaultVpcId = allocation.getAttribute( DefaultVpcIdKey )
      if ( defaultVpcId == null ) {
        Vpcs vpcs = new PersistenceVpcs( )
        try {
          defaultVpcId = vpcs.lookupDefault( account, CloudMetadatas.toDisplayName( ) )
        } catch ( VpcMetadataNotFoundException e ) {
          defaultVpcId = ""
        }
        allocation.setAttribute( DefaultVpcIdKey, defaultVpcId )
      }
      Strings.emptyToNull( defaultVpcId )
    }

    static String normalizeIdentifier( final String identifier ) {
      ResourceIdentifiers.tryNormalize( ).apply( identifier )
    }

    @Nonnull
    static Set<String> normalizeIdentifiers( @Nullable final Iterable<String> identifiers ) {
      final Set<String> normalizedIdentifiers = Sets.newLinkedHashSet( )
      if ( identifiers ) {
        Iterables.addAll(
            normalizedIdentifiers,
            Iterables.transform( identifiers, ResourceIdentifiers.tryNormalize( ) ) )
      }
      normalizedIdentifiers
    }

    @Nonnull
    static <T extends RestrictedType> T lookup( final String account, final String typeDesc, final String id, final Class<T> type ) {
      final T resource = RestrictedTypes.<T>resolver( type ).apply( normalizeIdentifier( id ) )
      if ( !RestrictedTypes.filterPrivileged( ).apply( resource ) ||
          ( account && account != resource.owner.accountNumber ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use ${typeDesc} ${id}" )
      }
      resource
    }
  }

  static final class PrivateIPVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final Logger logger = Logger.getLogger( PrivateIPVmInstanceLifecycleHelper )

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, PrivateIPResource ) ) {
        prepareNetworkResourcesType.getResources( ).addAll(
            allocation.allocationTokens.collect{ VmInstanceToken token ->
              new PrivateIPResource( ownerId: token.instanceId ) }
        )
      }
    }

    @Override
    void prepareVmRunType(
        final ResourceToken resourceToken,
        final VmRunBuilder builder
    ) {
      PrivateIPResource resource = ( PrivateIPResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof PrivateIPResource }
      resource?.with{
        builder.macAddress( resource.mac )
        builder.privateAddress( resource.value )
      }
    }

    @Override
    void prepareVmInstance(
        final ResourceToken resourceToken,
        final VmInstanceBuilder builder) {
      PrivateIPResource resource = ( PrivateIPResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof PrivateIPResource }
      resource?.with{
        builder.onBuild({ VmInstance instance ->
          instance.updateMacAddress( resource.mac )
          VmInstances.updatePrivateAddress( instance, resource.value )
          if ( !instance.vpcId ) {
            PrivateAddresses.associate( resource.value, instance )
          }
        } as Callback<VmInstance>)
      }
    }

    @Override
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance ) {
      PrivateIPResource resource = ( PrivateIPResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof PrivateIPResource }
      resource?.with{
        instance.updateMacAddress( resource.mac )
        VmInstances.updatePrivateAddress( instance, resource.value )
        if ( !instance.vpcId ) {
          PrivateAddresses.associate( resource.value, instance )
        }
      }
    }

    @Override
    void cleanUpInstance(
        final VmInstance instance,
        final VmState state ) {
      if ( VmInstance.VmStateSet.TORNDOWN.contains( state ) ) try {
        if ( !instance.vpcId &&
            !Strings.isNullOrEmpty( instance.privateAddress ) &&
            VmNetworkConfig.DEFAULT_IP != instance.privateAddress) {
          Networking.instance.release( new ReleaseNetworkResourcesType( resources: [
              new PrivateIPResource( value: instance.privateAddress, ownerId: instance.displayName )
          ] as ArrayList<NetworkResource> ) )
          VmInstances.updatePrivateAddress( instance, VmNetworkConfig.DEFAULT_IP )
        }
      } catch ( final Exception ex ) {
        logger.error( "Error releasing private address '${instance.privateAddress}' on instance '${instance.displayName}' clean up.", ex )
      }
    }
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  static final class PublicIPVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      final String vpcId = allocation.subnet?.vpc?.displayName
      final Boolean isVpc = vpcId != null
      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface = getPrimaryNetworkInterface( ((RunInstancesType)allocation.request) )
      final Boolean requestedAllocation = instanceNetworkInterface?.associatePublicIpAddress
      if ( ( isVpc && (instanceNetworkInterface?.networkInterfaceId==null) && firstNonNull(requestedAllocation,allocation.subnet.mapPublicIpOnLaunch) ) ||
          ( !isVpc && !allocation.usePrivateAddressing ) ) {
        if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, PublicIPResource ) ) {
          allocation?.allocationTokens?.each{ VmInstanceToken token ->
            Collection<NetworkResource> resources =
                token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource resource -> resource instanceof PublicIPResource }
            if ( resources.isEmpty( ) ) {
              resources = [ new PublicIPResource( ownerId: token.instanceId ) ] as Collection<NetworkResource>
            }
            token.getAttribute(NetworkResourcesKey).removeAll( resources )
            prepareNetworkResourcesType.resources.addAll( resources )
          }
        }
      } else if ( isVpc ) {
        allocation.usePrivateAddressing = true
      }
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    void prepareVmInstance( final ResourceToken resourceToken,
                            final VmInstanceBuilder builder ) {
      Address address = getAddress( resourceToken )
      if ( address ) {
        builder.onBuild({ VmInstance instance ->
          if ( !instance.vpcId ) { // Network interface handles public IP for VPC
            withBatch {
              Addresses.getInstance( ).assign( address, instance )
              Addresses.AddressingBatch.reset( ) // Flush after running
            }
            VmInstances.updatePublicAddress( instance, address.address )
          }
        })
      }
    }

    @Override
    void startVmInstance( final ResourceToken resourceToken,
                          final VmInstance instance ) {
      Address address = getAddress( resourceToken )
      if ( address && !instance.vpcId ) {
        Addresses.getInstance( ).assign( address, instance )
        VmInstances.updatePublicAddress( instance, address.address )
      }
    }

    @Override
    void cleanUpInstance( final VmInstance instance, final VmState state ) {
      // For EC2-Classic unassign any public/elastic IP
      if ( !instance.vpcId &&
          !( VmNetworkConfig.DEFAULT_IP == instance.publicAddress || Strings.isNullOrEmpty( instance.publicAddress ) ) &&
          VmInstance.VmStateSet.TORNDOWN.contains( state )
      ) {
        if ( Addresses.getInstance( ).unassign( instance.publicAddress ) ) {
          VmInstances.updatePublicAddress( instance, VmNetworkConfig.DEFAULT_IP )
          PublicAddresses.markDirty( instance.publicAddress, instance.partition )
        }
      }
    }
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  static final class SecurityGroupVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final Logger logger = Logger.getLogger( SecurityGroupVmInstanceLifecycleHelper )

    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final AccountFullName accountFullName = allocation.ownerFullName.asAccountFullName( )

      final Set<String> networkNames = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupNames( ) )
      final Set<String> networkIds = Sets.newLinkedHashSet( allocation.getRequest().securityGroupsIds() )
      final String defaultVpcId = getDefaultVpcId( accountFullName, allocation )
      final String vpcId = allocation?.subnet?.vpc?.displayName ?: defaultVpcId
      final boolean isVpc = vpcId != null
      if ( networkNames.isEmpty( ) && networkIds.isEmpty( ) ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) )
      }
      if ( !isVpc && networkNames.contains( NetworkGroups.defaultNetworkName( ) ) ) try {
        // Use separate thread so group is committed immediately
        Threads.enqueue( Eucalyptus, VmInstanceLifecycleHelper, 5 ){
          NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) )
        }.get( )
      } catch( final Exception e ) {
        logger.trace( "Error looking up default network", e )
      }

      final List<NetworkGroup> groups = Lists.newArrayList( )
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName() ) ).isPresent() ) {
          if ( !isVpc ) {
            groups.add( NetworkGroups.lookup( accountFullName, groupName ) )
          } else if ( defaultVpcId && defaultVpcId == vpcId ) {
            groups.add( NetworkGroups.lookup( null, defaultVpcId, groupName ) )
          } else if ( groupName == NetworkGroups.defaultNetworkName( ) ) {
            groups.add( NetworkGroups.lookupDefault( null, vpcId ) )
          }
        }
      }
      final AuthContext authContext = allocation.authContext.get( )
      for ( String groupId : networkIds ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId() ) ).isPresent() ) {
          groups.add( NetworkGroups.lookupByGroupId( authContext.isSystemUser( ) ? null : accountFullName, groupId ) )
        }
      }
      if (Collections.singleton(vpcId) != Sets.newHashSet(Iterables.transform(groups, NetworkGroup.vpcId()))) {
        throw new InvalidMetadataException( "Invalid security groups (inconsistent VPC)" )
      }

      final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( )
      for ( final NetworkGroup group : groups ) {
        if ( !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " +group.groupId+ "/" + group.displayName + " for " + allocation.ownerFullName.userName )
        }
        networkRuleGroups.put( group.displayName, group )
      }

      if ( allocation.networkGroups.isEmpty( ) ) {
        allocation.setNetworkRules( networkRuleGroups )
      }
    }

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, SecurityGroupResource ) ) {
        prepareNetworkResourcesType.getResources( ).addAll( ( allocation.networkGroups*.groupId?.
            collect( SecurityGroupResource.&forId ) ?: [ ] ) as List<SecurityGroupResource> )
      }
    }

    @Override
    void cleanUpInstance(
        final VmInstance instance,
        final VmState state ) {
      if ( VmInstance.VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
        instance.networkGroups.clear( )
      }
    }
  }

  /**
   * For network interface, including mac address and private IP address
   */
  static final class VpcNetworkInterfaceVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final NetworkInterfaces networkInterfaces = new PersistenceNetworkInterfaces( )
    private static final Subnets subnets = new PersistenceSubnets( )
    private static final TypedKey<List<InstanceNetworkInterfaceSetItemRequestType>> secondaryNetworkInterfacesKey =
        TypedKey.create( "SecondaryNetworkInterfaces", { Lists.newArrayList( ) } as Supplier<List<InstanceNetworkInterfaceSetItemRequestType>> )

    /**
     * This helper has a higher precedence to ensure the zone / subnet are initialized prior to other lifecycle helpers using them
     */
    @Override
    int getOrder() {
      -100
    }

    @Override
    void prepareAllocation( final VmInstance instance, final Allocation allocation ) {
      if ( instance.vpcId ) {
        allocation.runInstancesRequest.networkInterfaceSet = new InstanceNetworkInterfaceSetRequestType(
            item: [
                new InstanceNetworkInterfaceSetItemRequestType(
                    deviceIndex: 0,
                    networkInterfaceId: instance.networkInterfaces[0].displayName,
                    associatePublicIpAddress: false
                )
            ] as ArrayList<InstanceNetworkInterfaceSetItemRequestType>
        )
        allocation.subnet = instance.bootRecord.subnet

        final Partition partition = Partitions.lookupByName( allocation.subnet.availabilityZone )
        allocation.setPartition( partition )
      }
    }

    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final RunInstancesType runInstances = (RunInstancesType) allocation.request
      final boolean allowMultiVpc = Principals.systemUser().name == runInstances.effectiveUserId

      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface =
          getPrimaryNetworkInterface( runInstances )
      final Iterable<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces =
          getSecondaryNetworkInterfaces( runInstances )
      final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.privateIpAddress )
      final String subnetId = normalizeIdentifier( instanceNetworkInterface?.subnetId ?: runInstances.subnetId )
      final Set<String> networkIds = getSecurityGroupIds( instanceNetworkInterface )
      final Set<NetworkingFeature> networkingFeatures = Networking.getInstance( ).describeFeatures( )
      //noinspection GrDeprecatedAPIUsage
      final String requiredResourceAccountNumber = !allocation.context.privileged ?
          allocation.ownerFullName.accountNumber :
          null
      if ( !Strings.isNullOrEmpty( subnetId ) ||
          instanceNetworkInterface != null ||
          !Iterables.isEmpty( secondaryNetworkInterfaces ) ) {
        if (!networkingFeatures.contains(NetworkingFeature.Vpc)) {
          throw new InvalidMetadataException("EC2-VPC not supported, for EC2-Classic do not specify subnet or network interface")
        }
        if ( runInstances.addressingType ) {
          throw new InvalidMetadataException("Addressing scheme not supported in EC2-VPC")
        }

        if (instanceNetworkInterface != null) {
          if ( !runInstances.securityGroupNames( ).isEmpty( ) ||
              !runInstances.securityGroupsIds( ).isEmpty( ) ) {
            throw new InvalidParameterCombinationMetadataException(
                "Network interfaces and an instance-level security groups may not be specified on the same request" )
          }

          if ( !Strings.isNullOrEmpty( runInstances.subnetId ) ) {
            throw new InvalidParameterCombinationMetadataException(
                "Network interfaces and an instance-level subnet ID may not be specified on the same request" )
          }

          if ( !Strings.isNullOrEmpty( runInstances.privateIpAddress ) ) {
            throw new InvalidParameterCombinationMetadataException(
                "Network interfaces and an instance-level private address may not be specified on the same request" )
          }
          if ( MoreObjects.firstNonNull( instanceNetworkInterface.associatePublicIpAddress, false ) && secondaryNetworkInterfaces ) {
            throw new InvalidParameterCombinationMetadataException(
                "The associatePublicIPAddress parameter cannot be specified when launching with multiple network interfaces." )
          }
        } else if ( secondaryNetworkInterfaces ) {
          throw new InvalidParameterCombinationMetadataException(
              "Primary network interface required when secondary network interface(s) specified" )
        }

        String networkInterfaceSubnetId = null
        String networkInterfaceAvailabilityZone = null
        if ( instanceNetworkInterface?.networkInterfaceId != null ) {
          if ( runInstances.minCount > 1 || runInstances.maxCount > 1 ) {
            throw new InvalidMetadataException( "Network interface can only be specified for a single instance" )
          }
          Entities.transaction( VpcNetworkInterface ){
            final VpcNetworkInterface networkInterface
            try {
              networkInterface =
                  lookup( requiredResourceAccountNumber, "network interface", instanceNetworkInterface.networkInterfaceId, VpcNetworkInterface) as
                      VpcNetworkInterface
            } catch ( Exception e ) {
              throw new NoSuchNetworkInterfaceMetadataException( "Network interface (${instanceNetworkInterface.networkInterfaceId}) not found", e )
            }
            if ( networkInterface.attached ) {
              throw new NetworkInterfaceInUseMetadataException( "Network interface (${instanceNetworkInterface.networkInterfaceId}) in use", )
            }
            networkInterfaceSubnetId = networkInterface.subnet.displayName
            networkInterfaceAvailabilityZone = networkInterface.availabilityZone
          }
        }
        final String resolveSubnetId = subnetId?:networkInterfaceSubnetId
        if ( !resolveSubnetId ) throw new InvalidMetadataException( "SubnetId required" )
        final Subnet subnet
        try {
          subnet = lookup( requiredResourceAccountNumber, "subnet", resolveSubnetId, Subnet )
        } catch ( NoSuchElementException e ) {
          throw new NoSuchSubnetMetadataException( "Subnet (${resolveSubnetId}) not found", e )
        }
        if ( privateIp ) {
          if ( runInstances.minCount > 1 || runInstances.maxCount > 1 ) {
            throw new InvalidMetadataException("Private address can only be specified for a single instance")
          }
          if ( !validPrivateIpForCidr( subnet.cidr, privateIp ) ) {
            throw new InvalidMetadataException( "Private address ${privateIp} not valid for subnet ${subnetId} cidr ${subnet.cidr}" )
          }
        }
        if ( networkInterfaceAvailabilityZone && networkInterfaceAvailabilityZone != subnet.availabilityZone ) {
          throw new InvalidMetadataException( "Network interface availability zone (${networkInterfaceAvailabilityZone}) not valid for subnet ${subnetId} zone ${subnet.availabilityZone}" )
        }
        final Set<NetworkGroup> groups = Sets.newHashSet( )
        for ( String groupId : networkIds ) {
          if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId() ) ).isPresent() ) try {
            groups.add( lookup( requiredResourceAccountNumber, "security group", groupId, NetworkGroup  ) )
          } catch ( Exception e ) {
            throw new NoSuchGroupMetadataException( "Security group (${groupId}) not found", e )
          }
        }
        if ( !groups.empty && Collections.singleton(subnet.vpc.displayName) != Sets.newHashSet(Iterables.transform(groups, NetworkGroup.vpcId()))) {
          throw new InvalidMetadataException( "Invalid security groups (inconsistent VPC)" )
        }
        if ( groups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
          throw new SecurityGroupLimitMetadataException( )
        }

        if ( secondaryNetworkInterfaces ) Entities.transaction( VpcNetworkInterface ) {
          final int maxInterfaces = allocation.getVmType( )?.getNetworkInterfaces( )?:1
          final Set<Integer> deviceIndexes = [ 0 ] as Set<Integer>
          final Set<String> networkInterfaceIds = [ ] as Set<String>
          final Set<Pair<String,String>> subnetPrivateAddresses = Sets.newHashSet( )
          if ( instanceNetworkInterface?.networkInterfaceId ) {
            networkInterfaceIds.add( normalizeIdentifier( instanceNetworkInterface.networkInterfaceId ) )
          }
          if ( privateIp ) {
            subnetPrivateAddresses.add( Pair.pair( subnet.displayName, privateIp ) )
          }
          secondaryNetworkInterfaces.each { InstanceNetworkInterfaceSetItemRequestType networkInterfaceItem ->
            if ( networkInterfaceItem.networkInterfaceId != null && ( runInstances.minCount > 1 || runInstances.maxCount > 1 ) ) {
              throw new InvalidMetadataException("Network interface can only be specified for a single instance")
            }
            Integer networkInterfaceDeviceIndex = networkInterfaceItem.deviceIndex
            if ( networkInterfaceDeviceIndex == null ) {
              throw new InvalidMetadataException("Network interface device index required" )
            } else if ( !deviceIndexes.add( networkInterfaceDeviceIndex ) ) {
              throw new InvalidMetadataException("Network interface duplicate device index (${networkInterfaceDeviceIndex})" )
            } else if ( networkInterfaceDeviceIndex < 1 || networkInterfaceDeviceIndex > 31 ) {
              throw new InvalidMetadataException("Network interface device index invalid ${networkInterfaceDeviceIndex}")
            }

            Subnet secondarySubnet = null
            if ( networkInterfaceItem.networkInterfaceId ) try {
              final String secondaryNetworkInterfaceId = normalizeIdentifier( networkInterfaceItem.networkInterfaceId )
              if ( !networkInterfaceIds.add( secondaryNetworkInterfaceId ) ) {
                throw new InvalidMetadataException("Network interface duplicate (${networkInterfaceItem.networkInterfaceId})" )
              }
              final VpcNetworkInterface secondaryNetworkInterface =
                  lookup(requiredResourceAccountNumber, "network interface", secondaryNetworkInterfaceId, VpcNetworkInterface)
              if ( secondaryNetworkInterface.attached ) {
                throw new NetworkInterfaceInUseMetadataException( "Network interface (${networkInterfaceItem.networkInterfaceId}) in use", )
              }
              secondarySubnet = secondaryNetworkInterface.subnet
            } catch ( InvalidMetadataException e ) {
              throw e
            } catch ( Exception e ) {
              throw new NoSuchNetworkInterfaceMetadataException( "Network interface (${networkInterfaceItem.networkInterfaceId}) not found", e )
            }
            if ( !secondarySubnet ) try {
              secondarySubnet = lookup( requiredResourceAccountNumber, "subnet", networkInterfaceItem.subnetId, Subnet )
            } catch ( Exception e ) {
              throw new NoSuchSubnetMetadataException( "Subnet (${networkInterfaceItem.subnetId}) not found", e )
            }
            final String secondaryPrivateIp = getPrimaryPrivateIp( networkInterfaceItem, null )
            if ( secondaryPrivateIp ) {
              if ( runInstances.minCount > 1 || runInstances.maxCount > 1 ) {
                throw new InvalidMetadataException("Private address can only be specified for a single instance")
              }
              if ( !validPrivateIpForCidr( secondarySubnet.cidr, secondaryPrivateIp ) ) {
                throw new InvalidMetadataException("Private address ${secondaryPrivateIp} not valid for subnet ${secondarySubnet.displayName} cidr ${secondarySubnet.cidr}")
              }
              if ( !subnetPrivateAddresses.add( Pair.pair( secondarySubnet.displayName, secondaryPrivateIp ) ) ) {
                throw new InvalidMetadataException("Network interface duplicate private address (${secondaryPrivateIp})" )
              }
            }
            if ( !allowMultiVpc && subnet.vpc.displayName != secondarySubnet.vpc.displayName ) {
              throw new InvalidMetadataException( "Network interface vpc (${secondarySubnet.vpc.displayName}) for ${secondarySubnet.displayName} not valid for instance vpc ${subnet.vpc.displayName}" )
            }
            if ( secondarySubnet.availabilityZone != subnet.availabilityZone ) {
              throw new InvalidMetadataException( "Network interface availability zone (${secondarySubnet.availabilityZone}) for ${secondarySubnet.displayName} not valid for subnet ${subnet.displayName} zone ${subnet.availabilityZone}" )
            }
            final Set<String> secondaryNetworkIds = getSecurityGroupIds( networkInterfaceItem )
            final Set<NetworkGroup> secondaryGroups = [] as Set<NetworkGroup>
            for ( String groupId : secondaryNetworkIds ) {
              if ( !Iterables.tryFind( secondaryGroups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId() ) ).isPresent() ) try {
                secondaryGroups.add( lookup( requiredResourceAccountNumber, "security group", groupId, NetworkGroup ) )
              } catch ( Exception e ) {
                throw new NoSuchGroupMetadataException( "Security group (${groupId}) not found", e )
              }
            }
            if ( !secondaryGroups.empty && Collections.singleton(secondarySubnet.vpc.displayName) != Sets.newHashSet(Iterables.transform(secondaryGroups, NetworkGroup.vpcId()))) {
              throw new InvalidMetadataException( "Invalid security groups for device ${networkInterfaceItem.deviceIndex} (inconsistent VPC)" )
            }
            if ( secondaryGroups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
              throw new SecurityGroupLimitMetadataException( )
            }
          }
          if ( deviceIndexes.size( ) > maxInterfaces ) {
            throw new InvalidMetadataException(
                "Interface count ${deviceIndexes.size( )} exceeds the limit for ${allocation.getVmType( )?.name}" )
          }
        }

        allocation.subnet = subnet

        final Partition partition = Partitions.lookupByName( subnet.availabilityZone )
        allocation.setPartition( partition )

        final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( )
        for ( final NetworkGroup networkGroup : groups ) {
          networkRuleGroups.put( networkGroup.displayName, networkGroup )
        }
        if ( !networkRuleGroups.isEmpty( ) ) {
          allocation.setNetworkRules( networkRuleGroups )
        }

        allocation.setAttribute( secondaryNetworkInterfacesKey, ImmutableList.copyOf( secondaryNetworkInterfaces ) )
      } else {
        // Default VPC, lookup subnet for user specified or system selected partition
        final AccountFullName accountFullName = allocation.ownerFullName.asAccountFullName( )
        final String defaultVpcId = getDefaultVpcId( accountFullName, allocation )
        if ( defaultVpcId != null && !networkingFeatures.contains( NetworkingFeature.Vpc ) ) {
          throw new InvalidMetadataException( "EC2-VPC not supported, delete default VPC to run in EC2-Classic" )
        } else if ( defaultVpcId == null && !networkingFeatures.contains( NetworkingFeature.Classic ) ) {
          throw new VpcRequiredMetadataException( )
        } else if ( runInstances.addressingType && !networkingFeatures.contains( NetworkingFeature.Classic ) ) {
          throw new InvalidMetadataException("Addressing scheme not supported in EC2-VPC")
        }
      }
    }

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType) {
      // Default VPC, lookup subnet for user specified or system selected partition
      final AccountFullName accountFullName = allocation.ownerFullName.asAccountFullName( )
      final String defaultVpcId = getDefaultVpcId( accountFullName, allocation )
      if ( defaultVpcId != null && allocation.partition != null && allocation.subnet == null ) {
        allocation.subnet = subnets.lookupDefault( accountFullName, allocation.partition.name, { Subnet subnet -> subnet } as Function<Subnet,Subnet> )
      }
      prepareNetworkResourcesType.vpc = allocation?.subnet?.vpc?.displayName
      prepareNetworkResourcesType.subnet = allocation?.subnet?.displayName
      final List<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces =
          allocation.getAttribute( secondaryNetworkInterfacesKey )

      if ( allocation.subnet && !prepareFromTokenResources( allocation, prepareNetworkResourcesType, VpcNetworkInterfaceResource ) ) {
        allocation?.allocationTokens?.each{ VmInstanceToken token ->
          Collection<NetworkResource> resources = token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource resource ->
            resource instanceof VpcNetworkInterfaceResource && resource.ownerId != null }
          if ( resources.isEmpty( ) ) {
            final RunInstancesType runInstances = ((RunInstancesType)allocation.request)
            final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface = getPrimaryNetworkInterface( runInstances )
            if ( instanceNetworkInterface?.networkInterfaceId != null ) {
              resources = [
                  new VpcNetworkInterfaceResource(
                      ownerId: token.instanceId,
                      value: instanceNetworkInterface.networkInterfaceId,
                      device: 0,
                      deleteOnTerminate: firstNonNull( instanceNetworkInterface.deleteOnTermination, false )
                  )
              ] as Collection<NetworkResource>
            } else {
              final String identifier = ResourceIdentifiers.generateString( 'eni' )
              final String mac = NetworkInterfaceHelper.mac( identifier )
              final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.privateIpAddress )
              final Set<String> networkIds =
                  getSecurityGroupIds( instanceNetworkInterface ) ?: ( allocation.networkGroups*.groupId as Set<String> )
              resources = [
                  new VpcNetworkInterfaceResource(
                      ownerId: token.instanceId,
                      value: identifier,
                      device: 0,
                      mac: mac,
                      privateIp: privateIp,
                      description: instanceNetworkInterface?.description ?: 'Primary network interface',
                      deleteOnTerminate: firstNonNull( instanceNetworkInterface?.deleteOnTermination, true ),
                      networkGroupIds: networkIds as ArrayList<String>
                  )
              ] as Collection<NetworkResource>
            }
            secondaryNetworkInterfaces.each{ InstanceNetworkInterfaceSetItemRequestType secondaryNetworkInterface ->
              if ( secondaryNetworkInterface?.networkInterfaceId != null ) {
                resources <<
                    new VpcNetworkInterfaceResource(
                        ownerId: token.instanceId,
                        value: secondaryNetworkInterface.networkInterfaceId,
                        device: secondaryNetworkInterface.deviceIndex,
                        deleteOnTerminate: firstNonNull( secondaryNetworkInterface.deleteOnTermination, false )
                    )
              } else {
                final String identifier = ResourceIdentifiers.generateString( 'eni' )
                final String mac = NetworkInterfaceHelper.mac( identifier )
                final String privateIp = getPrimaryPrivateIp( secondaryNetworkInterface, null )
                final Set<String> networkIds = getSecurityGroupIds( secondaryNetworkInterface )
                final Subnet subnet = RestrictedTypes.resolver( Subnet ).apply( secondaryNetworkInterface.subnetId )
                resources <<
                    new VpcNetworkInterfaceResource(
                        ownerId: token.instanceId,
                        value: identifier,
                        device: secondaryNetworkInterface.deviceIndex,
                        vpc: subnet.vpc.displayName,
                        subnet: subnet.displayName,
                        mac: mac,
                        privateIp: privateIp,
                        description: secondaryNetworkInterface?.description ?: 'Secondary network interface',
                        deleteOnTerminate: firstNonNull( secondaryNetworkInterface?.deleteOnTermination, true ),
                        networkGroupIds: networkIds as ArrayList<String>
                    )
              }
            }
          }
          token.getAttribute(NetworkResourcesKey).removeAll( resources )
          prepareNetworkResourcesType.resources.addAll( resources )
        }
      }
    }

    @Override
    void verifyNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesResultType prepareNetworkResourcesResultType
    ) {
      final Set<Pair<String,Integer>> privateAddressDeviceIndexPairs = Sets.newHashSet( )

      final RunInstancesType runInstances = (RunInstancesType) allocation.request
      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface =
          getPrimaryNetworkInterface( runInstances )
      if ( instanceNetworkInterface ) {
        String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.privateIpAddress )
        if ( privateIp ) {
          privateAddressDeviceIndexPairs << Pair.pair( privateIp, 0 )
        }
      }

      final List<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces =
          allocation.getAttribute(secondaryNetworkInterfacesKey)
      secondaryNetworkInterfaces.each { InstanceNetworkInterfaceSetItemRequestType secondaryNetworkInterface ->
        String privateIp = getPrimaryPrivateIp( secondaryNetworkInterface, null )
        if ( privateIp ) {
          privateAddressDeviceIndexPairs << Pair.pair( privateIp, secondaryNetworkInterface.deviceIndex )
        }
      }

      // check that there is a resource for each device index requested
      allocation.allocationTokens.each { ResourceToken token ->
        privateAddressDeviceIndexPairs.each{ final Pair<String,Integer> addressAndDeviceIndex ->
          if ( token.getAttribute(NetworkResourcesKey).find{ NetworkResource networkResource ->
            networkResource instanceof VpcNetworkInterfaceResource &&
                ((VpcNetworkInterfaceResource)networkResource).device == addressAndDeviceIndex.right
          } == null ) {
            throw new PrivateAddressResourceAllocationException( "Private address not available (${addressAndDeviceIndex.left})" )
          }
        }

        if ( instanceNetworkInterface ) {
          if ( Iterables.size( token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource networkResource ->
            networkResource instanceof VpcNetworkInterfaceResource } ) != ( secondaryNetworkInterfaces.size( ) + 1 ) ) {
            throw new NotEnoughPrivateAddressResourcesException( "Insufficient private addresses" )
          }
        }
      }
    }

    @Override
    void prepareVmRunType(
        final ResourceToken resourceToken,
        final VmRunBuilder builder
    ) {
      Iterable<NetworkResource> resources = resourceToken.getAttribute(NetworkResourcesKey).findAll{
        it instanceof VpcNetworkInterfaceResource }
      if ( !Iterables.isEmpty( resources ) ) {
        // Handle primary interface
        final VpcNetworkInterfaceResource primaryInterface = resources.find{ NetworkResource networkResource ->
          networkResource instanceof VpcNetworkInterfaceResource && ((VpcNetworkInterfaceResource)networkResource).device == 0
        } as VpcNetworkInterfaceResource

        if ( primaryInterface.mac == null ) {
          VpcNetworkInterface networkInterface =
              RestrictedTypes.resolver( VpcNetworkInterface ).apply( primaryInterface.value )
          builder.privateAddress( networkInterface.privateIpAddress )
          builder.macAddress( networkInterface.macAddress )
          builder.primaryEniAttachmentId( networkInterface.attachment.attachmentId )
        } else {
          builder.privateAddress( primaryInterface.privateIp )
          builder.macAddress( primaryInterface.mac )
          builder.primaryEniAttachmentId( primaryInterface.attachmentId )
        }

        // Handle secondary interfaces
        final List<VpcNetworkInterfaceResource> secondaryInterfaces = resources.findAll{ NetworkResource networkResource ->
          networkResource instanceof VpcNetworkInterfaceResource && ((VpcNetworkInterfaceResource)networkResource).device != 0
        } as List<VpcNetworkInterfaceResource>

        secondaryInterfaces.each { VpcNetworkInterfaceResource secondaryInterface ->
          NetworkConfigType netConfig = new NetworkConfigType(secondaryInterface.value, secondaryInterface.device)
          if ( secondaryInterface.mac == null ) {
            VpcNetworkInterface networkInterface =
                RestrictedTypes.resolver( VpcNetworkInterface ).apply( secondaryInterface.value )
            netConfig.macAddress = networkInterface.macAddress
            netConfig.ipAddress = networkInterface.privateIpAddress
            netConfig.attachmentId = networkInterface.attachment.attachmentId
          } else {
            netConfig.macAddress = secondaryInterface.mac
            netConfig.ipAddress = secondaryInterface.privateIp
            netConfig.attachmentId = secondaryInterface.attachmentId
          }
          builder.secondaryNetConfig(netConfig)
        }
      }
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    void prepareVmInstance( final ResourceToken resourceToken,
                            final VmInstanceBuilder builder ) {
      NetworkInterfaces networkInterfaces = VpcNetworkInterfaceVmInstanceLifecycleHelper.networkInterfaces
      Iterable<NetworkResource> resources = resourceToken.getAttribute(NetworkResourcesKey).findAll{
        it instanceof VpcNetworkInterfaceResource }
      if ( !Iterables.isEmpty( resources ) ) {
        builder.onBuild( { VmInstance instance ->
          final VpcNetworkInterfaceResource resource = resources.find{ NetworkResource networkResource ->
            networkResource instanceof VpcNetworkInterfaceResource && ((VpcNetworkInterfaceResource)networkResource).device == 0
          } as VpcNetworkInterfaceResource
          VpcNetworkInterface networkInterface = resource.mac == null ?
              RestrictedTypes.resolver( VpcNetworkInterface ).apply( resource.value ) :
              networkInterfaces.save( VpcNetworkInterface.create(
                  instance.owner,
                  instance.bootRecord.vpc,
                  instance.bootRecord.subnet,
                  resource.networkGroupIds.collect{ String groupId ->
                    NetworkGroups.lookupByGroupId( groupId )
                  } as Set<NetworkGroup>,
                  resource.value,
                  resource.mac,
                  resource.privateIp,
                  instance.bootRecord.vpc.dnsHostnames ?
                      VmInstances.dnsName( resource.privateIp, DomainNames.internalSubdomain( ) ) :
                      null as String,
                  firstNonNull( resource.description, "" )
              ) )
          if ( resource.privateIp != null ) {
            PrivateAddresses.associate( resource.privateIp, networkInterface )
          }
          instance.updateMacAddress( networkInterface.macAddress )
          VmInstances.updatePrivateAddress( instance, networkInterface.privateIpAddress )
          resource.privateIp = networkInterface.privateIpAddress
          resource.mac = networkInterface.macAddress
          networkInterface.attach( NetworkInterfaceAttachment.create(
              ResourceIdentifiers.generateString( "eni-attach" ),
              instance,
              instance.displayName,
              instance.owner.accountNumber,
              0,
              NetworkInterfaceAttachment.Status.attached,
              new Date( ),
              resource.deleteOnTerminate
          ) )
          resource.attachmentId = networkInterface.attachment.attachmentId
          Address address = getAddress( resourceToken )
          withBatch {
            if ( address != null ) {
              NetworkInterfaceHelper.associate( address, networkInterface, Optional.of( instance ) )
            } else {
              if ( networkInterface.associated ) {
                VmInstances.updatePublicAddress( instance, networkInterface.association.publicIp )
              }
              NetworkInterfaceHelper.start( networkInterface, instance )
            }
            Addresses.AddressingBatch.reset( ) // Flush after running
          }
          // Add so eni information is available from instance, not for
          // persistence
          instance.addNetworkInterface( networkInterface )

          // Handle secondary interfaces
          final List<VpcNetworkInterfaceResource> secondaryResources = resources.findAll{ NetworkResource networkResource ->
            networkResource instanceof VpcNetworkInterfaceResource && ((VpcNetworkInterfaceResource)networkResource).device != 0
          } as List<VpcNetworkInterfaceResource>
          secondaryResources.sort(
              true,
              { VpcNetworkInterfaceResource networkInterfaceResource -> networkInterfaceResource.device }
          )
          secondaryResources.each { VpcNetworkInterfaceResource secondaryResource ->
            final Subnet subnet = secondaryResource.subnet == null ?
                null :
                RestrictedTypes.resolver( Subnet ).apply( secondaryResource.subnet )
            VpcNetworkInterface secondaryNetworkInterface = secondaryResource.mac == null ?
                RestrictedTypes.resolver( VpcNetworkInterface ).apply( secondaryResource.value ) :
                networkInterfaces.save( VpcNetworkInterface.create(
                    instance.owner,
                    subnet.vpc,
                    subnet,
                    secondaryResource.networkGroupIds.collect{ String groupId ->
                      NetworkGroups.lookupByGroupId( groupId )
                    } as Set<NetworkGroup>,
                    secondaryResource.value,
                    secondaryResource.mac,
                    secondaryResource.privateIp,
                    instance.bootRecord.vpc.dnsHostnames ?
                        VmInstances.dnsName( secondaryResource.privateIp, DomainNames.internalSubdomain( ) ) :
                        null as String,
                    firstNonNull( secondaryResource.description, "" )
                ) )
            if ( secondaryResource.privateIp ) {
              PrivateAddresses.associate( secondaryResource.privateIp, secondaryNetworkInterface )
            }
            secondaryResource.privateIp = secondaryNetworkInterface.privateIpAddress
            secondaryResource.mac = secondaryNetworkInterface.macAddress
            secondaryNetworkInterface.attach( NetworkInterfaceAttachment.create(
                ResourceIdentifiers.generateString( "eni-attach" ),
                instance,
                instance.displayName,
                instance.owner.accountNumber,
                secondaryResource.device,
                NetworkInterfaceAttachment.Status.attached,
                new Date( ),
                secondaryResource.deleteOnTerminate
            ) )
            secondaryResource.attachmentId = secondaryNetworkInterface.attachment.attachmentId
            withBatch {
              NetworkInterfaceHelper.start(secondaryNetworkInterface, instance)
              Addresses.AddressingBatch.reset( ) // Flush after running
            }
            // Add so eni information is available from instance, not for
            // persistence
            instance.addNetworkInterface( secondaryNetworkInterface )
          }
        } as Callback<VmInstance> )
      }
    }

    @Override
    void startVmInstance( final ResourceToken resourceToken, final VmInstance instance ) {
      for ( VpcNetworkInterface networkInterface : instance.networkInterfaces ) {
        if ( networkInterface.attached && networkInterface.attachment.deviceIndex == 0 ) {
          if ( networkInterface.associated ) {
            VmInstances.updateAddresses( instance, networkInterface.privateIpAddress, networkInterface.association.publicIp )
          } else {
            VmInstances.updatePrivateAddress(instance, networkInterface.privateIpAddress)
          }
        }
        NetworkInterfaceHelper.start( networkInterface, instance )
      }
    }

    @Override
    void cleanUpInstance( final VmInstance instance, final VmState state ) {
      for ( VpcNetworkInterface networkInterface : instance.networkInterfaces ) {
        if ( networkInterface.associated && VmInstance.VmStateSet.DONE.contains( state ) ) {
          PublicAddresses.markDirty( networkInterface.association.publicIp, instance.partition )
        }
        if ( VmInstance.VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
          if ( networkInterface.attached && networkInterface.attachment.deviceIndex == 0 ) {
            VmInstances.updateAddresses( instance, VmNetworkConfig.DEFAULT_IP, VmNetworkConfig.DEFAULT_IP )
          }
          if ( networkInterface?.attachment?.deleteOnTerminate ) {
            NetworkInterfaceHelper.release( networkInterface )
            Entities.delete( networkInterface )
          } else if ( networkInterface.associated ) {
            NetworkInterfaceHelper.stop( networkInterface )
          }
          networkInterface.detach( )
        } else if ( VmInstance.VmStateSet.TORNDOWN.contains( state ) && Entities.isPersistent( instance ) ) {
          NetworkInterfaceHelper.stop( networkInterface )
        }
      }
    }

    /**
     * The first four and last addresses are reserved in a subnet
     */
    private static boolean validPrivateIpForCidr( String cidrText, String privateIp ) {
      boolean valid = true
      if ( privateIp ) {
        final int addressAsInt = PrivateAddresses.asInteger( privateIp )
        final Cidr cidr = Cidr.parse( cidrText )
        final IPRange range = IPRange.fromCidr( cidr ) // range omits first and last
        valid = range.contains( IPRange.fromCidr( Cidr.of( addressAsInt, 32 ) ) ) && //
            !Iterables.contains( Iterables.limit( IPRange.fromCidr( cidr ), 3 ), addressAsInt )
      }
      valid
    }
  }

}
