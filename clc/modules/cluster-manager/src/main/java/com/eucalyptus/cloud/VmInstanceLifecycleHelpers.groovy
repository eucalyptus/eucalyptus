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
package com.eucalyptus.cloud

import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.auth.principal.AccountFullName
import com.eucalyptus.auth.principal.UserFullName
import com.eucalyptus.cloud.VmRunType.Builder as VmRunBuilder
import com.eucalyptus.cloud.run.Allocations.Allocation
import com.eucalyptus.cloud.run.ClusterAllocator
import com.eucalyptus.cloud.run.ClusterAllocator.State
import com.eucalyptus.cloud.util.IllegalMetadataAccessException
import com.eucalyptus.cloud.util.InvalidMetadataException
import com.eucalyptus.cloud.util.MetadataException
import com.eucalyptus.cloud.util.ResourceAllocationException
import com.eucalyptus.cloud.util.SecurityGroupLimitMetadataException
import com.eucalyptus.cluster.callback.StartNetworkCallback
import com.eucalyptus.component.Partition
import com.eucalyptus.component.Partitions
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.common.CloudMetadatas
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.Networking
import com.eucalyptus.compute.common.network.NetworkingFeature
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PrivateNetworkIndexResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.SecurityGroupResource
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource
import com.eucalyptus.compute.identifier.ResourceIdentifiers
import com.eucalyptus.compute.vpc.NetworkInterfaceAssociation
import com.eucalyptus.compute.vpc.NetworkInterfaceAttachment
import com.eucalyptus.compute.vpc.NetworkInterfaceHelper
import com.eucalyptus.compute.vpc.NetworkInterfaces
import com.eucalyptus.compute.vpc.NetworkInterface as VpcNetworkInterface
import com.eucalyptus.compute.vpc.Subnet
import com.eucalyptus.compute.vpc.Subnets
import com.eucalyptus.compute.vpc.VpcConfiguration
import com.eucalyptus.compute.vpc.VpcMetadataNotFoundException
import com.eucalyptus.compute.vpc.VpcRequiredMetadataException
import com.eucalyptus.compute.vpc.Vpcs
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces
import com.eucalyptus.compute.vpc.persist.PersistenceSubnets
import com.eucalyptus.compute.vpc.persist.PersistenceVpcs
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.Transactions
import com.eucalyptus.network.NetworkGroup
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.network.PrivateNetworkIndex
import com.eucalyptus.records.EventRecord
import com.eucalyptus.records.EventType
import com.eucalyptus.records.Logs
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Callback
import com.eucalyptus.util.Cidr
import com.eucalyptus.util.CollectionUtils
import com.eucalyptus.util.Exceptions
import com.eucalyptus.util.LockResource
import com.eucalyptus.util.Ordered
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.util.TypedKey
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.util.async.Request
import com.eucalyptus.util.async.StatefulMessageSet
import com.eucalyptus.util.dns.DomainNames
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstance.VmState
import com.eucalyptus.vm.VmInstance.Builder as VmInstanceBuilder
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.vm.VmNetworkConfig
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Predicates
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets

import edu.ucsb.eucalyptus.cloud.VmInfo
import edu.ucsb.eucalyptus.msgs.InstanceNetworkInterfaceSetItemRequestType
import edu.ucsb.eucalyptus.msgs.InstanceNetworkInterfaceSetRequestType
import edu.ucsb.eucalyptus.msgs.PrivateIpAddressesSetItemRequestType
import edu.ucsb.eucalyptus.msgs.RunInstancesType
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import org.apache.log4j.Logger
import org.springframework.core.OrderComparator

import javax.annotation.Nullable
import javax.persistence.EntityTransaction

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy as JavaProxy
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

import static com.google.common.base.Objects.firstNonNull


/**
 *
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
@CompileStatic
class VmInstanceLifecycleHelpers {
  private static final DispatchingVmInstanceLifecycleHelper dispatchingHelper =
      new DispatchingVmInstanceLifecycleHelper( )

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

  static VmInstanceLifecycleHelper get( ) {
    dispatchingHelper
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
    void prepareNetworkMessages(
        final Allocation allocation,
        final StatefulMessageSet<State> state
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
    void prepareAllocation(
        final VmInfo vmInfo,
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
    void restoreInstanceResources(
        final ResourceToken resourceToken,
        final VmInfo vmInfo
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
          Addresses.getInstance().lookup( publicIPResource.getValue() ) :
          null
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
    static String getPrimaryPrivateIp(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface,
        final String privateIp
    ) {
      instanceNetworkInterface?.privateIpAddressesSet?.item?.find{ PrivateIpAddressesSetItemRequestType item ->
        item.primary?:false
      }?.privateIpAddress ?: instanceNetworkInterface?.privateIpAddress ?: privateIp
    }

    @Nullable
    static Set<String> getSecurityGroupIds(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface
    ) {
      Sets.newLinkedHashSet( instanceNetworkInterface?.groupSet?.item*.groupId ?: [] )
    }

    @Nullable
    static String getDefaultVpcId( final AccountFullName account, final Allocation allocation ) {
      String defaultVpcId = allocation.getAttribute( DefaultVpcIdKey )
      if ( defaultVpcId == null ) {
        Vpcs vpcs = new PersistenceVpcs( )
        try {
          defaultVpcId = vpcs.lookupDefault( account, CloudMetadatas.toDisplayName( ) )
        } catch ( VpcMetadataNotFoundException e ) {
          defaultVpcId = "";
        }
        allocation.setAttribute( DefaultVpcIdKey, defaultVpcId )
      }
      Strings.emptyToNull( defaultVpcId )
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
            allocation.allocationTokens.collect{ ResourceToken token ->
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
          instance.updatePrivateAddress( resource.value )
          PrivateAddresses.associate( resource.value, instance )
        } as Callback<VmInstance>)
      }
    }

    @Override
    void prepareAllocation(
        final VmInfo vmInfo,
        final Allocation allocation ) {
      vmInfo?.netParams?.with {
        if ( ipAddress != null ) {
          allocation?.allocationTokens?.find{ final ResourceToken resourceToken ->
            resourceToken.instanceUuid == vmInfo.uuid
          }?.getAttribute(NetworkResourcesKey)?.add( new PrivateIPResource(
              value: ipAddress, ownerId: vmInfo.instanceId
          ) )
        }
        void
      }
    }

    @Override
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance ) {
      PrivateIPResource resource = ( PrivateIPResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof PrivateIPResource }
      resource?.with{
        instance.updatePrivateAddress( resource.value )
        PrivateAddresses.associate( resource.value, instance )
      }
    }

    @Override
    void cleanUpInstance(
        final VmInstance instance,
        final VmState state ) {
      if ( VmInstance.VmStateSet.TORNDOWN.contains( state ) ) try {
        if ( instance.networkIndex == null &&
            !Strings.isNullOrEmpty( instance.privateAddress ) &&
            !VmNetworkConfig.DEFAULT_IP.equals( instance.privateAddress ) ) {
          Networking.instance.release( new ReleaseNetworkResourcesType( resources: [
              new PrivateIPResource( value: instance.privateAddress, ownerId: instance.displayName )
          ] as ArrayList<NetworkResource> ) )
        }
      } catch ( final Exception ex ) {
        logger.error( "Error releasing private address '${instance.privateAddress}' on instance '${instance.displayName}' clean up.", ex )
      }
    }
  }

  static final class PrivateNetworkIndexVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final Logger logger = Logger.getLogger( PrivateNetworkIndexVmInstanceLifecycleHelper )

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, PrivateNetworkIndexResource ) ) {
        allocation?.allocationTokens?.each{ ResourceToken token ->
          Collection<NetworkResource> resources = token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource resource ->
            resource instanceof PrivateNetworkIndexResource && resource.ownerId != null }
          token.getAttribute(NetworkResourcesKey).removeAll( resources )
          prepareNetworkResourcesType.resources.addAll( resources )
        }
      }
    }

    @Override
    void prepareVmRunType(
        final ResourceToken resourceToken,
        final VmRunBuilder builder ) {
      doWithPrivateNetworkIndex( resourceToken ){ Integer vlan, Long networkIndex ->
        builder.vlan( vlan )
        builder.networkIndex( networkIndex )
      }
    }

    @Override
    void prepareVmInstance(
        final ResourceToken resourceToken,
        final VmInstanceBuilder builder) {
      doWithPrivateNetworkIndex( resourceToken ){ Integer vlan, Long networkIndex ->
        builder.networkIndex( Transactions.find( PrivateNetworkIndex.named( vlan, networkIndex ) ) )
      }
    }

    @Override
    void prepareAllocation(
        final VmInfo vmInfo,
        final Allocation allocation ) {
      vmInfo?.netParams?.with {
        if ( vlan != null && networkIndex != null && networkIndex > -1 ) {
          allocation?.allocationTokens?.find{ final ResourceToken resourceToken ->
            resourceToken.instanceUuid == vmInfo.uuid
          }?.getAttribute(NetworkResourcesKey)?.add( new PrivateNetworkIndexResource(
              ownerId: vmInfo.instanceId,
              tag: String.valueOf( vlan ),
              value: String.valueOf( networkIndex )
          ) )
        }
        void
      }
    }

    @Override
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance ) {
      doWithPrivateNetworkIndex( resourceToken ){ Integer vlan, Long networkIndex ->
        Entities.transaction( PrivateNetworkIndex ) {
          Entities.uniqueResult( PrivateNetworkIndex.named( vlan, networkIndex ) ).with{
            set( instance )
            instance.setNetworkIndex( (PrivateNetworkIndex) getDelegate( ) )
          }
        }
      }
    }

    @Override
    void cleanUpInstance(
        final VmInstance instance,
        final VmState state ) {
      if ( VmInstance.VmStateSet.TORNDOWN.contains( state ) && Entities.isPersistent( instance ) ) try {
        if ( instance.networkIndex != null ) {
          instance.networkIndex.release( )
          instance.networkIndex.teardown( )
          instance.networkIndex = null
        }
      } catch ( final ResourceAllocationException ex ) {
        logger.error( "Error cleaning up network index '${instance.networkIndex}' for instance '${instance.displayName}'", ex )
      }
    }

    private <V> V doWithPrivateNetworkIndex( final ResourceToken resourceToken,
                                             final Closure<V> closure ) {
      PrivateNetworkIndexResource resource = ( PrivateNetworkIndexResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof PrivateNetworkIndexResource }
      resource?.with{
        closure.call( Integer.valueOf( tag ),  Long.valueOf( value ) )
      }
    }
  }

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
          allocation?.allocationTokens?.each{ ResourceToken token ->
            Collection<NetworkResource> resources =
                token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource resource -> resource instanceof PublicIPResource }
            if ( resources.isEmpty( ) ) {
              resources = [ new PublicIPResource( ownerId: token.instanceId ) ] as Collection<NetworkResource>
            }
            token.getAttribute(NetworkResourcesKey).removeAll( resources )
            prepareNetworkResourcesType.resources.addAll( resources )
          }
        }
      }
    }

    @Override
    void prepareAllocation(
        final VmInfo vmInfo,
        final Allocation allocation
    ) {
      vmInfo?.netParams?.with {
        if ( !Strings.isNullOrEmpty( ignoredPublicIp ) && !VmNetworkConfig.DEFAULT_IP.equals( ignoredPublicIp ) ) {
          allocation?.allocationTokens?.find{ final ResourceToken resourceToken ->
            resourceToken.instanceUuid == vmInfo.uuid
          }?.getAttribute(NetworkResourcesKey)?.add( new PublicIPResource( ownerId: vmInfo.instanceId, value: ignoredPublicIp ) )
        }
        void
      }
    }

    @Override
    void restoreInstanceResources(
        final ResourceToken resourceToken,
        final VmInfo vmInfo ) {
      Optional<String> publicIp = Optional.fromNullable( resourceToken?.getAttribute( NetworkResourcesKey )?.find{
        NetworkResource resource -> resource instanceof PublicIPResource }?.value )
      restoreAddress( publicIp, vmInfo )
    }

    private static void restoreAddress(
        final Optional<String> reservedPublicIp,
        final VmInfo input ) {
      Entities.transaction( VmInstance ) { EntityTransaction db ->
        try {
          final VmInstance instance = VmInstances.lookup( input.getInstanceId( ) )

          Closure<?> assign = { Address address ->
            address.assign( instance ).clearPending( )
            null
          }

          Closure<?> updateInstanceAddresses = { Address address ->
            instance.updateAddresses( input.getNetParams( ).getIpAddress( ), address.getDisplayName( ) )
            null
          }

          if ( reservedPublicIp.isPresent( ) ) { // Restore impending system address
            final Address pendingAddress = Addresses.getInstance().lookup( reservedPublicIp.get( ) )
            updateInstanceAddresses.call( pendingAddress )
            if ( !pendingAddress.isReallyAssigned( ) ) {
              assign.call( pendingAddress )
            }
          } else { // Check for / restore an Elastic IP
            final UserFullName userFullName = UserFullName.getInstance( input.getOwnerId( ) )
            final Address address = Addresses.getInstance( ).lookup( input.getNetParams( ).getIgnoredPublicIp( ) )
            if ( address.isAssigned( ) &&
                address.getInstanceAddress( ).equals( input.getNetParams( ).getIpAddress( ) ) &&
                address.getInstanceId( ).equals( input.getInstanceId( ) ) ) {
              updateInstanceAddresses.call( address )
            } else if ( !address.isAssigned( ) && address.isAllocated( ) && address.getOwnerAccountNumber( ).equals( userFullName.getAccountNumber() ) ) {
              updateInstanceAddresses.call( address )
              assign.call( address )
            }
          }

          db.commit( )
        } catch ( final Exception e ) {
          Logger.getLogger( PublicIPVmInstanceLifecycleHelper ).error( "Failed to restore address state " + input.getNetParams( )
              + " for instance "
              + input.getInstanceId( )
              + " because of: "
              + e.getMessage( ) )
          Logs.extreme( ).error( e, e )
        }
      }
    }
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  static final class SecurityGroupVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( )

      final Set<String> networkNames = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupNames( ) )
      final Set<String> networkIds = Sets.newLinkedHashSet( allocation.getRequest().securityGroupsIds() )
      final String defaultVpcId = getDefaultVpcId( accountFullName, allocation )
      final String vpcId = allocation?.subnet?.vpc?.displayName ?: defaultVpcId
      final boolean isVpc = vpcId != null
      if ( networkNames.isEmpty( ) && networkIds.isEmpty( ) ) {
        if ( !isVpc ) Threads.enqueue( Eucalyptus, VmInstanceLifecycleHelper, 5 ){
          NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) )
        }
        networkNames.add( NetworkGroups.defaultNetworkName( ) )
      }

      final List<NetworkGroup> groups = Lists.newArrayList( )
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName() ) ).isPresent() ) {
          if ( !isVpc ) {
            groups.add(NetworkGroups.lookup(accountFullName, groupName))
          } else if ( defaultVpcId && defaultVpcId == vpcId ) {
            groups.add( NetworkGroups.lookup( accountFullName, defaultVpcId, groupName ) )
          } else if ( groupName == NetworkGroups.defaultNetworkName( ) ) {
            groups.add( NetworkGroups.lookupDefault( accountFullName, vpcId ) )
          }
        }
      }
      for ( String groupId : networkIds ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroups.groupId() ) ).isPresent() ) {
          groups.add( NetworkGroups.lookupByGroupId( accountFullName, groupId ) )
        }
      }

      final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( )
      for ( final NetworkGroup group : groups ) {
        if ( !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " +group.getGroupId()+ "/" + group.getDisplayName() + " for " + allocation.getOwnerFullName( ).getUserName( ) )
        }
        networkRuleGroups.put( group.getDisplayName(), group )
      }

      allocation.setNetworkRules( networkRuleGroups )
    }

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, SecurityGroupResource ) ) {
        prepareNetworkResourcesType.getResources( ).addAll( allocation.networkGroups*.groupId?.
            collect( SecurityGroupResource.&forId ) ?: [ ] as List<SecurityGroupResource> )
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

  @SuppressWarnings("GroovyUnusedDeclaration")
  static final class ExtantNetworkVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    @Override
    void prepareNetworkMessages(
        final Allocation allocation,
        final StatefulMessageSet<State> messages
    ) {
      final NetworkGroup net = allocation.getPrimaryNetwork( )
      if ( net ) {
        Entities.transaction( NetworkGroup ) { EntityTransaction db ->
          if ( Entities.merge( net ).hasExtantNetwork( ) ) {
            final Request callback = AsyncRequests.newRequest( new StartNetworkCallback( allocation.getExtantNetwork( ) ) )
            messages.addRequest( State.CREATE_NETWORK, callback )
            EventRecord.here( ClusterAllocator, EventType.VM_PREPARE, callback.getClass( ).getSimpleName( ), net.toString( ) ).debug( )
          }
          void
        }
      }
    }
  }

  /**
   * For network interface, including mac address and private IP address
   */
  static final class VpcNetworkInterfaceVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final NetworkInterfaces networkInterfaces = new PersistenceNetworkInterfaces( )
    private static final Subnets subnets = new PersistenceSubnets( )

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

        final Partition partition = Partitions.lookupByName( allocation.subnet.availabilityZone );
        allocation.setPartition( partition );
      }
    }

    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final RunInstancesType runInstances = (RunInstancesType) allocation.request

      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface =
          getPrimaryNetworkInterface( runInstances )
      final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.privateIpAddress )
      final String subnetId = ResourceIdentifiers.tryNormalize( ).apply( instanceNetworkInterface?.subnetId ?: runInstances.subnetId )
      final Set<String> networkIds = getSecurityGroupIds( instanceNetworkInterface )
      final Set<NetworkingFeature> networkingFeatures = Networking.getInstance( ).describeFeatures( );
      if ( !Strings.isNullOrEmpty( subnetId ) || instanceNetworkInterface != null ) {
        if ( !networkingFeatures.contains( NetworkingFeature.Vpc ) ) {
          throw new InvalidMetadataException( "EC2-VPC not supported, for EC2-Classic do not specify subnet or network interface" )
        }
        String networkInterfaceSubnetId = null
        String networkInterfaceAvailabilityZone = null
        if ( instanceNetworkInterface?.networkInterfaceId != null ) {
          if ( runInstances.minCount > 1 || runInstances.maxCount > 1 ) {
            throw new InvalidMetadataException( "Network interface can only be specified for a single instance" )
          }

          Entities.transaction( VpcNetworkInterface ){
            final VpcNetworkInterface networkInterface =
                lookup( "network interface", instanceNetworkInterface.networkInterfaceId, VpcNetworkInterface ) as
                    VpcNetworkInterface
            networkInterfaceSubnetId = networkInterface.subnet.displayName
            networkInterfaceAvailabilityZone = networkInterface.availabilityZone
          }
        }

        final String resolveSubnetId = subnetId?:networkInterfaceSubnetId
        if ( !resolveSubnetId ) throw new InvalidMetadataException( "SubnetId required" )

        final Subnet subnet = lookup( "subnet", resolveSubnetId, Subnet ) as Subnet;
        if ( privateIp && !Cidr.parse( subnet.cidr ).contains( privateIp ) ) {
          throw new InvalidMetadataException( "Private IP ${privateIp} not valid for subnet ${subnetId} cidr ${subnet.cidr}" )
        }

        if ( networkInterfaceAvailabilityZone && networkInterfaceAvailabilityZone != subnet.availabilityZone ) {
          throw new InvalidMetadataException( "Network interface availability zone (${networkInterfaceAvailabilityZone}) not valid for subnet ${subnetId} zone ${subnet.availabilityZone}" )
        }

        final Set<NetworkGroup> groups = Sets.newHashSet( )
        for ( String groupId : networkIds ) {
          if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroups.groupId() ) ).isPresent() ) {
            groups.add( lookup( "security group", groupId, NetworkGroup  ) as NetworkGroup )
          }
        }
        if ( groups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
          throw new SecurityGroupLimitMetadataException( );
        }

        allocation.subnet = subnet

        final Partition partition = Partitions.lookupByName( subnet.availabilityZone );
        allocation.setPartition( partition );
      } else {
        // Default VPC, lookup subnet for user specified or system selected partition
        final AccountFullName accountFullName = allocation.ownerFullName.asAccountFullName( )
        final String defaultVpcId = getDefaultVpcId( accountFullName, allocation )
        if ( defaultVpcId != null && !networkingFeatures.contains( NetworkingFeature.Vpc ) ) {
          throw new InvalidMetadataException( "EC2-VPC not supported, delete default VPC to run in EC2-Classic" )
        } else if ( defaultVpcId == null && !networkingFeatures.contains( NetworkingFeature.Classic ) ) {
          throw new VpcRequiredMetadataException( );
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

      if ( allocation.subnet && !prepareFromTokenResources( allocation, prepareNetworkResourcesType, VpcNetworkInterfaceResource ) ) {
        allocation?.allocationTokens?.each{ ResourceToken token ->
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
                      deleteOnTerminate: firstNonNull( instanceNetworkInterface.deleteOnTermination, true )
                  )
              ] as Collection<NetworkResource>
            } else {
              final String identifier = ResourceIdentifiers.generateString( 'eni' )
              final String mac = NetworkInterfaceHelper.mac( identifier )
              final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.privateIpAddress )
              final Set<String> networkIds = getSecurityGroupIds( instanceNetworkInterface )
              resources = [
                  new VpcNetworkInterfaceResource(
                      ownerId: token.instanceId,
                      value: identifier,
                      mac: mac,
                      privateIp: privateIp,
                      description: instanceNetworkInterface?.description ?: 'Primary network interface',
                      deleteOnTerminate: firstNonNull( instanceNetworkInterface?.deleteOnTermination, true ),
                      networkGroupIds: networkIds as ArrayList<String>
                  )
              ] as Collection<NetworkResource>
            }
          }
          token.getAttribute(NetworkResourcesKey).removeAll( resources )
          prepareNetworkResourcesType.resources.addAll( resources )
        }
      }
    }

    @Override
    void prepareVmRunType(
        final ResourceToken resourceToken,
        final VmRunBuilder builder
    ) {
      VpcNetworkInterfaceResource resource = ( VpcNetworkInterfaceResource ) \
          resourceToken.getAttribute(NetworkResourcesKey).find{ it instanceof VpcNetworkInterfaceResource }
      resource?.with{
        if ( resource.mac == null ) {
          VpcNetworkInterface networkInterface =
              RestrictedTypes.resolver( VpcNetworkInterface ).apply( resource.value )
          builder.privateAddress( networkInterface.privateIpAddress )
          builder.macAddress( networkInterface.macAddress )
        } else {
          builder.privateAddress( resource.privateIp )
          builder.macAddress( resource.mac )
        }
      }
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    void prepareVmInstance( final ResourceToken resourceToken,
                            final VmInstanceBuilder builder ) {
      NetworkInterfaces networkInterfaces = VpcNetworkInterfaceVmInstanceLifecycleHelper.networkInterfaces
      VpcNetworkInterfaceResource resource = resourceToken.getAttribute(NetworkResourcesKey).find{
        it instanceof VpcNetworkInterfaceResource } as VpcNetworkInterfaceResource
      if ( resource ) {
        builder.onBuild( { VmInstance instance ->
          if ( resource.privateIp!=null ) PrivateAddresses.associate( resource.privateIp, instance )
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
          instance.updateMacAddress( networkInterface.macAddress )
          instance.updatePrivateAddress( networkInterface.privateIpAddress )
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
          Address address = getAddress( resourceToken )
          if ( address != null ) {
            address.assign( networkInterface ).start( instance )
            networkInterface.associate( NetworkInterfaceAssociation.create(
                address.associationId,
                address.allocationId,
                address.ownerAccountNumber,
                address.displayName,
                networkInterface.vpc.dnsHostnames ?
                    VmInstances.dnsName( address.displayName, DomainNames.externalSubdomain( ) ) :
                    null as String
            ) )
            instance.updatePublicAddress( address.displayName );
          } else {
            NetworkInterfaceHelper.start( networkInterface, instance )
          }
          // Add so eni information is available from instance, not for
          // persistence
          instance.getNetworkInterfaces( ).add( networkInterface );
        } as Callback<VmInstance> )
      }
    }

    @Override
    void startVmInstance( final ResourceToken resourceToken, final VmInstance instance ) {
      for ( VpcNetworkInterface networkInterface : instance.networkInterfaces ) {
        NetworkInterfaceHelper.start( networkInterface, instance )
      }
    }

    @Override
    void cleanUpInstance( final VmInstance instance, final VmState state ) {
      for ( VpcNetworkInterface networkInterface : instance.networkInterfaces ) {
        if ( VmInstance.VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
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

    // Generic parameter (type) hits https://jira.codehaus.org/browse/GROOVY-6556 so use ?
    private static <T> T lookup( final String typeDesc, final String id, final Class<?> type ) {
      final T resource = RestrictedTypes.<T>resolver( (Class<T>)type ).apply( id )
      if ( !RestrictedTypes.filterPrivileged( ).apply( resource ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use ${typeDesc} ${id}" )
      }
      resource
    }


  }

}
