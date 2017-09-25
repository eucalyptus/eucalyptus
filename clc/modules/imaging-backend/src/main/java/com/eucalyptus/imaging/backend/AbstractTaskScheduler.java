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

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.resources.client.EuareClient;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.common.msgs.ServerCertificateType;
import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.imaging.common.ImageManifest;
import com.eucalyptus.imaging.common.InstanceStoreTask;
import com.eucalyptus.imaging.common.VolumeTask;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
import com.eucalyptus.imaging.worker.ImagingServiceLaunchers;
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
      if(nextTask instanceof ImportVolumeImagingTask){
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
