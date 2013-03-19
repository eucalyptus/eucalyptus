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

import javax.persistence.CascadeType;
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
import org.jgroups.util.UUID;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;

/**
 * @author Sang-Min Park
*
*/
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_zone" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerZone extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerZone.class );

	@Transient
	private static final long serialVersionUID = 1L;

	private LoadBalancerZone(){}
	
	private LoadBalancerZone(final LoadBalancer lb, final String zone){
		this.loadbalancer = lb;
		this.zoneName=zone;
		this.uniqueName = this.createUniqueName();
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
	
	@Column(name="unique_name", nullable=false)
	private String uniqueName = null;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "zone")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servoInstances = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "zone")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
		
	public String getName(){
		return this.zoneName;
	}
	
	public LoadBalancer getLoadbalancer(){
		return this.loadbalancer;
	}
	
	public Collection<LoadBalancerServoInstance> getServoInstances(){
		return servoInstances;
	}
	
	public Collection<LoadBalancerBackendInstance> getBackendInstances(){
		return backendInstances;
	}

    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("zone-%s-%s", this.loadbalancer.getDisplayName(), this.zoneName);
    }
	  
	@Override
	public String toString(){
		String name="unassigned";
		if(this.loadbalancer!=null && this.zoneName!=null)
			name = String.format("loadbalancer-zone-%s-%s", this.loadbalancer.getDisplayName(), this.zoneName);
		return name;
	}
}
