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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeDescription.LoadBalancerPolicyAttributeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.Cardinality;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyAttributeTypeDescription.LoadBalancerPolicyAttributeTypeDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeTypeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeTypeDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyTypeDescription;
import com.eucalyptus.loadbalancing.service.DuplicatePolicyNameException;
import com.eucalyptus.loadbalancing.service.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.service.LoadBalancingException;
import com.eucalyptus.loadbalancing.service.PolicyTypeNotFoundException;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Exceptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.base.Predicate;
/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerPolicies {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerPolicies.class );

  public static String LATEST_SECURITY_POLICY_NAME = null;
  /**
   * initialize the policy types that ELB will support
   * this method is idempotent
   */
  public static void initialize(){
    final List<LoadBalancerPolicyTypeDescription> requiredPolicyTypes =
        Lists.newArrayList(initialize42());

    for(final LoadBalancerPolicyTypeDescription policyType : requiredPolicyTypes){
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyTypeDescription.class ) ) {
        try {
          Entities.uniqueResult( policyType );
        }catch( final NoSuchElementException ex ){
          Entities.persist( policyType );
          db.commit();
          LOG.debug(String.format("New policy type has been added: %s", policyType));
        }
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
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

  final static List<String> cipherNamesIn42 = Lists.newArrayList(
      "ECDHE-ECDSA-AES128-GCM-SHA256",
      "ECDHE-RSA-AES128-GCM-SHA256",
      "ECDHE-ECDSA-AES128-SHA256",
      "ECDHE-RSA-AES128-SHA256",
      "ECDHE-ECDSA-AES128-SHA",
      "ECDHE-RSA-AES128-SHA",
      "DHE-RSA-AES128-SHA",
      "ECDHE-ECDSA-AES256-GCM-SHA384",
      "ECDHE-RSA-AES256-GCM-SHA384",
      "ECDHE-ECDSA-AES256-SHA384",
      "ECDHE-RSA-AES256-SHA384",
      "ECDHE-RSA-AES256-SHA",
      "ECDHE-ECDSA-AES256-SHA",
      "AES128-GCM-SHA256",
      "AES128-SHA256",
      "AES128-SHA",
      "AES256-GCM-SHA384",
      "AES256-SHA256",
      "AES256-SHA",
      "DHE-DSS-AES128-SHA",
      "CAMELLIA128-SHA",
      "EDH-RSA-DES-CBC3-SHA",
      "DES-CBC3-SHA",
      "ECDHE-RSA-RC4-SHA",
      "RC4-SHA",
      "ECDHE-ECDSA-RC4-SHA",
      "DHE-DSS-AES256-GCM-SHA384",
      "DHE-RSA-AES256-GCM-SHA384",
      "DHE-RSA-AES256-SHA256",
      "DHE-DSS-AES256-SHA256",
      "DHE-RSA-AES256-SHA",
      "DHE-DSS-AES256-SHA",
      "DHE-RSA-CAMELLIA256-SHA",
      "DHE-DSS-CAMELLIA256-SHA",
      "CAMELLIA256-SHA",
      "EDH-DSS-DES-CBC3-SHA",
      "DHE-DSS-AES128-GCM-SHA256",
      "DHE-RSA-AES128-GCM-SHA256",
      "DHE-RSA-AES128-SHA256",
      "DHE-DSS-AES128-SHA256",
      "DHE-RSA-CAMELLIA128-SHA",
      "DHE-DSS-CAMELLIA128-SHA",
      "ADH-AES128-GCM-SHA256",
      "ADH-AES128-SHA",
      "ADH-AES128-SHA256",
      "ADH-AES256-GCM-SHA384",
      "ADH-AES256-SHA",
      "ADH-AES256-SHA256",
      "ADH-CAMELLIA128-SHA",
      "ADH-CAMELLIA256-SHA",
      "ADH-DES-CBC3-SHA",
      "ADH-DES-CBC-SHA",
      "ADH-RC4-MD5",
      "ADH-SEED-SHA",
      "DES-CBC-SHA",
      "DHE-DSS-SEED-SHA",
      "DHE-RSA-SEED-SHA",
      "EDH-DSS-DES-CBC-SHA",
      "EDH-RSA-DES-CBC-SHA",
      "IDEA-CBC-SHA",
      "RC4-MD5",
      "SEED-SHA",
      "DES-CBC3-MD5",
      "DES-CBC-MD5",
      "RC2-CBC-MD5",
      "PSK-AES256-CBC-SHA",
      "PSK-3DES-EDE-CBC-SHA",
      "KRB5-DES-CBC3-SHA",
      "KRB5-DES-CBC3-MD5",
      "PSK-AES128-CBC-SHA",
      "PSK-RC4-SHA",
      "KRB5-RC4-SHA",
      "KRB5-RC4-MD5",
      "KRB5-DES-CBC-SHA",
      "KRB5-DES-CBC-MD5",
      "EXP-EDH-RSA-DES-CBC-SHA",
      "EXP-EDH-DSS-DES-CBC-SHA",
      "EXP-ADH-DES-CBC-SHA",
      "EXP-DES-CBC-SHA",
      "EXP-RC2-CBC-MD5",
      "EXP-KRB5-RC2-CBC-SHA",
      "EXP-KRB5-DES-CBC-SHA",
      "EXP-KRB5-RC2-CBC-MD5",
      "EXP-KRB5-DES-CBC-MD5",
      "EXP-ADH-RC4-MD5",
      "EXP-RC4-MD5",
      "EXP-KRB5-RC4-SHA",
      "EXP-KRB5-RC4-MD5"
      );

  private static List<LoadBalancerPolicyTypeDescription> initialize42(){
    final List<LoadBalancerPolicyTypeDescription> requiredPolicyTypes =
        initialize40();
    final LoadBalancerPolicyTypeDescription sslNego =   new LoadBalancerPolicyTypeDescription(
        "SSLNegotiationPolicyType",
        "Listener policy that defines the ciphers and protocols that will be accepted by the load balancer. This policy can be associated only with HTTPS/SSL listeners."
        );

    final List<LoadBalancerPolicyAttributeTypeDescription> sslNegoAttributeTypes = Lists.newArrayList(
        new LoadBalancerPolicyAttributeTypeDescription("Protocol-SSLv2", "Boolean", Cardinality.ZERO_OR_ONE),
        new LoadBalancerPolicyAttributeTypeDescription("Protocol-TLSv1", "Boolean", Cardinality.ZERO_OR_ONE),
        new LoadBalancerPolicyAttributeTypeDescription("Protocol-SSLv3", "Boolean", Cardinality.ZERO_OR_ONE),
        new LoadBalancerPolicyAttributeTypeDescription("Protocol-TLSv1.1", "Boolean", Cardinality.ZERO_OR_ONE, "A description for Protocol-TLSv1.1"),
        new LoadBalancerPolicyAttributeTypeDescription("Protocol-TLSv1.2", "Boolean", Cardinality.ZERO_OR_ONE, "A description for Protocol-TLSv1.2"),
        new LoadBalancerPolicyAttributeTypeDescription("Reference-Security-Policy", "String", Cardinality.ZERO_OR_ONE, "The value of this attribute is the name of our sample policy (referring to our sample policy"),
        new LoadBalancerPolicyAttributeTypeDescription("Server-Defined-Cipher-Order", "Boolean", Cardinality.ZERO_OR_ONE, "The value true means the policy will follow the cipher order")
    );
    for(final String name : cipherNamesIn42){
      sslNegoAttributeTypes.add(new LoadBalancerPolicyAttributeTypeDescription(name, "Boolean", Cardinality.ZERO_OR_ONE, String.format("A description for %s", name)));
    }
    for(final LoadBalancerPolicyAttributeTypeDescription attrType : sslNegoAttributeTypes){
      sslNego.addPolicyAttributeTypeDescription(attrType);
    }

    // policy type for ssl protocol/cipher negotiation
    requiredPolicyTypes.add(sslNego);

    // policy type for backend server authentication
    requiredPolicyTypes.add(new LoadBalancerPolicyTypeDescription(
        "BackendServerAuthenticationPolicyType",
        "Policy that controls authentication to back-end server(s) and contains one or more policies, such as an instance of a PublicKeyPolicyType. This policy can be associated only with back-end servers that are using HTTPS/SSL.",
        Lists.newArrayList(
            new LoadBalancerPolicyAttributeTypeDescription("PublicKeyPolicyName", "PolicyName", Cardinality.ONE_OR_MORE))
            ));

    requiredPolicyTypes.add(new LoadBalancerPolicyTypeDescription(
        "ProxyProtocolPolicyType",
        "Policy that controls whether to include the IP address and port of the originating request for TCP messages. This policy operates on TCP/SSL listeners only",
        Lists.newArrayList(
            new LoadBalancerPolicyAttributeTypeDescription("ProxyProtocol", "Boolean", Cardinality.ONE))
            ));
    // policy type for containing the list of public key when authenticating back-end servers
    requiredPolicyTypes.add(new LoadBalancerPolicyTypeDescription(
        "PublicKeyPolicyType",
        "Policy containing a list of public keys to accept when authenticating the back-end server(s). This policy cannot be applied directly to back-end servers or listeners but must be part of a BackendServerAuthenticationPolicyType.",
        Lists.newArrayList(
            new LoadBalancerPolicyAttributeTypeDescription("PublicKey", "String", Cardinality.ONE))
        ));
    return requiredPolicyTypes;
  }

  public static List<LoadBalancerPolicyTypeDescription> getLoadBalancerPolicyTypeDescriptions(){
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyTypeDescription.class ) ) {
      return Entities.query(new LoadBalancerPolicyTypeDescription());
    }catch(final NoSuchElementException ex){
      return Lists.newArrayList();
    }catch(final Exception ex){
      throw ex;
    }
  }

  public static LoadBalancerPolicyTypeDescription findLoadBalancerPolicyTypeDescription(final String policyTypeName)
      throws NoSuchElementException {
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyTypeDescription.class ) ) {
      return Entities.uniqueResult(LoadBalancerPolicyTypeDescription.named(policyTypeName));
    }catch(final NoSuchElementException ex){
      throw ex;
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
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
      final List<PolicyAttribute> policyAttributes) throws LoadBalancingException
  {
      for(final LoadBalancerPolicyDescriptionCoreView current : lb.getPolicies()){
        if(policyName.equals(current.getPolicyName()))
          throw new DuplicatePolicyNameException();
      }

      LoadBalancerPolicyTypeDescription policyType = null;
      for(final LoadBalancerPolicyTypeDescription type : getLoadBalancerPolicyTypeDescriptions()){
          if(policyTypeName.equals(type.getPolicyTypeName())){
            policyType = type;
            break;
          }
      }
      if(policyType == null)
        throw new PolicyTypeNotFoundException();

      List<PolicyAttribute> attributes = null;
      // Check Reference-Security-Policy
      if ("SSLNegotiationPolicyType".equals(policyType.getPolicyTypeName())) {
        String refPolicy = null;
        for(final PolicyAttribute attr : policyAttributes) {
          if("Reference-Security-Policy".equals(attr.getAttributeName())) {
            refPolicy = attr.getAttributeValue();
            break;
          }
        }
        if(refPolicy!=null) {
          PolicyDescription predefinedPolicy = null;
          List<PolicyDescription> predefinedPolicies = LoadBalancerPolicies.getSamplePolicyDescription();
          for(final PolicyDescription policy : predefinedPolicies) {
            if(refPolicy.equals(policy.getPolicyName())) {
              predefinedPolicy = policy;
              break;
            }
          }
          if (predefinedPolicy == null) {
            throw new InvalidConfigurationRequestException(String.format("Referenced security policy %s is not found", refPolicy));
          }else {
            attributes = Lists.transform(predefinedPolicy.getPolicyAttributeDescriptions().getMember(),
               new Function<PolicyAttributeDescription,PolicyAttribute>() {
                @Override
                public PolicyAttribute apply(PolicyAttributeDescription arg0) {
                  final PolicyAttribute attr = new PolicyAttribute();
                  attr.setAttributeName(arg0.getAttributeName());
                  attr.setAttributeValue(arg0.getAttributeValue());
                  return attr;
                }
            });
          }
        }
      }

      if(attributes == null)
        attributes = policyAttributes;

      /* check for cardinality
       * ONE(1) : Single value required
        ZERO_OR_ONE(0..1) : Up to one value can be supplied
        ZERO_OR_MORE(0..*) : Optional. Multiple values are allowed
        ONE_OR_MORE(1..*0) : Required. Multiple values are allowed
       */
      final List<LoadBalancerPolicyAttributeTypeDescriptionCoreView> policyAttrTypes =
          policyType.getPolicyAttributeTypeDescriptions();
      for (final LoadBalancerPolicyAttributeTypeDescriptionCoreView policyAttrType : policyAttrTypes) {
        if("ONE".equals(policyAttrType.getCardinality()) || "ONE_OR_MORE".equals(policyAttrType.getCardinality())) {
          boolean attrFound = false;
          for(final PolicyAttribute attr : attributes) {
            if(policyAttrType.getAttributeName().equals(attr.getAttributeName()) && attr.getAttributeValue()!=null){
              attrFound = true;
              break;
            }
          }
          if (!attrFound)
            throw new InvalidConfigurationRequestException(String.format("There is no attribute %s found (Cardinality: %s)", policyAttrType.getAttributeName(), policyAttrType.getCardinality()));
        } // other rules are enforced in addPolicyAttributeDescription
      }

      final LoadBalancerPolicyDescription policyDesc = new LoadBalancerPolicyDescription(lb, policyName, policyTypeName);
      for(final PolicyAttribute attr : attributes){
        policyDesc.addPolicyAttributeDescription(attr.getAttributeName(), attr.getAttributeValue());
      }
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyDescription.class ) ) {
        Entities.persist(policyDesc);
        db.commit();
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

    // check policy - backend association
    final List<LoadBalancerBackendServerDescriptionCoreView> backends = toDelete.getBackendServers();
    if(backends!=null && backends.size()>0)
      throw new InvalidConfigurationRequestException("The policy is enabled for backend servers");

    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyAttributeDescription.class ) ) {
      Entities.deleteAllMatching(LoadBalancerPolicyAttributeDescription.class,
          "WHERE metadata_policy_desc_fk = :metadata_policy_desc_fk",
          Collections.singletonMap("metadata_policy_desc_fk", toDelete.getRecordId()));
      db.commit();
    }catch(final Exception ex){
      LOG.error( "Failed to delete policy attributes", ex );
    }

    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerPolicyDescription.class ) ) {
      Entities.deleteAllMatching(LoadBalancerPolicyDescription.class,
          "WHERE unique_name = :unique_name",
          Collections.singletonMap("unique_name", toDelete.getUniqueName()));
      db.commit();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
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
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerListener.class ) ) {
      final LoadBalancerListener found = Entities.uniqueResult(listener);
      final List<LoadBalancerPolicyDescriptionCoreView> policies=found.getPolicies();
      db.commit();
      return Lists.transform(policies, LoadBalancerPolicyDescriptionEntityTransform.INSTANCE);
    }catch(final NoSuchElementException ex){
      return Lists.newArrayList();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public static void removePoliciesFromListener(final LoadBalancerListener listener){
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerListener.class ) ) {
      final LoadBalancerListener update = Entities.uniqueResult(listener);
      update.resetPolicies();
      Entities.persist(update);
      db.commit();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public static void addPoliciesToBackendServer(final LoadBalancerBackendServerDescription server, final List<LoadBalancerPolicyDescription> policies)
      throws LoadBalancingException {
    try ( final TransactionResource db =
        Entities.transactionFor( LoadBalancerBackendServerDescription.class ) ) {
      try{
        final LoadBalancerBackendServerDescription entity = Entities.uniqueResult(server);
        for(final LoadBalancerPolicyDescription p : policies)
          entity.addPolicy(p);
        Entities.persist(entity);
        db.commit();
      }catch(final NoSuchElementException ex){
        throw new InvalidConfigurationRequestException("Backend server description is not found on db");
      }catch(final Exception ex){
        throw new InvalidConfigurationRequestException("Unknown error ocrrued while updating db");
      }
    }
  }


  public static void removePoliciesFromBackendServer(final LoadBalancerBackendServerDescription backend, final List<String> policyNames)
      throws LoadBalancingException {
    final List<LoadBalancerPolicyDescription> policyToRemove = Lists.newArrayList();
    for(final LoadBalancerPolicyDescriptionCoreView pview : backend.getPolicyDescriptions()) {
      if(policyNames.contains(pview.getPolicyName()))
        policyToRemove.add(LoadBalancerPolicyDescriptionEntityTransform.INSTANCE.apply(pview));
    }

    if(policyToRemove.size() < policyNames.size())
      throw new InvalidConfigurationRequestException("Unknow policy names found");

    try ( final TransactionResource db =
        Entities.transactionFor( LoadBalancerBackendServerDescription.class ) ) {
      try{
        final LoadBalancerBackendServerDescription entity = Entities.uniqueResult(backend);
        for(final LoadBalancerPolicyDescription p : policyToRemove)
          entity.removePolicy(p);
        Entities.persist(entity);
        db.commit();
      }catch(final NoSuchElementException ex){
        throw new InvalidConfigurationRequestException("Backend server description is not found on db");
      }catch(final Exception ex){
        throw new InvalidConfigurationRequestException("Unknown error ocrrued while updating db");
      }
    }
  }

  public static void clearPoliciesFromBackendServer(final LoadBalancerBackendServerDescription backend) throws LoadBalancingException {
    if (backend == null)
      throw new InvalidConfigurationRequestException("Backend server description is not found");
    final List<String> allPolicies = Lists.transform( backend.getPolicyDescriptions(),
        new Function<LoadBalancerPolicyDescriptionCoreView, String> () {
          @Override
          public String apply(LoadBalancerPolicyDescriptionCoreView arg0) {
            return arg0.getPolicyName();
          }
        });
    removePoliciesFromBackendServer(backend, allPolicies);
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

    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerListener.class ) ) {
      final LoadBalancerListener update = Entities.uniqueResult(listener);
      for(final LoadBalancerPolicyDescription policy : policies){
        update.removePolicy(policy);
        update.addPolicy(policy);
      }
      db.commit();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public static List<PolicyDescription> getSamplePolicyDescription(){
    return Lists.newArrayList(getSamplePolicyDescription42());
  }


  private static class AttributeNameValuePair {
    @JsonProperty("AttributeName")
    public String AttributeName = null;

    @JsonProperty("AttributeValue")
    public String AttributeValue = null;
  }

  private static class SSLSecurityPolicy {
    @JsonProperty("PolicyName")
    public String PolicyName = null;

    @JsonProperty("PolicyTypeName")
    public String PolicyTypeName = null;

    @JsonProperty("PolicyAttributeDescriptions")
    public List<AttributeNameValuePair> PolicyAttributeDescriptions = null;
  }

  private static PolicyDescription getPolicyDescription(final String pathToAttributeJson) {
    try{
      final InputStream fileStream = new FileInputStream(pathToAttributeJson);
      final ObjectMapper objectMapper = new ObjectMapper();
      final SSLSecurityPolicy policy = objectMapper.readValue(fileStream, SSLSecurityPolicy.class);
      final PolicyDescription policyDesc = new PolicyDescription();
      final String policyName = policy.PolicyName;
      final String policyTypeName = policy.PolicyTypeName;
      if(policyName == null || policyTypeName==null)
        throw new Exception("PolicyName and PolicyTypeName must not be null");
      policyDesc.setPolicyName(policyName);
      policyDesc.setPolicyTypeName(policyTypeName);
      final List<PolicyAttributeDescription> policyAttrs = Lists.newArrayList(Iterables.filter(
          Lists.transform(policy.PolicyAttributeDescriptions, new Function<AttributeNameValuePair, PolicyAttributeDescription>(){
        @Override
        public PolicyAttributeDescription apply(AttributeNameValuePair arg0) {
          if(arg0.AttributeName==null || arg0.AttributeValue==null)
            return null;
          final PolicyAttributeDescription attr = new PolicyAttributeDescription();
          attr.setAttributeName(arg0.AttributeName);
          attr.setAttributeValue(arg0.AttributeValue);
          return attr;
        }
      }), Predicates.notNull()));
      final PolicyAttributeDescriptions descs = new PolicyAttributeDescriptions();
      descs.setMember((ArrayList<PolicyAttributeDescription>) policyAttrs);
      policyDesc.setPolicyAttributeDescriptions(descs);
      return policyDesc;
    }catch(final Exception ex){
      LOG.warn(String.format("Unable to read ELB security policy file: %s", pathToAttributeJson), ex);
      return null;
    }
  }

  private static List<PolicyDescription> samplePolicyDescription = Lists.newArrayList();
  private static List<PolicyDescription> getSamplePolicyDescription42(){
    if(samplePolicyDescription.isEmpty()){
      samplePolicyDescription = getSamplePolicyDescription40();
      PolicyDescription policyDesc = null;
      String[] policyFiles = null;
      final Set<String> policyNames = Sets.newHashSet();
      final String dir = String.format("%s/elb-security-policy", BaseDirectory.ETC);
      try{
        final File policyDirectory = new File(dir);
        policyFiles = policyDirectory.list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            if(name!=null && name.toLowerCase().endsWith(".json"))
              return true;
            else
              return false;
          }});
        if(policyFiles == null)
          throw new Exception("The directory '"+dir+"' is not found");
      }catch(final Exception ex) {
        LOG.error("Failed to find ELB security policy files", ex);
      }
      Date latest = new Date(Long.MIN_VALUE);
      for(final String policyFileName : policyFiles) {
        final String policyFilePath = String.format("%s/%s", dir, policyFileName);
        if ((policyDesc = getPolicyDescription(policyFilePath))!=null) {
          if(!policyNames.contains(policyDesc.getPolicyName())) {
            samplePolicyDescription.add(policyDesc);
            policyNames.add(policyDesc.getPolicyName());
            try{
              final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
              final Date policyDate = df.parse(policyDesc.getPolicyName().substring(policyDesc.getPolicyName().length()-7));
              if (policyDate.after(latest)) {
                latest = policyDate;
                LATEST_SECURITY_POLICY_NAME = policyDesc.getPolicyName();
              }
            }catch(final Exception ex){
              ;
            }
          } else {
            LOG.warn("Policy with dupilcate policy name found: "+ policyDesc.getPolicyName());
          }
        }
      }
    }
    return samplePolicyDescription;
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
