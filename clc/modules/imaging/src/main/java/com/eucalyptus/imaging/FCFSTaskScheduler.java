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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
import com.google.common.collect.Lists;
/**
 * @author Sang-Min Park
 *
 */
public class FCFSTaskScheduler extends AbstractTaskScheduler {
  private static Logger LOG = Logger.getLogger( FCFSTaskScheduler.class );

  @Override
  public ImagingTask getNext() {
    List<ImagingTask> allTasks = null;
    List<ImagingTask> pendingTasks = Lists.newArrayList();
    // pick a pending task whose timestamp is the oldest
    try{
      try ( final TransactionResource db =
          Entities.transactionFor( VolumeImagingTask.class ) ) {
        allTasks = Entities.query(ImagingTask.named());
      }
      
      for(final ImagingTask t : allTasks){
        if(ImportTaskState.PENDING.equals(t.getState()))
          pendingTasks.add(t);
        else if (ImportTaskState.CONVERTING.equals(t.getState()) && t instanceof InstanceImagingTask)
          pendingTasks.add(t); // more than one volumes should be processed by worker
      }
      
      ImagingTask oldestTask = null;
      Date oldest = new Date(Long.MAX_VALUE) ;
      for(final ImagingTask task : pendingTasks){
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
