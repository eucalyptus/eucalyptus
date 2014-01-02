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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.ImagingTask;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.ImageManifests;
import com.eucalyptus.images.ImportTaskState;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.Imaging;
import com.eucalyptus.images.ImagingTaskDao;
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
import edu.ucsb.eucalyptus.msgs.DiskImageDescription;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.DiskImageVolumeDescription;
import edu.ucsb.eucalyptus.msgs.ImportInstanceLaunchSpecification;
import edu.ucsb.eucalyptus.msgs.ImportInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportInstanceType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;
import edu.ucsb.eucalyptus.msgs.ImportVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

public class ImportManager {
	private static Logger LOG = Logger.getLogger( ImportManager.class );
	private static final Map<String, ImagingTask> tasks = Maps.newHashMap( );
	private static final int CONVERSION_EXPIRATION_TIMEOUT = 30; // configure?

	static {
		// load from DB on start up
		for(ImagingTask task:ImagingTaskDao.getInstance().loadAllFromDb()) {
			task.createTaskFromJSON();
			tasks.put(task.getId(), task);
			LOG.debug("Loaded " + task.getId() + " import task from DB");
		}
	}

	public static ImagingTask getConversionTask(String taskId) {
		return tasks.get(taskId);
	}

	public static void putConversionTask(String taskId, ImagingTask task) {
		if (tasks.put(taskId, task) == null) {
			// save new to DB
			ImagingTaskDao.getInstance().addToDb(task);
		} else {
			// update existing in DB
			ImagingTaskDao.getInstance().updateInDb(task);
		}
	}

	public static Iterator<Entry<String, ImagingTask>> getTasksIterator() {
		return tasks.entrySet().iterator();
	}

	/**
     * <ol>
     * <li>Persist import instance request state
     * </ol>
     */
	public ImportInstanceResponseType ImportInstance( final ImportInstanceType request ) throws Exception {
		LOG.info(request);

		final ImportInstanceResponseType reply = request.getReply( );
		ImagingTask task = new ImagingTask( new ConversionTask(), ImportTaskState.NEW, 0);
		ImportInstanceTaskDetails taskDetails = new ImportInstanceTaskDetails( );
		final UserFullName ufn = Contexts.lookup( ).getUserFullName( );

		for ( DiskImage diskImage : request.getDiskImageSet( ) ) {
			final DiskImageDetail imageDetails = diskImage.getImage( );
			final String manifestLocation = imageDetails.getImportManifestUrl( );
			final String imageDescription = diskImage.getDescription( );
			ImportInstanceVolumeDetail volumeDetail = new ImportInstanceVolumeDetail();
			volumeDetail.setBytesConverted(0L);
			volumeDetail.setDescription(imageDescription);
			volumeDetail.setAvailabilityZone(""); // TODO: set AZ
			volumeDetail.setVolume(new DiskImageVolumeDescription(diskImage.getVolume().getSize(), "")); // TODO what should be put to id?
			volumeDetail.setImage(new DiskImageDescription(imageDetails.getFormat(), imageDetails.getBytes(), imageDetails.getImportManifestUrl(), "")); // TODO: checksum?
			volumeDetail.setStatus(ImportTaskState.NEW.getExternalVolumeStateName());
			volumeDetail.setStatusMessage(ImportTaskState.NEW.getExternalVolumeStatusMessage());
			taskDetails.getVolumes().add(volumeDetail);
		/*  
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
		*/
		}

		String taskId = Crypto.generateId( request.getCorrelationId( ), "import-i" );
		final String instanceId = Crypto.generateId( request.getCorrelationId( ), "i" );
		final String reservationId = Crypto.generateId( request.getCorrelationId( ), "r" );
		final String imageId = Crypto.generateId( request.getCorrelationId( ), "emi" );
		task.setId(taskId);
		Date expiration = Dates.hoursFromNow( CONVERSION_EXPIRATION_TIMEOUT );
		task.getTask().setExpirationTime( expiration.toString( ) );
		task.getTask().setState( ImportTaskState.NEW.getExternalTaskStateName() );
		// taskDetails.setInstanceId( instanceId );
		taskDetails.setPlatform( request.getPlatform( ) );
		taskDetails.setDescription( request.getDescription( ) );
		task.getTask().setImportInstance( taskDetails );
		// task.setStatusMessage( LogUtil.dumpObject( request ) );
		/*
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
		*/
		reply.setConversionTask(task.getTask());
		putConversionTask(taskId, task);
		return reply;
	}

	/**
     * <ol>
     * <li>Persist import volume request state
     * </ol>
     */
	public ImportVolumeResponseType importVolume( ImportVolumeType request ) throws Exception {
		LOG.info(request);
		if (true) throw new ComputeException( "InternalError", "Service not available" );
		ImportVolumeResponseType reply = request.getReply( );
		return reply;
	}

    /**
     * <ol>
     * <li>Persist cancellation request state
     * <li>Submit cancellations for any outstanding import sub-tasks to imaging service
     * </ol>
     */
	public CancelConversionTaskResponseType CancelConversionTask( CancelConversionTaskType request ) throws Exception {
		LOG.info(request);
		CancelConversionTaskResponseType reply = request.getReply( );
		// todo? change state?
		tasks.remove( request.getConversionTaskId( ) );
		return reply;
	}

    /**
     * <ol>
     * <li>Describe import tasks from persisted state
     * </ol>
     */
	public DescribeConversionTasksResponseType DescribeConversionTasks( DescribeConversionTasksType request ) throws Exception {
		LOG.debug(request);
		DescribeConversionTasksResponseType reply = request.getReply( );
		//TODO: extends for volumes
		for(ImagingTask task:tasks.values()){
			ConversionTask t = task.getTask();
			if (task.getBytesProcessed()>0 && t.getImportInstance().getVolumes().size() == 1) {
				ImportInstanceVolumeDetail vd = t.getImportInstance().getVolumes().get(0);
				vd.setBytesConverted(task.getBytesProcessed());
				vd.setStatus(task.getState().getExternalVolumeStateName());
				vd.setStatusMessage(task.getState().getExternalVolumeStatusMessage());
			}
			t.setState(task.getState().getExternalTaskStateName());
			t.setStatusMessage(task.getState().toString().toLowerCase());
			reply.getConversionTasks().add(t);
		}
		return reply;
	}
}
