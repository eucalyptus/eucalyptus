/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendServerDescriptionView;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_backend_server_description" )
public class LoadBalancerBackendServerDescription extends AbstractPersistent implements LoadBalancerBackendServerDescriptionView {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendServerDescription.class );

  private static final long serialVersionUID = 1L;
  
  @ManyToOne
  @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
  private LoadBalancer loadbalancer = null;
  
  @Column( name = "instance_port", nullable=false)
  private Integer instancePort = null;
  
  @Column(name="unique_name", nullable=false, unique=true)
  private String uniqueName = null;
 
  @ManyToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE )
  @JoinTable( name = "metadata_policy_set_for_backends", joinColumns = { @JoinColumn( name = "metadata_backend_fk" ) },  inverseJoinColumns = @JoinColumn( name = "metadata_policy_fk" ) )
  private List<LoadBalancerPolicyDescription> policyDescriptions = null;
  
  private LoadBalancerBackendServerDescription() { }
  
  public LoadBalancerBackendServerDescription(final LoadBalancer lb, final int instancePort){
    this.loadbalancer = lb;
    this.instancePort = instancePort;
  }
  
  public LoadBalancerBackendServerDescription(final LoadBalancer lb, final int instancePort, 
      final List<LoadBalancerPolicyDescription> policyDescriptions){
    this(lb, instancePort);
    this.policyDescriptions = policyDescriptions;
  }
  
  public static LoadBalancerBackendServerDescription named(final LoadBalancer lb, final int instancePort){
    final LoadBalancerBackendServerDescription backend = new LoadBalancerBackendServerDescription(lb, instancePort);
    backend.uniqueName = backend.createUniqueName();
    return backend;
  }
  
  public Integer getInstancePort(){
    return this.instancePort;
  }

  public List<LoadBalancerPolicyDescription> getPolicyDescriptions() {
    return this.policyDescriptions;
  }

  public void addPolicy(final LoadBalancerPolicyDescription policy){
    if(this.policyDescriptions==null){
      this.policyDescriptions = Lists.newArrayList();
    }
    if(!this.policyDescriptions.contains(policy))
      this.policyDescriptions.add(policy);
  }
  
  public void removePolicy(final LoadBalancerPolicyDescription policy){
    if(this.policyDescriptions==null || policy==null)
      return;
    this.policyDescriptions.remove(policy);
  }

  @PrePersist
  private void generateOnCommit( ) {
    if(this.uniqueName==null)
      this.uniqueName = createUniqueName( );
  }

  protected String createUniqueName( ) {
    return String.format("backend-server-%s-%s-%d", this.loadbalancer.getOwnerAccountNumber(), 
        this.loadbalancer.getDisplayName(), 
        this.instancePort);
  }
  
  public String getUniqueName(){
    return this.uniqueName;
  }
  
  @Override
  public boolean equals(final Object obj){
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final LoadBalancerBackendServerDescription other = (LoadBalancerBackendServerDescription) obj;
    if(this.loadbalancer==null){
      if( other.loadbalancer!=null){
        return false;
      }
    }else if(!this.loadbalancer.equals(other.loadbalancer)){
      return false;
    }
    
    if ( this.instancePort == null ) {
      if ( other.instancePort != null ) {
        return false;
      }
    } else if ( !this.instancePort.equals(other.instancePort)) {
      return false;
    }
    
    return true;
  }
  
  
  @Override
  public int hashCode(){
    final int prime = 31;
    int result = 1;

    result = prime * result +  ( ( this.loadbalancer == null )
      ? 0
      : this.loadbalancer.hashCode( ) );
    
    result = prime * result + ( ( this.instancePort == null )
      ? 0
      : this.instancePort.hashCode( ) );
    return result;
  }
  
  @Override
  public String toString(){
    return String.format("[%s] Backend Server Description - instance port: %d", 
        this.loadbalancer, this.instancePort);
  }
}
