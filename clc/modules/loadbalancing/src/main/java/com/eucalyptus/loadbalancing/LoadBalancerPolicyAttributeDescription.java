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
package com.eucalyptus.loadbalancing;

import java.util.NoSuchElementException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_policy_attr_description" )
public class LoadBalancerPolicyAttributeDescription extends AbstractPersistent{
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicyAttributeDescription.class );
  
  @Transient
  private static final long serialVersionUID = 1L;
 
  @ManyToOne
  @JoinColumn( name = "metadata_policy_desc_fk", nullable=false )
  LoadBalancerPolicyDescription policyDescription = null;
  
  @Column( name = "attribute_name", nullable=true)
  private String attributeName = null;

  @Type(type="text")
  @Column( name = "attribute_value", nullable=true)
  private String attributeValue = null;
  
  private LoadBalancerPolicyAttributeDescription(){}
  
  public LoadBalancerPolicyAttributeDescription(final LoadBalancerPolicyDescription policyDesc, final String attrName, final String attrValue){
    this.policyDescription = policyDesc;
    this.attributeName = attrName;
    this.attributeValue = attrValue;
  }
  
  public LoadBalancerPolicyDescription getPolicyDescription(){
    return this.policyDescription;
  }
  
  public void setPolicyDescription( final LoadBalancerPolicyDescription desc ){
    this.policyDescription = desc;
  }
  
  public void setAttributeName(final String attrName){
    this.attributeName = attrName;
  }
  
  public void setAttributeValue(final String attrValue){
    this.attributeValue= attrValue;
  }
  
  public String getAttributeName(){
    return this.attributeName;
  }
  
  public String getAttributeValue(){
    return this.attributeValue;
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
    
    final LoadBalancerPolicyAttributeDescription other = (LoadBalancerPolicyAttributeDescription) obj;
    
    if ( this.policyDescription == null ) {
      if ( other.policyDescription != null ) {
        return false;
      }
    } else if ( !this.policyDescription.equals( other.policyDescription ) ) {
      return false;
    }
    
    if ( this.attributeName == null ) {
      if ( other.attributeName != null ) {
        return false;
      }
    } else if ( !this.attributeName.equals( other.attributeName ) ) {
      return false;
    }
    
    if ( this.attributeValue == null ) {
      if ( other.attributeValue != null ) {
        return false;
      }
    } else if ( !this.attributeValue.equals( other.attributeValue ) ) {
      return false;
    }

    return true;
  }
  
  @Override
  public int hashCode(){
    final int prime = 31;
    int result = 1;
    result = prime * result +  ( ( this.policyDescription == null )
      ? 0
      : this.policyDescription.hashCode( ) );
    
    result = prime * result + ( ( this.attributeName == null )
      ? 0
      : this.attributeName.hashCode( ) );
    
    result = prime * result + ( ( this.attributeValue == null )
        ? 0
        : this.attributeValue.hashCode( ) );
    return result;
  }
  
  @Override
  public String toString(){
    return String.format("LoadBalancer Policy Attribute Description %s: %s-%s", this.policyDescription.getPolicyName(), this.attributeName, this.attributeValue);
  }
  
  public static class LoadBalancerPolicyAttributeDescriptionCoreView {
    private LoadBalancerPolicyAttributeDescription policyDesc = null;
    public LoadBalancerPolicyAttributeDescriptionCoreView(final LoadBalancerPolicyAttributeDescription policyDesc){
     this.policyDesc = policyDesc; 
    }
    
    public String getAttributeName(){
      return this.policyDesc.attributeName;
    }
    
    public String getAttributeValue(){
      return this.policyDesc.attributeValue;
    }
  }
  
  public enum LoadBalancerPolicyAttributeDescriptionCoreViewTransform implements Function<LoadBalancerPolicyAttributeDescription, LoadBalancerPolicyAttributeDescriptionCoreView> {
    INSTANCE;

    @Override
    public LoadBalancerPolicyAttributeDescriptionCoreView apply(
        LoadBalancerPolicyAttributeDescription arg0) {
      return new LoadBalancerPolicyAttributeDescriptionCoreView(arg0);
    }
  }

  public enum LoadBalancerPolicyAtttributeDescriptionEntityTransform implements
  Function<LoadBalancerPolicyAttributeDescriptionCoreView, LoadBalancerPolicyAttributeDescription>{
    INSTANCE;

    @Override
    public LoadBalancerPolicyAttributeDescription apply(
        LoadBalancerPolicyAttributeDescriptionCoreView arg0) {
      try ( TransactionResource db = Entities.transactionFor( LoadBalancerPolicyAttributeDescription.class ) ) {
        return Entities.uniqueResult(arg0.policyDesc);
      }catch(final NoSuchElementException ex){
        throw ex;
      }catch (final Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
}
