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
package com.eucalyptus.imaging.backend;

import java.net.URI;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.resources.client.EuareClient;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.imaging.common.DiskImageConversionTask;
import com.eucalyptus.imaging.common.ImageManifest;
import com.eucalyptus.imaging.common.ImportDiskImageDetail;
import com.eucalyptus.imaging.common.InstanceStoreTask;
import com.eucalyptus.imaging.common.VolumeTask;
import com.eucalyptus.imaging.manifest.BundleImageManifest;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
import com.eucalyptus.imaging.worker.ImagingServiceLaunchers;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public abstract class AbstractTaskScheduler {
  private static Logger LOG = Logger.getLogger( AbstractTaskScheduler.class );
  private PublicKey imagingServiceKey = null;
  private String imagingServiceCertArn = null;
  
  public enum WorkerTaskType { import_volume, convert_image }
  
  public static class WorkerTask {
    String importTaskId = null;
    WorkerTaskType importTaskType = null;
    VolumeTask volumeTask = null;
    InstanceStoreTask instanceStoreTask = null;

    public WorkerTask(){ }
    public WorkerTask(final String importTaskId, final WorkerTaskType taskType){
      this.importTaskId = importTaskId;
      this.importTaskType = taskType;
    }

    public String getImportTaskId(){
      return this.importTaskId;
    }
    
    public WorkerTaskType getImportTaskType(){
      return this.importTaskType;
    }
    
    public void setVolumeTask( final VolumeTask volumeTask ){
      this.volumeTask = volumeTask;
    }
    
    public VolumeTask getVolumeTask(){
      return this.volumeTask;
    }
    
    public void setInstanceStoreTask(final InstanceStoreTask task){
      this.instanceStoreTask = task;
    }
    
    public InstanceStoreTask getInstanceStoreTask(){
      return this.instanceStoreTask;
    }
  }

  protected abstract ImagingTask getNext(String availabilityZone);
  
  private void loadImagingServiceKey() throws Exception{
    try{
      final ServerCertificateType cert = 
          EuareClient.getInstance().getServerCertificate(
              Accounts.lookupSystemAccountByAlias( AccountIdentifiers.IMAGING_SYSTEM_ACCOUNT ).getUserId( ),
              ImagingServiceLaunchers.SERVER_CERTIFICATE_NAME);
      final String certBody = cert.getCertificateBody();
      final X509Certificate x509 = PEMFiles.toCertificate(B64.url.encString(certBody));
      this.imagingServiceKey = x509.getPublicKey();
      this.imagingServiceCertArn = cert.getServerCertificateMetadata().getArn();
    }catch(final Exception ex){
      throw new Exception("Failed to load public key of the imaging service", ex);
    }
  }

  private static Object taskLock = new Object();
  public WorkerTask getTask(final String availabilityZone) throws Exception{
    ImagingTask nextTask = null;
    synchronized(taskLock){
      nextTask = this.getNext(availabilityZone);
      if(nextTask!=null) {
        ImagingTasks.transitState(nextTask, ImportTaskState.PENDING, ImportTaskState.CONVERTING, "");
      }
    }
    if(nextTask==null)
      return null;
 
    this.imagingServiceKey = null;
    this.imagingServiceCertArn = null;
    loadImagingServiceKey();
    if(this.imagingServiceKey==null || this.imagingServiceCertArn==null)
      throw new Exception("Failed to load public key of the imaging service");

    WorkerTask newTask = null;

    try{
      if (nextTask instanceof DiskImagingTask){
        final DiskImagingTask imagingTask = (DiskImagingTask) nextTask;
        final DiskImageConversionTask conversionTask = imagingTask.getTask();
        // generate temporary download manifests
        try{
          final List<ImportDiskImageDetail> importImages = conversionTask.getImportDisk().getDiskImageSet();
          for(final ImportDiskImageDetail image : importImages){
            String manifestUrl = image.getDownloadManifestUrl();
            final String key = manifestUrl.substring(manifestUrl.lastIndexOf("/")+1);
            manifestUrl = manifestUrl.substring(0, manifestUrl.lastIndexOf("/"));
            final String bucket = manifestUrl.substring(manifestUrl.lastIndexOf("/")+1);
            final ImageManifestFile manifestFile = new ImageManifestFile(
                String.format("%s/%s", bucket, key),
                BundleImageManifest.INSTANCE,
                ImageConfiguration.getInstance( ).getMaxManifestSizeBytes( ) );
            String manifestName = String.format("%s-%s-%s", imagingTask.getDisplayName(),
                conversionTask.getImportDisk().getConvertedImage().getPrefix(),
                image.getFormat());
            final String downloadManifest = 
                DownloadManifestFactory.generateDownloadManifest(manifestFile, this.imagingServiceKey, 
                    manifestName, 5, false);
            image.setDownloadManifestUrl(downloadManifest);
          }
        }catch(final Exception ex){
          ImagingTasks.setState(imagingTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_DOWNLOAD_MANIFEST);
          throw new EucalyptusCloudException("Failed to generate download manifest", ex);
        }

        newTask = new WorkerTask(imagingTask.getDisplayName(), WorkerTaskType.convert_image);

        final InstanceStoreTask ist = new InstanceStoreTask();
        ist.setAccountId(imagingTask.getOwnerAccountNumber());
        ist.setAccessKey(conversionTask.getImportDisk().getAccessKey());
        ist.setConvertedImage(conversionTask.getImportDisk().getConvertedImage());
        ist.setImportImageSet(conversionTask.getImportDisk().getDiskImageSet());
        ist.setUploadPolicy(conversionTask.getImportDisk().getUploadPolicy());
        ist.setUploadPolicySignature(conversionTask.getImportDisk().getUploadPolicySignature());
        ist.setServiceCertArn(this.imagingServiceCertArn);
        final ServiceConfiguration osg = Topology.lookup( ObjectStorage.class );
        final URI osgUri = osg.getUri();
        if ( DNSProperties.LOCALHOST_DOMAIN.equals(DNSProperties.getDomain()) )
          ist.setS3Url(String.format("%s://%s:%d%s", osgUri.getScheme(), osgUri.getHost(), osgUri.getPort(), osgUri.getPath())); 
        else
          ist.setS3Url(String.format("%s://objectstorage.%s:%d", osgUri.getScheme(), DNSProperties.getDomain(), osgUri.getPort()));
        newTask.setInstanceStoreTask(ist);
      }else if(nextTask instanceof ImportVolumeImagingTask){
        final ImportVolumeImagingTask volumeTask = (ImportVolumeImagingTask) nextTask;
        String manifestLocation = null;
        if(volumeTask.getDownloadManifestUrl().size() == 0){
          try{
            manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                new ImageManifestFile(
                    volumeTask.getImportManifestUrl(),
                    ImportImageManifest.INSTANCE,
                    ImageConfiguration.getInstance( ).getMaxManifestSizeBytes( ) ),
                    null, volumeTask.getDisplayName( ), 1, false);
          }catch(final InvalidBaseManifestException ex){
            ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_DOWNLOAD_MANIFEST);
            throw new EucalyptusCloudException("Failed to generate download manifest", ex);
          }
          ImagingTasks.addDownloadManifestUrl(volumeTask, volumeTask.getImportManifestUrl(), manifestLocation);
        }else
          manifestLocation = volumeTask.getDownloadManifestUrl().get(0).getDownloadManifestUrl();
        newTask = new WorkerTask(volumeTask.getDisplayName(), WorkerTaskType.import_volume);
        final VolumeTask vt = new VolumeTask();
        final ImageManifest im = new ImageManifest();
        im.setManifestUrl(manifestLocation);
        im.setFormat(volumeTask.getFormat());
        vt.setImageManifestSet(Lists.newArrayList(im));
        vt.setVolumeId(volumeTask.getVolumeId());
        newTask.setVolumeTask( vt );
      }else if (nextTask instanceof ImportInstanceImagingTask){
        final ImportInstanceImagingTask instanceTask = (ImportInstanceImagingTask) nextTask;
        for(final ImportInstanceVolumeDetail volume : instanceTask.getVolumes()){
          final String importManifestUrl = volume.getImage().getImportManifestUrl();
          // that this task has not been fully processed by worker and the zone matches
          if(! instanceTask.hasDownloadManifestUrl(importManifestUrl) && 
              availabilityZone.equals(volume.getAvailabilityZone())){
            String manifestLocation = null;
            manifestLocation = instanceTask.getDownloadManifestUrl(importManifestUrl);
            if(manifestLocation == null){
              try{
                String manifestName = String.format("%s-%s", nextTask.getDisplayName(), volume.getVolume().getId());
                manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                    new ImageManifestFile(
                        importManifestUrl,
                        ImportImageManifest.INSTANCE,
                        ImageConfiguration.getInstance( ).getMaxManifestSizeBytes( ) ),
                        null,
                        manifestName,
                        1, false);
                ImagingTasks.addDownloadManifestUrl(instanceTask, importManifestUrl, manifestLocation);
              }catch(final InvalidBaseManifestException ex){
                ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_DOWNLOAD_MANIFEST);
                throw new EucalyptusCloudException("Failed to generate download manifest", ex);
              }       
            }
            newTask = new WorkerTask(instanceTask.getDisplayName(), WorkerTaskType.import_volume);
            final VolumeTask vt = new VolumeTask();
            final ImageManifest im = new ImageManifest();
            im.setManifestUrl(manifestLocation);
            im.setFormat(volume.getImage().getFormat());
            vt.setImageManifestSet(Lists.newArrayList(im));
            vt.setVolumeId(volume.getVolume().getId());
            newTask.setVolumeTask( vt );
            break;
          }
        }
      }
    }catch(final EucalyptusCloudException ex){
      throw new Exception("failed to prepare worker task", ex);
    }catch(final Exception ex){
      ImagingTasks.setState(nextTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_FAILED_UNEXPECTED);
      throw new Exception("failed to prepare worker task", ex);
    }

    if(newTask!=null){
      try{
        ImagingTasks.transitState(nextTask, ImportTaskState.PENDING, ImportTaskState.CONVERTING, "");
      }catch(final Exception ex){
        ;
      }
    }else{
      ImagingTasks.setState(nextTask, ImportTaskState.FAILED, ImportTaskState.STATE_MSG_FAILED_UNEXPECTED);
    }

    return newTask;
  }

  public static AbstractTaskScheduler getScheduler(){
    return new TaskSchedulers.ImportImageFirstTaskScheduler();
  }
}
