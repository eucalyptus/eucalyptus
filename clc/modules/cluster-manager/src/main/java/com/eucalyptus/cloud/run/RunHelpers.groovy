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
package com.eucalyptus.cloud.run

import com.eucalyptus.auth.principal.AccountFullName
import com.eucalyptus.cloud.ResourceToken
import com.eucalyptus.cloud.VmRunType.Builder as VmRunBuilder
import com.eucalyptus.cloud.util.IllegalMetadataAccessException
import com.eucalyptus.cloud.util.MetadataException
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.Transactions
import com.eucalyptus.network.NetworkGroup
import com.eucalyptus.network.NetworkGroups
import com.eucalyptus.network.NetworkResource
import com.eucalyptus.network.PrepareNetworkResourcesType
import com.eucalyptus.network.PrivateIPResource
import com.eucalyptus.network.PrivateNetworkIndex
import com.eucalyptus.network.PrivateNetworkIndexResource
import com.eucalyptus.network.PublicIPResource
import com.eucalyptus.network.SecurityGroupResource
import com.eucalyptus.util.CollectionUtils
import com.eucalyptus.util.RestrictedTypes
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstance.Builder as VmInstanceBuilder
import com.eucalyptus.vm.VmNetworkConfig
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.cloud.VmInfo
import groovy.transform.CompileStatic

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

/**
 *
 */
@CompileStatic
class RunHelpers {

  static class DispatchingRunHelper {
    private final List<RunHelper> helpers = [
        new PrivateIPRunHelper( ),
        new PrivateNetworkIndexRunHelper( ),
        new PublicIPRunHelper( ),
        new SecurityGroupRunHelper( ),
    ] as List<RunHelper>

    @Delegate RunHelper instance = RunHelper.cast( java.lang.reflect.Proxy.newProxyInstance(
        RunHelper.getClassLoader( ),
        [ RunHelper ] as Class<?>[],
        { Object proxy, Method method, Object[] args ->
          helpers.each { method.invoke( it, args ) }
        } as InvocationHandler
    ) )
  }

  //TODO:STEVE: RunHelper implementation that that discovers/dispatches to underlying implementations
  static RunHelper getRunHelper( ) {
    new DispatchingRunHelper( )
  }

  static class RunHelperSupport implements RunHelper {
    @Override
    void verifyAllocation(
        final Allocations.Allocation allocation
    ) throws MetadataException {
    }

    @Override
    void prepareNetworkAllocation(
        final Allocations.Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
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
        final VmInstanceBuilder builder) {
    }

    @Override
    void prepareAllocation(
        final VmInstance instance,
        final Allocations.Allocation allocation ) {
    }

    @Override
    void prepareAllocation(
        final VmInfo vmInfo,
        final Allocations.Allocation allocation ) {
    }

    @Override
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance ) {
    }
  }

  private static final class PrivateIPRunHelper extends RunHelperSupport {
    @Override
    void prepareNetworkAllocation(
        final Allocations.Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      prepareNetworkResourcesType.getResources( ).addAll(
          allocation.allocationTokens.collect{ ResourceToken token ->
            new PrivateIPResource( ownerId: token.instanceId ) }
      )
    }
  }

  private static final class PrivateNetworkIndexRunHelper extends RunHelperSupport {
    @Override
    void prepareNetworkAllocation(
        final Allocations.Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
      allocation?.allocationTokens?.each{ ResourceToken token ->
        Collection<NetworkResource> resources = token.networkResources.findAll{ NetworkResource resource ->
          resource instanceof PrivateNetworkIndexResource && resource.ownerId != null }
        token.networkResources.removeAll( resources )
        prepareNetworkResourcesType.resources.addAll( resources )
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
        final Allocations.Allocation allocation ) {
      vmInfo?.netParams?.with {
        if ( vlan != null && networkIndex != null && networkIndex > -1 ) {
          allocation?.allocationTokens?.find{ final ResourceToken resourceToken ->
            resourceToken.instanceUuid == vmInfo.uuid
          }?.networkResources?.add( new PrivateNetworkIndexResource(
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

    private <V> V doWithPrivateNetworkIndex( final ResourceToken resourceToken,
                                             final Closure<V> closure ) {
      PrivateNetworkIndexResource resource = ( PrivateNetworkIndexResource ) \
          resourceToken.networkResources.find{ it instanceof PrivateNetworkIndexResource }
      resource?.with{
        closure.call( Integer.valueOf( tag ),  Long.valueOf( value ) )
      }
    }

  }

  private static final class PublicIPRunHelper extends RunHelperSupport {
    @Override
    void prepareNetworkAllocation(
        final Allocations.Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !allocation.usePrivateAddressing ) {
        allocation?.allocationTokens?.each{ ResourceToken token ->
          Collection<NetworkResource> resources =
              token.networkResources.findAll{ NetworkResource resource -> resource instanceof PublicIPResource }
          if ( resources.isEmpty( ) ) {
            resources = [ new PublicIPResource( ownerId: token.instanceId ) ] as Collection<NetworkResource>
          }
          token.networkResources.removeAll( resources )
          prepareNetworkResourcesType.resources.addAll( resources )
        }
      }
    }

    @Override
    void prepareAllocation(
        final VmInfo vmInfo,
        final Allocations.Allocation allocation ) {
      vmInfo?.netParams?.with {
        if ( !Strings.isNullOrEmpty( ignoredPublicIp ) != null && !VmNetworkConfig.DEFAULT_IP == ignoredPublicIp ) {
          allocation?.allocationTokens?.find{ final ResourceToken resourceToken ->
            resourceToken.instanceUuid == vmInfo.uuid
          }?.networkResources?.add( new PublicIPResource( ownerId: vmInfo.instanceId, value: ignoredPublicIp ) )
        }
        void
      }
    }
  }

  private static final class SecurityGroupRunHelper extends RunHelperSupport {
    @Override
    void verifyAllocation( final Allocations.Allocation allocation ) throws MetadataException {
      final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( )
      NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) )

      final Set<String> networkNames = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupNames( ) )
      final Set<String> networkIds = Sets.newLinkedHashSet( allocation.getRequest().securityGroupsIds() )
      if ( networkNames.isEmpty( ) && networkIds.isEmpty() ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) )
      }

      final List<NetworkGroup> groups = Lists.newArrayList( )
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName() ) ).isPresent() ) {
          groups.add( NetworkGroups.lookup( accountFullName, groupName ) )
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
        final Allocations.Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      //TODO:STEVE: how do we know if we are in EC2-Classic or VPC?
      //TODO:STEVE: should be "primary" only here? (check network service feature?)
      prepareNetworkResourcesType.getResources( ).addAll( allocation.networkGroups*.groupId?.
          collect( SecurityGroupResource.&forId ) ?: [ ] as List<SecurityGroupResource> )
    }
  }
}
