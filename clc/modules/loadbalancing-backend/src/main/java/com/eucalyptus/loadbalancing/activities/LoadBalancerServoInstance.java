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
package com.eucalyptus.loadbalancing.activities;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.backend.LoadBalancingServoCache;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_servo_instance" )
public class LoadBalancerServoInstance extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerServoInstance.class );
	@Transient
	private static final long serialVersionUID = 1L;
	
	@Transient
	private LoadBalancerServoInstanceRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerServoInstanceRelationView(this);
	}
	
	enum STATE {
		Pending, InService, Error, OutOfService, Retired
	}
	enum DNS_STATE {
		Registered, Deregistered, None
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_zone_fk", nullable=true)
    private LoadBalancerZone zone = null;
    
    @ManyToOne
    @JoinColumn( name = "metadata_group_fk", nullable=true)
    private LoadBalancerSecurityGroup security_group = null;

    @ManyToOne
    @JoinColumn( name = "metadata_asg_fk", nullable=true)
    private LoadBalancerAutoScalingGroup autoscaling_group = null; 
       
    @Transient
    private LoadBalancer loadbalancer = null;
    
    @Column(name="metadata_instance_id", nullable=false, unique=true)
    private String instanceId = null;
    
    @Column(name="metadata_state", nullable=false)
    private String state = null;
    
    @Column(name="metadata_address", nullable=true)
    private String address = null;

    @Column(name="metadata_private_ip", nullable=true)
    private String privateIp = null;
    
    @Column(name="metadata_dns_state", nullable=true)
    private String dnsState = null;
    
    private LoadBalancerServoInstance(){
    }
    
    private LoadBalancerServoInstance(final LoadBalancerZone lbzone){
    	this.state = STATE.Pending.name();
    	this.zone = lbzone;
    	try{
	    	this.loadbalancer = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
    	}catch(final Exception ex){
    		throw Exceptions.toUndeclared(ex);
    	}
    }
    private LoadBalancerServoInstance(final LoadBalancerZone lbzone, final LoadBalancerSecurityGroup group){
    	this.state = STATE.Pending.name();
    	this.zone = lbzone;
    	try{
	    	this.loadbalancer = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
    	}catch(final Exception ex){
    		throw Exceptions.toUndeclared(ex);
    	}
    	this.security_group = group;
    }
    
    public static LoadBalancerServoInstance newInstance(final LoadBalancerZone lbzone, final LoadBalancerSecurityGroup group, final LoadBalancerAutoScalingGroup as_group, String instanceId)
    {
    	final LoadBalancerServoInstance instance = new LoadBalancerServoInstance(lbzone, group);
    	instance.setInstanceId(instanceId);
    	instance.setAutoScalingGroup(as_group);
    	instance.dnsState = DNS_STATE.None.name();

    	return instance;
    }
    
    public static LoadBalancerServoInstance named(final LoadBalancerZone lbzone){
    	final LoadBalancerServoInstance sample = new LoadBalancerServoInstance(lbzone);
    	return sample;
    }
    
    public static LoadBalancerServoInstance named(String instanceId){
    	final LoadBalancerServoInstance sample = new LoadBalancerServoInstance();
    	sample.instanceId = instanceId;
    	return sample;
    }
    
    public static LoadBalancerServoInstance named(){
    	return new LoadBalancerServoInstance();
    }
    
    public static LoadBalancerServoInstance withState(String state){
    	final LoadBalancerServoInstance sample = new LoadBalancerServoInstance();
    	sample.state = state;
    	return sample;
    }
    
    public void setInstanceId(String id){
    	this.instanceId = id;
    }
    
    public String getInstanceId(){
    	return this.instanceId;
    }
    
    public void setState(STATE update){
    	this.state = update.name();
    }
    
    public STATE getState(){
    	return Enum.valueOf(STATE.class, this.state);
    }
    
    public void setAddress(String address){
    	this.address=address;
    }
    
    public String getAddress(){
    	return this.address; 
    }
    
    public void setSecurityGroup(LoadBalancerSecurityGroup group){
    	this.security_group=group;
    }
    
    public void setAutoScalingGroup(LoadBalancerAutoScalingGroup group){
    	this.autoscaling_group = group;
    }
    
    public void setAvailabilityZone(LoadBalancerZone zone){
    	this.zone = zone;
    }
    
    public LoadBalancerZoneCoreView getAvailabilityZone(){
    	return this.view.getZone();
    }

	public LoadBalancerAutoScalingGroupCoreView getAutoScalingGroup() { return this.view.getAutoScalingGroup(); }

    public String getPrivateIp(){
    	return this.privateIp;
    }
    
    public void setPrivateIp(final String ipAddr){
    	this.privateIp = ipAddr;
    }
    
    public void setDnsState(final DNS_STATE dnsState){
    	this.dnsState = dnsState.toString();
    }
    
    public DNS_STATE getDnsState(){
    	return Enum.valueOf(DNS_STATE.class, this.dnsState);
    }
    
	@Override
	public String toString(){
		String id = this.instanceId==null? "unassigned" : this.instanceId;
		return String.format("Servo-instance (%s) for %s", id, this.zone.getName());
	}
	
	public static class LoadBalancerServoInstanceCoreView {
		private LoadBalancerServoInstance entity = null;
		LoadBalancerServoInstanceCoreView(final LoadBalancerServoInstance servo){
			this.entity = servo;
		}

		public String getInstanceId(){
			return this.entity.getInstanceId();
		}

		public STATE getState(){
				return this.entity.getState();
			}

		public String getAddress(){
			return this.entity. getAddress();
		}

		public String getPrivateIp(){
				return this.entity.getPrivateIp();
		}

    public boolean canResolveDns(){
      return DNS_STATE.Registered.equals(this.entity.getDnsState()) && 
            STATE.InService.equals(this.entity.getState());
    }

		public static Function<LoadBalancerServoInstanceCoreView,String> address( ) {
			return StringFunctions.ADDRESS;
		}

		public static Function<LoadBalancerServoInstanceCoreView,String> privateIp( ) {
			return StringFunctions.PRIVATE_IP;
		}
		
		private enum StringFunctions implements Function<LoadBalancerServoInstanceCoreView,String>{
			ADDRESS {
				@Nullable
				@Override
				public String apply( @Nullable final LoadBalancerServoInstanceCoreView loadBalancerServoInstanceCoreView ) {
					return loadBalancerServoInstanceCoreView.getAddress( );
				}
			},
			PRIVATE_IP {
				@Nullable
				@Override
				public String apply( @Nullable final LoadBalancerServoInstanceCoreView loadBalancerServoInstanceCoreView ) {
					return loadBalancerServoInstanceCoreView.getPrivateIp( );
				}
			},
			;
		}
	}

	@TypeMapper
	public enum LoadBalancerServoInstanceCoreViewTransform implements Function<LoadBalancerServoInstance, LoadBalancerServoInstanceCoreView> {
		INSTANCE;
	
		@Override
		public LoadBalancerServoInstanceCoreView apply( final LoadBalancerServoInstance servo ) {
			return new LoadBalancerServoInstanceCoreView( servo );
		}
	}
	
	public enum LoadBalancerServoInstanceEntityTransform implements Function<LoadBalancerServoInstanceCoreView, LoadBalancerServoInstance> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerServoInstance apply(
				@Nullable LoadBalancerServoInstanceCoreView arg0) {
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
				return Entities.uniqueResult(arg0.entity);
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	public static class LoadBalancerServoInstanceRelationView {
	    private LoadBalancerZoneCoreView zone = null;
	    private LoadBalancerAutoScalingGroupCoreView autoscaling_group = null;
	    
		private LoadBalancerServoInstanceRelationView(final LoadBalancerServoInstance servo){
			if(servo.zone!=null)
				this.zone = TypeMappers.transform(servo.zone, LoadBalancerZoneCoreView.class);
			if(servo.autoscaling_group!=null)
				this.autoscaling_group = TypeMappers.transform(servo.autoscaling_group, LoadBalancerAutoScalingGroupCoreView.class);
		}
		
		public LoadBalancerZoneCoreView getZone(){
			return this.zone;
		}

		public LoadBalancerAutoScalingGroupCoreView getAutoScalingGroup() { return this.autoscaling_group; }
	}
	
	// make sure  InService servo instance has its IP registered to DNS
	// also make sure Error or OutOfService servo instance has its IP deregistered from DNS
	public static class ServoInstanceDnsCheck implements EventListener<ClockTick> {
		static final int CHECK_EVERY_SECONDS = 60;
		static Date lastCheckTime = new Date(System.currentTimeMillis());
		public static void register( ) {
		      Listeners.register( ClockTick.class, new ServoInstanceDnsCheck() );
		}

		@Override
		public void fireEvent(ClockTick event) {
			if (!( Bootstrap.isFinished() &&
			          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
			          Topology.isEnabled( Compute.class ) ))
				return;

			final Date now = new Date(System.currentTimeMillis());
			final int elapsedSec =  (int)((now.getTime() - lastCheckTime.getTime())/1000.0);
			if(elapsedSec < CHECK_EVERY_SECONDS)
			  return;
			lastCheckTime = now;

	   /// determine the servo instances to query
			final List<LoadBalancerServoInstance> allInstances = Lists.newArrayList();
			//final List<LoadBalancerServoInstance> stateOutdated = Lists.newArrayList();
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
				allInstances.addAll(
						Entities.query(LoadBalancerServoInstance.named()));
			}catch(final Exception ex){
			}
			
			final List<LoadBalancerServoInstance> stateOutdated = allInstances;
			for(final LoadBalancerServoInstance instance : stateOutdated){
				if(LoadBalancerServoInstance.STATE.InService.equals(instance.getState())){
					if(!LoadBalancerServoInstance.DNS_STATE.Registered.equals(instance.getDnsState())){
						String ipAddr = null;
						String privateIpAddr = null;
						if (LoadBalancingSystemVpcs.isCloudVpc().isPresent()&&
								LoadBalancingSystemVpcs.isCloudVpc().get()) { /// in vpc mode
							final List<Optional<String>> userVpcInterfaceAddresses =
									LoadBalancingSystemVpcs.getUserVpcInterfaceIps(instance.getInstanceId());
							if(userVpcInterfaceAddresses!=null) {
								final Optional<String> optPublicIp = userVpcInterfaceAddresses.get(0);
								final Optional<String> optPrivateIp = userVpcInterfaceAddresses.get(1);
								if(optPublicIp.isPresent())
									ipAddr = optPublicIp.get();
								if(optPrivateIp.isPresent())
									privateIpAddr = optPrivateIp.get();
							}
						}else { /// in classic mode
							if (instance.getAddress() == null) {
								try {
									List<RunningInstancesItemType> result = null;
									result = EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(Lists.newArrayList(instance.getInstanceId()));
									if (result != null && result.size() > 0) {
										ipAddr = result.get(0).getIpAddress();
										privateIpAddr = result.get(0).getPrivateIpAddress();
									}
								} catch (Exception ex) {
									LOG.warn("failed to run describe-instances", ex);
									continue;
								}
							} else {
								ipAddr = instance.getAddress();
								privateIpAddr = instance.getPrivateIp();
							}
						}
						try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
							final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
							update.setAddress(ipAddr);
							update.setPrivateIp(privateIpAddr);
							if (!(ipAddr == null && privateIpAddr == null) )
							  update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Registered);
							db.commit();
							LoadBalancingServoCache.getInstance().invalidate(update);
						}catch(NoSuchElementException ex){
							LOG.warn("failed to find the servo instance named "+instance.getInstanceId(), ex);
						}catch(Exception ex){
							LOG.warn("failed to update servo instance's ip address", ex);
						}
					}
				}else if (LoadBalancerServoInstance.STATE.OutOfService.equals(instance.getState()) ||
						LoadBalancerServoInstance.STATE.Error.equals(instance.getState())
						){
					if(!LoadBalancerServoInstance.DNS_STATE.Deregistered.equals(instance.getDnsState())){
						try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
							final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
							update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
							db.commit();
	            LoadBalancingServoCache.getInstance().invalidate(update);
						}catch(NoSuchElementException ex){
							LOG.warn("failed to find the servo instance named "+instance.getInstanceId(), ex);
						}catch(Exception ex){
							LOG.warn("failed to update servo instance's ip address", ex);
						}
					}
				}
			}
		}
	}
}
