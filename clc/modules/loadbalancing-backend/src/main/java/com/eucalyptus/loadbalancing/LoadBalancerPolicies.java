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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeDescription.LoadBalancerPolicyAttributeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.LoadBalancerPolicyAttributeTypeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.backend.DuplicatePolicyNameException;
import com.eucalyptus.loadbalancing.backend.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.backend.LoadBalancingException;
import com.eucalyptus.loadbalancing.backend.PolicyTypeNotFoundException;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyAttributeDescription;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyAttributeDescriptions;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyAttributeTypeDescription;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyAttributeTypeDescriptions;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.backend.msgs.PolicyTypeDescription;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Collections2;
import com.google.common.base.Predicate;
/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerPolicies {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicies.class );

  /**
   * initialize the policy types that ELB will support
   * this method is idempotent 
   */
  public static void initialize(){
    final List<LoadBalancerPolicyTypeDescription> requiredPolicyTypes =
        Lists.newArrayList(initialize40());
   
    for(final LoadBalancerPolicyTypeDescription policyType : requiredPolicyTypes){
      final EntityTransaction db = Entities.get( LoadBalancerPolicyTypeDescription.class );
      try{
        LoadBalancerPolicyTypeDescription found = Entities.uniqueResult(policyType);
        db.commit();
      }catch(final NoSuchElementException ex){
        Entities.persist(policyType);
        db.commit();
        LOG.debug(String.format("New policy type has been added: %s", policyType));
      }catch(final Exception ex){
        db.rollback();
        throw Exceptions.toUndeclared(ex);
      }finally{
        if(db.isActive())
          db.rollback();
      }
    }
  }
  
  // initialize ELB policy types in version 4.0
  private static List<LoadBalancerPolicyTypeDescription> initialize40(){
    final List<LoadBalancerPolicyTypeDescription> requiredPolicyTypes =
        Lists.newArrayList();
    requiredPolicyTypes.add(
        new LoadBalancerPolicyTypeDescription("AppCookieStickinessPolicyType", 
            "Stickiness policy with session lifetimes controlled by the lifetime of the application-generated cookie. This policy can be associated only with HTTP/HTTPS listeners.",
            Lists.newArrayList( 
                new LoadBalancerPolicyAttributeTypeDescription("CookieName", "String", 
                    LoadBalancerPolicyAttributeTypeDescription.Cardinality.ONE))));
    requiredPolicyTypes.add(
        new LoadBalancerPolicyTypeDescription("LBCookieStickinessPolicyType",
            "Stickiness policy with session lifetimes controlled by the browser (user-agent) or a specified expiration period. This policy can be associated only with HTTP/HTTPS listeners.",
            Lists.newArrayList(
                new LoadBalancerPolicyAttributeTypeDescription("CookieExpirationPeriod", "Long",
                    LoadBalancerPolicyAttributeTypeDescription.Cardinality.ZERO_OR_ONE))));
    
    return requiredPolicyTypes;
  }
  
  public static List<LoadBalancerPolicyTypeDescription> getLoadBalancerPolicyTypeDescriptions(){
    final EntityTransaction db = Entities.get( LoadBalancerPolicyTypeDescription.class );
    List<LoadBalancerPolicyTypeDescription> result = null;
    try{
      result = Entities.query(new LoadBalancerPolicyTypeDescription());
      db.commit();
      return result;
    }catch(final NoSuchElementException ex){
      db.rollback();
      return Lists.newArrayList();
    }catch(final Exception ex){
      db.rollback();
      throw ex;
    }finally{
      if(db.isActive())
       db.rollback();
    }
  }
  
  public static LoadBalancerPolicyTypeDescription findLoadBalancerPolicyTypeDescription(final String policyTypeName) 
      throws NoSuchElementException {
    final EntityTransaction db = Entities.get( LoadBalancerPolicyTypeDescription.class );
    LoadBalancerPolicyTypeDescription result = null;
    try{
      result = Entities.uniqueResult(LoadBalancerPolicyTypeDescription.named(policyTypeName));
      db.commit();
      return result;
    }catch(final NoSuchElementException ex){
      db.rollback();
      throw ex;
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
       db.rollback();
    }
  }

  public static boolean isAttributeValueValid(final String attrType, final String cardinality, final String attrValue){
    if(attrType ==null)
      return true;

    try{
      if(attrType.toLowerCase().equals("boolean")){
        Boolean.parseBoolean(attrValue);
      }else if(attrType.toLowerCase().equals("integer")){
        Integer.parseInt(attrValue);
      }else if(attrType.toLowerCase().equals("long")){
        Long.parseLong(attrValue);
      }else if(attrType.toLowerCase().equals("string")){
        ;
      }
    }catch(final Exception ex){
      return false;
    }  
    return true;
  }
  
  public static void addLoadBalancerPolicy(final LoadBalancer lb, final String policyName, final String policyTypeName, 
      final List<PolicyAttribute> attributes) throws LoadBalancingException
  {
      for(final LoadBalancerPolicyDescriptionCoreView current : lb.getPolicies()){
        if(policyName.equals(current.getPolicyName()))
          throw new DuplicatePolicyNameException();
      }
      
      boolean typeFound=false;
      for(final LoadBalancerPolicyTypeDescription type : getLoadBalancerPolicyTypeDescriptions()){
          if(policyTypeName.equals(type.getPolicyTypeName())){
            typeFound=true;
            break;
          }
      }
      if(!typeFound)
        throw new PolicyTypeNotFoundException();
      final LoadBalancerPolicyDescription policyDesc = new LoadBalancerPolicyDescription(lb, policyName, policyTypeName); 
      
      for(final PolicyAttribute attr : attributes){
        policyDesc.addPolicyAttributeDescription(attr.getAttributeName(), attr.getAttributeValue());
      }
      final EntityTransaction db = Entities.get(LoadBalancerPolicyDescription.class);
      try{
        Entities.persist(policyDesc);
        db.commit();
      }catch(final Exception ex){
        db.rollback();
        throw ex;
      }finally{
        if(db.isActive())
          db.rollback();
      }
  }
  
  public static void deleteLoadBalancerPolicy(final LoadBalancer lb, final String policyName)
    throws LoadBalancingException
  {
    // FIXME: spark - for some reason, Entities.delete does not delete the queried object
    // To work around, had to use deleteAll with where clause
    final List<LoadBalancerPolicyDescription> policies= 
        getLoadBalancerPolicyDescription(lb, Lists.newArrayList(policyName));
    if(policies == null || policies.size()<=0)
      return;
    final LoadBalancerPolicyDescription toDelete = policies.get(0);
    
    // check policy - listener association
    final List<LoadBalancerListenerCoreView> listeners = toDelete.getListeners();
    if(listeners!=null && listeners.size()>0)
      throw new InvalidConfigurationRequestException("The policy is enabled for listeners");
    
    EntityTransaction db = Entities.get(LoadBalancerPolicyAttributeDescription.class);
    try{
      final Map<String, String> criteria = new HashMap<String, String>();
      criteria.put("metadata_policy_desc_fk", toDelete.getRecordId());
      Entities.deleteAllMatching(LoadBalancerPolicyAttributeDescription.class, 
          "WHERE metadata_policy_desc_fk = :metadata_policy_desc_fk", criteria);
      db.commit();
    }catch(final NoSuchElementException ex){
      db.rollback();
    }catch(final Exception ex){
      LOG.error("Failed to delete policy attributes", ex);
      db.rollback();
    }finally{
      if(db.isActive())
        db.rollback();
    }
   
    db = Entities.get(LoadBalancerPolicyDescription.class);
    try{
      final Map<String, String> criteria = new HashMap<String, String>();
      criteria.put("unique_name", toDelete.getUniqueName());
      Entities.deleteAllMatching(LoadBalancerPolicyDescription.class, "WHERE unique_name = :unique_name", criteria);
      db.commit();
    }catch(final NoSuchElementException ex){
      db.rollback();
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static List<LoadBalancerPolicyDescription> getLoadBalancerPolicyDescription(final LoadBalancer lb){
   final List<LoadBalancerPolicyDescriptionCoreView> policyViews = Lists.newArrayList(lb.getPolicies());
   final List<LoadBalancerPolicyDescription> policies = Lists.newArrayList();
   for(final LoadBalancerPolicyDescriptionCoreView policyView: policyViews){
     policies.add(LoadBalancerPolicyDescriptionEntityTransform.INSTANCE.apply(policyView));
   }
   return policies;
  }
  
  public static LoadBalancerPolicyDescription getLoadBalancerPolicyDescription(final LoadBalancer lb, final String policyName)
    throws NoSuchElementException
  {
    LoadBalancerPolicyDescription policy = null;
    for(final LoadBalancerPolicyDescriptionCoreView p : lb.getPolicies()){
      if(p.getPolicyName().equals(policyName)){
        policy = LoadBalancerPolicyDescriptionEntityTransform.INSTANCE.apply(p);
        break;
      }
    }
    if(policy!=null)
      return policy;
    else
      throw new NoSuchElementException();
  }
  
  public static List<LoadBalancerPolicyDescription> getLoadBalancerPolicyDescription(final LoadBalancer lb, final List<String> policyNames){
    final List<LoadBalancerPolicyDescription> allPolicies = getLoadBalancerPolicyDescription(lb);
    final List<LoadBalancerPolicyDescription> filtered = Lists.newArrayList(Collections2.filter(allPolicies, new Predicate<LoadBalancerPolicyDescription>(){
      @Override
      public boolean apply(LoadBalancerPolicyDescription arg0) {
        return policyNames.contains(arg0.getPolicyName());
      }
    }));
    return filtered;
  }
  
  public static List<LoadBalancerPolicyDescription> getPoliciesOfListener(final LoadBalancerListener listener){
    final EntityTransaction db = Entities.get(LoadBalancerListener.class);
    try{
      final LoadBalancerListener found = Entities.uniqueResult(listener);
      final List<LoadBalancerPolicyDescriptionCoreView> policies=found.getPolicies();
      db.commit();
      return Lists.transform(policies, LoadBalancerPolicyDescriptionEntityTransform.INSTANCE);
    }catch(final NoSuchElementException ex){
      db.rollback();
      return Lists.newArrayList();
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static void removePoliciesFromListener(final LoadBalancerListener listener){
    final EntityTransaction db = Entities.get(LoadBalancerListener.class);
    try{
      final LoadBalancerListener update = Entities.uniqueResult(listener);
      update.resetPolicies();
      Entities.persist(update);
      db.commit();
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static void removePolicyFromListener(final LoadBalancerListener listener, final LoadBalancerPolicyDescription policy){
    final EntityTransaction db = Entities.get(LoadBalancerListener.class);
    try{
      final LoadBalancerListener update = Entities.uniqueResult(listener);
      update.removePolicy(policy);
      Entities.persist(update);
      db.commit();
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static void addPoliciesToListener(final LoadBalancerListener listener, 
      final List<LoadBalancerPolicyDescription> policies) throws LoadBalancingException{
    // either one not both of LBCookieStickinessPolicy and AppCookieStickinessPolicy is allowed
    if(policies!=null && policies.size()>0){
      int numCookies = 0;
      for(final LoadBalancerPolicyDescription policy : policies){
        if("LBCookieStickinessPolicyType".equals(policy.getPolicyTypeName())){
          numCookies ++;
          if( !( listener.getProtocol().equals(PROTOCOL.HTTP) || listener.getProtocol().equals(PROTOCOL.HTTPS)))
            throw new InvalidConfigurationRequestException("Session stickiness policy can be associated with only HTTP/HTTPS");
        }
        else if("AppCookieStickinessPolicyType".equals(policy.getPolicyTypeName())){
          numCookies ++;
          if( !( listener.getProtocol().equals(PROTOCOL.HTTP) || listener.getProtocol().equals(PROTOCOL.HTTPS)))
            throw new InvalidConfigurationRequestException("Session stickiness policy can be associated with only HTTP/HTTPS");
        }
      }
      if(numCookies > 1){
        throw new InvalidConfigurationRequestException("Only one cookie stickiness policy can be set");
      }
    }
    
    final EntityTransaction db = Entities.get(LoadBalancerListener.class);
    try{
      final LoadBalancerListener update = Entities.uniqueResult(listener);
      for(final LoadBalancerPolicyDescription policy : policies){
        update.removePolicy(policy);
        update.addPolicy(policy);
      }
      Entities.persist(update);
      db.commit();
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static List<PolicyDescription> getSamplePolicyDescription(){
    return Lists.newArrayList(getSamplePolicyDescription40());
  }
  
  private static List<PolicyDescription> getSamplePolicyDescription40(){
    final List<PolicyDescription> sampleList = Lists.newArrayList();
    
    final PolicyDescription appCookieStick = new PolicyDescription();
    appCookieStick.setPolicyName("ELBSample-AppCookieStickinessPolicy");
    appCookieStick.setPolicyTypeName("AppCookieStickinessPolicyType");
    final PolicyAttributeDescription appCookieAttr = new PolicyAttributeDescription();
    appCookieAttr.setAttributeName("CookieName");
    appCookieAttr.setAttributeValue("ELBSampleCookie");
    final PolicyAttributeDescriptions appCookieAttrs = new PolicyAttributeDescriptions();
    appCookieAttrs.setMember(Lists.newArrayList(appCookieAttr));
    appCookieStick.setPolicyAttributeDescriptions(appCookieAttrs);
    sampleList.add(appCookieStick);
    
    final PolicyDescription lbCookieStick = new PolicyDescription();
    lbCookieStick.setPolicyName("ELBSample-LBCookieStickinessPolicy");
    lbCookieStick.setPolicyTypeName("LBCookieStickinessPolicyType");
    final PolicyAttributeDescription lbCookieAttr = new PolicyAttributeDescription();
    lbCookieAttr.setAttributeName("CookieExpirationPeriod");
    lbCookieAttr.setAttributeValue("100");
    final PolicyAttributeDescriptions lbCookieAttrs = new PolicyAttributeDescriptions();
    lbCookieAttrs.setMember(Lists.newArrayList(lbCookieAttr));
    lbCookieStick.setPolicyAttributeDescriptions(lbCookieAttrs);
    sampleList.add(lbCookieStick);
    
    return sampleList;
  }
  
  public enum AsPolicyDescription implements Function<LoadBalancerPolicyDescription, PolicyDescription> {
    INSTANCE;

    @Override
    public PolicyDescription apply(LoadBalancerPolicyDescription arg0) {
      if(arg0==null)
        return null;
      final PolicyDescription policy = new PolicyDescription();
      policy.setPolicyName(arg0.getPolicyName());
      policy.setPolicyTypeName(arg0.getPolicyTypeName());
      
      final List<PolicyAttributeDescription> attrDescs = Lists.newArrayList();
      for(final LoadBalancerPolicyAttributeDescriptionCoreView descView : arg0.getPolicyAttributeDescription()){
        final PolicyAttributeDescription desc = new PolicyAttributeDescription();
        desc.setAttributeName(descView.getAttributeName());
        desc.setAttributeValue(descView.getAttributeValue());
        attrDescs.add(desc);
      }
      final PolicyAttributeDescriptions descs = new PolicyAttributeDescriptions();
      descs.setMember((ArrayList<PolicyAttributeDescription>) attrDescs);
      policy.setPolicyAttributeDescriptions(descs);
      return policy;
    }
  }
  
  public enum AsPolicyTypeDescription implements Function<LoadBalancerPolicyTypeDescription, PolicyTypeDescription>{
    INSTANCE;
    @Override
    public PolicyTypeDescription apply(LoadBalancerPolicyTypeDescription arg0) {
      if(arg0 == null)
        return null;
      final PolicyTypeDescription policyType = new PolicyTypeDescription();
      policyType.setPolicyTypeName(arg0.getPolicyTypeName());
      policyType.setDescription(arg0.getDescription());
      final List<LoadBalancerPolicyAttributeTypeDescriptionCoreView> policyAttributeTypeDesc  = 
          arg0.getPolicyAttributeTypeDescriptions();
      if(policyAttributeTypeDesc != null && policyAttributeTypeDesc.size()>0){
        final List<PolicyAttributeTypeDescription> attrTypes = Lists.newArrayList();
        for(final LoadBalancerPolicyAttributeTypeDescriptionCoreView from : policyAttributeTypeDesc){
          final PolicyAttributeTypeDescription to = new PolicyAttributeTypeDescription();
          to.setAttributeName(from.getAttributeName());
          to.setAttributeType(from.getAttributeType());
          to.setCardinality(from.getCardinality());
          to.setDefaultValue(from.getDefaultValue());
          to.setDescription(from.getDescription());
          attrTypes.add(to);
        }
        final PolicyAttributeTypeDescriptions attrDescs = new PolicyAttributeTypeDescriptions();
        attrDescs.setMember((ArrayList<PolicyAttributeTypeDescription>) attrTypes);
        policyType.setPolicyAttributeTypeDescriptions(attrDescs);
      }
      return policyType;
    }
  }
}
