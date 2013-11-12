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
