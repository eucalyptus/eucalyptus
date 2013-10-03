/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.mule.RequestContext;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.run.AdmissionControl;
import com.eucalyptus.cloud.run.Allocations;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.run.ClusterAllocator;
import com.eucalyptus.cloud.run.ContractEnforcement;
import com.eucalyptus.cloud.run.VerifyMetadata;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.callback.ConsoleOutputCallback;
import com.eucalyptus.cluster.callback.PasswordDataCallback;
import com.eucalyptus.cluster.callback.RebootCallback;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.network.PrivateNetworkIndex;
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
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
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

      ReservationInfoType reservation = new ReservationInfoType( allocInfo.getReservationId( ),
                                                                 allocInfo.getOwnerFullName( ).getAccountNumber( ),
                                                                 allocInfo.getNetworkGroupsMap() );
      reply.setRsvInfo( reservation );
      for ( ResourceToken allocToken : allocInfo.getAllocationTokens( ) ) {
        VmInstance entity = Entities.merge( allocToken.getVmInstance( ) );
        reservation.getInstancesSet( ).add( VmInstances.transform( entity ) );
      }
      db.commit( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      allocInfo.abort( );
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
    final ArrayList<String> instancesSet = msg.getInstancesSet( );    
    final Multimap<String, RunningInstancesItemType> instanceMap = TreeMultimap.create( );
    final Map<String, ReservationInfoType> reservations = Maps.newHashMap();
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class )
        .withOptionalInternalFilter( "instance-id", msg.getInstancesSet() )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( msg.getInstancesSet( ) ) // filters without wildcard support
        .byPredicate( filter.asPredicate() )
        .byPredicate( filter.isFilteringOnTags() ? Predicates.not( VmState.TERMINATED ) : Predicates.<VmInstance>alwaysTrue() ) // terminated instances have no tags
        .byPrivileges()
        .buildPredicate();
    OwnerFullName ownerFullName = ( ctx.hasAdministrativePrivileges( ) && showAll )
      ? null
      : ctx.getUserFullName( ).asAccountFullName( );
    try {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, filter.asCriterion(), filter.getAliases(), Predicates.and( requestedAndAccessible, VmInstances.initialize( ) ) );
      final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( VmInstance.class )
          .getResourceTagMap( AccountFullName.getInstance( ctx.getAccount() ),
              Iterables.transform( instances, CloudMetadatas.toDisplayName() ) );

      for ( final VmInstance vm : instances ) {
        if ( !instancesSet.isEmpty( ) && !instancesSet.contains( vm.getInstanceId( ) ) ) {
          continue;
        }
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance v = VmState.TERMINATED.apply( vm ) ? vm : Entities.merge( vm );
          if ( instanceMap.put( v.getReservationId( ), VmInstances.transform( v ) ) && !reservations.containsKey( v.getReservationId( ) ) ) {
            reservations.put( v.getReservationId( ), new ReservationInfoType( v.getReservationId( ), v.getOwner( ).getAccountNumber( ), v.getNetworkMap( ) ) );
          }
        } catch ( Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
          try {
            if ( vm != null ) {
              try {
                RunningInstancesItemType ret = VmInstances.transform( vm );
                if ( ret != null && vm.getReservationId( ) != null ) {
                  if ( instanceMap.put( vm.getReservationId( ), VmInstances.transform( vm ) ) && !reservations.containsKey( vm.getReservationId( ) ) ) {
                    reservations.put( vm.getReservationId( ), TypeMappers.transform( vm, ReservationInfoType.class ) );
                  }
                }
              } catch ( Exception ex1 ) {
                LOG.error( ex1, ex1 );
              }
            }
          } catch ( Exception ex1 ) {
            LOG.error( ex1, ex1 );
          }
        } finally {
          if (db.isActive()) db.rollback( );
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
    final Filter filter = Filters.generateFor( msg.getFilterSet(), VmInstance.class, "status" )
        .withOptionalInternalFilter( "instance-id", msg.getInstancesSet() )
        .generate();
    final Predicate<? super VmInstance> requestedAndAccessible = CloudMetadatas.filteringFor( VmInstance.class )
        .byId( msg.getInstancesSet() ) // filters without wildcard support
        .byPredicate( includeAllInstances ? Predicates.<VmInstance>alwaysTrue() : VmState.RUNNING )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    OwnerFullName ownerFullName = ( ctx.hasAdministrativePrivileges( ) && showAll )
        ? null
        : ctx.getUserFullName( ).asAccountFullName( );
    try {
      final List<VmInstance> instances =
          VmInstances.list( ownerFullName, filter.asCriterion(), filter.getAliases(), requestedAndAccessible );

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
    try {
      final Context ctx = Contexts.lookup( );
      final List<TerminateInstancesItemType> results = reply.getInstancesSet( );
      Function<String,TerminateInstancesItemType> terminateFunction = new Function<String,TerminateInstancesItemType>( ) {
        @Override
        public TerminateInstancesItemType apply( final String instanceId ) {
          String oldState = null, newState = null;
          int oldCode = 0, newCode = 0;
          TerminateInstancesItemType result = null;
          try {
            VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
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
            } else if ( VmState.TERMINATED.apply( vm ) ) {
              oldCode = newCode = VmState.TERMINATED.getCode( );
              oldState = newState = VmState.TERMINATED.getName( );
              VmInstances.delete( vm );
            }
            result = new TerminateInstancesItemType( instanceId, oldCode, oldState, newCode, newState );
          } catch ( final TerminatedInstanceException e ) {
            oldCode = newCode = VmState.TERMINATED.getCode( );
            oldState = newState = VmState.TERMINATED.getName( );
            VmInstances.delete( instanceId );
            result = new TerminateInstancesItemType( instanceId, oldCode, oldState, newCode, newState );
          } catch ( final NoSuchElementException e ) {
            LOG.debug( "Ignoring terminate request for non-existant instance: " + instanceId );
          } catch ( final Exception e ) {
            throw Exceptions.toUndeclared( e );
          }
          return result;
        }
      };
      Function<String, TerminateInstancesItemType> terminateTx = Entities.asTransaction( VmInstance.class, terminateFunction, VmInstances.TX_RETRIES );
      for ( String instanceId : request.getInstancesSet( ) ) {
        try {
          TerminateInstancesItemType termInstance = terminateTx.apply( instanceId );
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
    final RebootInstancesResponseType reply = ( RebootInstancesResponseType ) request.getReply( );
    try {
        ArrayList <String> instanceSet = request.getInstancesSet();
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
  
  public void getConsoleOutput( final GetConsoleOutputType request ) throws EucalyptusCloudException {
    VmInstance v = null;
    try {
      v = VmInstances.lookup( request.getInstanceId( ) );
    } catch ( final NoSuchElementException e2 ) {
      try {
        v = VmInstances.lookup( request.getInstanceId( ) );
        final GetConsoleOutputResponseType reply = request.getReply( );
        reply.setInstanceId( request.getInstanceId( ) );
        reply.setTimestamp( new Date( ) );
        reply.setOutput( v.getConsoleOutputString( ) );
        Contexts.response( reply );
      } catch ( final NoSuchElementException ex ) {
        throw new EucalyptusCloudException( "No such instance: " + request.getInstanceId( ) );
      }
    }
    if ( !RestrictedTypes.filterPrivileged( ).apply( v ) ) {
      throw new EucalyptusCloudException( "Permission denied for vm: " + request.getInstanceId( ) );
    } else if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
      final GetConsoleOutputResponseType reply = request.getReply( );
      reply.setInstanceId( request.getInstanceId( ) );
      reply.setTimestamp( new Date( ) );
      reply.setOutput( v.getConsoleOutputString( ) );
      Contexts.response( reply );
    } else {
      Cluster cluster = null;
      try {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, v.lookupPartition( ) );
        cluster = Clusters.lookup( ccConfig );
      } catch ( final NoSuchElementException e1 ) {
        throw new EucalyptusCloudException( "Failed to find cluster info for '" + v.getPartition( ) + "' related to vm: " + request.getInstanceId( ) );
      }
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
      ConsoleOutputCallback messageCallback = new ConsoleOutputCallback( request );
      try {
        AsyncRequests.newRequest( messageCallback ).sendSync( cluster.getConfiguration( ) );
      } catch(Exception e) {
    	/* The synchronous call failed, lets make sure we empty the output and fire our callback to answer the tool */
        GetConsoleOutputResponseType reply = request.getReply();
        reply.setTimestamp( new Date( ) );
        reply.setOutput( " " );
        reply.set_return(false);
        reply.setStatusMessage("ERROR");
        messageCallback.fire( reply );
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
  
  public UnmonitorInstancesResponseType unmonitorInstances( final UnmonitorInstancesType request ) {

      final UnmonitorInstancesResponseType reply = request.getReply( );

      ArrayList<String> instanceSet = Lists.newArrayList( request.getInstancesSet() );
      ArrayList<MonitorInstanceState> monitorFalseList = Lists.newArrayList();
      
      for(String inst : instanceSet) {
	  final MonitorInstanceState monitorInstanceState = new MonitorInstanceState();
	  monitorInstanceState.setInstanceId(inst);
	  monitorInstanceState.setMonitoringState("disabled");
	  monitorFalseList.add(monitorInstanceState);
      }
      
      reply.setInstancesSet(SetMonitorFunction.INSTANCE.apply( monitorFalseList ) );
      return reply;
  }
  
  public StartInstancesResponseType startInstances( final StartInstancesType request ) throws Exception {
    final StartInstancesResponseType reply = request.getReply( );
    for ( String instanceId : request.getInstancesSet( ) ) {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {//scope for transaction
        final VmInstance vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
        if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
          Allocation allocInfo = Allocations.start( vm );
          try {//scope for allocInfo
            AdmissionControl.run( ).apply( allocInfo );
            PrivateNetworkIndex vmIdx = allocInfo.getAllocationTokens( ).get( 0 ).getNetworkIndex( );
            if ( vmIdx != null && !PrivateNetworkIndex.bogus( ).equals( vmIdx ) ) {
              vmIdx.set( vm );
              vm.setNetworkIndex( vmIdx );
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
      
      for( final String instanceId : request.getInstancesSet()){
    	  try{
    		  final VmInstance vm = VmInstances.lookup(instanceId);
    		  if(!vm.isBlockStorage())
    			  throw new ClientComputeException("UnsupportedOperation",
    					  String.format("The instance '%s' does not have an 'ebs' root device type and cannot be stopped.", instanceId));
    	  }catch( final TerminatedInstanceException ex) {
    		  throw new ClientComputeException("IncorrectInstanceState", 
    				  String.format("This instance '%s' is not in a state from which it can be stopped.", instanceId));
    	  }catch( final NoSuchElementException ex) {
    		  throw new ClientComputeException("InvalidInstanceID.NotFound",
    				  String.format("The instance ID '%s' does not exist", instanceId));
    	  }catch (final EucalyptusCloudException ex){
    		  throw ex;
    	  }
      }
      
      Predicate<String> stopTx = Entities.asTransaction( VmInstance.class, stopPredicate );
      Iterables.all( request.getInstancesSet( ), stopTx );
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
  
  public ResetInstanceAttributeResponseType resetInstanceAttribute( final ResetInstanceAttributeType request ) {
    final ResetInstanceAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public MonitorInstancesResponseType monitorInstances( final MonitorInstancesType request ) {
    
      final MonitorInstancesResponseType reply = request.getReply();

      ArrayList<String> instanceSet = Lists.newArrayList( request.getInstancesSet() );
      ArrayList<MonitorInstanceState> monitorTrueList = Lists.newArrayList();
      
      for(final String inst : instanceSet) {
	  final MonitorInstanceState monitorInstanceState = new MonitorInstanceState();
	  monitorInstanceState.setInstanceId(inst);
	  monitorInstanceState.setMonitoringState("enabled");
	  monitorTrueList.add(monitorInstanceState);
      }
      
      reply.setInstancesSet(SetMonitorFunction.INSTANCE.apply( monitorTrueList ) );
      return reply;
  }
  
  private enum SetMonitorFunction implements Function<ArrayList<MonitorInstanceState>, ArrayList<MonitorInstanceState>> {
      INSTANCE;

      @Override
      public ArrayList<MonitorInstanceState> apply(
	      final ArrayList<MonitorInstanceState> monitorList) {

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
    
  public ModifyInstanceAttributeResponseType modifyInstanceAttribute( final ModifyInstanceAttributeType request ) {
    final ModifyInstanceAttributeResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribePlacementGroupsResponseType describePlacementGroups( final DescribePlacementGroupsType request ) {
    final DescribePlacementGroupsResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribeInstanceAttributeResponseType describeInstanceAttribute( final DescribeInstanceAttributeType request ) {
    final DescribeInstanceAttributeResponseType reply = request.getReply( );
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
      final VmInstance v = VmInstances.lookupByBundleId( request.getBundleId( ) );
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
    final String instanceId = request.getInstanceId( );
    if (!validBucketName(request.getBucket( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getBucket( ) + ") for parameter Bucket is invalid." );
    } else if (!validBucketName(request.getPrefix( ) ) ) {
       throw new ClientComputeException(" InvalidParameterValue", "Value (" + request.getPrefix( ) + ") for parameter Prefix is invalid." );
    }
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
            throw new EucalyptusCloudException( "Failed to bundle requested vm because it is not currently 'running': " + request.getInstanceId( ) );
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
            throw new EucalyptusCloudException( "Failed to find instance: " + request.getInstanceId( ) );
          }
          return v;
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        }
      }
    };
    VmInstance bundledVm = Entities.asTransaction( VmInstance.class, bundleFunc ).apply( instanceId );
    try {
      ServiceConfiguration cluster = Topology.lookup( ClusterController.class, bundledVm.lookupPartition( ) );
      BundleInstanceType reqInternal = new BundleInstanceType(){
			{
  			setInstanceId(request.getInstanceId());
  			setBucket(request.getBucket());
  			setPrefix(request.getPrefix());
  			setAwsAccessKeyId(request.getAwsAccessKeyId());
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
  
  public void getPasswordData( final GetPasswordDataType request ) throws Exception {
    try {
      final Context ctx = Contexts.lookup( );
      Cluster cluster = null;
      final VmInstance v = VmInstances.lookup( request.getInstanceId( ) );
      if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " is not in a running state." );
      }
      if ( !ImageMetadata.Platform.windows.name().equals(v.getPlatform()))
    	  throw new ClientComputeException("OperationNotPermitted", "Instance's platform is not Windows");
    	  
      if ( RestrictedTypes.filterPrivileged( ).apply( v ) ) {
        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, v.lookupPartition( ) );
        cluster = Clusters.lookup( ccConfig );
      } else {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " does not exist." );
      }
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
      if ( v.getPasswordData( ) == null ) {
        AsyncRequests.newRequest( new PasswordDataCallback( request ) ).dispatch( cluster.getConfiguration( ) );
      } else {
        final GetPasswordDataResponseType reply = request.getReply( );
        reply.set_return( true );
        reply.setOutput( v.getPasswordData( ) );
        reply.setTimestamp( new Date( ) );
        reply.setInstanceId( v.getInstanceId( ) );
        ServiceContext.dispatch( "ReplyQueue", reply );
      }
    } catch ( final NoSuchElementException e ) {
      ServiceContext.dispatch( "ReplyQueue", new EucalyptusErrorMessageType( "VmControl", request, e.getMessage( ) ) );
    }
  }

  private static Set<String> toInstanceIds( final Iterable<String> ids ) {
    final Set<String> result = Sets.newHashSet();
    if ( ids != null ) for ( final String id : ids ) {
      result.add( id.replace( "bun-", "i-" ) );
    }
    return result;
  }

  private boolean validBucketName(String name) {
    return java.util.regex.Pattern.matches( "^[a-zA-Z\\d\\.\\-_]{3,255}$", name );
  }
}
