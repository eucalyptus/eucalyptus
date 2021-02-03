/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionView;
import com.google.common.collect.Lists;


@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_policy_description" )
public class LoadBalancerPolicyDescription extends AbstractPersistent implements LoadBalancerPolicyDescriptionView {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicyDescription.class );

  private static final long serialVersionUID = 1L;
  
  @ManyToOne
  @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
  private LoadBalancer loadbalancer = null;
  
  @ManyToMany( fetch = FetchType.LAZY, mappedBy="policies")
  private List<LoadBalancerListener> listeners = null;

  @ManyToMany( fetch = FetchType.LAZY, mappedBy="policyDescriptions" )
  private List<LoadBalancerBackendServerDescription> backendServers = null;
  
  @Column( name = "policy_name", nullable=false)
  private String policyName = null;
  
  @Column( name = "policy_type_name" )
  private String policyTypeName = null;

  @Column( name = "unique_name", unique=true, nullable=false)
  private String uniqueName = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "policyDescription")
  private List<LoadBalancerPolicyAttributeDescription> policyAttrDescription = null;
  
  private LoadBalancerPolicyDescription(){}
  
  private LoadBalancerPolicyDescription(final LoadBalancer lb, final String policyName){
    this.loadbalancer = lb;
    this.policyName = policyName;
  }
  
  public LoadBalancerPolicyDescription(final LoadBalancer lb, final String policyName, final String policyTypeName){
    this(lb, policyName);
    this.policyTypeName = policyTypeName;
  }
  
  public LoadBalancerPolicyDescription(final LoadBalancer lb, final String policyName, 
      final String policyTypeName, final List<LoadBalancerPolicyAttributeDescription> descs){
    this(lb, policyName, policyTypeName);
    this.policyAttrDescription = descs;
  } 
  
  public static LoadBalancerPolicyDescription named(final LoadBalancer lb, final String policyName){
    final LoadBalancerPolicyDescription instance = new LoadBalancerPolicyDescription(lb, policyName);
    instance.uniqueName = instance.createUniqueName();
    return instance;
  }
  
  public String getPolicyName(){
    return this.policyName;
  }
  
  public String getPolicyTypeName(){
    return this.policyTypeName;
  }
  
  public void addPolicyAttributeDescription(final String attrName, String attrValue) {
    final LoadBalancerPolicyAttributeDescription attr = new LoadBalancerPolicyAttributeDescription(this, attrName, attrValue);
    if(this.policyAttrDescription == null)
      this.policyAttrDescription = Lists.newArrayList();
    this.policyAttrDescription.add(attr);
  }

  public List<LoadBalancerPolicyAttributeDescription> getPolicyAttributeDescriptions(){
    return this.policyAttrDescription;
  }

  public List<LoadBalancerListener> getListeners(){
    return this.listeners;
  }
  
  public List<LoadBalancerBackendServerDescription> getBackendServers(){
    return this.backendServers;
  }
  
  @PrePersist
  private void generateOnCommit( ) {
    if(this.uniqueName==null)
      this.uniqueName = createUniqueName( );
  }

  protected String createUniqueName( ) {
    return String.format("policy-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.policyName);
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
    final LoadBalancerPolicyDescription other = (LoadBalancerPolicyDescription) obj;
    if(this.loadbalancer==null){
      if( other.loadbalancer!=null){
        return false;
      }
    }else if(!this.loadbalancer.equals(other.loadbalancer)){
      return false;
    }
    
    if ( this.policyName == null ) {
      if ( other.policyName != null ) {
        return false;
      }
    } else if ( !this.policyName.equals( other.policyName ) ) {
      return false;
    }
    
    return true;
  }
  
  public final String getRecordId(){
    return this.getId();
  }
  
  @Override
  public int hashCode(){
    final int prime = 31;
    int result = 1;

    result = prime * result +  ( ( this.loadbalancer == null )
      ? 0
      : this.loadbalancer.hashCode( ) );
    
    result = prime * result + ( ( this.policyName == null )
      ? 0
      : this.policyName.hashCode( ) );
    return result;
  }
  
  @Override
  public String toString(){
    return String.format("LoadBalancer Policy Description for (%s):%s-%s", this.loadbalancer, this.policyName, this.policyTypeName);
  }
}
