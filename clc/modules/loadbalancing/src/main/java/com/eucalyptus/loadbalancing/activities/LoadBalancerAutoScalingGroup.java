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

import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.google.common.collect.Lists;
/**
 * @author Sang-Min Park
 *
 */

@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_autoscale_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerAutoScalingGroup extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerAutoScalingGroup.class );
	@Transient
	private static final long serialVersionUID = 1L;

	@OneToOne
	@JoinColumn( name = "metadata_loadbalancer_fk", nullable=true)
	private LoadBalancer loadbalancer = null;
	
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "autoscaling_group")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servos = null;

	@Column(name="metadata_group_name", nullable=false)
	private String groupName = null;
	
	@Column(name="metadata_launch_config_name", nullable=false, unique=true)
	private String launchConfigName = null;

	@Column(name="metadata_capacity", nullable=true)
	private Integer capacity = null;
	
	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	private LoadBalancerAutoScalingGroup(){}
	private LoadBalancerAutoScalingGroup(final LoadBalancer lb, final String launchConfigName, final String groupName){
		this.loadbalancer = lb;
		this.launchConfigName = launchConfigName;
		this.groupName = groupName;
		this.uniqueName = this.createUniqueName();
	}
	
	public static LoadBalancerAutoScalingGroup newInstance(final LoadBalancer lb, final String launchConfigName, final String groupName){
		final LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup(lb, launchConfigName, groupName);
		return instance;
	}
	
	public static LoadBalancerAutoScalingGroup named(final LoadBalancer lb){
		LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup();
		instance.loadbalancer = lb;
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
		
	public static LoadBalancerAutoScalingGroup named(){
		return new LoadBalancerAutoScalingGroup();
	}
	
	public String getName(){
		return this.groupName;
	}
	
	public String getLaunchConfigName(){
		return this.launchConfigName;
	}
	
	public void setCapacity(int capacity){
		this.capacity = capacity;
	}
	
	public int getCapacity(){
		return this.capacity;
	}
	
	public List<LoadBalancerServoInstance> getServos(){
		return Lists.newArrayList(this.servos);
	}
	
	public LoadBalancer getLoadBalancer(){
		return this.loadbalancer;
	}
    
    public void setLoadBalancer(LoadBalancer lb){
    	this.loadbalancer = lb;
    }
    
    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("autoscale-group-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName());
    }
	
}
