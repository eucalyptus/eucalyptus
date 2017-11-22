/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.vm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.DescribeSnapshotsResponseType;
import com.eucalyptus.compute.common.DescribeSnapshotsType;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.cluster.callback.StartInstanceCallback;
import com.eucalyptus.cluster.callback.StopInstanceCallback;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.Snapshot;
import com.eucalyptus.compute.common.backend.CreateSnapshotResponseType;
import com.eucalyptus.compute.common.backend.CreateSnapshotType;
import com.eucalyptus.compute.common.internal.vm.VmCreateImageSnapshot;
import com.eucalyptus.compute.common.internal.vm.VmCreateImageTask;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.DeviceMapping;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.Images.DeviceMappingDetails;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.compute.common.internal.vm.VmCreateImageTask.CreateImageState;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceResponseType;

/**
 * @author Sang-Min Park
 *
 */
public class CreateImageTask {
	private static Logger LOG = Logger.getLogger( CreateImageTask.class );
	private static Map<String, CreateImageTask> createImageTasks =
			new ConcurrentHashMap<String, CreateImageTask>();

	/* from CreateImage request */
	private String accountNumber = null;
	private String instanceId = null;
	private String imageId = null;
	private Boolean noReboot = null;
	private List<BlockDeviceMappingItemType> blockDevices = null;

	public CreateImageTask(final String accountNumber, final String instanceId, final String imageId, final Boolean noReboot, List<BlockDeviceMappingItemType> blockDevices){
		this.accountNumber = accountNumber;
		this.instanceId = instanceId;
		this.imageId = imageId;
		this.noReboot = noReboot;
		this.blockDevices  = blockDevices;
	}

	//restore CreateImageTask object from VmInstance (DB record)
	static Function<VmInstance, CreateImageTask> RESTORE = new Function<VmInstance, CreateImageTask>(){
		@Override
		@Nullable
		public CreateImageTask apply(@Nullable VmInstance input) {
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
			final CreateImageTask newTask = new CreateImageTask(input.getOwnerAccountNumber(), instanceId, vmTask.getImageId(), noReboot, deviceMaps);
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
			if (!( Bootstrap.isOperational() &&
			          Topology.isEnabledLocally( Eucalyptus.class ) ) )
			          return;

			/*
			 * Check if CreateImageTask is held in memory.
			 * If not, restore from VmInstance (i.e., when service restarts)
			 */
			final List<VmInstance> candidates = VmInstances.list(
					null,
					Restrictions.and(
							VmInstance.criterion( VmState.RUNNING, VmState.STOPPED ),
							Restrictions.not( VmCreateImageTask.inState( VmCreateImageTask.idleStates( ) ) )
					),
					Collections.<String,String>emptyMap( ),
					new Predicate<VmInstance>(){
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
						LOG.debug(String.format("Restoring create image task for %s", candidate.getInstanceId()));
						final CreateImageTask task = RESTORE.apply(candidate);
						if(task!=null){
							createImageTasks.put(candidate.getInstanceId(), task);
							LOG.info(String.format("create image task for %s restored", candidate.getInstanceId()));
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
						LOG.debug(String.format("Stopping instance %s", task.instanceId));
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
			for(final CreateImageTask task : tasks) {
				VmInstance vm = null;
				try {
				  vm = task.getVmInstance();
				} catch (NoSuchElementException ex) {
				  return;
				}
				if(vm.getRuntimeState()!=null && "poweredOff".equals(vm.getRuntimeState().getGuestState())) {
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
						LOG.debug(String.format("Image %s is available", task.getImageId()));
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
							LOG.debug(String.format("Restarting instance %s", vm.getInstanceId()));
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
		try ( TransactionResource db =
		          Entities.transactionFor( VmInstance.class ) ) {
			 final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			 vm.getRuntimeState().setCreateImageTaskState(state);
			 Entities.persist(vm);
			 db.commit();
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }
	}

	/*
	 * Finds VmInstance. Throws NoSuchElementException if instance can't be found
	 */

	private VmInstance getVmInstance() throws NoSuchElementException {
	  try ( TransactionResource db =
        Entities.transactionFor( VmInstance.class ) ) {
      final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
      if(VmState.TERMINATED.equals(vm.getState()) || VmState.BURIED.equals(vm.getState()))
          throw new NoSuchElementException();
      return vm;
    }catch(NoSuchElementException ex){
      LOG.error("Unable to find the vm instance (terminated?) - " + this.instanceId);
      try{
        Images.setImageState(this.getImageId(), ImageMetadata.State.failed);
      }catch(final Exception innerEx){
        LOG.error("Unable to set image state as failed", ex);
      }
      createImageTasks.remove(this.instanceId);
      throw ex;
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
	}

	private static List<BlockDeviceMappingItemType> getDeviceMappingsFromImage(final String imageId){
		List<BlockDeviceMappingItemType> deviceMaps = Lists.newArrayList();
		try ( TransactionResource db =
		          Entities.transactionFor( VmInstance.class ) ) {
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
			LOG.info( String.format( "Starting create image task for %s : %s", this.instanceId, imageId ) );
			// will throw Exception if check failed
			this.validateVmInstance();
			try ( TransactionResource db =
			          Entities.transactionFor( VmInstance.class ) ) {
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
			}
			createImageTasks.put(this.instanceId, this);
			return;
		}catch(final Exception ex){
			throw ex;
		}
	}

	private String getInstanceImageId(){
		return this.getVmInstance().getImageId();
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

	private String getImageId(){
		return this.imageId;
	}

	private List<String> getSnapshotIds(){
		List<String> snapshots = null;
		try ( TransactionResource db =
		          Entities.transactionFor( VmInstance.class ) ) {
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
					LOG.debug(String.format("[%s] snapshot [%s] creating - %s", this.getImageId(), s.getSnapshotId(), s.getProgress()));
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
		// blockDevices might have devices that should be suppressed as well as ephemeral devices
		// first check if there are any volumes that should be suppressed
		List<String> suppressedDevice = new ArrayList<String>();
		for(BlockDeviceMappingItemType device : blockDevices){
			if(device.getNoDevice() != null)
				suppressedDevice.add(device.getDeviceName());
		}

		// device -> volume-id
		for(Map.Entry<String, String> vol:getInstanceNonRootVolumes()){
			if (!suppressedDevice.contains(vol.getKey()))
				volumesToSnapshot.add(vol);
		}

	    Boolean isRootDevice = true;
		for(final Map.Entry<String,String> volume: volumesToSnapshot){
			final String deviceName = volume.getKey();
			final String volumeId = volume.getValue();
			String snapshotId = null;
			try{
				final EucalyptusCreateSnapshotTask task = new EucalyptusCreateSnapshotTask(volumeId,
						String.format("Created by CreateImage(%s) for %s from %s", instanceId, getImageId(), volumeId));
				final CheckedListenableFuture<Boolean> result = task.dispatch();
				if(result.get()){
					 snapshotId = task.getSnapshotId();
				}else
					throw new EucalyptusCloudException(String.format("Unable to create the snapshot from volume %s", volumeId));
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
			LOG.debug(String.format("Created snapshot %s from volume %s for device %s", snapshotId, volumeId, deviceName));
			try ( TransactionResource db =
			          Entities.transactionFor( VmInstance.class ) ) {
				final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
				vm.getRuntimeState().getVmCreateImageTask().addSnapshot(deviceName, snapshotId, isRootDevice, isDeleteOnTerminate(volumeId));
				isRootDevice=false;
				Entities.persist(vm);
				db.commit();
			}catch(final Exception ex){
				LOG.error("failed to add new snapshot", ex);
				throw Exceptions.toUndeclared(ex);
			}
		}
	}

	private List<VmCreateImageSnapshot> getSnapshots(){
		try ( TransactionResource db =
		          Entities.transactionFor( VmInstance.class ) ) {
			final VmInstance vm = Entities.uniqueResult(VmInstance.named(this.instanceId));
			final List<VmCreateImageSnapshot> snapshots =
					Lists.newArrayList(vm.getRuntimeState().getVmCreateImageTask().getSnapshots());
			db.commit();
			return snapshots;
		}catch(final Exception ex){
			LOG.error("failed to pull snapshot info", ex);
			throw Exceptions.toUndeclared(ex);
		}
	}

	private class CreateImageStopInstanceCallback extends StopInstanceCallback{
		@Override
		public void fire(ClusterStopInstanceResponseType msg) {
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
		public void fire(ClusterStartInstanceResponseType msg) {
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
		final VmInstance vm = this.getVmInstance( );
		VmInstances.stopVmInstance( vm, new CreateImageStopInstanceCallback( ) );
	}

	private void startInstance() {
		final VmInstance vm = this.getVmInstance( );
    VmInstances.startVmInstance( vm, new CreateImageStartInstanceCallback( ) );
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

			final AccountFullName accountFullName = AccountFullName.getInstance(this.accountNumber);
			final ImageInfo updatedImage =
					Images.updateWithDeviceMapping(imageId, accountFullName, rootDeviceName, devices );
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}

	private class EucalyptusDescribeSnapshotTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus> {
		private List<Snapshot> snapshots = null;
		private List<String> snapshotIds = null;
		private EucalyptusDescribeSnapshotTask(final List<String> snapshotIds){
			this.snapshotIds = snapshotIds;
		}

		private DescribeSnapshotsType describeSnapshots(){
			final DescribeSnapshotsType req = new DescribeSnapshotsType();
			req.getFilterSet( ).add( CloudFilters.filter( "snapshot-id", this.snapshotIds ) );
			return req;
		}

		@Override
		void dispatchInternal(Checked<ComputeMessage> callback) {
			final DispatchingClient<ComputeMessage, Compute> client = this.getClient(Compute.class);
			client.dispatch(describeSnapshots(), callback);
		}

		@Override
		void dispatchSuccess(ComputeMessage response) {
			final DescribeSnapshotsResponseType resp = (DescribeSnapshotsResponseType) response;
			this.snapshots = resp.getSnapshotSet();
		}

		public  List<Snapshot>  getSnapshots(){
			return this.snapshots;
		}
	}

	private class EucalyptusCreateSnapshotTask extends EucalyptusActivityTask<ComputeMessage, Eucalyptus> {
		private String volumeId = null;
		private String snapshotId = null;
		private String description = null;
		private EucalyptusCreateSnapshotTask(final String volumeId, String description){
			this.volumeId = volumeId;
			this.description = description;
		}
		private CreateSnapshotType createSnapshot(){
			final CreateSnapshotType req = new CreateSnapshotType();
			req.setVolumeId(volumeId);
			req.setDescription(description);
			return req;
		}

		@Override
		void dispatchInternal(Checked<ComputeMessage> callback) {
			final DispatchingClient<ComputeMessage, Eucalyptus> client = this.getClient(Eucalyptus.class);
			client.dispatch(createSnapshot(), callback);
		}

		@Override
		void dispatchSuccess(ComputeMessage response) {
			final CreateSnapshotResponseType resp = (CreateSnapshotResponseType) response;
			this.snapshotId = resp.getSnapshot().getSnapshotId();
		}

		public String getSnapshotId(){
			return this.snapshotId;
		}
	}

	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
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

	    protected <T extends ComponentId> DispatchingClient<ComputeMessage, T> getClient( final Class<T> component ) {
			try{
				final DispatchingClient<ComputeMessage, T> client =
					new DispatchingClient<>(AccountFullName.getInstance( CreateImageTask.this.accountNumber ), component );
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
	}
}
