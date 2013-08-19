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
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreViewTransform;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_autoscale_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerAutoScalingGroup extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerAutoScalingGroup.class );
	@Transient
	private static final long serialVersionUID = 1L;

	@Transient
	private LoadBalancerAutoScalingGroupRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerAutoScalingGroupRelationView(this);
	}
	
	@OneToOne
	@JoinColumn( name = "metadata_loadbalancer_fk", nullable=true)
	private LoadBalancer loadbalancer = null;
	
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "autoscaling_group")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servos = null;

	@Column(name="metadata_group_name", nullable=false)
	private String groupName = null;
	
	@Column(name="metadata_capacity", nullable=true)
	private Integer capacity = null;
	
	/// NOTE: Post 3.4, launch_config_name is not used; left here for upgrade
	/// To reference the latest launch config name associated with scaling group, use 'describe-autoscaling-group'
	@Column(name="metadata_launch_config_name", nullable=true)
	private String launchConfig = null; // not used post 3.4.
	
	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	private LoadBalancerAutoScalingGroup(){}
	private LoadBalancerAutoScalingGroup(final LoadBalancer lb, final String groupName, final String launchConfig){
		this.loadbalancer = lb;
		this.groupName = groupName;		
		this.launchConfig = launchConfig;
		this.uniqueName = this.createUniqueName();
		view = new LoadBalancerAutoScalingGroupRelationView(this);
	}
	
	public static LoadBalancerAutoScalingGroup newInstance(final LoadBalancer lb, final String groupName, final String launchConfig){
		final LoadBalancerAutoScalingGroup instance = new LoadBalancerAutoScalingGroup(lb, groupName, launchConfig);
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
	
	public void setCapacity(int capacity){
		this.capacity = capacity;
	}
	
	public int getCapacity(){
		return this.capacity;
	}
	
	public List<LoadBalancerServoInstanceCoreView> getServos(){
		return view.getServos();
	}
	
    public void setLoadBalancer(LoadBalancer lb){
    	this.loadbalancer = lb;
    }
    
    public LoadBalancerCoreView getLoadBalancer(){
    	return this.view.getLoadBalancer();
    }
    
    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("autoscale-group-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName());
    }
    
    public static class LoadBalancerAutoScalingGroupCoreView {
    	private LoadBalancerAutoScalingGroup group = null;
    	
    	LoadBalancerAutoScalingGroupCoreView(LoadBalancerAutoScalingGroup group){
    		this.group = group;
    	}
    	
    	public String getName(){
    		return this.group.getName();
    	}
    	
    	public int getCapacity(){
    		return this.group.getCapacity();
    	}    	
    	
    }
    
	@TypeMapper
    public enum LoadBalancerAutoScalingGroupCoreViewTransform implements Function<LoadBalancerAutoScalingGroup, LoadBalancerAutoScalingGroupCoreView>{
    	INSTANCE;

		@Override
		@Nullable
		public LoadBalancerAutoScalingGroupCoreView apply(
				@Nullable LoadBalancerAutoScalingGroup arg0) {
			return new LoadBalancerAutoScalingGroupCoreView(arg0);
		}	
    }
	
	public enum LoadBalancerAutoScalingGroupEntityTransform implements Function<LoadBalancerAutoScalingGroupCoreView, LoadBalancerAutoScalingGroup>{
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerAutoScalingGroup apply(
				@Nullable LoadBalancerAutoScalingGroupCoreView arg0) {
			final EntityTransaction db = Entities.get(LoadBalancerAutoScalingGroup.class);
			try{
				final LoadBalancerAutoScalingGroup group = Entities.uniqueResult(arg0.group);
				db.commit();
				return group;
			}catch(final Exception ex){
				db.rollback();
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
		}
	}
	
	public static class LoadBalancerAutoScalingGroupRelationView {
		private ImmutableList<LoadBalancerServoInstanceCoreView> servos = null;
		private LoadBalancerCoreView loadbalancer = null;
		
		LoadBalancerAutoScalingGroupRelationView(
				LoadBalancerAutoScalingGroup group) {
			if(group.servos!=null)
				servos = ImmutableList.copyOf(Collections2.transform(group.servos, LoadBalancerServoInstanceCoreViewTransform.INSTANCE));
			if(group.loadbalancer!=null)
				loadbalancer = TypeMappers.transform(group.loadbalancer, LoadBalancerCoreView.class);
		}
		
		public ImmutableList<LoadBalancerServoInstanceCoreView> getServos(){
			return servos;
		}
		
		public LoadBalancerCoreView getLoadBalancer(){
			return loadbalancer;
		}
	}
}
