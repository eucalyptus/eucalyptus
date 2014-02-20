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

import org.apache.log4j.Logger;

import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;

import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;

/**
 * @author Sang-Min Park
 *
 */
public abstract class AbstractTaskScheduler {
  private static Logger LOG = Logger.getLogger( AbstractTaskScheduler.class );

  public static class WorkerTask {
    private String taskId = null;
    private String downloadManifestUrl = null;
    private String volumeId = null;

    public WorkerTask(){ }
    public WorkerTask(final String taskId, final String downloadManifestUrl, final String volumeId){
      this.taskId = taskId;
      this.downloadManifestUrl = downloadManifestUrl;
      this.volumeId = volumeId;
    }

    public void setTaskId(final String taskId){
      this.taskId = taskId;
    }

    public String getTaskId(){
      return this.taskId;
    }

    public void setDownloadManifestUrl(final String url){
      this.downloadManifestUrl = url;
    }

    public String getDownloadManifestUrl(){
      return this.downloadManifestUrl;
    }

    public void setVolumeId(final String volumeId){
      this.volumeId = volumeId;
    }

    public String getVolumeId(){
      return this.volumeId;
    }
  }

  protected abstract ImagingTask getNext();

  public WorkerTask getTask() throws Exception{
    final ImagingTask nextTask = this.getNext();
    final WorkerTask newTask = new WorkerTask();
    if (nextTask == null)
      return null;
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
            volumeTask.addDownloadManifestUrl(volumeTask.getImportManifestUrl(), manifestLocation);
            ImagingTasks.save(volumeTask);
          }catch(final InvalidBaseManifestException ex){
            ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "Failed to generate download manifest");
            throw new Exception("Failed to generate download manifest", ex);
          }
        } 
        newTask.setVolumeId(volumeTask.getVolumeId());
        newTask.setDownloadManifestUrl(manifestLocation);
        newTask.setTaskId(volumeTask.getDisplayName());
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
              instanceTask.addDownloadManifestUrl(importManifestUrl, manifestLocation);
              ImagingTasks.save(instanceTask);
            }catch(final InvalidBaseManifestException ex){
              ImagingTasks.setState(instanceTask, ImportTaskState.FAILED, "Failed to generate download manifest");
              throw new Exception("Failed to generate download manifest", ex);
            }
            newTask.setDownloadManifestUrl(manifestLocation);
            newTask.setTaskId(instanceTask.getDisplayName());
            newTask.setVolumeId(volume.getVolume().getId());
          }
        }
      }
    }catch(final Exception ex){
      throw new Exception("failed to prepare worker task", ex);
    }
    ImagingTasks.setState(nextTask, ImportTaskState.CONVERTING, null);
    
    return newTask;
  }

  public static AbstractTaskScheduler getScheduler(){
    return new FCFSTaskScheduler();
  }
}
