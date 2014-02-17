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
package com.eucalyptus.loadbalancing.activities;

import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupEntityTransform;
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
		this.insert(new RemoveDnsRecord(this));
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
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			//  check if the current zone contains the requested zone
			LoadBalancer lb = null;
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
		public void rollback() throws EventHandlerException {
			;	
		}
	} 
	
	// disabling zones will terminate instances.
	// should remove their IPs from dns service.
	private static class RemoveDnsRecord extends AbstractEventHandler<DisabledZoneEvent> {
		protected RemoveDnsRecord(EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		private String dnsName = null;
		private String dnsZone = null;
		private List<String> ipAddressRemoved = null;
		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}	
			
			final LoadBalancerDnsRecordCoreView dnsRec = lb.getDns();
			if(dnsRec == null){
				LOG.warn("failed to find the dns record for the loadbalancer");
				return;
			}
			this.dnsName = dnsRec.getName();
			this.dnsZone = dnsRec.getZone();
			final List<String> ipAddressToRemove = Lists.newArrayList();
			final List<LoadBalancerZoneCoreView> currentZones = Lists.newArrayList(lb.getZones());
			for(final LoadBalancerZoneCoreView zoneView : currentZones){
				if(evt.getZones().contains(zoneView.getName())){ // the zone will be disabled
					LoadBalancerZone zone = null;
					try{
						zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
					}catch(final Exception ex){
						LOG.error("unable to transform the zone from its view", ex);
						continue;
					}
					
					for(final LoadBalancerServoInstanceCoreView instance : zone.getServoInstances()){
						if(! LoadBalancerServoInstance.STATE.InService.equals(instance.getState()))
							continue;
						final String ipAddr = instance.getAddress();
						ipAddressToRemove.add(ipAddr);
					}
				}
			}
			this.ipAddressRemoved = Lists.newArrayList();
			for(final String ipAddr : ipAddressToRemove){
				try{
					EucalyptusActivityTasks.getInstance().removeARecord(dnsZone, dnsName, ipAddr);
					this.ipAddressRemoved.add(ipAddr);
				}catch(Exception ex){
					LOG.warn(String.format("failed to remove A record %s-%s-%s", dnsZone, dnsName, ipAddr));
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.dnsName!= null && this.dnsZone !=null && this.ipAddressRemoved!= null){
				for(final String ipAddr : ipAddressRemoved){
					try{
						EucalyptusActivityTasks.getInstance().addARecord(this.dnsZone, this.dnsName, ipAddr);
					}catch(final Exception ex){
						LOG.warn("failed to add A record back to the service", ex);
					}
				}
			}
		}
		
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
			LoadBalancer lb = null;
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
					LoadBalancerZone zone = null;
					try{
						zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
					}catch(final Exception ex){
						LOG.error("unable to transform zone from the view", ex);
						continue;
					}
					for(final LoadBalancerServoInstanceCoreView instanceView : zone.getServoInstances()){
						LoadBalancerServoInstance instance = null;
						try{
							instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
						}catch(final Exception ex){
							LOG.error("unable to transfrom servo-instance from the view", ex);
							continue;
						}
						
						final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
						try{
							final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
							update.setState(LoadBalancerServoInstance.STATE.Retired);
							Entities.persist(update);
							db.commit();
							retiredInstances.add(update);
						}catch(final NoSuchElementException ex){
							db.rollback();
							LOG.warn("Failed to update the servo instance's state: no such instance found");
						}catch(final Exception ex){
							db.rollback();
							LOG.warn("Failed to update the servo instance's state", ex);
						}finally {
							if(db.isActive())
								db.rollback();
						}
					}
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.retiredInstances!=null){
				for(final LoadBalancerServoInstance instance : retiredInstances){
					final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
					try{
						final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
						update.setState(LoadBalancerServoInstance.STATE.InService);
						Entities.persist(update);
						db.commit();
					}catch(final NoSuchElementException ex){
						db.rollback();
						LOG.warn("Failed to update the servo instance's state: no such instance found");
					}catch(final Exception ex){
						db.rollback();
						LOG.warn("Failed to update the servo instance's state", ex);
					}finally {
						if(db.isActive())
							db.rollback();
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
		
		private LoadBalancerAutoScalingGroupCoreView group = null;
		private String groupName = null;
		private List<String> beforeUpdate = null;
		private List<String> afterUpdate = null;

		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			
			group = lb.getAutoScaleGroup();
			if(group==null) {
				LOG.warn(String.format("No autoscaling group found for %s-%s", evt.getContext().getUserFullName(), evt.getLoadBalancer()));
				return;
			}else{
				this.groupName = group.getName();
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
				
				final List<String> updatedZones = Lists.newArrayList();
				updatedZones.addAll(availableZones);
				
				for (final String req : evt.getZones()){
					updatedZones.remove(req);
				}
				
				try{
					int capacityPerZone = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
					if(capacityPerZone <= 0)
						capacityPerZone = 1;
					final int newCapacity = capacityPerZone * updatedZones.size();
					
					EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(this.groupName, updatedZones, newCapacity);
					this.beforeUpdate = availableZones;
					this.afterUpdate = updatedZones;
					
					LoadBalancerAutoScalingGroup scaleGroup = null;
					try{
						scaleGroup = LoadBalancerAutoScalingGroupEntityTransform.INSTANCE.apply(this.group);
					}catch(final Exception ex){
						LOG.error("unable to transform autoscale group from the view", ex);
						throw ex;
					}
					
					final EntityTransaction db = Entities.get( LoadBalancerAutoScalingGroup.class );
					try{
						final LoadBalancerAutoScalingGroup update = Entities.uniqueResult(scaleGroup);
						update.setCapacity(newCapacity);
						Entities.persist(update);
						db.commit();
					}catch(NoSuchElementException ex){
						db.rollback();
						LOG.error("failed to find the autoscaling group record", ex);
					}catch(Exception ex){
						db.rollback();
						LOG.error("failed to update the autoscaling group record", ex);
					}finally {
						if(db.isActive())
							db.rollback();
					}
				}catch(final Exception ex){
					throw new EventHandlerException("failed to update the autoscaling group", ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.groupName!=null && this.beforeUpdate != null){
				try{
					int capacityPerZone = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
					if(capacityPerZone <= 0)
						capacityPerZone = 1;
					final int oldCapacity = capacityPerZone * this.beforeUpdate.size();
				
					EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(this.groupName, this.beforeUpdate, oldCapacity);
					
					LoadBalancerAutoScalingGroup scaleGroup = null;
					try{
						scaleGroup = LoadBalancerAutoScalingGroupEntityTransform.INSTANCE.apply(this.group);
					}catch(final Exception ex){
						LOG.error("unable to transfrom scaling group from the view", ex);
						throw ex;
					}
					
					final EntityTransaction db = Entities.get( LoadBalancerAutoScalingGroup.class );
					try{
						final LoadBalancerAutoScalingGroup update = Entities.uniqueResult(scaleGroup);
						update.setCapacity(oldCapacity);
						Entities.persist(update);
						db.commit();
					}catch(NoSuchElementException ex){
						db.rollback();
						LOG.error("failed to find the autoscaling group record", ex);
					}catch(Exception ex){
						db.rollback();
						LOG.error("failed to update the autoscaling group record", ex);
					}finally {
						if(db.isActive())
							db.rollback();
					}
				}catch(Exception ex){
					throw new EventHandlerException("failed to update the zone to the original list", ex);
				}
			}
		}

		@Override
		public List<String> getResult() {
			// the list of zones that's removed
			final List<String> removed = Lists.newArrayList(
					Sets.difference(	
							Sets.newHashSet(this.beforeUpdate),
							Sets.newHashSet(this.afterUpdate))
							);
			return removed;
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
			LoadBalancer lb = null;
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
					final EntityTransaction db = Entities.get( LoadBalancerZone.class );
					try{
						final LoadBalancerZone update = Entities.uniqueResult(LoadBalancerZone.named(lb, removedZone));
						update.setState(LoadBalancerZone.STATE.OutOfService);
						Entities.persist(update);
						db.commit();
					}catch(final NoSuchElementException ex){
						db.rollback();
					}catch(final Exception ex){
						db.rollback();
					}finally {
						if(db.isActive())
							db.rollback();
					}
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	private static class PersistBackendInstanceState extends AbstractEventHandler<DisabledZoneEvent>  {

		protected PersistBackendInstanceState(
				EventHandlerChain<DisabledZoneEvent> chain) {
			super(chain);
		}

		private List<LoadBalancerBackendInstance> updatedInstances = null;
		@Override
		public void apply(DisabledZoneEvent evt) throws EventHandlerException {
			LoadBalancer lb = null;
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
							final EntityTransaction db = Entities.get( LoadBalancerBackendInstance.class );
							try{
								final LoadBalancerBackendInstance update = Entities.uniqueResult(
										LoadBalancerBackendInstance.named(lb, instance.getInstanceId()));
								update.setState(LoadBalancerBackendInstance.STATE.OutOfService);
								update.setReasonCode("ELB");
								update.setDescription("Zone disabled");
								Entities.persist(update);
								db.commit();
								this.updatedInstances.add(update);
							}catch(final NoSuchElementException ex){
								db.rollback();
								LOG.warn("failed to find the backend instance");
							}catch(final Exception ex){
								db.rollback();
								LOG.warn("failed to query the backend instance", ex);
							}finally {
								if(db.isActive())
									db.rollback();
							}
						}
					}
				}catch(final Exception ex){
					LOG.warn("failed to update the backend instance's state to OutOfService", ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			; // if zone is still InService, the backend instances will automatically transit to InService
		}
		
	}
}
