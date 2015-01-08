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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.util.InvalidInstanceProfileMetadataException;
import com.eucalyptus.cloud.util.NoSuchImageIdException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.identifier.InvalidResourceIdentifier;
import com.eucalyptus.cloud.VmInstanceLifecycleHelpers;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.images.KernelImageInfo;
import com.eucalyptus.images.RamdiskImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.keys.NoSuchKeyMetadataException;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
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

import edu.ucsb.eucalyptus.msgs.CreatePlacementGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreatePlacementGroupType;
import edu.ucsb.eucalyptus.msgs.DeletePlacementGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeletePlacementGroupType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DescribePlacementGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePlacementGroupsType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputResponseType;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataResponseType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataType;
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType;
import edu.ucsb.eucalyptus.msgs.ModifyInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.MonitorInstanceState;
import edu.ucsb.eucalyptus.msgs.MonitorInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.MonitorInstancesType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.ResetInstanceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetInstanceAttributeType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.GroupItemType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.StartInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.StartInstancesType;
import edu.ucsb.eucalyptus.msgs.StopInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.StopInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.msgs.UnmonitorInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.UnmonitorInstancesType;
import edu.ucsb.eucalyptus.msgs.InstanceBlockDeviceMapping;


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

      Predicates.and( VerifyMetadata.get( ), AdmissionControl.run( ), ContractEnforcement.run() ).apply( allocInfo );
      allocInfo.commit( );

      ReservationInfoType reservation = new ReservationInfoType(
          allocInfo.getReservationId( ),
          allocInfo.getOwnerFullName( ).getAccountNumber( ),
          Collections2.transform(
              allocInfo.getNetworkGroups( ),
              TypeMappers.lookup( NetworkGroup.class, GroupItemType.class ) ) );

      reply.setRsvInfo( reservation );
      for ( ResourceToken allocToken : allocInfo.getAllocationTokens( ) ) {
        VmInstance entity = Entities.merge( allocToken.getVmInstance( ) );
        reservation.getInstancesSet( ).add( VmInstances.transform( entity ) );
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
      if ( e4 != null ) throw new ClientComputeException( "InvalidParameterValue", e4.getMessage( ) );
      final NoSuchImageIdException e5 = Exceptions.findCause( ex, NoSuchImageIdException.class );
      if ( e5 != null ) throw new ClientComputeException( "InvalidAMIID.NotFound", e5.getMessage( ) );
      LOG.error( ex, ex );
      throw ex;
    } finally {
      if ( db.isActive() ) db.rollback();
    }
    ClusterAllocator.get( ).apply( allocInfo );
    return reply;
  }

  public DescribeInstancesResponseType describeInstances( final DescribeInstancesType msg ) throws EucalyptusCloudException {
    final DescribeInstancesResponseType reply = ( DescribeInstancesResponseType ) msg.getReply( );
    Context ctx = Contexts.lookup( );
    boolean showAll = msg.getInstancesSet( ).remove( "verbose" );
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
    final DescribeInstanceStatusResponseType reply = ( DescribeInstanceStatusResponseType ) msg.getReply( );
    final Context ctx = Contexts.lookup();
    final boolean showAll = msg.getInstancesSet( ).remove( "verbose" );
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

  public TerminateInstancesResponseType terminateInstances( final TerminateInstancesType request ) throws EucalyptusCloudException {
    final TerminateInstancesResponseType reply = request.getReply( );
    final List<String> failedVmList = new ArrayList<String>( );
    final List<VmInstance> vmList = new ArrayList<VmInstance>(  );
    final Collection<String> identifiers = normalizeIdentifiers( request.getInstancesSet( ) );
    try {
      for ( String requestedInstanceId : identifiers ) {
        try {
          VmInstance vm = RestrictedTypes.doPrivileged( requestedInstanceId, VmInstance.class );
          vmList.add( vm );
        } catch ( final Exception e ) {
          LOG.debug( e );
          LOG.debug( "Ignoring terminate request for non-existant instance: " + requestedInstanceId );
          failedVmList.add( requestedInstanceId );
        }
      }
      if ( !failedVmList.isEmpty( ) ) {
        throw new NoSuchElementException( "InvalidInstanceID.NotFound" );
      }
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Function<VmInstance,TerminateInstancesItemType> terminateFunction = new Function<VmInstance,TerminateInstancesItemType>( ) {
        @Override
        public TerminateInstancesItemType apply( final VmInstance vm ) {
          String oldState = null, newState = null;
          int oldCode = 0, newCode = 0;
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
              VmInstances.delete( vm );
            }
            result = new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState );
          } catch ( final TerminatedInstanceException e ) {
            oldCode = newCode = VmState.TERMINATED.getCode( );
            oldState = newState = VmState.TERMINATED.getName( );
            VmInstances.delete( vm.getInstanceId( ) );
            result = new TerminateInstancesItemType( vm.getInstanceId( ), oldCode, oldState, newCode, newState );
          } catch ( final NoSuchElementException e ) {
            LOG.debug( "Ignoring terminate request for non-existant instance: " + vm.getInstanceId( ) );
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
      if ( Exceptions.isCausedBy( e, NoSuchElementException.class ) ) {
        if ( failedVmList.size( ) > 1 )
          throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance IDs '" + Joiner.on( ", " ).join( failedVmList ) +"' do not exist" );
        else
          throw new ClientComputeException( "InvalidInstanceID.NotFound", "The instance ID '" + Joiner.on( ", " ).join( failedVmList ) +"' does not exist" );
      }
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public RebootInstancesResponseType rebootInstances( final RebootInstancesType request ) throws EucalyptusCloudException {
    final RebootInstancesResponseType reply = ( RebootInstancesResponseType ) request.getReply( );
    try {
        List <String> instanceSet = normalizeIdentifiers( request.getInstancesSet() );
        ArrayList <String> noAccess = new ArrayList<String>();
        ArrayList <String> migrating = new ArrayList<String>();
        ArrayList <String> noSuchElement = new ArrayList<String>();
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
              final Request<RebootInstancesType, RebootInstancesResponseType> req = AsyncRequests.newRequest( new RebootCallback( v.getInstanceId( ) ) );
              req.getRequest( ).regarding( request );
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
        final GetConsoleOutputResponseType response =
            AsyncRequests.sendSync( cluster.getConfiguration( ), new GetConsoleOutputType( instanceId ) );
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

    final Filter filter = Filters.generate( request.getFilterSet(), VmBundleTask.class );
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      
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
      final Filter noFilters = Filters.generate( new ArrayList<edu.ucsb.eucalyptus.msgs.Filter>(), VmBundleTask.class );
      final Collection<VmInstance> dbBundles = VmInstances.list( null, noFilters.asCriterion(), noFilters.getAliases(), requestedAndAccessible );
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
    } finally {
      db.rollback( );
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
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {//scope for transaction
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
      } finally {
        if ( db.isActive() ) db.rollback();
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
        } catch ( final EucalyptusCloudException ex ) {
          throw ex;
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
    
    final EntityTransaction tx = Entities.get( VmInstance.class );
    try {
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
        } else {
          // SourceDestCheck not implemented
        }
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
    } finally {
      if ( tx.isActive( ) ) tx.rollback( );
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
      public ArrayList<MonitorInstanceState> apply(
	      final List<MonitorInstanceState> monitorList) {

	  ArrayList<MonitorInstanceState> monitorInstanceSet = Lists
		  .newArrayList();

	  for (final MonitorInstanceState monitorInst : monitorList) {

	      final EntityTransaction db = Entities.get(VmInstance.class);

	      try {

		  VmInstance vmInst = VmInstances.lookup(monitorInst
			  .getInstanceId());

		  if (RestrictedTypes.filterPrivileged().apply(vmInst)) {
		      vmInst.getBootRecord()
		      .setMonitoring(
			      monitorInst.getMonitoringState()
			      .equals("enabled") ? Boolean.TRUE
				      : Boolean.FALSE);
		      Entities.merge(vmInst);
		      monitorInstanceSet.add(monitorInst);
		      db.commit();
		  }

	      } catch (NoSuchElementException nse) {
		  LOG.debug("Unable to find instance : "
			  + monitorInst.getInstanceId());
	      } catch (Exception ex) {
		  LOG.debug("Unable to set monitoring state for instance : "
			  + monitorInst.getInstanceId());
	      } finally {
		  if (db.isActive())
		      db.rollback();
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

      if ( request.getBlockDeviceMappingAttribute( ) != null ) {
        boolean isValidBlockDevice = false;
        Set<VmVolumeAttachment> persistentVolumes = vm.getBootRecord( ).getPersistentVolumes( );
        for ( VmVolumeAttachment vmVolumeAttachment : persistentVolumes ) {
            if ( vmVolumeAttachment.getDevice( ).equals( request.getBlockDeviceMappingDeviceName( ) ) ) {
              // NOTE: AWS looks for a valid device name with any valid volume Id.
              // Invalid volume Id results an InvalidVolumeID.Malformed.
              // Current implementation for this negative use case is to throw InvalidVolumeID.Malformed exception
              // when user is not allowed to access the requested volume
              try {
                Volume volume = Volumes.lookup( ctx.getUserFullName( ).asAccountFullName( ), request.getBlockDeviceMappingVolumeId( ) );
              } catch ( Exception e) {
                throw new NoSuchElementException( "InvalidVolumeID.Malformed: '" + request.getBlockDeviceMappingVolumeId( )
                        + "' does not exist or " + ctx.getUserFullName( ) + " is now allowed to access this volume.");
              }
              isValidBlockDevice = true;
              vmVolumeAttachment.setDeleteOnTerminate( request.getBlockDeviceMappingDeleteOnTermination( ) );
            break;
          }
        }
        if ( !isValidBlockDevice )
          throw new NoSuchElementException( "NoSuchBlockDevice: " + "No device is currently mapped at " + request.getBlockDeviceMappingDeviceName( ) );
        Entities.merge( vm );
        tx.commit( );
      } else {
        if ( !VmState.STOPPED.equals( vm.getState( ) ) ) {
          throw new EucalyptusCloudException( "IncorrectInstanceState: " + "The instance '" + instanceId + "' is not in the 'stopped' state." );
        }
        if ( request.getInstanceTypeValue( ) != null ) {
          VmType vmType = VmTypes.lookup( request.getInstanceTypeValue( ) ); // throws NoSuchMetadataException
          if ( !RestrictedTypes.filterPrivileged( ).apply( vmType ) ) {
            throw new IllegalAccessException( "Not authorized to allocate vm type " + vmType + " for " + ctx.getUserFullName( ) );
          }
          vm.getBootRecord( ).setVmType( vmType );
          Entities.merge( vm );
          tx.commit( );
        } else if ( request.getKernelValue( ) != null ) {
          try {
            final KernelImageInfo kernelImg = Images.lookupKernel( request.getKernelValue( ) );
            if ( Images.FilterPermissions.INSTANCE.apply( kernelImg )
                    && ImageMetadata.State.available.equals( kernelImg.getState( ) ) ) {
              if ( !RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( kernelImg ) )
                throw new IllegalAccessException( "Not authorize to use image " + kernelImg.getName( ) + " for ModifyInstanceAttribute" );
              vm.getBootRecord( ).setKernel( kernelImg );
              Entities.merge( vm );
              tx.commit( );
            } else {
              throw new NoSuchElementException( "InvalidAMIID.NotFound: " + "The image id '[" + request.getKernelValue( ) + "]' does not exist" );
            }
          } catch ( Exception e ) {
            throw e;
          }
        } else if ( request.getRamdiskValue( ) != null ) {
          try {
            final RamdiskImageInfo ramdiskImg = Images.lookupRamdisk( request.getRamdiskValue( ) );
            if ( Images.FilterPermissions.INSTANCE.apply( ramdiskImg )
                    && ImageMetadata.State.available.equals( ramdiskImg.getState( ) ) ) {
              if ( !RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( ramdiskImg ) )
                throw new IllegalAccessException( "Not authorize to use image " + ramdiskImg.getName( ) + " for ModifyInstanceAttribute" );
              vm.getBootRecord( ).setRamdisk( ramdiskImg );
              Entities.merge( vm );
              tx.commit( );
            } else {
              throw new NoSuchElementException( "InvalidAMIID.NotFound: " + "The image id '[" + request.getRamdiskValue( ) + "]' does not exist" );
            }
          } catch ( Exception e ) {
            throw e;
          }
        } else if ( request.getUserDataValue( ) != null ) {
          final byte[] userData;
          try {
            userData = B64.standard.dec( request.getUserDataValue( ) );
          } catch ( ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | DecoderException e ) {
            throw new ClientComputeException( "InvalidParameterValue", "User data decoding error." );
          }
          if ( userData.length > Integer.parseInt( VmInstances.USER_DATA_MAX_SIZE_KB ) * 1024 ) {
            throw new InvalidMetadataException( "User data may not exceed " + VmInstances.USER_DATA_MAX_SIZE_KB + " KB" );
          }
          vm.getBootRecord( ).setUserData( userData );
          Entities.merge( vm );
          tx.commit( );
        } else {
          // InstanceInitiatedShutdownBehavior, SourceDestCheck, GroupId [EC2-VPC], EbsOptimized are not supported yet.
        }
      }
      reply.set_return( true );
    } catch ( final ComputeException e ) {
      throw  e;
    } catch ( Exception ex ) {
      if ( Exceptions.isCausedBy( ex, EucalyptusCloudException.class ) ) {
        throw new ClientComputeException( "IncorrectInstanceState", "The instance '" + instanceId + "' is not in the 'stopped' state." );
      } else if ( Exceptions.isCausedBy( ex, NoSuchMetadataException.class ) ) {
        throw new ClientComputeException( "InvalidInstanceAttributeValue", "The instanceType '" + request.getInstanceTypeValue( ) + "' is invalid." );
      } else if ( Exceptions.isCausedBy( ex, IllegalAccessException.class ) ) {
        throw new ClientComputeException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
      } else if ( Exceptions.isCausedBy( ex, NoSuchElementException.class ) ) {
        if ( ex.toString( ).contains( "InvalidAMIID.NotFound" ) ) {
          String imageId = ( request.getKernelValue( ) != null ) ? request.getKernelValue( ) : request.getRamdiskValue( );
          throw new ClientComputeException( "InvalidAMIID.NotFound", "The image id '[" + imageId + "]' does not exist" );
        } else if ( ex.toString( ).contains( "NoSuchBlockDevice" ) ) {
          throw new ClientComputeException( "InvalidInstanceAttributeValue", "No device is currently mapped at " + request.getBlockDeviceMappingDeviceName( ) );
        } else if ( ex.toString( ).contains( "InvalidVolumeID.Malformed" ) ) {
          throw new ClientComputeException( "InvalidVolumeID.Malformed", "Invalid id: '" + request.getBlockDeviceMappingVolumeId( ) + "'" );
        }
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
    reply.setInstanceId( instanceId );
    final EntityTransaction tx = Entities.get( VmInstance.class );
    try {
      final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
      if ( request.getAttribute( ).equals( "kernel" ) ) {
        if ( vm.getKernelId( ) != null ) {
          reply.getKernel( ).add( vm.getKernelId( ) );
        }
      } else if ( request.getAttribute( ).equals( "ramdisk" ) ) {
        if ( vm.getRamdiskId( ) != null ) {
          reply.getRamdisk( ).add( vm.getRamdiskId( ) );
        }
      } else if ( request.getAttribute( ).equals( "instanceType" ) ) {
        if ( vm.getBootRecord( ).getVmType( ).getDisplayName( ) != null ) {
          reply.getInstanceType( ).add( vm.getBootRecord( ).getVmType( ).getDisplayName( ) );
        }
      } else if ( request.getAttribute( ).equals( "userData" ) ) {
        if ( vm.getUserData() != null ) {
          reply.getUserData( ).add( Base64.toBase64String( vm.getUserData( ) ) );
        }
      } else if ( request.getAttribute( ).equals( "rootDeviceName" ) ) {
        if ( vm.getBootRecord( ).getMachine( ) != null && vm.getBootRecord( ).getMachine( ).getRootDeviceName( ) != null ) {
          reply.getRootDeviceName( ).add( ( vm.getBootRecord().getMachine().getRootDeviceName() ) );
        }
      } else if ( request.getAttribute( ).equals( "blockDeviceMapping" ) ) {
        if ( vm.getBootRecord( ).getMachine( ) instanceof BlockStorageImageInfo ) {
          BlockStorageImageInfo bfebsInfo = ( BlockStorageImageInfo ) vm.getBootRecord( ).getMachine( );
          Set<VmVolumeAttachment> persistentVolumes = vm.getBootRecord().getPersistentVolumes();
          for ( VmVolumeAttachment volumeAttachment : persistentVolumes ) {
            if ( volumeAttachment.getIsRootDevice() ) {
              reply.getBlockDeviceMapping( ).add( new InstanceBlockDeviceMapping(
                      bfebsInfo.getRootDeviceName(),
                      volumeAttachment.getVolumeId(),
                      volumeAttachment.getStatus(),
                      volumeAttachment.getAttachTime(),
                      volumeAttachment.getDeleteOnTerminate() ) );
            } else {
              reply.getBlockDeviceMapping().add( new InstanceBlockDeviceMapping(
                      volumeAttachment.getDevice(),
                      volumeAttachment.getVolumeId(),
                      volumeAttachment.getStatus(),
                      volumeAttachment.getAttachTime(),
                      volumeAttachment.getDeleteOnTerminate() ) );
            }
          }
        }
      } else if ( request.getAttribute( ).equals( "groupSet" ) ) {
          Set<NetworkGroupId> networkGroups = vm.getNetworkGroupIds( );
          for( NetworkGroupId networkGroup : networkGroups ) {
              reply.getGroupSet( ).add(
                      new GroupItemType( networkGroup.getGroupId( ), networkGroup.getGroupName( ) ) );
          }
      } else {
          // disableApiTermination | ebsOptimized | instanceInitiatedShutdownBehavior | productCodes | sourceDestCheck
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      throw new ClientComputeException("InvalidInstanceID.NotFound", "The instance ID '" + instanceId + "' does not exist");
    } finally {
      if ( tx.isActive( ) ) tx.rollback( );
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
        v.getRuntimeState( ).updateBundleTaskState( BundleState.canceling );
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
    final BundleInstanceResponseType reply = request.getReply( );//TODO: check if the instance has platform windows.
    final String instanceId = normalizeIdentifier( request.getInstanceId( ) );
    if (!validBucketName(request.getBucket( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getBucket( ) + ") for parameter Bucket is invalid." );
    } else if (!validBucketName(request.getPrefix( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getPrefix( ) + ") for parameter Prefix is invalid." );
    }
    
    Bundles.checkAndCreateBucket(ctx.getUser(), request.getBucket());
    Function<String, VmInstance> bundleFunc = new Function<String,VmInstance> () {

      @Override
      public VmInstance apply( String input ) {
        reply.set_return( false );
        try {
          final VmInstance v = RestrictedTypes.doPrivileged( input, VmInstance.class );
          if ( v.getRuntimeState( ).isBundling( ) ) {
            reply.setTask( Bundles.transform( v.getRuntimeState( ).getBundleTask( ) ) );
            reply.markWinning( );
          } else if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
            throw new EucalyptusCloudException( "Failed to bundle requested vm because it is not currently 'running': " + instanceId );
          } else if ( RestrictedTypes.filterPrivileged( ).apply( v ) ) {
            final VmBundleTask bundleTask = Bundles.create( v, request.getBucket( ), request.getPrefix( ), new String( Base64.decode( request.getUploadPolicy( ) ) ) );
            if ( v.getRuntimeState( ).startBundleTask( bundleTask ) ) {
              reply.setTask( Bundles.transform( bundleTask ) );
              reply.markWinning( );
            } else if ( v.getRuntimeState( ).getBundleTask( ) == null ) {
              v.resetBundleTask( );
              if ( v.getRuntimeState( ).startBundleTask( bundleTask ) ) {
                reply.setTask( Bundles.transform( bundleTask ) );
                reply.markWinning( );
              }
            } else {
              throw new EucalyptusCloudException( "Instance is already being bundled: " + v.getRuntimeState( ).getBundleTask( ).getBundleId( ) );
            }
            EventRecord.here( VmControl.class,
                              EventType.BUNDLE_PENDING,
                              ctx.getUserFullName( ).toString( ),
                              v.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                              v.getInstanceId( ) ).debug( );
          } else {
            throw new EucalyptusCloudException( "Failed to find instance: " + instanceId );
          }
          return v;
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        }
      }
    };
    
    final Function<String, AccessKey> LookupAccessKey = new Function<String, AccessKey>(){
      @Override
      public AccessKey apply(final String policySignature) {
        try{
          final List<AccessKey> keys = ctx.getUser().getKeys();
          AccessKey keyForSign = null;
          final Mac hmac = Mac.getInstance("HmacSHA1");
          for(final AccessKey key : keys){
            hmac.init(new SecretKeySpec(key.getSecretKey().getBytes("UTF-8"), "HmacSHA1"));
            final String sig = B64.standard.encString(hmac.doFinal(request.getUploadPolicy().getBytes("UTF-8")));
            if(sig.equals(policySignature)){
              keyForSign = key;
              break;
            }
          }
          return keyForSign;
        }catch(final Exception ex){
          LOG.warn("Failed to generate upload policy signature", ex);
          return null;
        }
      }
    };
    
    final AccessKey accessKeyForPolicySignature = LookupAccessKey.apply(request.getUploadPolicySignature());
    if(accessKeyForPolicySignature==null){
      throw new ComputeException( "InternalError", "Error processing request: unable to find the access key signed the upload policy" );
    }
    
    VmInstance bundledVm = Entities.asTransaction( VmInstance.class, bundleFunc ).apply( instanceId );
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
