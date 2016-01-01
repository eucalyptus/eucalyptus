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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreViewTransform;
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
 * @author Sang-Min Park
*
*/
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_zone" )
public class LoadBalancerZone extends AbstractPersistent {

	public enum STATE {
		InService, OutOfService
	}
	
	private static final long serialVersionUID = 1L;

	@Transient
	private transient LoadBalancerZoneRelationView view;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerZoneRelationView(this);
	}
	
	protected LoadBalancerZone(){ }
	
	private LoadBalancerZone(
		final LoadBalancer lb,
		final String zone,
		final String subnetId
	){
		this.loadbalancer = lb;
		this.zoneName=zone;
		this.subnetId = subnetId;
		this.uniqueName = this.createUniqueName();
		view = new LoadBalancerZoneRelationView(this);
	}

	public static LoadBalancerZone create(
		final LoadBalancer lb,
		final String zone,
		final String subnetId
	){
		return new LoadBalancerZone(lb, zone, subnetId);
	}

	public static LoadBalancerZone named(
		final LoadBalancer lb,
		final String zone
	){
		return new LoadBalancerZone(lb, zone, null);
	}
	
	@ManyToOne
	@JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
	private LoadBalancer loadbalancer = null;
	
	
	@Column(name="autoscaling_group", nullable=true)
	private String autoscalingGroup = null;	

	@Column(name="zone_name", nullable=false)
	private String zoneName;

	@Column(name="subnetId")
	private String subnetId;

	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName;
	
	@Column(name="zone_state", nullable=true)
	private String zoneState;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "zone")
	private Collection<LoadBalancerServoInstance> servoInstances = null;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "zone")
	private Collection<LoadBalancerBackendInstance> backendInstances;
		
	public String getName( ){
		return this.zoneName;
	}

	public String getSubnetId( ) {
		return subnetId;
	}

  public LoadBalancerCoreView getLoadbalancer(){
		return this.view.getLoadBalancer();
	} 
  
	public Collection<LoadBalancerServoInstanceCoreView> getServoInstances(){
		return this.view.getServoInstances();
	}
	
	public Collection<LoadBalancerBackendInstanceCoreView> getBackendInstances(){
		return this.view.getBackendInstances();
	}
	
	public void setState(STATE state){
		this.zoneState = state.name();
	}
	
	public STATE getState(){
		return Enum.valueOf(STATE.class, this.zoneState);
	}
	
	public String getAutoscalingGroup() {
	  return this.autoscalingGroup;
	}
	
	public void setAutoscalingGroup(final String group) {
	  this.autoscalingGroup = group;
	}

	@PrePersist
	private void generateOnCommit( ) {
		if(this.uniqueName==null)
			this.uniqueName = createUniqueName( );
	}

	protected String createUniqueName( ) {
		return String.format("zone-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.zoneName);
	}
	  
	@Override
	public String toString(){
		String name="unassigned";
		if(this.loadbalancer!=null && this.zoneName!=null)
			name = String.format("loadbalancer-zone-%s-%s", this.loadbalancer.getDisplayName(), this.zoneName);
		return name;
	}
	
	public static class LoadBalancerZoneCoreView {
		private final LoadBalancerZone zone;
		LoadBalancerZoneCoreView(final LoadBalancerZone zone){
			this.zone = zone;
		}
		
		public String getName( ){
			return zone.getName( );
		}

		public String getSubnetId( ){
			return zone.getSubnetId( );
		}

		public STATE getState( ){
			return this.zone.getState( );
		}
		
		public String getAutoscalingGroup(){
		  return this.zone.getAutoscalingGroup();
		}

		public static NonNullFunction<LoadBalancerZoneCoreView,String> name( ) {
			return StringPropertyFunctions.Name;
		}

		public static NonNullFunction<LoadBalancerZoneCoreView,String> subnetId( ) {
			return StringPropertyFunctions.SubnetId;
		}

		private enum StringPropertyFunctions implements NonNullFunction<LoadBalancerZoneCoreView,String> {
			Name {
				@Nonnull
				@Override
				public String apply( final LoadBalancerZoneCoreView loadBalancerZoneCoreView ) {
					return loadBalancerZoneCoreView.getName( );
				}
			},
			SubnetId {
				@Nonnull
				@Override
				public String apply( final LoadBalancerZoneCoreView loadBalancerZoneCoreView ) {
					return loadBalancerZoneCoreView.getSubnetId( );
				}
			},
		}
	}

	@TypeMapper
	public enum LoadBalancerZoneCoreViewTransform implements Function<LoadBalancerZone, LoadBalancerZoneCoreView> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerZoneCoreView apply(@Nullable LoadBalancerZone arg0) {
			return new LoadBalancerZoneCoreView(arg0);
		}
	}
	
	public enum LoadBalancerZoneEntityTransform implements NonNullFunction<LoadBalancerZoneCoreView, LoadBalancerZone> {
		INSTANCE;

		@Nonnull
		@Override
		public LoadBalancerZone apply( LoadBalancerZoneCoreView arg0) {
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
				return Entities.uniqueResult(arg0.zone);
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	public static class LoadBalancerZoneRelationView {
		private LoadBalancer loadbalancer= null;
		private ImmutableList<LoadBalancerBackendInstanceCoreView> backendInstances = null;
		private ImmutableList<LoadBalancerServoInstanceCoreView> servoInstances = null;
		
		LoadBalancerZoneRelationView(final LoadBalancerZone zone){
			if(zone.loadbalancer!=null) {
				Entities.initialize( zone.loadbalancer );
				loadbalancer = zone.loadbalancer;
			}
			
			if(zone.backendInstances!=null)
				this.backendInstances = ImmutableList.copyOf(Collections2.transform(zone.backendInstances, LoadBalancerBackendInstanceCoreViewTransform.INSTANCE));
			
			if(zone.servoInstances!=null)
				this.servoInstances = ImmutableList.copyOf(Collections2.transform(zone.servoInstances, LoadBalancerServoInstanceCoreViewTransform.INSTANCE));
		}
		
		public ImmutableList<LoadBalancerBackendInstanceCoreView> getBackendInstances() {
			return this.backendInstances;
		}
		
		public ImmutableList<LoadBalancerServoInstanceCoreView> getServoInstances() {
			return this.servoInstances;
		}
		
		public LoadBalancerCoreView getLoadBalancer(){
			return this.loadbalancer.getCoreView();
		}
	}
}
