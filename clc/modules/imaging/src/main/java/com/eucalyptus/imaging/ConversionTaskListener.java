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

package com.eucalyptus.imaging;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.compute.conversion.ImportManager;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;

@ConfigurableClass( root = "imaging", description = "Parameters controlling image conversion tasks")
public class ConversionTaskListener implements EventListener<Hertz> {

  	private static Long DEFAULT_WORK_INTERVAL_SEC = 30L;
   	@ConfigurableField(initial = "360", description = "How long (min) cloud controller would report completed"
   			+ " or canceled image/volume import task back to the user")
   	public static Long CONVERSION_TASKS_REPORT_TIME_MIN = 360L;
   	// 5 min
   	private static Long INITIAL_WAIT_TIME_FROM_IMAGING_SERVICE_MS = 5*60*1000L;
   	
  	private static final Logger LOG = Logger.getLogger(ConversionTaskListener.class);
  
  	public static void register() {
	  	Listeners.register( Hertz.class, new ConversionTaskListener() );
  	}
  
	@Override
	public void fireEvent( Hertz event ) {
	  /*
		if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController() || !event.isAsserted(DEFAULT_WORK_INTERVAL_SEC)) {
			return;
		} else {
			Iterator<Entry<String, ImagingTask>> it = ImportManager.getTasksIterator();
			while(it.hasNext()){
				Entry<String, ImagingTask> entry = it.next();
				ImagingTask task = entry.getValue();
				// lets check if we need to revert any PENDING task back to NEW
				if (task.getState() == ImportTaskState.PENDING
						&& task.lastUpdateMillis() < INITIAL_WAIT_TIME_FROM_IMAGING_SERVICE_MS) {
					LOG.info("Conversion task " + task.getDisplayName() + " has not been changed from PENDING state. Setting it back to NEW.");
					ImportManager.putConversionTask(task.getDisplayName(), new ImagingTask( task.getOwner(), task.getDisplayName(), task.getTask(), ImportTaskState.NEW, 0));
				}
				// lets check if we need to remove task from DB
				if ((task.getState() == ImportTaskState.DONE || task.getState() == ImportTaskState.CANCELLED)
						&& task.lastUpdateMillis() <  CONVERSION_TASKS_REPORT_TIME_MIN*60*1000L) {
					LOG.debug("Conversion task " + task.getDisplayName() + " has been removed from DB.");
					ImagingTaskDao.getInstance().removeFromDb(task);
					it.remove();
				}
			}
		}*/
	}
}
