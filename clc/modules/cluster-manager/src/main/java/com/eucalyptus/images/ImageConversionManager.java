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
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentId;
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
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

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
         Topology.isEnabled( Eucalyptus.class) && 
             Topology.isEnabled(Imaging.class) ) )
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
        
        if(runManifestPath==null || runManifestPath.length()<=0)
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
         continue;
       
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
           Images.setImageVirtualizationType(machineImage.getDisplayName(), ImageMetadata.VirtualizationType.hvm);
           Images.setImageState(machineImage.getDisplayName(), ImageMetadata.State.available);
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
  
  class DescribeConversionTasks extends ImagingSystemActivityTask {
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
            + "[\"starts-with\", \"$key\", \"%s\"],{\"acl\":\"ec2-bundle-read\"}]}",
            expiration, bucket, prefix);
        this.importDisk.setUploadPolicy(B64.standard.encString(policy));

        final Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(adminAccessKey.getSecretKey().getBytes("UTF-8"), "HmacSHA1"));

        this.importDisk.setUploadPolicySignature(
            B64.standard.encString(hmac.doFinal(policy.getBytes("UTF-8"))));
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
  
  private abstract class ObjectStorageActivityTask extends ActivityTask<ObjectStorageRequestType, ObjectStorage> {
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
  
  private abstract class ImagingSystemActivityTask extends ActivityTask<ImagingMessage, Imaging> {
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
  
  private abstract class EucalyptusActivityTask extends ActivityTask<EucalyptusMessage, Eucalyptus> {
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

  private abstract class ActivityTask <TM extends BaseMessage, TC extends ComponentId>{
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
