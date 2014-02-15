package com.eucalyptus.imaging;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.ImportVolumeType;

public class ImagingTasks {
  private static Logger    LOG                           = Logger.getLogger( ImagingTasks.class );
  public enum IMAGE_FORMAT {  VMDK , RAW , VHD };
  
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
        throw new ImagingServiceException("Requeted volume size is not enough to hold the image");
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
        throw new ImagingServiceException("Failed to persist VolumeImagingTask", ex);
      }
    }
    return transform;
  }
  
  public static VolumeImagingTask getVolumeImagingTask(final OwnerFullName owner, final String taskId) 
      throws NoSuchElementException{
    VolumeImagingTask task = null;
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
      final VolumeImagingTask sample = VolumeImagingTask.named(owner, taskId);
      try{
        task = Entities.uniqueResult(sample);
      }catch(final TransactionException ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
    
    return task;
  } 
  
  public static List<ImagingTask> getImagingTasks(final OwnerFullName owner, final List<String> taskIdList){
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
  
  // return all imaging tasks in DB
  public static List<ImagingTask> getImagingTasks(){
    List<ImagingTask> result = Lists.newArrayList();
    try ( final TransactionResource db =
        Entities.transactionFor( ImagingTask.class ) ) {
      result = Entities.query(ImagingTask.named(), true);
    }
    return result;
  }
  
  public static void setState(final ImagingTask task, final ImportTaskState state, final String stateReason)
      throws NoSuchElementException
  {
    setState(task.getOwner(), task.getDisplayName(), state, stateReason);
  }
  
  public static void setState(final OwnerFullName owner, final String taskId, 
      final ImportTaskState state, final String stateReason) throws NoSuchElementException{
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
      try{
        final ImagingTask task = Entities.uniqueResult(ImagingTask.named(owner, taskId));
        task.setState(state);
        if(stateReason!=null)
          task.setStateReason(stateReason);
        Entities.persist(task);
        db.commit();
      }catch(final TransactionException ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void setVolumeId(final VolumeImagingTask task, final String volumeId) 
      throws NoSuchElementException{
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
  
  public static void setDownloadManifestUrl(final VolumeImagingTask task, final String url)
    throws NoSuchElementException{
    try ( final TransactionResource db =
        Entities.transactionFor( VolumeImagingTask.class ) ) {
      try{
        final VolumeImagingTask update = Entities.uniqueResult(task);
        update.setDownloadManifestUrl(url);
        Entities.persist(update);
        db.commit();
      }catch(final TransactionException ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  } 
}
