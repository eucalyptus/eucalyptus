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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.imaging.manifest.DownloadManifestException;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportVolumeTaskDetails;
import edu.ucsb.eucalyptus.msgs.Volume;

/**
 * @author Sang-Min Park
 *
 */
public class ImagingTaskStateManager implements EventListener<ClockTick> {
  private static Logger LOG  = Logger.getLogger( ImagingTaskStateManager.class );

  public static void register( ) {
        Listeners.register( ClockTick.class, new ImagingTaskStateManager() );
  }

  @Override
  public void fireEvent(ClockTick event) {
    if (!( Bootstrap.isFinished() &&
        // Topology.isEnabledLocally( Imaging.class ) &&
         Topology.isEnabled( Eucalyptus.class ) ) )
       return;
    final Map <ImportTaskState, List<ImagingTask>> taskByState =
        Maps.newHashMap();
    final List<ImagingTask> allTasks = ImagingTasks.getImagingTasks();
    for(final ImagingTask task : allTasks){
      if(! taskByState.containsKey(task.getState()))
        taskByState.put(task.getState(), Lists.<ImagingTask>newArrayList());
     taskByState.get(task.getState()).add(task); 
    }
    if(taskByState.containsKey(ImportTaskState.NEW)){
      this.processNewTasks(taskByState.get(ImportTaskState.NEW));
    }
  }
  
  private void processNewTasks(final List<ImagingTask> tasks){
    for(final ImagingTask task : tasks){
      try{
        // create a volume and update the database
        final VolumeImagingTask volumeTask = (VolumeImagingTask) task;
        if(volumeTask.getVolumeId()==null || volumeTask.getVolumeId().length()<=0){
          final String zone = volumeTask.getAvailabilityZone();
          final int size = volumeTask.getVolumeSize();
          //create volume (already sanitized)
          try{
            final String volumeId = EucalyptusActivityTasks.getInstance().createVolume(zone, size);
            ImagingTasks.setVolumeId(volumeTask, volumeId);
          }catch(final Exception ex){
            ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "Failed to create the volume");
          }
        } else { /// check status
          final List<Volume> volumes = 
              EucalyptusActivityTasks.getInstance().describeVolumes(Lists.newArrayList(volumeTask.getVolumeId()));
          final Volume volume = volumes.get(0);
          final String volumeStatus = volume.getStatus();
          if("available".equals(volumeStatus)){
            try{
              final ConversionTask conversionTask = volumeTask.getTask();
              if(conversionTask.getImportVolume() != null){
                final ImportVolumeTaskDetails details = conversionTask.getImportVolume();
                final String manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                    new ImageManifestFile(details.getImage().getImportManifestUrl() , 
                        ImportImageManifest.INSTANCE ),
                        null, conversionTask.getConversionTaskId(), 1);
                ImagingTasks.setDownloadManifestUrl(volumeTask, manifestLocation);
                ImagingTasks.setState(volumeTask, ImportTaskState.PENDING, null);
              }else if (conversionTask.getImportInstance() != null){
                final ImportInstanceTaskDetails details = conversionTask.getImportInstance();
                final String manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                    new ImageManifestFile(details.getVolumes().get(0).getImage().getImportManifestUrl() , 
                        ImportImageManifest.INSTANCE ),
                        null, conversionTask.getConversionTaskId(), 1);
                ImagingTasks.setDownloadManifestUrl(volumeTask, manifestLocation);
                ImagingTasks.setState(volumeTask, ImportTaskState.PENDING, null);
              }else{
                LOG.error("Neither importInstance nor importVolume detail is found in the conversion task");
                ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "Not enough information is in the ImagingTask");         
              }
            } catch (final DownloadManifestException e) {
              LOG.error("Failed to generate download manifest", e);
              ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "failed to generate download manifest");
            }/// imaging worker can fetch the task
          }else if ("creating".equals(volumeStatus)){
            ; // continue to poll
          }else{
            ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, "The volume state is "+volumeStatus);
          }
        } 
      }catch(final Exception ex){
        LOG.error("Failed to process new task", ex);
      }
    }
  }
  
  private void processPendingTasks(final List<ImagingTask> tasks){
    
  }
}

