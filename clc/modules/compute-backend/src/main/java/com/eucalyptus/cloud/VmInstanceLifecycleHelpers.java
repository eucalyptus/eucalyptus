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
package com.eucalyptus.cloud;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.springframework.core.OrderComparator;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.Addresses.AddressingBatch;
import com.eucalyptus.auth.AuthContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.common.ResourceToken;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.VmRunType.Builder;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemRequestType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.NoSuchGroupMetadataException;
import com.eucalyptus.compute.common.internal.util.IllegalMetadataAccessException;
import com.eucalyptus.compute.common.internal.util.InvalidMetadataException;
import com.eucalyptus.compute.common.internal.util.InvalidParameterCombinationMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.util.SecurityGroupLimitMetadataException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment.Status;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Subnets;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.compute.common.network.PrivateIPResource;
import com.eucalyptus.compute.common.network.PublicIPResource;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.compute.common.network.SecurityGroupResource;
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource;
import com.eucalyptus.compute.vpc.NetworkInterfaceHelper;
import com.eucalyptus.compute.vpc.NetworkInterfaceInUseMetadataException;
import com.eucalyptus.compute.vpc.NoSuchNetworkInterfaceMetadataException;
import com.eucalyptus.compute.vpc.NoSuchSubnetMetadataException;
import com.eucalyptus.compute.vpc.NotEnoughPrivateAddressResourcesException;
import com.eucalyptus.compute.vpc.PrivateAddressResourceAllocationException;
import com.eucalyptus.compute.vpc.VpcConfiguration;
import com.eucalyptus.compute.vpc.VpcRequiredMetadataException;
import com.eucalyptus.compute.vpc.persist.PersistenceNetworkInterfaces;
import com.eucalyptus.compute.vpc.persist.PersistenceSubnets;
import com.eucalyptus.compute.vpc.persist.PersistenceVpcs;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.PrivateAddresses;
import com.eucalyptus.network.PublicAddresses;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.Ordered;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.util.TypedKey;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;

/**
 *
 */
@SuppressWarnings( "UnnecessaryQualifiedReference" )
public class VmInstanceLifecycleHelpers {
  private static final ReadWriteLock helpersLock = new ReentrantReadWriteLock( );
  private static final AtomicReference<List<VmInstanceLifecycleHelper>> helpers =
      new AtomicReference<List<VmInstanceLifecycleHelper>>( Lists.newArrayList( ) );
  private static final VmInstanceLifecycleHelper dispatchingHelper = vmInstanceLifecycleHelperProxy(
      ( Object proxy, Method method, Object[] args ) -> {
        final List<VmInstanceLifecycleHelper> helpers =
            LockResource.withLock( helpersLock.readLock( ), () -> VmInstanceLifecycleHelpers.helpers.get( ) );
        for ( final VmInstanceLifecycleHelper helper : helpers ) try {
          method.invoke( helper, args );
        } catch ( InvocationTargetException e ) {
          throw e.getTargetException( );
        }
        return null;
      } );

  static void register( final VmInstanceLifecycleHelper helper ) {
    LockResource.withLock( helpersLock.writeLock( ), () -> {
        List<VmInstanceLifecycleHelper> helpers = Lists.newArrayList( VmInstanceLifecycleHelpers.helpers.get( ) );
        helpers.add( helper );
        helpers.sort( new OrderComparator( ) );
        VmInstanceLifecycleHelpers.helpers.set( ImmutableList.copyOf( helpers ) );
        return null;
    } );
  }

  private static VmInstanceLifecycleHelper vmInstanceLifecycleHelperProxy( final InvocationHandler handler ) {
    return (VmInstanceLifecycleHelper) Proxy.newProxyInstance(
        VmInstanceLifecycleHelpers.class.getClassLoader( ),
        new Class[]{ VmInstanceLifecycleHelper.class },
        handler );
  }

  public static VmInstanceLifecycleHelper get( ) {
    return dispatchingHelper;
  }

  public static abstract class VmInstanceLifecycleHelperSupport implements Ordered, VmInstanceLifecycleHelper {
    public static final int DEFAULT_ORDER = 0;

    @Override
    public int getOrder( ) {
      return DEFAULT_ORDER;
    }

    @Override
    public void verifyAllocation( final Allocation allocation ) throws MetadataException {
    }

    @Override
    public void prepareNetworkAllocation( final Allocation allocation, final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
    }

    @Override
    public void verifyNetworkAllocation( final Allocation allocation, final PrepareNetworkResourcesResultType prepareNetworkResourcesResultType ) {
    }

    @Override
    public void prepareVmRunType( final ResourceToken resourceToken, final Builder builder ) {
    }

    @Override
    public void prepareVmInstance( final ResourceToken resourceToken, final VmInstances.Builder builder ) {
    }

    @Override
    public void prepareAllocation( final VmInstance instance, final Allocation allocation ) {
    }

    @Override
    public void startVmInstance( final ResourceToken resourceToken, final VmInstance instance ) {
    }

    @Override
    public void cleanUpInstance( final VmInstance instance, final VmState state ) {
    }
  }

  public static abstract class NetworkResourceVmInstanceLifecycleHelper extends VmInstanceLifecycleHelperSupport {

    public static boolean prepareFromTokenResources(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType,
        final Class<? extends NetworkResource> resourceClass
    ) {
      final List<NetworkResource> resources = Stream.ofAll( allocation.getAllocationTokens( ) )
          .flatMap( resourceToken -> resourceToken.getAttribute( NetworkResourcesKey ) )
          .filter( resourceClass::isInstance )
          .toJavaList( );

      for ( ResourceToken resourceToken : allocation.getAllocationTokens( ) ) {
        resourceToken.getAttribute( NetworkResourcesKey ).removeAll( resources );
      }

      prepareNetworkResourcesType.getResources( ).addAll( resources );

      return !resources.isEmpty( );
    }

    @SuppressWarnings( "UnnecessaryQualifiedReference" )
    public static Address getAddress( final ResourceToken token ) {
      final PublicIPResource publicIPResource = (PublicIPResource) Iterables.find(
          token.getAttribute( NetworkResourcesKey ),
          Predicates.instanceOf( PublicIPResource.class ),
          null );
      return publicIPResource != null && publicIPResource.getValue( ) != null ?
          Addresses.getInstance( ).lookupActiveAddress( publicIPResource.getValue( ) ) :
          null;
    }

    public static void withBatch( final Runnable runnable ) {
      final AddressingBatch batch = Addresses.getInstance( ).batch( );
      try {
        runnable.run( );
      } finally {
        batch.close( );
      }
    }

    @Nullable
    public static InstanceNetworkInterfaceSetItemRequestType getPrimaryNetworkInterface(
        final RunInstancesType runInstancesType
    ) {
      if ( runInstancesType != null &&
          runInstancesType.getNetworkInterfaceSet( ) != null &&
          runInstancesType.getNetworkInterfaceSet( ).getItem( ) != null ) {
        for ( InstanceNetworkInterfaceSetItemRequestType item : runInstancesType.getNetworkInterfaceSet( ).getItem( ) ) {
          if ( item.getDeviceIndex( ) != null && item.getDeviceIndex( ) == 0 ) {
            return item;
          }
        }
      }
      return null;
    }

    @Nullable
    public static Iterable<InstanceNetworkInterfaceSetItemRequestType> getSecondaryNetworkInterfaces(
        final RunInstancesType runInstancesType
    ) {
      final List<InstanceNetworkInterfaceSetItemRequestType> interfaces = Lists.newArrayList( );
      final InstanceNetworkInterfaceSetItemRequestType primary = getPrimaryNetworkInterface( runInstancesType );
      if ( runInstancesType != null && runInstancesType.getNetworkInterfaceSet( ) != null &&
          runInstancesType.getNetworkInterfaceSet( ).getItem( ) != null ) {
        interfaces.addAll( runInstancesType.getNetworkInterfaceSet( ).getItem( ) );
        if ( primary != null ) {
          interfaces.remove( primary );// check against primary in case multiple interfaces with device index 0
        }
      }
      return interfaces;
    }

    @Nullable
    public static String getPrimaryPrivateIp(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface,
        final String privateIp
    ) {
      String primaryPrivateIp = null;
      if ( instanceNetworkInterface != null &&
          instanceNetworkInterface.getPrivateIpAddressesSet( ) != null &&
          instanceNetworkInterface.getPrivateIpAddressesSet( ).getItem( ) != null ) {
        for ( final PrivateIpAddressesSetItemRequestType item :
            instanceNetworkInterface.getPrivateIpAddressesSet( ).getItem( ) ) {
          if ( item.getPrimary( ) != null && item.getPrimary( ) ) {
            primaryPrivateIp = item.getPrivateIpAddress( );
            break;
          }
        }
      }
      if ( primaryPrivateIp == null && instanceNetworkInterface != null ) {
        primaryPrivateIp = instanceNetworkInterface.getPrivateIpAddress( );
      }
      if ( primaryPrivateIp == null ) {
        primaryPrivateIp = Strings.emptyToNull( privateIp );
      }
      return primaryPrivateIp;
    }

    @Nullable
    protected static Set<String> getSecurityGroupIds(
        final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface
    ) {
      return normalizeIdentifiers(
          instanceNetworkInterface == null || instanceNetworkInterface.getGroupSet( ) == null ?
              null :
              instanceNetworkInterface.getGroupSet( ).groupIds( ) );
    }

    @Nullable
    public static String getDefaultVpcId( final AccountFullName account, final Allocation allocation ) {
      String defaultVpcId = allocation.getAttribute( DefaultVpcIdKey );
      if ( defaultVpcId == null ) {
        Vpcs vpcs = new PersistenceVpcs( );
        try {
          defaultVpcId = vpcs.lookupDefault( account, CloudMetadatas.toDisplayName( ) );
        } catch ( VpcMetadataNotFoundException e ) {
          defaultVpcId = "";
        } catch ( VpcMetadataException e ) {
          throw Exceptions.toUndeclared( "Error looking up default vpc", e );
        }

        allocation.setAttribute( DefaultVpcIdKey, defaultVpcId );
      }

      return Strings.emptyToNull( defaultVpcId );
    }

    public static String normalizeIdentifier( final String identifier ) {
      return ResourceIdentifiers.tryNormalize( ).apply( identifier );
    }

    @Nonnull
    public static Set<String> normalizeIdentifiers( @Nullable final Iterable<String> identifiers ) {
      final Set<String> normalizedIdentifiers = Sets.newLinkedHashSet( );
      if ( identifiers != null ) {
        Iterables.addAll(
            normalizedIdentifiers,
            Iterables.transform( identifiers, ResourceIdentifiers.tryNormalize( ) ) );
      }
      return normalizedIdentifiers;
    }

    @Nonnull
    public static <T extends RestrictedType> T lookup(
        final String account,
        final String typeDesc,
        final String id,
        final Class<T> type
    ) throws IllegalMetadataAccessException {
      final T resource = RestrictedTypes.resolver( type ).apply( normalizeIdentifier( id ) );
      if ( !RestrictedTypes.filterPrivileged( ).apply( resource ) ||
          ( account != null && !Objects.equals( account, resource.getOwner( ).getAccountNumber( ) ) ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use " + typeDesc + " " + id );
      }
      return resource;
    }
  }

  public final static class PrivateIPVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final Logger logger = Logger.getLogger( PrivateIPVmInstanceLifecycleHelper.class );

    @Override
    public void prepareNetworkAllocation( final Allocation allocation, final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, PrivateIPResource.class ) ) {
        prepareNetworkResourcesType.getResources( ).addAll(
            Stream.ofAll( allocation.getAllocationTokens( ) )
                .map( token -> new PrivateIPResource( token.getInstanceId( ), null, null ) )
                .toJavaList( ) );
      }
    }

    @Override
    public void prepareVmRunType( final ResourceToken resourceToken, final Builder builder ) {
      final PrivateIPResource resource = (PrivateIPResource) Stream.ofAll( resourceToken.getAttribute(NetworkResourcesKey) )
          .filter( PrivateIPResource.class::isInstance )
          .headOption( ).getOrNull( );
      if ( resource != null ) {
        builder.macAddress( resource.getMac( ) );
        builder.privateAddress( resource.getValue( ) );
      }
    }

    @Override
    public void prepareVmInstance(
        final ResourceToken resourceToken,
        final VmInstances.Builder builder
    ) {
      final PrivateIPResource resource = (PrivateIPResource) Stream.ofAll( resourceToken.getAttribute(NetworkResourcesKey) )
          .filter( PrivateIPResource.class::isInstance )
          .headOption( ).getOrNull( );
      if ( resource != null ) {
        builder.onBuild( instance -> {
          instance.updateMacAddress( resource.getMac( ) );
          VmInstances.updatePrivateAddress( instance, resource.getValue( ) );
          if ( instance.getVpcId( ) == null ) {
            try {
              PrivateAddresses.associate( resource.getValue( ), instance );
            } catch ( ResourceAllocationException e ) {
              logger.error( "Unable to associate private address "+resource.getValue( )+" when preparing instance " + resource.getOwnerId( ), e );
            }
          }
        } );
      }
    }

    @Override
    public void startVmInstance(
        final ResourceToken resourceToken,
        final VmInstance instance
    ) {
      final PrivateIPResource resource = (PrivateIPResource) Stream.ofAll( resourceToken.getAttribute(NetworkResourcesKey) )
          .filter( PrivateIPResource.class::isInstance )
          .headOption( ).getOrNull( );
      if ( resource != null ) {
        instance.updateMacAddress( resource.getMac( ) );
        VmInstances.updatePrivateAddress( instance, resource.getValue( ) );
        if ( instance.getVpcId( ) == null ) {
          try {
            PrivateAddresses.associate( resource.getValue( ), instance );
          } catch ( ResourceAllocationException e ) {
            logger.error( "Unable to associate private address "+resource.getValue( )+" when starting instance " + resource.getOwnerId( ), e );
          }
        }
      }
    }

    @Override
    public void cleanUpInstance( final VmInstance instance, final VmState state ) {
      if ( VmStateSet.TORNDOWN.contains( state ) ) try {
        if ( instance.getVpcId( ) == null &&
            !Strings.isNullOrEmpty( instance.getPrivateAddress( ) ) &&
            !VmNetworkConfig.DEFAULT_IP.equals( instance.getPrivateAddress( ) ) ) {
          Networking.getInstance( ).release( new ReleaseNetworkResourcesType(
              null,
              Lists.newArrayList( new PrivateIPResource( instance.getDisplayName( ), instance.getPrivateAddress( ), null ) )
          ) );
          VmInstances.updatePrivateAddress( instance, VmNetworkConfig.DEFAULT_IP );
        }
      } catch ( final Exception ex ) {
        logger.error( "Error releasing private address \'" + instance.getPrivateAddress( ) + "\' on instance \'" + instance.getDisplayName( ) + "\' clean up.", ex );
      }
    }
  }

  public final static class PublicIPVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {

    @Override
    public void prepareNetworkAllocation( final Allocation allocation, final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
      final Subnet subnet = allocation.getSubnet( );
      final Vpc vpc = ( subnet == null ? null : subnet.getVpc( ) );
      final String vpcId = ( vpc == null ? null : vpc.getDisplayName( ) );
      final Boolean isVpc = vpcId != null;
      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface =
          getPrimaryNetworkInterface( ( (RunInstancesType) allocation.getRequest( ) ) );
      final Boolean requestedAllocation = ( instanceNetworkInterface == null ?
          null :
          instanceNetworkInterface.getAssociatePublicIpAddress( ) );
      if ( ( isVpc &&
          ( ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getNetworkInterfaceId( ) ) == null ) &&
          MoreObjects.firstNonNull( requestedAllocation, allocation.getSubnet( ).getMapPublicIpOnLaunch( ) ) ) ||
          ( !isVpc && !allocation.isUsePrivateAddressing( ) ) ) {
        if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, PublicIPResource.class ) ) {
          for ( final VmInstanceToken token : allocation.getAllocationTokens( ) ) {
            List<PublicIPResource> resources = Stream.ofAll( token.getAttribute( NetworkResourcesKey ) )
                .filter( PublicIPResource.class::isInstance )
                .map( PublicIPResource.class::cast )
                .toJavaList( );
            if ( resources.isEmpty( ) ) {
              resources = Lists.newArrayList( new PublicIPResource( token.getInstanceId( ), null ) );
            }
            token.getAttribute( NetworkResourcesKey ).removeAll( resources );
            prepareNetworkResourcesType.getResources( ).addAll( resources );
          }
        }
      } else if ( isVpc ) {
        allocation.setUsePrivateAddressing( true );
      }
    }

    @SuppressWarnings( "UnnecessaryQualifiedReference" )
    @Override
    public void prepareVmInstance( final ResourceToken resourceToken, final VmInstances.Builder builder ) {
      final Address address = getAddress( resourceToken );
      if ( address != null ) {
        builder.onBuild( instance -> {
            if ( instance.getVpcId( ) == null ) {// Network interface handles public IP for VPC
              withBatch( ( ) -> {
                  Addresses.getInstance( ).assign( address, instance );
                  AddressingBatch.reset( );// Flush after running
              } );
              VmInstances.updatePublicAddress( instance, address.getAddress( ) );
            }
        } );
      }
    }

    @Override
    public void startVmInstance( final ResourceToken resourceToken, final VmInstance instance ) {
      Address address = getAddress( resourceToken );
      if ( address != null && instance.getVpcId( ) == null ) {
        Addresses.getInstance( ).assign( address, instance );
        VmInstances.updatePublicAddress( instance, address.getAddress( ) );
      }
    }

    @Override
    public void cleanUpInstance( final VmInstance instance, final VmState state ) {
      // For EC2-Classic unassign any public/elastic IP
      if ( instance.getVpcId( ) == null &&
          !( VmNetworkConfig.DEFAULT_IP.equals( instance.getPublicAddress( ) ) ||
              Strings.isNullOrEmpty( instance.getPublicAddress( ) ) ) &&
          VmStateSet.TORNDOWN.contains( state ) ) {
        if ( Addresses.getInstance( ).unassign( instance.getPublicAddress( ) ) ) {
          VmInstances.updatePublicAddress( instance, VmNetworkConfig.DEFAULT_IP );
          PublicAddresses.markDirty( instance.getPublicAddress( ), instance.getPartition( ) );
        }
      }
    }
  }

  public final static class SecurityGroupVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final Logger logger = Logger.getLogger( SecurityGroupVmInstanceLifecycleHelper.class );

    @Override
    public void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( );

      final Set<String> networkNames = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupNames( ) );
      final Set<String> networkIds = Sets.newLinkedHashSet( allocation.getRequest( ).securityGroupsIds( ) );
      final String defaultVpcId = getDefaultVpcId( accountFullName, allocation );
      final String vpcId = allocation.getSubnet( ) != null ? allocation.getSubnet( ).getVpc( ).getDisplayName( ) : defaultVpcId;
      final boolean isVpc = vpcId != null;
      if ( networkNames.isEmpty( ) && networkIds.isEmpty( ) ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) );
      }
      if ( !isVpc && networkNames.contains( NetworkGroups.defaultNetworkName( ) ) ) try {
        // Use separate thread so group is committed immediately
        Threads.enqueue( Eucalyptus.class, VmInstanceLifecycleHelper.class, 5, () ->
            NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) )
        ).get( );
      } catch ( final Exception e ) {
        logger.trace( "Error looking up default network", e );
      }

      final List<NetworkGroup> groups = Lists.newArrayList( );
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName( ) ) ).isPresent( ) ) {
          if ( !isVpc ) {
            groups.add( NetworkGroups.lookup( accountFullName, groupName ) );
          } else if ( defaultVpcId != null && defaultVpcId.equals( vpcId ) ) {
            groups.add( NetworkGroups.lookup( null, defaultVpcId, groupName ) );
          } else if ( groupName.equals( NetworkGroups.defaultNetworkName( ) ) ) {
            groups.add( NetworkGroups.lookupDefault( null, vpcId ) );
          }
        }
      }

      final AuthContext authContext;
      try {
        authContext = allocation.getAuthContext( ).get( );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( "Error accessing auth context for allocation" );
      }
      for ( String groupId : networkIds ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId( ) ) ).isPresent( ) ) {
          groups.add( NetworkGroups.lookupByGroupId( authContext.isSystemUser( ) ? null : accountFullName, groupId ) );
        }
      }

      if ( !Collections.singleton( vpcId ).equals( Sets.newHashSet( Iterables.transform( groups, NetworkGroup.vpcId( ) ) ) ) ) {
        throw new InvalidMetadataException( "Invalid security groups (inconsistent VPC)" );
      }

      final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( );
      for ( final NetworkGroup group : groups ) {
        if ( !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " + group.getGroupId( ) + "/" + group.getDisplayName( ) + " for " + allocation.getOwnerFullName( ).getUserName( ) );
        }
        networkRuleGroups.put( group.getDisplayName( ), group );
      }


      if ( allocation.getNetworkGroups( ).isEmpty( ) ) {
        allocation.setNetworkRules( networkRuleGroups );
      }
    }

    @Override
    public void prepareNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesType prepareNetworkResourcesType
    ) {
      if ( !prepareFromTokenResources( allocation, prepareNetworkResourcesType, SecurityGroupResource.class ) ) {
        prepareNetworkResourcesType.getResources( ).addAll(
            Stream.ofAll( allocation.getNetworkGroups( ) )
                .map( NetworkGroup::getGroupId )
                .map( SecurityGroupResource::forId )
                .toJavaList( )  );
      }
    }

    @Override
    public void cleanUpInstance( final VmInstance instance, final VmState state ) {
      if ( VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
        instance.getNetworkGroups( ).clear( );
      }
    }
  }

  /**
   * For network interface, including mac address and private IP address
   */
  public final static class VpcNetworkInterfaceVmInstanceLifecycleHelper extends NetworkResourceVmInstanceLifecycleHelper {
    private static final NetworkInterfaces networkInterfaces = new PersistenceNetworkInterfaces( );

    private static final Subnets subnets = new PersistenceSubnets( );

    private static final TypedKey<List<InstanceNetworkInterfaceSetItemRequestType>> secondaryNetworkInterfacesKey =
        TypedKey.create( "SecondaryNetworkInterfaces", ArrayList::new );

    /**
     * This helper has a higher precedence to ensure the zone / subnet are initialized prior to other lifecycle helpers using them
     */
    @Override
    public int getOrder( ) {
      return -100;
    }

    @Override
    public void prepareAllocation( final VmInstance instance, final Allocation allocation ) {
      if ( instance.getVpcId( ) != null ) {
        allocation.getRunInstancesRequest( ).setNetworkInterfaceSet( new InstanceNetworkInterfaceSetRequestType(
            new InstanceNetworkInterfaceSetItemRequestType(
                0,
                instance.getNetworkInterfaces( ).get( 0 ).getDisplayName( ),
                false )
        ) );
        allocation.setSubnet( instance.getBootRecord( ).getSubnet( ) );

        final Partition partition = Partitions.lookupByName( allocation.getSubnet( ).getAvailabilityZone( ) );
        allocation.setPartition( partition );
      }
    }

    @Override
    public void verifyAllocation( final Allocation allocation ) throws MetadataException {
      final RunInstancesType runInstances = (RunInstancesType) allocation.getRequest( );
      final boolean allowMultiVpc = Principals.systemUser( ).getName( ).equals( runInstances.getEffectiveUserId( ) );

      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface = getPrimaryNetworkInterface( runInstances );
      final Iterable<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces = getSecondaryNetworkInterfaces( runInstances );
      final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.getPrivateIpAddress( ) );
      final String id = ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getSubnetId( ) );
      final String subnetId = normalizeIdentifier( !Strings.isNullOrEmpty( id ) ? id : runInstances.getSubnetId( ) );
      final Set<String> networkIds = getSecurityGroupIds( instanceNetworkInterface );
      final Set<NetworkingFeature> networkingFeatures = Networking.getInstance( ).describeFeatures( );
      //noinspection GrDeprecatedAPIUsage
      final String requiredResourceAccountNumber =
          !allocation.getContext( ).isPrivileged( ) ? allocation.getOwnerFullName( ).getAccountNumber( ) : null;
      if ( !Strings.isNullOrEmpty( subnetId ) || instanceNetworkInterface != null || !Iterables.isEmpty( secondaryNetworkInterfaces ) ) {
        if ( !networkingFeatures.contains( NetworkingFeature.Vpc ) ) {
          throw new InvalidMetadataException( "EC2-VPC not supported, for EC2-Classic do not specify subnet or network interface" );
        }

        if ( runInstances.getAddressingType( ) != null ) {
          throw new InvalidMetadataException( "Addressing scheme not supported in EC2-VPC" );
        }

        if ( instanceNetworkInterface != null ) {
          if ( !runInstances.securityGroupNames( ).isEmpty( ) || !runInstances.securityGroupsIds( ).isEmpty( ) ) {
            throw new InvalidParameterCombinationMetadataException( "Network interfaces and an instance-level security groups may not be specified on the same request" );
          }


          if ( !Strings.isNullOrEmpty( runInstances.getSubnetId( ) ) ) {
            throw new InvalidParameterCombinationMetadataException( "Network interfaces and an instance-level subnet ID may not be specified on the same request" );
          }


          if ( !Strings.isNullOrEmpty( runInstances.getPrivateIpAddress( ) ) ) {
            throw new InvalidParameterCombinationMetadataException( "Network interfaces and an instance-level private address may not be specified on the same request" );
          }

          if ( MoreObjects.firstNonNull( instanceNetworkInterface.getAssociatePublicIpAddress( ), false ) &&
              !Iterables.isEmpty( secondaryNetworkInterfaces ) ) {
            throw new InvalidParameterCombinationMetadataException( "The associatePublicIPAddress parameter cannot be specified when launching with multiple network interfaces." );
          }
        } else if ( !Iterables.isEmpty( secondaryNetworkInterfaces ) ) {
          throw new InvalidParameterCombinationMetadataException( "Primary network interface required when secondary network interface(s) specified" );
        }

        String networkInterfaceSubnetId = null;
        String networkInterfaceAvailabilityZone = null;
        if ( ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getNetworkInterfaceId( ) ) != null ) {
          if ( runInstances.getMinCount( ) > 1 || runInstances.getMaxCount( ) > 1 ) {
            throw new InvalidMetadataException( "Network interface can only be specified for a single instance" );
          }

          try ( TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
            final NetworkInterface networkInterface;
            try {
              networkInterface = lookup(
                  requiredResourceAccountNumber,
                  "network interface",
                  instanceNetworkInterface.getNetworkInterfaceId( ),
                  NetworkInterface.class );
            } catch ( Exception e ) {
              throw new NoSuchNetworkInterfaceMetadataException( "Network interface (" + instanceNetworkInterface.getNetworkInterfaceId( ) + ") not found", e );
            }

            if ( networkInterface.isAttached( ) ) {
              throw new NetworkInterfaceInUseMetadataException( "Network interface (" + instanceNetworkInterface.getNetworkInterfaceId( ) + ") in use" );
            }

            networkInterfaceSubnetId = networkInterface.getSubnet( ).getDisplayName( );
            networkInterfaceAvailabilityZone = networkInterface.getAvailabilityZone( );
          }
        }

        final String resolveSubnetId = subnetId != null ? subnetId : networkInterfaceSubnetId;
        if ( resolveSubnetId == null ) throw new InvalidMetadataException( "SubnetId required" );
        Subnet subnet;
        try {
          subnet = lookup( requiredResourceAccountNumber, "subnet", resolveSubnetId, Subnet.class );
        } catch ( NoSuchElementException e ) {
          throw new NoSuchSubnetMetadataException( "Subnet (" + resolveSubnetId + ") not found", e );
        }

        if ( privateIp != null ) {
          if ( runInstances.getMinCount( ) > 1 || runInstances.getMaxCount( ) > 1 ) {
            throw new InvalidMetadataException( "Private address can only be specified for a single instance" );
          }

          if ( !validPrivateIpForCidr( subnet.getCidr( ), privateIp ) ) {
            throw new InvalidMetadataException( "Private address " + privateIp + " not valid for subnet " + subnetId + " cidr " + subnet.getCidr( ) );
          }
        }

        if ( networkInterfaceAvailabilityZone != null &&
            !networkInterfaceAvailabilityZone.equals( subnet.getAvailabilityZone( ) ) ) {
          throw new InvalidMetadataException( "Network interface availability zone (" + networkInterfaceAvailabilityZone + ") not valid for subnet " + subnetId + " zone " + subnet.getAvailabilityZone( ) );
        }

        final Set<NetworkGroup> groups = Sets.newHashSet( );
        for ( String groupId : networkIds ) {
          if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId( ) ) ).isPresent( ) )
            try {
              groups.add( lookup( requiredResourceAccountNumber, "security group", groupId, NetworkGroup.class ) );
            } catch ( Exception e ) {
              throw new NoSuchGroupMetadataException( "Security group (" + groupId + ") not found", e );
            }
        }

        if ( !groups.isEmpty( ) && !Collections.singleton( subnet.getVpc( ).getDisplayName( ) ).equals( Sets.newHashSet( Iterables.transform( groups, NetworkGroup.vpcId( ) ) ) ) ) {
          throw new InvalidMetadataException( "Invalid security groups (inconsistent VPC)" );
        }
        if ( groups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
          throw new SecurityGroupLimitMetadataException( );
        }

        if ( !Iterables.isEmpty( secondaryNetworkInterfaces ) ) {
          try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
            final Integer interfaces = allocation.getVmType( ).getNetworkInterfaces( );
            final int maxInterfaces = interfaces!=null && interfaces > 0 ? interfaces : 1;
            final Set<Integer> deviceIndexes = Sets.newHashSet( 0 );
            final Set<String> networkInterfaceIds = Sets.newHashSet( );
            final Set<Pair<String, String>> subnetPrivateAddresses = Sets.newHashSet( );
            if ( ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getNetworkInterfaceId( ) ) != null ) {
              networkInterfaceIds.add( normalizeIdentifier( instanceNetworkInterface.getNetworkInterfaceId( ) ) );
            }

            if ( privateIp != null ) {
              subnetPrivateAddresses.add( Pair.pair( subnet.getDisplayName( ), privateIp ) );
            }

            for ( final InstanceNetworkInterfaceSetItemRequestType networkInterfaceItem : secondaryNetworkInterfaces ) {
              if ( networkInterfaceItem.getNetworkInterfaceId( ) != null &&
                  ( runInstances.getMinCount( ) > 1 || runInstances.getMaxCount( ) > 1 ) ) {
                throw new InvalidMetadataException( "Network interface can only be specified for a single instance" );
              }

              final Integer networkInterfaceDeviceIndex = networkInterfaceItem.getDeviceIndex( );
              if ( networkInterfaceDeviceIndex == null ) {
                throw new InvalidMetadataException( "Network interface device index required" );
              } else if ( !deviceIndexes.add( networkInterfaceDeviceIndex ) ) {
                throw new InvalidMetadataException( "Network interface duplicate device index (" + String.valueOf( networkInterfaceDeviceIndex ) + ")" );
              } else if ( networkInterfaceDeviceIndex < 1 || networkInterfaceDeviceIndex > 31 ) {
                throw new InvalidMetadataException( "Network interface device index invalid " + String.valueOf( networkInterfaceDeviceIndex ) );
              }


              Subnet secondarySubnet = null;
              if ( networkInterfaceItem.getNetworkInterfaceId( ) != null ) try {
                final String secondaryNetworkInterfaceId =
                    normalizeIdentifier( networkInterfaceItem.getNetworkInterfaceId( ) );
                if ( !networkInterfaceIds.add( secondaryNetworkInterfaceId ) ) {
                  throw new InvalidMetadataException( "Network interface duplicate (" + networkInterfaceItem.getNetworkInterfaceId( ) + ")" );
                }

                final NetworkInterface secondaryNetworkInterface = lookup(
                    requiredResourceAccountNumber,
                    "network interface",
                    secondaryNetworkInterfaceId,
                    NetworkInterface.class );
                if ( secondaryNetworkInterface.isAttached( ) ) {
                  throw new NetworkInterfaceInUseMetadataException( "Network interface (" + networkInterfaceItem.getNetworkInterfaceId( ) + ") in use" );
                }

                secondarySubnet = secondaryNetworkInterface.getSubnet( );
              } catch ( InvalidMetadataException e ) {
                throw e;
              } catch ( Exception e ) {
                throw new NoSuchNetworkInterfaceMetadataException( "Network interface (" + networkInterfaceItem.getNetworkInterfaceId( ) + ") not found", e );
              }

              if ( secondarySubnet == null ) try {
                secondarySubnet = lookup(
                    requiredResourceAccountNumber,
                    "subnet",
                    networkInterfaceItem.getSubnetId( ),
                    Subnet.class );
              } catch ( Exception e ) {
                throw new NoSuchSubnetMetadataException( "Subnet (" + networkInterfaceItem.getSubnetId( ) + ") not found", e );
              }

              final String secondaryPrivateIp = getPrimaryPrivateIp( networkInterfaceItem, null );
              if ( secondaryPrivateIp != null ) {
                if ( runInstances.getMinCount( ) > 1 || runInstances.getMaxCount( ) > 1 ) {
                  throw new InvalidMetadataException( "Private address can only be specified for a single instance" );
                }

                if ( !validPrivateIpForCidr( secondarySubnet.getCidr( ), secondaryPrivateIp ) ) {
                  throw new InvalidMetadataException( "Private address " + secondaryPrivateIp + " not valid for subnet " + secondarySubnet.getDisplayName( ) + " cidr " + secondarySubnet.getCidr( ) );
                }

                if ( !subnetPrivateAddresses.add( Pair.pair( secondarySubnet.getDisplayName( ), secondaryPrivateIp ) ) ) {
                  throw new InvalidMetadataException( "Network interface duplicate private address (" + secondaryPrivateIp + ")" );
                }
              }

              if ( !allowMultiVpc && !subnet.getVpc( ).getDisplayName( ).equals( secondarySubnet.getVpc( ).getDisplayName( ) ) ) {
                throw new InvalidMetadataException( "Network interface vpc (" + secondarySubnet.getVpc( ).getDisplayName( ) + ") for " + secondarySubnet.getDisplayName( ) + " not valid for instance vpc " + subnet.getVpc( ).getDisplayName( ) );
              }

              if ( !secondarySubnet.getAvailabilityZone( ).equals( subnet.getAvailabilityZone( ) ) ) {
                throw new InvalidMetadataException( "Network interface availability zone (" + secondarySubnet.getAvailabilityZone( ) + ") for " + secondarySubnet.getDisplayName( ) + " not valid for subnet " + subnet.getDisplayName( ) + " zone " + subnet.getAvailabilityZone( ) );
              }

              final Set<String> secondaryNetworkIds = getSecurityGroupIds( networkInterfaceItem );
              final Set<NetworkGroup> secondaryGroups = Sets.newHashSet( );
              for ( String groupId : secondaryNetworkIds ) {
                if ( !Iterables.tryFind( secondaryGroups, CollectionUtils.propertyPredicate( groupId, NetworkGroup.groupId( ) ) ).isPresent( ) )
                  try {
                    secondaryGroups.add( lookup( requiredResourceAccountNumber, "security group", groupId, NetworkGroup.class ) );
                  } catch ( Exception e ) {
                    throw new NoSuchGroupMetadataException( "Security group (" + groupId + ") not found", e );
                  }
              }

              if ( !secondaryGroups.isEmpty( ) &&
                  !Collections.singleton( secondarySubnet.getVpc( ).getDisplayName( ) )
                      .equals( Sets.newHashSet( Iterables.transform( secondaryGroups, NetworkGroup.vpcId( ) ) ) ) ) {
                throw new InvalidMetadataException( "Invalid security groups for device " + String.valueOf( networkInterfaceItem.getDeviceIndex( ) ) + " (inconsistent VPC)" );
              }

              if ( secondaryGroups.size( ) > VpcConfiguration.getSecurityGroupsPerNetworkInterface( ) ) {
                throw new SecurityGroupLimitMetadataException( );
              }
            }

            if ( deviceIndexes.size( ) > maxInterfaces ) {
              final VmType type = allocation.getVmType( );
              throw new InvalidMetadataException( "Interface count " + String.valueOf( deviceIndexes.size( ) ) + " exceeds the limit for " + ( type == null ? null : type.getName( ) ) );
            }
          }
        }

        allocation.setSubnet( subnet );

        final Partition partition = Partitions.lookupByName( subnet.getAvailabilityZone( ) );
        allocation.setPartition( partition );

        final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( );
        for ( final NetworkGroup networkGroup : groups ) {
          networkRuleGroups.put( networkGroup.getDisplayName( ), networkGroup );
        }
        if ( !networkRuleGroups.isEmpty( ) ) {
          allocation.setNetworkRules( networkRuleGroups );
        }

        allocation.setAttribute( secondaryNetworkInterfacesKey, ImmutableList.copyOf( secondaryNetworkInterfaces ) );
      } else {
        // Default VPC, lookup subnet for user specified or system selected partition
        final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( );
        final String defaultVpcId = getDefaultVpcId( accountFullName, allocation );
        if ( defaultVpcId != null && !networkingFeatures.contains( NetworkingFeature.Vpc ) ) {
          throw new InvalidMetadataException( "EC2-VPC not supported, delete default VPC to run in EC2-Classic" );
        } else if ( defaultVpcId == null && !networkingFeatures.contains( NetworkingFeature.Classic ) ) {
          throw new VpcRequiredMetadataException( );
        } else if ( runInstances.getAddressingType( ) != null && !networkingFeatures.contains( NetworkingFeature.Classic ) ) {
          throw new InvalidMetadataException( "Addressing scheme not supported in EC2-VPC" );
        }
      }
    }

    @Override
    public void prepareNetworkAllocation( final Allocation allocation, final PrepareNetworkResourcesType prepareNetworkResourcesType ) {
      // Default VPC, lookup subnet for user specified or system selected partition
      final AccountFullName accountFullName = allocation.getOwnerFullName( ).asAccountFullName( );
      final String defaultVpcId = getDefaultVpcId( accountFullName, allocation );
      if ( defaultVpcId != null && allocation.getPartition( ) != null && allocation.getSubnet( ) == null ) {
        try {
          allocation.setSubnet( subnets.lookupDefault( accountFullName, allocation.getPartition( ).getName( ), Functions.identity( ) ) );
        } catch ( VpcMetadataException e ) {
          throw Exceptions.toUndeclared( "Error preparing subnet for network allocation", e );
        }
      }

      final Subnet subnet1 = ( allocation == null ? null : allocation.getSubnet( ) );
      final Vpc vpc = ( subnet1 == null ? null : subnet1.getVpc( ) );
      prepareNetworkResourcesType.setVpc( ( vpc == null ? null : vpc.getDisplayName( ) ) );
      final Subnet subnet2 = ( allocation == null ? null : allocation.getSubnet( ) );
      prepareNetworkResourcesType.setSubnet( ( subnet2 == null ? null : subnet2.getDisplayName( ) ) );
      final List<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces = allocation.getAttribute( secondaryNetworkInterfacesKey );

      if ( allocation.getSubnet( ) != null && !prepareFromTokenResources( allocation, prepareNetworkResourcesType, VpcNetworkInterfaceResource.class ) ) {
        for ( VmInstanceToken token : allocation.getAllocationTokens( ) ) {
          Stream<VpcNetworkInterfaceResource> resources = Stream.ofAll( token.getAttribute( NetworkResourcesKey ) )
              .filter( VpcNetworkInterfaceResource.class::isInstance )
              .map( VpcNetworkInterfaceResource.class::cast )
              .filter( resource -> resource.getOwnerId( ) != null );
          if ( resources.isEmpty( ) ) {
            final RunInstancesType runInstances = ( (RunInstancesType) allocation.getRequest( ) );
            final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface = getPrimaryNetworkInterface( runInstances );
            if ( ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getNetworkInterfaceId( ) ) != null ) {
              final VpcNetworkInterfaceResource resource = new VpcNetworkInterfaceResource(
                  token.getInstanceId( ),
                  instanceNetworkInterface.getNetworkInterfaceId( ) );
              resource.setDevice( 0 );
              resource.setDeleteOnTerminate( MoreObjects.firstNonNull( instanceNetworkInterface.getDeleteOnTermination( ), false ) );
              resources = Stream.of( resource );
            } else {
              final String identifier = ResourceIdentifiers.generateString( "eni" );
              final String mac = NetworkInterfaceHelper.mac( identifier );
              final String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.getPrivateIpAddress( ) );
              final Set<String> ids = getSecurityGroupIds( instanceNetworkInterface );
              final Set<String> networkIds = ids!=null && !ids.isEmpty() ?
                  ids :
                  Stream.ofAll( allocation.getNetworkGroups( ) ).map( NetworkGroup::getGroupId ).toJavaSet( );
              final VpcNetworkInterfaceResource resource =
                  new VpcNetworkInterfaceResource( token.getInstanceId( ), identifier );
              resource.setDevice( 0 );
              resource.setMac( mac );
              resource.setPrivateIp( privateIp );
              final String description = ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getDescription( ) );
              resource.setDescription( !Strings.isNullOrEmpty( description ) ? description : "Primary network interface" );
              resource.setDeleteOnTerminate( MoreObjects.firstNonNull( ( instanceNetworkInterface == null ? null : instanceNetworkInterface.getDeleteOnTermination( ) ), true ) );
              resource.setNetworkGroupIds( Lists.newArrayList( networkIds ) );
              resources = Stream.of( resource );
            }

            for ( InstanceNetworkInterfaceSetItemRequestType secondaryNetworkInterface : secondaryNetworkInterfaces ) {
              if ( ( secondaryNetworkInterface == null ? null : secondaryNetworkInterface.getNetworkInterfaceId( ) ) != null ) {
                final VpcNetworkInterfaceResource resource = new VpcNetworkInterfaceResource( token.getInstanceId( ), secondaryNetworkInterface.getNetworkInterfaceId( ) );
                resource.setDevice( secondaryNetworkInterface.getDeviceIndex( ) );
                resource.setDeleteOnTerminate( MoreObjects.firstNonNull( secondaryNetworkInterface.getDeleteOnTermination( ), false ) );
                resources = resources.append( resource );
              } else {
                final String identifier = ResourceIdentifiers.generateString( "eni" );
                final String mac = NetworkInterfaceHelper.mac( identifier );
                final String privateIp = getPrimaryPrivateIp( secondaryNetworkInterface, null );
                final Set<String> networkIds = getSecurityGroupIds( secondaryNetworkInterface );
                final Subnet subnet = RestrictedTypes.resolver( Subnet.class ).apply( secondaryNetworkInterface.getSubnetId( ) );
                final VpcNetworkInterfaceResource resource = new VpcNetworkInterfaceResource( token.getInstanceId( ), identifier );
                resource.setDevice( secondaryNetworkInterface.getDeviceIndex( ) );
                resource.setVpc( subnet.getVpc( ).getDisplayName( ) );
                resource.setSubnet( subnet.getDisplayName( ) );
                resource.setMac( mac );
                resource.setPrivateIp( privateIp );
                final String description = ( secondaryNetworkInterface == null ? null : secondaryNetworkInterface.getDescription( ) );
                resource.setDescription( !Strings.isNullOrEmpty( description ) ? description : "Secondary network interface" );
                resource.setDeleteOnTerminate( MoreObjects.firstNonNull( secondaryNetworkInterface.getDeleteOnTermination( ), false ) );
                resource.setNetworkGroupIds( Lists.newArrayList( networkIds ) );
                resources = resources.append( resource );
              }
            }
          }

          token.getAttribute( NetworkResourcesKey ).removeAll( resources.toJavaList( ) );
          prepareNetworkResourcesType.getResources( ).addAll( resources.toJavaList( ) );
        }
      }
    }

    @Override
    public void verifyNetworkAllocation(
        final Allocation allocation,
        final PrepareNetworkResourcesResultType prepareNetworkResourcesResultType
    ) {
      final Set<Pair<String, Integer>> privateAddressDeviceIndexPairs = Sets.newHashSet( );

      final RunInstancesType runInstances = (RunInstancesType) allocation.getRequest( );
      final InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterface = getPrimaryNetworkInterface( runInstances );
      if ( instanceNetworkInterface != null ) {
        String privateIp = getPrimaryPrivateIp( instanceNetworkInterface, runInstances.getPrivateIpAddress( ) );
        if ( privateIp != null ) {
          privateAddressDeviceIndexPairs.add( Pair.pair( privateIp, 0 ) );
        }
      }

      final List<InstanceNetworkInterfaceSetItemRequestType> secondaryNetworkInterfaces =
          allocation.getAttribute( secondaryNetworkInterfacesKey );
      for ( InstanceNetworkInterfaceSetItemRequestType secondaryNetworkInterface : secondaryNetworkInterfaces ) {
        String privateIp = getPrimaryPrivateIp( secondaryNetworkInterface, null );
        if ( privateIp != null ) {
         privateAddressDeviceIndexPairs.add( Pair.pair( privateIp, secondaryNetworkInterface.getDeviceIndex( ) ) );
        }
      }

      // check that there is a resource for each device index requested
      for ( ResourceToken token : allocation.getAllocationTokens( ) ) {
        final Stream<VpcNetworkInterfaceResource> networkInterfaceResources =
            Stream.ofAll( token.getAttribute( NetworkResourcesKey ) )
                .filter( VpcNetworkInterfaceResource.class::isInstance )
                .map( VpcNetworkInterfaceResource.class::cast );

        for ( final Pair<String, Integer> addressAndDeviceIndex : privateAddressDeviceIndexPairs ) {
          if ( networkInterfaceResources.filter( resource -> resource.getDevice( ) == addressAndDeviceIndex.getRight( ) ).isEmpty( ) ) {
            throw Exceptions.toUndeclared( new PrivateAddressResourceAllocationException(
                "Private address not available (" + addressAndDeviceIndex.getLeft( ) + ")" ) );
          }
        }

        if ( instanceNetworkInterface != null ) {
          if ( networkInterfaceResources.size( ) != ( secondaryNetworkInterfaces.size( ) + 1 ) ) {
            throw Exceptions.toUndeclared( new NotEnoughPrivateAddressResourcesException( "Insufficient private addresses" ) );
          }
        }
      }
    }

    @Override
    public void prepareVmRunType( final ResourceToken resourceToken, final Builder builder ) {
      final Stream<VpcNetworkInterfaceResource> resources =
          Stream.ofAll( resourceToken.getAttribute( NetworkResourcesKey ) )
              .filter( VpcNetworkInterfaceResource.class::isInstance )
              .map( VpcNetworkInterfaceResource.class::cast );
      if ( !resources.isEmpty( ) ) {
        // Handle primary interface
        final VpcNetworkInterfaceResource primaryInterface =
            resources.filter( resource -> resource.getDevice( ) == 0 ).head( );

        if ( primaryInterface.getMac( ) == null ) {
          NetworkInterface networkInterface =
              RestrictedTypes.resolver( NetworkInterface.class ).apply( primaryInterface.getValue( ) );
          builder.privateAddress( networkInterface.getPrivateIpAddress( ) );
          builder.macAddress( networkInterface.getMacAddress( ) );
          builder.primaryEniAttachmentId( networkInterface.getAttachment( ).getAttachmentId( ) );
        } else {
          builder.privateAddress( primaryInterface.getPrivateIp( ) );
          builder.macAddress( primaryInterface.getMac( ) );
          builder.primaryEniAttachmentId( primaryInterface.getAttachmentId( ) );
        }

        // Handle secondary interfaces
        final List<VpcNetworkInterfaceResource> secondaryInterfaces = Stream.ofAll( resources )
            .filter( VpcNetworkInterfaceResource.class::isInstance )
            .map( VpcNetworkInterfaceResource.class::cast )
            .filter( networkResource -> networkResource.getDevice( ) != 0 )
            .toJavaList( );

        for ( VpcNetworkInterfaceResource secondaryInterface : secondaryInterfaces ) {
          NetworkConfigType netConfig = new NetworkConfigType( secondaryInterface.getValue( ), secondaryInterface.getDevice( ) );
          if ( secondaryInterface.getMac( ) == null ) {
            NetworkInterface networkInterface = RestrictedTypes.resolver( NetworkInterface.class ).apply( secondaryInterface.getValue( ) );
            netConfig.setMacAddress( networkInterface.getMacAddress( ) );
            netConfig.setIpAddress( networkInterface.getPrivateIpAddress( ) );
            netConfig.setAttachmentId( networkInterface.getAttachment( ).getAttachmentId( ) );
          } else {
            netConfig.setMacAddress( secondaryInterface.getMac( ) );
            netConfig.setIpAddress( secondaryInterface.getPrivateIp( ) );
            netConfig.setAttachmentId( secondaryInterface.getAttachmentId( ) );
          }

          builder.secondaryNetConfig( netConfig );
        }
      }
    }

    @SuppressWarnings( "UnnecessaryQualifiedReference" )
    @Override
    public void prepareVmInstance( final ResourceToken resourceToken, final VmInstances.Builder builder ) {
      final NetworkInterfaces networkInterfaces = VpcNetworkInterfaceVmInstanceLifecycleHelper.networkInterfaces;
      final Stream<VpcNetworkInterfaceResource> resources = Stream.ofAll( resourceToken.getAttribute( NetworkResourcesKey ) )
          .filter( VpcNetworkInterfaceResource.class::isInstance )
          .map( VpcNetworkInterfaceResource.class::cast );
      if ( !resources.isEmpty( ) ) {
        builder.onBuild( instance -> {
          final VpcNetworkInterfaceResource resource =
              resources.filter( networkResource -> networkResource.getDevice( ) == 0 ).head( );
          try {
            final NetworkInterface networkInterface = resource.getMac( ) == null ?
                RestrictedTypes.resolver( NetworkInterface.class ).apply( resource.getValue( ) ) :
                networkInterfaces.save( NetworkInterface.create(
                    instance.getOwner( ),
                    instance.getBootRecord( ).getVpc( ),
                    instance.getBootRecord( ).getSubnet( ),
                    Stream.ofAll( resource.getNetworkGroupIds( ) )
                        .map( ThrowingFunction.<String,NetworkGroup>undeclared( NetworkGroups::lookupByGroupId ) )
                        .toJavaSet( ),
                    resource.getValue( ),
                    resource.getMac( ),
                    resource.getPrivateIp( ),
                    instance.getBootRecord( ).getVpc( ).getDnsHostnames( ) ?
                        VmInstances.dnsName( resource.getPrivateIp( ), DomainNames.internalSubdomain( ) ) :
                        (String) null,
                    MoreObjects.firstNonNull( resource.getDescription( ), "" )
                ) );
            if ( resource.getPrivateIp( ) != null ) {
              PrivateAddresses.associate( resource.getPrivateIp( ), networkInterface );
            }
            instance.updateMacAddress( networkInterface.getMacAddress( ) );
            VmInstances.updatePrivateAddress( instance, networkInterface.getPrivateIpAddress( ) );
            resource.setPrivateIp( networkInterface.getPrivateIpAddress( ) );
            resource.setMac( networkInterface.getMacAddress( ) );
            networkInterface.attach( NetworkInterfaceAttachment.create(
                ResourceIdentifiers.generateString( "eni-attach" ),
                instance,
                instance.getDisplayName( ),
                instance.getOwner( ).getAccountNumber( ),
                0,
                Status.attached,
                new Date( ),
                resource.getDeleteOnTerminate( )
            ) );
            resource.setAttachmentId( networkInterface.getAttachment( ).getAttachmentId( ) );
            final Address address = getAddress( resourceToken );
            withBatch( () -> {
                if ( address != null ) {
                  NetworkInterfaceHelper.associate( address, networkInterface, Optional.of( instance ) );
                } else {
                  if ( networkInterface.isAssociated( ) ) {
                    VmInstances.updatePublicAddress( instance, networkInterface.getAssociation( ).getPublicIp( ) );
                  }
                  NetworkInterfaceHelper.start( networkInterface, instance );
                }
                AddressingBatch.reset( );// Flush after running
            } );
            // Add so eni information is available from instance, not for
            // persistence
            instance.addNetworkInterface( networkInterface );

            // Handle secondary interfaces
            List<VpcNetworkInterfaceResource> secondaryResources =
                resources.filter( networkResource -> networkResource.getDevice( ) != 0 ).toJavaList( );
            secondaryResources = Ordering.natural( ).onResultOf( VpcNetworkInterfaceResource::getDevice ).sortedCopy( secondaryResources );
            for ( VpcNetworkInterfaceResource secondaryResource : secondaryResources ) {
              final Subnet subnet = secondaryResource.getSubnet( ) == null ?
                  null :
                  RestrictedTypes.resolver( Subnet.class ).apply( secondaryResource.getSubnet( ) );
              final NetworkInterface secondaryNetworkInterface = secondaryResource.getMac( ) == null ?
                  RestrictedTypes.resolver( NetworkInterface.class ).apply( secondaryResource.getValue( ) ) :
                  networkInterfaces.save( NetworkInterface.create(
                      instance.getOwner( ),
                      subnet.getVpc( ),
                      subnet,
                      Stream.ofAll( secondaryResource.getNetworkGroupIds( ) )
                          .map( ThrowingFunction.<String,NetworkGroup>undeclared( NetworkGroups::lookupByGroupId ) )
                          .toJavaSet( ),
                      secondaryResource.getValue( ),
                      secondaryResource.getMac( ),
                      secondaryResource.getPrivateIp( ),
                      instance.getBootRecord( ).getVpc( ).getDnsHostnames( ) ?
                          VmInstances.dnsName( secondaryResource.getPrivateIp( ), DomainNames.internalSubdomain( ) ) :
                          (String) null,
                      MoreObjects.firstNonNull( secondaryResource.getDescription( ), "" )
                  ) );
              if ( secondaryResource.getPrivateIp( ) != null ) {
                PrivateAddresses.associate( secondaryResource.getPrivateIp( ), secondaryNetworkInterface );
              }
              secondaryResource.setPrivateIp( secondaryNetworkInterface.getPrivateIpAddress( ) );
              secondaryResource.setMac( secondaryNetworkInterface.getMacAddress( ) );
              secondaryNetworkInterface.attach( NetworkInterfaceAttachment.create(
                  ResourceIdentifiers.generateString( "eni-attach" ),
                  instance,
                  instance.getDisplayName( ),
                  instance.getOwner( ).getAccountNumber( ),
                  secondaryResource.getDevice( ),
                  Status.attached,
                  new Date( ),
                  secondaryResource.getDeleteOnTerminate( )
              ) );
              secondaryResource.setAttachmentId( secondaryNetworkInterface.getAttachment( ).getAttachmentId( ) );
              withBatch( () -> {
                  NetworkInterfaceHelper.start( secondaryNetworkInterface, instance );
                  AddressingBatch.reset( );// Flush after running
              } );
              // Add so eni information is available from instance, not for
              // persistence
              instance.addNetworkInterface( secondaryNetworkInterface );
            }
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( "Error preparing vm", e );
          }
        } );
      }
    }

    @Override
    public void startVmInstance( final ResourceToken resourceToken, final VmInstance instance ) {
      for ( NetworkInterface networkInterface : instance.getNetworkInterfaces( ) ) {
        if ( networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
          if ( networkInterface.isAssociated( ) ) {
            VmInstances.updateAddresses( instance, networkInterface.getPrivateIpAddress( ), networkInterface.getAssociation( ).getPublicIp( ) );
          } else {
            VmInstances.updatePrivateAddress( instance, networkInterface.getPrivateIpAddress( ) );
          }
        }
        NetworkInterfaceHelper.start( networkInterface, instance );
      }
    }

    @Override
    public void cleanUpInstance( final VmInstance instance, final VmState state ) {
      for ( NetworkInterface networkInterface : instance.getNetworkInterfaces( ) ) {
        if ( networkInterface.isAssociated( ) && VmStateSet.DONE.contains( state ) ) {
          PublicAddresses.markDirty( networkInterface.getAssociation( ).getPublicIp( ), instance.getPartition( ) );
        }

        if ( VmStateSet.DONE.contains( state ) && Entities.isPersistent( instance ) ) {
          if ( networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
            VmInstances.updateAddresses( instance, VmNetworkConfig.DEFAULT_IP, VmNetworkConfig.DEFAULT_IP );
          }

          final NetworkInterfaceAttachment attachment = ( networkInterface == null ? null : networkInterface.getAttachment( ) );
          if ( ( attachment == null ? null : attachment.getDeleteOnTerminate( ) ) ) {
            NetworkInterfaceHelper.release( networkInterface );
            Entities.delete( networkInterface );
          } else if ( networkInterface.isAssociated( ) ) {
            NetworkInterfaceHelper.stop( networkInterface );
          }

          networkInterface.detach( );
        } else if ( VmStateSet.TORNDOWN.contains( state ) && Entities.isPersistent( instance ) ) {
          NetworkInterfaceHelper.stop( networkInterface );
        }
      }
    }

    /**
     * The first four and last addresses are reserved in a subnet
     */
    private static boolean validPrivateIpForCidr( String cidrText, String privateIp ) {
      boolean valid = true;
      if ( privateIp != null ) {
        final int addressAsInt = PrivateAddresses.asInteger( privateIp );
        final Cidr cidr = Cidr.parse( cidrText );
        final IPRange range = IPRange.fromCidr( cidr );// range omits first and last
        valid = range.contains( IPRange.fromCidr( Cidr.of( addressAsInt, 32 ) ) ) &&
            !Iterables.contains( Iterables.limit( IPRange.fromCidr( cidr ), 3 ), addressAsInt );
      }
      return valid;
    }
  }
}
