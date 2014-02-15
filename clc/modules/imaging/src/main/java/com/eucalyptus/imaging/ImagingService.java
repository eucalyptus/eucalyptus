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

import com.eucalyptus.util.EucalyptusCloudException;
public class ImagingService {
  private static Logger LOG = Logger.getLogger( ImagingService.class );

  public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
		final PutInstanceImportTaskStatusResponseType reply = request.getReply( );
	/*	LOG.debug(request);
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
		  final VolumeImagingTask task = (VolumeImagingTask) AbstractTaskScheduler.getScheduler().getNext();
		  if(task!=null){
  		  reply.setImportTaskId(task.getTask().getConversionTaskId());
  		  reply.setManifestUrl(task.getDownloadManifestUrl());
  		}
		}catch(final Exception ex){
		  LOG.error("Failed to schedule a task", ex);
		}
		return reply;
	}
}
