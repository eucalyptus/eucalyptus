/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.reporting.modules.backend;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.callback.DescribeSensorCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstance.VmState;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "reporting", description = "Parameters controlling reporting")
public class DescribeSensorsListener implements EventListener<Hertz> {

  @ConfigurableField(initial = "5", description = "How often the reporting system requests information from the cluster controller")
  public static long DEFAULT_POLL_INTERVAL_MINS = 5;
  
  private Integer COLLECTION_INTERVAL_TIME_MS;
  private Integer HISTORY_SIZE = 5;
  private Integer MAX_WRITE_INTERVAL_MS = 86400000;
  private Integer SENSOR_QUERY_BATCH_SIZE = 10;
  
  private static final Logger LOG = Logger.getLogger(DescribeSensorsListener.class);
  
  public static void register() {
    Listeners.register( Hertz.class, new DescribeSensorsListener() );
  }
  
  @Override
  public void fireEvent( Hertz event ) {
    if ( !Bootstrap.isOperational( ) || !BootstrapArgs.isCloudController( ) || !event.isAsserted( DEFAULT_POLL_INTERVAL_MINS ) ) {
      return;
    } else {
      if (DEFAULT_POLL_INTERVAL_MINS >= 1) {
	  COLLECTION_INTERVAL_TIME_MS = ((int) TimeUnit.MINUTES
		  .toMillis(DEFAULT_POLL_INTERVAL_MINS) / 2);
      } else {
	  COLLECTION_INTERVAL_TIME_MS = 0; 
      }

      if (COLLECTION_INTERVAL_TIME_MS == 0 ) {	
	  LOG.debug("The instance usage report is disabled");
      } else if (COLLECTION_INTERVAL_TIME_MS <= MAX_WRITE_INTERVAL_MS) {

	  try {

	      if (event.isAsserted(TimeUnit.MINUTES
		      .toSeconds(DEFAULT_POLL_INTERVAL_MINS))) {
		  if (Bootstrap.isFinished() && Hosts.isCoordinator()) {

		      List<VmInstance> instList = VmInstances.list( VmState.RUNNING );
		      
		      List<String> instIdList = Lists.newArrayList();
		      
		      for (final VmInstance inst : instList) {
		        instIdList.add(inst.getInstanceId());
		      }
		      Iterable<List<String>> processInts = Iterables.paddedPartition(instIdList, SENSOR_QUERY_BATCH_SIZE);
		      

		      for (final ServiceConfiguration ccConfig : Topology
			      .enabledServices(ClusterController.class)) {
			  for(List<String> instIds : processInts) {

			  ArrayList<String> instanceIds = Lists.newArrayList(instIds);
			  Iterables.removeIf(instanceIds, Predicates.isNull());
			  
			  AsyncRequests.newRequest(
				  new DescribeSensorCallback(HISTORY_SIZE,
					  COLLECTION_INTERVAL_TIME_MS, instanceIds))
					  .dispatch(ccConfig);
			  LOG.debug("DecribeSensorCallback has been successfully executed");
			  }
		      }
		  }
	      }
	  } catch (Exception ex) {
	      LOG.error("Unable to listen for describe sensors events", ex);
	  }

      } else {
	  LOG.error("DEFAULT_POLL_INTERVAL_MINS : "
		  + DEFAULT_POLL_INTERVAL_MINS
		  + " must be less than 1440 minutes");
      }
    }
  }
}
