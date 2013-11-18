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

import java.util.NoSuchElementException;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;

/**
 * @author Sang-Min Park
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_policy_attr_description" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerPolicyAttributeDescription extends AbstractPersistent{
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicyAttributeDescription.class );
  
  @Transient
  private static final long serialVersionUID = 1L;
 
  @ManyToOne
  @JoinColumn( name = "metadata_policy_desc_fk", nullable=false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  LoadBalancerPolicyDescription policyDescription = null;
  
  @Column( name = "attribute_name", nullable=true)
  private String attributeName = null;

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
      final EntityTransaction db = Entities.get(LoadBalancerPolicyAttributeDescription.class);
      LoadBalancerPolicyAttributeDescription attr = null;
      try{
        attr = Entities.uniqueResult(arg0.policyDesc);
        db.commit();
      }catch(final NoSuchElementException ex){
        throw ex;
      }catch (final Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }finally{
        if(db.isActive())
          db.rollback();
      }
      return attr;
    }
  }
}
