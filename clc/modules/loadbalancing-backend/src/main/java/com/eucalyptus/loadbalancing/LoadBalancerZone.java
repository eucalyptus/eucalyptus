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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreViewTransform;
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
 * @author Sang-Min Park
*
*/
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_zone" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerZone extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerZone.class );
	public enum STATE {
		InService, OutOfService
	}
	
	@Transient
	private static final long serialVersionUID = 1L;

	@Transient
	private LoadBalancerZoneRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerZoneRelationView(this);
	}
	
	private LoadBalancerZone(){ }
	
	private LoadBalancerZone(final LoadBalancer lb, final String zone){
		this.loadbalancer = lb;
		this.zoneName=zone;
		this.uniqueName = this.createUniqueName();
		view = new LoadBalancerZoneRelationView(this);
	}
	
	public static LoadBalancerZone named(final LoadBalancer lb, final String zone){
		return new LoadBalancerZone(lb, zone);
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
    private LoadBalancer loadbalancer = null;

	@Column(name="zone_name", nullable=false)
	private String zoneName = null;
	
	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	@Column(name="zone_state", nullable=true)
	private String zoneState = null;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "zone")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servoInstances = null;
	
	//@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "zone")
	//@OneToMany(fetch = FetchType.LAZY, mappedBy = "zone")
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "zone")
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
		
	public String getName(){
		return this.zoneName;
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
		final STATE state = Enum.valueOf(STATE.class, this.zoneState);
		return state;
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
		private LoadBalancerZone zone = null;
		LoadBalancerZoneCoreView(final LoadBalancerZone zone){
			this.zone = zone;
		}
		
		public String getName(){
			return this.zone.getName();
		}
		
		public STATE getState(){
			return this.zone.getState();
		}
	}
	
	@TypeMapper
	public enum LoadBalancerZoneCoreViewTransform implements Function<LoadBalancerZone, LoadBalancerZoneCoreView>{
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerZoneCoreView apply(@Nullable LoadBalancerZone arg0) {
			return new LoadBalancerZoneCoreView(arg0);
		}
	}
	
	public enum LoadBalancerZoneEntityTransform implements Function<LoadBalancerZoneCoreView, LoadBalancerZone> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerZone apply(@Nullable LoadBalancerZoneCoreView arg0) {
			final EntityTransaction db = Entities.get(LoadBalancerZone.class);
			try{
				final LoadBalancerZone zone = Entities.uniqueResult(arg0.zone);
				db.commit();
				return zone;
			}catch(final Exception ex){
				db.rollback();
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
		}
	}
	
	public static class LoadBalancerZoneRelationView {
		private LoadBalancerCoreView loadbalancer= null;
		private ImmutableList<LoadBalancerBackendInstanceCoreView> backendInstances = null;
		private ImmutableList<LoadBalancerServoInstanceCoreView> servoInstances = null;
		
		LoadBalancerZoneRelationView(final LoadBalancerZone zone){
			if(zone.loadbalancer!=null)
				loadbalancer = TypeMappers.transform(zone.loadbalancer, LoadBalancerCoreView.class);
			
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
			return this.loadbalancer;
		}
	}
}
