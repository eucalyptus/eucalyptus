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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.common.ImportInstanceVolumeDetail;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class TaskSchedulers {
  private static Logger LOG = Logger.getLogger(TaskSchedulers.class);

  // first-come, first-served order, but give higher priority for import-image (partition to disk conversion) task
  public static class ImportImageFirstTaskScheduler extends AbstractTaskScheduler {
    @Override
    protected ImagingTask getNext(final String availabilityZone) {
      List<ImagingTask> allTasks = null;
      List<ImagingTask> imagePendingTasks = Lists.newArrayList();
      List<ImagingTask> pendingTasks = Lists.newArrayList();
      // pick a pending task whose timestamp is the oldest
      try{
        allTasks = ImagingTasks.getImagingTasks();
        for(final ImagingTask t : allTasks){
          if(ImportTaskState.PENDING.equals(t.getState()))
            pendingTasks.add(t);
          else if (ImportTaskState.CONVERTING.equals(t.getState()) && 
              (t instanceof ImportInstanceImagingTask))
            pendingTasks.add(t); // more than one volumes should be processed by worker
        }

        ImagingTask oldestTask = null;
        Date oldest = new Date(Long.MAX_VALUE) ;
        for(final ImagingTask task : imagePendingTasks){
          if (oldest.after(task.getCreationTimestamp())){
            oldest = task.getCreationTimestamp();
            oldestTask = task;
          }
        }
        if(oldestTask!=null)
          return oldestTask;
        
        for(final ImagingTask task : pendingTasks){
          if(task instanceof ImportVolumeImagingTask){
            if(! availabilityZone.equals(((ImportVolumeImagingTask)task).getAvailabilityZone()))
              continue;
          }else if(task instanceof ImportInstanceImagingTask){
            boolean clusterFound = false;
            for(final ImportInstanceVolumeDetail volume : ((ImportInstanceImagingTask)task).getVolumes()){
              final String importManifestUrl = volume.getImage().getImportManifestUrl();
              /// this volume is not yet converted and the zone matches
              if(! ((ImportInstanceImagingTask)task).hasDownloadManifestUrl(importManifestUrl) &&
                  availabilityZone.equals(volume.getAvailabilityZone())){
                clusterFound = true;
                break;
              }
            }   
            if(!clusterFound)
              continue;
          }
          if (oldest.after(task.getCreationTimestamp())){
            oldest = task.getCreationTimestamp();
            oldestTask = task;
          }
        }
        return oldestTask;
      }catch(final Exception ex){
        LOG.error("failed to schedule the task to imaging worker", ex);
        return null;
      }
    }
  }
  
  public static class FCFSTaskScheduler extends AbstractTaskScheduler {
    @Override
    public ImagingTask getNext(final String availabilityZone) {
      List<ImagingTask> allTasks = null;
      List<ImagingTask> pendingTasks = Lists.newArrayList();
      // pick a pending task whose timestamp is the oldest
      try{
        allTasks = ImagingTasks.getImagingTasks();
        for(final ImagingTask t : allTasks){
          if(ImportTaskState.PENDING.equals(t.getState()))
            pendingTasks.add(t);
          else if (ImportTaskState.CONVERTING.equals(t.getState()) && t instanceof ImportInstanceImagingTask)
            pendingTasks.add(t); // more than one volumes should be processed by worker
        }

        ImagingTask oldestTask = null;
        Date oldest = new Date(Long.MAX_VALUE) ;
        for(final ImagingTask task : pendingTasks){
          if(task instanceof ImportVolumeImagingTask){
            if(! availabilityZone.equals(((ImportVolumeImagingTask)task).getAvailabilityZone()))
              continue;
          }else if(task instanceof ImportInstanceImagingTask){
            boolean clusterFound = false;
            for(final ImportInstanceVolumeDetail volume : ((ImportInstanceImagingTask)task).getVolumes()){
              final String importManifestUrl = volume.getImage().getImportManifestUrl();
              /// this volume is not yet converted and the zone matches
              if(! ((ImportInstanceImagingTask)task).hasDownloadManifestUrl(importManifestUrl) &&
                  availabilityZone.equals(volume.getAvailabilityZone())){
                clusterFound = true;
                break;
              }
            }   
            if(!clusterFound)
              continue;
          }
          
          if (oldest.after(task.getCreationTimestamp())){
            oldest = task.getCreationTimestamp();
            oldestTask = task;
          }
        }
        return oldestTask;
      }catch(final Exception ex){
        LOG.error("failed to schedule the task to imaging worker", ex);
        return null;
      }
    }
  }
}
