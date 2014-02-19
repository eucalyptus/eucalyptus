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

import com.eucalyptus.imaging.manifest.DownloadManifestException;
import com.eucalyptus.imaging.manifest.DownloadManifestFactory;
import com.eucalyptus.imaging.manifest.ImageManifestFile;
import com.eucalyptus.imaging.manifest.ImportImageManifest;
import com.eucalyptus.imaging.manifest.InvalidBaseManifestException;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ConversionTask;
import edu.ucsb.eucalyptus.msgs.ImportInstanceTaskDetails;
import edu.ucsb.eucalyptus.msgs.ImportVolumeTaskDetails;
public class ImagingService {
  private static Logger LOG = Logger.getLogger( ImagingService.class );

  public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
    final PutInstanceImportTaskStatusResponseType reply = request.getReply( );
  /*  LOG.debug(request);
    ImportTaskState status;
    try {
      status = ImportTaskState.fromString(request.getStatus());
    } catch (IllegalArgumentException ex) {
      LOG.debug("Invalid conversions status");
      reply.setStatusMessage("Invalid status");
      reply.set_return(false);
      return reply;
    }
    ImagingTask task = ImportManager.getConversionTask(request.getImportTaskId());
    if (task == null) {
      LOG.debug("Invalid conversions task id");
      reply.setStatusMessage("Invalid task id");
      reply.set_return(false);
      return reply;
    }

    if (task != null) {
      // replace old with new task
      if ( (status == ImportTaskState.CONVERTING || status == ImportTaskState.CONVERTED)
          && request.getBytesConverted() > 0) {
        ImportManager.putConversionTask(request.getImportTaskId(),
            new ImagingTask( task.getOwner(), task.getDisplayName(), task.getTask(), status, request.getBytesConverted()));
      } else {
        ImportManager.putConversionTask(request.getImportTaskId(),
            new ImagingTask( task.getOwner(), task.getDisplayName(), task.getTask(), status, task.getBytesProcessed()));
      }
    }*/
    return reply;
  }

  public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) throws EucalyptusCloudException {
    final GetInstanceImportTaskResponseType reply = request.getReply( );
    try{
      final VolumeImagingTask volumeTask = (VolumeImagingTask) AbstractTaskScheduler.getScheduler().getNext();
      if(volumeTask!=null){
        ConversionTask conversionTask = volumeTask.getTask();
        try {
          String manifestLocation = null;
          if(conversionTask.getImportVolume() != null){
            final ImportVolumeTaskDetails details = conversionTask.getImportVolume();
            manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                new ImageManifestFile(details.getImage().getImportManifestUrl(),
                    ImportImageManifest.INSTANCE ),
                    null, conversionTask.getConversionTaskId(), 1);
          }else if (conversionTask.getImportInstance() != null){
            final ImportInstanceTaskDetails details = conversionTask.getImportInstance();
            manifestLocation = DownloadManifestFactory.generateDownloadManifest(
                new ImageManifestFile(details.getVolumes().get(0).getImage().getImportManifestUrl(),
                    ImportImageManifest.INSTANCE ),
                    null, conversionTask.getConversionTaskId(), 1);
            ImagingTasks.setDownloadManifestUrl(volumeTask, manifestLocation);
          }
          ImagingTasks.setDownloadManifestUrl(volumeTask, manifestLocation);
          ImagingTasks.setState(volumeTask, ImportTaskState.DOWNLOADING, null); //TODO: this should be a new state
          reply.setImportTaskId(volumeTask.getTask().getConversionTaskId());
          reply.setManifestUrl(manifestLocation);
          reply.setVolumeId(volumeTask.getVolumeId());
        } catch (InvalidBaseManifestException ex) {
          // if base manifest is invalid there are no reason to try generating download manifest again
          ImagingTasks.setState(volumeTask, ImportTaskState.FAILED, null);
        }
      }
    }catch(final Exception ex){
      LOG.error("Failed to schedule a task", ex);
    }
    return reply;
  }
}
