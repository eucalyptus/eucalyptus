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

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parent;

import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;

@Embeddable
public class LoadBalancerPolicyAttributeTypeDescription {
	public enum Cardinality {
		   ONE ( "ONE" ), ZERO_OR_ONE( "ZERO_OR_ONE" ), ZERO_OR_MORE( "ZERO_OR_MORE"), ONE_OR_MORE("ONE_OR_MORE");
		   
		   private String strState = null;
		   private Cardinality(final String state){
			   strState = state;
		   }
		   @Override
		   public String toString(){
			   return strState;
		   }
	}
	
	@Parent
	private LoadBalancerPolicyTypeDescription policyType = null;
	
	@Column( name = "attribute_name", nullable=true)
	private String attributeName = null;

	@Column( name = "attribute_type", nullable=true)
	private String attributeType = null;

	@Column( name = "cardinality", nullable=true)
	private String cardinality = null;
	
	@Column( name = "default_value", nullable=true)
	private String defaultValue = null;

	@Column( name = "description", nullable=true)
	private String description = null;
	
	public LoadBalancerPolicyAttributeTypeDescription(){ }
	
	public LoadBalancerPolicyAttributeTypeDescription(final String name, final String type){
	  this.attributeName=name;
	  this.attributeType=type;
	}
	public LoadBalancerPolicyAttributeTypeDescription(final String name, final String type, final Cardinality c){
    this.attributeName=name;
    this.attributeType=type;
    this.cardinality = c.toString();
  }
	public LoadBalancerPolicyAttributeTypeDescription(final String name, final String type, final Cardinality c, final String description){
    this.attributeName=name;
    this.attributeType=type;
    this.cardinality = c.toString();
    this.description = description;
  }
	
	public LoadBalancerPolicyTypeDescription getPolicyType(){
	  return this.policyType;
	}
	
	public void setPolicyType(final LoadBalancerPolicyTypeDescription type){
	  this.policyType = type;
	}
	
	private String getAttributeName(){
	  return this.attributeName;
	}
	
	private String getAttributeType(){
	  return this.attributeType;
	}
	
	public void setCardinality(final Cardinality c){
	  this.cardinality = c.toString();
	}
	
	private String getCardinality(){
	  return this.cardinality;
	}
	
	public void setDefaultValue(final String defaultValue){
	  this.defaultValue = defaultValue;
	}
	
	private String getDefaultValue(){
	  return this.defaultValue;
	}
	
	public void setDescription(final String description){
	  this.description = description;
	}
	
	private String getDescription(){
	  return this.description;
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
    LoadBalancerPolicyAttributeTypeDescription other = ( LoadBalancerPolicyAttributeTypeDescription ) obj;
    if ( this.attributeName == null ) {
      if ( other.attributeName != null ) {
        return false;
      }
    } else if ( !this.attributeName.equals( other.attributeName ) ) {
      return false;
    }
    
    if ( this.attributeType == null ) {
      if ( other.attributeType != null ) {
        return false;
      }
    } else if ( !this.attributeType.equals( other.attributeType ) ) {
      return false;
    }
    
    return true;
	}
	
	@Override
	public int hashCode(){
	  final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.attributeName == null )
      ? 0
      : this.attributeName.hashCode( ) );
    result = prime * result +  ( ( this.attributeType == null )
      ? 0
      : this.attributeType.hashCode( ) );
    return result;
	}
	
	public static class LoadBalancerPolicyAttributeTypeDescriptionCoreView {
	  LoadBalancerPolicyAttributeTypeDescription description = null;
	  public LoadBalancerPolicyAttributeTypeDescriptionCoreView(final LoadBalancerPolicyAttributeTypeDescription description){
	    this.description = description;
	  }
	  
	  public String getAttributeName(){
	    return this.description.getAttributeName();
	  }
	  
	  public String getAttributeType(){
	    return this.description.getAttributeType();
	  }
	  
	  public String getCardinality(){
	    return this.description.getCardinality();
	  }
	  
	  public String getDefaultValue(){
	    return this.description.getDefaultValue();
	  }
	  
	  public String getDescription(){
	    return this.description.getDescription();
	  }
	}
	
	@TypeMapper
	public enum  LoadBalancerPolicyAttributeTypeDescriptionCoreViewTransform implements Function<LoadBalancerPolicyAttributeTypeDescription, LoadBalancerPolicyAttributeTypeDescriptionCoreView> {
	  INSTANCE;

    @Override
    public LoadBalancerPolicyAttributeTypeDescriptionCoreView apply(
      LoadBalancerPolicyAttributeTypeDescription arg0) {
        return new LoadBalancerPolicyAttributeTypeDescriptionCoreView(arg0);
    }
	}
	
}
