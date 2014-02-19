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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerCwatchMetrics {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerCwatchMetrics.class );

	private static LoadBalancerCwatchMetrics _instance = new LoadBalancerCwatchMetrics();
	private Map<ElbDimension, ElbAggregate> metricsMap = new ConcurrentHashMap<ElbDimension, ElbAggregate>();
	
	private Map<BackendInstance, Boolean> instanceHealthMap = new ConcurrentHashMap<BackendInstance, Boolean>();
	private Map<BackendInstance, ElbDimension> instanceToDimensionMap = new ConcurrentHashMap<BackendInstance, ElbDimension>();
	
	
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
		LoadBalancerZoneCoreView lbZone = null;
		try{
			LoadBalancerServoInstance servo = Entities.uniqueResult(LoadBalancerServoInstance.named(servoId));
			lbZone = servo.getAvailabilityZone();
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
			throw Exceptions.toUndeclared("Failed to find the servo instance "+servoId);
		}catch(Exception ex){
			db.rollback();
			throw Exceptions.toUndeclared("database error while querying "+servoId);
		}finally{
			if(db.isActive())
				db.rollback();
		}
		
		LoadBalancerZone zone = null;
		LoadBalancerCoreView lb = null;
		
		try{
			zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(lbZone);
			lb = zone.getLoadbalancer();
		}catch(final Exception ex){
			return;
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
			maybeReport(userId);
		}catch(Exception ex){
			LOG.error(String.format("Failed to report cloudwatch metrics: %s-%s", lb.getOwnerUserName(), lb.getDisplayName()));
		}
	}
	
	public void updateHealthy(final LoadBalancerCoreView lb, final String zone, final String instanceId){
		final ElbDimension dim = new ElbDimension(lb.getOwnerUserId(), lb.getDisplayName(), zone);
		final BackendInstance key = new BackendInstance(lb, instanceId);
		synchronized(lock){
			if(!this.instanceToDimensionMap.containsKey(key))
				this.instanceToDimensionMap.put(key, dim);
			
			this.instanceHealthMap.put(key, Boolean.TRUE);
			if(!metricsMap.containsKey(dim))
				metricsMap.put(dim, new ElbAggregate(lb.getDisplayName(), zone)); 
		}
	}
	
	public void updateUnHealthy(final LoadBalancerCoreView lb, final String zone, final String instanceId){
		final ElbDimension dim = new ElbDimension(lb.getOwnerUserId(), lb.getDisplayName(), zone);
		final BackendInstance key = new BackendInstance(lb, instanceId);
		
		synchronized(lock){
			if(!this.instanceToDimensionMap.containsKey(key))
				this.instanceToDimensionMap.put(key, dim);
			this.instanceHealthMap.put(key, Boolean.FALSE);
			if(!metricsMap.containsKey(dim))
				metricsMap.put(dim, new ElbAggregate(lb.getDisplayName(), zone));
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
    	
		final Map<ElbDimension, Integer> healthyCountMap = new HashMap<ElbDimension, Integer>();
    	final Map<ElbDimension, Integer> unhealthyCountMap = new HashMap<ElbDimension, Integer>();
    	
    	final List<BackendInstance> candidates = Lists.newArrayList(Iterables.filter(this.instanceHealthMap.keySet(), new Predicate<BackendInstance>(){
			@Override
			public boolean apply(@Nullable BackendInstance instance) {
				try{
					if(instance.getLoadBalancer()==null)
						return false;
					if(!userId.equals(instance.getLoadBalancer().getOwnerUserId()))
						return false; // only for the requested user
					final LoadBalancer lb = LoadBalancerEntityTransform.INSTANCE.apply(instance.getLoadBalancer());
					final LoadBalancerBackendInstance be = LoadBalancers.lookupBackendInstance(lb, instance.getInstanceId());
					if(be==null)
						return false;
					if(be.getAvailabilityZone()==null || ! LoadBalancerZone.STATE.InService.equals(be.getAvailabilityZone().getState()))
						return false;
					
					return (LoadBalancerBackendInstance.STATE.InService.equals(be.getState())||
							LoadBalancerBackendInstance.STATE.OutOfService.equals(be.getState()));
				}catch(final Exception ex){
					return false;
				}
			}
    	}));
    	
    	synchronized(lock){
			/// add HealthyHostCount and UnHealthyHostCount
        	for(final BackendInstance instance : candidates){
        		final ElbDimension thisDim = this.instanceToDimensionMap.get(instance);
        		if(this.instanceHealthMap.get(instance).booleanValue()){ // healthy	
        			if(!healthyCountMap.containsKey(thisDim))
        				healthyCountMap.put(thisDim, 0);
        			healthyCountMap.put(thisDim, healthyCountMap.get(thisDim)+1);
        		}else{
        			if(!unhealthyCountMap.containsKey(thisDim))
        				unhealthyCountMap.put(thisDim,  0);
        			unhealthyCountMap.put(thisDim, unhealthyCountMap.get(thisDim)+1);
        		}
        	}
        	
			for (final ElbDimension dim : this.metricsMap.keySet()){
				if(!dim.getUserId().equals(userId))
					continue;
				
				final ElbAggregate aggr = this.metricsMap.get(dim);
				final  List<MetricDatum> datumList = aggr.toELBStatistics();
				
			 	Dimensions dims = new Dimensions();
	        	Dimension lb = new Dimension();
	        	lb.setName("LoadBalancerName");
	        	lb.setValue(dim.getLoadbalancer());
	        	Dimension az = new Dimension();
	        	az.setName("AvailabilityZone");
	        	az.setValue(dim.getAvailabilityZone());
	        	dims.setMember(Lists.newArrayList(lb, az));
	        		
				if(healthyCountMap.containsKey(dim)){
		        	int numHealthy = healthyCountMap.get(dim);
		        	if(numHealthy > 0){
						MetricDatum datum = new MetricDatum();
						datum.setDimensions(dims);
						datum.setMetricName("HealthyHostCount");
						datum.setUnit("Count");
			        	final StatisticSet sset = new StatisticSet();
			        	sset.setSampleCount(1.0);
			        	sset.setMaximum((double)numHealthy);
			        	sset.setMinimum((double)numHealthy);
			        	sset.setSum((double)numHealthy);
			        	datum.setStatisticValues(sset);
						datumList.add(datum);
		        	}
				}
				if(unhealthyCountMap.containsKey(dim)){
					int numUnhealthy = unhealthyCountMap.get(dim);
					if(numUnhealthy > 0){
						MetricDatum datum = new MetricDatum();
						datum.setDimensions(dims);
						datum.setMetricName("UnHealthyHostCount");
						datum.setUnit("Count");
			        	final StatisticSet sset = new StatisticSet();
			        	sset.setSampleCount(1.0);
			        	sset.setMaximum((double)numUnhealthy);
			        	sset.setMinimum((double)numUnhealthy);
			        	sset.setSum((double)numUnhealthy);
			        	datum.setStatisticValues(sset);
			        	datumList.add(datum);
					}
				}
	        	
				if(datumList.size()>0)
					data.getMember().addAll(datumList);
				
				toCleanup.add(dim);
			}
			
			for(final ElbDimension cleanup : toCleanup){
				this.metricsMap.remove(cleanup);
				healthyCountMap.remove(cleanup);
				unhealthyCountMap.remove(cleanup);
			}
			for(final BackendInstance instance : candidates){
				this.instanceHealthMap.remove(instance);
				this.instanceToDimensionMap.remove(instance);
			}
		}// end of lock
		
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
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.requestCount);
	        	sset.setMaximum(1.0);
	        	sset.setMinimum(1.0);
	        	sset.setSum((double)this.requestCount);
	        	reqCountData.setStatisticValues(sset);
	        	result.add(reqCountData);
        	}
        	if(this.httpCode_ELB_4XX>0){
	        	final MetricDatum httpCode_ELB_4XX = new MetricDatum();
	        	httpCode_ELB_4XX.setDimensions(dims);
	        	httpCode_ELB_4XX.setMetricName("HTTPCode_ELB_4XX");
	        	httpCode_ELB_4XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_ELB_4XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_ELB_4XX);
	        	httpCode_ELB_4XX.setStatisticValues(sset);
	        	result.add(httpCode_ELB_4XX);
        	}
        	if(this.httpCode_ELB_5XX>0){
	        	final MetricDatum httpCode_ELB_5XX = new MetricDatum();
	        	httpCode_ELB_5XX.setDimensions(dims);
	        	httpCode_ELB_5XX.setMetricName("HTTPCode_ELB_5XX");
	        	httpCode_ELB_5XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_ELB_5XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_ELB_5XX);
	        	httpCode_ELB_5XX.setStatisticValues(sset);
	        	result.add(httpCode_ELB_5XX);
        	}
        	if(this.httpCode_Backend_2XX>0){
	        	final MetricDatum httpCode_Backend_2XX = new MetricDatum();
	        	httpCode_Backend_2XX.setDimensions(dims);
	        	httpCode_Backend_2XX.setMetricName("HTTPCode_Backend_2XX");
	        	httpCode_Backend_2XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_Backend_2XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_Backend_2XX);
	        	httpCode_Backend_2XX.setStatisticValues(sset);
	        	result.add(httpCode_Backend_2XX);
        	}
        	if(this.httpCode_Backend_3XX>0){
	        	final MetricDatum httpCode_Backend_3XX = new MetricDatum();
	        	httpCode_Backend_3XX.setDimensions(dims);
	        	httpCode_Backend_3XX.setMetricName("HTTPCode_Backend_3XX");
	        	httpCode_Backend_3XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_Backend_3XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_Backend_3XX);
	        	httpCode_Backend_3XX.setStatisticValues(sset);	        	
	        	result.add(httpCode_Backend_3XX);
        	}
        	if(this.httpCode_Backend_4XX > 0){
	        	final MetricDatum httpCode_Backend_4XX = new MetricDatum();
	        	httpCode_Backend_4XX.setDimensions(dims);
	        	httpCode_Backend_4XX.setMetricName("HTTPCode_Backend_4XX");
	        	httpCode_Backend_4XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_Backend_4XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_Backend_4XX);
	        	httpCode_Backend_4XX.setStatisticValues(sset);
	        	result.add(httpCode_Backend_4XX);
        	}
        	if(this.httpCode_Backend_5XX > 0){
	        	final MetricDatum httpCode_Backend_5XX = new MetricDatum();
	        	httpCode_Backend_5XX.setDimensions(dims);
	        	httpCode_Backend_5XX.setMetricName("HTTPCode_Backend_5XX");
	        	httpCode_Backend_5XX.setUnit("Count");
	        	final StatisticSet sset = new StatisticSet();
	        	sset.setSampleCount((double)this.httpCode_Backend_5XX);
	        	sset.setMaximum(0.0);
	        	sset.setMinimum(0.0);
	        	sset.setSum((double)this.httpCode_Backend_5XX);
	        	httpCode_Backend_5XX.setStatisticValues(sset);
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
	
	private static class BackendInstance{
		private String instanceId = null;
		private LoadBalancerCoreView loadbalancer = null;
		private String userId = null;
		private String loadbalancerName = null;
		
		private BackendInstance(final LoadBalancerCoreView lb, final String instanceId){
			this.loadbalancer = lb;
			this.instanceId = instanceId;
			this.userId = lb.getOwnerUserId();
			this.loadbalancerName = lb.getDisplayName();
		}
		
		@Override 
		public boolean equals(Object obj){
			if(obj==null)
				return false;
			if(obj.getClass() != BackendInstance.class)
				return false;
			BackendInstance other = (BackendInstance) obj;
			
			if(this.userId == null){
				if(other.userId != null)
					return false;
			}else if(! this.userId.equals(other.userId))
				return false;
			
			if(this.loadbalancerName == null){
				if(other.loadbalancerName!=null)
					return false;
			}else if(! this.loadbalancerName.equals(other.loadbalancerName))
				return false;
		
			if(this.instanceId == null){
				if(other.instanceId !=null)
					return false;
			}else if(! this.instanceId.equals(other.instanceId))
				return false;
			
			return true;
		}
		
		public String getInstanceId(){
			return this.instanceId;
		}
		
		public LoadBalancerCoreView getLoadBalancer(){
			return this.loadbalancer;
		}
	
		@Override
		public int hashCode(){
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.userId == null) ? 0 : this.userId.hashCode());
			result = prime * result + ((this.loadbalancerName == null) ? 0 : this.loadbalancerName.hashCode());
			result = prime * result + ((this.instanceId == null) ? 0 : this.instanceId.hashCode());
			return result;
		}
		
		@Override
		public String toString(){
			return String.format("backend instance-%s-%s-%s", this.userId, this.loadbalancerName, this.instanceId);
		}
	}
	
	private static class ElbDimension{
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
