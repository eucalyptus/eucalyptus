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

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */

@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerSecurityGroup extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerSecurityGroup.class );

	public enum STATE { InService, OutOfService }
	@Transient
	private static final long serialVersionUID = 1L;

	private LoadBalancerSecurityGroup(){}
	
	private LoadBalancerSecurityGroup(LoadBalancer lb, String groupName){
		this.loadbalancer = lb;
		this.groupName = groupName;
		this.state= STATE.InService.name();
	}
	
	public static LoadBalancerSecurityGroup named(){
		return new LoadBalancerSecurityGroup(); // query all
	}

	public static LoadBalancerSecurityGroup named(LoadBalancer lb, String groupName){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup(lb, groupName);
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
	
	public static LoadBalancerSecurityGroup named(LoadBalancer lb, String groupName, STATE state){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup(lb, groupName);
		instance.state = state.name();
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
	
	public static LoadBalancerSecurityGroup withState(STATE state){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup();
		instance.state = state.name();
		return instance;
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk", nullable=true)
    private LoadBalancer loadbalancer = null;

	@Column(name="group_name", nullable=false)
    private String groupName = null;
	
	@Column(name="metadata_state", nullable=false)
	private String state = null;
    
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "security_group")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servoInstances = null;

	@Column(name="metadata_unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	public String getName(){
		return this.groupName;
	}
	public Collection<LoadBalancerServoInstance> getServoInstances(){
		return this.servoInstances;
	}
	
	public LoadBalancer getLoadBalancer(){
		return this.loadbalancer;
	}
	
	public void setLoadBalancer(final LoadBalancer lb){
		this.loadbalancer = lb;
	}

	public void retire(){
		this.state = STATE.OutOfService.name();
	}
	
	public STATE getState(){
		return Enum.valueOf(STATE.class, this.state);
	}
	
	@PrePersist
	private void generateOnCommit( ) {
		if(this.uniqueName==null)
			this.uniqueName = createUniqueName( );
	}

	private String createUniqueName(){
		return String.format("loadbalancer-sgroup-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.groupName);
	}
	
	@Override
	public String toString(){
		return this.uniqueName;
	}
}
