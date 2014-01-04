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
import com.eucalyptus.context.Context
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.Transactions
import com.eucalyptus.network.NetworkGroup
import com.eucalyptus.network.NetworkGroups
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
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
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

  private static final class PrivateNetworkIndexRunHelper extends RunHelperSupport{
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
    void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance ) {
      doWithPrivateNetworkIndex( resourceToken ){ Integer vlan, Long networkIndex ->
        Entities.transaction( PrivateNetworkIndex ) {
          Entities.uniqueResult( PrivateNetworkIndex.named( vlan, networkIndex ) ).with{
            set( instance );
            instance.setNetworkIndex( (PrivateNetworkIndex) getDelegate( ) );
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
        prepareNetworkResourcesType.getResources( ).addAll(
            allocation.allocationTokens.collect{ ResourceToken token ->
              new PublicIPResource( ownerId: token.instanceId ) }
        )
      }
    }
  }

  private static final class SecurityGroupRunHelper extends RunHelperSupport {
    @Override
    void verifyAllocation( final Allocations.Allocation allocation ) throws MetadataException {
      final Context ctx = allocation.getContext( )
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName()
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
          throw new IllegalMetadataAccessException( "Not authorized to use network group " +group.getGroupId()+ "/" + group.getDisplayName() + " for " + ctx.getUser( ).getName( ) )
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
