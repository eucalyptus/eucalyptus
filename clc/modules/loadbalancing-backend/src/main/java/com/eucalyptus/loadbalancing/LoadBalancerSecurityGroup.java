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
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreViewTransform;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerSecurityGroup extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerSecurityGroup.class );

	public enum STATE { InService, OutOfService }
	@Transient
	private static final long serialVersionUID = 1L;

	private LoadBalancerSecurityGroup(){}
	
	@Transient
	private LoadBalancerSecurityGroupRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerSecurityGroupRelationView(this);
	}
	
	private LoadBalancerSecurityGroup(LoadBalancer lb, String ownerAccountId, String groupName){
		this.loadbalancer = lb;
		this.groupName = groupName;
		this.ownerAccountId = ownerAccountId;
		this.state= STATE.InService.name();
	}
	
	public static LoadBalancerSecurityGroup named(){
		return new LoadBalancerSecurityGroup(); // query all
	}

	public static LoadBalancerSecurityGroup named(LoadBalancer lb, String ownerAccountId, String groupName){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup(lb, ownerAccountId, groupName);
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
	
	public static LoadBalancerSecurityGroup named(LoadBalancer lb, String ownerAccountId, String groupName, STATE state){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup(lb, ownerAccountId, groupName);
		instance.state = state.name();
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
	
	public static LoadBalancerSecurityGroup withState(STATE state){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup();
		instance.state = state.name();
		return instance;
	}
	
    @OneToOne
    @JoinColumn( name = "metadata_loadbalancer_fk", nullable=true)
    private LoadBalancer loadbalancer = null;

	@Column(name="group_name", nullable=false)
    private String groupName = null;
	
	@Column(name="group_owner_account_id", nullable=false)
	private String ownerAccountId = null;
	
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
	
	public String getGroupOwnerAccountId(){
		return this.ownerAccountId;
	}
	
	public Collection<LoadBalancerServoInstanceCoreView> getServoInstances(){
		return this.view.getServoInstances();
	}
	
	public LoadBalancerCoreView getLoadBalancer(){
		return this.view.getLoadBalancer();
	}
	
	public void setLoadBalancer(final LoadBalancer lb){
		this.loadbalancer = lb;
	}
	
	public void setState(STATE state){
		this.state = state.name();
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
		return String.format("loadbalancer-sgroup-%s-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.ownerAccountId, this.groupName);
	}
	
	@Override
	public String toString(){
		return this.uniqueName;
	}
	
	public static class LoadBalancerSecurityGroupCoreView {
		private LoadBalancerSecurityGroup group = null;
		LoadBalancerSecurityGroupCoreView(final LoadBalancerSecurityGroup group){
			this.group = group;
		}
		
		public String getName(){
			return this.group.getName();
		}
		
		public String getGroupOwnerAccountId(){
			return this.group.getGroupOwnerAccountId();
		}
		
		public STATE getState(){
			return this.group.getState();
		}
	}
	

	@TypeMapper
	public enum LoadBalancerSecurityGroupCoreViewTransform implements Function<LoadBalancerSecurityGroup, LoadBalancerSecurityGroupCoreView> {
		INSTANCE;

		@Override
		public LoadBalancerSecurityGroupCoreView apply( final LoadBalancerSecurityGroup group ) {
			return new LoadBalancerSecurityGroupCoreView( group );
		}
	}
	
	public enum LoadBalancerSecurityGroupEntityTransform implements Function<LoadBalancerSecurityGroupCoreView, LoadBalancerSecurityGroup> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerSecurityGroup apply(
				@Nullable LoadBalancerSecurityGroupCoreView arg0) {
			final EntityTransaction db = Entities.get(LoadBalancerSecurityGroup.class);
			try{
				final LoadBalancerSecurityGroup group = Entities.uniqueResult(arg0.group);
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
	
	public static class LoadBalancerSecurityGroupRelationView  {
		private LoadBalancerCoreView lbView = null;
		private ImmutableList<LoadBalancerServoInstanceCoreView> servoViews = null;

		LoadBalancerSecurityGroupRelationView(LoadBalancerSecurityGroup group) {
			if(group.loadbalancer!=null)
				lbView = TypeMappers.transform( group.loadbalancer, LoadBalancerCoreView.class );
			if(group.servoInstances!=null)
				servoViews = ImmutableList.copyOf( Collections2.transform(group.servoInstances, 
					LoadBalancerServoInstanceCoreViewTransform.INSTANCE));
		}
	
		LoadBalancerCoreView getLoadBalancer(){
			return this.lbView;
		}
	
		public ImmutableList<LoadBalancerServoInstanceCoreView> getServoInstances(){
			return this.servoViews;
		}	
	}
}
