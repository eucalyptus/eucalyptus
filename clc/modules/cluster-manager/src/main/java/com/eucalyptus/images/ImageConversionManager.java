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
 ************************************************************************/
package com.eucalyptus.images;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.images.Emis.LookupMachine;
import com.eucalyptus.imaging.ConvertedImageDetail;
import com.eucalyptus.imaging.DescribeConversionTasksResponseType;
import com.eucalyptus.imaging.DescribeConversionTasksType;
import com.eucalyptus.imaging.DiskImageConversionTask;
import com.eucalyptus.imaging.Imaging;
import com.eucalyptus.imaging.ImagingMessage;
import com.eucalyptus.imaging.ImportDiskImage;
import com.eucalyptus.imaging.ImportDiskImageDetail;
import com.eucalyptus.imaging.ImportImageResponseType;
import com.eucalyptus.imaging.ImportImageType;
import com.eucalyptus.imaging.manifest.BundleImageManifest;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstance.VmState;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DeleteResourceTag;
import edu.ucsb.eucalyptus.msgs.DeleteTagsResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteTagsType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ResourceTag;

/**
 * @author Sang-Min Park
 *
 */
public class ImageConversionManager implements EventListener<ClockTick> {
  private static Logger LOG  = Logger.getLogger( ImageConversionManager.class );
  public static void register( ) {
      Listeners.register( ClockTick.class, new ImageConversionManager() );
  }
  
  @Override
  public void fireEvent(ClockTick event) {
    if (!( Bootstrap.isFinished() &&
         Topology.isEnabledLocally( Eucalyptus.class)  ) )
       return;
    
    /// check the state of emis
    final List<ImageInfo> partitionedImages = getPartitionedImages();
    final List<ImageInfo> newConversion = Lists.newArrayList(Collections2.filter(partitionedImages, new Predicate<ImageInfo>(){
      @Override
      public boolean apply(ImageInfo arg0) {
        if(arg0 instanceof MachineImageInfo){
          final MachineImageInfo image = (MachineImageInfo) arg0;
          return ImageMetadata.State.pending_conversion.equals(image.getState()) &&
              (image.getImageConversionId()==null || image.getImageConversionId().length()<=0);
        }else
          return false;
      }
    }));
    
    /// check the task id
    final List<ImageInfo> inConversion = Lists.newArrayList(Collections2.filter(partitionedImages, new Predicate<ImageInfo>(){
      @Override
      public boolean apply(ImageInfo arg0) {
        if(arg0 instanceof MachineImageInfo){
          final MachineImageInfo image = (MachineImageInfo) arg0;
          return (image.getImageConversionId()!=null && image.getImageConversionId().length()>0) && 
              (ImageMetadata.State.pending_conversion.equals(image.getState()) || 
                  ImageMetadata.State.deregistered_cleanup.equals(image.getState()));
        }else
          
          return false;
      }
    }));
    
    if(! Topology.isEnabled(Imaging.class) &&
       ! ( newConversion.isEmpty() && inConversion.isEmpty())){
      LOG.warn("To convert partitioned images, Imaging Service should be enabled");
      return;
    }
    if(! Topology.isEnabled(ObjectStorage.class) &&
        ! newConversion.isEmpty()){
      LOG.warn("To convert partitioned images, Object Storage Service should be enabled");
      return;
    }
    
    try{
      createBuckets(newConversion);
      convertImages(newConversion);
    }catch(final Exception ex){
      LOG.error("Failed to convert images", ex);
    }
    
    try{
      checkConversion(inConversion);
    }catch(final Exception ex){
      LOG.error("Failed to check and update images in conversion", ex);
    }
    
    // clean up objects and buckets of any deregistered images
    final List<ImageInfo> cleanupImages = 
        Lists.newArrayList(Collections2.filter(this.getDeregisteredCleanupImages(), new Predicate<ImageInfo>(){
          @Override
          public boolean apply(ImageInfo arg0) {
            // the conversion task may be still in progress by workers
            return ! (ImageMetadata.State.pending_conversion.equals(arg0.getLastState()));
          }
        }));
    try{
      cleanupBuckets(cleanupImages, true);
    }catch(final Exception ex){
      LOG.error("Failed to clean up objects and bucket of deregistered images", ex);
    }

    try{
      this.updateTags(Lists.transform(partitionedImages, new Function<ImageInfo, String>(){
        @Override
        public String apply(ImageInfo arg0) {
          return arg0.getDisplayName();
        }
      }));
    }catch(final Exception ex){
      LOG.error("Failed to tag images and instances in conversion", ex);
    }
  }
  
  public static final String BUCKET_PREFIX = "euca-internal";
  private void createBuckets(final List<ImageInfo> images) throws Exception {
    Set<String> systemBuckets = null;
    if(images.size()>0){
      try{
        final ListBucketsTask task = new ListBucketsTask();
        final CheckedListenableFuture<Boolean> result = task.dispatch();
        if(result.get()){
          final List<String> bucketNames = task.getBuckets();
          systemBuckets = Sets.newHashSet();
          systemBuckets.addAll(bucketNames);
        }
      }catch(final Exception ex){
        throw new Exception("Failed to check the existing buckets", ex);
      }
    }
    
    for(final ImageInfo image: images){
      try{
        if(!(image instanceof MachineImageInfo))
          continue;
        final MachineImageInfo machineImage = (MachineImageInfo) image;

        final String manifestLocation = machineImage.getManifestLocation();
        final String[] tokens = manifestLocation.split("/");
        final String bucketName = tokens[0];
        final String prefix = tokens[1].replace(".manifest.xml", "");

        String newBucket = null;
        do{
          newBucket = String.format("%s-%s-%s", 
              BUCKET_PREFIX, Crypto.generateAlphanumericId(5, ""), bucketName);
          if(newBucket.length()>63){
            newBucket = String.format("%s-%s", BUCKET_PREFIX, Crypto.generateAlphanumericId(8, ""));
          }
          newBucket = newBucket.toLowerCase();
        }while (systemBuckets.contains(newBucket));

        try{
          final CreateBucketTask task = new CreateBucketTask(newBucket);
          final CheckedListenableFuture<Boolean> result = task.dispatch();
          if(result.get()){
            ;
          }
        }catch(final Exception ex){
          throw new Exception("Failed to create a system-owned bucket for converted image", ex);
        }

        // set run manifest path
        final String runManifestPath = String.format("%s/%s.manifest.xml", newBucket, prefix);
        try{
          machineImage.setRunManifestLocation(runManifestPath);
          Images.setRunManifestLocation(machineImage.getDisplayName(), runManifestPath);
        }catch(final Exception ex){
          throw new Exception("Failed to update run manifest location");
        }
      }catch(final Exception ex){
        try{
          Images.setImageState(image.getDisplayName(), ImageMetadata.State.failed);
        }catch(final Exception ex2){
          ;
        }
        throw ex;
      }
    }
  }
  
  private void cleanupBuckets(final List<ImageInfo> images, boolean deregister){
    Set<String> systemBuckets = null;
    if(images.size()>0){
      try{
        final ListBucketsTask task = new ListBucketsTask();
        final CheckedListenableFuture<Boolean> result = task.dispatch();
        if(result.get()){
          final List<String> bucketNames = task.getBuckets();
          systemBuckets = Sets.newHashSet();
          systemBuckets.addAll(bucketNames);
        }
      }catch(final Exception ex){
        ;
      }
    }
    
    for(final ImageInfo image: images) {
      if(!(image instanceof MachineImageInfo))
        continue;
      try{
        final MachineImageInfo machineImage = (MachineImageInfo) image;
        final String runManifestPath = machineImage.getRunManifestLocation();
        final String manifestPath = machineImage.getManifestLocation();
        
        if(runManifestPath==null || runManifestPath.length()<=0)
          continue;
        if(runManifestPath.equals(manifestPath)) // should not delete if runManifest is not system-generated one
          continue;
        
        final String[] tokens = runManifestPath.split("/");
        final String bucket = tokens[0];
        if(!systemBuckets.contains(bucket))
          continue;
        
        final ListObjectsInBucketTask listObjTask = new ListObjectsInBucketTask(bucket);
        CheckedListenableFuture<Boolean> result = listObjTask.dispatch();
        List<String> keysInBucket = null;
        if(result.get()){
          keysInBucket = listObjTask.getKeyNames();
        }else
          throw new Exception("Failed to list bucket");
        
        for(final String key : keysInBucket){
          final DeleteObjectTask delObjTask = new DeleteObjectTask(bucket, key);
          final CheckedListenableFuture<Boolean> delResult = delObjTask.dispatch();
          if(delResult.get()){
            ;
          }else{
            throw new Exception("Failed to delete object "+bucket+":"+key);
          }
        }
        
        final DeleteBucketTask delBucketTask = new DeleteBucketTask(bucket);
        result = delBucketTask.dispatch();
        if(result.get()){
          ;
        }else
          throw new Exception("Failed to delete the bucket "+bucket);
        
        if(deregister)
          Images.setImageState(image.getDisplayName(), ImageMetadata.State.deregistered);
      }catch(final Exception ex){
        LOG.error("Failed to cleanup buckets and objects of deregistered image "+image.getDisplayName(), ex);
      }
    }
  }
  
  private void convertImages(final List<ImageInfo> images){
   for(final ImageInfo image: images){
     if(!(image instanceof MachineImageInfo))
       continue;
     try{
       final MachineImageInfo machineImage = (MachineImageInfo) image;
       final String kernelId = machineImage.getKernelId();
       final String ramdiskId = machineImage.getRamdiskId();
       if(kernelId==null || ramdiskId ==null)
         throw new Exception("Kernel and ramdisk are not found for the image");
       
       final KernelImageInfo kernel = Images.lookupKernel(kernelId);
       final RamdiskImageInfo ramdisk = Images.lookupRamdisk(ramdiskId);
       final ServiceConfiguration osg = Topology.lookup( ObjectStorage.class );
       final URI osgUri = osg.getUri();
       final String osgPrefix = 
           String.format("%s://%s:%d%s", osgUri.getScheme(), osgUri.getHost(), osgUri.getPort(), osgUri.getPath());

       final String kernelManifest = String.format("%s/%s", osgPrefix, kernel.getManifestLocation());
       final String ramdiskManifest = String.format("%s/%s", osgPrefix, ramdisk.getManifestLocation());
       final String machineManifest = String.format("%s/%s", osgPrefix, machineImage.getManifestLocation());
       final String[] tokens = machineImage.getRunManifestLocation().split("/");
       final String bucket = tokens[0];
       final String prefix = tokens[1].replace(".manifest.xml","");
       
       String taskId = null;
       try{
         final ImportImageBuilder builder = new ImportImageBuilder();
         final ImportImageTask task = new ImportImageTask(builder
             .withArchitecture(machineImage.getArchitecture().name())
             .withBucket(bucket)
             .withPrefix(prefix)
             .withKernel(kernel.getDisplayName(), kernelManifest, kernel.getImageSizeBytes())
             .withRamdisk(ramdisk.getDisplayName(), ramdiskManifest, ramdisk.getImageSizeBytes())
             .withMachineImage(machineImage.getDisplayName(), machineManifest, machineImage.getImageSizeBytes())
             .withBucketUploadPolicy(bucket, prefix));
         final CheckedListenableFuture<Boolean> result = task.dispatch();
         if(result.get()){
           taskId = task.getTaskId();
         }
       }catch(final Exception ex){
         throw ex;
       }
       if(taskId == null)
         throw new Exception("ImportImage Task id is null");
       Images.setConversionTaskId(machineImage.getDisplayName(), taskId);
     }catch(final Exception ex) {
       LOG.error("Failed to convert the image "+image.getDisplayName(), ex);
       try{
         this.cleanupBuckets(Lists.newArrayList(image), false);
       }catch(final Exception ex2){
         ;
       }
       try{
         Images.setImageState(image.getDisplayName(), ImageMetadata.State.failed);
       }catch(final Exception ex2){
         ;
       }
     }
   }
  }
  
  private void checkConversion(final List<ImageInfo> images){
    for(final ImageInfo image : images){
      if(!(image instanceof MachineImageInfo))
        continue;
      try{
        final MachineImageInfo machineImage = (MachineImageInfo) image;
        final String taskId = machineImage.getImageConversionId();
        if(taskId==null || taskId.length()<=0)
          throw new Exception("Image "+machineImage.getDisplayName()+" has no conversion task Id");
        
        final DescribeConversionTasks task = 
            new DescribeConversionTasks(Lists.newArrayList(machineImage.getImageConversionId()));
            
       final CheckedListenableFuture<Boolean> result = task.dispatch();
       List<DiskImageConversionTask> ctasks = null;
       if(result.get()){
         ctasks = task.getTasks();
       }
       boolean conversionSuccess=true;
       String errorMessage = null;
       if(ctasks.size()<=0){
         /// consider this task as done when describe tasks has no result
         conversionSuccess=true;
       }else{
         DiskImageConversionTask ct = ctasks.get(0);
         if("completed".equals(ct.getState())){
           conversionSuccess=true;
         }else if("active".equals(ct.getState())){
           continue;
         }else{
           conversionSuccess=false;
           errorMessage = ct.getStatusMessage();
         }
       }
       if(conversionSuccess){
         /// if user deregistered the image while in conversion
          if(ImageMetadata.State.deregistered_cleanup.equals(image.getState())){
            try{
              this.cleanupBuckets(Lists.newArrayList(image), true);
            }catch(final Exception ex){
              ;
            }
          }else{
           Images.setImageFormat(machineImage.getDisplayName(), ImageMetadata.ImageFormat.fulldisk);
           /// the service and the backend (NC) rely on virtualizationType=HVM when they prepare full-disk type instances
           Images.setImageState(machineImage.getDisplayName(), ImageMetadata.State.available);
           try{
             generateDownloadManifests(machineImage.getDisplayName());
           }catch(final Exception ex){
             ;
           }
          }
       }else{
         Images.setImageState(machineImage.getDisplayName(), ImageMetadata.State.failed);
         try{
           this.cleanupBuckets(Lists.newArrayList(image), false);
         }catch(final Exception ex){
           ;
         }
         LOG.error("Failed to convert partitioned image: "+errorMessage);
       }
      }catch(final Exception ex){
        try{
          Images.setImageState(image.getDisplayName(), ImageMetadata.State.failed);
          try{
            this.cleanupBuckets(Lists.newArrayList(image), false);
          }catch(final Exception ex2){
            ;
          }
          LOG.error("Failed to convert partitioned image", ex);
        }catch(final Exception ex1){
          ;
        }
      }
    }
  }
  
  private void generateDownloadManifests(final String imageId){
    // lookup all reservations that reference the newly available image id
    final List<VmInstance> pendingInstances = VmInstances.list(new Predicate<VmInstance>(){
      @Override
      public boolean apply(VmInstance arg0) {
        return imageId.equals(arg0.getBootRecord().getMachineImageId()) && 
            VmState.PENDING.equals(arg0.getState());
      }
    });
    for(final VmInstance instance : pendingInstances){
      final String reservationId = instance.getReservationId();
      final String partitionName = instance.getPartition();
      final Partition partition = Partitions.lookupByName(partitionName);
      final MachineImageInfo machineImage = LookupMachine.INSTANCE.apply(imageId);
      try{
        final String manifestLocation = DownloadManifestFactory.generateDownloadManifest(
            new ImageManifestFile(machineImage.getRunManifestLocation(), BundleImageManifest.INSTANCE ), 
            partition.getNodeCertificate().getPublicKey(), reservationId);
        LOG.debug(String.format("Generated download manifest for instance %s", instance.getDisplayName()));
      }catch(final Exception ex){
        LOG.error(String.format("Failed to generate download manifest for instance %s", instance.getDisplayName()), ex);
      }
    }
  }
  
  private List<ImageInfo> getPartitionedImages(){
    List<ImageInfo> partitionedImages = null;
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      partitionedImages = Entities.query(Images.exampleWithImageFormat(ImageMetadata.ImageFormat.partitioned));
      
    }
    return partitionedImages;
  }
  
  private List<ImageInfo> getDeregisteredCleanupImages(){
    List<ImageInfo> images = null;
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      images = Entities.query(Images.exampleWithImageState(ImageMetadata.State.deregistered_cleanup));
    }
    return images;
  }
  
  private List<ImageInfo> getPendingConversionImages(){
    List<ImageInfo> pendingImages = null;
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      pendingImages = Entities.query(Images.exampleWithImageState(ImageMetadata.State.pending_conversion));
      
    }
    return pendingImages;
  }
  
  private static final LoadingCache<String, Optional<DiskImageConversionTask>> conversionTaskCache =
      CacheBuilder.newBuilder()
      .maximumSize(250)
      .expireAfterWrite(20, TimeUnit.SECONDS)
      .build( new CacheLoader<String, Optional<DiskImageConversionTask>>(){
        @Override
        public Optional<DiskImageConversionTask> load(String taskId)
            throws Exception {
          try{
            final DescribeConversionTasks task = 
                new DescribeConversionTasks(Lists.newArrayList(taskId));
            final CheckedListenableFuture<Boolean> result = task.dispatch();
            if(result.get()){
              return Optional.of(task.getTasks().get(0));
            }
          }catch(final Exception ex){
            LOG.error("Failed to call describe-conversion-task: "+taskId);
          }
          return Optional.absent();
        }
      });

  private static Set<String> taggedImages = Sets.newHashSet();
  private void updateTags(final List<String> images) throws Exception{
    for(final String imageId : images){
      try{
        final ImageInfo image = Images.lookupImage(imageId);
        final ImageMetadata.State imgState = image.getState();
        final String taskId = ((MachineImageInfo) image).getImageConversionId();
        if(ImageMetadata.State.pending_available.equals(imgState)){
          ; // do nothing for images not yet in conversion
        }else if (ImageMetadata.State.pending_conversion.equals(imgState)){
          String message = "";
          try{
            Optional<DiskImageConversionTask> task = 
               conversionTaskCache.get(taskId);
            if(task.isPresent()){
              message = task.get().getStatusMessage();
            }
          }catch(final Exception ex){
            ;
          }
          // if needed, we can add messages as well; not sure yet if the messages are clear
          this.tagResources(imageId, "active", message);
          taggedImages.add(imageId);
        }else if (ImageMetadata.State.available.equals(imgState) && taggedImages.contains(imageId)){
          try{
            this.removeTags(imageId);
          }catch(final Exception ex){
            ;
          }finally{
            taggedImages.remove(imageId);
          }
        }else if (ImageMetadata.State.failed.equals(imgState) && taggedImages.contains(imageId)){
          String message = "";
          try{
            conversionTaskCache.invalidate(taskId);
            Optional<DiskImageConversionTask> task = 
               conversionTaskCache.get(taskId);
            if(task.isPresent())
              message = task.get().getStatusMessage();
          }catch(final Exception ex){
            ;
          }finally{
            taggedImages.remove(imageId);
          }
          this.tagResources(imageId, "failed", message);
        }
      }catch(final Exception ex){
        LOG.error("Failed to update tags for resources in conversion", ex);
      }
    }
  }
  
  final static String TAG_KEY_STATE = "euca:image-conversion-state";
  final static String TAG_KEY_MESSAGE = "euca:image-conversion-status";
  final static Map<String, String> tagState = Maps.newHashMap();
  final static Map<String, String> tagMessage = Maps.newHashMap();
  
  private void tagResources(final String imageId, final String state, String statusMessage) throws Exception{
    final ImageInfo image = Images.lookupImage(imageId);
    final String imageOwnerId = image.getOwnerUserId();
    
    final List<VmInstance> instances = this.lookupInstances(imageId);
    if(tagState.containsKey(imageId) && state.equals(tagState.get(imageId))){
      ;
    }else{
      resetTag(imageOwnerId, imageId, TAG_KEY_STATE, state);
      tagState.put(imageId, state);
    }
    for(final VmInstance instance : instances){
      final String instanceId = instance.getInstanceId();
      final String instanceOwnerId = instance.getOwnerUserId();
      if(tagState.containsKey(instanceId) && state.equals(tagState.get(instanceId))){
        ;
      }else{
        resetTag(instanceOwnerId, instanceId, TAG_KEY_STATE, state);
        tagState.put(instanceId, state);
      }
    }
    
    if(statusMessage == null)
      statusMessage = "";
    
    if(tagMessage.containsKey(imageId) && statusMessage.equals(tagMessage.get(imageId))){
      ;
    }else{
      resetTag(imageOwnerId, imageId, TAG_KEY_MESSAGE, statusMessage);
      tagMessage.put(imageId, statusMessage);
    }
    for(final VmInstance instance : instances){
      final String instanceId = instance.getInstanceId();
      final String instanceOwnerId = instance.getOwnerUserId();
      if(tagMessage.containsKey(instanceId) && statusMessage.equals(tagMessage.get(instanceId))){
        ;
      }else{
        resetTag(instanceOwnerId, instanceId, TAG_KEY_MESSAGE, statusMessage);
        tagMessage.put(instanceId, statusMessage);
      }
    }
  }
  
  private void resetTag(final String userId, final String resourceId, final String tagKey, final String tagValue) throws Exception{
    /// try deleting tags
    try{
      final DeleteTagsTask task = new DeleteTagsTask(userId, Lists.newArrayList(resourceId), Lists.newArrayList(tagKey));
      final CheckedListenableFuture<Boolean> result = task.dispatch();
      if(result.get()){
        ;
      }
    }catch(final Exception ex){
      ;
    }
    // create tag
    final Map<String,String> tag = Maps.newHashMap();
    tag.put(tagKey, tagValue);
    final CreateTagsTask task = new CreateTagsTask(userId, Lists.newArrayList(resourceId), tag);
    final CheckedListenableFuture<Boolean> result = task.dispatch();
    if(result.get()){
      ;
    }else
      throw new Exception(String.format("Failed to create tag (%s-%s:%s)", resourceId, tagKey,tagValue));
  }
  
  private void removeTags(final String imageId) throws Exception{
    final ImageInfo image = Images.lookupImage(imageId);
    final String imageOwnerId = image.getOwnerUserId();
    
    DeleteTagsTask task = new DeleteTagsTask(imageOwnerId, Lists.newArrayList(image.getDisplayName()), 
        Lists.newArrayList(TAG_KEY_STATE, TAG_KEY_MESSAGE));
    CheckedListenableFuture<Boolean> result = task.dispatch();
    if(result.get()){
      ;
    }
    final List<VmInstance> instances = this.lookupInstances(imageId);
    for(final VmInstance instance : instances){
      final String instanceId = instance.getInstanceId();
      final String instanceOwnerId = instance.getOwnerUserId();
      try{
        task = new DeleteTagsTask(instanceOwnerId, Lists.newArrayList(instanceId), 
            Lists.newArrayList(TAG_KEY_STATE, TAG_KEY_MESSAGE));
        result = task.dispatch();
        if(result.get()){
          ;
        }
      }catch(final Exception ex){
        ;
      }
    }
  }
  
  private List<VmInstance> lookupInstances(final String imageId){
    try{
      final List<VmInstance> instances = VmInstances.list(new Predicate<VmInstance>(){
        @Override
        public boolean apply(VmInstance arg0) {
          return imageId.equals(arg0.getBootRecord().getMachineImageId());
        }
      });
      return instances;
    }catch(final Exception ex){
      return Lists.newArrayList();
    }
  }
    
  private class DeleteBucketTask extends ObjectStorageActivityTask {
    private String bucketName = null;
    private DeleteBucketTask(final String bucketName){
      this.bucketName = bucketName;
    }
    
    private DeleteBucketType deleteBucket(){
      final DeleteBucketType req = new DeleteBucketType();
      req.setBucket(this.bucketName);
      return req;
    }
    
    @Override
    void dispatchInternal(Checked<ObjectStorageRequestType> callback) {
      final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client = this.getClient();
      client.dispatch(deleteBucket(), callback);
    }

    @Override
    void dispatchSuccess(ObjectStorageRequestType response) {
      final DeleteBucketResponseType resp = (DeleteBucketResponseType) response;
    }
  }
  
  private class DeleteObjectTask extends ObjectStorageActivityTask {
    private String bucketName = null;
    private String objectKey = null;
    private DeleteObjectTask(final String bucketName, final String objectKey){
      this.bucketName = bucketName;
      this.objectKey = objectKey;
    }
    
    private DeleteObjectType deleteObject(){
      final DeleteObjectType req = new DeleteObjectType();
      req.setBucket(this.bucketName);
      req.setKey(this.objectKey);
      return req;
    }
    
    @Override
    void dispatchInternal(Checked<ObjectStorageRequestType> callback) {
      final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client = this.getClient();
      client.dispatch(deleteObject(), callback);
    }

    @Override
    void dispatchSuccess(ObjectStorageRequestType response) {
      final DeleteObjectResponseType resp = (DeleteObjectResponseType) response;
    }
  }
  
  private class ListObjectsInBucketTask extends ObjectStorageActivityTask {
    private String bucketName = null;
    private List<String> keyNames = null;
    private ListObjectsInBucketTask(final String bucketName){
      this.bucketName = bucketName;
    }
    
    private ListBucketType listBucket(){
      final ListBucketType req = new ListBucketType();
      req.setBucket(this.bucketName);
      return req;
    }

    @Override
    void dispatchInternal(Checked<ObjectStorageRequestType> callback) {
      final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client = this.getClient();
      client.dispatch(listBucket(), callback);      
    }

    @Override
    void dispatchSuccess(ObjectStorageRequestType response) {
      final ListBucketResponseType resp = (ListBucketResponseType) response;
      final List<ListEntry> keys = resp.getContents();
      if(keys!=null ){
        keyNames = Lists.newArrayList();
        for(final ListEntry key : keys){
          keyNames.add(key.getKey());
        }
      }
    }
    
    public List<String> getKeyNames(){
      return this.keyNames;
    }
  }
  
  private class CreateBucketTask extends ObjectStorageActivityTask {
    private String bucketName = null;
    private CreateBucketTask(final String bucketName){
      this.bucketName = bucketName;
    }
    
    private CreateBucketType createBucket(){
      final CreateBucketType req = new CreateBucketType(this.bucketName);
      req.setBucket(this.bucketName);
      return req; 
    }
    
    @Override
    void dispatchInternal(Checked<ObjectStorageRequestType> callback) {
      final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client = this.getClient();
      client.dispatch(createBucket(), callback);
    }

    @Override
    void dispatchSuccess(ObjectStorageRequestType response) {
      final CreateBucketResponseType resp = (CreateBucketResponseType) response;
      final String createdBucket = resp.getBucket();
    }
  }
  
  private class ListBucketsTask extends ObjectStorageActivityTask {
    List<String> buckets = null;
    private ListAllMyBucketsType listBuckets(){
      final ListAllMyBucketsType req = new ListAllMyBucketsType();
      return req;
    }
    
    @Override
    void dispatchInternal(Checked<ObjectStorageRequestType> callback) {   
      final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client = this.getClient();
      client.dispatch(listBuckets(), callback);             
    }

    @Override
    void dispatchSuccess(ObjectStorageRequestType response) {
      final ListAllMyBucketsResponseType resp = (ListAllMyBucketsResponseType) response;
      if(resp.getBucketList()!=null && resp.getBucketList().getBuckets()!=null){
        buckets = Lists.newArrayList();
        for(final BucketListEntry entry: resp.getBucketList().getBuckets()){
          buckets.add(entry.getName());
        }
      }
    }
    
    public List<String> getBuckets(){
      return this.buckets;
    }
  }
  
  private static class DescribeConversionTasks extends ImagingSystemActivityTask {
    private List<String> taskIds = null;
    private List<DiskImageConversionTask> tasks = null;
    private DescribeConversionTasks(final List<String> taskIds){
      this.taskIds = taskIds;
    }
    
    private DescribeConversionTasksType describeConversionTasks(){
      final DescribeConversionTasksType req = new DescribeConversionTasksType();
      req.setConversionTaskIdSet(new ArrayList(taskIds));
      return req;
    }
    
    @Override
    void dispatchInternal(Checked<ImagingMessage> callback) {
      final DispatchingClient<ImagingMessage, Imaging> client = this.getClient();
      client.dispatch(describeConversionTasks(), callback);             
    }

    @Override
    void dispatchSuccess(ImagingMessage response) {
      final DescribeConversionTasksResponseType resp = (DescribeConversionTasksResponseType) response;
      if(resp.getConversionTasks()!=null)
        tasks = resp.getConversionTasks();
      else
        tasks = Lists.newArrayList();
    }
    
    public List<DiskImageConversionTask> getTasks(){
      return this.tasks;
    }
  }
 
  private class ImportImageBuilder {
    final ImportDiskImage importDisk = new ImportDiskImage();;
    public ImportImageBuilder(){
      importDisk.setConvertedImage(new ConvertedImageDetail());
      importDisk.setDiskImageSet(Lists.<ImportDiskImageDetail>newArrayList());
    }
    
    public ImportImageBuilder withBucket(final String bucket){
      importDisk.getConvertedImage().setBucket(bucket);
      return this;
    }
    
    public ImportImageBuilder withArchitecture(final String arch){
      importDisk.getConvertedImage().setArchitecture(arch);
      return this;
    }
    
    public ImportImageBuilder withPrefix(final String prefix){
      importDisk.getConvertedImage().setPrefix(prefix);
      return this;
    }
    
    public ImportImageBuilder withMachineImage(final String id, final String manifestUrl, final long bytes){
      final ImportDiskImageDetail image = new ImportDiskImageDetail();
      image.setId(id);
      image.setDownloadManifestUrl(manifestUrl);
      image.setBytes(bytes);
      image.setFormat("PARTITION");
      importDisk.getDiskImageSet().add(image);
      return this;
    }
    
    public ImportImageBuilder withKernel(final String id, final String manifestUrl, final long bytes){
      final ImportDiskImageDetail image = new ImportDiskImageDetail();
      image.setId(id);
      image.setDownloadManifestUrl(manifestUrl);
      image.setBytes(bytes);
      image.setFormat("KERNEL");
      importDisk.getDiskImageSet().add(image);
      return this;
    }
    
    public ImportImageBuilder withRamdisk(final String id, final String manifestUrl, final long bytes){
      final ImportDiskImageDetail image = new ImportDiskImageDetail();
      image.setId(id);
      image.setDownloadManifestUrl(manifestUrl);
      image.setBytes(bytes);
      image.setFormat("RAMDISK");
      importDisk.getDiskImageSet().add(image);
      return this; 
    }
    
    public ImportImageBuilder withBucketUploadPolicy(final String bucket, final String prefix) {
      try{
        final AccessKey adminAccessKey = Accounts.lookupSystemAdmin().getKeys().get(0);
        this.importDisk.setAccessKey(adminAccessKey.getAccessKey());
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 48); // IMPORT_TASK_EXPIRATION_HOURS=48
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String expiration = sdf.format(c.getTime());
        // based on http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-BundleInstance.html
        final String policy = String.format("{\"expiration\":\"%s\",\"conditions\":[{\"bucket\": \"%s\"},"
            + "[\"starts-with\", \"$key\", \"%s\"],{\"acl\":\"aws-exec-read\"}]}",
            expiration, bucket, prefix);
        this.importDisk.setUploadPolicy(B64.standard.encString(policy));

        final Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(adminAccessKey.getSecretKey().getBytes("UTF-8"), "HmacSHA1"));

        this.importDisk.setUploadPolicySignature(
	     B64.standard.encString(hmac.doFinal(B64.standard.encString(policy).getBytes("UTF-8"))));
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
      return this;
    }
  }

  private class ImportImageTask extends ImagingSystemActivityTask {
    private String taskId = null;
    private ImportDiskImage importImage = null;
  
    private ImportImageTask(final ImportImageBuilder builder){
      importImage = builder.importDisk;
    }
    
    private ImportImageType importImage(){
      final ImportImageType req = new ImportImageType();
      req.setImage(importImage);
      req.setDescription("Conversion during image registration");
      return req;
    }

    @Override
    void dispatchInternal(
        Checked<ImagingMessage> callback) {
      final DispatchingClient<ImagingMessage, Imaging> client = this.getClient();
      client.dispatch(importImage(), callback);       
    }

    @Override
    void dispatchSuccess(
        ImagingMessage response) {
      final ImportImageResponseType resp = (ImportImageResponseType) response;
      this.taskId = resp.getConversionTask().getConversionTaskId();
    }

    public String getTaskId(){
      return this.taskId;
    }
  }
  
  
  private static class CreateTagsTask extends EucalyptusUserActivityTask {
    private List<String> resourceIds = null;
    private Map<String,String> tags = null;
    private CreateTagsTask(final String userId, final List<String> resourceIds, final Map<String,String> tags){
      super(userId);
      this.resourceIds = resourceIds;
      this.tags = tags;
    }
    
    private CreateTagsType createTag(){
      final CreateTagsType req = new CreateTagsType();
      req.setResourcesSet(new ArrayList<String>(this.resourceIds));
      req.setTagSet(new ArrayList<ResourceTag>());
      for(final String key : this.tags.keySet()){
        final ResourceTag tag = new ResourceTag();
        tag.setKey(key);
        tag.setValue(this.tags.get(key));
        req.getTagSet().add(tag);
      }
      req.markPrivileged();
      return req;
    }
    
    @Override
    void dispatchInternal(Checked<EucalyptusMessage> callback) {
      final DispatchingClient<EucalyptusMessage, Eucalyptus> client = this.getClient();
      client.dispatch(createTag(), callback);             
    }

    @Override
    void dispatchSuccess(EucalyptusMessage response) {
      final CreateTagsResponseType resp = (CreateTagsResponseType) response;
    }
  }
  
  private static class DeleteTagsTask extends EucalyptusUserActivityTask {
    private List<String> resourceIds = null;
    private List<String> tagKeys = null;
    private DeleteTagsTask(final String userId, final List<String> resourceIds, final List<String> tagKeys){
      super(userId);
      this.resourceIds = resourceIds;
      this.tagKeys = tagKeys;
    }
    
    private DeleteTagsType deleteTag(){
      final DeleteTagsType req = new DeleteTagsType();
      req.setResourcesSet(new ArrayList<String>(resourceIds));
      req.setTagSet(new ArrayList<DeleteResourceTag>());
      for(final String tagKey : tagKeys){
        final DeleteResourceTag tag = new DeleteResourceTag();
        tag.setKey(tagKey);
        req.getTagSet().add(tag);
      }
      req.markPrivileged();
      return req;
    }
    @Override
    void dispatchInternal(Checked<EucalyptusMessage> callback) {
      final DispatchingClient<EucalyptusMessage, Eucalyptus> client = this.getClient();
      client.dispatch(deleteTag(), callback);      
    }

    @Override
    void dispatchSuccess(EucalyptusMessage response) {
      final DeleteTagsResponseType resp = (DeleteTagsResponseType) response;
    }
  }
  
  private static abstract class ObjectStorageActivityTask extends ActivityTask<ObjectStorageRequestType, ObjectStorage> {
    @Override
    protected DispatchingClient<ObjectStorageRequestType, ObjectStorage> getClient() {
      try{
        final DispatchingClient<ObjectStorageRequestType, ObjectStorage> client =
            new DispatchingClient<>( Accounts.lookupSystemAdmin().getUserId() , ObjectStorage.class );
            client.init();
            return client;
      }catch(Exception e){
        throw Exceptions.toUndeclared(e);
      }
    }
  }
  
  private static abstract class ImagingSystemActivityTask extends ActivityTask<ImagingMessage, Imaging> {
    @Override
    protected DispatchingClient<ImagingMessage, Imaging> getClient() {
      try{
        final DispatchingClient<ImagingMessage, Imaging> client =
            new DispatchingClient<>( Accounts.lookupSystemAdmin().getUserId() , Imaging.class );
            client.init();
            return client;
      }catch(Exception e){
        throw Exceptions.toUndeclared(e);
      }
    }
  }
  
  private static abstract class EucalyptusUserActivityTask extends ActivityTask<EucalyptusMessage, Eucalyptus> {
    private String userId = null;
    private EucalyptusUserActivityTask(final String userId){
      this.userId = userId;
    }
    @Override
    protected DispatchingClient<EucalyptusMessage, Eucalyptus> getClient() {
      try{
        final DispatchingClient<EucalyptusMessage, Eucalyptus> client =
            new DispatchingClient<>( userId, Eucalyptus.class );
            client.init();
            return client;
      }catch(Exception e){
        throw Exceptions.toUndeclared(e);
      }
    }
  }
  
  private static abstract class EucalyptusActivityTask extends ActivityTask<EucalyptusMessage, Eucalyptus> {
    @Override
    protected DispatchingClient<EucalyptusMessage, Eucalyptus> getClient() {
      try{
        final DispatchingClient<EucalyptusMessage, Eucalyptus> client =
            new DispatchingClient<>( Accounts.lookupSystemAdmin().getUserId() , Eucalyptus.class );
            client.init();
            return client;
      }catch(Exception e){
        throw Exceptions.toUndeclared(e);
      }
    }
  }

  private static abstract class ActivityTask <TM extends BaseMessage, TC extends ComponentId>{
    private volatile boolean dispatched = false;

    protected ActivityTask(){}

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
      LOG.error( "Image conversion task error", throwable );
    }

    abstract void dispatchSuccess(TM response );

    protected abstract DispatchingClient<TM, TC> getClient() ;
  }
}
