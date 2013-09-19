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
 ************************************************************************/
package com.eucalyptus.vm;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cluster.callback.StartInstanceCallback;
import com.eucalyptus.cluster.callback.StopInstanceCallback;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.DeviceMapping;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.Images.DeviceMappingDetails;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.vm.VmCreateImageTask.CreateImageState;
import com.eucalyptus.vm.VmInstance.VmState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.EbsDeviceMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.Snapshot;

/**
 * @author Sang-Min Park
 *
 */
public class CreateImageTask {
	private static Logger LOG = Logger.getLogger( CreateImageTask.class );
	private static Map<String, CreateImageTask> createImageTasks = 
			new ConcurrentHashMap<String, CreateImageTask>();
	
	/* from CreateImage request */
	private String userId = null;
	private String accountAdminId = null;
	private String instanceId = null;
	private Boolean noReboot = null;
	private List<BlockDeviceMappingItemType> blockDevices = null;
	
	public CreateImageTask(final String userId, final String instanceId, final Boolean noReboot, List<BlockDeviceMappingItemType> blockDevices){
		this.userId = userId;
		this.instanceId = instanceId;
		this.noReboot = noReboot;
		this.blockDevices  = blockDevices;
	}
	
	//restore CreateImageTask object from VmInstance (DB record)
	static Function<VmInstance, CreateImageTask> RESTORE = new Function<VmInstance, CreateImageTask>(){
		@Override
		@Nullable
		public CreateImageTask apply(@Nullable VmInstance input) {
			final String userId = input.getOwnerUserId();
			final String instanceId = input.getDisplayName();
			Boolean noReboot =null;
			List<BlockDeviceMappingItemType> deviceMaps = null;
			
			final VmCreateImageTask vmTask = input.getRuntimeState().getVmCreateImageTask();
			if(vmTask==null)
				throw Exceptions.toUndeclared("Failed to find the VmCreateImageTask");
			
			if(vmTask.getImageId()!=null){
				try{
					deviceMaps = getDeviceMappingsFromImage(vmTask.getImageId());
				}catch(final Exception ex){
					LOG.error("failed to pull device mapping information from the image", ex);
				}
			}
			
			noReboot = vmTask.getNoReboot();
			final CreateImageTask newTask =
					new CreateImageTask(userId, instanceId, noReboot, deviceMaps);
			try{
				newTask.setAccountAdmin();
			}catch(final AuthException ex){
				throw Exceptions.toUndeclared("Failed to set account admin", ex);
			}
			
			try{
				final String partition = input.getPartition();
				final ServiceConfiguration sc = Topology.lookup(Storage.class, 
						Partitions.lookupByName(partition));
				final Collection<ServiceConfiguration> enabledSCs = Topology.enabledServices(Storage.class);
				if(enabledSCs.contains(sc))
					return newTask;
				else
					return null;
			}catch(final Exception ex){
				LOG.error("failed to check state of storage controller", ex);
				return null;
			}
		}
	};
		
	/* For each CreateImageTask, check its latest state and take appropriate action */
	public static class CreateImageTasksManager implements EventListener<ClockTick> {
		public static void register( ) {
		      Listeners.register( ClockTick.class, new CreateImageTasksManager() );
		}

		@Override
		public void fireEvent(ClockTick event) {
			if (!( Bootstrap.isFinished() &&
			          Topology.isEnabledLocally( Eucalyptus.class ) ) )
			          return;
			
			/*
			 * Check if CreateImageTask is held in memory.
			 * If not, restore from VmInstance (i.e., when service restarts)
			 */
			final List<VmInstance> candidates = VmInstances.list(new Predicate<VmInstance>(){
				@Override
				public boolean apply(@Nullable VmInstance input) {
					if((VmState.RUNNING.equals(input.getState()) || VmState.STOPPED.equals(input.getState()))
							&& input.isBlockStorage() 
							&& input.getRuntimeState().isCreatingImage())
						return true;
					else
						return false;
				}
			});
			
			try{
				for(final VmInstance candidate : candidates){
					if(!createImageTasks.containsKey(candidate.getInstanceId())){
						LOG.info(String.format("Restoring create image task for %s", candidate.getInstanceId()));
						final CreateImageTask task = RESTORE.apply(candidate);
						if(task!=null){
							createImageTasks.put(candidate.getInstanceId(), task);
							LOG.info(String.format("craete image task for %s restored", candidate.getInstanceId()));
						}
					}
				}
			}catch(final Exception ex){
				LOG.error("unable to retore create-image task", ex);
			}
			
			final Map<VmCreateImageTask.CreateImageState, List<CreateImageTask>> taskByState = 
					new HashMap<VmCreateImageTask.CreateImageState, List<CreateImageTask> >();
					
			for (final String instId : createImageTasks.keySet()){
				final CreateImageTask task = createImageTasks.get(instId);
				final CreateImageState taskState = task.getVmCreateImageTaskState();
				if(! taskByState.containsKey(taskState)){
					taskByState.put(taskState, Lists.<CreateImageTask>newArrayList());
				}
				taskByState.get(taskState).add(task);
			}
			
			if(taskByState.containsKey(CreateImageState.pending)){
				try{
					this.processPendingTasks(taskByState.get(VmCreateImageTask.CreateImageState.pending));
				}catch(final Exception ex){
					LOG.error("exception while processing pending CreateImageTask", ex);
				}
			}
			if(taskByState.containsKey(CreateImageState.guest_stopping)){
				try{
					this.processStoppingTasks(taskByState.get(VmCreateImageTask.CreateImageState.guest_stopping));
				}catch(final Exception ex){
					LOG.error("exception while processing stopping CreateImageTask", ex);
				}
			}
			if(taskByState.containsKey(CreateImageState.creating_snapshot)){
				try{
					this.processSnapshottingTasks(taskByState.get(VmCreateImageTask.CreateImageState.creating_snapshot));
				}catch(final Exception ex){
					LOG.error("exception while processing snapshotting CreateImageTask", ex);
				}
			}
			if(taskByState.containsKey(CreateImageState.guest_starting)){
				try{
					this.processStartingTasks(taskByState.get(VmCreateImageTask.CreateImageState.guest_starting));
				}catch(final Exception ex){
					LOG.error("exception while processing snapshotting CreateImageTask", ex);
				}
			}
			if(taskByState.containsKey(CreateImageState.complete)){
				try{
					this.processCompleteTasks(taskByState.get(VmCreateImageTask.CreateImageState.complete));
				}catch(final Exception ex){
					LOG.error("exception while processing complete CreateImageTask", ex);
				}
			}
			if(taskByState.containsKey(CreateImageState.failed)){
				try{
					this.processFailedTasks(taskByState.get(VmCreateImageTask.CreateImageState.failed));
				}catch(final Exception ex){
					LOG.error("exception while processing failed CreateImageTask", ex);
				}
			}
		}
		
		private void processPendingTasks(final List<CreateImageTask> tasks){
			/*
			 * for each pending task, stop instance if !noReboot;
			 *   otherwise, create snapshot;
			 */
			for(final CreateImageTask task : tasks){
				if(!task.noReboot){
					try{
						task.stopInstance();
						LOG.info(String.format("Stopping instance %s", task.instanceId));
					}catch(final Exception ex){
						LOG.error(String.format("failed to stop instance %s", task.instanceId), ex);
						task.setVmCreateImageTaskState(CreateImageState.failed);
					}
				}else{
					try{
						task.createSnapshot();
						task.setVmCreateImageTaskState(CreateImageState.creating_snapshot);
					}catch(final Exception ex){
						LOG.error(String.format("failed to create the snapshots from %s", task.instanceId), ex);
						task.setVmCreateImageTaskState(CreateImageState.failed);
					}
				}
			}
		}
		
		private void processStoppingTasks(final List<CreateImageTask> tasks){
			/*
			 * for each stopping instance, check the latest guestState
			 *   if stopped, then create snapshot;
			 *   otherwise, wait;
			 */
			for(final CreateImageTask task : tasks){
				final VmInstance vm = task.getVmInstance();
				if(vm.getRuntimeState()!=null && "poweredOff".equals(vm.getRuntimeState().getGuestState())){
					try{
						task.createSnapshot();
						task.setVmCreateImageTaskState(CreateImageState.creating_snapshot);
					}catch(final Exception ex){
						LOG.error(String.format("failed to create the snapshot from %s", task.instanceId), ex);
						task.setVmCreateImageTaskState(CreateImageState.failed);
					}
				} /// TODO: spark: should timeout?
			}
		}
		
		private void processSnapshottingTasks(final List<CreateImageTask> tasks){
			/*
			 * for each instance for which the snapshot is created from the root volume;
			 *     describe snapshot;
			 *     if snapshot is available:
			 *    	   register image with the snapshot id
			 *         if !noReboot: 
			 *             start instance;
			 *         else
			 *             mark complete	   
			 */	
			
			for(final CreateImageTask task : tasks){
				try{
					final List<String> status = task.getSnapshotStatus();
					boolean allDone = true;
					
					for(final String s : status){
						if("completed".equals(s)){
							;
						}else if("failed".equals(s)){
							throw new EucalyptusCloudException("Snapshot creation failed");
						}else
							allDone=false;
					}
					
					if(allDone){
						task.registerImage();
						LOG.info(String.format("Image %s is available", task.getImageId()));
						if(task.noReboot)
							task.setVmCreateImageTaskState(CreateImageState.complete);
					}else
						continue;
				}catch(final Exception ex){
					LOG.error("failed to register the image", ex);
					task.setVmCreateImageTaskState(CreateImageState.failed);
				}
				if(!task.noReboot){
					try{
						final VmInstance vm = task.getVmInstance();
						if(vm.getRuntimeState()!=null && "poweredOff".equals(vm.getRuntimeState().getGuestState())){
							task.startInstance();
							LOG.info(String.format("Restarting instance %s", vm.getInstanceId()));
						}
					}catch(final Exception ex){
						LOG.error(String.format("failed to start the instance %s", task.instanceId), ex);
						task.setVmCreateImageTaskState(CreateImageState.failed);
					}
				}
			}
		}
		
		private void processStartingTasks(final List<CreateImageTask> tasks){
			/*
			 * for each starting instance, check the guestState;
			 *    if guestState == "poweredOn":
			 *        mark complete
			 */
			for(final CreateImageTask task : tasks){
				try{
					final VmInstance vm = task.getVmInstance();
					if(vm.getRuntimeState()!=null && "poweredOn".equals(vm.getRuntimeState().getGuestState())){
						task.setVmCreateImageTaskState(CreateImageState.complete);
					}
				}catch(final Exception ex){
					LOG.error(String.format("failed to check the guest state for %s", task.instanceId), ex);
					task.setVmCreateImageTaskState(CreateImageState.failed);
				}
			}
		}
		
		private void processCompleteTasks(final List<CreateImageTask> tasks){
			for(final CreateImageTask task : tasks){
				LOG.info(String.format("CreateImage is done for instance %s; Image %s registered", task.instanceId, task.getImageId()));
				createImageTasks.remove(task.instanceId);
			}
		}
		
		private void processFailedTasks(final List<CreateImageTask> tasks){
			for(final CreateImageTask task : tasks){
				LOG.error(String.format("CreateImage has failed for instance %s", task.instanceId));
				try{
					Images.setImageState(task.getImageId(), ImageMetadata.State.failed);
				}catch(final Exception ex){
					LOG.error("Unable to set image state as failed");
				}
				createImageTasks.remove(task.instanceId);
			}
		}
	}
	
	private void setVmCreateImageTaskState(final VmCreateImageTask.CreateImageState state){
		 final EntityTransaction db = Entities.get( VmInstance.class );
		 try{
			 final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			 vm.getRuntimeState().setCreateImageTaskState(state);
			 Entities.persist(vm);
			 db.commit();
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}
	
	private VmInstance getVmInstance() {
		 final EntityTransaction db = Entities.get( VmInstance.class );
		 try{
			 final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			 db.commit();
			 return vm;
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}
	
	private static List<BlockDeviceMappingItemType> getDeviceMappingsFromImage(final String imageId){
		List<BlockDeviceMappingItemType> deviceMaps = Lists.newArrayList();
		final EntityTransaction db = Entities.get(BlockStorageImageInfo.class);
		try{
			final BlockStorageImageInfo image = (BlockStorageImageInfo) Entities.uniqueResult(BlockStorageImageInfo.named(imageId));
			final List<DeviceMapping> dmSet = image.getDeviceMappings();
			for(final DeviceMapping dm : dmSet){
				final BlockDeviceMappingItemType dmItem = DeviceMappingDetails.INSTANCE.apply(dm);
					deviceMaps.add(dmItem);
			}
			db.commit();
			return deviceMaps;
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}finally{
			if(db.isActive())
				db.rollback();
		}
	}
	
	private VmCreateImageTask.CreateImageState getVmCreateImageTaskState(){
		final VmInstance vm = getVmInstance();
		 if(vm.getRuntimeState()!=null){
			 return vm.getRuntimeState().getCreateImageTaskState();
		 }else
			 throw Exceptions.toUndeclared(
					 new EucalyptusCloudException("No Runtime state found for the instance"));
	}
	
	private void validateVmInstance() throws Exception{
		final VmInstance vm = this.getVmInstance();
		if(vm==null)
			throw new EucalyptusCloudException("Unable to find the vm");
		if( ! VmState.RUNNING.equals(vm.getState()) && ! VmState.STOPPED.equals(vm.getState()))
			throw new EucalyptusCloudException("Instance is not in running or stopped state");
		if(vm.getBootRecord()==null || ! vm.getBootRecord().isBlockStorage())
			throw new EucalyptusCloudException("createImage can be performed only to EBS-backed instances");
		final VmRuntimeState runtime = vm.getRuntimeState();
		if(runtime!=null && runtime.isCreatingImage()){
			throw new EucalyptusCloudException("Existing CreateImage task is found for the instance");
		}
		if(createImageTasks.containsKey(this.instanceId))
			throw new EucalyptusCloudException("Existing CreateImageTask is found");
	}
	
	public void create(final String imageId) throws Exception{
		try{
			LOG.info(String.format("Starting create image task for %s : %s", this.instanceId, imageId));
			// find account admin's user id
			this.setAccountAdmin();
			// will throw Exception if check failed
			this.validateVmInstance();
			final EntityTransaction db = Entities.get( VmInstance.class );
			try{
				final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
				if(VmState.STOPPED.equals(vm.getState()) && !this.noReboot){
					LOG.debug("Reboot is not possible for stopped instance");
					this.noReboot=true;
				}
				vm.getRuntimeState().resetCreateImageTask(CreateImageState.pending, imageId, null, this.noReboot);
				Entities.persist(vm);
				db.commit();
			}catch(NoSuchElementException ex){
				throw ex;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
			createImageTasks.put(this.instanceId, this);
			return;
		}catch(final Exception ex){
			throw ex;
		}
	}
	
	private void setAccountAdmin() throws AuthException{
		final User requestingUser = Accounts.lookupUserById(this.userId);
		final Account account = requestingUser.getAccount();
		final User adminUser = account.lookupAdmin();
		this.accountAdminId = adminUser.getUserId();
	}
	
	private String getInstanceImageId(){
		return this.getVmInstance().getImageId();
	}
	
	private String getInstanceArchitecture(){
		final ImageInfo image = Emis.LookupImage.INSTANCE.apply(this.getInstanceImageId());
		return image.getArchitecture().name();
	}
	
	/*
	 * @return key-value pair of device - volumeId mapping
	 */
	private Map.Entry<String, String> getInstanceRootVolume(){
		final VmInstance vm = this.getVmInstance();
		final List<Map.Entry<String, String>> rootVolumes = Lists.newArrayList();		
		
		vm.eachVolumeAttachment(new Predicate<VmVolumeAttachment>(){
			@Override
			public boolean apply(@Nullable VmVolumeAttachment input) {
				if(input.getIsRootDevice())
					rootVolumes.add( new AbstractMap.SimpleEntry<String,String>(input.getDevice(), input.getVolumeId()));
					return true;
			}
		});
		if(rootVolumes.size()>0)
			return rootVolumes.get(0);
		else
			throw Exceptions.toUndeclared(new EucalyptusCloudException("Root volume is not found"));
	}
	
	private List<Map.Entry<String, String>> getInstanceNonRootVolumes(){
		final VmInstance vm = this.getVmInstance();
		final List<Map.Entry<String, String>> volumes = Lists.newArrayList();	
		vm.eachVolumeAttachment(new Predicate<VmVolumeAttachment>(){
			@Override
			public boolean apply(@Nullable VmVolumeAttachment input) {
				if(! input.getIsRootDevice())
					volumes.add( new AbstractMap.SimpleEntry<String,String>(input.getDevice(), input.getVolumeId()));
					return true;
			}
		});
		return volumes;
	}
	
	private Boolean isDeleteOnTerminate(final String volumeId){
		final VmInstance vm = this.getVmInstance();
		final List<String> deleteOnTerminates = Lists.newArrayList();
		
		vm.eachVolumeAttachment(new Predicate<VmVolumeAttachment>(){
			@Override
			public boolean apply(@Nullable VmVolumeAttachment input) {
				if(input.getDeleteOnTerminate())
					deleteOnTerminates.add(input.getVolumeId());
				return true;
			}
		});
		return deleteOnTerminates.contains(volumeId);
	}
	
	private void setImageId(final String imageId){
		 final EntityTransaction db = Entities.get( VmInstance.class );
		 try{
			 final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			 vm.getRuntimeState().getVmCreateImageTask().setImageId(imageId);
			 Entities.persist(vm);
			 db.commit();
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}
	
	private String getImageId(){
		final VmInstance vm = this.getVmInstance();
		return vm.getRuntimeState().getVmCreateImageTask().getImageId();
	}
	
	private List<String> getSnapshotIds(){
		List<String> snapshots = null;
		final EntityTransaction db = Entities.get( VmInstance.class );
		 try{
			 final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			 snapshots = Lists.transform(Lists.newArrayList(vm.getRuntimeState().getVmCreateImageTask().getSnapshots()), 
					 new Function<VmCreateImageSnapshot, String>(){
						@Override
						@Nullable
						public String apply(@Nullable VmCreateImageSnapshot input) {
							return input.getSnapshotId();
						}
			 });
			 db.commit();
			 return snapshots;
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}

	private List<String> getSnapshotStatus() {
		try{
			final EucalyptusDescribeSnapshotTask task = new EucalyptusDescribeSnapshotTask(this.getSnapshotIds());
			final CheckedListenableFuture<Boolean> result = task.dispatch();
			if(result.get()){
				final List<Snapshot> snapshots = task.getSnapshots();
				final List<String> status = Lists.newArrayList();
				for(final Snapshot s : snapshots){
					LOG.info(String.format("snapshot creating - %s", s.getProgress()));
					status.add(s.getStatus());
				}
				return status;
			}else
				throw new EucalyptusCloudException("Unable to describe snapshots");
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	private void createSnapshot(){ 
		/* 
		 * take the snapshot of root device always
		 * if block-device-mapping is not requested explicitly, take snapshots of all attached volumes
		 */
		final List<Map.Entry<String, String>> volumesToSnapshot = 
				Lists.newArrayList();
		volumesToSnapshot.add(this.getInstanceRootVolume());
		if(this.blockDevices == null || this.blockDevices.size() <= 0){ // block device mapping not requested
			volumesToSnapshot.addAll(this.getInstanceNonRootVolumes());
		}
	    Boolean isRootDevice = true;
		for(final Map.Entry<String,String> volume: volumesToSnapshot){
			final String deviceName = volume.getKey();
			final String volumeId = volume.getValue();
			String snapshotId = null;
			try{
				final EucalyptusCreateSnapshotTask task = new EucalyptusCreateSnapshotTask(volumeId);
				final CheckedListenableFuture<Boolean> result = task.dispatch();
				if(result.get()){
					 snapshotId = task.getSnapshotId();
				}else
					throw new EucalyptusCloudException(String.format("Unable to create the snapshot from volume %s", volumeId));
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
			LOG.info(String.format("Created snapshot %s from volume %s for device %s", snapshotId, volumeId, deviceName));
			final EntityTransaction db = Entities.get( VmInstance.class );
			try{
				final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
				vm.getRuntimeState().getVmCreateImageTask().addSnapshot(deviceName, snapshotId, isRootDevice, isDeleteOnTerminate(volumeId));
				isRootDevice=false;
				Entities.persist(vm);
				db.commit();
			}catch(final Exception ex){
				LOG.error("failed to add new snapshot", ex);
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
		}
	}
	
	private List<VmCreateImageSnapshot> getSnapshots(){
		final EntityTransaction db = Entities.get( VmInstance.class );
		try{
			final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			final List<VmCreateImageSnapshot> snapshots =
					Lists.newArrayList(vm.getRuntimeState().getVmCreateImageTask().getSnapshots());
			db.commit();
			return snapshots;
		}catch(final Exception ex){
			LOG.error("failed to pull snapshot info", ex);
			throw Exceptions.toUndeclared(ex);
		}finally{
			if(db.isActive())
				db.rollback();
		}
	}
	
	private RunningInstancesItemType describeInstance(){
		try{
			final EucalyptusDescribeInstanceTask task = new EucalyptusDescribeInstanceTask();
			final CheckedListenableFuture<Boolean> result = task.dispatch();
			if(result.get()){
				return task.getResult();
			}else
				throw new EucalyptusCloudException(String.format("Failed to describe instance %s", this.instanceId));
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	private class CreateImageStopInstanceCallback extends StopInstanceCallback{
		@Override
		public void fire(StopInstanceResponseType msg) {
			if(!msg.get_return()){
				LOG.error(String.format("failed to stop instance %s", CreateImageTask.this.instanceId));
				CreateImageTask.this.setVmCreateImageTaskState(CreateImageState.failed);
			}else{
				CreateImageTask.this.setVmCreateImageTaskState(CreateImageState.guest_stopping);
			}
		}		
	}
	
	private class CreateImageStartInstanceCallback extends StartInstanceCallback{
		@Override
		public void fire(StartInstanceResponseType msg) {
			if(!msg.get_return()){
				LOG.error(String.format("failed to start instance %s", CreateImageTask.this.instanceId));
				CreateImageTask.this.setVmCreateImageTaskState(CreateImageState.failed);
			}else{
				final CreateImageState lastState = CreateImageTask.this.getVmCreateImageTaskState();
				if( !(lastState == CreateImageState.failed || lastState==  CreateImageState.complete))
					CreateImageTask.this.setVmCreateImageTaskState(CreateImageState.guest_starting);
			}
		}
	}
	
	private void stopInstance() {
		final VmInstance vm = this.getVmInstance();
		vm.getRuntimeState().stopVmInstance(new CreateImageStopInstanceCallback());
	}
	
	private void startInstance() {
		final VmInstance vm = this.getVmInstance();
		vm.getRuntimeState().startVmInstance(new CreateImageStartInstanceCallback());
	}

	private void registerImage(){
		try{
			final String imageId = this.getImageId();
			if(imageId==null)
				throw new EucalyptusCloudException("Image Id should be available before full registration");

			final List<VmCreateImageSnapshot> snapshots = this.getSnapshots();
			final List<BlockDeviceMappingItemType> devices = Lists.newArrayList();
			String rootDeviceName = null;
			for(final VmCreateImageSnapshot snapshot : snapshots){
				if(snapshot.isRootDevice())
					rootDeviceName = snapshot.getDeviceName();

				final BlockDeviceMappingItemType device = new BlockDeviceMappingItemType();
				device.setDeviceName(snapshot.getDeviceName());
				final EbsDeviceMapping ebsMap = new EbsDeviceMapping();
				ebsMap.setSnapshotId(snapshot.getSnapshotId());
				device.setEbs(ebsMap);
				devices.add(device);
			}

			final UserFullName accountAdmin = UserFullName.getInstance(this.accountAdminId);
			final ImageInfo updatedImage = 
					Images.updateWithDeviceMapping(imageId, accountAdmin, rootDeviceName, devices );
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
		
	private class EucalyptusDescribeSnapshotTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private List<Snapshot> snapshots = null;
		private List<String> snapshotIds = null;
		private EucalyptusDescribeSnapshotTask(final List<String> snapshotIds){
			this.snapshotIds = snapshotIds;
		}
		
		private DescribeSnapshotsType describeSnapshots(){
			final DescribeSnapshotsType req = new DescribeSnapshotsType();
			req.setSnapshotSet(Lists.newArrayList(this.snapshotIds));
			return req;
		}
		
		@Override
		void dispatchInternal(
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = this.getClient();
			client.dispatch(describeSnapshots(), callback);				
		}

		@Override
		void dispatchSuccess(
				EucalyptusMessage response) {
			final DescribeSnapshotsResponseType resp = (DescribeSnapshotsResponseType) response;
			this.snapshots = resp.getSnapshotSet();
		}
		
		public  List<Snapshot>  getSnapshots(){
			return this.snapshots;
		}
	}

	private class EucalyptusCreateSnapshotTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private String volumeId = null;
		private String snapshotId = null;
		private EucalyptusCreateSnapshotTask(final String volumeId){
			this.volumeId = volumeId;
		}
		private CreateSnapshotType createSnapshot(){
			final CreateSnapshotType req = new CreateSnapshotType();
			req.setVolumeId(volumeId);
			return req;
		}
		
		@Override
		void dispatchInternal(
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = this.getClient();
			client.dispatch(createSnapshot(), callback);				
		}

		@Override
		void dispatchSuccess(
				EucalyptusMessage response) {
			final CreateSnapshotResponseType resp = (CreateSnapshotResponseType) response;
			this.snapshotId = resp.getSnapshot().getSnapshotId();
		}
		
		public String getSnapshotId(){
			return this.snapshotId;
		}
	}
	
	private class EucalyptusDescribeInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private RunningInstancesItemType result = null;
		private EucalyptusDescribeInstanceTask(){}
		private DescribeInstancesType describeInstances(){
			final DescribeInstancesType req = new DescribeInstancesType();
			req.setInstancesSet(Lists.newArrayList(CreateImageTask.this.instanceId));
			return req;
		}
		
		@Override
		void dispatchInternal(
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = this.getClient();
			client.dispatch(describeInstances(), callback);				
		}

		@Override
		void dispatchSuccess(
				EucalyptusMessage response) {
			final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
			final List<RunningInstancesItemType> resultInstances = Lists.newArrayList();
			for(final ReservationInfoType res : resp.getReservationSet()){
				resultInstances.addAll(res.getInstancesSet());
			}
			if(resultInstances.size()>0)
				this.result = resultInstances.get(0);
		}
		
		public RunningInstancesItemType getResult(){
			return this.result;
		}
	}
	
	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
	    private volatile boolean dispatched = false;
	    
	    protected EucalyptusActivityTask(){}
	
	    final CheckedListenableFuture<Boolean> dispatch( ) {
	      try {
	        final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
	        dispatchInternal( new Callback.Checked<TM>(){
	          @Override
	          public void fireException( final Throwable throwable ) {
	            try {
	              dispatchFailure( throwable );
	            } finally {
	              future.set( false );
	            }
	          }
	
	          @Override
	          public void fire( final TM response ) {
	            try {
	              dispatchSuccess( response );
	            } finally {
	              future.set( true );
	            }
	          }
	        } );
	        dispatched = true;
	        return future;
	      } catch ( Exception e ) {
	        LOG.error( e, e );
	      }
	      return Futures.predestinedFuture( false );
	    }
	
	    abstract void dispatchInternal( Callback.Checked<TM> callback );
	
	    void dispatchFailure( Throwable throwable ) {
	      LOG.error( "CreateImage task error", throwable );
	    }
	
	    abstract void dispatchSuccess(TM response );
	    
	    protected DispatchingClient<EucalyptusMessage, Eucalyptus> getClient() {
			try{
				final DispatchingClient<EucalyptusMessage, Eucalyptus> client =
					new DispatchingClient<>(CreateImageTask.this.accountAdminId , Eucalyptus.class );
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
	}
}
