/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreViewTransform;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMapper;
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
public class LoadBalancerSecurityGroup extends AbstractPersistent {

	public enum STATE { InService, OutOfService }

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
	
	public static LoadBalancerSecurityGroup create(LoadBalancer lb, String ownerAccountId, String groupName){
		final LoadBalancerSecurityGroup instance = new LoadBalancerSecurityGroup(lb, ownerAccountId, groupName);
		instance.uniqueName = instance.createUniqueName();
		return instance;
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

	public enum LoadBalancerSecurityGroupEntityTransform implements NonNullFunction<LoadBalancerSecurityGroupCoreView, LoadBalancerSecurityGroup> {
		INSTANCE;

		@Nonnull
		@Override
		public LoadBalancerSecurityGroup apply( LoadBalancerSecurityGroupCoreView arg0 ) {
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
				return Entities.uniqueResult(arg0.group);
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	public static class LoadBalancerSecurityGroupRelationView  {
		private LoadBalancer loadBalancer = null;
		private ImmutableList<LoadBalancerServoInstanceCoreView> servoViews = null;

		LoadBalancerSecurityGroupRelationView(LoadBalancerSecurityGroup group) {
			if(group.loadbalancer!=null) {
				Entities.initialize( group.loadbalancer );
				this.loadBalancer = group.loadbalancer;
			}
			if(group.servoInstances!=null)
				servoViews = ImmutableList.copyOf( Collections2.transform(group.servoInstances, 
					LoadBalancerServoInstanceCoreViewTransform.INSTANCE));
		}
	
		LoadBalancerCoreView getLoadBalancer(){
			return this.loadBalancer.getCoreView();
		}
	
		public ImmutableList<LoadBalancerServoInstanceCoreView> getServoInstances(){
			return this.servoViews;
		}	
	}
}
