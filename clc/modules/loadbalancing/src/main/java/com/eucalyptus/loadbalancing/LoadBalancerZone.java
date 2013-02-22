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
	}
	public static LoadBalancerZone newInstance(final LoadBalancer lb, final String zone){
		return new LoadBalancerZone(lb, zone);
	}
	public static LoadBalancerZone named(final LoadBalancer lb, final String zone){
		return new LoadBalancerZone(lb, zone);
	}
	
    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk" )
    private LoadBalancer loadbalancer = null;

	@Column(name="zone_name", nullable=false)
	private String zoneName = null;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "zone")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servoInstances = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "zone")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
	
	@Column(name="metadata_unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	@PrePersist
	private void generateOnCommit( ) {
		this.uniqueName = createUniqueName( );
	}

	private String createUniqueName(){
		return String.format("%s-zone-%s", this.loadbalancer.getDisplayName(), this.zoneName);
	}
	
	public String getName(){
		return this.zoneName;
	}
	
	public LoadBalancer getLoadbalancer(){
		return this.loadbalancer;
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
		LoadBalancerZone other = ( LoadBalancerZone ) obj;
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
		return this.uniqueName;
	}
}
