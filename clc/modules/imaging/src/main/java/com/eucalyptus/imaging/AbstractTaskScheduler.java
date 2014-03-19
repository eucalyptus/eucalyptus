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
package com.eucalyptus.imaging;

import java.net.URI;
import org.apache.log4j.Logger;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;

/**
 * @author Sang-Min Park
 *
 */
public abstract class AbstractTaskScheduler {
  private static Logger LOG = Logger.getLogger( AbstractTaskScheduler.class );

  public enum WorkerTaskType { import_volume, convert_image };
  
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
    
    public void setVoumeTask(final VolumeTask volumeTask){
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

  protected abstract ImagingTask getNext();

  public WorkerTask getTask() throws Exception{
    final ImagingTask nextTask = this.getNext();
    if(nextTask==null)
      return null;
    
    WorkerTask newTask = null;
    try{
      if (nextTask instanceof DiskImagingTask){
        final DiskImagingTask imagingTask = (DiskImagingTask) nextTask;
        final DiskImageConversionTask conversionTask = (DiskImageConversionTask) imagingTask.getTask();
        newTask = new WorkerTask(imagingTask.getDisplayName(), WorkerTaskType.convert_image);
        
        final InstanceStoreTask ist = new InstanceStoreTask();
        ist.setAccountId(imagingTask.getOwnerAccountNumber());
        //ist.setAccessKey(imagingTask.getOwn); /// how to obtain an access key?
        ist.setConvertedImage(conversionTask.getImportDisk().getConvertedImage());
        ist.setImportImageSet(conversionTask.getImportDisk().getDiskImageSet());
        ist.setUploadPolicy(conversionTask.getImportDisk().getUploadPolicy());
        final ServiceConfiguration osg = Topology.lookup( ObjectStorage.class );
        final URI osgUri = osg.getUri();
        ist.setS3Url(String.format("%s://%s:%d%s", osgUri.getScheme(), osgUri.getHost(), osgUri.getPort(), osgUri.getPath()));
        newTask.setInstanceStoreTask(ist);
      }else if(nextTask instanceof ImportVolumeImagingTask){
        final ImportVolumeImagingTask volumeTask = (ImportVolumeImagingTask) nextTask;
        String manifestLocation = null;
        if(volumeTask.getDownloadManifestUrl().size() == 0){
          try{
            manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                new ImageManifestFile(volumeTask.getImportManifestUrl(),
                    ImportImageManifest.INSTANCE ),
                    null, volumeTask.getDisplayName(), 1);
          }catch(final InvalidBaseManifestException ex){
            ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "Failed to generate download manifest");
            throw new Exception("Failed to generate download manifest", ex);
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
        newTask.setVoumeTask(vt);
      }else if (nextTask instanceof ImportInstanceImagingTask){
        final ImportInstanceImagingTask instanceTask = (ImportInstanceImagingTask) nextTask;
        for(final ImportInstanceVolumeDetail volume : instanceTask.getVolumes()){
          final String importManifestUrl = volume.getImage().getImportManifestUrl();
          if(! instanceTask.hasDownloadManifestUrl(importManifestUrl)){
            // meaning that this task has not been fully processed by worker
            String manifestLocation = null;
            manifestLocation = instanceTask.getDownloadManifestUrl(importManifestUrl);
            if(manifestLocation == null){
              try{
                manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                    new ImageManifestFile(importManifestUrl,
                        ImportImageManifest.INSTANCE ),
                        null, nextTask.getDisplayName(), 1);
                ImagingTasks.addDownloadManifestUrl(instanceTask, importManifestUrl, manifestLocation);
              }catch(final InvalidBaseManifestException ex){
                ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, "Failed to generate download manifest");
                throw new Exception("Failed to generate download manifest", ex);
              }       
            }
            newTask = new WorkerTask(instanceTask.getDisplayName(), WorkerTaskType.import_volume);
            final VolumeTask vt = new VolumeTask();
            final ImageManifest im = new ImageManifest();
            im.setManifestUrl(manifestLocation);
            im.setFormat(volume.getImage().getFormat());
            vt.setImageManifestSet(Lists.newArrayList(im));
            vt.setVolumeId(volume.getVolume().getId());
            newTask.setVoumeTask(vt);
            break;
          }
        }
      }
    }catch(final Exception ex){
      ImagingTasks.setState(nextTask, ImportTaskState.FAILED, "Internal error");
      throw new Exception("failed to prepare worker task", ex);
    }
    try{
      ImagingTasks.transitState(nextTask, ImportTaskState.PENDING, ImportTaskState.CONVERTING, null);
    }catch(final Exception ex){
      ;
    }
    
    return newTask;
  }

  public static AbstractTaskScheduler getScheduler(){
    return new TaskSchedulers.ImportImageFirstTaskScheduler();
  }
}
