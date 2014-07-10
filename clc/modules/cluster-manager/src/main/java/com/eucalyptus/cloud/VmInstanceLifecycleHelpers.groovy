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
import com.eucalyptus.cloud.util.MetadataException
import com.eucalyptus.cloud.util.ResourceAllocationException
import com.eucalyptus.cluster.callback.StartNetworkCallback
import com.eucalyptus.component.Partition
import com.eucalyptus.component.Partitions
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.Networking
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PrivateNetworkIndexResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.SecurityGroupResource
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource
import com.eucalyptus.compute.common.network.VpcResource
import com.eucalyptus.compute.identifier.ResourceIdentifiers
import com.eucalyptus.compute.vpc.NetworkInterfaceAttachment
import com.eucalyptus.compute.vpc.NetworkInterfaces
import com.eucalyptus.compute.vpc.NetworkInterface as VpcNetworkInterface
import com.eucalyptus.compute.vpc.Subnet
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces
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
import com.eucalyptus.util.LockResource
import com.eucalyptus.util.Ordered
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.util.TypedKey
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.util.async.Request
import com.eucalyptus.util.async.StatefulMessageSet
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstance.VmState
import com.eucalyptus.vm.VmInstance.Builder as VmInstanceBuilder
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.vm.VmNetworkConfig
import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.cloud.VmInfo
import edu.ucsb.eucalyptus.msgs.RunInstancesType
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import org.springframework.core.OrderComparator

import javax.persistence.EntityTransaction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy as JavaProxy
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 *
 */
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
          LockResource.withLock( helpersLock.readLock( ) ){ helpers.get( ) }.each { method.invoke( it, args ) }
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
    private static final Logger logger = Logger.getLogger( PublicIPVmInstanceLifecycleHelper )

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !allocation.usePrivateAddressing ) {
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
        if ( !Strings.isNullOrEmpty( ignoredPublicIp ) != null && !VmNetworkConfig.DEFAULT_IP == ignoredPublicIp ) {
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
          logger.error( "Failed to restore address state " + input.getNetParams( )
              + " for instance "
              + input.getInstanceId( )
              + " because of: "
              + e.getMessage( ) )
          Logs.extreme( ).error( e, e )
        }
      }
    }
  }

  static final class SecurityGroupVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      //TODO:STEVE: update to verify VPC constraints
      final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( )

      final Set<String> networkNames = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupNames( ) )
      final Set<String> networkIds = Sets.newLinkedHashSet( allocation.getRequest().securityGroupsIds() )
      final String vpcId = allocation?.subnet?.vpc?.displayName
      final boolean isVpc = vpcId != null
      if ( networkNames.isEmpty( ) && networkIds.isEmpty( ) ) {
        Threads.enqueue( Eucalyptus, VmInstanceLifecycleHelper, 5 ){
          NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) )
        }
        networkNames.add( NetworkGroups.defaultNetworkName( ) )
      }

      final List<NetworkGroup> groups = Lists.newArrayList( )
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName() ) ).isPresent() ) {
          if ( !isVpc ) {
            groups.add( NetworkGroups.lookup( accountFullName, groupName ) )
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
    final NetworkInterfaces networkInterfaces = new PersistenceNetworkInterfaces( )

    @Override
    void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final RunInstancesType runInstances = (RunInstancesType) allocation.request
      final String subnetId = runInstances.subnetId
      final String privateIp = runInstances.privateIpAddress
      if ( !Strings.isNullOrEmpty( subnetId ) || !Strings.isNullOrEmpty( privateIp ) ) {
        final Subnet subnet = Entities.transaction( Subnet ){ Entities.uniqueResult( Subnet.exampleWithName( null, subnetId ) ) }
        if ( !RestrictedTypes.filterPrivileged( ).apply( subnet ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use subnet ${subnetId} for ${allocation.ownerFullName.userName}" )
        }
        if ( !Cidr.parse( subnet.cidr ).contains( privateIp ) ) {
          throw new MetadataException( "Private IP ${privateIp} not valid for subnet ${subnetId} cidr ${subnet.cidr}" )
        }
        allocation.subnet = subnet

        //TODO:STEVE: Error if availability zone was set in request
        final Partition partition = Partitions.lookupByName( subnet.getAvailabilityZone( ) );
        allocation.setPartition( partition );
      }
    }

    @Override
    void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType) {
      if ( allocation.subnet && !prepareFromTokenResources( allocation, prepareNetworkResourcesType, VpcResource ) ) {
        allocation?.allocationTokens?.each{ ResourceToken token ->
          Collection<NetworkResource> resources = token.getAttribute(NetworkResourcesKey).findAll{ NetworkResource resource ->
            resource instanceof VpcResource && resource.ownerId != null }
          if ( resources.isEmpty( ) ) {
            String identifier = ResourceIdentifiers.generateString( 'eni' )
            //TODO:STEVE: mac address prefix? use eni identifier for mac uniqueness
            final String mac = String.format( "d0:0d:%s:%s:%s:%s",
                token.instanceId.substring( 2, 4 ),
                token.instanceId.substring( 4, 6 ),
                token.instanceId.substring( 6, 8 ),
                token.instanceId.substring( 8, 10 ) )
                //String.format( "d0:0d:%s:%s:%s:%s", identifier.substring( 4, 6 ), identifier.substring( 6, 8 ), identifier.substring( 8, 10 ), identifier.substring( 10, 12 ) );
            resources = [
                new VpcResource(
                    ownerId: token.instanceId,
                    value: allocation.subnet.vpc.displayName
                ),
                new VpcNetworkInterfaceResource(
                    ownerId: token.instanceId,
                    value: identifier,
                    mac: mac,
                    privateIp: ((RunInstancesType)allocation.request).privateIpAddress // TODO:STEVE: track address usage and update subnet free address count
                )
            ] as Collection<NetworkResource>
            if ( allocation.subnet.mapPublicIpOnLaunch ) {
              resources << new PublicIPResource( ownerId: identifier )
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
        builder.privateAddress( resource.privateIp )
      }
    }

    @Override
    void prepareVmInstance( final ResourceToken resourceToken,
                            final VmInstanceBuilder builder ) {
      List<VpcNetworkInterfaceResource> resources =  resourceToken.getAttribute(NetworkResourcesKey).findAll{
        it instanceof VpcNetworkInterfaceResource } as List<VpcNetworkInterfaceResource>
      if ( resources ) {
        builder.onBuild( { VmInstance instance ->
          instance.updateMacAddress( resources[0].mac )
          instance.updatePrivateAddress( resources[0].privateIp )
          int index = 0;
          for ( VpcNetworkInterfaceResource resource : resources  ) {
            VpcNetworkInterface networkInterface = networkInterfaces.save( VpcNetworkInterface.create(
                instance.owner,
                instance.bootRecord.vpc,
                instance.bootRecord.subnet,
                resource.value,
                resource.mac,
                resource.privateIp,
                'Primary network interface' //TODO:STEVE: use description passed in run instances (if any)
            ) )
            networkInterface.attach( NetworkInterfaceAttachment.create(
                ResourceIdentifiers.generateString( "eni-attach" ),
                instance,
                instance.displayName,
                instance.owner.accountNumber,
                index,
                NetworkInterfaceAttachment.Status.attached,
                new Date( ),
                true //TODO:STEVE: use value passed in run instances (if any)
            ) )
            // Add so eni information is available from instance, not for
            // persistence
            instance.getNetworkInterfaces( ).add( networkInterface );
            index++
          }
        } as Callback<VmInstance> )
      }
    }

    @Override
    void cleanUpInstance( final VmInstance instance, final VmState state ) {
      if ( VmInstance.VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
        for ( VpcNetworkInterface networkInterface : instance.networkInterfaces ) {
          if ( networkInterface?.attachment?.deleteOnTerminate ) {
            Entities.delete( networkInterface )
          }
          networkInterface.detach( )
        }
      }
    }
  }

}
