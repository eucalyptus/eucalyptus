/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.conversion;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.conversion.tasks.ConversionState;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.ImageManifests;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.Imaging;
import com.eucalyptus.images.ImportImageType;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.PrivateNetworkIndex;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Dates;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.UserDatas;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.CancelConversionTaskResponseType;
import edu.ucsb.eucalyptus.msgs.CancelConversionTaskType;
import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DescribeConversionTasksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeConversionTasksType;
import edu.ucsb.eucalyptus.msgs.DiskImage;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.ImportInstanceLaunchSpecification;
import edu.ucsb.eucalyptus.msgs.ImportInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportInstanceType;
import edu.ucsb.eucalyptus.msgs.ImportResourceTag;
import edu.ucsb.eucalyptus.msgs.ImportVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

/**
 * @todo doc
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class ImportManager {
  private static Logger                            LOG                           = Logger.getLogger( ImportManager.class );
  /**
   * Amount of time in hours during which a conversion task must complete.
   */
  public static final int                          CONVERSION_EXPIRATION_TIMEOUT = 1;
  private static final Map<String, ConversionTask> tasks                         = Maps.newHashMap( );
  
  public ImportInstanceResponseType importInstance( final ImportInstanceType request ) throws Exception {
    if (true) throw new ComputeException( "InternalError", "Service not available" );
    
    final ImportInstanceResponseType reply = request.getReply( );
    final UserFullName ufn = Contexts.lookup( ).getUserFullName( );
    for ( DiskImage diskImage : request.getDiskImageSet( ) ) {
      final DiskImageDetail imageDetails = diskImage.getImage( );
      final String manifestLocation = imageDetails.getImportManifestUrl( );
      try {
        LOG.info( AsyncRequests.sendSync( Topology.lookup( Imaging.class ), new ImportImageType( ) ) );
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
      final String imageDescription = diskImage.getDescription( );
      try {
        final ImageManifest manifest = ImageManifests.lookup( manifestLocation.replaceAll( "\\?.*", imageDescription ) );
        ImageInfo image = Images.createPendingFromManifest( ufn,
                                                            manifest.getName( ),
                                                            imageDescription,
                                                            manifest.getArchitecture( ),
                                                            manifest.getVirtualizationType( ),
                                                            manifest.getKernelId( ),
                                                            manifest.getRamdiskId( ),
                                                            manifest );
        
      } catch ( Exception ex ) {
        throw new ImageManifestException( "Manifest lookup failed for "
                                          + manifestLocation
                                          + " because of: "
                                          + ex.getMessage( ) );
      }
    }
    
    /**
     * <ol>
     * <li>Persist import request state
     * <li>Submit image import request to imaging service
     * <li><i>Asynchronously</i>Monitor image import updating state, to completion or failure
     * <li><i>Asynchronously</i>On completion of image import, submit instance import request to
     * imaging service
     * <li><i>Asynchronously</i>Monitor instance import updating state, to completion or failure
     * </ol>
     */
    ConversionTask task = new ConversionTask( );
    String taskId = Crypto.generateId( request.getCorrelationId( ), "import-i-" );
    final String instanceId = Crypto.generateId( request.getCorrelationId( ), "i-" );
    final String reservationId = Crypto.generateId( request.getCorrelationId( ), "r-" );
    final String imageId = Crypto.generateId( request.getCorrelationId( ), "emi-" );
    task.setConversionTaskId( taskId );
    Date expiration = Dates.hoursFromNow( CONVERSION_EXPIRATION_TIMEOUT );
    task.setExpirationTime( expiration.toString( ) );
    task.setState( ConversionState.active.name( ) );
    task.setStatusMessage( LogUtil.dumpObject( request ) );
    ImportInstanceTaskDetails taskDetails = new ImportInstanceTaskDetails( );
    
    Function<ImportInstanceLaunchSpecification, VmInstance> builder = new Function<ImportInstanceLaunchSpecification, VmInstance>( ) {
      
      @Override
      public VmInstance apply( ImportInstanceLaunchSpecification arg0 ) {
        Partition partition = Partitions.lookupByName( arg0.getPlacement( ).getAvailabilityZone( ) );
        List<NetworkGroup> groups = Lists.newArrayList( );
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance vm = new VmInstance.Builder( ).owner( Contexts.lookup( ).getUserFullName( ) )
                                                   .withIds( instanceId,
                                                             reservationId,
                                                             request.getCorrelationId( ),
                                                             request.getCorrelationId( ) )
                                                   .bootRecord( Emis.newBootableSet( imageId ),
                                                                UserDatas.decode( arg0.getUserData( ).getData( ) ),
                                                                KeyPairs.noKey( ),
                                                                VmTypes.lookup( arg0.getInstanceType( ) ),
                                                                arg0.getMonitoring( ).getEnabled( ),
                                                                null, null, null )
                                                   .placement( partition, partition.getName( ) )
                                                   .networking( groups, PrivateNetworkIndex.bogus( ) )
                                                   .addressing( false )
                                                   .build( 1 );
          vm.setNaturalId( request.getCorrelationId( ) );
          vm.setState( VmState.STOPPED );
          vm = Entities.persist( vm );
          Entities.flush( vm );
          db.commit( );
          return vm;
        } catch ( final ResourceAllocationException ex ) {
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( ex );
        } catch ( final Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          throw Exceptions.toUndeclared( new TransactionExecutionException( ex ) );
        } finally {
          if ( db.isActive( ) ) db.rollback( );
        }
      }
      
    };
    builder.apply( request.getLaunchSpecification( ) );
    taskDetails.setInstanceId( instanceId );
    taskDetails.setPlatform( request.getPlatform( ) );
    taskDetails.setDescription( request.getDescription( ) );
    task.setImportInstance( taskDetails );
    tasks.put( taskId, task );
    return reply;
  }
  
  public ImportVolumeResponseType importVolume( ImportVolumeType request ) throws Exception {
    if (true) throw new ComputeException( "InternalError", "Service not available" );

    ImportVolumeResponseType reply = request.getReply( );
    /**
     * <ol>
     * <li>Persist import request state
     * <li>Submit volume import request to imaging service
     * <li><i>Asynchronously</i>Monitor image import updating state, to completion or failure
     * </ol>
     */
    return reply;
  }
  
  public CancelConversionTaskResponseType cancelConversionTask( CancelConversionTaskType request ) throws Exception {
    CancelConversionTaskResponseType reply = request.getReply( );
    /**
     * <ol>
     * <li>Persist cancellation request state
     * <li>Submit cancellations for any outstanding import sub-tasks to imaging service
     * </ol>
     */
    tasks.remove( request.getConversionTaskId( ) );
    return reply;
  }
  
  public DescribeConversionTasksResponseType describeConversionTasks( DescribeConversionTasksType request ) throws Exception {
    DescribeConversionTasksResponseType reply = request.getReply( );
    /**
     * <ol>
     * <li>Describe import tasks from persisted state
     * </ol>
     */
    reply.getConversionTasks( ).addAll( tasks.values( ) );
    return reply;
  }
}
