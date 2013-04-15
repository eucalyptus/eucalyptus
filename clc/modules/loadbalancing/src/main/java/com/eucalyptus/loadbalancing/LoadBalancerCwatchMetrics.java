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
package com.eucalyptus.loadbalancing;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.Dimension;
import com.eucalyptus.cloudwatch.Dimensions;
import com.eucalyptus.cloudwatch.MetricData;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerCwatchMetrics {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerCwatchMetrics.class );

	private static LoadBalancerCwatchMetrics _instance = new LoadBalancerCwatchMetrics();
	private Map<ElbDimension, ElbAggregate> metricsMap = new ConcurrentHashMap<ElbDimension, ElbAggregate>();
	private Map<ElbDimension, Integer> healthyCountMap = new ConcurrentHashMap<ElbDimension, Integer>();
	private Map<ElbDimension, Integer> unhealthyCountMap = new ConcurrentHashMap<ElbDimension, Integer>();
	private Map<String, Date> lastReported = new ConcurrentHashMap<String, Date>();
	
	private final int CLOUDWATCH_REPORTING_INTERVAL_SEC = 60;// http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/US_MonitoringLoadBalancerWithCW.html
	private final String CLOUDWATCH_ELB_METRIC_NAMESPACE = "AWS/ELB";
	
	private Object lock = new Object();
	private LoadBalancerCwatchMetrics(){	}
	public static LoadBalancerCwatchMetrics getInstance(){
		return _instance;
	}
	
	public void addMetric(final String servoId, final MetricData metric){
		// based on the servo Id, find the loadbalancer and the availability zone
		// 
		final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
		LoadBalancer lb = null;
		LoadBalancerZone lbZone = null;
		try{
			LoadBalancerServoInstance servo = Entities.uniqueResult(LoadBalancerServoInstance.named(servoId));
			lbZone = servo.getAvailabilityZone();
			lb = lbZone.getLoadbalancer();
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			throw Exceptions.toUndeclared("Failed to find the servo instance "+servoId);
		}catch(Exception ex){
			db.rollback();
			throw Exceptions.toUndeclared("database error while querying "+servoId);
		}
		final String userId = lb.getOwnerUserId();
		final String lbName = lb.getDisplayName();
		final String zoneName = lbZone.getName();
		ElbDimension dim = new ElbDimension(userId, lbName, zoneName);
		synchronized(lock){
			if(!metricsMap.containsKey(dim))
				metricsMap.put(dim, new ElbAggregate(lbName, zoneName));
			metricsMap.get(dim).addMetric(metric);
		}
		
		try{
			maybeReport(lb.getOwnerUserId());
		}catch(Exception ex){
			LOG.error(String.format("Failed to report cloudwatch metrics: %s-%s", lb.getOwnerUserName(), lb.getDisplayName()));
		}
	}
	
	public void updateHealthyCount(final String userId, final String loadbalancer, final String zone, int numHealthy){
		ElbDimension dim = new ElbDimension(userId, loadbalancer, zone);
		synchronized(lock){
			this.healthyCountMap.put(dim, numHealthy);
		}
	}
	
	public void updateUnhealthyCount(final String userId, final String loadbalancer, final String zone, int numUnHealthy){
		ElbDimension dim = new ElbDimension(userId, loadbalancer, zone);
		synchronized(lock){
			this.unhealthyCountMap.put(dim, numUnHealthy);
		}
	}
	
	private void maybeReport(final String userId){
		MetricData data = null;
		if(! this.lastReported.containsKey(userId)){
			this.lastReported.put(userId, new Date(System.currentTimeMillis()));
			return;
		}
		
		final Date lastReport = this.lastReported.get(userId);
		long currentTime = System.currentTimeMillis();
		int diffSec = (int)((currentTime - lastReport.getTime())/1000.0);
		if(diffSec >= CLOUDWATCH_REPORTING_INTERVAL_SEC)
			data = this.getDataAndClear(userId);
		
		if(data!=null && data.getMember()!=null && data.getMember().size()>0){
			try{
				EucalyptusActivityTasks.getInstance().putCloudWatchMetricData(userId, CLOUDWATCH_ELB_METRIC_NAMESPACE, data);
			}catch(Exception ex){
				Exceptions.toUndeclared(ex);
			}finally{
				this.lastReported.put(userId, new Date(currentTime));
			}
		}
	}
	
	private MetricData getDataAndClear(final String userId){
		/// dimensions
		/// lb - availability zone	
		final MetricData data = new MetricData();		
		data.setMember(Lists.<MetricDatum>newArrayList());
		final List<ElbDimension> toCleanup = Lists.newArrayList();
		
		synchronized(lock){
			for (final ElbDimension dim : this.metricsMap.keySet()){
				if(!dim.getUserId().equals(userId))
					continue;
				
				final ElbAggregate aggr = this.metricsMap.get(dim);
				final  List<MetricDatum> datumList = aggr.toELBStatistics();
				
				/// add HealthyHostCount and UnHealthyHostCount
			 	Dimensions dims = new Dimensions();
	        	Dimension lb = new Dimension();
	        	lb.setName("LoadBalancerName");
	        	lb.setValue(dim.getLoadbalancer());
	        	Dimension az = new Dimension();
	        	az.setName("AvailabilityZone");
	        	az.setValue(dim.getAvailabilityZone());
	        	dims.setMember(Lists.newArrayList(lb, az));
	        	
				if(this.healthyCountMap.containsKey(dim)){
		        	int numHealthy = this.healthyCountMap.get(dim);
		        	if(numHealthy > 0){
						MetricDatum datum = new MetricDatum();
						datum.setDimensions(dims);
						datum.setMetricName("HealthyHostCount");
						datum.setUnit("Count");
						datum.setValue((double)numHealthy);
						datumList.add(datum);
		        	}
				}
				if(this.unhealthyCountMap.containsKey(dim)){
					int numUnhealthy = this.unhealthyCountMap.get(dim);
					if(numUnhealthy > 0){
						MetricDatum datum = new MetricDatum();
						datum.setDimensions(dims);
						datum.setMetricName("UnHealthyHostCount");
						datum.setUnit("Count");
						datum.setValue((double)numUnhealthy);
						datumList.add(datum);
					}
				}
				if(datumList.size()>0)
					data.getMember().addAll(datumList);
				
				toCleanup.add(dim);
			}
			
			for(ElbDimension cleanup : toCleanup){
				this.metricsMap.remove(cleanup);
				this.healthyCountMap.remove(cleanup);
				this.unhealthyCountMap.remove(cleanup);
			}
		}
		
		return data;
	}
	
	
	public static class ElbAggregate{
        private double latency = 0; // latency in seconds
        private long requestCount = 0;
        private long httpCode_ELB_4XX = 0;
        private long httpCode_ELB_5XX = 0;
        private long httpCode_Backend_2XX = 0;
        private long httpCode_Backend_3XX = 0;
        private long httpCode_Backend_4XX = 0;
        private long httpCode_Backend_5XX = 0;
        private String loadbalancer = null;
        private String availabilityZone = null;
        
        public ElbAggregate(final String loadbalancer, final String availabilityZone){
        	this.loadbalancer = loadbalancer;
        	this.availabilityZone = availabilityZone;
        }
       
        public void addMetric(final MetricData metric){
        	//        name = ['Latency','RequestCount','HTTPCode_ELB_4XX','HTTPCode_ELB_5XX','HTTPCode_Backend_2XX','HTTPCode_Backend_3XX','HTTPCode_Backend_4XX','HTTPCode_Backend_5XX']
            // value = [metric.Latency, metric.RequestCount, metric.HTTPCode_ELB_4XX, metric.HTTPCode_ELB_5XX, metric.HTTPCode_Backend_2XX, metric.HTTPCode_Backend_3XX, metric.HTTPCode_Backend_4XX, metric.HTTPCode_Backend_5XX]
        	if(metric.getMember()!=null){
        		for(final MetricDatum datum : metric.getMember()){
        			String name = datum.getMetricName();
        			double value = datum.getValue();
        			if(name.equals("Latency")){ /// sent in milliseconds
        				value = value / 1000.0; // to seconds
        				this.latency += value;
        			}else if(name.equals("RequestCount")){
        				this.requestCount += (long) value;
        			}else if (name.equals("HTTPCode_ELB_4XX")){
        				this.httpCode_ELB_4XX += (long) value;
        			}else if (name.equals("HTTPCode_ELB_5XX")){
        				this.httpCode_ELB_5XX += (long) value;
        			}else if(name.equals("HTTPCode_Backend_2XX")){
        				this.httpCode_Backend_2XX += (long) value;
        			}else if(name.equals("HTTPCode_Backend_3XX")){
        				this.httpCode_Backend_3XX += (long) value;
        			}else if(name.equals("HTTPCode_Backend_4XX")){
        				this.httpCode_Backend_4XX += (long) value;
        			}else if(name.equals("HTTPCode_Backend_5XX")){
        				this.httpCode_Backend_5XX += (long) value;
        			}
        		}
        	}
        }
        
        public List<MetricDatum> toELBStatistics(){
        	List<MetricDatum> result = Lists.<MetricDatum>newArrayList();
        	Dimensions dims = new Dimensions();
        	Dimension lb = new Dimension();
        	lb.setName("LoadBalancerName");
        	lb.setValue(this.loadbalancer);
        	Dimension az = new Dimension();
        	az.setName("AvailabilityZone");
        	az.setValue(this.availabilityZone);
        	dims.setMember(Lists.newArrayList(lb, az));
        	
        	if(this.latency > 0 && this.requestCount>0){
	        	final MetricDatum latencyData = new MetricDatum();
	        	latencyData.setDimensions(dims);
	        	latencyData.setMetricName("Latency");
	        	latencyData.setUnit("Seconds");
	        	double latency = this.latency / (double) this.requestCount;
	        	latencyData.setValue(latency);
	        	result.add(latencyData);
        	}
        	
        	if(this.requestCount>0){
        		final MetricDatum reqCountData = new MetricDatum();
	        	reqCountData.setDimensions(dims);
	        	reqCountData.setMetricName("RequestCount");
	        	reqCountData.setUnit("Count");
	        	reqCountData.setValue((double)this.requestCount);
	        	result.add(reqCountData);
        	}
        	if(this.httpCode_ELB_4XX>0){
	        	final MetricDatum httpCode_ELB_4XX = new MetricDatum();
	        	httpCode_ELB_4XX.setDimensions(dims);
	        	httpCode_ELB_4XX.setMetricName("HTTPCode_ELB_4XX");
	        	httpCode_ELB_4XX.setUnit("Count");
	        	httpCode_ELB_4XX.setValue((double)this.httpCode_ELB_4XX);
	        	result.add(httpCode_ELB_4XX);
        	}
        	if(this.httpCode_ELB_5XX>0){
	        	final MetricDatum httpCode_ELB_5XX = new MetricDatum();
	        	httpCode_ELB_5XX.setDimensions(dims);
	        	httpCode_ELB_5XX.setMetricName("HTTPCode_ELB_5XX");
	        	httpCode_ELB_5XX.setUnit("Count");
	        	httpCode_ELB_5XX.setValue((double)this.httpCode_ELB_5XX);
	        	result.add(httpCode_ELB_5XX);
        	}
        	if(this.httpCode_Backend_2XX>0){
	        	final MetricDatum httpCode_Backend_2XX = new MetricDatum();
	        	httpCode_Backend_2XX.setDimensions(dims);
	        	httpCode_Backend_2XX.setMetricName("HTTPCode_Backend_2XX");
	        	httpCode_Backend_2XX.setUnit("Count");
	        	httpCode_Backend_2XX.setValue((double)this.httpCode_Backend_2XX);
	        	result.add(httpCode_Backend_2XX);
        	}
        	if(this.httpCode_Backend_3XX>0){
	        	final MetricDatum httpCode_Backend_3XX = new MetricDatum();
	        	httpCode_Backend_3XX.setDimensions(dims);
	        	httpCode_Backend_3XX.setMetricName("HTTPCode_Backend_3XX");
	        	httpCode_Backend_3XX.setUnit("Count");
	        	httpCode_Backend_3XX.setValue((double)this.httpCode_Backend_3XX);
	        	result.add(httpCode_Backend_3XX);
        	}
        	if(this.httpCode_Backend_4XX > 0){
	        	final MetricDatum httpCode_Backend_4XX = new MetricDatum();
	        	httpCode_Backend_4XX.setDimensions(dims);
	        	httpCode_Backend_4XX.setMetricName("HTTPCode_Backend_4XX");
	        	httpCode_Backend_4XX.setUnit("Count");
	        	httpCode_Backend_4XX.setValue((double)this.httpCode_Backend_4XX);
	        	result.add(httpCode_Backend_4XX);
        	}
        	if(this.httpCode_Backend_5XX > 0){
	        	final MetricDatum httpCode_Backend_5XX = new MetricDatum();
	        	httpCode_Backend_5XX.setDimensions(dims);
	        	httpCode_Backend_5XX.setMetricName("HTTPCode_Backend_5XX");
	        	httpCode_Backend_5XX.setUnit("Count");
	        	httpCode_Backend_5XX.setValue((double) this.httpCode_Backend_5XX);
	        	result.add(httpCode_Backend_5XX);
        	}
        	return result;
        }


        @Override
        public String toString(){
        	return String.format("aggregate=%.2f %d %d %d %d %d %d %d", this.latency, this.requestCount, this.httpCode_ELB_4XX, this.httpCode_ELB_5XX,
        			this.httpCode_Backend_2XX, this.httpCode_Backend_3XX, this.httpCode_Backend_4XX, this.httpCode_Backend_5XX);
        }
	}
	
	public static class ElbDimension{
		private String userId=null;
		private String loadbalancer=null;
		private String availabilityZone=null;
		
		public ElbDimension(final String userId, final String lb, final String zone){
			this.userId = userId;
			this.loadbalancer = lb;
			this.availabilityZone = zone;
		}
		
		public String getUserId(){
			return this.userId;
		}
		
		public String getLoadbalancer(){
			return this.loadbalancer;
		}
		
		public String getAvailabilityZone(){
			return this.availabilityZone;
		}
		
		@Override 
		public boolean equals(Object obj){
			if(obj==null)
				return false;
			if(obj.getClass() != ElbDimension.class)
				return false;
			ElbDimension other = (ElbDimension) obj;
			
			if(this.userId==null){
				if(other.userId!=null)
					return false;
			}else if(! this.userId.equals(other.userId))
				return false;
			
			if(this.loadbalancer==null){
				if(other.loadbalancer!=null)
					return false;
			}else if(! this.loadbalancer.equals(other.loadbalancer))
				return false;
			
			if(this.availabilityZone == null){
				if (other.availabilityZone!=null)
					return false;
			}else if(! this.availabilityZone.equals(other.availabilityZone))
				return false;
			
			return true;
		}
		
		@Override
		public int hashCode(){
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.userId == null) ? 0 : this.userId.hashCode());
			result = prime * result + ((this.loadbalancer == null) ? 0 : this.loadbalancer.hashCode());
			result = prime * result + ((this.availabilityZone == null) ? 0 : this.availabilityZone.hashCode());
			return result;
		}
		
		@Override
		public String toString(){
			return String.format("dimension-%s-%s-%s", this.userId, this.loadbalancer, this.availabilityZone);
		}
	}
}
