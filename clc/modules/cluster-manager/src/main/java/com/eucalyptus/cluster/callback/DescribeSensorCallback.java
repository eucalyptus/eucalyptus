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

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.records.Logs;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.BroadcastCallback;

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

	    //Document sensorDoc = loadXMLFromString(msg.toString());
	    
	    //sensorDoc.get
	    // TODO : Need to fire the correct usage events to the domain model

	     for (SensorsResourceType sensorData : msg.getSensorsResources()) {
		LOG.debug("sensorData.getResourceName() : "
			+ sensorData.getResourceName());

		for (MetricsResourceType metricType : sensorData.getMetrics()) {
		    LOG.debug("sensorData.getMetrics() : "
			    + metricType.getMetricName());

		    for (MetricCounterType counterType : metricType
			    .getCounters()) {
			LOG.debug("metricType.getCounters : " + counterType);

			for (MetricDimensionsType dimensionType : counterType
				.getDimensions()) {
			    LOG.debug("counterType.getDimensions() : "
				    + dimensionType);

			    for (MetricDimensionsValuesType valueType : dimensionType
				    .getValues()) {
				// fire constructed event to the domain model
				// Need real uuid from describe sensors.

				ListenerRegistry
					.getInstance()
					.fireEvent(
						new com.eucalyptus.reporting.event.InstanceUsageEvent(
							String.valueOf(sensorData
								.hashCode()),
							System.currentTimeMillis(),
							sensorData
								.getResourceName(),
							metricType
								.getMetricName(),
							Integer.parseInt(counterType
								.getSequenceNum()
								.toString()),
							dimensionType
								.getDimensionName(),
							valueType.getValue()
								,
							Long.getLong(valueType
								.getTimestamp()
								.toString())));
			    }
			}
		    }
		}
	    }
	    
	   
	} catch (Exception ex) {
	    LOG.debug("Unable to fire describe sensors call back", ex);

	}
    }

    private static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private InstanceUsageEvent buildInstanceUsageEvent(Document sensorsDoc) {
    
	String uuid = sensorsDoc.getElementsByTagName("uuid").item(0).getNodeValue();
	String timestamp = sensorsDoc.getElementsByTagName("timestamp").item(0).getNodeValue();
	String resourceName = sensorsDoc.getElementsByTagName("resourceName").item(0).getNodeValue();
	String metric = sensorsDoc.getElementsByTagName("metric").item(0).getNodeValue();
	String sequenceNum = sensorsDoc.getElementsByTagName("sequenceNum").item(0).getNodeValue();
	String dimension;
	Double value;
	long valueTimestamp;
	
	
	
	Element TIMESTAMP = sensorsDoc.getElementById("timestamp");
	
	return null; //  new InstanceUsageEvent(UUID.getNodeValue(),TIMESTAMP.getNodeValue(), );
    }
    
}
