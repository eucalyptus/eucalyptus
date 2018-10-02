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

package com.eucalyptus.loadbalancing;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.LoadBalancerPolicyAttributeTypeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.LoadBalancerPolicyAttributeTypeDescriptionCoreViewTransform;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_policy_type_description" )
public class LoadBalancerPolicyTypeDescription extends AbstractPersistent{
	private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicyTypeDescription.class );

	private static final long serialVersionUID = 1L;
	
	@Transient
	private LoadBalancerPolicyTypeDescriptionRelationView view = null;
	
	@Column( name = "description", nullable=true)
	private String description = null;
	
	@Column( name = "policy_type_name", nullable=true)
	private String policyTypeName = null;
	
	@ElementCollection
	@CollectionTable( name = "metadata_policy_attr_type_description" )
	private List<LoadBalancerPolicyAttributeTypeDescription> policyAttributeTypeDescriptions = null;
	
	public LoadBalancerPolicyTypeDescription(){ }
	
	public LoadBalancerPolicyTypeDescription(final String typeName){
	  this.policyTypeName = typeName;
	  this.view = new LoadBalancerPolicyTypeDescriptionRelationView(this);
	}
  
	public LoadBalancerPolicyTypeDescription(final String typeName, final String description){
    this(typeName);
    this.description = description;
  }
  
  public LoadBalancerPolicyTypeDescription(final String typeName, final String description, 
      final List<LoadBalancerPolicyAttributeTypeDescription> attributeTypes){
    this(typeName, description);
    this.policyAttributeTypeDescriptions = attributeTypes;
  }
  
  public static LoadBalancerPolicyTypeDescription named(final String typeName){
    return new LoadBalancerPolicyTypeDescription(typeName);
  }

  @PostLoad
  private void onLoad(){
    if(this.view==null)
      this.view = new LoadBalancerPolicyTypeDescriptionRelationView(this);
  }

	public String getPolicyTypeName(){
	  return this.policyTypeName;
	}
	
	public void setDescription(final String description){
	  this.description = description;
	}

	public String getDescription(){
	  return this.description;
	}
	
	public List<LoadBalancerPolicyAttributeTypeDescriptionCoreView> getPolicyAttributeTypeDescriptions(){
	  return this.view.getAttributeTypeDescription();
	}
	
	void addPolicyAttributeTypeDescription(final LoadBalancerPolicyAttributeTypeDescription attrDesc){
	  if(this.policyAttributeTypeDescriptions==null)
	    this.policyAttributeTypeDescriptions = Lists.newArrayList();
	  this.removePolicyAttributeTypeDescription(attrDesc);
	  this.policyAttributeTypeDescriptions.add(attrDesc);
	}
	
	void removePolicyAttributeTypeDescription(final LoadBalancerPolicyAttributeTypeDescription attrDesc){
	   if(this.policyAttributeTypeDescriptions==null)
	     return;
	   this.policyAttributeTypeDescriptions.remove(attrDesc);
	}
	
	@Override
	public String toString(){
	  return String.format("PolicyTypeDescription: %s", this.policyTypeName);
	}
	
	@Override
	public int hashCode(){
	  final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.policyTypeName == null )
      ? 0
      : this.policyTypeName.hashCode( ) );
    return result;
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
    LoadBalancerPolicyTypeDescription other = ( LoadBalancerPolicyTypeDescription ) obj;
    if ( this.policyTypeName == null ) {
      if ( other.policyTypeName != null ) {
        return false;
      }
    } else if ( !this.policyTypeName.equals( other.policyTypeName ) ) {
      return false;
    }
    
    return true;
	}
	
	public static class LoadBalancerPolicyTypeDescriptionRelationView {
	  private ImmutableList<LoadBalancerPolicyAttributeTypeDescriptionCoreView> attrTypeDescs = null;
	  
	  private LoadBalancerPolicyTypeDescriptionRelationView(final LoadBalancerPolicyTypeDescription policyDesc){
	    if(policyDesc.policyAttributeTypeDescriptions != null){
	      attrTypeDescs = ImmutableList.copyOf(Collections2.transform(policyDesc.policyAttributeTypeDescriptions,
	          LoadBalancerPolicyAttributeTypeDescriptionCoreViewTransform.INSTANCE));
	    }
	  }

	  public ImmutableList<LoadBalancerPolicyAttributeTypeDescriptionCoreView> getAttributeTypeDescription(){
	    return this.attrTypeDescs;
	  }
	}
}
