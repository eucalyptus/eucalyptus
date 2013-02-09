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
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;

import edu.ucsb.eucalyptus.cloud.InvalidParameterValueException;

/**
 * @author Sang-Min Park
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_servo_instance" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerServoInstance extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendInstance.class );
	@Transient
	private static final long serialVersionUID = 1L;
	
	enum STATE {
		Pending, InService, OutOfService
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk" )
    private LoadBalancer loadbalancer = null;
    
    @ManyToOne
    @JoinColumn( name = "metadata_zone_fk")
    private LoadBalancerZone zone = null;
        
    @Column(name="metadata_instance_id", nullable=false, unique=true)
    private String instanceId = null;
    
    @Column(name="metadata_state", nullable=false)
    private String state = null;
    
    @Column(name="metadata_zone_name", nullable=false)
    private String zoneName = null;
    
    @Column(name="metadata_unique_name", nullable=false, unique=true)
    private String uniqueName = null;

    private LoadBalancerServoInstance(){}
    private LoadBalancerServoInstance(final String instanceId, final String zoneName){
    	this.instanceId=instanceId;
    	this.zoneName=zoneName;
    }
    
    public static LoadBalancerServoInstance named(String instanceId){
    	final LoadBalancerServoInstance sample = new LoadBalancerServoInstance();
    	sample.instanceId = instanceId;
    	return sample;
    }
    
    public static LoadBalancerServoInstance getInstance(String instanceId){
    	// return if DB has the instance 
    	try{
    		final LoadBalancerServoInstance servo = Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
    		return servo;
    	}catch(Exception ex){
    		// retrieve VmInstance
    		try{
    			final VmInstance vm = VmInstances.lookup(instanceId);
    			final LoadBalancerServoInstance servo = new LoadBalancerServoInstance(instanceId, vm.getPartition());
    			return servo;
    		}catch(Exception ex2){
    			LOG.error("failed to find vm instance = "+instanceId);
    			return null;
    		}
    	}
    }
    
    public void setLoadbalancer(final LoadBalancer lb){
    	this.loadbalancer = lb;
    }
    
    public LoadBalancer getLoadbalancer(){
    	return this.loadbalancer;
    }
    
    public void setAvailabilityZone(final LoadBalancerZone zone) throws InvalidParameterValueException{
    	if(zone.getName() != this.zoneName){
    		throw new InvalidParameterValueException("Availability zone doesn't match");
    	}
    	this.zone = zone;
    	this.loadbalancer = this.zone.getLoadbalancer();
    }
    
    public LoadBalancerZone getAvailabilityZone(){
    	return this.zone;
    }

	@PrePersist
	private void generateOnCommit( ) {
		this.uniqueName = createUniqueName( );
	}

	private String createUniqueName(){
		return String.format("%s-%s-servo-instance-%d", this.loadbalancer.getDisplayName(), this.zone.getName(), this.instanceId);
	}
	
	@Override
	public int hashCode( ) {
	    final int prime = 31;
	    int result = 0;
	    result = prime * result + ( ( this.uniqueName == null )
	      ? 0
	      : this.uniqueName.hashCode( ) );
	    return result;
	}
	  
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass( ) != obj.getClass( ) ) {
			return false;
		}
		LoadBalancerServoInstance other = ( LoadBalancerServoInstance ) obj;
		if ( this.uniqueName == null ) {
			if ( other.uniqueName != null ) {
				return false;
			}
		} else if ( !this.uniqueName.equals( other.uniqueName ) ) {
			return false;
		}
		return true;
  }
	@Override
	public String toString(){
		return String.format("Servo-instance %s for loadbalancer %s in %s", this.instanceId, this.loadbalancer.getDisplayName(), this.zone.getName());
	}
}
