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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm;

import static com.eucalyptus.cloud.run.VerifyMetadata.ImageInstanceTypeVerificationException;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.util.IllegalMetadataAccessException;
import com.eucalyptus.cloud.util.InvalidParameterCombinationMetadataException;
import com.eucalyptus.cloud.util.NoSuchImageIdException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cloud.util.SecurityGroupLimitMetadataException;
import com.eucalyptus.compute.ClientUnauthorizedComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.backend.ReportInstanceStatusResponseType;
import com.eucalyptus.compute.common.backend.ReportInstanceStatusType;
import com.eucalyptus.compute.identifier.InvalidResourceIdentifier;
import com.eucalyptus.cloud.VmInstanceLifecycleHelpers;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.vpc.NetworkInterface;
import com.eucalyptus.compute.vpc.NoSuchSubnetMetadataException;
import com.eucalyptus.compute.vpc.VpcRequiredMetadataException;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.KernelImageInfo;
import com.eucalyptus.images.RamdiskImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.keys.NoSuchKeyMetadataException;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Joiner;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.run.AdmissionControl;
import com.eucalyptus.cloud.run.Allocations;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.run.ClusterAllocator;
import com.eucalyptus.cloud.run.ContractEnforcement;
import com.eucalyptus.cloud.run.VerifyMetadata;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.callback.RebootCallback;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.tracking.MessageContexts;
import com.eucalyptus.tags.Filter;
import com.eucalyptus.tags.Filters;
import com.eucalyptus.tags.Tag;
import com.eucalyptus.tags.TagSupport;
import com.eucalyptus.tags.Tags;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.vm.Bundles.BundleCallback;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.eucalyptus.compute.common.InstanceBlockDeviceMappingItemType;
import com.eucalyptus.compute.common.InstanceStatusItemType;
import com.eucalyptus.compute.common.MonitorInstanceState;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.TerminateInstancesItemType;
import com.eucalyptus.compute.common.InstanceBlockDeviceMapping;
import com.eucalyptus.compute.common.backend.CreatePlacementGroupResponseType;
import com.eucalyptus.compute.common.backend.CreatePlacementGroupType;
import com.eucalyptus.compute.common.backend.DeletePlacementGroupResponseType;
import com.eucalyptus.compute.common.backend.DeletePlacementGroupType;
import com.eucalyptus.compute.common.backend.DescribeInstanceAttributeResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstanceAttributeType;
import com.eucalyptus.compute.common.backend.DescribeInstanceStatusResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstanceStatusType;
import com.eucalyptus.compute.common.backend.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstancesType;
import com.eucalyptus.compute.common.backend.DescribePlacementGroupsResponseType;
import com.eucalyptus.compute.common.backend.DescribePlacementGroupsType;
import com.eucalyptus.compute.common.backend.GetConsoleOutputResponseType;
import com.eucalyptus.compute.common.backend.GetConsoleOutputType;
import com.eucalyptus.compute.common.backend.GetPasswordDataResponseType;
import com.eucalyptus.compute.common.backend.GetPasswordDataType;
import com.eucalyptus.compute.common.backend.ModifyInstanceAttributeResponseType;
import com.eucalyptus.compute.common.backend.ModifyInstanceAttributeType;
import com.eucalyptus.compute.common.backend.MonitorInstancesResponseType;
import com.eucalyptus.compute.common.backend.MonitorInstancesType;
import com.eucalyptus.compute.common.backend.RebootInstancesResponseType;
import com.eucalyptus.compute.common.backend.RebootInstancesType;
import com.eucalyptus.compute.common.backend.ResetInstanceAttributeResponseType;
import com.eucalyptus.compute.common.backend.ResetInstanceAttributeType;
import com.eucalyptus.compute.common.backend.RunInstancesResponseType;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.compute.common.backend.StartInstancesResponseType;
import com.eucalyptus.compute.common.backend.StartInstancesType;
import com.eucalyptus.compute.common.backend.StopInstancesResponseType;
import com.eucalyptus.compute.common.backend.StopInstancesType;
import com.eucalyptus.compute.common.backend.TerminateInstancesResponseType;
import com.eucalyptus.compute.common.backend.TerminateInstancesType;
import com.eucalyptus.compute.common.backend.UnmonitorInstancesResponseType;
import com.eucalyptus.compute.common.backend.UnmonitorInstancesType;
import edu.ucsb.eucalyptus.msgs.ClusterGetConsoleOutputResponseType;
import edu.ucsb.eucalyptus.msgs.ClusterGetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.ClusterRebootInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.ClusterRebootInstancesType;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;


@SuppressWarnings( "UnusedDeclaration" )
public class VmControl {
  
  private static Logger LOG = Logger.getLogger( VmControl.class );
  
  public static RunInstancesResponseType runInstances( RunInstancesType request ) throws Exception {
    RunInstancesResponseType reply = request.getReply( );
    Allocation allocInfo = Allocations.run( request );
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      if ( !Strings.isNullOrEmpty( allocInfo.getClientToken() ) ) {
        final List<VmInstance> instances = VmInstances.listByClientToken(
            allocInfo.getOwnerFullName( ).asAccountFullName(),
            allocInfo.getClientToken(), RestrictedTypes.filterPrivileged() );
        if ( !instances.isEmpty() ) {
          final VmInstance vm = instances.get( 0 );
          final ReservationInfoType reservationInfoType = TypeMappers.transform( vm, ReservationInfoType.class );
          for ( final VmInstance instance : instances ) {
            reservationInfoType.getInstancesSet().add( VmInstances.transform( instance ) );
          }
          reply.setRsvInfo( reservationInfoType );
          return reply;
        }
      }

      // VerifyMetadata set some data that is used by AdmissionControl so it have to precede AdmissionControl
      // AdmissionControl sets final partition selection, so some partition related checks has be run after that
      Predicates.and( VerifyMetadata.getVerifiers( ), AdmissionControl.run( ),
                      ContractEnforcement.run(), VerifyMetadata.getPostVerifiers()).apply( allocInfo );
      allocInfo.commit( );

      ReservationInfoType reservation = new ReservationInfoType(
          allocInfo.getReservationId( ),
          allocInfo.getOwnerFullName( ).getAccountNumber( ),
          Collections2.transform(
              allocInfo.getNetworkGroups( ),
              TypeMappers.lookup( NetworkGroup.class, GroupItemType.class ) ) );

      reply.setRsvInfo( reservation );
      for ( ResourceToken allocToken : allocInfo.getAllocationTokens( ) ) {
        reservation.getInstancesSet( ).add( VmInstances.transform( allocToken.getVmInstance( ) ) );
      }
      db.commit( );
    } catch ( Exception ex ) {
      allocInfo.abort( );
      final ImageInstanceTypeVerificationException e1 = Exceptions.findCause( ex, ImageInstanceTypeVerificationException.class );
      if ( e1 != null ) throw new ClientComputeException( "InvalidParameterCombination", e1.getMessage( ) );
      final NotEnoughResourcesException e2 = Exceptions.findCause( ex, NotEnoughResourcesException.class );
      if ( e2 != null ) throw new ComputeException( "InsufficientInstanceCapacity", e2.getMessage( ) );
      final NoSuchKeyMetadataException e3 = Exceptions.findCause( ex, NoSuchKeyMetadataException.class );
      if ( e3 != null ) throw new ClientComputeException( "InvalidKeyPair.NotFound", e3.getMessage( ) );
      final InvalidMetadataException e4 = Exceptions.findCause( ex, InvalidMetadataException.class );
      if ( e4 instanceof VpcRequiredMetadataException ) {
        throw new ClientComputeException( "VPCIdNotSpecified", "Default VPC not found, please specify a subnet." );
      }
      if ( e4 instanceof InvalidParameterCombinationMetadataException ) {
        throw new ClientComputeException( "InvalidParameterCombination", e4.getMessage( ) );
      }
      if ( e4 != null ) throw new ClientComputeException( "InvalidParameterValue", e4.getMessage( ) );
      final NoSuchImageIdException e5 = Exceptions.findCause( ex, NoSuchImageIdException.class );
      if ( e5 != null ) throw new ClientComputeException( "InvalidAMIID.NotFound", e5.getMessage( ) );
      final NoSuchSubnetMetadataException e6 = Exceptions.findCause( ex, NoSuchSubnetMetadataException.class );
      if ( e6 != null ) throw new ClientComputeException( "InvalidSubnetID.NotFound", e6.getMessage( ) );
      final IllegalMetadataAccessException e7 = Exceptions.findCause( ex, IllegalMetadataAccessException.class );
      if ( e7 != null ) throw new ClientUnauthorizedComputeException( e7.getMessage( ) );
      final SecurityGroupLimitMetadataException e8 = Exceptions.findCause( ex, SecurityGroupLimitMetadataException.class );
      if ( e8 != null ) throw new ClientComputeException( "SecurityGroupLimitExceeded", "Security group limit exceeded" );
      LOG.error( ex, ex );
      throw ex;
    } finally {
      if ( db.isActive() ) db.rollback();
    }

    MessageContexts.remember(allocInfo.getReservationId(), request.getClass(), request);
    for( final ResourceToken allocToken : allocInfo.getAllocationTokens()){
      MessageContexts.remember(allocToken.getInstanceId(), request.getClass(), request);
    }
    
    ClusterAllocator.get( ).apply( allocInfo );
    return reply;
  }

  public DescribeInstancesResponseType describeInstances( final DescribeInstancesType msg ) throws EucalyptusCloudException {
    final DescribeInstancesResponseType reply = msg.getReply( );
    Context ctx = Contexts.lookup( );
    boolean showAll = msg.getInstancesSet( ).remove( "verbose" ) || !msg.getInstancesSet( ).isEmpty( );
    final Multimap<String, RunningInstancesItemType> instanceMap = TreeMultimap.create();
    final Map<String, ReservationInfoType> reservations = Maps.newHashMap();
    final Collection<String> identifiers = normalizeIdentifiers( msg.getInstancesSet() );
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class )
        .withOptionalInternalFilter( "instance-id", identifiers )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( identifiers ) // filters without wildcard support
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    final Criterion criterion = filter.asCriterionWithConjunction( Restrictions.not( VmInstances.criterion( VmState.BURIED ) ) );
    final OwnerFullName ownerFullName = ( ctx.isAdministrator( ) && showAll )
      ? null
      : ctx.getUserFullName( ).asAccountFullName( );
    try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, criterion, filter.getAliases(), requestedAndAccessible );
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( VmInstance.class )
          .getResourceTagMap( AccountFullName.getInstance( ctx.getAccount() ),
              Iterables.transform( instances, CloudMetadatas.toDisplayName() ) );

      for ( final VmInstance vm : instances ) {
        if ( instanceMap.put( vm.getReservationId( ), VmInstances.transform( vm ) ) && !reservations.containsKey( vm.getReservationId( ) ) ) {
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
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }

  public DescribeInstanceStatusResponseType describeInstanceStatus( final DescribeInstanceStatusType msg ) throws EucalyptusCloudException {
    final DescribeInstanceStatusResponseType reply = msg.getReply( );
    final Context ctx = Contexts.lookup();
    final boolean showAll = msg.getInstancesSet( ).remove( "verbose" ) || !msg.getInstancesSet( ).isEmpty( );
    final boolean includeAllInstances = Objects.firstNonNull( msg.getIncludeAllInstances(), Boolean.FALSE );
    final Collection<String> identifiers = normalizeIdentifiers( msg.getInstancesSet() );
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class, "status" )
        .withOptionalInternalFilter( "instance-id", identifiers )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( identifiers ) // filters without wildcard support
        .byPredicate( includeAllInstances ? Predicates.<VmInstance>alwaysTrue() : VmState.RUNNING )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    final Criterion criterion = filter.asCriterionWithConjunction( Restrictions.not( VmInstances.criterion( VmState.BURIED ) ) );
    final OwnerFullName ownerFullName = ( ctx.isAdministrator( ) && showAll )
        ? null
        : ctx.getUserFullName( ).asAccountFullName( );
    try {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, criterion, filter.getAliases(), requestedAndAccessible );

      Iterables.addAll(
          reply.getInstanceStatusSet().getItem(),
          Iterables.transform( instances, TypeMappers.lookup( VmInstance.class, InstanceStatusItemType.class ) ) );

    } catch ( final Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }

  public ReportInstanceStatusResponseType reportInstanceStatus( final ReportInstanceStatusType request ) {
    return request.getReply( );
  }

  public TerminateInstancesResponseType terminateInstances( final TerminateInstancesType request ) throws EucalyptusCloudException {
    final TerminateInstancesResponseType reply = request.getReply( );
    final List<String> failedVmList = new ArrayList<>( );
    final List<VmInstance> vmList = new ArrayList<>(  );
    final Collection<String> identifiers = normalizeIdentifiers( request.getInstancesSet( ) );
    for ( String requestedInstanceId : identifiers ) {
      try {
        VmInstance vm = RestrictedTypes.doPrivileged( requestedInstanceId, VmInstance.class );
        vmList.add( vm );
      } catch ( final AuthException | NoSuchElementException e ) {
        failedVmList.add( requestedInstanceId );
      } catch ( final Exception e ) {
        LOG.error( "Error looking up instance for termination: " + requestedInstanceId, e );
        failedVmList.add( requestedInstanceId );
      }
    }
    if ( !failedVmList.isEmpty( ) ) {
      if ( failedVmList.size( ) > 1 )
        throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance IDs '" + Joiner.on( ", " ).join( failedVmList ) +"' do not exist" );
      else
        throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '" + Joiner.on( ", " ).join( failedVmList ) +"' does not exist" );
    }
    try {
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Function<VmInstance,TerminateInstancesItemType> terminateFunction = new Function<VmInstance,TerminateInstancesItemType>( ) {
        @Override
        public TerminateInstancesItemType apply( final VmInstance vm ) {
          String oldState, newState = null;
          int oldCode, newCode = 0;
          TerminateInstancesItemType result = null;
          try {
            if ( MigrationState.isMigrating( vm ) ) {
              throw Exceptions.toUndeclared( "Cannot terminate an instance which is currently migrating: "
                                             + vm.getInstanceId( )
                                             + " "
                                             + vm.getMigrationTask( ) );
            }
            oldCode = vm.getState( ).getCode( );
            oldState = vm.getState( ).getName( );
            if ( VmState.STOPPED.apply( vm ) ) {
              newCode = VmState.TERMINATED.getCode( );
              newState = VmState.TERMINATED.getName( );
              VmInstances.terminated( vm );
            } else if ( VmStateSet.RUN.apply( vm ) ) {
              newCode = VmState.SHUTTING_DOWN.getCode( );
              newState = VmState.SHUTTING_DOWN.getName( );
              VmInstances.shutDown( vm );
            } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
              newCode = VmState.SHUTTING_DOWN.getCode( );
              newState = VmState.SHUTTING_DOWN.getName( );
            } else if ( VmStateSet.DONE.apply( vm ) ) {
              oldCode = newCode = VmState.TERMINATED.getCode( );
              oldState = newState = VmState.TERMINATED.getName( );
              VmInstances.buried( vm );
            }
            MessageContexts.remember(vm.getInstanceId(), request.getClass(), request);
            result = new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState );
          } catch ( final TerminatedInstanceException e ) {
            oldCode = newCode = VmState.TERMINATED.getCode( );
            oldState = newState = VmState.TERMINATED.getName( );
            try {
              VmInstances.buried( vm.getInstanceId( ) );
            } catch ( TransactionException e1 ) {
              throw Exceptions.toUndeclared( e1 );
            }
            result = new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState );
          } catch ( final NoSuchElementException e ) {
            LOG.debug( "Ignoring terminate request for non-existent instance: " + vm.getInstanceId( ) );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
          return result;
        }
      };
      Function<VmInstance, TerminateInstancesItemType> terminateTx = Entities.asTransaction( VmInstance.class, terminateFunction, VmInstances.TX_RETRIES );
      for ( VmInstance vm : vmList ) {
        try {
          TerminateInstancesItemType termInstance = terminateTx.apply( vm );
          if ( termInstance != null ) {
            results.add( termInstance );
          }
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
      }
      reply.set_return( !reply.getInstancesSet( ).isEmpty( ) );
      return reply;
    } catch ( final Throwable e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public RebootInstancesResponseType rebootInstances( final RebootInstancesType request ) throws EucalyptusCloudException {
    final RebootInstancesResponseType reply = request.getReply( );
    try {
        List <String> instanceSet = normalizeIdentifiers( request.getInstancesSet() );
        ArrayList <String> noAccess = new ArrayList<>();
        ArrayList <String> migrating = new ArrayList<>();
        ArrayList <String> noSuchElement = new ArrayList<>();
        for( int i = 0; i < instanceSet.size(); i++) {
          String currentInstance = instanceSet.get(i);
          try {
            final VmInstance v = VmInstances.lookup(  currentInstance );
            if( !RestrictedTypes.filterPrivileged( ).apply( v ) ) {
              noAccess.add( currentInstance );
            }
            if( MigrationState.isMigrating( v ) ) {
              migrating.add( currentInstance );
            }
          } catch (NoSuchElementException nse) {
            if( !( nse instanceof TerminatedInstanceException ) ) {
              noSuchElement.add( currentInstance );
            } else {
              instanceSet.remove(i--);
            }
          }
          if( ( i == instanceSet.size( ) - 1 ) && ( !noSuchElement.isEmpty( ) ) ) {
            String outList = noSuchElement.toString();
            throw new EucalyptusCloudException( "No such instance(s): " + outList.substring( 1, outList.length( ) - 1 ) );
          } else if( ( i == instanceSet.size( ) - 1 ) && ( !noAccess.isEmpty( ) ) ) {
            String outList = noAccess.toString( );
            throw new EucalyptusCloudException( "Permission denied for vm(s): " + outList.substring( 1, outList.length( ) - 1 ) );
          } else if( ( i == instanceSet.size( ) - 1 ) && ( !migrating.isEmpty( ) ) ) {
            String outList = noAccess.toString( );
            throw new EucalyptusCloudException( "Cannot reboot an instances which is currently migrating: " + outList.substring( 1, outList.length( ) - 1 ) );
          }
        }
        final boolean result = Iterables.all( instanceSet , new Predicate<String>( ) {
        @Override
        public boolean apply( final String instanceId ) {
          try {
            final VmInstance v = VmInstances.lookup( instanceId );
              final Request<ClusterRebootInstancesType, ClusterRebootInstancesResponseType> req =
                  AsyncRequests.newRequest( new RebootCallback( v.getInstanceId( ) ) );
              ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, v.lookupPartition( ) );
              req.dispatch( ccConfig );
              return true;
          } catch ( final NoSuchElementException e ) {
            return false;
          }
        }
      } );
      reply.set_return( result );
      return reply;
    } catch ( final Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public GetConsoleOutputResponseType getConsoleOutput( final GetConsoleOutputType request ) throws EucalyptusCloudException {
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    VmInstance v;
    try {
      v = VmInstances.lookup( instanceId );
    } catch ( final NoSuchElementException ex ) {
      throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '"+instanceId+"' does not exist" );
    }
    if ( !RestrictedTypes.filterPrivileged( ).apply( v ) ) {
      throw new EucalyptusCloudException( "Permission denied for vm: " + instanceId );
    } else if ( !VmState.RUNNING.apply( v ) ) {
      throw new EucalyptusCloudException( "Instance " + instanceId + " is not in a running state." );
    } else {
      Cluster cluster;
      try {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, v.lookupPartition( ) );
        cluster = Clusters.lookup( ccConfig );
      } catch ( final NoSuchElementException e1 ) {
        throw new EucalyptusCloudException( "Failed to find cluster info for '" + v.getPartition( ) + "' related to vm: " + instanceId );
      }
      try {
        final ClusterGetConsoleOutputResponseType response =
            AsyncRequests.sendSync( cluster.getConfiguration( ), new ClusterGetConsoleOutputType( instanceId ) );
        GetConsoleOutputResponseType reply = request.getReply();
        reply.setInstanceId( instanceId );
        reply.setTimestamp( response.getTimestamp() );
        reply.setOutput( response.getOutput() );
        return reply;
      } catch( Exception e ) {
        LOG.error( e, e );
        throw new ComputeException( "InternalError", "Error processing request: " + e.getMessage( ) );
      }
    }
  }
  
  public DescribeBundleTasksResponseType describeBundleTasks( final DescribeBundleTasksType request ) throws EucalyptusCloudException {
    final DescribeBundleTasksResponseType reply = request.getReply( );

    final boolean showAll = request.getBundleIds( ).remove( "verbose" ) || !request.getBundleIds( ).isEmpty( );
    final Filter filter = Filters.generate( request.getFilterSet(), VmBundleTask.class );
    try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
      
      // Get all from cache that match filters......
      final Predicate<? super VmBundleTask> filteredAndBundling = 
          Predicates.and(filter.asPredicate(), VmBundleTask.Filters.BUNDLING);
      Collection<VmBundleTask> cachedValues = Bundles.getPreviousBundleTasks().values();
      final Map<String, VmBundleTask> cachedBundles = buildMap(Collections2.filter(cachedValues, filteredAndBundling));
      final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
          .byId( toInstanceIds( request.getBundleIds( ) ) )
          .byPrivileges()
          .buildPredicate();
      // Get all from the db that are owned
      
      final Predicate<? super VmInstance> filteredInstances = 
          Predicates.compose( filter.asPredicate(), VmInstances.bundleTask() );
      final Filter noFilters = Filters.generate( new ArrayList<com.eucalyptus.compute.common.Filter>(), VmBundleTask.class );
      final Context ctx = Contexts.lookup();
      final OwnerFullName ownerFullName = ( ctx.isAdministrator( ) && showAll )
          ? null
          : ctx.getUserFullName( ).asAccountFullName( );
      final Collection<VmInstance> dbBundles =
          VmInstances.list( ownerFullName, noFilters.asCriterion(), noFilters.getAliases(), requestedAndAccessible );
      for ( final VmInstance v : dbBundles) {
        
        if ( filteredInstances.apply(v) && VmInstance.Filters.BUNDLING.apply(v)) {
          LOG.debug("Getting current bundle for " + v.getInstanceId());
          reply.getBundleTasks( ).add( Bundles.transform( v.getRuntimeState( ).getBundleTask( ) ) );
        } else {
          if ( !VmInstance.Filters.BUNDLING.apply(v) && cachedBundles.containsKey(v.getInstanceId())) {
            LOG.debug("Getting previous bundle for " + v.getInstanceId());
            reply.getBundleTasks( ).add( Bundles.transform( cachedBundles.get(v.getInstanceId())));
          }
        }
      }
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new EucalyptusCloudException( ex );
    }
    return reply;
  }

  private static Map<String, VmBundleTask> buildMap(Collection<VmBundleTask> tasks) {
    Map<String, VmBundleTask> map = Maps.newHashMap();
    for (VmBundleTask task: tasks) {
      map.put(task.getInstanceId(),  task);
    }
    return map;
  }
  
  public UnmonitorInstancesResponseType unmonitorInstances( final UnmonitorInstancesType request ) throws EucalyptusCloudException {
    final UnmonitorInstancesResponseType reply = request.getReply();
    final List<String> instanceSet = normalizeIdentifiers( request.getInstancesSet( ) );
    final List<MonitorInstanceState> monitorFalseList = Lists.newArrayList( );

    for ( final String inst : instanceSet ) {
      final MonitorInstanceState monitorInstanceState = new MonitorInstanceState();
      monitorInstanceState.setInstanceId( inst );
      monitorInstanceState.setMonitoringState( "disabled" );
      monitorFalseList.add( monitorInstanceState );
    }

    reply.setInstancesSet( SetMonitorFunction.INSTANCE.apply( monitorFalseList ) );
    return reply;
  }
  
  public StartInstancesResponseType startInstances( final StartInstancesType request ) throws Exception {
    final StartInstancesResponseType reply = request.getReply( );
    for ( String instanceId : normalizeIdentifiers( request.getInstancesSet() ) ) {
      try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {//scope for transaction
        final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
        if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
          Allocation allocInfo = Allocations.start( vm );
          VmInstanceLifecycleHelpers.get( ).prepareAllocation( vm, allocInfo );
          try {//scope for allocInfo
            AdmissionControl.run( ).apply( allocInfo );
            for ( final ResourceToken resourceToken : allocInfo.getAllocationTokens( ) ) {
              VmInstanceLifecycleHelpers.get( ).startVmInstance( resourceToken, vm );
            }
            final int oldCode = vm.getState( ).getCode( );
            final int newCode = VmState.PENDING.getCode( );
            final String oldState = vm.getState( ).getName( );
            final String newState = VmState.PENDING.getName( );
            vm.setState( VmState.PENDING );
            db.commit( );
            MessageContexts.remember(instanceId, request.getClass(), request);
            ClusterAllocator.get( ).apply( allocInfo );
            reply.getInstancesSet( ).add( new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState ) );
          } catch ( Exception ex ) {
            db.rollback( );
            allocInfo.abort( );
            throw ex;
          }
        }
      } catch ( Exception ex1 ) {
        LOG.trace( ex1, ex1 );
        throw ex1;
      }
    }
    return reply;
  }
  
  public StopInstancesResponseType stopInstances( final StopInstancesType request ) throws EucalyptusCloudException {
    final StopInstancesResponseType reply = request.getReply( );
    try {
      final Context ctx = Contexts.lookup( );
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Predicate<String> stopPredicate = new Predicate<String>( ) {
        @Override
        public boolean apply( final String instanceId ) {
          try {
            final VmInstance v = VmInstances.lookup( instanceId );
            if ( RestrictedTypes.filterPrivileged( ).apply( v ) ) {
              if ( !MigrationState.isMigrating( v ) && v.getBootRecord( ).getMachine( ) instanceof BlockStorageImageInfo ) {
                final int oldCode = v.getState( ).getCode( ), newCode = VmState.STOPPING.getCode( );
                final String oldState = v.getState( ).getName( ), newState = VmState.STOPPING.getName( );
                TerminateInstancesItemType termInfo = new TerminateInstancesItemType( v.getInstanceId( ), oldCode, oldState, newCode, newState );
                if ( !results.contains( termInfo ) ) {
                  results.add( termInfo );
                }
                VmInstances.stopped( v );
              }
            }
            MessageContexts.remember(instanceId, request.getClass(), request);
            return true;//GRZE: noop needs to be true to continue Iterables.all
          } catch ( final NoSuchElementException e ) {
            try {
              VmInstances.stopped( instanceId );
              return true;
            } catch ( final NoSuchElementException e1 ) {
              return true;
            } catch ( TransactionException ex ) {
              Logs.extreme( ).error( ex, ex );
              return true;
            }
          } catch ( Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            throw Exceptions.toUndeclared( ex );
          }
        }
      };

      final List<String> identifiers = normalizeIdentifiers( request.getInstancesSet( ) );
      for( final String instanceId : identifiers ){
        try {
          final VmInstance vm = VmInstances.lookup( instanceId );
          if ( !vm.isBlockStorage() )
            throw new ClientComputeException( "UnsupportedOperation",
                String.format( "The instance '%s' does not have an 'ebs' root device type and cannot be stopped.", instanceId ) );
        } catch ( final TerminatedInstanceException ex ) {
          throw new ClientComputeException( "IncorrectInstanceState",
              String.format( "This instance '%s' is not in a state from which it can be stopped.", instanceId ) );
        } catch ( final NoSuchElementException ex ) {
          throw new ClientComputeException( "InvalidInstanceID.NotFound",
              String.format( "The instance ID '%s' does not exist", instanceId ) );
        }
      }

      for(final String instanceId : identifiers){
        final VmInstance vm = VmInstances.lookup( instanceId );
        // EUCA-9596: forget windows password
        if(ImageMetadata.Platform.windows.name().equals(vm.getPlatform())){
          try ( final TransactionResource db =
              Entities.transactionFor( VmInstance.class )){
            try{
              final VmInstance updatedVm = Entities.uniqueResult(vm);
              updatedVm.updatePasswordData(null);
              Entities.persist(updatedVm);
              db.commit();
            }catch(final Exception ex){
              throw new EucalyptusCloudException("Failed to erase Windows password");
            }
          }
        }   
      }
      
      Predicate<String> stopTx = Entities.asTransaction( VmInstance.class, stopPredicate );
      Iterables.all( identifiers, stopTx );
      reply.set_return( !reply.getInstancesSet( ).isEmpty( ) );
      return reply;
    } catch( final EucalyptusCloudException ex){ 
    	throw ex;
    } catch ( final Throwable e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }

  public ResetInstanceAttributeResponseType resetInstanceAttribute( final ResetInstanceAttributeType request )
          throws EucalyptusCloudException {
    final ResetInstanceAttributeResponseType reply = request.getReply( );
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    
    try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
      final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
      if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
        if ( request.getAttribute( ).equals( "kernel" ) ) {
          String kernelId = vm.getKernelId( );
          if ( kernelId == null ) {
            vm.getBootRecord( ).setKernel( );
          } else {
            KernelImageInfo kernelImg = Images.lookupKernel( kernelId );
            if ( !ImageMetadata.State.available.equals( kernelImg.getState( ) ) ) {
              throw new NoSuchElementException( "InvalidAMIID.NotFound: Unable to start instance with deregistered/failed image : " + kernelImg.getImageName( ) );
            }
            vm.getBootRecord( ).setKernel( kernelImg );
          }
          Entities.merge( vm );
          tx.commit( );
        } else if ( request.getAttribute( ).equals( "ramdisk" ) ) {
          String ramdiskId = vm.getRamdiskId( );
          if ( ramdiskId == null ) {
            vm.getBootRecord( ).setRamdisk( );
          } else {
            RamdiskImageInfo ramdiskImg = Images.lookupRamdisk( ramdiskId );
            if ( !ImageMetadata.State.available.equals( ramdiskImg.getState( ) ) ) {
              throw new NoSuchElementException( "InvalidAMIID.NotFound: Unable to start instance with deregistered/failed image : " + ramdiskImg.getImageName( ) );
            }
            vm.getBootRecord( ).setRamdisk( ramdiskImg );
          }
          Entities.merge( vm );
          tx.commit( );
        } // SourceDestCheck not implemented
        reply.set_return( true );
      } else {
        throw new EucalyptusCloudException( "IncorrectInstanceState: The instance '" + instanceId + "' is not in the 'stopped' state." );
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      if ( Exceptions.isCausedBy( ex, EucalyptusCloudException.class ) ) {
        throw new ClientComputeException( "IncorrectInstanceState", "The instance '" + instanceId + "' is not in the 'stopped' state." );
      } else if ( Exceptions.isCausedBy( ex, NoSuchElementException.class ) && ex.toString( ).contains( "InvalidAMIID.NotFound" ) ) {
        throw new ClientComputeException( "InvalidAMIID.NotFound", "The default " + request.getAttribute( ) + " does not exist" );
      }
      throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist" );
    }
    return reply;
  }
  
  public MonitorInstancesResponseType monitorInstances( final MonitorInstancesType request ) throws EucalyptusCloudException {    
      final MonitorInstancesResponseType reply = request.getReply();
      final List<String> instanceSet = normalizeIdentifiers( request.getInstancesSet() );
      final List<MonitorInstanceState> monitorTrueList = Lists.newArrayList();
      
      for(final String inst : instanceSet) {
        final MonitorInstanceState monitorInstanceState = new MonitorInstanceState();
        monitorInstanceState.setInstanceId(inst);
        monitorInstanceState.setMonitoringState("enabled");
        monitorTrueList.add(monitorInstanceState);
      }
      
      reply.setInstancesSet(SetMonitorFunction.INSTANCE.apply( monitorTrueList ) );
      return reply;
  }

  private enum SetMonitorFunction implements Function<List<MonitorInstanceState>, ArrayList<MonitorInstanceState>> {
    INSTANCE;

    @Override
    public ArrayList<MonitorInstanceState> apply( final List<MonitorInstanceState> monitorList ) {
      final ArrayList<MonitorInstanceState> monitorInstanceSet = Lists.newArrayList();
      for ( final MonitorInstanceState monitorInst : monitorList ) {
        try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
          final VmInstance vmInst = VmInstances.lookup( monitorInst.getInstanceId() );
          if ( RestrictedTypes.filterPrivileged().apply( vmInst ) ) {
            vmInst.getBootRecord( ).setMonitoring( "enabled".equals( monitorInst.getMonitoringState( ) ) );
            monitorInstanceSet.add( monitorInst );
            db.commit();
          }
        } catch ( final NoSuchElementException nse ) {
          LOG.debug( "Unable to find instance : " + monitorInst.getInstanceId() );
        } catch ( final Exception ex ) {
          LOG.debug( "Unable to set monitoring state for instance : " + monitorInst.getInstanceId( ), ex );
        }
      }
      return monitorInstanceSet;
    }
  }

  public ModifyInstanceAttributeResponseType modifyInstanceAttribute( final ModifyInstanceAttributeType request )
          throws EucalyptusCloudException, NoSuchMetadataException {
    final ModifyInstanceAttributeResponseType reply = request.getReply( );
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    Context ctx = Contexts.lookup( );

    try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
      final VmInstance vm;
      try {
        vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
      } catch ( AuthException | NoSuchElementException e ) {
        throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist" );
      }

      if ( request.getBlockDeviceMappingSet() != null && !request.getBlockDeviceMappingSet( ).getItem( ).isEmpty( ) ) {
        nextmapping:
        for ( final InstanceBlockDeviceMappingItemType mapping : request.getBlockDeviceMappingSet( ).getItem( ) ) {
          for ( VmVolumeAttachment vmVolumeAttachment : Iterables.concat( vm.getBootRecord().getPersistentVolumes(), vm.getTransientVolumeState().getAttachments() ) ) {
            if ( vmVolumeAttachment.getDevice().equals( mapping.getDeviceName() ) ) {
              // NOTE: AWS looks for a valid device name with any valid volume Id.
              // Invalid volume Id results an InvalidVolumeID.Malformed.
              // Current implementation for this negative use case is to throw InvalidVolumeID.Malformed exception
              // when user is not allowed to access the requested volume
              if ( mapping.getEbs( ) != null && mapping.getEbs( ).getVolumeId( ) != null ) {
                if ( mapping.getEbs( ).getVolumeId( ).equals( vmVolumeAttachment.getVolumeId( ) ) ) try {
                  Volumes.lookup(
                      ctx.getUserFullName().asAccountFullName(),
                      ResourceIdentifiers.tryNormalize().apply( mapping.getEbs().getVolumeId() ) );
                } catch ( Exception e ) {
                  throw new ClientComputeException( "InvalidInstanceAttributeValue", "Invalid volume ("+mapping.getEbs().getVolumeId()+")" );
                } else {
                  throw new ClientComputeException( "InvalidInstanceAttributeValue", "Invalid volume ("+mapping.getEbs().getVolumeId()+")" );
                }
              }
              vmVolumeAttachment.setDeleteOnTerminate( mapping.getEbs( ) == null ?
                  true :
                  Objects.firstNonNull( mapping.getEbs( ).getDeleteOnTermination( ), true ) );
              continue nextmapping;
            }
          }
          throw new ClientComputeException( "InvalidInstanceAttributeValue", "No device is currently mapped at " + mapping.getDeviceName( ) );
        }
        tx.commit();
      } else if ( request.getDisableApiTermination() != null ) {
        // not currently supported
      } else if ( request.getEbsOptimized() != null ) {
        // not currently supported
      } else if ( request.getGroupIdSet( ) != null && !request.getGroupIdSet( ).getItem( ).isEmpty( ) ) {
        final Collection<NetworkGroup> groups = Lists.newArrayList( );
        final AccountFullName accountFullName = ctx.getUserFullName( ).asAccountFullName( );
        for ( final SecurityGroupIdSetItemType groupIdItemType : request.getGroupIdSet( ).getItem( ) ) try {
          final String groupId = ResourceIdentifiers.tryNormalize().apply( groupIdItemType.getGroupId( ) );
          final NetworkGroup networkGroup = NetworkGroups.lookupByGroupId( accountFullName, groupId );
          if ( !RestrictedTypes.filterPrivileged( ).apply( networkGroup ) ) {
            throw new IllegalAccessException( "Not authorized to access security group " + groupId + " for " + ctx.getUserFullName( ) );
          }
          if ( !Objects.firstNonNull( networkGroup.getVpcId( ), "" ).equals( vm.getVpcId( ) ) ) {
            throw new ClientComputeException( "InvalidGroup.NotFound", "Security group ("+groupId+") not found" );
          }
          groups.add( networkGroup );
        } catch ( NoSuchMetadataException e ) {
          throw new ClientComputeException( "InvalidGroup.NotFound", "Security group ("+groupIdItemType.getGroupId( )+") not found" );
        }
        vm.getNetworkGroups( ).clear( );
        vm.getNetworkGroups( ).addAll( groups );
        if ( vm.getNetworkInterfaces( ) != null ) {
          for ( final NetworkInterface networkInterface : vm.getNetworkInterfaces( ) ) {
            if ( networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
              networkInterface.getNetworkGroups( ).clear( );
              networkInterface.getNetworkGroups( ).addAll( groups );
              break;
            }
          }
        }
        tx.commit();
        NetworkGroups.flushRules( );
      } else if ( request.getInstanceInitiatedShutdownBehavior( ) != null ) {
        // not currently supported
      } else if ( request.getSourceDestCheck( ) != null ) {
        // not currently supported
      } else if ( request.getSriovNetSupport( ) != null ) {
        // not currently supported
      } else {
        if ( !VmState.STOPPED.apply( vm ) ) {
          throw new ClientComputeException( "IncorrectInstanceState", "The instance (" + instanceId + ") is not in the 'stopped' state." );
        }
        if ( request.getInstanceType( ) != null ) {
          VmType vmType = VmTypes.lookup( request.getInstanceType( ).getValue( ) ); // throws NoSuchMetadataException
          if ( !RestrictedTypes.filterPrivileged( ).apply( vmType ) ) {
            throw new IllegalAccessException( "Not authorized to allocate vm type " + vmType + " for " + ctx.getUserFullName( ) );
          }
          vm.getBootRecord( ).setVmType( vmType );
          tx.commit( );
        } else if ( request.getKernel() != null ) {
          try {
            final KernelImageInfo kernelImg = Images.lookupKernel( request.getKernel( ).getValue( ) );
            if ( Images.FilterPermissions.INSTANCE.apply( kernelImg )
                    && ImageMetadata.State.available.equals( kernelImg.getState( ) ) ) {
              if ( !RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( kernelImg ) )
                throw new IllegalAccessException( "Not authorize to use image " + kernelImg.getName( ) + " for ModifyInstanceAttribute" );
              vm.getBootRecord( ).setKernel( kernelImg );
              tx.commit( );
            } else {
              throw new ClientComputeException( "InvalidAMIID.NotFound", "Image id (" + request.getRamdisk( ).getValue( ) + ") not found" );
            }
          } catch ( final NoSuchElementException e ) {
            throw new ClientComputeException( "InvalidAMIID.NotFound", "Image id (" + request.getRamdisk( ).getValue( ) + ") not found" );
          }
        } else if ( request.getRamdisk() != null ) {
          try {
            final RamdiskImageInfo ramdiskImg = Images.lookupRamdisk( request.getRamdisk( ).getValue( ) );
            if ( Images.FilterPermissions.INSTANCE.apply( ramdiskImg )
                    && ImageMetadata.State.available.equals( ramdiskImg.getState( ) ) ) {
              if ( !RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( ramdiskImg ) )
                throw new IllegalAccessException( "Not authorize to use image " + ramdiskImg.getName( ) + " for ModifyInstanceAttribute" );
              vm.getBootRecord( ).setRamdisk( ramdiskImg );
              tx.commit( );
            } else {
              throw new ClientComputeException( "InvalidAMIID.NotFound", "Image id (" + request.getRamdisk( ).getValue( ) + ") not found" );
            }
          } catch ( final NoSuchElementException e ) {
            throw new ClientComputeException( "InvalidAMIID.NotFound", "Image id (" + request.getRamdisk( ).getValue( ) + ") not found" );
          }
        } else if ( request.getUserData() != null ) {
          final byte[] userData;
          try {
            userData = B64.standard.dec( request.getUserData( ).getValue( ) );
          } catch ( ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | DecoderException e ) {
            throw new ClientComputeException( "InvalidParameterValue", "User data decoding error." );
          }
          if ( userData.length > Integer.parseInt( VmInstances.USER_DATA_MAX_SIZE_KB ) * 1024 ) {
            throw new InvalidMetadataException( "User data may not exceed " + VmInstances.USER_DATA_MAX_SIZE_KB + " KB" );
          }
          vm.getBootRecord( ).setUserData( userData );
          tx.commit( );
        } else {
          // InstanceInitiatedShutdownBehavior, SourceDestCheck, GroupId [EC2-VPC], EbsOptimized are not supported yet.
        }
      }
      reply.set_return( true );
    } catch ( final ComputeException e ) {
      throw  e;
    } catch ( Exception ex ) {
      if ( Exceptions.isCausedBy( ex, NoSuchMetadataException.class ) ) {
        throw new ClientComputeException( "InvalidInstanceAttributeValue", "The instanceType '" + request.getInstanceType() + "' is invalid." );
      } else if ( Exceptions.isCausedBy( ex, IllegalAccessException.class ) ) {
        throw new ClientComputeException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
      } else if ( Exceptions.isCausedBy( ex, InvalidMetadataException.class ) ) {
        throw new ClientComputeException( "InvalidParameterValue", "User data is limited to 16384 bytes" );
      }
      LOG.error( ex, ex );
      throw new ComputeException( "InternalError", "Error processing request: " + ex.getMessage( ) );
    }
    return reply;
  }
  
  public DescribePlacementGroupsResponseType describePlacementGroups( final DescribePlacementGroupsType request ) {
    final DescribePlacementGroupsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeInstanceAttributeResponseType describeInstanceAttribute( final DescribeInstanceAttributeType request )
          throws EucalyptusCloudException {
    final DescribeInstanceAttributeResponseType reply = request.getReply( );
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    final String attribute = request.getAttribute( );
    if ( attribute == null ) {
      throw new ClientComputeException( " MissingParameter", "Attribute parameter is required" );
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
          reply.setUserData( vm.getUserData( ) == null ? null : Base64.toBase64String( vm.getUserData( ) ) );
          break;
        default:
          throw new ClientComputeException( " InvalidParameterValue", "Invalid value for attribute ("+attribute+")" );
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      throw new ClientComputeException("InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist");
    }
    return reply;
  }

  public DeletePlacementGroupResponseType deletePlacementGroup( final DeletePlacementGroupType request ) {
    final DeletePlacementGroupResponseType reply = request.getReply( );
    return reply;
  }
  
  public CreatePlacementGroupResponseType createPlacementGroup( final CreatePlacementGroupType request ) {
    final CreatePlacementGroupResponseType reply = request.getReply( );
    return reply;
  }
  
  public CancelBundleTaskResponseType cancelBundleTask( final CancelBundleTaskType request ) throws EucalyptusCloudException {
    final CancelBundleTaskResponseType reply = request.getReply( );
    reply.set_return( true );
    final Context ctx = Contexts.lookup( );
    try {
      final VmInstance v = VmInstances.lookupByBundleId( normalizeBundleIdentifier( request.getBundleId() ) );
      BundleState bundleState = v.getRuntimeState( ).getBundleTaskState( );
      if ( !( bundleState == BundleState.pending || bundleState == BundleState.storing ) )
        throw new EucalyptusCloudException( "Can't cancel bundle task when the bundle task is " + bundleState );
      
      if ( RestrictedTypes.filterPrivileged( ).apply( v ) ) {
        v.getRuntimeState( ).updateBundleTaskState( BundleState.canceling, 0.0d );
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELING, ctx.getUserFullName( ).toString( ),
                                      v.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                                      v.getInstanceId( ) ) );
        
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, v.lookupPartition( ) );
        final Cluster cluster = Clusters.lookup( ccConfig );
        
        request.setInstanceId( v.getInstanceId( ) );
        reply.setTask( Bundles.transform( v.getRuntimeState( ).getBundleTask( ) ) );
        AsyncRequests.newRequest( Bundles.cancelCallback( request ) ).dispatch( cluster.getConfiguration( ) );
        return reply;
      } else {
        throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
      }
    } catch ( final NoSuchElementException e ) {
      throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
    }
  }
  
  public BundleInstanceResponseType bundleInstance( final BundleInstanceType request ) throws EucalyptusCloudException {
    final Context ctx = Contexts.lookup( );
    final BundleInstanceResponseType reply = request.getReply( );
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    if (!validBucketName(request.getBucket( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getBucket( ) + ") for parameter Bucket is invalid." );
    } else if (!validBucketName(request.getPrefix( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getPrefix( ) + ") for parameter Prefix is invalid." );
    }
    if ( request.getUploadPolicy( ) != null && request.getUploadPolicy( ).length( ) > 4096 ) {
      throw new ClientComputeException( " InvalidParameterValue", "Value for parameter UploadPolicy is invalid (too long)" );
    }
    final String uploadPolicyJson = B64.standard.decString( request.getUploadPolicy( ) );

    Bundles.checkAndCreateBucket( ctx.getUser( ), request.getBucket( ), request.getPrefix( ) );

    final Function<String, VmInstance> bundleFunc = new Function<String,VmInstance>( ) {
      @Override
      public VmInstance apply( String input ) {
        reply.set_return( false );
        try {
          final VmInstance v = RestrictedTypes.doPrivileged( input, VmInstance.class );
          if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
            throw new ClientComputeException( "InvalidState", "Failed to bundle requested vm because it is not currently 'running': " + instanceId );
          } else {
            final VmBundleTask bundleTask = Bundles.create( v, request.getBucket(), request.getPrefix(), uploadPolicyJson );
            if ( v.getRuntimeState( ).startBundleTask( bundleTask ) ) {
              reply.setTask( Bundles.transform( bundleTask ) );
              reply.markWinning( );
            } else {
              throw new ClientComputeException( "BundlingInProgress", "Instance is already being bundled: " + v.getRuntimeState().getBundleTask().getBundleId() );
            }
            EventRecord.here( VmControl.class,
                EventType.BUNDLE_PENDING,
                ctx.getUserFullName( ).toString( ),
                v.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                v.getInstanceId( ) ).debug( );
          }
          return v;
        } catch ( final ComputeException e ) {
          throw Exceptions.toUndeclared( e );
        } catch ( final AuthException | NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist" ) );
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        }
      }
    };

    AccessKey accessKey;
    try {
      final String accessKeyId = request.getAwsAccessKeyId();
      final JSONObject policyJsonObj = JSONObject.fromObject( uploadPolicyJson );
      final JSONArray conditions = policyJsonObj.getJSONArray( "conditions" );
      String securityToken = null;
      for ( final Object object : conditions ) {
        if ( object instanceof JSONObject && ( (JSONObject) object ).has( "x-amz-security-token" ) ) {
          securityToken = ( (JSONObject) object ).getString( "x-amz-security-token" );
          break;
        }
      }
      accessKey = AccessKeys.lookupAccessKey( accessKeyId, securityToken );
      final byte[] sig = HmacUtils.getSignature( accessKey.getSecretKey( ), request.getUploadPolicy( ), Hmac.HmacSHA1 );
      if ( !MessageDigest.isEqual( sig, B64.standard.dec( request.getUploadPolicySignature() ) ) ) {
        throw new ClientComputeException( "InvalidParameterValue", "Value for UploadPolicySignature is invalid." );
      }
    } catch ( final JSONException e ) {
      throw new ClientComputeException( "InvalidParameterValue", "Value for UploadPolicy is invalid." );
    } catch ( final AuthException e ) {
      throw new ClientComputeException( "InvalidParameterValue", "Value ("+request.getAwsAccessKeyId( )+") for AWSAccessKeyId is invalid."  );
    } catch ( final AuthenticationException e ) {
      LOG.error( "Error processing upload policy signature ", e );
      throw new ClientComputeException( "InternalError", "Error processing request; upload policy signature error." );
    }

    final VmInstance bundledVm;
    try {
      bundledVm = Entities.asTransaction( VmInstance.class, bundleFunc ).apply( instanceId );
    } catch ( final RuntimeException e ) {
      LOG.error( e, e );
      Exceptions.findAndRethrow( e, ComputeException.class );
      throw e;
    }
    final ImageInfo imageInfo = Images.lookupImage(bundledVm.getImageId());
    final AccessKey accessKeyForPolicySignature = accessKey;
    try {
      ServiceConfiguration cluster = Topology.lookup( ClusterController.class, bundledVm.lookupPartition( ) );
      BundleInstanceType reqInternal = new BundleInstanceType(){
			{
  			setInstanceId(request.getInstanceId());
  			setBucket(request.getBucket());
  			setPrefix(request.getPrefix());
  			setAwsAccessKeyId(accessKeyForPolicySignature.getAccessKey());
  			setUploadPolicy(request.getUploadPolicy());
  			setUploadPolicySignature(request.getUploadPolicySignature());
  			setUrl(request.getUrl());
  			setUserKey(request.getUserKey());
  			setArchitecture(imageInfo != null ? imageInfo.getArchitecture().name() : "i386");
			}
		}.regardingUserRequest(request);      
      AsyncRequests.newRequest( Bundles.createCallback(reqInternal)).dispatch( cluster );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
      throw Exceptions.toUndeclared( ex );
    }
    return reply;
  }
  
  public GetPasswordDataResponseType getPasswordData( final GetPasswordDataType request ) throws Exception {
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    final VmInstance v;
    try {
      v = VmInstances.lookup( instanceId );
    } catch ( NoSuchElementException e ) {
      throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '"+instanceId+"' does not exist" );
    }

    if ( !RestrictedTypes.filterPrivileged( ).apply( v ) ) {
      throw new EucalyptusCloudException( "Instance " + instanceId + " does not exist." );
    }

    if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
      throw new EucalyptusCloudException( "Instance " + instanceId + " is not in a running state." );
    }

    if ( !ImageMetadata.Platform.windows.name().equals(v.getPlatform())) {
      throw new ClientComputeException("OperationNotPermitted", "Instance's platform is not Windows");
    }
    
    if( Strings.isNullOrEmpty( v.getKeyPair( ).getPublicKey( ) ) ){
      throw new ClientComputeException("OperationNotPermitted", "Keypair is not found for the instance");
    }

    if ( v.getPasswordData( ) == null ) {
      try {
        final GetConsoleOutputResponseType consoleOutput = getConsoleOutput( new GetConsoleOutputType( instanceId ) );
        final String tempCo = B64.standard.decString( String.valueOf( consoleOutput.getOutput( ) ) ).replaceAll( "[\r\n]*", "" );
        final String passwordData = tempCo.replaceAll( ".*<Password>", "" ).replaceAll( "</Password>.*", "" );
        if ( tempCo.matches( ".*<Password>[\\w=+/]*</Password>.*" ) ) {
          Entities.asTransaction( VmInstance.class, new Predicate<String>() {
            @Override
            public boolean apply( final String passwordData ) {
              final VmInstance vm = Entities.merge( v );
              vm.updatePasswordData( passwordData );
              return true;
            }
          } ).apply( passwordData );
          v.updatePasswordData( passwordData );
        }
      } catch ( Exception e ) {
        throw new ComputeException( "InternalError", "Error processing request: " + e.getMessage( ) );
      }
    }

    final GetPasswordDataResponseType reply = request.getReply();
    reply.set_return( true );
    reply.setOutput( v.getPasswordData( ) );
    reply.setTimestamp( new Date() );
    reply.setInstanceId( v.getInstanceId() );
    return reply;
  }

  private static Set<String> toInstanceIds( final Iterable<String> ids ) throws EucalyptusCloudException {
    final Set<String> result = Sets.newHashSet();
    if ( ids != null ) for ( final String id : ids ) {
      result.add( normalizeBundleIdentifier( id ).replace( "bun-", "i-" ) );
    }
    return result;
  }

  private boolean validBucketName(String name) {
    return java.util.regex.Pattern.matches( "^[a-zA-Z\\d\\.\\-_]{3,255}$", name );
  }

  private static String normalizeIdentifier( final String identifier ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.parse( VmInstance.ID_PREFIX, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidInstanceID.Malformed", "Invalid id: \""+e.getIdentifier()+"\"" );
    }
  }

  private static List<String> normalizeIdentifiers( final List<String> identifiers ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.normalize( VmInstance.ID_PREFIX, identifiers );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidInstanceID.Malformed", "Invalid id: \""+e.getIdentifier()+"\"" );
    }
  }

  private static String normalizeBundleIdentifier( final String identifier ) throws EucalyptusCloudException {
    try {
      return ResourceIdentifiers.parse( "bun", identifier ).getIdentifier();
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidInstanceID.Malformed", "Invalid id: \""+e.getIdentifier()+"\"" );
    }
  }
}
