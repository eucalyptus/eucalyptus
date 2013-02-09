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

import javax.persistence.Column;
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
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;

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
	
	public enum STATE {
		InService, OutOfService
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk" )
	private LoadBalancer loadbalancer = null;

    @ManyToOne
    @JoinColumn( name = "metadata_zone_fk")
    private LoadBalancerZone zone = null;
        
    @Transient
    private VmInstance vmInstance = null;
	
	@Column( name = "reason_code", nullable=true)
	private String reasonCode = null;

    private LoadBalancerBackendInstance(){
    	super(null,null);
    }
	
	private LoadBalancerBackendInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId){
		super(userFullName, vmId);
		this.loadbalancer = lb;
		this.setState(STATE.OutOfService);
		this.vmInstance=VmInstances.lookup(vmId);
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId){
		return new LoadBalancerBackendInstance(userFullName, lb, vmId);
	}
	
	public static LoadBalancerBackendInstance newInstance(final OwnerFullName userFullName, final LoadBalancer lb, final String vmId, STATE state){
		LoadBalancerBackendInstance instance= new LoadBalancerBackendInstance(userFullName, lb, vmId);
		instance.setBackendState(state);
		return instance;
	}
	
	public static LoadBalancerBackendInstance named(final OwnerFullName userName, final String vmId){
		return new LoadBalancerBackendInstance(userName, null, vmId);
	}
	
	public String getInstanceId(){
		return this.getDisplayName();
	}
	
	public VmInstance getVmInstance(){
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
    
    public void setAvailabilityZone(final LoadBalancerZone zone){
    	this.zone = zone;
    }
   
    public LoadBalancerZone getAvailabilityZone(){
    	return this.zone;
    }
    
	@Override
	public String getPartition() {
		// TODO Auto-generated method stub
		return this.vmInstance != null ? this.vmInstance.getPartition() : null;
	}

	@Override
	public FullName getFullName() {
		// TODO Auto-generated method stub
		return FullName.create.vendor( "euca" )
                .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                .namespace( this.getOwnerAccountNumber( ) )
                .relativeId( "loadbalancer-backend-instance", this.vmInstance!=null ? this.vmInstance.getDisplayName() : "");
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
}
