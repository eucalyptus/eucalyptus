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

import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeDescription.LoadBalancerPolicyAttributeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeDescription.LoadBalancerPolicyAttributeDescriptionCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeDescription.LoadBalancerPolicyAtttributeDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.LoadBalancerPolicyAttributeTypeDescriptionCoreView;
import com.eucalyptus.loadbalancing.backend.InvalidConfigurationRequestException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_policy_description" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerPolicyDescription extends AbstractPersistent {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicyDescription.class );
  
  @Transient
  private static final long serialVersionUID = 1L;
  
  @Transient
  private LoadBalancerPolicyDescriptionRelationView view = null;
  
  @ManyToOne
  @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private LoadBalancer loadbalancer = null;
  
  @ManyToMany( fetch = FetchType.LAZY, mappedBy="policies", cascade = CascadeType.REMOVE )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<LoadBalancerListener> listeners = null;
  
  @Column( name = "policy_name", nullable=false)
  private String policyName = null;
  
  @Column( name = "policy_type_name", nullable=true)
  private String policyTypeName = null;

  @Column( name = "unique_name", unique=true, nullable=false)
  private String uniqueName = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "policyDescription")
  @Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<LoadBalancerPolicyAttributeDescription> policyAttrDescription = null;
  
  @Transient
  private LoadBalancerPolicyTypeDescription policyType = null;
  
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
  
  public LoadBalancerPolicyTypeDescription getPolicyTypeDescription(){
    if(this.policyType == null){
      this.policyType = LoadBalancerPolicies.findLoadBalancerPolicyTypeDescription(this.policyTypeName);
    }
    return this.policyType;
  }
  
  public void addPolicyAttributeDescription(final String attrName, final String attrValue) 
      throws InvalidConfigurationRequestException {
    if(this.getPolicyTypeDescription() != null){
      LoadBalancerPolicyAttributeTypeDescriptionCoreView attrType = null;
      for(final LoadBalancerPolicyAttributeTypeDescriptionCoreView type: this.policyType.getPolicyAttributeTypeDescriptions()){
        if(attrName.equals(type.getAttributeName())){
          attrType = type;
          break;
        }
      }
      if(attrType==null)
         throw new InvalidConfigurationRequestException(String.format("Attribute %s is not defined in the policy type", attrName));
      if(!LoadBalancerPolicies.isAttributeValueValid(attrType.getAttributeType(), attrType.getCardinality(), attrValue))
        throw new InvalidConfigurationRequestException(String.format("Attribute value %s is not valid", attrValue));
    }
    
    final LoadBalancerPolicyAttributeDescription attr = new LoadBalancerPolicyAttributeDescription(this, attrName, attrValue);
    if(this.policyAttrDescription == null)
      this.policyAttrDescription = Lists.newArrayList();
    this.removePolicyAttributeDescription(attr);
    this.policyAttrDescription.add(attr);
  }
  
  public void removePolicyAttributeDescription(final LoadBalancerPolicyAttributeDescription desc){
    if(this.policyAttrDescription == null)
      return;
    this.policyAttrDescription.remove(desc);
  }
  
  public List<LoadBalancerPolicyAttributeDescriptionCoreView> getPolicyAttributeDescription(){
    return this.view.getPolicyAttributeDescription();
  }
  
  public LoadBalancerPolicyAttributeDescription findAttributeDescription(final String attrName) throws NoSuchElementException{
    for (final LoadBalancerPolicyAttributeDescriptionCoreView attrView : this.getPolicyAttributeDescription()){
      if(attrView.getAttributeName().equals(attrName))
        return LoadBalancerPolicyAtttributeDescriptionEntityTransform.INSTANCE.apply(attrView);
    }
    throw new NoSuchElementException();
  }
  
  public List<LoadBalancerListenerCoreView> getListeners(){
    return this.view.getListeners();
  }
  
  @PostLoad
  private void onLoad(){
    if(this.view==null)
      this.view = new LoadBalancerPolicyDescriptionRelationView(this);
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
  
  public static class LoadBalancerPolicyDescriptionRelationView{
    private LoadBalancerPolicyDescription policyDesc = null;
    private ImmutableList<LoadBalancerPolicyAttributeDescriptionCoreView> policyAttrDesc = null;
    private ImmutableList<LoadBalancerListenerCoreView> listeners = null;
    LoadBalancerPolicyDescriptionRelationView(final LoadBalancerPolicyDescription desc){
      this.policyDesc = desc;
      if(desc.policyAttrDescription != null)
        this.policyAttrDesc = ImmutableList.copyOf(Collections2.transform(desc.policyAttrDescription,
            LoadBalancerPolicyAttributeDescriptionCoreViewTransform.INSTANCE));
      if(desc.listeners!= null)
        this.listeners = ImmutableList.copyOf(Collections2.transform(desc.listeners, 
            LoadBalancerListenerCoreViewTransform.INSTANCE));
    }
    
    public ImmutableList<LoadBalancerPolicyAttributeDescriptionCoreView> getPolicyAttributeDescription(){
      return this.policyAttrDesc;
    }
    
    public ImmutableList<LoadBalancerListenerCoreView> getListeners(){
      return this.listeners;
    }
  }
  
  public static class LoadBalancerPolicyDescriptionCoreView {
    private LoadBalancerPolicyDescription policyDesc = null;
    LoadBalancerPolicyDescriptionCoreView(final LoadBalancerPolicyDescription desc){
      this.policyDesc = desc;
    }
    
    public String getPolicyName(){
      return this.policyDesc.policyName;
    }
    
    public String getPolicyTypeName(){
      return this.policyDesc.policyTypeName;
    }
  }
  
  public enum LoadBalancerPolicyDescriptionCoreViewTransform implements 
    Function<LoadBalancerPolicyDescription, LoadBalancerPolicyDescriptionCoreView>{
    INSTANCE;

    @Override
    public LoadBalancerPolicyDescriptionCoreView apply(
        LoadBalancerPolicyDescription arg0) {
      return new LoadBalancerPolicyDescriptionCoreView(arg0);
    }
  }
  
  public enum LoadBalancerPolicyDescriptionEntityTransform implements
    Function<LoadBalancerPolicyDescriptionCoreView, LoadBalancerPolicyDescription>{
    INSTANCE;

    @Override
    public LoadBalancerPolicyDescription apply(
        LoadBalancerPolicyDescriptionCoreView arg0) {
      final EntityTransaction db = Entities.get(LoadBalancerPolicyDescription.class);
      LoadBalancerPolicyDescription policy = null;
      try{
        policy = Entities.uniqueResult(arg0.policyDesc);
        db.commit();
      }catch(final NoSuchElementException ex){
        throw ex;
      }catch (final Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }finally{
        if(db.isActive())
          db.rollback();
      }
      return policy;
    }
  }
}
