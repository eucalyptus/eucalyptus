/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.backend.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.backend.InvalidEndPointException;
import com.eucalyptus.loadbalancing.backend.LoadBalancingException;
import com.eucalyptus.loadbalancing.backend.LoadBalancingServoCache;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_backend_instance" )
public class LoadBalancerBackendInstance extends UserMetadata<LoadBalancerBackendInstance.STATE> {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendInstance.class );
	@Transient
	private static final long serialVersionUID = 1L;

	@Transient
	private LoadBalancerBackendInstanceRelationView view;
	
	@PostLoad
	private void onLoad(){
		if(view==null)
			view = new LoadBalancerBackendInstanceRelationView(this); 
	}
	
	public enum STATE {
		InService, OutOfService, Unknown, Error
	}

	@ManyToOne()
	@JoinColumn( name = "metadata_loadbalancer_fk" )
	private LoadBalancer loadbalancer = null;

	@ManyToOne()
	@JoinColumn( name = "metadata_zone_fk")
	private LoadBalancerZone zone = null;

	@Transient
	private RunningInstancesItemType vmInstance = null;

	@Column( name = "reason_code", nullable=true)
	private String reasonCode = null;
	
	@Column( name = "description", nullable=true)
	private String description = null;

	@Column( name = "ip_address", nullable=true)
	private String ipAddress = null;

  @Column( name = "partition", nullable=true)
  private String partition = null;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "instance_update_timestamp", nullable=true)
	private Date instanceUpdateTimestamp = null;
	  
	  
    private LoadBalancerBackendInstance(){
    	super(null,null);
    }
	
	private LoadBalancerBackendInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId) 
		throws LoadBalancingException
	{
		super(userFullName, vmId);
		this.loadbalancer = lb;
		this.setState(STATE.OutOfService);
		this.setReasonCode("ELB");
		this.setDescription("Instance registration is still in progress.");
		this.updateInstanceStateTimestamp();
		if(this.getVmInstance() == null)
			throw new InvalidEndPointException();

		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
			final LoadBalancerZone found = Entities.uniqueResult(LoadBalancerZone.named(lb, this.vmInstance.getPlacement()));
			this.setAvailabilityZone(found);
		}catch(final NoSuchElementException ex){
		}catch(final Exception ex){
			throw new InternalFailure400Exception("unable to find the zone");
		}finally{
			if(this.zone == null)
				throw new InternalFailure400Exception("unable to find the instance's zone");
		}
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId)
		throws LoadBalancingException
	{
		return new LoadBalancerBackendInstance(userFullName, lb, vmId);
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId, STATE state)
		throws LoadBalancingException
	{
		LoadBalancerBackendInstance instance= new LoadBalancerBackendInstance(userFullName, lb, vmId);
		instance.setBackendState(state);
		return instance;
	}
	
	public static LoadBalancerBackendInstance named(final LoadBalancer lb, final String vmId){
		LoadBalancerBackendInstance instance = new LoadBalancerBackendInstance();
		instance.setOwner(null);
		instance.setDisplayName(vmId);
		instance.setLoadBalancer(lb);
		instance.setState(null);
		instance.setStateChangeStack(null);
		instance.getUniqueName();
		return instance;
	}
	
	private static LoadBalancerBackendInstance named(){
		return new LoadBalancerBackendInstance();
	}
		
	public String getInstanceId(){
		return this.getDisplayName();
	}
	
	private RunningInstancesItemType getVmInstance(){
		try{
			if(this.vmInstance==null){
				List<RunningInstancesItemType> instanceIds = 
					EucalyptusActivityTasks.getInstance().describeUserInstances(this.getOwnerAccountNumber(), Lists.newArrayList(this.displayName));
				for(RunningInstancesItemType instance : instanceIds ) {
				      if(instance.getInstanceId().equals(this.getDisplayName()) && instance.getStateName().equals("running")){
				        this.vmInstance = instance;
				        this.partition = instance.getPlacement();
				        if(this.loadbalancer != null) {
				          if(this.loadbalancer.getVpcId() == null)
				            this.ipAddress = instance.getIpAddress();
				          else
				            this.ipAddress = instance.getPrivateIpAddress();
				        }
				        break;
				      }
				}
			}
		}catch(Exception ex){
			;
		}
		return this.vmInstance;
	}

	private void setLoadBalancer(final LoadBalancer lb){
		this.loadbalancer = lb;
	}
	
	public LoadBalancerCoreView getLoadBalancer(){
		return this.view.getLoadBalancer();
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
   
    public LoadBalancerZoneCoreView getAvailabilityZone(){
    	return this.view.getZone();
    } 
    
    public void updateInstanceStateTimestamp(){
    	final long currentTime = System.currentTimeMillis();
    	this.instanceUpdateTimestamp = new Date(currentTime);
    }
    
    public Date instanceStateLastUpdated(){
      return this.instanceUpdateTimestamp;
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
	
	public void setPartition(final String partition) {
	  this.partition = partition;
	}
	
	public String getIpAddress(){
	    return this.ipAddress;
	}

	public void setIpAddress(final String ipAddress) {
	  this.ipAddress = ipAddress;
	}
	
	@Override
	public FullName getFullName() {
		return FullName.create.vendor( "euca" )
                .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                .namespace( this.getOwnerAccountNumber( ) )
                .relativeId( "loadbalancer-backend-instance", this.vmInstance.getInstanceId()!=null ? this.vmInstance.getInstanceId() : "");
	}
	
	@Override 
	public boolean equals(Object obj){
		if(obj==null)
			return false;
		if(obj.getClass() != LoadBalancerBackendInstance.class)
			return false;
		final LoadBalancerBackendInstance other = (LoadBalancerBackendInstance) obj;
		
		if(this.loadbalancer == null){
			if(other.loadbalancer!=null)
				return false;
		}else if(this.loadbalancer.getOwnerUserId() == null){
			if(other.loadbalancer.getOwnerUserId()!=null)
				return false;
		}else if(! this.loadbalancer.getOwnerUserId().equals(other.loadbalancer.getOwnerUserId()))
			return false;
		
		if(this.loadbalancer == null){
			if(other.loadbalancer!=null)
				return false;
		}else if(this.loadbalancer.getDisplayName() == null){
			if(other.loadbalancer.getDisplayName()!=null)
				return false;
		}else if(! this.loadbalancer.getDisplayName().equals(other.loadbalancer.getDisplayName()))
			return false;
		
		if(this.displayName == null){
			if(other.displayName != null)
				return false;
		}else if(!this.displayName.equals(other.displayName))
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode( ) {
		final int prime = 31;
		int result = super.hashCode( );
		result = prime * result + ( ( this.loadbalancer == null || this.loadbalancer.getOwnerUserId() == null )
	                                                     ? 0
	                                                     : this.loadbalancer.getOwnerUserId().hashCode());
		result = prime * result + ( ( this.loadbalancer == null || this.loadbalancer.getDisplayName() == null )
											             ? 0
											             : this.loadbalancer.getDisplayName().hashCode());	
		result = prime * result + ( ( this.displayName == null )
										                 ? 0
										                 : this.displayName.hashCode());	
		return result;
	}
	
	@Override
	public String toString(){
		return String.format("%s backend instance - %s", this.loadbalancer, this.getDisplayName());
	}
	
	
	@Override
	protected String createUniqueName( ) {
	    return ( this.loadbalancer != null && this.getDisplayName( ) != null)
	      ? this.loadbalancer.getOwnerAccountNumber( ) + ":" + this.loadbalancer.getDisplayName()+":"+this.getDisplayName( )
	      : null;
	}
	
	/*
	 * Note that the backend instance check does not affect the health check result of the instances.
	 * the health check is left to the "ping" mechanism by the servo. the state update here is the mean
	 * by which to include only the non-faulty instances in the list delivered to servo.
	 */
	public static class BackendInstanceChecker implements EventListener<ClockTick> {
		private final static int CHECK_EVERY_SECONDS = 120;
		private final static int NUM_INSTANCES_TO_DESCRIBE = 8;

		private static Date lastCheckTime = new Date(System.currentTimeMillis());
		public static void register(){
			Listeners.register(ClockTick.class, new BackendInstanceChecker() );
		}
		
		@Override
		public void fireEvent(ClockTick event) {
			if (!( Bootstrap.isOperational() &&
			          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
			          Topology.isEnabled( Compute.class ) ))
				return;

			final Date now = new Date(System.currentTimeMillis());
			final int elapsedSec =  (int)((now.getTime() - lastCheckTime.getTime())/1000.0);
			if(elapsedSec < CHECK_EVERY_SECONDS)
			  return;
			lastCheckTime = now;
			
			/// determine the BE instances to query
			final List<LoadBalancerBackendInstance> allInstances = Lists.newArrayList();
			//final List<LoadBalancerBackendInstance> stateOutdated = Lists.newArrayList();
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
				allInstances.addAll(
						Entities.query(LoadBalancerBackendInstance.named()));
			}catch(final Exception ex){
			}

			final List<LoadBalancerBackendInstance> stateOutdated = allInstances;
			final Set<String> instancesToCheck = 
					Sets.newHashSet(Lists.transform(stateOutdated, new Function<LoadBalancerBackendInstance,String>(){
						@Override
						@Nullable
						public String apply(
								@Nullable LoadBalancerBackendInstance arg0) {
							return arg0.getInstanceId();
						}
			}));
			
			final List<RunningInstancesItemType> result  = Lists.newArrayList();
			for(final List<String> partition : Iterables.partition(instancesToCheck, NUM_INSTANCES_TO_DESCRIBE)) {
			  try{
			    result.addAll(
			        EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(partition));
			  }catch(final Exception ex){
			    LOG.warn("failed to query instances", ex);
			    break;
			  }
			}
			
			//EUCA-9919: remove registered instances when terminated
			final Set<String> instancesToDelete = 
			    Sets.newHashSet();
			final Map<String, STATE> stateMap = new HashMap<String, STATE>();
			final Map<String, RunningInstancesItemType> instanceMap = 
			    new HashMap<String, RunningInstancesItemType>();
			for(final RunningInstancesItemType instance : result){
				final String state = instance.getStateName();
				if("pending".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.OutOfService);
				else if("running".equals(state)){
					instanceMap.put(instance.getInstanceId(), instance);
				}else if("shutting-down".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
				else if("terminated".equals(state)) {
					stateMap.put(instance.getInstanceId(), STATE.Error);
					instancesToDelete.add(instance.getInstanceId());
				}else if("stopping".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
				else if("stopped".equals(state))
					stateMap.put(instance.getInstanceId(), STATE.Error);
			}
			
			final List<LoadBalancerBackendInstance> beToDelete = Lists.newArrayList();

			for(final LoadBalancerBackendInstance be : stateOutdated){
			  if(instancesToDelete.contains(be.getInstanceId())){
			    beToDelete.add(be);
			    continue;
			  }
			  if(stateMap.containsKey(be.getInstanceId())){ // OutOfService || Error
			    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
			      final STATE trueState = stateMap.get(be.getInstanceId());
			      final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
			      update.setBackendState(trueState);
			      Entities.persist(update);
			      db.commit();
			    }catch(final Exception ex) {
			      ;
			    }finally{
			      LoadBalancingServoCache.getInstance().invalidate(be);
			    }
			  }else if (instanceMap.containsKey(be.getInstanceId())) {
			    String instanceIpAddress = null;
			    if (be.getLoadBalancer().getVpcId() == null)
			      instanceIpAddress = instanceMap.get(be.getInstanceId()).getIpAddress();
			    else
			      instanceIpAddress = instanceMap.get(be.getInstanceId()).getPrivateIpAddress();
			    if(instanceIpAddress==null) {
			      LOG.warn(String.format("Failed to determine ELB backend instance's IP address: %s", 
			          be.getInstanceId()));
			    }else if(!instanceIpAddress.equals(be.getIpAddress())) {
			      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
			        final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
			        update.setIpAddress(instanceIpAddress);
			        update.setPartition(instanceMap.get(be.getInstanceId()).getPlacement());
			        Entities.persist(update);
			        db.commit();
			      }catch(final Exception ex) {
			        ;
			      }finally{
			        LoadBalancingServoCache.getInstance().invalidate(be);
			      }
			    }
			  }
			}

			for(final LoadBalancerBackendInstance be : beToDelete) {
			  try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
			    final LoadBalancerBackendInstance entity = Entities.uniqueResult(be);
			    Entities.delete(entity);
			    LOG.info("Instance "+be.getInstanceId()+" is terminated and removed from ELB");
			    db.commit();
			  }catch(final Exception ex) {
			    ;
			  }finally{
			    LoadBalancingServoCache.getInstance().invalidate(be);
			  }
			}
		}
	}
	
	public static class LoadBalancerBackendInstanceCoreView {
		private LoadBalancerBackendInstance instance = null;
		LoadBalancerBackendInstanceCoreView(LoadBalancerBackendInstance instance){
			this.instance = instance;
		}
		
		public String getDisplayName(){
			return this.instance.getDisplayName();
		}
		
		public STATE getState(){
			return this.instance.getState();
		}
		
		public String getInstanceId(){
			return this.instance.getInstanceId();
		}
		
		public STATE getBackendState(){
			return this.instance.getBackendState();
		}
	    
		public String getReasonCode(){
			return this.instance.getReasonCode();
		}
	    
		public String getDescription(){
			return this.instance.getDescription();
		}
	    
		public String getIpAddress(){
			return this.instance.getIpAddress();
		}
		
		public String getPartition(){
		  return this.instance.getPartition();
		}
		
		public Date instanceStateLastUpdated(){
			return this.instance.instanceUpdateTimestamp;
		}
		
		public Date instanceCreationTimestamp(){
		  return this.instance.getCreationTimestamp();
		}

		public static NonNullFunction<LoadBalancerBackendInstanceCoreView,String> instanceId( ) {
			return StringPropertyFunctions.InstanceId;
		}

		private enum StringPropertyFunctions implements NonNullFunction<LoadBalancerBackendInstanceCoreView,String> {
			InstanceId {
				@Nonnull
				@Override
				public String apply( final LoadBalancerBackendInstanceCoreView loadBalancerBackendInstanceCoreView ) {
					return loadBalancerBackendInstanceCoreView.getInstanceId( );
				}
			},
		}
	}

	@TypeMapper
	public enum LoadBalancerBackendInstanceCoreViewTransform implements Function<LoadBalancerBackendInstance, LoadBalancerBackendInstanceCoreView> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerBackendInstanceCoreView apply(
				@Nullable LoadBalancerBackendInstance arg0) {
			return new LoadBalancerBackendInstanceCoreView(arg0);
		}
	}

	public enum LoadBalancerBackendInstanceEntityTransform implements Function<LoadBalancerBackendInstanceCoreView, LoadBalancerBackendInstance> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerBackendInstance apply(
				@Nullable LoadBalancerBackendInstanceCoreView arg0) {
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
				return Entities.uniqueResult(arg0.instance);
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}	
	}
	
	private static class LoadBalancerBackendInstanceRelationView  {
		private LoadBalancerZoneCoreView zone = null;
		private LoadBalancer loadbalancer = null;
		
		LoadBalancerBackendInstanceRelationView(
				LoadBalancerBackendInstance instance) {
			if(instance.zone!=null)
				zone = TypeMappers.transform(instance.zone, LoadBalancerZoneCoreView.class);
			if(instance.loadbalancer!=null) {
				Entities.initialize( instance.loadbalancer );
				loadbalancer = instance.loadbalancer;
			}
		}
		
		public LoadBalancerZoneCoreView getZone(){
			return this.zone;
		}
		
		public LoadBalancerCoreView getLoadBalancer(){
			return this.loadbalancer.getCoreView( );
		}
		
	}
}
