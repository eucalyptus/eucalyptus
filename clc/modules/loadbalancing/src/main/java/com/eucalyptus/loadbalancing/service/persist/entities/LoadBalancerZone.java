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
package com.eucalyptus.loadbalancing.service.persist.entities;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZoneView;

/**
 * @author Sang-Min Park
*
*/
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_zone" )
public class LoadBalancerZone extends AbstractPersistent implements LoadBalancerZoneView {

	public enum STATE {
		InService, OutOfService
	}
	
	private static final long serialVersionUID = 1L;

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
	
	
	@Column(name="autoscaling_group")
	private String autoscalingGroup = null;	

	@Column(name="zone_name", nullable=false)
	private String zoneName;

	@Column(name="subnetId")
	private String subnetId;

	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName;
	
	@Column(name="zone_state")
	private String zoneState;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "zone")
	private Collection<LoadBalancerServoInstance> servoInstances = null;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "zone")
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
		
	public String getName( ){
		return this.zoneName;
	}

	public String getSubnetId( ) {
		return subnetId;
	}

  public LoadBalancer getLoadbalancer(){
		return this.loadbalancer;
	} 

	public Collection<LoadBalancerServoInstance> getServoInstances( ) {
		return servoInstances;
	}

	public Collection<LoadBalancerBackendInstance> getBackendInstances(){
		return this.backendInstances;
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
}
