/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerAutoScalingGroupView;


/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_autoscale_group" )
public class LoadBalancerAutoScalingGroup extends AbstractPersistent implements LoadBalancerAutoScalingGroupView {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerAutoScalingGroup.class );

	private static final long serialVersionUID = 1L;

  @ManyToOne
  @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
  private LoadBalancer loadbalancer = null;
  
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "autoscaling_group")
	private Collection<LoadBalancerServoInstance> servos = null;

	@Column(name="metadata_group_name", nullable=false)
	private String groupName = null;
	
	@Column(name="metadata_capacity", nullable=true)
	private Integer capacity = null;
	
	/// NOTE: Post 3.4, launch_config_name is not used; left here for upgrade
	/// To reference the latest launch config name associated with scaling group, use 'describe-autoscaling-group'
	@Column(name="metadata_launch_config_name", nullable=true)
	private String launchConfig = null; // not used post 3.4.
	
  @Column(name="metadata_availability_zone", nullable=true)
  private String availabilityZone = null;

	@Column(name="metadata_user_subnet_id", nullable=true)
	private String userSubnetId = null;

	@Column(name="metadata_system_subnet_id", nullable=true)
	private String systemSubnetId = null;


	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	private LoadBalancerAutoScalingGroup(){}
	private LoadBalancerAutoScalingGroup(final LoadBalancer lb, final String availabilityZone, final String groupName, final String launchConfig){
		this.loadbalancer = lb;
		this.availabilityZone = availabilityZone;
		this.groupName = groupName;		
		this.launchConfig = launchConfig;
		this.uniqueName = this.createUniqueName();
	}

	private LoadBalancerAutoScalingGroup(final LoadBalancer lb, final String availabilityZone,
										 final String userSubnetId, final String systemSubnetId, final String groupName, final String launchConfig){
		this.loadbalancer = lb;
		this.availabilityZone = availabilityZone;
		this.userSubnetId =  userSubnetId;
		this.systemSubnetId = systemSubnetId;
		this.groupName = groupName;
		this.launchConfig = launchConfig;
		this.uniqueName = this.createUniqueName();
	}

	public static LoadBalancerAutoScalingGroup newInstance(final LoadBalancer lb, final String availabilityZone,
														   final String userSubnetId, final String systemSubnetId, final String groupName, final String launchConfig) {
		final LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup(lb, availabilityZone,
				userSubnetId, systemSubnetId, groupName, launchConfig);
		return instance;
	}

	public static LoadBalancerAutoScalingGroup newInstance(final LoadBalancer lb, final String availabilityZone, final String groupName, final String launchConfig){
		final LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup(lb, availabilityZone, groupName, launchConfig);
		return instance;
	}
	
	public static LoadBalancerAutoScalingGroup named(final LoadBalancer lb, final String availabilityZone){
		LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup();
		instance.loadbalancer = lb;
		instance.availabilityZone = availabilityZone;
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
		
	public static LoadBalancerAutoScalingGroup named(){
		return new LoadBalancerAutoScalingGroup();
	}
	
	public String getName(){
		return this.groupName;
	}
	
	public void setCapacity(int capacity){
		this.capacity = capacity;
	}
	
	public int getCapacity(){
		return this.capacity;
	}
	
	public String getAvailabilityZone() {
	  return this.availabilityZone;
	}
	
	public void setAvailabilityZone(final String zone) {
	  this.availabilityZone = zone;
	}

	public String getUserSubnetId() { return this.userSubnetId; }

	public String getSystemSubnetId() { return this.systemSubnetId; }

	public void setUserSubnetId(final String subnetId) { this.userSubnetId = subnetId; }

	public void setSystemSubnetId(final String subnetId) { this.systemSubnetId = subnetId; }
	
	public Collection<LoadBalancerServoInstance> getServos(){
		return this.servos;
	}
	
    public void setLoadBalancer(LoadBalancer lb){
    	this.loadbalancer = lb;
    }
    
    public LoadBalancer getLoadBalancer(){
    	return this.loadbalancer;
    }
    
    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("autoscale-group-%s-%s-%s", 
    	    this.loadbalancer.getOwnerAccountNumber(), 
    	    this.loadbalancer.getDisplayName(),
    	    this.availabilityZone);
    }
}
