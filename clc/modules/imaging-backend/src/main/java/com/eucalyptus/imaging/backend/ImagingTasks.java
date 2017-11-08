/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.imaging.backend;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.eucalyptus.resources.client.Ec2Client;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.compute.common.ConversionTask;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.DiskImage;
import com.eucalyptus.compute.common.DiskImageDetail;
import com.eucalyptus.compute.common.DiskImageVolume;
import com.eucalyptus.compute.common.ImportInstanceLaunchSpecification;
import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.eucalyptus.compute.common.InstancePlacement;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.compute.common.backend.ImportInstanceType;
import com.eucalyptus.compute.common.backend.ImportVolumeType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.imaging.ImagingServiceProperties;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ImagingTasks {
  private static Logger    LOG                           = Logger.getLogger( ImagingTasks.class );
  public enum IMAGE_FORMAT {  RAW, PARTITION, KERNEL, RAMDISK, VMDK };
  private static Object lock = new Object();
  
  public static ImportVolumeImagingTask createImportVolumeTask(ImportVolumeType request) throws ImagingServiceException {
    /// sanity check
    /// availability zone
    final String availabilityZone = request.getAvailabilityZone();
    if(availabilityZone == null || availabilityZone.length()<=0)
      throw new ImagingServiceException("Availability zone is required");
    else{
      try{
        final List<String> clusters = ImagingServiceProperties.listConfiguredZones();
        if(!clusters.contains(availabilityZone))
          throw new ImagingServiceException(String.format("The availability zone %s is not configured for import", availabilityZone));
      }catch(final ImagingServiceException ex){
        throw ex;
      }catch(final Exception ex){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Failed to verify availability zone");
      }
    }
    
    // Image format
    final String format = request.getImage().getFormat();
    if(format==null || format.length()<=0)
      throw new ImagingServiceException("Image format is required");
    try{
      final IMAGE_FORMAT imgFormat = IMAGE_FORMAT.valueOf(format.toUpperCase());
    }catch(final Exception ex){
      throw new ImagingServiceException("Unsupported image format");
    }
    
    // Image bytes
    
    // Image.ImportManifestUrl
    final String manifestUrl = request.getImage().getImportManifestUrl();
    if(manifestUrl == null || manifestUrl.length()<=0)
      throw new ImagingServiceException("Import manifest url is required");
    /// TODO should check if the manifest url is present and accessible in S3

    try{
      /// TODO: should we assume the converted image is always larger than or equal to the uploaded image
      final int volumeSize = request.getVolume().getSize();
      if (getMaxVolumeSize(availabilityZone) < volumeSize)
        throw new ImagingServiceException("Requested volume size exceeds max allowed volume size");
      final long imageBytes = request.getImage().getBytes();
      final long volumeSizeInBytes = (volumeSize * (long) Math.pow(1024, 3));
      if(imageBytes > volumeSizeInBytes)
        throw new ImagingServiceException("Requested volume size is not enough to hold the image");
    }catch(final ImagingServiceException ex){
      throw ex;
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
          "Failed to verify the requested volume size");
    }
    
    final ImportVolumeImagingTask transform = TypeMappers.transform( request, ImportVolumeImagingTask.class );
    try ( final TransactionResource db =
        Entities.transactionFor( ImportVolumeImagingTask.class ) ) {
      try{
        Entities.persist(transform);
        db.commit( );
      }catch(final Exception ex){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Failed to persist VolumeImagingTask", ex);
      }
    }
    return transform;
  }
  
  public static ImportInstanceImagingTask createImportInstanceTask(final ImportInstanceType request)
      throws ImagingServiceException {
    // verify the input
    final ImportInstanceLaunchSpecification launchSpec = request.getLaunchSpecification();
    if(launchSpec==null)
      throw new ImagingServiceException("Launch specification is required");
    if(launchSpec.getArchitecture()==null || 
        !("i386".equals(launchSpec.getArchitecture()) || "x86_64".equals(launchSpec.getArchitecture())))
        throw new ImagingServiceException("Architecture should be either i386 or x86_64");
    int vmTypeDiskSize = -1;
    if(launchSpec.getInstanceType()==null)
      throw new ImagingServiceException("Instance type is required");
    else{
      try{
        final List<VmTypeDetails> vmTypes = 
            Ec2Client.getInstance().describeInstanceTypes(Contexts.lookup().getUser().getUserId());
        for(final VmTypeDetails type : vmTypes) {
            if (launchSpec.getInstanceType().equals(type.getName())) {
              vmTypeDiskSize = type.getDisk();
              break;
            }
        }
        if (vmTypeDiskSize < 0)
          throw new Exception();
      }catch(final Exception ex) {
        throw new ImagingServiceException("Instance type " + launchSpec.getInstanceType()+" is not found");
      }
    }
    
    if(launchSpec.getKeyName()!=null && launchSpec.getKeyName().length() > 0){
      try{
        final List<DescribeKeyPairsResponseItemType> keys =
            Ec2Client.getInstance().describeKeyPairs(Contexts.lookup().getUser().getUserId(),
                    Lists.newArrayList(launchSpec.getKeyName()));
        if(! launchSpec.getKeyName().equals(keys.get(0).getKeyName()))
          throw new Exception();
      }catch(final Exception ex){
        throw new ImagingServiceException("Key "+launchSpec.getKeyName()+" is not found");
      }
    }
    
    if(launchSpec.getGroupName()!=null && launchSpec.getGroupName().size()>0){
      final Set<String> requestedGroups = Sets.newHashSet(launchSpec.getGroupName());
      try{
        final List<SecurityGroupItemType> groups =
            Ec2Client.getInstance().describeSecurityGroups(Contexts.lookup().getUser().getUserId(), launchSpec.getGroupName());
        for(final SecurityGroupItemType group : groups) {
          requestedGroups.remove(group.getGroupName());
        }
        if(requestedGroups.size()>0)
          throw new Exception();
      }catch(final Exception ex) {
        throw new ImagingServiceException("Security groups are not found: " + Joiner.on(",").join(requestedGroups));
      }
    }

    String availabilityZone = null;
    if( Strings.emptyToNull( launchSpec.getSubnetId( ) ) != null ){
      try{
        final List<SubnetType> subnets =
            Ec2Client.getInstance().describeSubnets(
                    Contexts.lookup().getUser().getUserId(),
                    Lists.newArrayList(launchSpec.getSubnetId()));
        if( subnets.size( ) != 1 ) {
          throw new ImagingServiceException( "Subnet " + launchSpec.getSubnetId() + " not found" );
        }
        availabilityZone = subnets.get( 0 ).getAvailabilityZone( );
        final String cidr = subnets.get( 0 ).getCidrBlock( );
        final String privateIp = Strings.emptyToNull( launchSpec.getPrivateIpAddress( ) );
        if ( privateIp != null && !Cidr.parse( cidr ).contains( privateIp ) ) {
          throw new ImagingServiceException("Private IP "+privateIp+" not valid for subnet "+launchSpec.getSubnetId());
        }
      } catch( final Exception ex ){
        Exceptions.findAndRethrow( ex, ImagingServiceException.class );
        throw new ImagingServiceException("Subnet "+launchSpec.getSubnetId()+" not found");
      }
    }

    final List<String> clusters;
    try{
      clusters = ImagingServiceProperties.listConfiguredZones( );
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Failed to verify availability zones");
    }

    if ( availabilityZone == null ) {
      if ( launchSpec.getPlacement() != null ) {
        availabilityZone = launchSpec.getPlacement().getAvailabilityZone();
      } else if ( clusters.size() > 0 ) {  //Default: Amazon EC2 chooses a zone for you.
        availabilityZone = clusters.get( 0 );
      } else {
        throw new ImagingServiceException( ImagingServiceException.INTERNAL_SERVER_ERROR,
            "No availability zone is found in the Cloud" );
      }
    }
    if ( !clusters.contains( availabilityZone ) ) {
      throw new ImagingServiceException( String.format( "The availability zone %s is not configured for import", availabilityZone ) );
    }
    if ( request.getLaunchSpecification().getPlacement() == null )
      request.getLaunchSpecification().setPlacement( new InstancePlacement() );
    request.getLaunchSpecification().getPlacement().setAvailabilityZone( availabilityZone );

    List<DiskImage> disks = request.getDiskImageSet();
    if(disks==null || disks.size()<=0)
      throw new ImagingServiceException("Disk images are required");
    
    for(final DiskImage disk : disks){
      final DiskImageDetail imageDetail = disk.getImage();
      final String format = imageDetail.getFormat();
      if(format==null || format.length()<=0)
        throw new ImagingServiceException("Image format is required");
      try{
        final IMAGE_FORMAT imgFormat = IMAGE_FORMAT.valueOf(format.toUpperCase());
      }catch(final Exception ex){
        throw new ImagingServiceException("Unsupported image format: "+format);
      }
      if(imageDetail.getImportManifestUrl()==null)
        throw new ImagingServiceException("Import manifest url is required");
      // TODO: manifest at S3
      final DiskImageVolume volumeDetail = disk.getVolume();
      if(volumeDetail==null)
        throw new ImagingServiceException("Volume detail is required for disk image");
      
      try{
        /// TODO: should we assume the converted image is always larger than or equal to the uploaded image
        final int volumeSize = volumeDetail.getSize();
        if (volumeSize > vmTypeDiskSize) 
          throw new ImagingServiceException("Requested volume size exceeds max disk size of instance type '" + launchSpec.getInstanceType()+"'");
        if (getMaxVolumeSize(availabilityZone) < volumeSize)
          throw new ImagingServiceException("Requested volume size exceeds max allowed volume size");
        final long imageBytes = imageDetail.getBytes();
        final long volumeSizeInBytes = (volumeSize * (long) Math.pow(1024, 3));
        if(imageBytes > volumeSizeInBytes)
          throw new ImagingServiceException("Requested volume size is not enough to hold the image");
      }catch(final ImagingServiceException ex){
        throw ex;
      }catch(final Exception ex){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Failed to verify the requested volume size");
      }
    }
    final ImportInstanceImagingTask transform = TypeMappers.transform( request, ImportInstanceImagingTask.class );
    try ( final TransactionResource db =
        Entities.transactionFor( ImportInstanceImagingTask.class ) ) {
      try{
        Entities.persist(transform);
        db.commit( );
      }catch(final Exception ex){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Failed to persist InstanceImagingTask", ex);
      }
    }
    return transform;
  }

  private static int getMaxVolumeSize(String availabilityZone) {
    ConfigurableProperty prop = null;
    int maxVolSize = Integer.MAX_VALUE;
    try {
      prop = PropertyDirectory.getPropertyEntry(availabilityZone + ".storage.maxvolumesizeingb");
    } catch (IllegalAccessException ex) {}
    if (prop == null){
      return maxVolSize;
    }
    try {
      maxVolSize = Integer.parseInt(prop.getValue());
    } catch (NumberFormatException ex) {}
    return maxVolSize;
  }
/*
  public static DiskImagingTask createDiskImagingTask(final ImportImageType request)
      throws ImagingServiceException{
    /// sanity check
    if(request.getImage().getDiskImageSet()==null || request.getImage().getDiskImageSet().size()<=0)
      throw new ImagingServiceException("Image detail for imported image is required");
    if(request.getImage().getConvertedImage()==null)
      throw new ImagingServiceException("Image detail for converted image is required");
    
    for(final ImportDiskImageDetail image : request.getImage().getDiskImageSet()){
      final String format = image.getFormat();
      final String manifestUrl = image.getDownloadManifestUrl();
      final String imageId = image.getId();

      if(format==null || format.length()<=0)
        throw new ImagingServiceException("Image format is required");
      try{
        final IMAGE_FORMAT imgFormat = IMAGE_FORMAT.valueOf(format.toUpperCase());
      }catch(final Exception ex){
        throw new ImagingServiceException("Unsupported image format");
      }
      if(manifestUrl == null || manifestUrl.length()<=0)
        throw new ImagingServiceException("Import manifest url is required");
      if(imageId == null || imageId.length()<=0)
        throw new ImagingServiceException("Import image's id is required");
    }
    
    final ConvertedImageDetail converted = request.getImage().getConvertedImage();
    final String bucket = converted.getBucket();
    final String prefix = converted.getPrefix();
    final String arch = converted.getArchitecture();
    
    if(bucket==null || bucket.length()<=0)
      throw new ImagingServiceException("bucket name must be specified");
    if(prefix==null || prefix.length()<=0)
      throw new ImagingServiceException("prefix must be specified");
    if(arch==null || arch.length()<=0)
      throw new ImagingServiceException("architecture must be specified");
    
    DiskImagingTask transform = null;
    try{
      transform =TypeMappers.transform( request, DiskImagingTask.class );
    }catch(final Exception ex){
      if(ex.getCause() instanceof ImagingServiceException)
        throw (ImagingServiceException) ex.getCause();
      else
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, "Failed to create DiskImagingTask", ex);
    }
    try ( final TransactionResource db =
        Entities.transactionFor( DiskImagingTask.class ) ) {
      try{
        Entities.persist(transform);
        db.commit( );
      }catch(final Exception ex){
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "Failed to persist DiskImagingTask", ex);
      }
    }
    return transform;
  }
 */ 
  /************************* Methods for generic imaging tasks ************************/
  public static List<ImagingTask> getImagingTasks(){
    synchronized(lock){
      List<ImagingTask> result = Lists.newArrayList();
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        result = Entities.query(ImagingTask.named(), true);
      }
      return result;
    }
  }

  public static ImagingTask lookup(final String taskId) 
      throws NoSuchElementException {
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        ImagingTask found;
        try {
          found = Entities.uniqueResult(ImagingTask.named(taskId));
        } catch (TransactionException e) {
          throw Exceptions.toUndeclared(e);
        }
        return found;
      }
    }
  }
  
  public static void deleteTask(final ImagingTask task){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(ImagingTask.class ) ) {
        try{
          final ImagingTask entity = Entities.uniqueResult(task);
          Entities.delete(entity);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void setState(final ImagingTask task, final ImportTaskState state, final String stateReason)
      throws NoSuchElementException
  {
    setState(AccountFullName.getInstance(task.getOwnerAccountNumber()), task.getDisplayName(), state, stateReason);
  }
  
  public static void setState(final AccountFullName owningAccount, final String taskId, 
      final ImportTaskState state, final String stateReason) throws NoSuchElementException{
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        try{
          final ImagingTask task = Entities.uniqueResult(ImagingTask.named(owningAccount, taskId));
          task.setState(state);
          if(stateReason!=null)
            task.setStateReason(stateReason);
          final String externalState = state.getExternalTaskStateName();
          task.setTaskState(externalState);
          if(stateReason!=null)
            task.setTaskStatusMessage(stateReason);
          task.serializeTaskToJSON();
          task.updateTimeStamps();
          Entities.persist(task);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  // transit a task's state in a lock 
  public static void transitState(final ImagingTask task, final ImportTaskState before, 
      final ImportTaskState after, final String stateMessage) throws Exception{
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        try{
          final ImagingTask entity = Entities.uniqueResult(task);
          if(!before.equals(entity.getState()))
            throw new Exception("Current state is not "+before);
          entity.setState(after);
          if(stateMessage!=null)
            entity.setStateReason(stateMessage);
          final String externalState = after.getExternalTaskStateName();
          entity.setTaskState(externalState);
          if(stateMessage!=null)
            entity.setTaskStatusMessage(stateMessage);
          entity.serializeTaskToJSON();
          entity.updateTimeStamps();
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }  
    }
  }
  
  public static void updateTaskInJson(final ImagingTask task){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(ImagingTask.class ) ) {
        try{
          task.serializeTaskToJSON();
          final ImagingTask update = Entities.uniqueResult(task);
          update.setTaskInJsons(task.getTaskInJsons());
          Entities.persist(update);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void setWorkerId(final String taskId, final String workerId){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(ImagingTask.class ) ) {
        try{
          final ImagingTask entity = Entities.uniqueResult(ImagingTask.named(taskId));
          entity.setWorkerId(workerId);
          Entities.persist(entity);
          db.commit();
        }catch(final Exception ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void killAndRerunTask(final String taskId){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(ImagingTask.class ) ) {
        try{
          final ImagingTask entity = Entities.uniqueResult(ImagingTask.named(taskId));
          if(! ImportTaskState.CONVERTING.equals(entity.getState())){
            return;
          }
          entity.setState(ImportTaskState.PENDING);
          entity.setWorkerId(null);
          if(entity instanceof VolumeImagingTask){
            ((VolumeImagingTask)entity).clearDownloadManifesturl();
          }
          Entities.persist(entity);
          db.commit();
        }catch(final Exception ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static ImagingTask getConvertingTaskByWorkerId(final String workerId){
    final List<ImagingTask> tasks = getImagingTasks();
    for(final ImagingTask task : tasks){
      if(ImportTaskState.CONVERTING.equals(task.getState()) && workerId.equals(task.getWorkerId())){
        return task;
      }
    }
    return null;
  }
  
  public static void timeoutTask(final String taskId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingTask.class ) ) {
      try{
        final ImagingTask entity = Entities.uniqueResult(ImagingTask.named(taskId));
        entity.incrementTimeout();
        entity.resetTimeout();
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  /************************* Methods for volume imaging tasks ************************/
  public static List<VolumeImagingTask> getVolumeImagingTasks() {
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( VolumeImagingTask.class ) ) {
        final VolumeImagingTask sample = VolumeImagingTask.named();
        final List<VolumeImagingTask> tasks = Entities.query(sample, true);
        return tasks;
      }
    }
  }
  
  public static void setVolumeId(final ImportVolumeImagingTask task, final String volumeId) 
      throws NoSuchElementException{
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( ImportVolumeImagingTask.class ) ) {
        try{
          final ImportVolumeImagingTask update = Entities.uniqueResult(task);
          update.setVolumeId(volumeId);
          Entities.persist(update);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void updateBytesConverted(final String taskId, final String volumeId, long bytesConverted ){
    try ( final TransactionResource db =
        Entities.transactionFor(VolumeImagingTask.class ) ) {
      try{
        final VolumeImagingTask entity = Entities.uniqueResult(VolumeImagingTask.named(taskId));
        final ConversionTask task = entity.getTask();
        if(task.getImportVolume()!=null){
          task.getImportVolume().setBytesConverted(bytesConverted);
        }else if(task.getImportInstance()!=null && task.getImportInstance().getVolumes()!=null){
          final List<ImportInstanceVolumeDetail> volumes = task.getImportInstance().getVolumes();
          for(final ImportInstanceVolumeDetail volume : volumes){
            if(volume.getVolume()!=null && volumeId.equals(volume.getVolume().getId())){
              volume.setBytesConverted(bytesConverted);
            }
          }
        }
        entity.serializeTaskToJSON();
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void addDownloadManifestUrl(final VolumeImagingTask task, 
      final String importManifestUrl, final String downloadManifestUrl){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(VolumeImagingTask.class ) ) {
        try{
          final VolumeImagingTask entity = Entities.uniqueResult(task);
          entity.addDownloadManifestUrl(importManifestUrl, downloadManifestUrl);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void updateVolumeStatus(final VolumeImagingTask imagingTask, 
      final String volumeId, ImportTaskState state, final String statusMessage){
    try ( final TransactionResource db =
        Entities.transactionFor(VolumeImagingTask.class ) ) {
      try{
        final VolumeImagingTask entity = Entities.uniqueResult(imagingTask);
        final ConversionTask task = entity.getTask();
        if(task.getImportInstance()!=null){
          final List<ImportInstanceVolumeDetail> volumes = task.getImportInstance().getVolumes();
          for(final ImportInstanceVolumeDetail volumeDetail : volumes){
            if(volumeDetail.getVolume()!=null && volumeId.equals(volumeDetail.getVolume().getId())){
              volumeDetail.setStatus(state.getExternalVolumeStateName());
              if(statusMessage!=null)
                volumeDetail.setStatusMessage(statusMessage);
              else
                volumeDetail.setStatusMessage("");
              break;
            }
          }
          entity.serializeTaskToJSON();
          Entities.persist(entity);
          db.commit();
        }
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static boolean isConversionDone(final VolumeImagingTask imagingTask){
    try ( final TransactionResource db =
        Entities.transactionFor(VolumeImagingTask.class ) ) {
      try{
        final VolumeImagingTask entity = Entities.uniqueResult(imagingTask);
        final ConversionTask task = entity.getTask();
        if (task.getImportInstance()!=null){
          final List<ImportInstanceVolumeDetail> volumes = task.getImportInstance().getVolumes();
          for(final ImportInstanceVolumeDetail volumeDetail : volumes){
            if("active".equals(volumeDetail.getStatus()))
              return false;
          }
          return true;
        }else
          return true;
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  /************************* Methods for import-instance imaging tasks ************************/
  public static void addSnapshotId(final ImportInstanceImagingTask imagingTask, final String snapshotId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImportInstanceImagingTask.class ) ) {
      try{
        final ImportInstanceImagingTask entity = Entities.uniqueResult(imagingTask);
        entity.addSnapshotId(snapshotId);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void setImageId(final ImportInstanceImagingTask imagingTask, final String imageId){
    try ( final TransactionResource db =
        Entities.transactionFor(ImportInstanceImagingTask.class ) ) {
      try{
        final ImportInstanceImagingTask entity = Entities.uniqueResult(imagingTask);
        entity.setImageId(imageId);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
}
