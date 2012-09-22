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

package com.eucalyptus.cluster.callback;

import java.util.ArrayList;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cluster.Clusters;

import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.InstanceUsageEvent;

import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.BroadcastCallback;

import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;

import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsType;
import edu.ucsb.eucalyptus.msgs.MetricCounterType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsType;
import edu.ucsb.eucalyptus.msgs.MetricsResourceType;
import edu.ucsb.eucalyptus.msgs.SensorsResourceType;
import edu.ucsb.eucalyptus.msgs.MetricDimensionsValuesType;

public class DescribeSensorCallback extends
	BroadcastCallback<DescribeSensorsType, DescribeSensorsResponse> {

    private static Logger LOG = Logger.getLogger(DescribeSensorCallback.class);
    private int historySize;
    private int collectionIntervalTimeMs;
    ArrayList<String> sensorIds = new ArrayList<String>();
    ArrayList<String> instanceIds = new ArrayList<String>();
    final ListenerRegistry listener = ListenerRegistry.getInstance();

    public DescribeSensorCallback(int historySize,
	    int collectionIntervalTimeMS, ArrayList<String> sensorIds,
	    ArrayList<String> instanceIds) {
	this.historySize = historySize;
	this.collectionIntervalTimeMs = collectionIntervalTimeMS;
	this.sensorIds = sensorIds;
	this.instanceIds = instanceIds;

	DescribeSensorsType msg = new DescribeSensorsType(this.historySize,
		this.collectionIntervalTimeMs, sensorIds, instanceIds);

	try {
	    msg.setUser(Accounts.lookupSystemAdmin());
	} catch (AuthException e) {
	    LOG.error("Unable to find the system user", e);
	}

	this.setRequest(msg);

    }

    @Override
    public void initialize(DescribeSensorsType msg) {
	try {
	    msg.setNameServer(edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration
		    .getSystemConfiguration().getNameserverAddress());
	    msg.setClusterControllers(Lists.newArrayList(Clusters.getInstance()
		    .getClusterAddresses()));
	} catch (Exception e) {
	    LOG.debug(e, e);
	}
    }

    @Override
    public BroadcastCallback<DescribeSensorsType, DescribeSensorsResponse> newInstance() {
	return new DescribeSensorCallback(this.historySize,
		this.collectionIntervalTimeMs, this.sensorIds, this.instanceIds);
    }

    @Override
    public void fireException(Throwable e) {
	LOG.debug("Request failed: "
		+ LogUtil.subheader(this.getRequest().toString(
			"eucalyptus_ucsb_edu")));
	Logs.extreme().error(e, e);
    }

    @Override
    public void fire(DescribeSensorsResponse msg) {

	try {

	    List<VmInstance> vmIntList = VmInstances.list();
	    List<String> uuidList = new ArrayList<String>();
	    for (VmInstance vmInt : vmIntList) {
		if (vmInt.getState().equals(VmState.RUNNING)) {
		    uuidList.add(vmInt.getInstanceUuid());
		}
	    }

	    String resourceName, resourceUuid, metricName, dimensionName = "";
	    int sequenceNum = -1;
	    long valueDatestamp = -1;
	    Double value = null;

	    for (SensorsResourceType sensorData : msg.getSensorsResources()) {
		resourceName = sensorData.getResourceName();
		resourceUuid = sensorData.getResourceUuid();

		if (uuidList.contains(resourceUuid)) {

		    for (MetricsResourceType metricType : sensorData
			    .getMetrics()) {
			metricName = metricType.getMetricName();

			for (MetricCounterType counterType : metricType
				.getCounters()) {
			    sequenceNum = Integer.parseInt(counterType
				    .getSequenceNum().toString());

			    for (MetricDimensionsType dimensionType : counterType
				    .getDimensions()) {

				dimensionName = dimensionType
					.getDimensionName();

				for (MetricDimensionsValuesType valueType : dimensionType
					.getValues()) {

				    value = valueType.getValue();
				    valueDatestamp = valueType.getTimestamp()
					    .getTime();

				    fireUsageEvent(new com.eucalyptus.reporting.event.InstanceUsageEvent(
					    resourceUuid,
					    System.currentTimeMillis(),
					    resourceName, metricName,
					    sequenceNum, dimensionName, value,
					    valueDatestamp));

				}
			    }
			}
		    }
		}
	    }
	} catch (Exception ex) {
	    LOG.debug("Unable to fire describe sensors call back", ex);

	}

    }

    private void fireUsageEvent(InstanceUsageEvent instanceUsageEvent) {

	try {
	    listener.fireEvent(instanceUsageEvent);
	} catch (EventFailedException e) {
	    LOG.debug("Failed to fire instance usage event"
		    + instanceUsageEvent, e);
	}

    }

}
