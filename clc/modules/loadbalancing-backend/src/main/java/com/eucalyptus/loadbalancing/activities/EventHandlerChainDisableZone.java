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
package com.eucalyptus.loadbalancing.activities;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancers.DeploymentVersion;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainDisableZone extends EventHandlerChain<DisabledZoneEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainEnableZone.class );

	@Override
	public EventHandlerChain<DisabledZoneEvent> build() {
		this.insert(new CheckAndModifyRequest(this));
		this.insert(new PersistUpdatedServoInstances(this));
		this.insert(new UpdateAutoScalingGroup(this));
		this.insert(new PersistUpdatedZones(this));
		this.insert(new PersistBackendInstanceState(this));
		return this;
	}

	private static class CheckAndModifyRequest extends  AbstractEventHandler<DisabledZoneEvent> {
		protected CheckAndModifyRequest(EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		@Override
		public void checkVersion(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}

			if(!LoadBalancers.v4_2_0.apply(lb))
				throw new LoadBalancerVersionException(DeploymentVersion.v4_2_0);
			if(!LoadBalancers.v4_3_0.apply(lb) && lb.getVpcId()!= null)
				throw new LoadBalancerVersionException(DeploymentVersion.v4_3_0);
		}

		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			//  check if the current zone contains the requested zone
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}	

			final List<LoadBalancerZoneCoreView> currentZones = 
					Lists.newArrayList(Collections2.filter(lb.getZones(), new Predicate<LoadBalancerZoneCoreView>(){
						@Override
						public boolean apply(@Nullable LoadBalancerZoneCoreView arg0) {
							return arg0.getState().equals(LoadBalancerZone.STATE.InService);
						}
			}));
			
			final List<String> availableZones = Lists.newArrayList(Collections2.transform(currentZones, 
					new Function<LoadBalancerZoneCoreView, String>(){
				@Override
				public String apply(@Nullable LoadBalancerZoneCoreView arg0) {
					return arg0.getName();
				}
			}));
			
			final List<String> unknownZones = Lists.newArrayList();
			for(final String requested : evt.getZones()){
				if(! availableZones.contains(requested)){
					unknownZones.add(requested);
				}
			}
			// remove the requested zones not already in the loadbalancer
			for(final String unknown : unknownZones){
				evt.getZones().remove(unknown);
			}
		}

		@Override
		public void rollback() { }
	} 
	
	// disabling zones will terminate the instances.
	// mark their state 'Retired'. ServoInstanceCleanup will clean up the record.
	private static class PersistUpdatedServoInstances extends AbstractEventHandler<DisabledZoneEvent> {

		protected PersistUpdatedServoInstances(
				EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		private List<LoadBalancerServoInstance> retiredInstances = null;
		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}	
			
			retiredInstances = Lists.newArrayList();
			final List<LoadBalancerZoneCoreView> currentZones = Lists.newArrayList(lb.getZones());
			for(final LoadBalancerZoneCoreView zoneView : currentZones){
				if(evt.getZones().contains(zoneView.getName())){ // the zone will be disabled
					LoadBalancerZone zone;
					try{
						zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
					}catch(final Exception ex){
						LOG.error("unable to transform zone from the view", ex);
						continue;
					}
					for(final LoadBalancerServoInstanceCoreView instanceView : zone.getServoInstances()){
						LoadBalancerServoInstance instance;
						try{
							instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
						}catch(final Exception ex){
							LOG.error("unable to transfrom servo-instance from the view", ex);
							continue;
						}
						
						try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
							final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
							update.setState(LoadBalancerServoInstance.STATE.Retired);
							update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
							Entities.persist(update);
							db.commit();
							retiredInstances.add(update);
						}catch(final NoSuchElementException ex){
							LOG.warn("Failed to update the servo instance's state: no such instance found");
						}catch(final Exception ex){
							LOG.warn("Failed to update the servo instance's state", ex);
						}
					}
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.retiredInstances!=null){
				for(final LoadBalancerServoInstance instance : retiredInstances){
					try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
						final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
						update.setState(LoadBalancerServoInstance.STATE.InService);
						Entities.persist(update);
						db.commit();
					}catch(final NoSuchElementException ex){
						LOG.warn("Failed to update the servo instance's state: no such instance found");
					}catch(final Exception ex){
						LOG.warn("Failed to update the servo instance's state", ex);
					}
				}
			}
		}
	}
	
	private static class UpdateAutoScalingGroup 
		extends AbstractEventHandler<DisabledZoneEvent> implements StoredResult<String>{

		protected UpdateAutoScalingGroup(
				EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}
		
		private List<String> updatedZones = Lists.newArrayList();
		private DisabledZoneEvent event = null;
		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
		  this.event = evt;
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			
			final Set<String> zonesToDisable = Sets.newHashSet(evt.getZones());
			final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
			
			for(final LoadBalancerAutoScalingGroupCoreView group : groups) {
			  if (! zonesToDisable.contains(group.getAvailabilityZone()))
			      continue;

			  final String groupName = group.getName();
			  final int capacity = 0;
			  try{
			    EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, capacity, 
			        lb.useSystemAccount());
			  }catch(final Exception ex) {
			    LOG.error("Failed to change the capacity of ELB's autoscaling group", ex);
			  }
			  
			  try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ){
			    final LoadBalancerAutoScalingGroup update = 
			        Entities.uniqueResult(LoadBalancerAutoScalingGroup.named(lb, group.getAvailabilityZone()));
          update.setCapacity(capacity);
          Entities.persist(update);
          db.commit();
        }catch(NoSuchElementException ex){
          LOG.error("failed to find the autoscaling group record", ex);
        }catch(Exception ex){
          LOG.error("failed to update the autoscaling group record", ex);
        }
			  
			  updatedZones.add(group.getAvailabilityZone());
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
		  if(this.event == null)
		    return;
		  
		  LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer(event.getContext(), event.getLoadBalancer());
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Could not find the loadbalancer with name="+event.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Error while looking for loadbalancer with name="+event.getLoadBalancer(), ex);
      }
      
      final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
      
      for(final LoadBalancerAutoScalingGroupCoreView group : groups) {
        if (! updatedZones.contains(group.getAvailabilityZone()))
            continue;

        final String groupName = group.getName();
        final int capacity =  EventHandlerChainNew.getCapacityPerZone();
        try{
          EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, capacity, 
              lb.useSystemAccount());
        }catch(final Exception ex) {
          LOG.error("Failed to change the capacity of ELB's autoscaling group", ex);
        }
        
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ){
          final LoadBalancerAutoScalingGroup update = 
              Entities.uniqueResult(LoadBalancerAutoScalingGroup.named(lb, group.getAvailabilityZone()));
          update.setCapacity(capacity);
          Entities.persist(update);
          db.commit();
        }catch(NoSuchElementException ex){
          LOG.error("failed to find the autoscaling group record", ex);
        }catch(Exception ex){
          LOG.error("failed to update the autoscaling group record", ex);
        }
      }
		}

		@Override
		public List<String> getResult() {
			// the list of zones that's removed
			return updatedZones;
		}
	}
	
	/// mark the removed zones (from ASG) as OutOfService
	private static class PersistUpdatedZones extends AbstractEventHandler<DisabledZoneEvent> {
		protected PersistUpdatedZones(
				EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(DisabledZoneEvent evt)
				throws EventHandlerException {
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			
			final StoredResult<String> updated = 
					this.getChain().findHandler(UpdateAutoScalingGroup.class);
			if(updated!= null && updated.getResult()!= null){
				for(final String removedZone : updated.getResult()){
					try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
						final LoadBalancerZone update = Entities.uniqueResult(LoadBalancerZone.named(lb, removedZone));
						update.setState( LoadBalancerZone.STATE.OutOfService );
						db.commit();
					}catch(final Exception ex){
						LOG.debug( "Error updating state for load balancer zone", ex );
					}
				}
			}
		}

		@Override
		public void rollback() { }
	}
	
	private static class PersistBackendInstanceState extends AbstractEventHandler<DisabledZoneEvent>  {

		protected PersistBackendInstanceState(
				EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		private List<LoadBalancerBackendInstance> updatedInstances = null;
		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			
			final StoredResult<String> updated = 
					this.getChain().findHandler(UpdateAutoScalingGroup.class);
			if(updated!= null && updated.getResult()!= null){
				try{
					this.updatedInstances = Lists.newArrayList();
					for(final String removedZone : updated.getResult()){
						final LoadBalancerZone zone = LoadBalancers.findZone(lb, removedZone);
						for(final LoadBalancerBackendInstanceCoreView instance : zone.getBackendInstances()){
							try ( TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ){
								final LoadBalancerBackendInstance update = Entities.uniqueResult(
										LoadBalancerBackendInstance.named(lb, instance.getInstanceId()));
								update.setState(LoadBalancerBackendInstance.STATE.OutOfService);
								update.setReasonCode("ELB");
								update.setDescription("Zone disabled");
								Entities.persist(update);
								db.commit();
								this.updatedInstances.add(update);
							}catch(final NoSuchElementException ex){
								LOG.warn("failed to find the backend instance");
							}catch(final Exception ex){
								LOG.warn("failed to query the backend instance", ex);
							}
						}
					}
				}catch(final Exception ex){
					LOG.warn("failed to update the backend instance's state to OutOfService", ex);
				}
			}
		}

		@Override
		public void rollback() {
			// if zone is still InService, the backend instances will automatically transit to InService
		}
		
	}
}
