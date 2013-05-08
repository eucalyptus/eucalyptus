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

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_servo_instance" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
	
    @ManyToOne
    @JoinColumn( name = "metadata_zone_fk", nullable=true)
    private LoadBalancerZone zone = null;
    
    @ManyToOne
    @JoinColumn( name = "metadata_group_fk", nullable=true)
    private LoadBalancerSecurityGroup security_group = null;
    
    @ManyToOne
    @JoinColumn( name = "metadata_dns_fk", nullable=true)
    private LoadBalancerDnsRecord dns = null; 
    
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
    
    private LoadBalancerServoInstance(){
    }
    private LoadBalancerServoInstance(final LoadBalancerZone lbzone){
    	this.state = STATE.Pending.name();
    	this.zone = lbzone;
    	try{
	    	this.loadbalancer = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
	    	this.dns = LoadBalancerDnsRecordEntityTransform.INSTANCE.apply(this.loadbalancer.getDns());
    	}catch(final Exception ex){
    		throw Exceptions.toUndeclared(ex);
    	}
    }
    private LoadBalancerServoInstance(final LoadBalancerZone lbzone, final LoadBalancerSecurityGroup group){
    	this.state = STATE.Pending.name();
    	this.zone = lbzone;
    	try{
	    	this.loadbalancer = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
	    	this.dns = LoadBalancerDnsRecordEntityTransform.INSTANCE.apply(this.loadbalancer.getDns());
    	}catch(final Exception ex){
    		throw Exceptions.toUndeclared(ex);
    	}
    	this.security_group = group;
    }
    private LoadBalancerServoInstance(final LoadBalancerZone lbzone, final LoadBalancerSecurityGroup group, final LoadBalancerDnsRecord dns){
    	this.state = STATE.Pending.name();
    	this.zone = lbzone;
    	try{
    		this.loadbalancer = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
    	}catch(final Exception ex){
    		throw Exceptions.toUndeclared(ex);
    	}
    	this.security_group = group;
    	this.dns = dns;
    }
    
    public static LoadBalancerServoInstance newInstance(final LoadBalancerZone lbzone, final LoadBalancerSecurityGroup group, final LoadBalancerDnsRecord dns, final LoadBalancerAutoScalingGroup as_group, String instanceId)
    {
    	final LoadBalancerServoInstance instance = new LoadBalancerServoInstance(lbzone, group, dns);
    	instance.setInstanceId(instanceId);
    	instance.setAutoScalingGroup(as_group);
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
    
    public void leaveZone(){
    	this.zone = null;
    }
    
    public void unmapDns(){
    	this.dns = null;
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
    
    public void setDns(final LoadBalancerDnsRecord dns){
    	this.dns = dns;
    }

    public LoadBalancerDnsRecordCoreView getDns(){
    	return this.view.getDns();
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
    public String getPrivateIp(){
    	return this.privateIp;
    }
    
    public void setPrivateIp(final String ipAddr){
    	this.privateIp = ipAddr;
    }
    
	@Override
	public String toString(){
		String id = this.instanceId==null? "unassigned" : this.instanceId;
		return String.format("Servo-instance (%s) for loadbalancer %s in %s", id, this.loadbalancer.getDisplayName(), this.zone.getName());
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
			final EntityTransaction db = Entities.get(LoadBalancerServoInstance.class);
			try{
				final LoadBalancerServoInstance servo = Entities.uniqueResult(arg0.entity);
				db.commit();
				return servo;
			}catch(final Exception ex){
				db.rollback();
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
		}
	}
	
	public static class LoadBalancerServoInstanceRelationView {
	    private LoadBalancerZoneCoreView zone = null;
	    private LoadBalancerSecurityGroupCoreView security_group = null;
	    private LoadBalancerDnsRecordCoreView dns = null; 
	    private LoadBalancerAutoScalingGroupCoreView autoscaling_group = null; 
	    
		private LoadBalancerServoInstanceRelationView(final LoadBalancerServoInstance servo){
			if(servo.zone!=null)
				this.zone = TypeMappers.transform(servo.zone, LoadBalancerZoneCoreView.class);
			if(servo.security_group!=null)
				this.security_group = TypeMappers.transform(servo.security_group, LoadBalancerSecurityGroupCoreView.class);
			if(servo.dns!=null)
				this.dns = TypeMappers.transform(servo.dns, LoadBalancerDnsRecordCoreView.class);
			if(servo.autoscaling_group!=null)
				this.autoscaling_group = TypeMappers.transform(servo.autoscaling_group, LoadBalancerAutoScalingGroupCoreView.class);
		}
		
		public LoadBalancerDnsRecordCoreView getDns(){
			return this.dns;
		}
		
		public LoadBalancerZoneCoreView getZone(){
			return this.zone;
		}
	}
}
