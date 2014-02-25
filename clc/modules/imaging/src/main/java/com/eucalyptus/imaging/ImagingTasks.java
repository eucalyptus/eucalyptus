/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.imaging;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.DiskImage;
import edu.ucsb.eucalyptus.msgs.DiskImageDetail;
import edu.ucsb.eucalyptus.msgs.DiskImageVolume;
import edu.ucsb.eucalyptus.msgs.ImportInstanceLaunchSpecification;
import edu.ucsb.eucalyptus.msgs.ImportInstanceType;
import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;
import edu.ucsb.eucalyptus.msgs.InstancePlacement;

public class ImagingTasks {
  private static Logger    LOG                           = Logger.getLogger( ImagingTasks.class );
  public enum IMAGE_FORMAT {  VMDK , RAW , VHD };
  private static Object lock = new Object();
  
  public static VolumeImagingTask createImportVolumeTask(ImportVolumeType request) throws ImagingServiceException {
    /// sanity check
    /// availability zone
    final String availabilityZone = request.getAvailabilityZone();
    if(availabilityZone == null || availabilityZone.length()<=0)
      throw new ImagingServiceException("Availability zone is required");
    else{
      try{
        final List<ClusterInfoType> clusters = 
            EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
        boolean zoneFound = false;
        for(final ClusterInfoType cluster : clusters ){
          if(availabilityZone.equals(cluster.getZoneName())){
            zoneFound = true;
            break;
          }
        }
        if(!zoneFound)
          throw new ImagingServiceException(String.format("The availability zone %s is not found", availabilityZone));
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
    
    final VolumeImagingTask transform = TypeMappers.transform( request, VolumeImagingTask.class );
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
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
  
  public static InstanceImagingTask createImportInstanceTask(final ImportInstanceType request) 
      throws ImagingServiceException {
    // verify the input
    final ImportInstanceLaunchSpecification launchSpec = request.getLaunchSpecification();
    if(launchSpec==null)
      throw new ImagingServiceException("Launch specification is required");
    if(launchSpec.getArchitecture()==null || 
        !("i386".equals(launchSpec.getArchitecture()) || "x86_64".equals(launchSpec.getArchitecture())))
        throw new ImagingServiceException("Architecture should be either i386 or x86_64");
    if(launchSpec.getInstanceType()==null)
      throw new ImagingServiceException("Instance type is required");
    

    List<ClusterInfoType> clusters = null;
    try{
        clusters=EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
    }catch(final Exception ex){
      throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
          "Failed to verify availability zone");    
    }
    String availabilityZone = null;
    if(launchSpec.getPlacement()!=null)
      availabilityZone = launchSpec.getPlacement().getAvailabilityZone();
    if(availabilityZone != null){
      boolean zoneFound = false;
      for(final ClusterInfoType cluster : clusters ){
        if(availabilityZone.equals(cluster.getZoneName())){
          zoneFound = true;
          break;
        }
      }
      if(!zoneFound)
        throw new ImagingServiceException(String.format("The availability zone %s is not found", availabilityZone));
    }else{
      if (clusters.size()>0){
        //Default: Amazon EC2 chooses a zone for you.
        availabilityZone = clusters.get(0).getZoneName();
        if(request.getLaunchSpecification().getPlacement()==null)
          request.getLaunchSpecification().setPlacement(new InstancePlacement());
        request.getLaunchSpecification().getPlacement().setAvailabilityZone(availabilityZone);
      }else
        throw new ImagingServiceException(ImagingServiceException.INTERNAL_SERVER_ERROR, 
            "No availability zone is found in the Cloud");
    }
    
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
    final InstanceImagingTask transform = TypeMappers.transform( request, InstanceImagingTask.class );
    try ( final TransactionResource db =
        Entities.transactionFor( InstanceImagingTask.class ) ) {
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
  
  public static List<ImagingTask> getImagingTasks(final OwnerFullName owner, final List<String> taskIdList){
    synchronized(lock){
      final List<ImagingTask> result = Lists.newArrayList();
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        final ImagingTask sample = ImagingTask.named(owner);
        final List<ImagingTask> tasks = Entities.query(sample, true);
        if(taskIdList!=null && taskIdList.size()>0){
          for(final ImagingTask candidate : tasks){
            if(taskIdList.contains(candidate.getDisplayName()))
              result.add(candidate);
          }
        }else
          result.addAll(tasks);
      }
      return result;
    }
  }
  
  // return all imaging tasks in DB
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
  
  public static void setState(final ImagingTask task, final ImportTaskState state, final String stateReason)
      throws NoSuchElementException
  {
    setState(task.getOwner(), task.getDisplayName(), state, stateReason);
  }
  
  public static void setState(final OwnerFullName owner, final String taskId, 
      final ImportTaskState state, final String stateReason) throws NoSuchElementException{
    synchronized(lock){
      //TODO NEED TO VALIDATE THE STATE TRANSITION (IN MEMORY STATE MAY BE DIFFERENT FROM PERSISTENT STATE
      
      try ( final TransactionResource db =
          Entities.transactionFor( ImagingTask.class ) ) {
        try{
          final ImagingTask task = Entities.uniqueResult(ImagingTask.named(owner, taskId));
          task.setState(state);
          if(stateReason!=null)
            task.setStateReason(stateReason);
          final String externalState = state.getExternalTaskStateName();
          task.getTask().setState(externalState);
          if(stateReason!=null)
            task.getTask().setStatusMessage(stateReason);
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
          entity.getTask().setState(externalState);
          if(stateMessage!=null)
            entity.getTask().setStatusMessage(stateMessage);
          entity.serializeTaskToJSON();
          entity.updateTimeStamps();
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }  
    }
  }
  
  public static void setVolumeId(final VolumeImagingTask task, final String volumeId) 
      throws NoSuchElementException{
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor( VolumeImagingTask.class ) ) {
        try{
          final VolumeImagingTask update = Entities.uniqueResult(task);
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
        Entities.transactionFor(ImagingTask.class ) ) {
      try{
        final ImagingTask entity = Entities.uniqueResult(ImagingTask.named(taskId));
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
  
  public static void addDownloadManifestUrl(final ImagingTask task, 
      final String importManifestUrl, final String downloadManifestUrl){
    synchronized(lock){
      try ( final TransactionResource db =
          Entities.transactionFor(ImagingTask.class ) ) {
        try{
          final ImagingTask entity = Entities.uniqueResult(task);
          entity.addDownloadManifestUrl(importManifestUrl, downloadManifestUrl);
          db.commit();
        }catch(final TransactionException ex){
          throw Exceptions.toUndeclared(ex);
        }
      }
    }
  }
  
  public static void save(final ImagingTask task){
    try ( final TransactionResource db =
        Entities.transactionFor(ImagingTask.class ) ) {
      try{
        final ImagingTask update = Entities.uniqueResult(task);
        Entities.persist(update);
        db.commit();
      }catch(final TransactionException ex){
        throw Exceptions.toUndeclared(ex);
      }
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
}
