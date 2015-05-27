/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.component.ComponentException;

import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.*;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.images.BlockStorageDeviceMapping;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.DeviceMapping;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.images.Images;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.compute.common.internal.keys.KeyPairs;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.NetworkGroups;
import com.eucalyptus.compute.common.internal.tags.Filter;
import com.eucalyptus.compute.common.internal.tags.Filters;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.vm.NetworkGroupId;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSets;
import com.eucalyptus.compute.common.internal.vpc.InternetGateway;
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.Lister;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcls;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.RouteTables;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Subnets;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;


/**
 *
 */
@SuppressWarnings( "UnusedDeclaration" )
@ComponentNamed
public class ComputeService implements Callable {
  private static Logger LOG = Logger.getLogger( ComputeService.class );

  private final DhcpOptionSets dhcpOptionSets;
  private final InternetGateways internetGateways;
  private final NetworkAcls networkAcls;
  private final NetworkInterfaces networkInterfaces;
  private final RouteTables routeTables;
  private final Subnets subnets;
  private final Vpcs vpcs;

  @Inject
  public ComputeService( final DhcpOptionSets dhcpOptionSets,
                         final InternetGateways internetGateways,
                         final NetworkAcls networkAcls,
                         final NetworkInterfaces networkInterfaces,
                         final RouteTables routeTables,
                         final Subnets subnets,
                         final Vpcs vpcs ) {
    this.dhcpOptionSets = dhcpOptionSets;
    this.internetGateways = internetGateways;
    this.networkAcls = networkAcls;
    this.networkInterfaces = networkInterfaces;
    this.routeTables = routeTables;
    this.subnets = subnets;
    this.vpcs = vpcs;
  }

  public DescribeImagesResponseType describeImages( final DescribeImagesType request ) throws EucalyptusCloudException, TransactionException {
    DescribeImagesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final boolean showAllStates =
        ctx.isAdministrator( ) &&
            request.getImagesSet( ).remove( "verbose" );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final List<String> imageIds = normalizeImageIdentifiers( request.getImagesSet() );
    final List<String> ownersSet = request.getOwnersSet();
    if ( ownersSet.remove( Images.SELF ) ) {
      ownersSet.add( requestAccountId );
    }
    final Filter filter = Filters.generate( request.getFilterSet(), ImageInfo.class );
    final Predicate<? super ImageInfo> requestedAndAccessible = CloudMetadatas.filteringFor( ImageInfo.class )
        .byId( imageIds )
        .byOwningAccount( request.getOwnersSet() )
        .byPredicate( showAllStates ?
            Predicates.<ImageInfo>alwaysTrue() :
            Images.standardStatePredicate( ) )
        .byPredicate( Images.filterExecutableBy( request.getExecutableBySet() ) )
        .byPredicate( filter.asPredicate() )
        .byPredicate( Images.FilterPermissions.INSTANCE )
        .byPrivilegesWithoutOwner()
        .buildPredicate();
    final List<ImageDetails> imageDetailsList = Transactions.filteredTransform(
        new ImageInfo(),
        filter.asCriterion(),
        filter.getAliases(),
        requestedAndAccessible,
        Images.TO_IMAGE_DETAILS );

    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( ImageInfo.class )
        .getResourceTagMap( AccountFullName.getInstance( ctx.getAccountNumber() ),
            Iterables.transform( imageDetailsList, ImageDetails.imageId( ) ) );

    for ( final ImageDetails details : imageDetailsList ) {
      Tags.addFromTags( details.getTagSet(), ResourceTag.class, tagsMap.get( details.getImageId() ) );
    }
    reply.getImagesSet( ).addAll( imageDetailsList );
    return reply;
  }

  public DescribeImageAttributeResponseType describeImageAttribute( final DescribeImageAttributeType request ) throws EucalyptusCloudException {
    DescribeImageAttributeResponseType reply = request.getReply( );

    if ( request.getAttribute( ) != null ) request.applyAttribute( );

    try ( final TransactionResource tx = Entities.transactionFor( ImageInfo.class ) ) {
      final ImageInfo imgInfo = Entities.uniqueResult( Images.exampleWithImageId( imageIdentifier( request.getImageId( ) ) ) );
      if ( !canModifyImage( imgInfo ) ) {
        throw new EucalyptusCloudException( "Not authorized to describe image attribute" );
      }
      reply.setImageId( imgInfo.getDisplayName() );
      if ( request.getKernel( ) != null ) {
        if ( imgInfo instanceof MachineImageInfo ) {
          if ( ( ( MachineImageInfo ) imgInfo ).getKernelId( ) != null ) {
            reply.getKernel( ).add( ( ( MachineImageInfo ) imgInfo ).getKernelId( ) );
          }
        }
      } else if ( request.getRamdisk( ) != null ) {
        if ( imgInfo instanceof MachineImageInfo ) {
          if ( ( ( MachineImageInfo ) imgInfo ).getRamdiskId( ) != null ) {
            reply.getRamdisk( ).add( ( ( MachineImageInfo ) imgInfo ).getRamdiskId( ) );
          }
        }
      } else if ( request.getLaunchPermission( ) != null ) {
        if ( imgInfo.getImagePublic( ) ) {
          reply.getLaunchPermission( ).add( LaunchPermissionItemType.newGroupLaunchPermission() );
        }
        for ( final String permission : imgInfo.getPermissions() )
          reply.getLaunchPermission().add( LaunchPermissionItemType.newUserLaunchPermission( permission ) );
      } else if ( request.getProductCodes( ) != null ) {
        reply.getProductCodes( ).addAll( imgInfo.getProductCodes( ) );
      } else if ( request.getBlockDeviceMapping( ) != null ) {
        if ( imgInfo instanceof BlockStorageImageInfo ) {
          BlockStorageImageInfo bfebsImage = (BlockStorageImageInfo) imgInfo;
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( VmInstances.getEbsRootDeviceName( ), bfebsImage.getRootDeviceName( ) ) );
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( "root", bfebsImage.getRootDeviceName( ) ) );
          int i = 0;
          for ( DeviceMapping mapping : bfebsImage.getDeviceMappings() ) {
            if ( mapping.getDeviceName( ).equalsIgnoreCase( bfebsImage.getRootDeviceName( ) ) ) {
              continue;
            }
            switch ( mapping.getDeviceMappingType( ) ) {
              case blockstorage:
                BlockStorageDeviceMapping bsdm = ( BlockStorageDeviceMapping ) mapping;
                BlockDeviceMappingItemType bdmItem = new BlockDeviceMappingItemType( "ebs" + (++i), mapping.getDeviceName( ) );
                EbsDeviceMapping ebsItem = new EbsDeviceMapping( );
                ebsItem.setSnapshotId( bsdm.getSnapshotId( ) );
                ebsItem.setVolumeSize( bsdm.getSize( ) );
                ebsItem.setDeleteOnTermination( bsdm.getDelete( ) );
                bdmItem.setEbs( ebsItem );
                reply.getBlockDeviceMapping( ).add( bdmItem );
                break;
              case ephemeral:
                reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( mapping.getVirtualName() , mapping.getDeviceName( ) ) );
                break;
              default:
                break;
            }
          }
        } else {
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( VmInstances.getEbsRootDeviceName( ), "sda1" ) );
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( "ephemeral0", "sda2" ) );
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( "swap", "sda3" ) );
          reply.getBlockDeviceMapping( ).add( new BlockDeviceMappingItemType( "root", "/dev/sda1" ) );
        }
      } else if ( request.getDescription( ) != null ) {
        if ( imgInfo.getDescription() != null ) {
          reply.getDescription().add( imgInfo.getDescription() );
        }
      } else {
        throw new EucalyptusCloudException( "invalid image attribute request." );
      }
    } catch ( TransactionException | NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Error handling image attribute request: " + ex.getMessage( ), ex );
    }
    return reply;
  }

  public DescribeSecurityGroupsResponseType describe( final DescribeSecurityGroupsType request ) throws EucalyptusCloudException, MetadataException, TransactionException {
    final DescribeSecurityGroupsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final boolean showAll =
        request.getSecurityGroupSet( ).remove( "verbose" ) ||
            request.getSecurityGroupIdSet( ).remove( "verbose" );

    NetworkGroups.createDefault( ctx.getUserFullName() ); //ensure the default group exists to cover some old broken installs

    final Filters.FiltersBuilder builder = Filters.generateFor( request.getFilterSet( ), NetworkGroup.class );
    if ( ( request.getSecurityGroupSet( ).isEmpty( ) && !request.getSecurityGroupIdSet( ).isEmpty( ) ) ||
        ( !request.getSecurityGroupSet( ).isEmpty( ) && request.getSecurityGroupIdSet( ).isEmpty( ) )) {
      builder.withOptionalInternalFilter( "group-name", request.getSecurityGroupSet( ) );
      builder.withOptionalInternalFilter( "group-id", normalizeGroupIdentifiers( request.getSecurityGroupIdSet( ) ) );
    }
    final Filter filter = builder.generate( );
    final Predicate<? super NetworkGroup> requestedAndAccessible =
        CloudMetadatas.filteringFor( NetworkGroup.class )
            .byPredicate( Predicates.or(
                request.getSecurityGroupSet( ).isEmpty() && request.getSecurityGroupIdSet( ).isEmpty() ?
                    Predicates.<NetworkGroup>alwaysTrue() :
                    Predicates.<NetworkGroup>alwaysFalse(),
                request.getSecurityGroupSet( ).isEmpty() ?
                    Predicates.<NetworkGroup>alwaysFalse() :
                    CloudMetadatas.<NetworkGroup>filterById( request.getSecurityGroupSet( ) ),
                request.getSecurityGroupIdSet( ).isEmpty() ?
                    Predicates.<NetworkGroup>alwaysFalse() :
                    CloudMetadatas.filterByProperty( normalizeGroupIdentifiers( request.getSecurityGroupIdSet( ) ), NetworkGroup.groupId() ) ) )
            .byPredicate( filter.asPredicate( ) )
            .byPrivileges()
            .buildPredicate();

    final OwnerFullName ownerFn = Contexts.lookup( ).isAdministrator( ) && showAll ?
        null :
        AccountFullName.getInstance( ctx.getAccountNumber( ) );

    final Iterable<SecurityGroupItemType> securityGroupItems =
        Entities.asDistinctTransaction( NetworkGroup.class, new Function<Void, Iterable<SecurityGroupItemType>>() {
          @Nullable
          @Override
          public Iterable<SecurityGroupItemType> apply( @Nullable final Void aVoid ) {
            try {
              return Transactions.filteredTransform(
                  NetworkGroup.withOwner( ownerFn ),
                  filter.asCriterion(),
                  filter.getAliases(),
                  requestedAndAccessible,
                  TypeMappers.lookup( NetworkGroup.class, SecurityGroupItemType.class ) );
            } catch ( TransactionException e ) {
              if ( Exceptions.isCausedBy( e, EntityNotFoundException.class ) ) {
                // A rule may have been deleted, retry
                throw new Entities.RetryTransactionException( e, NetworkGroup.class );
              }
              throw Exceptions.toUndeclared( e );
            }
          }
        } ).apply( null );

    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( NetworkGroup.class )
        .getResourceTagMap( AccountFullName.getInstance( ctx.getAccountNumber( ) ),
            Iterables.transform( securityGroupItems, SecurityGroupItemToGroupId.INSTANCE ) );
    for ( final SecurityGroupItemType securityGroupItem : securityGroupItems ) {
      Tags.addFromTags( securityGroupItem.getTagSet(), ResourceTag.class, tagsMap.get( securityGroupItem.getGroupId() ) );
    }

    Iterables.addAll( reply.getSecurityGroupInfo( ), securityGroupItems );

    return reply;
  }

  public DescribeInstancesResponseType describeInstances( final DescribeInstancesType msg ) throws EucalyptusCloudException {
    final DescribeInstancesResponseType reply = msg.getReply( );
    Context ctx = Contexts.lookup( );
    boolean showAll = msg.getInstancesSet( ).remove( "verbose" ) || !msg.getInstancesSet( ).isEmpty( );
    final Multimap<String, RunningInstancesItemType> instanceMap = TreeMultimap.create();
    final Map<String, ReservationInfoType> reservations = Maps.newHashMap();
    final Collection<String> identifiers = normalizeInstanceIdentifiers( msg.getInstancesSet() );
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class )
        .withOptionalInternalFilter( "instance-id", identifiers )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( identifiers ) // filters without wildcard support
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    final Criterion criterion = filter.asCriterionWithConjunction( Restrictions.not( VmInstance.criterion( VmInstance.VmState.BURIED ) ) );
    final OwnerFullName ownerFullName = ( ctx.isAdministrator( ) && showAll )
        ? null
        : ctx.getUserFullName( ).asAccountFullName( );
    try ( final TransactionResource db = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, criterion, filter.getAliases(), requestedAndAccessible );
      if ( instances.isEmpty( ) && !identifiers.isEmpty( ) ) {
        throw new ComputeServiceClientException( "InvalidInstanceID.NotFound", "The instance '"+Iterables.get( identifiers, 0 )+"' was not found" );
      }
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( VmInstance.class )
          .getResourceTagMap( AccountFullName.getInstance( ctx.getAccountNumber() ),
              Iterables.transform( instances, CloudMetadatas.toDisplayName() ) );

      for ( final VmInstance vm : instances ) {
        if ( instanceMap.put( vm.getReservationId( ), VmInstance.transform( vm ) ) && !reservations.containsKey( vm.getReservationId( ) ) ) {
          reservations.put( vm.getReservationId( ), TypeMappers.transform( vm, ReservationInfoType.class ) );
        }
      }
      List<ReservationInfoType> replyReservations = reply.getReservationSet( );
      for ( ReservationInfoType r : reservations.values( ) ) {
        Collection<RunningInstancesItemType> instanceSet = instanceMap.get( r.getReservationId( ) );
        if ( !instanceSet.isEmpty( ) ) {
          for ( final RunningInstancesItemType instancesItemType : instanceSet ) {
            Tags.addFromTags( instancesItemType.getTagSet(), ResourceTag.class, tagsMap.get( instancesItemType.getInstanceId() ) );
          }
          r.getInstancesSet( ).addAll( instanceSet );
          replyReservations.add( r );
        }
      }
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, ComputeServiceException.class );
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }

  public DescribeInstanceStatusResponseType describeInstanceStatus( final DescribeInstanceStatusType msg ) throws EucalyptusCloudException {
    final DescribeInstanceStatusResponseType reply = msg.getReply();
    final Context ctx = Contexts.lookup();
    final boolean showAll = msg.getInstancesSet( ).remove( "verbose" ) || !msg.getInstancesSet( ).isEmpty( );
    final boolean includeAllInstances = Objects.firstNonNull( msg.getIncludeAllInstances(), Boolean.FALSE );
    final Collection<String> identifiers = normalizeInstanceIdentifiers( msg.getInstancesSet() );
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class, "status" )
        .withOptionalInternalFilter( "instance-id", identifiers )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( identifiers ) // filters without wildcard support
        .byPredicate( includeAllInstances ? Predicates.<VmInstance>alwaysTrue() : VmInstance.VmState.RUNNING )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    final Criterion criterion = filter.asCriterionWithConjunction( Restrictions.not( VmInstance.criterion( VmInstance.VmState.BURIED ) ) );
    final OwnerFullName ownerFullName = ( ctx.isAdministrator( ) && showAll )
        ? null
        : ctx.getUserFullName( ).asAccountFullName( );
    try {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, criterion, filter.getAliases(), requestedAndAccessible );
      if ( instances.isEmpty( ) && !identifiers.isEmpty( ) ) {
        throw new ComputeServiceClientException( "InvalidInstanceID.NotFound", "The instance '"+Iterables.get( identifiers, 0 )+"' was not found" );
      }

      Iterables.addAll(
          reply.getInstanceStatusSet().getItem(),
          Iterables.transform( instances, TypeMappers.lookup( VmInstance.class, InstanceStatusItemType.class ) ) );

    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, ComputeServiceException.class );
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }

  public DescribeInstanceAttributeResponseType describeInstanceAttribute( final DescribeInstanceAttributeType request )
      throws EucalyptusCloudException {
    final DescribeInstanceAttributeResponseType reply = request.getReply( );
    final String instanceId = normalizeInstanceIdentifier( request.getInstanceId() );
    final String attribute = request.getAttribute( );
    if ( attribute == null ) {
      throw new ComputeServiceClientException( " MissingParameter", "Attribute parameter is required" );
    }
    try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
      final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
      reply.setInstanceId( instanceId );
      switch ( attribute ) {
        case "blockDeviceMapping":
          if ( vm.getBootRecord( ).getMachine( ) instanceof BlockStorageImageInfo ) {
            final BlockStorageImageInfo bfebsInfo = ( BlockStorageImageInfo ) vm.getBootRecord( ).getMachine( );
            for ( final VmVolumeAttachment volumeAttachment : vm.getBootRecord().getPersistentVolumes( ) ) {
              reply.getBlockDeviceMapping( ).add( new InstanceBlockDeviceMapping(
                  volumeAttachment.getIsRootDevice( ) ?
                      bfebsInfo.getRootDeviceName( ) :
                      volumeAttachment.getDevice( ),
                  volumeAttachment.getVolumeId(),
                  volumeAttachment.getStatus(),
                  volumeAttachment.getAttachTime(),
                  volumeAttachment.getDeleteOnTerminate() ) );
            }
          }
          break;
        case "disableApiTermination":
          reply.setDisableApiTermination( false );
          break;
        case "ebsOptimized":
          reply.setEbsOptimized( false );
          break;
        case "groupSet":
          Iterables.addAll( reply.getGroupSet( ), Iterables.transform(
              vm.getNetworkGroupIds( ),
              TypeMappers.lookup( NetworkGroupId.class, GroupItemType.class ) ) );
          break;
        case "instanceInitiatedShutdownBehavior":
          reply.setInstanceInitiatedShutdownBehavior( "stop" );
          break;
        case "instanceType":
          reply.setInstanceType( vm.getBootRecord( ).getVmType( ).getDisplayName( ) );
          break;
        case "kernel":
          reply.setKernel( vm.getKernelId( ) );
          break;
        case "productCodes":
          reply.setProductCodes( false ); // set some value so an empty wrapper can be included in the response
          break;
        case "ramdisk":
          reply.setRamdisk( vm.getRamdiskId( ) );
          break;
        case "rootDeviceName":
          reply.setRootDeviceName( vm.getBootRecord( ).getMachine( ) == null ? null : vm.getBootRecord( ).getMachine( ).getRootDeviceName( ) );
          break;
        case "sourceDestCheck":
          reply.setSourceDestCheck( true );
          break;
        case "sriovNetSupport":
          reply.setSriovNetSupport( false );
          break;
        case "userData":
          reply.setUserData( vm.getUserData( ) == null ? "" : Base64.toBase64String( vm.getUserData() ) );
          break;
        default:
          throw new ComputeServiceClientException( " InvalidParameterValue", "Invalid value for attribute ("+attribute+")" );
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      throw new ComputeServiceClientException("InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist");
    }
    return reply;
  }

  public DescribeKeyPairsResponseType describeKeyPairs( DescribeKeyPairsType request ) throws Exception {
    final DescribeKeyPairsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.getKeySet( ).remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) &&  showAll  ? null : Contexts.lookup( ).getUserFullName( ).asAccountFullName( );
    final Filter filter = Filters.generate( request.getFilterSet(), SshKeyPair.class );
    final Predicate<? super SshKeyPair> requestedAndAccessible = CloudMetadatas.filteringFor( SshKeyPair.class )
        .byId( request.getKeySet( ) )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    final List<String> foundKeyNameList = new ArrayList<>( );
    for ( final SshKeyPair kp : KeyPairs.list( ownerFullName, requestedAndAccessible, filter.asCriterion(), filter.getAliases() ) ) {
      reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
      foundKeyNameList.add( kp.getDisplayName( ) );
    }

    if ( !request.getKeySet( ).isEmpty( ) && request.getKeySet( ).size( ) != reply.getKeySet( ).size( ) ) {
      List<String> reverseRequestedKeySet = ImmutableList.copyOf( request.getKeySet() ).reverse( );
      for ( String requestedKey : reverseRequestedKeySet ) {
        if ( !foundKeyNameList.contains( requestedKey ) ) {
          throw new ComputeServiceClientException( "InvalidKeyPair.NotFound", "The key pair '" + requestedKey + "' does not exist" );
        }
      }
    }
    return reply;
  }

  public DescribePlacementGroupsResponseType describePlacementGroups( final DescribePlacementGroupsType request ) {
    final DescribePlacementGroupsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeReservedInstancesResponseType describeReservedInstances(
      final DescribeReservedInstancesType request
  ) {
    return request.getReply( );
  }

  public DescribeReservedInstancesListingsResponseType describeReservedInstancesListings(
      final DescribeReservedInstancesListingsType request
  ) {
    return request.getReply( );
  }

  public DescribeReservedInstancesModificationsResponseType describeReservedInstancesModifications(
      final DescribeReservedInstancesModificationsType request
  ) {
    return request.getReply( );
  }

  public DescribeReservedInstancesOfferingsResponseType describeReservedInstancesListings(
      final DescribeReservedInstancesOfferingsType request
  ) {
    return request.getReply( );
  }

  public CreateReservedInstancesListingResponseType createReservedInstancesListing(
      final CreateReservedInstancesListingType request
  ) {
    return request.getReply( );
  }

  public CancelReservedInstancesListingResponseType cancelReservedInstancesListing(
      final CancelReservedInstancesListingType request
  ) {
    return request.getReply( );
  }

  public ModifyReservedInstancesResponseType modifyReservedInstances(
      final ModifyReservedInstancesType request
  ) {
    return request.getReply( );
  }

  public PurchaseReservedInstancesOfferingResponseType purchaseReservedInstancesOffering(
      final PurchaseReservedInstancesOfferingType request
  ) {
    return request.getReply( );
  }


  public CancelSpotInstanceRequestsResponseType cancelSpotInstanceRequests( CancelSpotInstanceRequestsType request ) {
    return request.getReply( );
  }

  public CreateSpotDatafeedSubscriptionResponseType createSpotDatafeedSubscription( CreateSpotDatafeedSubscriptionType request ) {
    return request.getReply( );
  }

  public DeleteSpotDatafeedSubscriptionResponseType deleteSpotDatafeedSubscription( DeleteSpotDatafeedSubscriptionType request ) {
    return request.getReply( );
  }

  public DescribeSpotDatafeedSubscriptionResponseType describeSpotDatafeedSubscription( DescribeSpotDatafeedSubscriptionType request ) {
    return request.getReply( );
  }

  public DescribeSpotInstanceRequestsResponseType describeSpotInstances( DescribeSpotInstanceRequestsType request ) {
    return request.getReply( );
  }

  public RequestSpotInstancesResponseType requestSpotInstances( RequestSpotInstancesType request ) {
    return request.getReply( );
  }

  /**
   *
   */
  public DescribeVolumesResponseType describeVolumes( DescribeVolumesType request ) throws Exception {
    final DescribeVolumesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );

    final boolean showAll = request.getVolumeSet( ).remove( "verbose" );
    final AccountFullName ownerFullName = ( ctx.isAdministrator( ) && showAll ) ? null : ctx.getUserFullName( ).asAccountFullName( );
    final Set<String> volumeIds = Sets.newLinkedHashSet( normalizeVolumeIdentifiers( request.getVolumeSet() ) );

    final Filter filter = Filters.generate( request.getFilterSet(), Volume.class );
    final Predicate<? super Volume> requestedAndAccessible = CloudMetadatas.filteringFor( Volume.class )
        .byId( volumeIds )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();

    final Function<Set<String>, Pair<Set<String>,ArrayList<com.eucalyptus.compute.common.Volume>>> populateVolumeSet
        = new Function<Set<String>, Pair<Set<String>,ArrayList<com.eucalyptus.compute.common.Volume>>>( ) {
      public Pair<Set<String>,ArrayList<com.eucalyptus.compute.common.Volume>> apply( final Set<String> input ) {
        final Set<String> allowedVolumeIds = Sets.newHashSet();
        final ArrayList<com.eucalyptus.compute.common.Volume> replyVolumes = Lists.newArrayList();
        final List<VmInstance> vms = VmInstances.list( ownerFullName, Predicates.alwaysTrue() );
        final List<Volume> volumes =
            Entities.query( Volume.named( ownerFullName, null ), true, filter.asCriterion(), filter.getAliases() );
        for ( final Volume foundVol : Iterables.filter(volumes, requestedAndAccessible )) {
          allowedVolumeIds.add( foundVol.getDisplayName( ) );
          if ( State.ANNIHILATED.equals( foundVol.getState( ) ) ) {
            Entities.delete( foundVol );
            replyVolumes.add( foundVol.morph( new com.eucalyptus.compute.common.Volume() ) );
          } else {
            AttachedVolume attachedVolume = null;
            try {
              VmVolumeAttachment attachment = VmInstances.lookupVolumeAttachment( foundVol.getDisplayName( ) , vms );
              attachedVolume = VmVolumeAttachment.asAttachedVolume( attachment.getVmInstance( ) ).apply( attachment );
            } catch ( NoSuchElementException ex ) {
              if ( State.BUSY.equals( foundVol.getState( ) ) ) {
                foundVol.setState( State.EXTANT );
              }
            }
            com.eucalyptus.compute.common.Volume msgTypeVolume = foundVol.morph( new com.eucalyptus.compute.common.Volume( ) );
            if ( attachedVolume != null ) {
              msgTypeVolume.setStatus( "in-use" );
              msgTypeVolume.getAttachmentSet( ).add( attachedVolume );
            }
            replyVolumes.add( msgTypeVolume );
          }
        }
        return Pair.pair( allowedVolumeIds, replyVolumes );
      }
    };

    final Pair<Set<String>,ArrayList<com.eucalyptus.compute.common.Volume>> volumeIdsAndVolumes =
        Entities.asTransaction( Volume.class, populateVolumeSet ).apply( volumeIds );
    @SuppressWarnings( "ConstantConditions" )
    final Set<String> allowedVolumeIds = volumeIdsAndVolumes.getLeft();
    reply.setVolumeSet( volumeIdsAndVolumes.getRight() );

    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( Volume.class )
        .getResourceTagMap( AccountFullName.getInstance( ctx.getAccountNumber( ) ), allowedVolumeIds );
    for ( final com.eucalyptus.compute.common.Volume volume : reply.getVolumeSet() ) {
      Tags.addFromTags( volume.getTagSet(), ResourceTag.class, tagsMap.get( volume.getVolumeId() ) );
    }

    return reply;
  }

  public DescribeSnapshotsResponseType describeSnapshots( final DescribeSnapshotsType request ) throws EucalyptusCloudException {
    final DescribeSnapshotsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup();
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber();
    // verbose does not have any special functionality for snapshots, but is ignored when passed as an identifier
    request.getSnapshotSet().remove( "verbose" );
    final Set<String> snapshotIds = Sets.newHashSet( normalizeSnapshotIdentifiers( request.getSnapshotSet() ) );
    final List<String> ownersSet = request.getOwnersSet( );
    if ( ownersSet.remove( Snapshots.SELF ) ) {
      ownersSet.add( requestAccountId );
    }
    final Filter filter = Filters.generate( request.getFilterSet(), Snapshot.class );
    try ( final TransactionResource tx = Entities.transactionFor( Snapshot.class ) ){
      final List<Snapshot> unfilteredSnapshots =
          Entities.query( Snapshot.named( null, null ), true, filter.asCriterion(), filter.getAliases() );
      final Predicate<? super Snapshot> requestedAndAccessible = CloudMetadatas.filteringFor( Snapshot.class )
          .byId( snapshotIds )
          .byOwningAccount( request.getOwnersSet( ) )
          .byPredicate( Snapshots.filterRestorableBy( request.getRestorableBySet( ), requestAccountId ) )
          .byPredicate( filter.asPredicate( ) )
          .byPredicate( Snapshots.FilterPermissions.INSTANCE )
          .byPrivilegesWithoutOwner( )
          .buildPredicate( );

      final Iterable<Snapshot> snapshots = Iterables.filter( unfilteredSnapshots, requestedAndAccessible );
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( Snapshot.class )
          .getResourceTagMap( AccountFullName.getInstance( ctx.getAccountNumber( ) ),
              Iterables.transform( snapshots, CloudMetadatas.toDisplayName() ) );
      for ( final Snapshot snap : snapshots ) {
        try {
          final com.eucalyptus.compute.common.Snapshot snapReply = snap.morph( new com.eucalyptus.compute.common.Snapshot( ) );
          Tags.addFromTags( snapReply.getTagSet(), ResourceTag.class, tagsMap.get( snapReply.getSnapshotId() ) );
          snapReply.setVolumeId( snap.getParentVolume( ) );
          snapReply.setOwnerId( snap.getOwnerAccountNumber( ) );
          reply.getSnapshotSet( ).add( snapReply );
        } catch ( NoSuchElementException e ) {
          LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
          LOG.debug( e, e );
        }
      }
    }
    return reply;
  }

  public DescribeSnapshotAttributeResponseType describeSnapshotAttribute( DescribeSnapshotAttributeType request ) throws EucalyptusCloudException {
    DescribeSnapshotAttributeResponseType reply = request.getReply( );
    reply.setSnapshotId(request.getSnapshotId());
    final Context ctx = Contexts.lookup();
    final String snapshotId = normalizeSnapshotIdentifier( request.getSnapshotId() );
    try (TransactionResource db = Entities.transactionFor(Snapshot.class)) {
      Snapshot result = Entities.uniqueResult( Snapshot.named(
          ctx.isAdministrator() ? null : ctx.getUserFullName().asAccountFullName(),
          snapshotId ) );
      if( !RestrictedTypes.filterPrivileged( ).apply( result ) ) {
        throw new EucalyptusCloudException("Not authorized to describe attributes for snapshot " + request.getSnapshotId());
      }

      ArrayList<CreateVolumePermissionItemType> permissions = Lists.newArrayList();
      for(String id : result.getPermissions()) {
        permissions.add(new CreateVolumePermissionItemType(id, null));
      }
      if(result.getSnapshotPublic()) {
        permissions.add(new CreateVolumePermissionItemType(null, "all"));
      }

      if(result.getProductCodes() != null) {
        reply.setProductCodes(new ArrayList<>(result.getProductCodes()));
      }
      reply.setCreateVolumePermission(permissions);
    } catch ( NoSuchElementException ex2 ) {
      throw new ComputeServiceClientException( "InvalidSnapshot.NotFound", "The snapshot '"+request.getSnapshotId( )+"' does not exist." );
    } catch ( ExecutionException ex1 ) {
      throw new EucalyptusCloudException( ex1.getCause( ) );
    }
    return reply;
  }

  public DescribeVolumeAttributeResponseType describeVolumeAttribute( DescribeVolumeAttributeType request ) {
    return request.getReply( );
  }

  public DescribeVolumeStatusResponseType describeVolumeStatus( DescribeVolumeStatusType request ) {
    return request.getReply( );
  }

  public DescribeTagsResponseType describeTags( final DescribeTagsType request ) throws Exception {
    final DescribeTagsResponseType reply = request.getReply( );
    final Context context = Contexts.lookup();

    final Filter filter = Filters.generate( request.getFilterSet(), Tag.class );
    final Predicate<? super Tag> requestedAndAccessible = CloudMetadatas.filteringFor(Tag.class)
        .byPredicate( filter.asPredicate( ) )
        .byPrivileges( )
        .buildPredicate( );
    final Ordering<Tag> ordering = Ordering.natural().onResultOf( Tags.resourceId() )
        .compound( Ordering.natural().onResultOf( Tags.key() ) )
        .compound( Ordering.natural().onResultOf( Tags.value() ) );
    Iterables.addAll( reply.getTagSet(), Iterables.transform(
        ordering.sortedCopy( Tags.list(
            context.getUserFullName().asAccountFullName(),
            requestedAndAccessible,
            filter.asCriterion(),
            filter.getAliases() ) ),
        TypeMappers.lookup( Tag.class, TagInfo.class )
    ) );

    return reply;
  }

  public DescribeAccountAttributesResponseType describeAccountAttributes(final DescribeAccountAttributesType request) throws EucalyptusCloudException {
    final DescribeAccountAttributesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final List<String> platforms = Lists.newArrayList( );
    final Set<NetworkingFeature> features = Networking.getInstance().describeFeatures( );
    if ( features.contains( NetworkingFeature.Classic ) ) platforms.add( "EC2" );
    if ( features.contains( NetworkingFeature.Vpc ) ) platforms.add( "VPC" );
    String vpcId = "none";
    try {
      vpcId = vpcs.lookupDefault( accountFullName, CloudMetadatas.toDisplayName( ) );
    } catch ( VpcMetadataNotFoundException e) {
      // no default vpc
    } catch ( Exception e ) {
      throw handleException( e );
    }
    final Map<String,List<String>> attributes = ImmutableMap.of(
        "supported-platforms", platforms,
        "default-vpc", Lists.newArrayList( vpcId )
    );
    final Set<String> requestedAttributes = Sets.newHashSet( request.attributeNames( ) );
    for ( final Map.Entry<String,List<String>> attributeEntry : attributes.entrySet( ) ) {
      if ( requestedAttributes.isEmpty( ) || requestedAttributes.contains( attributeEntry.getKey( ) ) ) {
        reply.getAccountAttributeSet().getItem().add(
            new AccountAttributeSetItemType( attributeEntry.getKey( ), attributeEntry.getValue( ) ) );
      }
    }
    return reply;
  }

  public DescribeCustomerGatewaysResponseType describeCustomerGateways(DescribeCustomerGatewaysType request) throws EucalyptusCloudException {
    DescribeCustomerGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeDhcpOptionsResponseType describeDhcpOptions( final DescribeDhcpOptionsType request ) throws EucalyptusCloudException {
    final DescribeDhcpOptionsResponseType reply = request.getReply( );
    describe(
        Identifier.dopt,
        request.dhcpOptionsIds( ),
        request.getFilterSet( ),
        DhcpOptionSet.class,
        DhcpOptionsType.class,
        reply.getDhcpOptionsSet( ).getItem( ),
        DhcpOptionsType.id( ),
        dhcpOptionSets );
    return reply;
  }

  public DescribeInternetGatewaysResponseType describeInternetGateways( final DescribeInternetGatewaysType request ) throws EucalyptusCloudException {
    final DescribeInternetGatewaysResponseType reply = request.getReply( );
    describe(
        Identifier.igw,
        request.internetGatewayIds( ),
        request.getFilterSet( ),
        InternetGateway.class,
        InternetGatewayType.class,
        reply.getInternetGatewaySet( ).getItem( ),
        InternetGatewayType.id( ),
        internetGateways );
    return reply;
  }

  public DescribeNetworkAclsResponseType describeNetworkAcls( final DescribeNetworkAclsType request ) throws EucalyptusCloudException {
    final DescribeNetworkAclsResponseType reply = request.getReply( );
    describe(
        Identifier.acl,
        request.networkAclIds(),
        request.getFilterSet( ),
        NetworkAcl.class,
        NetworkAclType.class,
        reply.getNetworkAclSet().getItem( ),
        NetworkAclType.id( ),
        networkAcls );
    return reply;
  }

  public DescribeNetworkInterfaceAttributeResponseType describeNetworkInterfaceAttribute(final DescribeNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    final DescribeNetworkInterfaceAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final NetworkInterface networkInterface =
          networkInterfaces.lookupByName( accountFullName, Identifier.eni.normalize( request.getNetworkInterfaceId() ), new Function<NetworkInterface,NetworkInterface>( ){
            @Override
            public NetworkInterface apply( final NetworkInterface networkInterface ) {
              Entities.initialize( networkInterface.getNetworkGroups( ) );
              return networkInterface;
            }
          } );
      if ( RestrictedTypes.filterPrivileged( ).apply( networkInterface ) ) {
        reply.setNetworkInterfaceId( networkInterface.getDisplayName( ) );
        switch ( request.getAttribute() ) {
          case "attachment":
            if ( networkInterface.isAttached( ) )
              reply.setAttachment( TypeMappers.transform( networkInterface.getAttachment( ), NetworkInterfaceAttachmentType.class ) );
            break;
          case "description":
            reply.setDescription( new NullableAttributeValueType( ) );
            reply.getDescription().setValue( networkInterface.getDescription( ) );
            break;
          case "groupSet":
            reply.setGroupSet( new GroupSetType( Collections2.transform(
                networkInterface.getNetworkGroups( ),
                TypeMappers.lookup( NetworkGroup.class, GroupItemType.class ) ) ) );
            break;
          case "sourceDestCheck":
            reply.setSourceDestCheck( new AttributeBooleanValueType() );
            reply.getSourceDestCheck().setValue( networkInterface.getSourceDestCheck( ) );
            break;
          default:
            throw new ComputeServiceClientException( "InvalidParameterValue", "Value ("+request.getAttribute( )+") for parameter attribute is invalid. Unknown network interface attribute"  );
        }
      }
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeNetworkInterfacesResponseType describeNetworkInterfaces( final DescribeNetworkInterfacesType request ) throws EucalyptusCloudException {
    final DescribeNetworkInterfacesResponseType reply = request.getReply( );
    describe(
        Identifier.eni,
        request.networkInterfaceIds(),
        request.getFilterSet( ),
        NetworkInterface.class,
        NetworkInterfaceType.class,
        reply.getNetworkInterfaceSet().getItem( ),
        NetworkInterfaceType.id( ),
        networkInterfaces );
    return reply;
  }

  public DescribeRouteTablesResponseType describeRouteTables( final DescribeRouteTablesType request ) throws EucalyptusCloudException {
    final DescribeRouteTablesResponseType reply = request.getReply( );
    describe(
        Identifier.rtb,
        request.routeTableIds( ),
        request.getFilterSet( ),
        RouteTable.class,
        RouteTableType.class,
        reply.getRouteTableSet( ).getItem( ),
        RouteTableType.id( ),
        routeTables );
    return reply;
  }

  public DescribeSubnetsResponseType describeSubnets( final DescribeSubnetsType request ) throws EucalyptusCloudException {
    final DescribeSubnetsResponseType reply = request.getReply( );
    describe(
        Identifier.subnet,
        request.subnetIds(),
        request.getFilterSet( ),
        Subnet.class,
        SubnetType.class,
        reply.getSubnetSet().getItem( ),
        SubnetType.id( ),
        subnets );
    return reply;
  }

  public DescribeVpcAttributeResponseType describeVpcAttribute(final DescribeVpcAttributeType request) throws EucalyptusCloudException {
    final DescribeVpcAttributeResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    try {
      final Vpc vpc =
          vpcs.lookupByName( accountFullName, Identifier.vpc.normalize( request.getVpcId( ) ), Functions.<Vpc>identity() );
      if ( RestrictedTypes.filterPrivileged( ).apply( vpc ) ) {
        reply.setVpcId( vpc.getDisplayName( ) );
        switch ( request.getAttribute( ) ) {
          case "enableDnsSupport":
            reply.setEnableDnsSupport( new AttributeBooleanValueType( ) );
            reply.getEnableDnsSupport( ).setValue( vpc.getDnsEnabled( ) );
            break;
          case "enableDnsHostnames":
            reply.setEnableDnsHostnames( new AttributeBooleanValueType( ) );
            reply.getEnableDnsHostnames( ).setValue( vpc.getDnsHostnames( ) );
            break;
          default:
            throw new ComputeServiceClientException( "InvalidParameterValue", "Value ("+request.getAttribute( )+") for parameter attribute is invalid. Unknown vpc attribute"  );
        }
      }
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeVpcPeeringConnectionsResponseType describeVpcPeeringConnections(DescribeVpcPeeringConnectionsType request) throws EucalyptusCloudException {
    DescribeVpcPeeringConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpcsResponseType describeVpcs( final DescribeVpcsType request ) throws EucalyptusCloudException {
    final DescribeVpcsResponseType reply = request.getReply( );
    describe(
        Identifier.vpc,
        request.vpcIds( ),
        request.getFilterSet( ),
        Vpc.class,
        VpcType.class,
        reply.getVpcSet( ).getItem( ),
        VpcType.id( ),
        vpcs );
    return reply;
  }

  public DescribeVpnConnectionsResponseType describeVpnConnections(DescribeVpnConnectionsType request) throws EucalyptusCloudException {
    DescribeVpnConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpnGatewaysResponseType describeVpnGateways(DescribeVpnGatewaysType request) throws EucalyptusCloudException {
    DescribeVpnGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  @Override
  public ComputeMessage onCall( final MuleEventContext muleEventContext ) throws EucalyptusCloudException {
    final ComputeMessage request = (ComputeMessage) muleEventContext.getMessage( ).getPayload( );
    LOG.debug(request.toSimpleString());

    // Dispatch
    try {
      BaseMessage backendRequest = BaseMessages.deepCopy( request, getBackendMessageClass( request ) );
      final BaseMessage backendResponse = send( backendRequest );
      final ComputeMessage response =
          (ComputeMessage) BaseMessages.deepCopy( backendResponse, request.getReply( ).getClass( ) );
      response.setCorrelationId( request.getCorrelationId() );
      LOG.debug(response.toSimpleString());
      return response;
    } catch ( Exception e ) {
      handleServiceException( e );
      Exceptions.findAndRethrow( e, EucalyptusWebServiceException.class, EucalyptusCloudException.class );
      throw new EucalyptusCloudException( e );
    }
  }

  private static <AP extends AbstractPersistent & CloudMetadata, AT extends VpcTagged> void describe(
      final Identifier identifier,
      final Collection<String> ids,
      final Collection<com.eucalyptus.compute.common.Filter> filters,
      final Class<AP> persistent,
      final Class<AT> api,
      final List<AT> results,
      final Function<AT,String> idFunction,
      final Lister<AP> lister ) throws EucalyptusCloudException {
    final boolean showAll = ids.remove( "verbose" );
    final Context ctx = Contexts.lookup( );
    final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
    final OwnerFullName ownerFullName = ctx.isAdministrator( ) && showAll ? null : accountFullName;
    final Filter filter = Filters.generate( filters, persistent );
    final Predicate<? super AP> requestedAndAccessible = CloudMetadatas.filteringFor( persistent )
        .byId( identifier.normalize( ids ) )
        .byPredicate( filter.asPredicate( ) )
        .byPrivileges()
        .buildPredicate();

    try {
      results.addAll( lister.list(
          ownerFullName,
          filter.asCriterion( ),
          filter.getAliases( ),
          requestedAndAccessible,
          TypeMappers.lookup( persistent, api ) ) );

      populateTags( accountFullName, persistent, results, idFunction );
    } catch ( Exception e ) {
      throw handleException( e );
    }
  }

  private static boolean canModifyImage( final ImageInfo imgInfo ) {
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    return
        ( ctx.isAdministrator( ) || imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) ) &&
            RestrictedTypes.filterPrivileged( ).apply( imgInfo );
  }

  private static <VT extends VpcTagged> void populateTags( final AccountFullName accountFullName,
                                                           final Class<? extends CloudMetadata> resourceType,
                                                           final List<? extends VT> items,
                                                           final Function<? super VT, String> idFunction ) {
    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( resourceType )
        .getResourceTagMap( accountFullName, Iterables.transform( items, idFunction ) );
    for ( final VT item : items ) {
      final ResourceTagSetType tags = new ResourceTagSetType( );
      Tags.addFromTags( tags.getItem(), ResourceTagSetItemType.class, tagsMap.get( idFunction.apply( item ) ) );
      if ( !tags.getItem().isEmpty() ) {
        item.setTagSet( tags );
      }
    }
  }

  private static String imageIdentifier( final String identifier ) throws EucalyptusCloudException {
    if( !CloudMetadatas.isImageIdentifier( identifier ) )
      throw new EucalyptusCloudException( "Invalid id: " + "\"" + identifier + "\"" );
    return normalizeImageIdentifier( identifier );
  }

  private static String normalizeInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.parse( VmInstance.ID_PREFIX, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException( "InvalidInstanceID.Malformed", "Invalid id: \""+e.getIdentifier()+"\"" );
    }
  }

  private static List<String> normalizeInstanceIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( VmInstance.ID_PREFIX, identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException( "InvalidInstanceID.Malformed", "Invalid id: \""+e.getIdentifier()+"\"" );
    }
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ComputeServiceClientException {
    try {
      return Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException( "InvalidParameterValue", String.format( message, e.getIdentifier( ) ) );
    }
  }

  private static String normalizeImageIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, null, true, "Value (%s) for parameter image is invalid." );
  }

  @Nullable
  private static String normalizeOptionalImageIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, null, false, "Value (%s) for parameter image is invalid." );
  }

  private static List<String> normalizeImageIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException(
          "InvalidParameterValue",
          "Value ("+e.getIdentifier()+") for parameter images is invalid." );
    }
  }

  private static List<String> normalizeVolumeIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( Volume.ID_PREFIX, identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException(
          "InvalidParameterValue",
          "Value ("+e.getIdentifier()+") for parameter volumes is invalid. Expected: 'vol-...'." );
    }
  }

  private static String normalizeSnapshotIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, Snapshot.ID_PREFIX, true, "Value (%s) for parameter snapshotId is invalid. Expected: 'snap-...'." );
  }

  private static List<String> normalizeSnapshotIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( Snapshot.ID_PREFIX, identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException(
          "InvalidParameterValue",
          "Value ("+e.getIdentifier()+") for parameter snapshots is invalid. Expected: 'snap-...'." );
    }
  }

  private enum Identifier {
    acl( "networkAcl" ),
    aclassoc( "networkAclAssociation" ),
    dopt( "DHCPOption" ),
    eni( "networkInterface" ),
    igw( "internetGateway" ),
    rtb( "routeTable" ),
    rtbassoc( "routeTableAssociation" ),
    subnet( "subnet" ),
    vpc( "vpc" ),
    ;

    private final String code;
    private final String defaultParameter;
    private final String defaultListParameter;

    Identifier( final String defaultParameter ) {
      this( defaultParameter, defaultParameter + "s" );
    }

    Identifier( final String defaultParameter, final String defaultListParameter ) {
      this.code = "InvalidParameterValue";
      this.defaultParameter = defaultParameter;
      this.defaultListParameter = defaultListParameter;
    }

    public String generate( ) {
      return ResourceIdentifiers.generateString( name( ) );
    }

    public String normalize( final String identifier ) throws EucalyptusCloudException {
      return normalize( identifier, defaultParameter );
    }

    public String normalize( final String identifier, final String parameter ) throws EucalyptusCloudException {
      return normalize( Collections.singleton( identifier ), parameter ).get( 0 );
    }

    public List<String> normalize( final Iterable<String> identifiers ) throws EucalyptusCloudException {
      return normalize( identifiers, defaultListParameter );
    }

    public List<String> normalize( final Iterable<String> identifiers,
                                   final String parameter ) throws EucalyptusCloudException {
      try {
        return ResourceIdentifiers.normalize( name( ), identifiers );
      } catch ( final InvalidResourceIdentifier e ) {
        throw new ComputeServiceClientException(
            code,
            "Value ("+e.getIdentifier()+") for parameter "+parameter+" is invalid. Expected: '"+name()+"-...'." );
      }
    }
  }

  private static List<String> normalizeGroupIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( NetworkGroup.ID_PREFIX, identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ComputeServiceClientException(
          "InvalidGroupId.Malformed",
          "Invalid id: \""+e.getIdentifier()+"\" (expecting \"sg-...\")" );
    }
  }

  private enum SecurityGroupItemToGroupId implements Function<SecurityGroupItemType, String> {
    INSTANCE {
      @Override
      public String apply( SecurityGroupItemType securityGroupItemType ) {
        return securityGroupItemType.getGroupId();
      }
    }
  }

  private static Class getBackendMessageClass( final BaseMessage request ) throws BindingException {
    final Binding binding = BindingManager.getDefaultBinding( );
    return binding.getElementClass( "Eucalyptus." + request.getClass( ).getSimpleName( ) );
  }

  private static BaseMessage send( final BaseMessage request ) throws Exception {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( Topology.lookup( Eucalyptus.class ), request );
    } catch ( final NoSuchElementException e ) {
      throw new ComputeServiceUnavailableException( "Service Unavailable" );
    } catch ( final ServiceDispatchException e ) {
      final ComponentException componentException = Exceptions.findCause( e, ComponentException.class );
      if ( componentException != null && componentException.getCause( ) instanceof Exception ) {
        throw (Exception) componentException.getCause( );
      }
      throw e;
    } catch ( final FailedRequestException e ) {
      if ( request.getReply( ).getClass( ).isInstance( e.getRequest( ) ) ) {
        return e.getRequest( );
      }
      throw e.getRequest( ) == null ?
          e :
          new ComputeServiceException( "InternalError", "Internal error " + e.getRequest().getClass().getSimpleName() + ":false" );
    }
  }

  @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
  private void handleServiceException( final Exception e ) throws EucalyptusCloudException {
    final Optional<AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( e );
    if ( serviceErrorOption.isPresent( ) ) {
      final AsyncWebServiceError serviceError = serviceErrorOption.get( );
      switch( serviceError.getHttpErrorCode() ) {
        case 400:
          throw new ComputeServiceClientException( serviceError.getCode(), serviceError.getMessage( ) );
        case 403:
          throw new ComputeServiceAuthorizationException( serviceError.getCode(), serviceError.getMessage( ) );
        case 503:
          throw new ComputeServiceUnavailableException( serviceError.getMessage( ) );
        default:
          throw new ComputeServiceException( serviceError.getCode( ), serviceError.getMessage( ) );
      }
    }
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static ComputeServiceException handleException( final Exception e  ) throws ComputeServiceException {
    final ComputeServiceException cause = Exceptions.findCause( e, ComputeServiceException.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      String code = "ResourceLimitExceeded";
      switch( quotaCause.getType( ) ) {
        case "vpc":
          code = "VpcLimitExceeded";
          break;
        case "internet-gateway":
          code = "InternetGatewayLimitExceeded";
          break;
      }
      throw new ComputeServiceClientException( code, "Request would exceed quota for type: " + quotaCause.getType() );
    }

    LOG.error( e, e );

    final ComputeServiceException exception = new ComputeServiceException( "InternalError", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}
