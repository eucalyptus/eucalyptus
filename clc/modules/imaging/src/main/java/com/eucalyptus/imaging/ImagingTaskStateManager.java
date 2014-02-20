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
import com.eucalyptus.imaging.worker.EucalyptusActivityTasks;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportInstanceVolumeDetail;
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
       if(task instanceof VolumeImagingTask)
         processNewVolumeImagingTask((VolumeImagingTask) task);
       else if(task instanceof InstanceImagingTask)
         processNewInstanceImagingTask((InstanceImagingTask)task);
       else
         throw new Exception("Invalid ImagingTask");
      }catch(final Exception ex){
        ImagingTasks.setState(task, ImportTaskState.FAILED, "Failed to create the volume");
        LOG.error("Failed to process new task", ex);
      }
    }
  }
  
  private void processNewInstanceImagingTask(final InstanceImagingTask instanceTask) throws Exception{
    // for each disk image, create a volume and set its state accordingly
    final ImportInstanceTaskDetails taskDetail=
        instanceTask.getTask().getImportInstance();
    final List<ImportInstanceVolumeDetail> volumes = taskDetail.getVolumes();
    if(volumes==null)
      return;
    
    try{
      int numVolumeCreated = 0;
      for(final ImportInstanceVolumeDetail volume : volumes){
        if(volume.getVolume()==null || volume.getVolume().getId() == null ||  
            volume.getVolume().getId().length()<=0){
          final String zone = volume.getAvailabilityZone();
          final Integer size = volume.getVolume().getSize();
          if(zone==null)
            throw new Exception("Availability zone is missing from the volume detail");
          if(size==null || size <=0 )
            throw new Exception("Volume size is missing from the volume detail");
          try{
            final String volumeId = EucalyptusActivityTasks.getInstance().createVolume(zone, size);
            volume.getVolume().setId(volumeId);
          }catch(final Exception ex){
            throw new Exception("Failed to create the volume", ex);
          }
        }else{
          String volumeStatus= null;
          try{
            final List<Volume> eucaVolumes = 
                EucalyptusActivityTasks.getInstance().describeVolumes(Lists.newArrayList(volume.getVolume().getId()));
            final Volume eucaVolume = eucaVolumes.get(0);
            volumeStatus = eucaVolume.getStatus();
          }catch(final Exception ex){
            throw new Exception("Failed to check the state of the volume "+volume.getVolume().getId());
          }
          if("available".equals(volumeStatus)){
            volume.setStatus("active");
            numVolumeCreated++;
          }else if ("creating".equals(volumeStatus)){
            volume.setStatus("active");
          }else{
            volume.setStatus("cancelled");
            volume.setStatusMessage("Failed to create the volume");
            throw new Exception("Volume "+volume.getVolume().getId()+" is in "+volumeStatus);
          }
        } 
      }
      if(numVolumeCreated == volumes.size()){
        ImagingTasks.setState(instanceTask, ImportTaskState.PENDING, null);
      }
    }catch(Exception ex){
      throw ex;
    }finally{
      ImagingTasks.updateTaskInJson(instanceTask); 
    }
  }
  
  private void processNewVolumeImagingTask(final VolumeImagingTask volumeTask) throws Exception{
    if(volumeTask.getVolumeId()==null || volumeTask.getVolumeId().length()<=0){
      final String zone = volumeTask.getAvailabilityZone();
      final int size = volumeTask.getVolumeSize();
      //create volume (already sanitized)
      try{
        final String volumeId = EucalyptusActivityTasks.getInstance().createVolume(zone, size);
        ImagingTasks.setVolumeId(volumeTask, volumeId);
      }catch(final Exception ex){
        throw new Exception("Failed to create the volume", ex);
      }
    } else { /// check status
      final List<Volume> volumes = 
          EucalyptusActivityTasks.getInstance().describeVolumes(Lists.newArrayList(volumeTask.getVolumeId()));
      final Volume volume = volumes.get(0);
      final String volumeStatus = volume.getStatus();
      if("available".equals(volumeStatus)){
        final ConversionTask conversionTask = volumeTask.getTask();
        if(conversionTask.getImportVolume() != null){
          ImagingTasks.setState(volumeTask, ImportTaskState.PENDING, null);
        }else{
          throw new Exception("No importVolume detail is found in the conversion task");
        }
      }else if ("creating".equals(volumeStatus)){
        ; // continue to poll
      }else{
        throw new Exception("The volume "+volume.getVolumeId()+"'s state is "+volumeStatus);
      }
    }  
  }
  
  private void processPendingTasks(final List<ImagingTask> tasks){
    
  }
}

