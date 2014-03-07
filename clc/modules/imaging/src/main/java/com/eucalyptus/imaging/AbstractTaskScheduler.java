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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
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
      if(nextTask instanceof VolumeImagingTask){
        final VolumeImagingTask volumeTask = (VolumeImagingTask) nextTask;
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
        }
        newTask = new WorkerTask(volumeTask.getDisplayName(), WorkerTaskType.import_volume);
        final VolumeTask vt = new VolumeTask();
        final ImageManifest im = new ImageManifest();
        im.setManifestUrl(manifestLocation);
        im.setFormat(volumeTask.getFormat());
        vt.setImageManifestSet(Lists.newArrayList(im));
        newTask.setVoumeTask(vt);
      }else if (nextTask instanceof InstanceStoreImagingTask){
        final InstanceStoreImagingTask isTask = (InstanceStoreImagingTask) nextTask;
        newTask = new WorkerTask(isTask.getDisplayName(), WorkerTaskType.convert_image);
        
        final List<ImageManifest> manifests = Lists.newArrayList();
        for(final ImportInstanceVolumeDetail volume : isTask.getVolumes()){
          final String manifestUrl = volume.getImage().getImportManifestUrl();
          final String format = volume.getImage().getFormat();
          final ImageManifest im = new ImageManifest();
          im.setManifestUrl(manifestUrl);
          im.setFormat(format);
          manifests.add(im);
        }
        final InstanceStoreTask ist = new InstanceStoreTask();
        ist.setImageManifestSet((ArrayList<ImageManifest>) manifests);
        ist.setBucket(isTask.getDestinationBucket());
        ist.setPrefix(isTask.getDestinationPrefix());
        newTask.setInstanceStoreTask(ist);
      }else if (nextTask instanceof InstanceImagingTask){
        final InstanceImagingTask instanceTask = (InstanceImagingTask) nextTask;
        for(final ImportInstanceVolumeDetail volume : instanceTask.getVolumes()){
          final String importManifestUrl = volume.getImage().getImportManifestUrl();
          if(! instanceTask.hasDownloadManifestUrl(importManifestUrl)){
            // meaning that this task has not been fully processed by worker
            String manifestLocation = null;
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
            newTask = new WorkerTask(instanceTask.getDisplayName(), WorkerTaskType.import_volume);
            final VolumeTask vt = new VolumeTask();
            final ImageManifest im = new ImageManifest();
            im.setManifestUrl(manifestLocation);
            im.setFormat(volume.getImage().getFormat());
            vt.setImageManifestSet(Lists.newArrayList(im));
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
