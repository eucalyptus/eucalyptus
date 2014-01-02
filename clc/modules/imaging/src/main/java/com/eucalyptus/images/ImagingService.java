/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.images;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.conversion.ImportManager;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ConversionTask;

public class ImagingService {
	private static Logger LOG = Logger.getLogger( ImagingService.class );

	public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
		PutInstanceImportTaskStatusResponseType reply = request.getReply( );
		LOG.debug(request);
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
						new ImagingTask(task.getTask(), status, request.getBytesConverted()));
			} else {
				ImportManager.putConversionTask(request.getImportTaskId(),
						new ImagingTask(task.getTask(), status, task.getBytesProcessed()));
			}
		}
		return reply;
	}

	public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) throws EucalyptusCloudException {
		GetInstanceImportTaskResponseType reply = request.getReply( );
		LOG.debug(request);
		ConversionTask taskToServe = null;
		Iterator<Entry<String, ImagingTask>> it = ImportManager.getTasksIterator();
		while(it.hasNext()){
			Entry<String, ImagingTask> entry = it.next();
			ImagingTask task = entry.getValue();
			// get a new task if exists
			if (task.getState() == ImportTaskState.NEW) {
				ImportManager.putConversionTask(task.getId(),
						new ImagingTask(task.getTask(), ImportTaskState.PENDING, 0));
				taskToServe = task.getTask();
				break;
			}
		}
		reply.setImportTaskId(taskToServe != null ? taskToServe.getConversionTaskId() : "");
		return reply;
	}
}

