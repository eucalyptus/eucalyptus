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

import java.util.ArrayList;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.callback.DescribeSensorCallback;

import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.util.async.AsyncRequests;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;

@ConfigurableClass( root = "reporting", description = "Parameters controlling reporting")
public class DescribeSensorsListener implements EventListener<Hertz> {

    @ConfigurableField(initial = "1200", description = "How often the reporting system requests information from the cluster controller")
    public static long DEFAULT_WRITE_INTERVAL_SECS = 1200;
    private static Logger LOG = Logger.getLogger(DescribeSensorsListener.class);

    public static void register() {
	Listeners.register(Hertz.class, new DescribeSensorsListener());
    }

    @Override
    public void fireEvent(Hertz event) {

	ArrayList<String> fakeSensorIds = new ArrayList<String>();
	ArrayList<String> fakeInstanceIds = new ArrayList<String>();
	fakeSensorIds.add("SensorId"); // future feature
	fakeInstanceIds.add("InstanceIds"); // future feature

	try {

	    if (event.isAsserted(DEFAULT_WRITE_INTERVAL_SECS)) {
		if (Bootstrap.isFinished() && Hosts.isCoordinator()) {

		    for (final ServiceConfiguration ccConfig : Topology
			    .enabledServices(ClusterController.class)) {

			AsyncRequests.newRequest(
				new DescribeSensorCallback(Units.HISTORY_SIZE,
					Units.COLLECTION_INTERVAL_TIME_MS,
					fakeSensorIds, fakeInstanceIds))
				.dispatch(ccConfig);
			LOG.debug("DecribeSensorCallback has been successfully executed");
		    }
		}
	    }
	} catch (Exception ex) {
	    LOG.error("Unable to listen for describe sensors events", ex);
	}
    }
}
