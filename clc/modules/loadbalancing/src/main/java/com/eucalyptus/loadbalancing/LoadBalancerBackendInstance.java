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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

/**
 * @author Sang-Min Park
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_backend_instance" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerBackendInstance extends UserMetadata<LoadBalancerBackendInstance.STATE> {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendInstance.class );
	@Transient
	private static final long serialVersionUID = 1L;
	@Transient
	private String partition = null;
	
	public enum STATE {
		InService, OutOfService, Error
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk" )
	private LoadBalancer loadbalancer = null;

    @ManyToOne
    @JoinColumn( name = "metadata_zone_fk")
    private LoadBalancerZone zone = null;
        
    @Transient
    private RunningInstancesItemType vmInstance = null;
	
	@Column( name = "reason_code", nullable=true)
	private String reasonCode = null;
	
	@Column( name = "description", nullable=true)
	private String description = null;

    private LoadBalancerBackendInstance(){
    	super(null,null);
    }
	
	private LoadBalancerBackendInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId) {
		super(userFullName, vmId);
		this.loadbalancer = lb;
		this.setState(STATE.OutOfService);
		this.setReasonCode("ELB");
		this.setDescription("Instance registration is still in progress.");
		
		if(this.getVmInstance() == null)
    		throw new IllegalArgumentException("Cannot find the instance with id="+vmId);

    	final EntityTransaction db = Entities.get( LoadBalancerBackendInstance.class );
    	try{
    		final LoadBalancerZone found = Entities.uniqueResult(LoadBalancerZone.named(lb, this.vmInstance.getPlacement()));
    		this.setAvailabilityZone(found);
    		db.commit();
    	}catch(NoSuchElementException ex){
    		db.rollback();
    	}catch(Exception ex){
    		db.rollback();
    	}finally{
    		if(this.zone == null)
    			throw new IllegalArgumentException("Cannot find the availability zone");
    	}
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId){
		return new LoadBalancerBackendInstance(userFullName, lb, vmId);
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId, STATE state){
		LoadBalancerBackendInstance instance= new LoadBalancerBackendInstance(userFullName, lb, vmId);
		instance.setBackendState(state);
		return instance;
	}
	
	/*public static LoadBalancerBackendInstance named(final OwnerFullName userName, final String vmId){
		LoadBalancerBackendInstance instance = new LoadBalancerBackendInstance();
		instance.setOwner(userName);
		instance.setDisplayName(vmId);
		instance.setState(null);
		instance.setStateChangeStack(null);
		
		return instance;
	}*/
	
	public static LoadBalancerBackendInstance named(final String vmId){
		LoadBalancerBackendInstance instance = new LoadBalancerBackendInstance();
		instance.setOwner(null);
		instance.setDisplayName(vmId);
		instance.setState(null);
		instance.setStateChangeStack(null);
		
		return instance;
	}
	
	private static LoadBalancerBackendInstance named(final STATE state){
		final LoadBalancerBackendInstance instance = new LoadBalancerBackendInstance();
		instance.setBackendState(state);
		instance.setStateChangeStack(null);
		return instance;
	}
	
	private static LoadBalancerBackendInstance named(){
		return new LoadBalancerBackendInstance();
	}
		
	public String getInstanceId(){
		return this.getDisplayName();
	}
	
	public RunningInstancesItemType getVmInstance(){
		try{
			if(this.vmInstance==null){
				List<RunningInstancesItemType> instanceIds = 
					EucalyptusActivityTasks.getInstance().describeUserInstances(this.getOwnerUserId(), Lists.newArrayList(this.displayName));
				for(RunningInstancesItemType instance : instanceIds ) {
				      if(instance.getInstanceId().equals(this.getDisplayName()) && instance.getStateName().equals("running")){
				        this.vmInstance = instance;
				        this.partition = instance.getPlacement();
				        break;
				      }
				}
			}
		}catch(Exception ex){
			;
		}
		return this.vmInstance;
	}

    public LoadBalancer getLoadBalancer(){
    	return this.loadbalancer;
    }
    
    public void setBackendState(final STATE state){
    	this.setState(state);
    }
    
    public STATE getBackendState(){
    	return this.getState();
    }
    
    public String getReasonCode(){
    	return this.reasonCode;
    }
    
    public void setReasonCode(final String reason){
    	this.reasonCode = reason;
    }
    
    public String getDescription(){
    	return this.description;
    }
    
    public void setDescription(final String description){
    	this.description = description;
    }
    
    public void setAvailabilityZone(final LoadBalancerZone zone){
    	this.zone = zone;
    }
   
    public LoadBalancerZone getAvailabilityZone(){
    	return this.zone;
    }
    
	@Override
	public String getPartition() {
		if(this.partition!=null)
			return this.partition;
		else{
			final RunningInstancesItemType vm = this.getVmInstance();
			return vm != null? vm.getPlacement() : null;
		}
	}
	
	public String getIpAddress(){
		final RunningInstancesItemType vm = this.getVmInstance();
		return vm!=null ? vm.getIpAddress() : null;
	}

	@Override
	public FullName getFullName() {
		// TODO Auto-generated method stub
		return FullName.create.vendor( "euca" )
                .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                .namespace( this.getOwnerAccountNumber( ) )
                .relativeId( "loadbalancer-backend-instance", this.vmInstance.getInstanceId()!=null ? this.vmInstance.getInstanceId() : "");
	}
	@Override
	public int hashCode( ) {
		final int prime = 31;
		int result = super.hashCode( );
		result = prime * result + ( ( this.vmInstance == null )
	                                                     ? 0
	                                                     : this.vmInstance.hashCode( ) );
	    return result;
	}
	
	@Override
	public String toString(){
		return String.format("%s backend instance - %s", this.loadbalancer, this.getDisplayName());
	}
	
	@Override
	protected String createUniqueName( ) {
	    return ( this.getOwnerAccountNumber( ) != null && this.getDisplayName( ) != null && this.getLoadBalancer() != null)
	      ? this.getOwnerAccountNumber( ) + ":" + this.getLoadBalancer().getDisplayName()+":"+this.getDisplayName( )
	      : null;
	}
	
	/*
	 * Note that the backend instance check does not affect the health check result of the instances.
	 * the health check is left to the "ping" mechanism by the servo. the state update here is the mean
	 * by which to include only the non-faulty instances in the list delivered to servo.
	 */
	public static class BackendInstanceChecker implements EventListener<ClockTick> {
		private static final int CHECK_EVERY_SECONDS = 10;

		public static void register(){
			Listeners.register(ClockTick.class, new BackendInstanceChecker() );
		}
		
		@Override
		public void fireEvent(ClockTick event) {
			/// determine the BE instances to query
			final List<LoadBalancerBackendInstance> allInstances = Lists.newArrayList();
			final List<LoadBalancerBackendInstance> stateOutdated = Lists.newArrayList();
			EntityTransaction db = Entities.get(LoadBalancerBackendInstance.class);
			try{
				allInstances.addAll(
						Entities.query(LoadBalancerBackendInstance.named()));
				db.commit();
			}catch(final Exception ex){
				db.rollback();
			}
			final Map<String, STATE> stateMap = new HashMap<String, STATE>();
			final Date current = new Date(System.currentTimeMillis());
			// find the record eligible to check its status
			for(final LoadBalancerBackendInstance be : allInstances){
				if(STATE.Error.equals(be.getBackendState()))
					continue;
				final Date lastUpdate = be.getLastUpdateTimestamp();
				int elapsedSec = (int)((current.getTime() - lastUpdate.getTime())/1000.0);
				if(elapsedSec > CHECK_EVERY_SECONDS){
					stateOutdated.add(be);
					stateMap.put(be.getInstanceId(), be.getBackendState());
				}
			}
			
			db = Entities.get(LoadBalancerBackendInstance.class);
			try{
				for(final LoadBalancerBackendInstance be: stateOutdated){
					final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
					update.setLastUpdateTimestamp(current);
					Entities.persist(update);
				}
				db.commit();
			}catch(final Exception ex){
				db.rollback();
			}
			
			final List<String> instancesToCheck = 
					Lists.transform(stateOutdated, new Function<LoadBalancerBackendInstance,String>(){
						@Override
						@Nullable
						public String apply(
								@Nullable LoadBalancerBackendInstance arg0) {
							return arg0.getInstanceId();
						}
			});
			 	
			List<RunningInstancesItemType> result  = null;
			try{
				result =
						EucalyptusActivityTasks.getInstance().describeSystemInstances(instancesToCheck);
				if(result==null)
					throw new Exception();
			}catch(final Exception ex){
				LOG.warn("failed to query instances", ex);
				return;
			}
			
			for(final RunningInstancesItemType instance : result){
				final String state = instance.getStateName();
				if("pending".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.OutOfService);
				else if("running".equals(state))
					;
				else if("shutting-down".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
				else if("terminated".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
				else if("stopping".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
				else if("stopped".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
			}
			
			db = Entities.get(LoadBalancerBackendInstance.class);
			try{
				for(final LoadBalancerBackendInstance be : stateOutdated){
					STATE trueState = stateMap.get(be.getInstanceId());
					if(! trueState.equals(be.getBackendState())){
						final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
						update.setBackendState(trueState);
						Entities.persist(update);
					}
				}
				db.commit();
			}catch(final Exception ex){
				db.rollback();
			}
		}
	}
	  
}
