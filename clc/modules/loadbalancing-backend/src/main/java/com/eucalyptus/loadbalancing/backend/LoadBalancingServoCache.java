/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.backend;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park
 * 
 * EUCA-11046: to address scale limit due to frequent pollings by servo VM, DB query is 
 * replaced by in-memory caching. Cached response is invalidated when there is any changes to the ELB.
 */
@ConfigurableClass(root = "services.loadbalancing.worker", description = "Parameters controlling loadbalancing")
public class LoadBalancingServoCache {
  private static Logger    LOG     = Logger.getLogger( LoadBalancingServoCache.class );

  private LoadBalancingServoCache() { }
  private static LoadBalancingServoCache _instance = null;
  public static synchronized LoadBalancingServoCache getInstance() {
    if (_instance == null) {
      _instance = new LoadBalancingServoCache();
    }
    if ( cachedEntities == null ) {
      cachedEntities =   CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(getCacheDuration(), TimeUnit.MINUTES)
          .build(new CacheLoader<String, CachedEntities> () {
            @Override
            public CachedEntities load(String instanceId) throws Exception {
              return CachedEntities.loadEntities(instanceId);
            }
          });
    }
    return _instance;
  }

  private static LoadingCache<String, CachedEntities> cachedEntities = null;
  private static ConcurrentMap<String, Long> recordVersion =
      Maps.newConcurrentMap();
  
  @ConfigurableField( displayName = "cache_duration",
      description = "duration of cached data delivered to workers",
      initial = "3", // 3 minutes 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbCacheDurationChangeListener.class)
  public static String CACHE_DURATION = "3";
  

  @ConfigurableField( displayName = "lb_poll_interval",
      description = "interval for distributing ELB data to haproxy VM",
      initial = "10", // 10 seconds 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbPollingIntervalChangeListener.class)
  public static String LB_POLL_INTERVAL = "10";
  
  @ConfigurableField( displayName = "cw_put_interval",
      description = "interval for updating CW metrics",
      initial = "10", // 10 seconds 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbCWPutIntervalChangeListener.class)
  public static String CW_PUT_INTERVAL = "10";

  @ConfigurableField( displayName = "backend_instance_update_interval",
      description = "interval for updating backend instance state",
      initial = "60", // 10 seconds 
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE,
      changeListener = ElbBackendInstanceUpdateIntervalChangeListener.class)
  public static String BACKEND_INSTANCE_UPDATE_INTERVAL = "60";
  
  private static Integer getCacheDuration(){
    try{
      return Integer.parseInt((String) CACHE_DURATION);
    }catch(final Exception ex) {
      return 3;
    }
  }
  
  public static class ElbPollingIntervalChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          final Integer newInterval = Integer.parseInt((String) newValue);
          if(newInterval < 10)
            throw new Exception("Interval must be bigger than 10 sec");
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not update ELB polling interval due to: " + e.getMessage());
      }
    }
  }
  
  public static class ElbCWPutIntervalChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          final Integer newInterval = Integer.parseInt((String) newValue);
          if(newInterval < 10)
            throw new Exception("Interval must be bigger than 10 sec");
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not update CW putMetric interval due to: " + e.getMessage());
      }
    }
  }

  public static class ElbBackendInstanceUpdateIntervalChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          final Integer newInterval = Integer.parseInt((String) newValue);
          if(newInterval < 10)
            throw new Exception("Interval must be bigger than 10 sec");
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not update ELB backend instance update interval due to: " + e.getMessage());
      }
    }
  }
  
  public static class ElbCacheDurationChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try {
        if ( newValue instanceof String  ) {
          final Integer newDuration = Integer.parseInt((String) newValue);
          if(newDuration < 1)
            throw new Exception("Duration must be larger than 0");
          if(newDuration == Integer.parseInt((String)t.getValue()))
            return;
          if (cachedEntities != null) {
            cachedEntities.cleanUp();
            cachedEntities.invalidateAll();
            cachedEntities = null;
          }
        }
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException("Could not update cache dueration due to: " + e.getMessage());
      }
    }
  }

  private static class CachedEntities {
    private LoadBalancer lb = null;
    private LoadBalancerZone zone = null;
    private LoadBalancerServoInstance servo = null;
    private List<LoadBalancerListener> listeners = null;
    private List<LoadBalancerBackendInstance> backendInstances = null;
    private List<LoadBalancerPolicyDescription> policyDescriptions = null;
    private List<LoadBalancerBackendServerDescription> backendServerDescs = null;
    
    private CachedEntities(){ }
    
    private LoadBalancer getLoadBalancer(){
      return lb;
    }
    private LoadBalancerZone getZone(){
      return zone;
    }
    private LoadBalancerServoInstance getServo(){
      return this.servo;
    }
    private List<LoadBalancerListener> getListeners(){
      return this.listeners;
    }
    private List<LoadBalancerBackendInstance> getBackendInstances(){
      return this.backendInstances;
    }
    private List<LoadBalancerPolicyDescription> getPolicyDescriptions(){
      return this.policyDescriptions;
    }
    private List<LoadBalancerBackendServerDescription> getBackendServerDescriptions(){
      return this.backendServerDescs;
    }
    private static CachedEntities loadEntities(final String servoInstanceId) throws Exception{
      recordVersion.putIfAbsent(servoInstanceId, 0L);
      final Long enteringVersion = recordVersion.get(servoInstanceId);
      
      final LoadBalancerServoInstance instance = LoadBalancers.lookupServoInstance(servoInstanceId);
      final LoadBalancerZoneCoreView zoneView = instance.getAvailabilityZone();
      final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
      final LoadBalancerCoreView lbView = zone.getLoadbalancer();
      final LoadBalancer lb = LoadBalancerEntityTransform.INSTANCE.apply(lbView);
      final List<LoadBalancerListener> listeners = Lists.newArrayList(Collections2.transform(lb.getListeners(), 
          new Function<LoadBalancerListenerCoreView, LoadBalancerListener>(){
            @Override
            public LoadBalancerListener apply(LoadBalancerListenerCoreView arg0) {
              return LoadBalancerListenerEntityTransform.INSTANCE.apply(arg0);
            }
      }));
      
      final List<LoadBalancerBackendInstance> backendInstances = Lists.newArrayList(Collections2.transform(lb.getBackendInstances(), 
          new Function<LoadBalancerBackendInstanceCoreView, LoadBalancerBackendInstance>(){
            @Override
            public LoadBalancerBackendInstance apply(
                LoadBalancerBackendInstanceCoreView arg0) {
              return LoadBalancerBackendInstanceEntityTransform.INSTANCE.apply(arg0);
            }
      }));
      final List<LoadBalancerPolicyDescription> policyDescriptions = Lists.newArrayList(Collections2.transform(lb.getPolicies(), 
          new Function<LoadBalancerPolicyDescriptionCoreView, LoadBalancerPolicyDescription>(){
            @Override
            public LoadBalancerPolicyDescription apply(
                LoadBalancerPolicyDescriptionCoreView arg0) {
              return LoadBalancerPolicyDescriptionEntityTransform.INSTANCE.apply(arg0);
            }
        
      }));
      final List<LoadBalancerBackendServerDescription> backendServers = Lists.newArrayList(Collections2.transform(lb.getBackendServers(), 
          new Function<LoadBalancerBackendServerDescriptionCoreView, LoadBalancerBackendServerDescription>(){
            @Override
            public LoadBalancerBackendServerDescription apply(
                LoadBalancerBackendServerDescriptionCoreView arg0) {
              return LoadBalancerBackendServerDescriptionEntityTransform.INSTANCE.apply(arg0);
            }
      }));
      
      
      final CachedEntities newEntities = new CachedEntities();
      newEntities.lb = lb;
      newEntities.zone = zone;
      newEntities.servo = instance;
      newEntities.listeners = listeners;
      newEntities.backendInstances = backendInstances;
      newEntities.policyDescriptions = policyDescriptions;
      newEntities.backendServerDescs = backendServers;
      
      final Long exitVersion = recordVersion.get(servoInstanceId);
      if (exitVersion.longValue() != enteringVersion.longValue()) {
        // in case cache is invalidated while it was read from DB
        return loadEntities(servoInstanceId);
      } else {
        return newEntities; 
      }
    }
  }

  public LoadBalancer getLoadBalancer(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getLoadBalancer();
  }

  public LoadBalancerZone getLoadBalancerZone(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getZone();
  }

  public LoadBalancerServoInstance getLoadBalancerServoInstance(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getServo();
  }

  public List<LoadBalancerListener> getLoadBalancerListeners(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getListeners();
  }
  
  public List<LoadBalancerBackendInstance> getLoadBalancerBackendInstances(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getBackendInstances();
  }
  
  public List<LoadBalancerPolicyDescription> getLoadBalancerPolicyDescriptions(final String servoInstanceId) throws Exception {
    return cachedEntities.get(servoInstanceId).getPolicyDescriptions();
  }

  public List<LoadBalancerBackendServerDescription> getLoadBalancerBackendServerDescriptions(final String servoInstanceId ) throws Exception {
    return cachedEntities.get(servoInstanceId).getBackendServerDescriptions();
  }
  
  private synchronized void invalidate(final String servoInstanceId) {
    cachedEntities.invalidate(servoInstanceId);
    recordVersion.putIfAbsent(servoInstanceId, 0L);
    Long version = recordVersion.get(servoInstanceId);
    recordVersion.put(servoInstanceId, ++version % Long.MAX_VALUE);
  }
  
  // invalidate all caches related to the ELB
  public void invalidate(final LoadBalancer lb) {
    try{
      final Collection<LoadBalancerZoneCoreView> zoneViews = lb.getZones();
      for(final LoadBalancerZoneCoreView zoneView : zoneViews) {
        final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
        for(final LoadBalancerServoInstanceCoreView servoView : zone.getServoInstances()) {
          invalidate(servoView.getInstanceId());
        }
      }
    }catch(final NoSuchElementException ex) {
      ;
    }catch(final Exception ex) {
      LOG.warn("Failed invalidating caches", ex);
    }
  }
 
  // invalidate the cache for servo instance
  public void invalidate(final LoadBalancerServoInstance instance) {
    try{
      invalidate(instance.getInstanceId());
    }catch(final Exception ex){
      LOG.warn("Failed invalidating caches", ex);
    }
  }
  
  public void invalidate(final LoadBalancerBackendInstance instance) {
    try{
      final LoadBalancer lb = LoadBalancerEntityTransform.INSTANCE.apply(instance.getLoadBalancer());
      invalidate(lb);
    }catch(final NoSuchElementException ex) {
      ;
    }catch(final Exception ex){
      LOG.warn("Failed invalidating caches", ex);
    }
  } 

  public boolean replaceBackendInstance(final String servoInstanceId, final LoadBalancerBackendInstance oldInstance, 
      final LoadBalancerBackendInstance newInstance) throws Exception {
    final CachedEntities entities = cachedEntities.get(servoInstanceId);
    final List<LoadBalancerBackendInstance> backendInstances =  entities.getBackendInstances();
    if(backendInstances.remove(oldInstance))
      return backendInstances.add(newInstance);
    return false;
  }
}
