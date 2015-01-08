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

package com.eucalyptus.loadbalancing.backend;

import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.name;
import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.subnetId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerCwatchMetrics;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerPolicies;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyTypeDescription;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroupRef;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.activities.ApplySecurityGroupsEvent;
import com.eucalyptus.loadbalancing.activities.CreateListenerEvent;
import com.eucalyptus.loadbalancing.activities.DeleteListenerEvent;
import com.eucalyptus.loadbalancing.activities.DeleteLoadbalancerEvent;
import com.eucalyptus.loadbalancing.activities.DeregisterInstancesEvent;
import com.eucalyptus.loadbalancing.activities.DisabledZoneEvent;
import com.eucalyptus.loadbalancing.activities.EnabledZoneEvent;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.NewLoadbalancerEvent;
import com.eucalyptus.loadbalancing.activities.ActivityManager;
import com.eucalyptus.loadbalancing.activities.RegisterInstancesEvent;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadatas;
import com.eucalyptus.loadbalancing.common.backend.msgs.AddTagsResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.AddTagsType;
import com.eucalyptus.loadbalancing.common.backend.msgs.ApplySecurityGroupsToLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.ApplySecurityGroupsToLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.AttachLoadBalancerToSubnetsResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.AttachLoadBalancerToSubnetsType;
import com.eucalyptus.loadbalancing.common.backend.msgs.ConfigureHealthCheckResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.ConfigureHealthCheckType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateAppCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateAppCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLBCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLBCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeregisterInstancesFromLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DeregisterInstancesFromLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeInstanceHealthResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeInstanceHealthType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerAttributesType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerPoliciesResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerPoliciesType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerPolicyTypesResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancerPolicyTypesType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancersByServoResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancersByServoType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DetachLoadBalancerFromSubnetsResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DetachLoadBalancerFromSubnetsType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DisableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.DisableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.EnableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.EnableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.AppCookieStickinessPolicies;
import com.eucalyptus.loadbalancing.common.msgs.AppCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResult;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionDraining;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResult;
import com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.eucalyptus.loadbalancing.common.msgs.InstanceState;
import com.eucalyptus.loadbalancing.common.msgs.InstanceStates;
import com.eucalyptus.loadbalancing.common.msgs.Instances;
import com.eucalyptus.loadbalancing.common.msgs.LBCookieStickinessPolicies;
import com.eucalyptus.loadbalancing.common.msgs.LBCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.backend.msgs.ModifyLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.ModifyLoadBalancerAttributesType;
import com.eucalyptus.loadbalancing.common.backend.msgs.PutServoStatesResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.PutServoStatesType;
import com.eucalyptus.loadbalancing.common.backend.msgs.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.RemoveTagsResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerListenerSSLCertificateResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerListenerSSLCertificateType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerPoliciesForBackendServerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerPoliciesForBackendServerType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerPoliciesOfListenerResponseType;
import com.eucalyptus.loadbalancing.common.backend.msgs.SetLoadBalancerPoliciesOfListenerType;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.Policies;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.common.msgs.PolicyTypeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyTypeDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateResult;
import com.eucalyptus.loadbalancing.common.msgs.SourceSecurityGroup;
import com.eucalyptus.loadbalancing.common.msgs.Subnets;
import com.eucalyptus.loadbalancing.common.msgs.Tag;
import com.eucalyptus.loadbalancing.common.msgs.TagDescription;
import com.eucalyptus.loadbalancing.common.msgs.TagDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.TagKeyOnly;
import com.eucalyptus.loadbalancing.common.msgs.TagList;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.net.HostSpecifier;

/**
 * @author Sang-Min Park
 */
@SuppressWarnings( "UnusedDeclaration" )
public class LoadBalancingBackendService {
  private static Logger    LOG     = Logger.getLogger( LoadBalancingBackendService.class );
  private static final int MIN_HEALTHCHECK_INTERVAL_SEC = 5;
  private static final int MIN_HEALTHCHECK_THRESHOLDS = 2;
  private static final int MAX_TAGS = 10;

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  private void isValidServoRequest(String instanceId, String remoteHost) throws LoadBalancingException{
	  try{
		  final LoadBalancerServoInstance instance = LoadBalancers.lookupServoInstance( instanceId );
		  if(! (remoteHost.equals(instance.getAddress()) || remoteHost.equals(instance.getPrivateIp())))
				  throw new LoadBalancingException(String.format("IP address (%s) not match with record (%s-%s)",remoteHost, instance.getAddress(), instance.getPrivateIp()));
	  }catch(final LoadBalancingException ex){
		  throw ex;
	  }catch(Exception ex){
		  throw new LoadBalancingException("unknown error", ex);
	  }
  }
  
  /// EUCA-specific, internal operations for fetching listener specification
  public DescribeLoadBalancersByServoResponseType describeLoadBalancersByServo(
      final DescribeLoadBalancersByServoType request
  ) throws EucalyptusCloudException {
    final DescribeLoadBalancersByServoResponseType reply = request.getReply( );
    final String instanceId = request.getInstanceId( );
    // lookup servo instance Id to see which LB zone it is assigned to
    LoadBalancerZoneCoreView zoneView;
    LoadBalancerZone zone;
    try{
      isValidServoRequest(instanceId, request.getSourceIp());
      final LoadBalancerServoInstance instance = LoadBalancers.lookupServoInstance(instanceId);
      zoneView = instance.getAvailabilityZone();
      zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
    }catch(final Exception ex){
      throw new InternalFailureException("Failed to verify request sender");
    }

    final Function<LoadBalancerZone, Optional<LoadBalancerServoDescription>> lookupLBDescriptions =
        new Function<LoadBalancerZone, Optional<LoadBalancerServoDescription>>( ) {
      @Override
      public Optional<LoadBalancerServoDescription> apply( final LoadBalancerZone zone ){
        final LoadBalancerCoreView lbView= zone.getLoadbalancer();
        final String lbName = lbView.getDisplayName();

        final LoadBalancerServoDescription desc = new LoadBalancerServoDescription();
        desc.setLoadBalancerName(lbName); /// loadbalancer name
        desc.setCreatedTime(lbView.getCreationTimestamp());/// createdtime
        final LoadBalancer lb;
        try {
          lb = LoadBalancerEntityTransform.INSTANCE.apply(lbView);
        } catch( final Exception ex ){
          return Optional.<LoadBalancerServoDescription>absent();
        }

        final LoadBalancerDnsRecordCoreView dnsView = lb.getDns();

        /// dns name
        desc.setDnsName(dnsView.getDnsName());

        Collection<LoadBalancerBackendInstanceCoreView> notInError =
            Collections2.filter(zone.getBackendInstances(), new Predicate<LoadBalancerBackendInstanceCoreView>(){
              @Override
              public boolean apply( LoadBalancerBackendInstanceCoreView arg0 ) {
                return !LoadBalancerBackendInstance.STATE.Error.equals( arg0.getBackendState( ) ) && 
                    !(arg0.getIpAddress()==null || arg0.getIpAddress().length()<=0)  ;
              }
            });

        if( !notInError.isEmpty( ) ){
          desc.setInstances( new Instances( ) );
          desc.getInstances( ).getMember( ).addAll(
              Collections2.transform(notInError, new Function<LoadBalancerBackendInstanceCoreView, Instance>(){
                @Override
                public Instance apply(final LoadBalancerBackendInstanceCoreView be){
                  Instance instance = new Instance();
                  // re-use instanceId field to mark the instance's IP
                  // servo tool should interpret it properly
                  instance.setInstanceId(be.getInstanceId() +":"+ be.getIpAddress());
                  return instance;
                }
              }));
        }

        /// availability zones
        desc.setAvailabilityZones(new AvailabilityZones( ));
        desc.getAvailabilityZones( ).getMember( ).add( zone.getName( ) );

        /// listeners
        if(lb.getListeners().size()>0){
          desc.setListenerDescriptions(new ListenerDescriptions());
          desc.getListenerDescriptions().setMember(new ArrayList<>(
              Collections2.transform(lb.getListeners(), new Function<LoadBalancerListenerCoreView, ListenerDescription>(){
                @Override
                public ListenerDescription apply(final LoadBalancerListenerCoreView input){
                  ListenerDescription desc = new ListenerDescription();
                  Listener listener = new Listener();
                  listener.setLoadBalancerPort(input.getLoadbalancerPort());
                  listener.setInstancePort(input.getInstancePort());
                  if(input.getInstanceProtocol() != PROTOCOL.NONE)
                    listener.setInstanceProtocol(input.getInstanceProtocol().name());
                  listener.setProtocol(input.getProtocol().name());
                  if(input.getCertificateId()!=null)
                    listener.setSSLCertificateId(input.getCertificateId());
                  desc.setListener(listener);
                  final LoadBalancerListener lbListener = LoadBalancerListenerEntityTransform.INSTANCE.apply(input);
                  final PolicyNames pnames = new PolicyNames();
                  pnames.setMember(new ArrayList<>(Lists.transform(lbListener.getPolicies(), new Function<LoadBalancerPolicyDescriptionCoreView, String>(){
                    @Override
                    public String apply(
                        LoadBalancerPolicyDescriptionCoreView arg0) {
                      try{
                        // HACK: to send the values associated with the cookie stickiness policy
                        final LoadBalancerPolicyDescription policy = LoadBalancerPolicyDescriptionEntityTransform.INSTANCE.apply(arg0);
                        if("LBCookieStickinessPolicyType".equals(arg0.getPolicyTypeName())){
                          final String expiration=
                              policy.findAttributeDescription("CookieExpirationPeriod").getAttributeValue();
                          return String.format("LBCookieStickinessPolicyType:%s", expiration);
                        }else if("AppCookieStickinessPolicyType".equals(arg0.getPolicyTypeName())){
                          final String cookieName=
                              policy.findAttributeDescription("CookieName").getAttributeValue();
                          return String.format("AppCookieStickinessPolicyType:%s", cookieName);
                        }else{
                          return arg0.getPolicyName(); // No other policy types are supported
                        }
                      }catch(final Exception ex){
                        return arg0.getPolicyName(); // No other policy types are supported
                      }
                    }
                  })));
                  desc.setPolicyNames(pnames);
                  return desc;
                }
              })));
        }

        /// health check
        try{
          int interval = lb.getHealthCheckInterval();
          String target = lb.getHealthCheckTarget();
          int timeout = lb.getHealthCheckTimeout();
          int healthyThresholds = lb.getHealthyThreshold();
          int unhealthyThresholds = lb.getHealthCheckUnhealthyThreshold();

          final HealthCheck hc = new HealthCheck();
          hc.setInterval(interval);
          hc.setHealthyThreshold(healthyThresholds);
          hc.setTarget(target);
          hc.setTimeout(timeout);
          hc.setUnhealthyThreshold(unhealthyThresholds);
          desc.setHealthCheck(hc);
        } catch(Exception ex){
        }

        // attributes
        desc.setLoadBalancerAttributes( TypeMappers.transform( lb, LoadBalancerAttributes.class ) );

        return Optional.of( desc );
      }
    };

    Optional<LoadBalancerServoDescription> descs = Optional.<LoadBalancerServoDescription>absent( );
    try {
      final LoadBalancer lb = LoadBalancerEntityTransform.INSTANCE.apply(zone.getLoadbalancer());
      if( zone != null && LoadBalancingMetadatas.filterPrivilegedWithoutOwner().apply( lb ) && zone.getState().equals(LoadBalancerZone.STATE.InService)){
        descs= lookupLBDescriptions.apply( zone );
      }
    } catch( final Exception ex ) {
    }

    final LoadBalancerServoDescriptions lbDescs = new LoadBalancerServoDescriptions( );
    lbDescs.getMember().addAll( descs.asSet( ) );
    reply.getDescribeLoadBalancersResult( ).setLoadBalancerDescriptions( lbDescs );
    reply.set_return(true);

    return reply;
  }
  
  /// EUCA-specific, internal operations for storing instance health check and cloudwatch metrics
  public PutServoStatesResponseType putServoStates(PutServoStatesType request) throws EucalyptusCloudException {
	  PutServoStatesResponseType reply = request.getReply();
	  final String servoId = request.getInstanceId();

	  try{
		  isValidServoRequest(servoId, request.getSourceIp());
	  }catch(final Exception ex){
      throw new InternalFailureException("Failed to verify request sender");
	  }
	  
	  final Instances instances = request.getInstances();
	  final MetricData metric = request.getMetricData();
	  
	  LoadBalancer lb = null;
	  if(servoId!= null){
		  try{
			  final LoadBalancerServoInstance servo = LoadBalancers.lookupServoInstance(servoId);
			  final LoadBalancerZoneCoreView zoneView = servo.getAvailabilityZone();
			  final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
			  final LoadBalancerCoreView lbView = zone.getLoadbalancer();
			  lb = LoadBalancerEntityTransform.INSTANCE.apply(lbView);
			  if(lb==null)
				  throw new Exception("Failed to find the loadbalancer");
			  if(! servo.getAvailabilityZone().getState().equals(LoadBalancerZone.STATE.InService))
				  lb= null;
		  }catch(NoSuchElementException ex){
			  LOG.warn("unknown servo VM id is used to query: "+servoId);
		  }catch(Exception ex){
			  LOG.warn("failed to query servo instance");
		  }
	  }
	  if(lb==null || !LoadBalancingMetadatas.filterPrivilegedWithoutOwner().apply( lb ))
		  return reply;
	  
	  /// INSTANCE HEALTH CHECK UPDATE
	  if(instances!= null && instances.getMember()!=null && instances.getMember().size()>0){
		  final Collection<LoadBalancerBackendInstanceCoreView> lbInstances = lb.getBackendInstances();
		  for(final Instance instance : instances.getMember()){
			  String instanceId = instance.getInstanceId();
				  // format: instanceId:state
			  String[] parts = instanceId.split(":");
			  if(parts==null || parts.length!= 2){
				  LOG.warn("instance id is in wrong format:"+ instanceId);
				  continue;
			  }
			  instanceId = parts[0];
			  final String state = parts[1];
			  LoadBalancerBackendInstanceCoreView found = null;
			  for(final LoadBalancerBackendInstanceCoreView lbInstance : lbInstances){
				  if(instanceId.equals(lbInstance.getInstanceId())){
					  found = lbInstance;
					  break;
				  }  
			  }
			  if(found!=null){
				  try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
					  final LoadBalancerBackendInstance update = Entities.uniqueResult(
							  LoadBalancerBackendInstance.named(lb, found.getInstanceId()));
				
					  update.setState(Enum.valueOf(LoadBalancerBackendInstance.STATE.class, state));
					  if(state.equals(LoadBalancerBackendInstance.STATE.OutOfService.name())){
						  update.setReasonCode("Instance");
						  update.setDescription("Instance has failed at least the UnhealthyThreshold number of health checks consecutively.");
					  }else{
						  update.setReasonCode("");
						  update.setDescription("");
					  }
					  update.updateInstanceStateTimestamp();
					  Entities.persist(update);
					  db.commit();
				  }catch(final NoSuchElementException ex){
					  LOG.error("unable to find the loadbancer backend instance", ex);
				  }catch(final Exception ex){
					  LOG.error("unable to update the state of loadbalancer backend instance", ex);
				  }
			  }
		  }
	  }
	  
	  /// Update Cloudwatch
	  for(final LoadBalancerBackendInstanceCoreView sample : lb.getBackendInstances()){
	 	  try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
	 		  final LoadBalancerBackendInstance found = Entities.uniqueResult(
	 				  LoadBalancerBackendInstance.named(lb, sample.getInstanceId()));
	 		  final String zoneName = found.getAvailabilityZone().getName();
	 		  if(found.getState().equals(LoadBalancerBackendInstance.STATE.InService)){
	 			  LoadBalancerCwatchMetrics.getInstance().updateHealthy(lb.getCoreView(), zoneName, found.getInstanceId());
	 		  }else if (found.getState().equals(LoadBalancerBackendInstance.STATE.OutOfService)){
	 			  LoadBalancerCwatchMetrics.getInstance().updateUnHealthy(lb.getCoreView(), zoneName, found.getInstanceId());
	 		  }
	 		  db.commit();
	 	  }catch(NoSuchElementException ex){
	 	  }catch(Exception ex){
	 		  LOG.warn("Failed to query loadbalancer backend instance", ex);
	 	  }
	  }
	  
	  if(metric!= null && metric.getMember()!= null && metric.getMember().size()>0){
		  try{
			  LoadBalancerCwatchMetrics.getInstance().addMetric(servoId, metric);
		  }catch(Exception ex){
			  LOG.error("Failed to add ELB cloudwatch metric", ex);
		  }
	  }
	  
	  return reply;
  }
 
  public CreateLoadBalancerResponseType createLoadBalancer(
      final CreateLoadBalancerType request
  ) throws EucalyptusCloudException {
    final CreateLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName( );

    // verify loadbalancer name
    if( lbName == null ||
        !HostSpecifier.isValid(String.format("%s.com", lbName)) ||
        !lbName.matches("[a-zA-Z0-9-]{1,32}")){
      throw new InvalidConfigurationRequestException("Invalid character found in the loadbalancer name");
    }

    final Map<String,String> tags = Maps.newHashMap( );
    if ( request.getTags( ) != null ) {
      for ( final Tag tag : request.getTags( ).getMember( ) ) {
        if ( tags.put( tag.getKey( ), Strings.nullToEmpty( tag.getValue( ) ) ) != null ) {
          throw new LoadBalancingBackendClientException( "DuplicateTagKeys", "Duplicate tag key (" + tag.getKey( ) + ")" );
        }
      }
      final int userTags = Iterables.size( Iterables.filter( tags.keySet( ), Predicates.not( isReservedTagPrefix( ) ) ) );
      if ( tags.size( ) - userTags > 0 && !Contexts.lookup( ).isPrivileged( ) ) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
      if ( userTags > MAX_TAGS ) {
        throw Exceptions.toUndeclared( new LoadBalancingBackendClientException( "TooManyTags", "Tag limit exceeded" ) );
      }
    }

    final List<Listener> listeners = request.getListeners( ) != null ?
        request.getListeners( ).getMember( ) :
        Collections.<Listener>emptyList( );

    LoadBalancers.validateListener( listeners );
    
    // Check SSL Certificate Id before creating LB
    try {
      for(final Listener l : listeners){
        if("HTTPS".equals(l.getProtocol().toUpperCase()) || "SSL".equals(l.getProtocol().toUpperCase())){
          final String certArn = l.getSSLCertificateId();
          if(certArn==null || certArn.length()<=0)
            throw new InvalidConfigurationRequestException("SSLCertificateId is required for HTTPS or SSL protocol");
          LoadBalancers.checkSSLCertificate(ctx.getUser().getUserId(), certArn);
        }
      }
    }catch(Exception ex){
      if(! (ex instanceof LoadBalancingException)){
          LOG.error("failed to check SSL certificate Id", ex);
          ex = new InternalFailure400Exception("failed to check SSL certificate Id", ex);
      }
      throw (LoadBalancingException) ex;
    }

    if ( request.getAvailabilityZones( ) != null && !request.getAvailabilityZones( ).getMember( ).isEmpty( ) ) {
      final Set<String> validZones = Sets.newHashSet( );
      try {
        Iterables.addAll(
            validZones,
            Iterables.transform(
                EucalyptusActivityTasks.getInstance().describeAvailabilityZones( false ),
                ClusterInfoType.zoneName() ) );
      } catch( Exception ex ){
        throw new InternalFailure400Exception("Unable to verify the requested zones");
      }
      for( final String zone : request.getAvailabilityZones( ).getMember( ) ) {
        if( !validZones.contains( zone ) ) {
          throw new InvalidConfigurationRequestException( "No cluster named " + zone + " is available" );
        }
      }
    }

    String subnetVpcId = null;
    final Map<String,String> zoneToSubnetIdMap = Maps.newHashMap( );
    if ( request.getSubnets( ) != null ) {
      final List<SubnetType> subnets =
          EucalyptusActivityTasks.getInstance( ).describeSubnets( request.getSubnets( ).getMember( ) );
      if ( subnets.size( ) != request.getSubnets( ).getMember( ).size( ) ) {
        throw new LoadBalancingBackendClientException( "SubnetNotFound", "Invalid subnet(s)" );
      }
      for ( final SubnetType subnetType : subnets ) {
        if ( subnetVpcId == null ) {
          subnetVpcId = subnetType.getVpcId( );
        } else if ( !subnetVpcId.equals( subnetType.getVpcId( ) ) ) {
          throw new InvalidConfigurationRequestException( "Subnets must belong to the same VPC" );
        }
        if ( zoneToSubnetIdMap.put( subnetType.getAvailabilityZone( ), subnetType.getSubnetId( ) ) != null ) {
          throw new InvalidConfigurationRequestException( "Multiple subnets for zone ("+subnetType.getAvailabilityZone( )+")" );
        }
      }
    }
    final String adminUserId = lookupAdmin( ctx );
    boolean defaultVpc = false;
    if ( subnetVpcId == null ) { // check for a default VPC
      final Optional<VpcType> vpcOptional = EucalyptusActivityTasks.getInstance( ).defaultVpc( adminUserId );
      subnetVpcId = vpcOptional.transform( VpcType.id( ) ).orNull( );
      defaultVpc = subnetVpcId != null;
    }
    final String vpcId = subnetVpcId;
    if ( defaultVpc && zoneToSubnetIdMap.isEmpty( ) && request.getAvailabilityZones( ) != null ) {
      final List<SubnetType> subnets = EucalyptusActivityTasks.getInstance( ).describeSubnetsByZone(
          vpcId, true, request.getAvailabilityZones( ).getMember( ) );
      CollectionUtils.putAll( subnets, zoneToSubnetIdMap, SubnetType.zone( ), SubnetType.id( ) );
      if ( request.getAvailabilityZones( ).getMember( ).size( ) != zoneToSubnetIdMap.size( ) ) {
        throw new InvalidConfigurationRequestException( "Default subnet not found for zone(s)" );
      }
    }

    LoadBalancer.Scheme reqScheme = LoadBalancer.Scheme.fromString( request.getScheme( ) ).orNull( );
    if ( reqScheme == null && !Strings.isNullOrEmpty( request.getScheme( ) ) ) {
      throw new InvalidConfigurationRequestException("Invalid scheme ("+request.getScheme( )+")");
    }
    if ( reqScheme != null && vpcId == null ) {
      if ( reqScheme != LoadBalancer.Scheme.InternetFacing ) {
        throw new InvalidConfigurationRequestException( "Scheme (" + request.getScheme() + ") should not be specified for EC2-Classic platform" );
      } else {
        reqScheme = null; // ignore internet-facing scheme for non-vpc ELB
      }
    }
    final LoadBalancer.Scheme scheme = reqScheme;

    if ( vpcId != null && scheme != LoadBalancer.Scheme.Internal ) try {
      final List<InternetGatewayType> internetGateways =
          EucalyptusActivityTasks.getInstance( ).describeInternetGateways( Collections.singleton( vpcId ) );
      if ( internetGateways.isEmpty( ) ) {
        throw new LoadBalancingBackendClientException( "InvalidSubnet", "VPC "+vpcId+" has no internet gateway" );
      }
    } catch ( final LoadBalancingException e ) {
      throw e;
    } catch ( final Exception e ) {
      LOG.error( "Error checking internet gateway", e );
      throw new InternalFailureException( "Unable to verify VPC configuration" );
    }

    final Set<String> securityGroupIds = Sets.newHashSet( );
    if ( request.getSecurityGroups( ) != null ) {
      securityGroupIds.addAll( request.getSecurityGroups( ).getMember( ) );
    } else if ( vpcId != null && !defaultVpc ) { // for default VPC a group is created/discovered later
      final List<SecurityGroupItemType> groups =  EucalyptusActivityTasks.getInstance( )
          .describeUserSecurityGroupsByName( adminUserId, vpcId, "default" );
      if ( groups.isEmpty( ) ) {
        throw new InvalidConfigurationRequestException( "Default security group not found for VPC " + vpcId );
      }
      securityGroupIds.add( groups.get( 0 ).getGroupId( ) );
    }
    if ( !securityGroupIds.isEmpty( ) && vpcId == null ) {
      throw new InvalidConfigurationRequestException("Security groups should not be specified for EC2-Classic platform");
    }
    final List<SecurityGroupItemType> groups = securityGroupIds.isEmpty() ?
        Collections.<SecurityGroupItemType>emptyList( ) :
        EucalyptusActivityTasks.getInstance().describeUserSecurityGroupsById( adminUserId, vpcId, securityGroupIds );
    if ( groups.size( ) != securityGroupIds.size( ) ) {
      throw new LoadBalancingBackendClientException( "InvalidSecurityGroup", "Invalid security group(s)" );
    }

    final Collection<String> zones = Lists.newArrayList( );
    if ( request.getAvailabilityZones( ) != null ) zones.addAll( request.getAvailabilityZones( ).getMember( ) );
    if ( zones.isEmpty( ) ) zones.addAll( zoneToSubnetIdMap.keySet( ) );
    if ( !zoneToSubnetIdMap.isEmpty( ) && !zoneToSubnetIdMap.keySet( ).equals( Sets.newHashSet( zones ) ) ) {
      throw new InvalidConfigurationRequestException("Availability zones and subnets are inconsistent");
    }

    final Supplier<LoadBalancer> allocator = new Supplier<LoadBalancer>() {
      @Override
      public LoadBalancer get() {
        try {
          final Map<String,String> securityGroupIdsToNames = CollectionUtils.putAll(
              groups,
              Maps.<String,String>newHashMap( ),
              SecurityGroupItemType.groupId( ),
              SecurityGroupItemType.groupName( ) );
          return LoadBalancers.addLoadbalancer( ownerFullName, lbName, vpcId, scheme, securityGroupIdsToNames, tags );
        } catch ( LoadBalancingException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    final LoadBalancer lb;
    try {
      lb = LoadBalancingMetadatas.allocateUnitlessResource( allocator );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    Function<String, Boolean> rollback = new Function<String, Boolean>(){
      @Override
      public Boolean apply(String lbName){
        try{
          LoadBalancers.unsetForeignKeys(ctx, lbName);
        }catch(final Exception ex){
          LOG.warn("unable to unset foreign keys", ex);
        }

        try{
          LoadBalancers.removeZone(lbName, ctx, zones);
        }catch(final Exception ex){
          LOG.error("unable to delete availability zones during rollback", ex);
        }

        try{
            LoadBalancers.deleteLoadbalancer(ownerFullName, lbName);
          }catch(LoadBalancingException ex){
            LOG.error("failed to rollback the loadbalancer: " + lbName, ex);
            return false;
          }
        return true;
      }
    };

    final LoadBalancerDnsRecord dns = LoadBalancers.getDnsRecord(lb);
    if(dns == null || dns.getName() == null){
      rollback.apply(lbName);
      throw new InternalFailure400Exception("Dns name could not be created");
    }

    if(zones != null && zones.size()>0){
      try{
        LoadBalancers.addZone( lbName, ctx, zones, zoneToSubnetIdMap );
      }catch(LoadBalancingException ex){
        rollback.apply(lbName);
        throw ex;
      }catch(Exception ex){
        rollback.apply(lbName);
        throw new InternalFailure400Exception("Failed to persist the zone");
      }
    }

    Entities.evictCache( LoadBalancer.class );

    /// trigger new loadbalancer event 
    try{
      NewLoadbalancerEvent evt = new NewLoadbalancerEvent();
      evt.setLoadBalancer(lbName);
      evt.setContext(ctx);
      evt.setZones(zones);
      ActivityManager.getInstance().fire(evt);
    }catch(EventFailedException e){
      LOG.error("failed to handle new loadbalancer event", e);
      rollback.apply(lbName);
      final String reason = e.getCause() != null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to create the loadbalancer: %s", reason), e);
    }
    
    if( !listeners.isEmpty( ) ){
      LoadBalancers.createLoadbalancerListener(lbName,  ctx, Lists.newArrayList(listeners));
      try{
        CreateListenerEvent evt = new CreateListenerEvent();
        evt.setLoadBalancer(lbName);
        evt.setListeners(listeners);
        evt.setContext(ctx);
        ActivityManager.getInstance().fire(evt);
      }catch(EventFailedException e){
        LOG.error("failed to handle createListener event", e);
        // rollback.apply(lbName);
        // TODO: this will leave the loadbalancer, which  will not be functional.
        // ideally, we should rollback the whole loadbalancer creation pipeline
        final String reason = e.getCause() != null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to setup the listener: %s", reason), e);
      }
    }
    
    final CreateLoadBalancerResult result = new CreateLoadBalancerResult( );
    result.setDnsName( dns.getDnsName( ) );
    reply.setCreateLoadBalancerResult( result );
    reply.set_return( true );
    return reply;
  }

  public DescribeLoadBalancersResponseType describeLoadBalancers(DescribeLoadBalancersType request) throws EucalyptusCloudException {
    DescribeLoadBalancersResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount().getAccountNumber();
    final Set<String> requestedNames = Sets.newHashSet( );
    if ( request.getLoadBalancerNames( ) != null ) {
      requestedNames.addAll( request.getLoadBalancerNames().getMember() );
    }
    final boolean showAll = requestedNames.remove( "verbose" ) && ctx.isAdministrator();

    final Function<Set<String>, Set<LoadBalancer>> lookupAccountLBs = new Function<Set<String>, Set<LoadBalancer>>( ) {
          @Override
          public Set<LoadBalancer> apply( final Set<String> identifiers ) {
            try {
              final Predicate<? super LoadBalancer> requestedAndAccessible =
                  LoadBalancingMetadatas.filteringFor( LoadBalancer.class )
                      .byId( identifiers )
                      .byPrivileges( )
                      .buildPredicate( );

              final LoadBalancer example = showAll ?
                  LoadBalancer.named( null, null ) :
                  LoadBalancer.namedByAccountId( accountNumber, null );
              final List<LoadBalancer> lbs = Entities.query( example, true );
              return Sets.newHashSet( Iterables.filter( lbs, requestedAndAccessible ) );
            } catch ( EntityNotFoundException e ) {
              Entities.evictCache( LoadBalancer.class );
              throw new OptimisticLockException( "Error loading load balancers", e );
            }
          }
    };

    final Set<LoadBalancer> allowedLBs =
        Entities.asTransaction( LoadBalancer.class, lookupAccountLBs ).apply( requestedNames );
    
    final Function<Set<LoadBalancer>, Set<LoadBalancerDescription>> lookupLBDescriptions = new Function<Set<LoadBalancer>, Set<LoadBalancerDescription>> () {
      public Set<LoadBalancerDescription> apply (final Set<LoadBalancer> input){
        final Set<LoadBalancerDescription> descs = Sets.newHashSet();
        for (final LoadBalancer lb : input){
          LoadBalancerDescription desc = new LoadBalancerDescription();
          if(lb==null) // loadbalancer not found
            continue;
          final String lbName = lb.getDisplayName();
          desc.setLoadBalancerName(lbName);
          desc.setCreatedTime(lb.getCreationTimestamp());

          // dns name
          final LoadBalancerDnsRecordCoreView dns = lb.getDns();
          if ( dns != null ) {
            desc.setDnsName( dns.getDnsName() );
          }

          // instances
          if(lb.getBackendInstances().size()>0){
            desc.setInstances(new Instances());
            Iterables.addAll( desc.getInstances().getMember( ), Iterables.transform(
                lb.getBackendInstances(),
                Functions.compose( Instance.instance( ), LoadBalancerBackendInstanceCoreView.instanceId( ) ) ) );
          }

          // availability zones
          final List<String> subnetIds = Lists.newArrayList( );
          if( !lb.getZones().isEmpty( ) ){
            desc.setAvailabilityZones(new AvailabilityZones());
            final List<LoadBalancerZoneCoreView> currentZones = 
              Lists.newArrayList(Iterables.filter(lb.getZones(), new Predicate<LoadBalancerZoneCoreView>(){
                @Override
                public boolean apply( LoadBalancerZoneCoreView arg0 ) {
                  return arg0.getState().equals(LoadBalancerZone.STATE.InService);
                }
              }));
            Iterables.addAll(
                desc.getAvailabilityZones().getMember( ),
                Iterables.transform( currentZones, LoadBalancerZoneCoreView.name( ) ) );
            Iterables.addAll(
                subnetIds,
                Iterables.filter(
                    Iterables.transform( currentZones, LoadBalancerZoneCoreView.subnetId() ),
                    Predicates.notNull() ) );
          }

          // subnets
          if ( !subnetIds.isEmpty( ) ) {
            desc.setSubnets( new Subnets( subnetIds ) );
          }

          desc.setVpcId( lb.getVpcId( ) );

          // listeners
          if(lb.getListeners().size()>0){
            desc.setListenerDescriptions(new ListenerDescriptions());
            desc.getListenerDescriptions().setMember(new ArrayList<>(
                Collections2.transform(lb.getListeners(), new Function<LoadBalancerListenerCoreView, ListenerDescription>(){
                  @Override
                  public ListenerDescription apply(final LoadBalancerListenerCoreView input){
                    ListenerDescription desc = new ListenerDescription();
                    Listener listener = new Listener();
                    listener.setLoadBalancerPort(input.getLoadbalancerPort());
                    listener.setInstancePort(input.getInstancePort());
                    if(input.getInstanceProtocol() != PROTOCOL.NONE)
                      listener.setInstanceProtocol(input.getInstanceProtocol().name());
                    listener.setProtocol(input.getProtocol().name());
                    if(input.getCertificateId()!=null)
                      listener.setSSLCertificateId(input.getCertificateId());
                    
                    desc.setListener(listener);
                    final LoadBalancerListener lbListener = LoadBalancerListenerEntityTransform.INSTANCE.apply(input);
                    final PolicyNames pnames = new PolicyNames();
                    pnames.setMember(new ArrayList<>(Lists.transform(lbListener.getPolicies(), new Function<LoadBalancerPolicyDescriptionCoreView, String>(){
                      @Override
                      public String apply(
                          LoadBalancerPolicyDescriptionCoreView arg0) {
                        return arg0.getPolicyName();
                      }
                    })));
                    desc.setPolicyNames(pnames);
                    return desc;
                  }
                })));
          }

          /// health check
          try{
            int interval = lb.getHealthCheckInterval();
            String target = lb.getHealthCheckTarget();
            int timeout = lb.getHealthCheckTimeout();
            int healthyThresholds = lb.getHealthyThreshold();
            int unhealthyThresholds = lb.getHealthCheckUnhealthyThreshold();

            final HealthCheck hc = new HealthCheck();
            hc.setInterval(interval);
            hc.setHealthyThreshold(healthyThresholds);
            hc.setTarget(target);
            hc.setTimeout(timeout);
            hc.setUnhealthyThreshold(unhealthyThresholds);
            desc.setHealthCheck(hc);
          } catch(Exception ex){
          }
          /// backend server description
          /// source security group
          try{
            LoadBalancerSecurityGroupCoreView group = lb.getGroup();
            if(group!=null){
              desc.setSourceSecurityGroup( new SourceSecurityGroup( group.getGroupOwnerAccountId(), group.getName() ) );
            }
          }catch(Exception ex){
          }

          if ( !lb.getCoreView( ).getSecurityGroupIdsToNames( ).isEmpty() ) {
            desc.setSecurityGroups( new SecurityGroups( lb.getCoreView( ).getSecurityGroupIdsToNames( ).keySet( ) ) );
            if ( desc.getSourceSecurityGroup( ) == null ) {
              desc.setSourceSecurityGroup( new SourceSecurityGroup(
                  lb.getOwnerAccountNumber( ),
                  Iterables.get( lb.getCoreView().getSecurityGroupIdsToNames().values(), 0 ) ) );
            }
          }
          
          // policies
          try{
            final List<LoadBalancerPolicyDescription> lbPolicies = 
                LoadBalancerPolicies.getLoadBalancerPolicyDescription( lb );
            final ArrayList<AppCookieStickinessPolicy> appCookiePolicies = Lists.newArrayList();
            final ArrayList<LBCookieStickinessPolicy> lbCookiePolicies = Lists.newArrayList();
            final ArrayList<String> otherPolicies = Lists.newArrayList();
            for(final LoadBalancerPolicyDescription policy : lbPolicies){
              if("LBCookieStickinessPolicyType".equals(policy.getPolicyTypeName())){
                final LBCookieStickinessPolicy lbp = new LBCookieStickinessPolicy();
                lbp.setPolicyName(policy.getPolicyName());
                lbp.setCookieExpirationPeriod(Long.parseLong(
                    policy.findAttributeDescription("CookieExpirationPeriod").getAttributeValue()));
                lbCookiePolicies.add(lbp);
              }else if("AppCookieStickinessPolicyType".equals(policy.getPolicyTypeName())){
                final AppCookieStickinessPolicy app = new AppCookieStickinessPolicy();
                app.setPolicyName(policy.getPolicyName());
                app.setCookieName(policy.findAttributeDescription("CookieName").getAttributeValue());
                appCookiePolicies.add(app);
              }
              else
                otherPolicies.add(policy.getPolicyName());
            }
            final Policies p = new Policies();
            final LBCookieStickinessPolicies lbp = new LBCookieStickinessPolicies();
            lbp.setMember(lbCookiePolicies);
            final AppCookieStickinessPolicies app = new AppCookieStickinessPolicies();
            app.setMember(appCookiePolicies);
            final PolicyNames other = new PolicyNames();
            other.setMember(otherPolicies);
            p.setAppCookieStickinessPolicies(app);
            p.setLbCookieStickinessPolicies(lbp);
            p.setOtherPolicies(other);
            desc.setPolicies(p);

            desc.setScheme( Objects.toString( lb.getScheme( ), "internet-facing" ) );
          } catch(final Exception ex){
            LOG.error("Failed to retrieve policies", ex);
          }
          descs.add(desc);
        }
        return descs;
      }
    };
    Set<LoadBalancerDescription> descs = lookupLBDescriptions.apply(allowedLBs);

    DescribeLoadBalancersResult descResult = new DescribeLoadBalancersResult();
    LoadBalancerDescriptions lbDescs = new LoadBalancerDescriptions();
    lbDescs.setMember(new ArrayList<>(descs));
    descResult.setLoadBalancerDescriptions(lbDescs);
    reply.setDescribeLoadBalancersResult(descResult);
    reply.set_return(true);

    return reply;
  }

  public DeleteLoadBalancerResponseType deleteLoadBalancer( DeleteLoadBalancerType request ) throws EucalyptusCloudException {
    DeleteLoadBalancerResponseType reply = request.getReply();
    final String candidateLB = request.getLoadBalancerName();
    final Context ctx = Contexts.lookup();
    Function<String, LoadBalancer> findLoadBalancer = new Function<String, LoadBalancer>(){
		@Override
		@Nullable
		public LoadBalancer apply(@Nullable String lbName) {
			try{
				LoadBalancer lb = LoadBalancers.getLoadbalancer(ctx, lbName);
				return lb;
			}catch(NoSuchElementException ex){
				if(ctx.isAdministrator()){
					try{
						final LoadBalancer lb = LoadBalancers.getLoadBalancerByDnsName(lbName);		
						final User owner = Accounts.lookupUserById(lb.getOwnerUserId());
						ctx.setUser(owner);
						return lb;
					}catch(Exception ex2){
						if(ex2 instanceof NoSuchElementException)
							throw Exceptions.toUndeclared(new LoadBalancingException("Unable to find the loadbalancer (use DNS name if you are Cloud admin)"));
						throw Exceptions.toUndeclared(ex2);
					}
				}
				throw ex;
			}
		}
    };
    
    try {
      if ( candidateLB != null ) {
    	String lbToDelete = null;
        LoadBalancer lb = null;
        try {
          lb = findLoadBalancer.apply(candidateLB);
          lbToDelete = lb.getDisplayName();
        } catch ( NoSuchElementException ex ) {
        } catch ( Exception ex){
        	if(ex.getCause() != null && ex.getCause() instanceof LoadBalancingException)
        		throw (LoadBalancingException) ex.getCause();
        	else
        		throw ex;
        }
        
        //IAM Support for deleting load balancers
        if (lb != null && ! LoadBalancingMetadatas.filterPrivileged().apply( lb ))
        	throw new AccessPointNotFoundException();
        
        if ( lb != null ) {
          Collection<LoadBalancerListenerCoreView> listeners = lb.getListeners();
          final List<Integer> ports = Lists.newArrayList( Collections2.transform( listeners, new Function<LoadBalancerListenerCoreView, Integer>() {
            @Override
            public Integer apply( @Nullable LoadBalancerListenerCoreView arg0 ) {
              return arg0.getLoadbalancerPort();
            }
          } ) );

          try {
            DeleteListenerEvent evt = new DeleteListenerEvent();
            evt.setLoadBalancer( lbToDelete );
            evt.setContext( ctx );
            evt.setPorts( ports );
            ActivityManager.getInstance().fire( evt );
          } catch ( EventFailedException e ) {
            LOG.error( "failed to handle DeleteListener event", e );
          }

          try {
            DeleteLoadbalancerEvent evt = new DeleteLoadbalancerEvent();
            evt.setLoadBalancer( lbToDelete );
            evt.setContext( ctx );
            ActivityManager.getInstance().fire( evt );
          } catch ( EventFailedException e ) {
            LOG.error( "failed to handle DeleteLoadbalancer event", e );
            throw e;
          }
          
          LoadBalancers.deleteLoadbalancer( UserFullName.getInstance(lb.getOwnerUserId()), lbToDelete );
        }
      }
    }catch (EventFailedException e){
        LOG.error( "Error deleting the loadbalancer: " + e.getMessage(), e );
    	final String reason = e.getCause() != null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
    	throw new InternalFailure400Exception( String.format("Failed to delete the loadbalancer: %s", reason), e );
    }catch (LoadBalancingException e){
    	throw new InternalFailure400Exception(e.getMessage());
    }catch ( Exception e ) {
      // success if the lb is not found in the system
      if ( !(e.getCause() instanceof NoSuchElementException) ) {
        LOG.error( "Error deleting the loadbalancer: " + e.getMessage(), e );
        final String reason = "internal error";
        throw new InternalFailure400Exception( String.format("Failed to delete the loadbalancer: %s", reason), e );
      }
    }
    DeleteLoadBalancerResult result = new DeleteLoadBalancerResult();
    reply.setDeleteLoadBalancerResult( result );
    reply.set_return( true );
    return reply;
  }
  
  public CreateLoadBalancerListenersResponseType createLoadBalancerListeners(CreateLoadBalancerListenersType request) throws EucalyptusCloudException {
	  final CreateLoadBalancerListenersResponseType reply = request.getReply( );
	  final Context ctx = Contexts.lookup( );
	  final String lbName = request.getLoadBalancerName();
	  final List<Listener> listeners = request.getListeners().getMember();
	  
	  LoadBalancer lb;
	  try{
	  		lb = LoadBalancers.getLoadbalancer(ctx, lbName);
	  }catch(Exception ex){
	  		throw new AccessPointNotFoundException();
	  }
  	  //IAM support to restricted lb modification
  	  if(lb != null && !LoadBalancingMetadatas.filterPrivileged().apply(lb)) {
	       throw new AccessPointNotFoundException(); 
	  }
	  
	  if(listeners!=null)
		  LoadBalancers.validateListener(lb, listeners);
	  
    try{
      for(final Listener l : listeners){
        if("HTTPS".equals(l.getProtocol().toUpperCase()) || "SSL".equals(l.getProtocol().toUpperCase())){
          final String certArn = l.getSSLCertificateId();
          if(certArn==null || certArn.length()<=0)
            throw new InvalidConfigurationRequestException("SSLCertificateId is required for HTTPS or SSL protocol");
          LoadBalancers.checkSSLCertificate(ctx.getUser().getUserId(), certArn);
        }
      }
    }catch(Exception ex){
      if(! (ex instanceof LoadBalancingException)){
          LOG.error("failed to check SSL certificate Id", ex);
          ex = new InternalFailure400Exception("failed to check SSL certificate Id", ex);
      }
      throw (LoadBalancingException) ex;
    }
	    
	  try{
    		CreateListenerEvent evt = new CreateListenerEvent();
    		evt.setLoadBalancer(lbName);
    		evt.setContext(ctx);
    		evt.setListeners(listeners);
    		ActivityManager.getInstance().fire(evt);
	  }catch(final EventFailedException e){
    		LOG.error("failed to handle CreateListener event", e);
    		final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getMessage() : "internal error";
    		throw new InternalFailure400Exception(String.format("Failed to create listener: %s", reason), e );
	  }
	  try{
		  LoadBalancers.createLoadbalancerListener(lbName,  ctx, listeners);
	  }catch(final LoadBalancingException ex){
		  throw ex;
	  }catch(final Exception e){
		  final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getMessage() : "internal error";
    	  throw new InternalFailure400Exception(String.format("Failed to create listener: %s", reason), e );
	  }
	  reply.set_return(true);
	  return reply;
  }
  
  public DeleteLoadBalancerListenersResponseType deleteLoadBalancerListeners(DeleteLoadBalancerListenersType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerListenersResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final Collection<Integer> listenerPorts;
    try{
      listenerPorts = Collections2.transform(
        request.getLoadBalancerPorts().getMember(), new Function<String, Integer>(){
          @Override
          public Integer apply(final String input){
            return new Integer(input);
          }
        });
    }catch(Exception ex){
      throw new InvalidConfigurationRequestException("Invalid port number");
    }

    final LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(Exception ex){
      LOG.error("failed to query loadbalancer due to unknown reason", ex);
	  final String reason = ex.getCause()!=null && ex.getCause().getMessage()!=null ? ex.getMessage() : "internal error";
      throw new InternalFailure400Exception( String.format("Failed to delete the listener: %s", reason), ex );
    }

   //IAM support to restricted lb modification
   if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) {
     throw new AccessPointNotFoundException();
   }

   final Function<Void, Collection<Integer>> filter = new Function<Void, Collection<Integer>>(){
      @Override
      public Collection<Integer> apply(Void v){
         final Collection<Integer> filtered = Sets.newHashSet();
         for(Integer port : listenerPorts){
           final LoadBalancerListenerCoreView found = lb.findListener(port);
           if(found!=null)
             filtered.add(port);
         }
         return filtered;
      }
    };

    final Collection<Integer> toDelete = Entities.asTransaction(LoadBalancer.class, filter).apply(null);

    final Predicate<Collection<Integer>> remover = new Predicate<Collection<Integer>>(){
      @Override
      public boolean apply(Collection<Integer> listeners){
        for(Integer port : listenerPorts){
          try{
            final LoadBalancerListener exist = Entities.uniqueResult(LoadBalancerListener.named(lb, port));
            Entities.delete(exist);
	       }catch(NoSuchElementException ex){
          }catch(Exception ex){
	          LOG.error("Failed to delete the listener", ex);
	          throw Exceptions.toUndeclared(ex);
	       }
        }
        return true;
      }
    };

    try{
      DeleteListenerEvent evt = new DeleteListenerEvent();
      evt.setLoadBalancer(lbName);
      evt.setContext(ctx);
      evt.setPorts(listenerPorts);
      ActivityManager.getInstance().fire(evt);
    }catch(EventFailedException e){
      LOG.error("failed to handle DeleteListener event", e);
      final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to delete listener: %s",reason),e );
    }

    try{
    	Entities.asTransaction(LoadBalancerListener.class, remover).apply(toDelete);
    }catch(final Exception ex){
    	final String reason = ex.getCause()!=null && ex.getCause().getMessage()!=null ? ex.getCause().getMessage() : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to delete listener: %s",reason), ex );
    }
    reply.set_return(true);

    return reply;
  }
  
  public RegisterInstancesWithLoadBalancerResponseType registerInstancesWithLoadBalancer(
      final RegisterInstancesWithLoadBalancerType request
  ) throws EucalyptusCloudException {
    final RegisterInstancesWithLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName( );
    final Collection<Instance> instances = request.getInstances( ).getMember( );

    final LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer( ctx, lbName );
    }catch(final Exception ex){
      throw new AccessPointNotFoundException();
    }
    if( !LoadBalancingMetadatas.filterPrivileged( ).apply( lb ) ) { // IAM policy restriction
      throw new AccessPointNotFoundException( );
    }

    final Set<String> backends = Sets.newHashSet(
        Iterables.transform( lb.getBackendInstances( ), LoadBalancerBackendInstanceCoreView.instanceId( ) ) );

    if ( lb.getVpcId( ) != null ) {
      final List<RunningInstancesItemType> instanceItems =
          EucalyptusActivityTasks.getInstance( ).describeSystemInstances(
              Lists.newArrayList( Iterables.transform( instances, Instance.instanceId( ) ) ) );
      for ( final RunningInstancesItemType instanceItem : instanceItems ) {
        if ( !lb.getVpcId( ).equals( instanceItem.getVpcId( ) ) ) {
          throw new InvalidConfigurationRequestException( "Invalid instance(s) for load balancer." );
        }
      }
    }

    final Predicate<LoadBalancer> creator = new Predicate<LoadBalancer>(){
          @Override
          public boolean apply( LoadBalancer lb ) {
            for(Instance vm : instances){
              if(lb.hasBackendInstance(vm.getInstanceId()))
                continue;	// the vm instance is already registered
              try{
                final LoadBalancerBackendInstance beInstance =
                    LoadBalancerBackendInstance.newInstance(ownerFullName, lb, vm.getInstanceId());
                beInstance.setState(LoadBalancerBackendInstance.STATE.OutOfService);
                Entities.persist(beInstance);
              }catch(final LoadBalancingException ex){
                throw Exceptions.toUndeclared(ex);
              }
            }
            return true;
          }
    };

   try{
     final RegisterInstancesEvent evt = new RegisterInstancesEvent();
     evt.setLoadBalancer(lbName);
     evt.setContext(ctx);
     evt.setInstances(instances);
     ActivityManager.getInstance().fire(evt);
   }catch(EventFailedException e){
     LOG.error("failed to handle RegisterInstances event", e);
     final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
     throw new InternalFailure400Exception(String.format("Failed to register instances: %s", reason), e );
   }

    try{
      Entities.asTransaction( LoadBalancerBackendInstance.class, creator ).apply( lb );
      Iterables.addAll( backends, Iterables.transform( instances, Instance.instanceId( ) ) );
    }catch(Exception ex){
      throw handleException( ex );
    }

    final Instances returnInstances = new Instances();
    Iterables.addAll( returnInstances.getMember( ), Iterables.transform( backends, Instance.instance( ) ) );
    reply.getRegisterInstancesWithLoadBalancerResult( ).setInstances( returnInstances );
    return reply;
  }

  public DeregisterInstancesFromLoadBalancerResponseType deregisterInstancesFromLoadBalancer(DeregisterInstancesFromLoadBalancerType request) throws EucalyptusCloudException {
	  DeregisterInstancesFromLoadBalancerResponseType reply = request.getReply( );
	  final Context ctx = Contexts.lookup( );
	  final String lbName = request.getLoadBalancerName();
	  final Collection<Instance> instances = request.getInstances().getMember();
	    
	  LoadBalancer lb;
	  try{
		  lb = LoadBalancers.getLoadbalancer(ctx, lbName);
	  }catch(Exception ex){
		  throw new AccessPointNotFoundException();
	  }
	  
	  if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
		  throw new AccessPointNotFoundException();
	  }
	  	 
	  final List<LoadBalancerBackendInstanceCoreView> allInstances = 
			  Lists.newArrayList(lb.getBackendInstances());
	  final Function<LoadBalancer, Collection<LoadBalancerBackendInstanceCoreView>> filter = new Function<LoadBalancer, Collection<LoadBalancerBackendInstanceCoreView>>(){
	    	@Override
	    	public Collection<LoadBalancerBackendInstanceCoreView> apply(LoadBalancer lb){
	    		Collection<LoadBalancerBackendInstanceCoreView> filtered = Sets.newHashSet();
	    		for(final LoadBalancerBackendInstanceCoreView be: lb.getBackendInstances()){
	    			 for(Instance inst : instances){
	    				 if(be.getInstanceId()!=null && be.getInstanceId().equals(inst.getInstanceId())){
	    					 filtered.add(be);
	    					 break;
	    				 }
	    			 }
	    		}
	    		return filtered;
	    	}
	   };
	    
	  final Collection<LoadBalancerBackendInstanceCoreView> instancesToRemove = Entities.asTransaction(LoadBalancer.class, filter).apply(lb);
	  if(instancesToRemove==null){
	  	reply.set_return(false);
	  	return reply;
	  }
	  final Predicate<Void> remover = new Predicate<Void>(){
	  	@Override
	  	public boolean apply(Void v){
	      	for(final LoadBalancerBackendInstanceCoreView instanceView : instancesToRemove){
	      		final LoadBalancerBackendInstance sample = LoadBalancerBackendInstanceEntityTransform.INSTANCE.apply(instanceView);
	      	    LoadBalancerBackendInstance toDelete;
	      	    try{
	      	    	toDelete = Entities.uniqueResult(sample);
	      	    }catch(NoSuchElementException ex){
	      	    	toDelete=null;
	      	    	throw Exceptions.toUndeclared(new InvalidEndPointException());
	      	    }catch(Exception ex){
	      	    	LOG.error("Can't query loadbalancer backend instance for "+instanceView.getInstanceId(), ex);
	      	    	toDelete=null;
	      	    }	
	      	    if(toDelete==null)
	      			continue;
	      		Entities.delete(toDelete);
	      	}
	  	    return true;
	  	}
	  };
	  final Function<Void, ArrayList<Instance>> finder = new Function<Void, ArrayList<Instance>>(){
	  @Override
	  public ArrayList<Instance> apply(Void v){
	  	  	 LoadBalancer lb;
	  	  	 try{
	  	  		lb= LoadBalancers.getLoadbalancer(ctx, lbName);
	  	  	 }catch(Exception ex){
	   	    	LOG.warn("No loadbalancer is found with name="+lbName);    
	   	    	return Lists.newArrayList();
	   	    }
	  	  	Entities.refresh(lb);
	  	    ArrayList<Instance> result = new ArrayList<>(Collections2.transform(lb.getBackendInstances(), new Function<LoadBalancerBackendInstanceCoreView, Instance>(){
		  		@Override
		  		public Instance apply(final LoadBalancerBackendInstanceCoreView input){
		  			final Instance newInst = new Instance();
		  			newInst.setInstanceId(input.getInstanceId());
		  			return newInst;
		  		}}));
	  	    return result;
	  	    }
	  	};
	    
	  try{
		  DeregisterInstancesEvent evt = new DeregisterInstancesEvent();
		  evt.setLoadBalancer(lbName);
		  evt.setContext(ctx);
		  evt.setInstances(instances);
		  ActivityManager.getInstance().fire(evt);
	  }catch(EventFailedException e){
		  LOG.error("failed to handle DeregisterInstances event", e);
    	  final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
    	  throw new InternalFailure400Exception(String.format("Failed to deregister instances: %s", reason),e );
	  }
	    
	  try{
		  reply.set_return(Entities.asTransaction(LoadBalancerBackendInstance.class, remover).apply(null));
		  allInstances.removeAll(instancesToRemove);
	  }catch(final Exception ex){
		  final String reason = ex.getCause()!=null && ex.getCause().getMessage()!=null ? ex.getCause().getMessage() : "internal error";
    	  throw new InternalFailure400Exception(String.format("Failed to deregister instances: %s", reason), ex );
	  }
	  DeregisterInstancesFromLoadBalancerResult result = new DeregisterInstancesFromLoadBalancerResult();
	  Instances returnInstances = new Instances();
	  returnInstances.setMember(
			  new ArrayList<>(Collections2.transform(allInstances, new Function<LoadBalancerBackendInstanceCoreView, Instance>(){
			  		@Override
			  		public Instance apply(final LoadBalancerBackendInstanceCoreView input){
			  			final Instance newInst = new Instance();
			  			newInst.setInstanceId(input.getInstanceId());
			  			return newInst;
			  		}}))
			  );
			  //Entities.asTransaction(LoadBalancer.class, finder).apply(null));
	  result.setInstances(returnInstances);
	  reply.setDeregisterInstancesFromLoadBalancerResult(result);
	  return reply;
  }
  
  public EnableAvailabilityZonesForLoadBalancerResponseType enableAvailabilityZonesForLoadBalancer(EnableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
    final EnableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName( );
    final Collection<String> requestedZones = request.getAvailabilityZones().getMember();

    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(final Exception ex){
      throw new AccessPointNotFoundException();
    }

    if( !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }

    final Set<String> allZones = Sets.newHashSet( Iterables.transform( lb.getZones(), LoadBalancerZoneCoreView.name( ) ) );

    // check for a default VPC
    final Optional<VpcType> vpcOptional = EucalyptusActivityTasks.getInstance( ).defaultVpc( lookupAdmin( ctx ) );
    final Map<String,String> zoneToSubnetIdMap = Maps.newHashMap( );
    if ( vpcOptional.isPresent( ) && Objects.equals( lb.getVpcId( ), vpcOptional.get( ) ) ) {
      final List<SubnetType> subnets = EucalyptusActivityTasks.getInstance( ).describeSubnetsByZone( lb.getVpcId( ), true, requestedZones );
      CollectionUtils.putAll( subnets, zoneToSubnetIdMap, SubnetType.zone( ), SubnetType.id( ) );
      if ( requestedZones.size( ) != zoneToSubnetIdMap.size( ) ) {
        throw new InvalidConfigurationRequestException( "Cannot enable zone for VPC loadbalancer, default subnet not found." );
      }
    } else if ( lb.getVpcId( ) != null ) {
      throw new InvalidConfigurationRequestException( "Cannot enable zone for VPC loadbalancer" );
    }

    if( !requestedZones.isEmpty( ) ){
      try{
        final EnabledZoneEvent evt = new EnabledZoneEvent( );
        evt.setLoadBalancer( lbName );
        evt.setContext( ctx );
        evt.setZones( requestedZones );
        evt.setZoneToSubnetIdMap( zoneToSubnetIdMap );
        ActivityManager.getInstance( ).fire( evt );
      }catch(EventFailedException e){
        LOG.error("failed to handle EnabledZone event", e);
        final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to enable zones: %s", reason),e );
      }
    }

    allZones.addAll( requestedZones );

    final EnableAvailabilityZonesForLoadBalancerResult result = new EnableAvailabilityZonesForLoadBalancerResult();
    final AvailabilityZones availZones = new AvailabilityZones( );
    availZones.getMember( ).addAll( allZones );
    reply.getEnableAvailabilityZonesForLoadBalancerResult( ).setAvailabilityZones( availZones );

    return reply;
  }

  public DisableAvailabilityZonesForLoadBalancerResponseType disableAvailabilityZonesForLoadBalancer(DisableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
	  final DisableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
	  final Context ctx = Contexts.lookup( );
	  final String lbName = request.getLoadBalancerName();
	  final Collection<String> zones = request.getAvailabilityZones().getMember();
	
	  LoadBalancer lb;
	  try{
		  lb = LoadBalancers.getLoadbalancer(ctx, lbName);
	  }catch(final Exception ex){
		  throw new AccessPointNotFoundException();
	  }
	
	  if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
		  throw new AccessPointNotFoundException();
	  }
	  
	  if(zones != null && zones.size()>0){
		 try{
	    	final DisabledZoneEvent evt = new DisabledZoneEvent();
	    	evt.setLoadBalancer(lbName);
	    	evt.setContext(ctx);
	    	evt.setZones(zones);
	    	ActivityManager.getInstance().fire(evt);
	     }catch(EventFailedException e){
	    	LOG.error("failed to handle DisabledZone event", e);
	    	final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
	    	throw new InternalFailure400Exception(String.format("Failed to disable zones: %s", reason), e );
	    }  
	  }
	  
	  List<String> availableZones = Lists.newArrayList();
	  try{
		  final LoadBalancer updatedLb = LoadBalancers.getLoadbalancer(ctx, lbName);
		  availableZones = Lists.transform(LoadBalancers.findZonesInService(updatedLb), new Function<LoadBalancerZoneCoreView, String>(){
			  @Override
			  public String apply(@Nullable LoadBalancerZoneCoreView arg0) {
					return arg0.getName();
					}
		    });
	  }catch(Exception ex){
    }
	  final DisableAvailabilityZonesForLoadBalancerResult result = new DisableAvailabilityZonesForLoadBalancerResult();
  	  final AvailabilityZones availZones = new AvailabilityZones();
  	  availZones.setMember(Lists.newArrayList(availableZones));
  	  result.setAvailabilityZones(availZones);
  	  reply.setDisableAvailabilityZonesForLoadBalancerResult(result);
  	  reply.set_return(true);
	  return reply;
  }
  
  public ConfigureHealthCheckResponseType configureHealthCheck(ConfigureHealthCheckType request) throws EucalyptusCloudException {
    ConfigureHealthCheckResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
	final String lbName = request.getLoadBalancerName();
	final HealthCheck hc = request.getHealthCheck();
	final Integer healthyThreshold = hc.getHealthyThreshold();
	if (healthyThreshold == null)
		throw new InvalidConfigurationRequestException("Healthy tresholds must be specified");
	final Integer interval = hc.getInterval();
	if(interval == null)
		throw new InvalidConfigurationRequestException("Interval must be specified");
	final String target = hc.getTarget();
	if(target == null)
		throw new InvalidConfigurationRequestException("Target must be specified");
	
	final Integer timeout = hc.getTimeout();
    if(timeout==null)
    	throw new InvalidConfigurationRequestException("Timeout must be specified");
    final Integer unhealthyThreshold = hc.getUnhealthyThreshold();
    if(unhealthyThreshold == null)
    	throw new InvalidConfigurationRequestException("Unhealthy tresholds must be specified");
    
    if(interval < MIN_HEALTHCHECK_INTERVAL_SEC){
    	throw new InvalidConfigurationRequestException(
    			String.format("Interval must be longer than %d seconds", MIN_HEALTHCHECK_INTERVAL_SEC));
    }
    if(healthyThreshold < MIN_HEALTHCHECK_THRESHOLDS){
    	throw new InvalidConfigurationRequestException(
    			String.format("Healthy thresholds must be larger than %d", MIN_HEALTHCHECK_THRESHOLDS));
    }
    if(unhealthyThreshold < MIN_HEALTHCHECK_THRESHOLDS) {
    	throw new InvalidConfigurationRequestException(
    			String.format("Unhealthy thresholds must be larger than %d", MIN_HEALTHCHECK_THRESHOLDS));
    }
    
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(Exception ex){
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }
    
    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }

    try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ){
      final LoadBalancer update = Entities.uniqueResult(lb);
      update.setHealthCheck(healthyThreshold, interval, target, timeout, unhealthyThreshold);
      Entities.persist(update);
      db.commit();
    }catch(final IllegalArgumentException ex){
      throw new InvalidConfigurationRequestException(ex.getMessage());
    }catch(final Exception ex){
      LOG.error("failed to persist health check config", ex);
      throw new InternalFailure400Exception("Failed to persist the health check config", ex);
    }
    ConfigureHealthCheckResult result = new ConfigureHealthCheckResult();
    result.setHealthCheck(hc);
    reply.setConfigureHealthCheckResult(result);
    return reply;
  }

  public DescribeInstanceHealthResponseType describeInstanceHealth(DescribeInstanceHealthType request) throws EucalyptusCloudException {
    DescribeInstanceHealthResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
 	final String lbName = request.getLoadBalancerName();
 	Instances instances = request.getInstances();
 	
	LoadBalancer lb;
	try{
		lb= LoadBalancers.getLoadbalancer(ctx, lbName);
	}catch(NoSuchElementException ex){
		throw new AccessPointNotFoundException();
    }catch(Exception ex){
    	throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }
 	
    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
	    throw new AccessPointNotFoundException();
    }

	List<LoadBalancerBackendInstanceCoreView> lbInstances = Lists.newArrayList(lb.getBackendInstances());
	List<LoadBalancerBackendInstanceCoreView> instancesFound;
	List<LoadBalancerBackendInstanceCoreView> stateOutdated= Lists.newArrayList();
	
	final int healthyTimeoutSec = 3*(lb.getHealthCheckInterval() * lb.getHealthyThreshold());
	long currentTime = System.currentTimeMillis();
	
 	if(instances != null && instances.getMember()!= null && instances.getMember().size()>0){
 		instancesFound = Lists.newArrayList();
 		for(Instance inst : instances.getMember()){
 			String instId = inst.getInstanceId();
 			for(final LoadBalancerBackendInstanceCoreView lbInstance : lbInstances){
 				if(instId.equals(lbInstance.getInstanceId())){
 					instancesFound.add(lbInstance);
 					break;
 				}
 			}
 		}
 	}else{
 		instancesFound = Lists.newArrayList(lb.getBackendInstances());
 	}
 	
 	final ArrayList<InstanceState> stateList = Lists.newArrayList();
 	for(final LoadBalancerBackendInstanceCoreView instance : instancesFound){
		boolean outdated = false;
 		Date lastUpdated;
		if((lastUpdated = instance.instanceStateLastUpdated()) != null){
			final int diffSec = (int)((currentTime - lastUpdated.getTime())/1000.0);
			if(LoadBalancerBackendInstance.STATE.InService.equals(instance.getState()) &&
					diffSec > healthyTimeoutSec){
				stateOutdated.add(instance);
				outdated = true;
			}
		}
 		
 		InstanceState state = new InstanceState();
 		state.setInstanceId(instance.getDisplayName());
 		if(outdated){
 			state.setState(LoadBalancerBackendInstance.STATE.OutOfService.toString());
			state.setReasonCode("ELB");
 			state.setDescription("Internal error: instance health not updated for extended period of time");
 		}else{
 			state.setState(instance.getState().name());
 			if(instance.getState().equals(LoadBalancerBackendInstance.STATE.OutOfService) && instance.getReasonCode()!=null)
	 			state.setReasonCode(instance.getReasonCode());
	 		if(instance.getDescription()!=null)
	 			state.setDescription(instance.getDescription());
 		}
 		
 		stateList.add(state);
 	}
 	
 	if(! stateOutdated.isEmpty()){
 		try ( TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ){
	 		for(final LoadBalancerBackendInstanceCoreView instanceView : stateOutdated){
	 			final LoadBalancerBackendInstance sample = LoadBalancerBackendInstanceEntityTransform.INSTANCE.apply(instanceView);
	 			final LoadBalancerBackendInstance update = Entities.uniqueResult(sample);
	 			update.setState(LoadBalancerBackendInstance.STATE.OutOfService);
	 			update.setReasonCode("ELB");
	 			update.setDescription("Internal error: instance health not updated for extended period of time");
	 			Entities.persist(update);
	 		}
	 		db.commit();
 		} catch(final Exception ex){
 		}
 	}
 	
 	final InstanceStates states = new InstanceStates();
 	states.setMember(stateList);
 	final DescribeInstanceHealthResult result = new DescribeInstanceHealthResult();
 	result.setInstanceStates(states);
    reply.setDescribeInstanceHealthResult(result);
    return reply;
  }
  
  public SetLoadBalancerListenerSSLCertificateResponseType setLoadBalancerListenerSSLCertificate(SetLoadBalancerListenerSSLCertificateType request) throws EucalyptusCloudException {
    final SetLoadBalancerListenerSSLCertificateResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final int lbPort = request.getLoadBalancerPort();
    final String certArn = request.getSSLCertificateId();
    
    if(lbPort <=0 || lbPort > 65535)
      throw new InvalidConfigurationRequestException("Invalid port");
   
    if(certArn == null || certArn.length()<=0)
      throw new InvalidConfigurationRequestException("SSLCertificateId is not specified");
    
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(Exception ex){
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }
    
    try{
      LoadBalancers.setLoadBalancerListenerSSLCertificate(lb, lbPort, certArn);
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to set loadbalancer listener SSL certificate", ex);
      throw new InternalFailure400Exception("Failed to set loadbalancer listener SSL certificate", ex);
    }
    reply.setSetLoadBalancerListenerSSLCertificateResult(new SetLoadBalancerListenerSSLCertificateResult());
    reply.set_return(true);
    return reply;
  }
  
  
  public DescribeLoadBalancerPolicyTypesResponseType describeLoadBalancerPolicyTypes(DescribeLoadBalancerPolicyTypesType request) throws EucalyptusCloudException {
    final List<PolicyTypeDescription> policyTypes = Lists.newArrayList();
    try{ 
      final List<LoadBalancerPolicyTypeDescription> internalPolicyTypes  = LoadBalancerPolicies.getLoadBalancerPolicyTypeDescriptions();
      for(final LoadBalancerPolicyTypeDescription from : internalPolicyTypes){
        policyTypes.add(LoadBalancerPolicies.AsPolicyTypeDescription.INSTANCE.apply(from));
      }
    }catch(final Exception ex){
      LOG.error("Failed to retrieve policy types", ex);
      throw new InternalFailure400Exception("Failed to retrieve policy types", ex);
    }
    final DescribeLoadBalancerPolicyTypesResponseType reply = request.getReply( );
    final PolicyTypeDescriptions desc = new PolicyTypeDescriptions();
    desc.setMember((ArrayList<PolicyTypeDescription>) policyTypes);
    final DescribeLoadBalancerPolicyTypesResult result = new DescribeLoadBalancerPolicyTypesResult();
    result.setPolicyTypeDescriptions(desc);
    reply.setDescribeLoadBalancerPolicyTypesResult(result);
    reply.set_return(true);
    return reply;
  }

  public DescribeLoadBalancerPoliciesResponseType describeLoadBalancerPolicies(DescribeLoadBalancerPoliciesType request) throws EucalyptusCloudException {
    DescribeLoadBalancerPoliciesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final PolicyNames policyNames = request.getPolicyNames();

    if(lbName == null || lbName.isEmpty()){
      // return sample policies according to ELB API
      final DescribeLoadBalancerPoliciesResult result = new DescribeLoadBalancerPoliciesResult();
      final PolicyDescriptions descs = new PolicyDescriptions();
      final List<PolicyDescription> policies = LoadBalancerPolicies.getSamplePolicyDescription();
      descs.setMember((ArrayList<PolicyDescription>) policies);
      result.setPolicyDescriptions(descs);
      reply.setDescribeLoadBalancerPoliciesResult(result);
    }else{
      LoadBalancer lb;
      try{
        lb= LoadBalancers.getLoadbalancer(ctx, lbName);
      }catch(NoSuchElementException ex){
        throw new AccessPointNotFoundException();
      }catch(final Exception ex){
        LOG.error("Failed to find the loadbalancer", ex);
        throw new InternalFailure400Exception("Failed to find the loadbalancer");
      }

      if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
        throw new AccessPointNotFoundException();
      }
      List<LoadBalancerPolicyDescription> lbPolicies;
      try{
        if(policyNames != null && policyNames.getMember()!=null && policyNames.getMember().size()>0)
          lbPolicies = LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb, policyNames.getMember());
        else
          lbPolicies = LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb);
      }catch(final Exception ex){
        LOG.error("Failed to find policy descriptions", ex);
        throw new InternalFailure400Exception("Failed to retrieve the policy descriptions", ex);
      }
      final DescribeLoadBalancerPoliciesResult result = new DescribeLoadBalancerPoliciesResult();
      final PolicyDescriptions descs = new PolicyDescriptions();
      final List<PolicyDescription> policies = Lists.newArrayList();
      for(final LoadBalancerPolicyDescription lbPolicy : lbPolicies){
        policies.add(LoadBalancerPolicies.AsPolicyDescription.INSTANCE.apply(lbPolicy));
      }
      descs.setMember((ArrayList<PolicyDescription>) policies);
      result.setPolicyDescriptions(descs);
      reply.setDescribeLoadBalancerPoliciesResult(result);
    }

    reply.set_return(true);
    return reply;
  }  

  public CreateLoadBalancerPolicyResponseType createLoadBalancerPolicy(CreateLoadBalancerPolicyType request) throws EucalyptusCloudException {
    CreateLoadBalancerPolicyResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final String policyTypeName = request.getPolicyTypeName();
    if(lbName==null || lbName.isEmpty())
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    if(policyName==null || policyName.isEmpty())
      throw new InvalidConfigurationRequestException("policy name must be specified");
    if(policyTypeName==null || policyTypeName.isEmpty())
      throw new InvalidConfigurationRequestException("policy type name must be specified");
    
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(final Exception ex){
      LOG.error("Failed to find the loadbalancer", ex);
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }

    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }
    
    final List<PolicyAttribute> attrs =
        (request.getPolicyAttributes() != null ? request.getPolicyAttributes().getMember() : Lists.<PolicyAttribute>newArrayList());
    try{
      LoadBalancerPolicies.addLoadBalancerPolicy(lb, policyName, policyTypeName, attrs);
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to add the policy", ex);
      throw new InternalFailure400Exception("Failed to add the policy", ex);
    }
    reply.set_return(true);
    return reply;
  }
  
  public DeleteLoadBalancerPolicyResponseType deleteLoadBalancerPolicy(DeleteLoadBalancerPolicyType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerPolicyResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    if(lbName==null || lbName.isEmpty())
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    if(policyName==null || policyName.isEmpty())
      throw new InvalidConfigurationRequestException("policy name must be specified");
    
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(final Exception ex){
      LOG.error("Failed to find the loadbalancer", ex);
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }

    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }
    
    try{
      LoadBalancerPolicies.deleteLoadBalancerPolicy(lb,  policyName);
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to delete policy", ex);
      throw new InternalFailure400Exception("Failed to delete policy", ex);
    }
    reply.set_return(true);
    return reply;
  }

  public CreateLBCookieStickinessPolicyResponseType createLBCookieStickinessPolicy(CreateLBCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateLBCookieStickinessPolicyResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final Long expiration = request.getCookieExpirationPeriod();
    
    if(lbName==null || lbName.isEmpty())
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    if(policyName==null || policyName.isEmpty())
      throw new InvalidConfigurationRequestException("Policy name must be specified");
    if(expiration == null || expiration <= 0)
      throw new InvalidConfigurationRequestException("Expiration period must be bigger than 0");
    
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(final NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(final Exception ex){
      LOG.error("Failed to find the loadbalancer", ex);
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }

    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }
    
    try{
      final PolicyAttribute attr = new PolicyAttribute();
      attr.setAttributeName("CookieExpirationPeriod");
      attr.setAttributeValue(expiration.toString());
      LoadBalancerPolicies.addLoadBalancerPolicy(lb, policyName, "LBCookieStickinessPolicyType", 
          Lists.newArrayList(attr));
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to create policy", ex);
      throw new InternalFailure400Exception("Failed to create policy", ex);
    }
    reply.set_return(true);
    return reply;
  }

  public CreateAppCookieStickinessPolicyResponseType createAppCookieStickinessPolicy(CreateAppCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateAppCookieStickinessPolicyResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final String cookieName = request.getCookieName();
    
    if(lbName==null || lbName.isEmpty())
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    if(policyName==null || policyName.isEmpty())
      throw new InvalidConfigurationRequestException("Policy name must be specified");
    if(cookieName == null)
      throw new InvalidConfigurationRequestException("Cookie name must be specified");
    
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(final NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(final Exception ex){
      LOG.error("Failed to find the loadbalancer", ex);
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }

    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }
    
    try{
      final PolicyAttribute attr = new PolicyAttribute();
      attr.setAttributeName("CookieName");
      attr.setAttributeValue(cookieName);
      LoadBalancerPolicies.addLoadBalancerPolicy(lb, policyName, "AppCookieStickinessPolicyType", 
          Lists.newArrayList(attr));
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to create policy", ex);
      throw new InternalFailure400Exception("Failed to create policy", ex);
    }
    reply.set_return(true);
    return reply;
  }

  public SetLoadBalancerPoliciesOfListenerResponseType setLoadBalancerPoliciesOfListener(SetLoadBalancerPoliciesOfListenerType request) throws EucalyptusCloudException {
    final SetLoadBalancerPoliciesOfListenerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final int portNum = request.getLoadBalancerPort();
    final PolicyNames pNames = request.getPolicyNames();
    
    if(lbName==null || lbName.isEmpty())
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    if(portNum <0 || portNum >65535)
      throw new InvalidConfigurationRequestException("Invalid port number specified");
    final List<String> policyNames = pNames.getMember();
    
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    }catch(final NoSuchElementException ex){
      throw new AccessPointNotFoundException();
    }catch(final Exception ex){
      LOG.error("Failed to find the loadbalancer", ex);
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }
    if( lb!=null && !LoadBalancingMetadatas.filterPrivileged().apply(lb) ) { // IAM policy restriction
      throw new AccessPointNotFoundException();
    }
    try{
      LoadBalancerListener listener = null;
      for(final LoadBalancerListenerCoreView l : lb.getListeners()){
        if(l.getLoadbalancerPort() == portNum){
          listener = LoadBalancerListenerEntityTransform.INSTANCE.apply(l);
          break;
        }
      }
      if(listener == null)
        throw new ListenerNotFoundException();
        
      final List<LoadBalancerPolicyDescription> policies = Lists.newArrayList();
      if(policyNames!=null){
        for(final String policyName : policyNames){
          try{
            policies.add(LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb, policyName));
          }catch(final Exception ex){
            throw new PolicyNotFoundException();
          }
        }
      }
      final List<LoadBalancerPolicyDescription> oldPolicies = LoadBalancerPolicies.getPoliciesOfListener(listener);
      LoadBalancerPolicies.removePoliciesFromListener(listener);
      try{
        if(policies.size()>0)
          LoadBalancerPolicies.addPoliciesToListener(listener, policies);
      }catch(final Exception ex){
        LoadBalancerPolicies.addPoliciesToListener(listener, oldPolicies);
        throw ex;
      }
    }catch(final LoadBalancingException ex){
      throw ex;
    }catch(final Exception ex){
      LOG.error("Failed to set policies to listener", ex);
      throw new InternalFailure400Exception("Failed to set policies to listener", ex);
    }
    
    return reply;
  }

  public SetLoadBalancerPoliciesForBackendServerResponseType setLoadBalancerPoliciesForBackendServer(SetLoadBalancerPoliciesForBackendServerType request) throws EucalyptusCloudException {
    SetLoadBalancerPoliciesForBackendServerResponseType reply = request.getReply( );
    return reply;
  }

  public ApplySecurityGroupsToLoadBalancerResponseType applySecurityGroupsToLoadBalancer(
      final ApplySecurityGroupsToLoadBalancerType request
  ) throws EucalyptusCloudException {
    final ApplySecurityGroupsToLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount( ).getAccountNumber();
    final String adminUserId;
    try {
      adminUserId = ctx.getAccount().lookupAdmin().getUserId();
    } catch ( final AuthException e ) {
      throw new LoadBalancingException( "Error accessing default security group" );
    }
    final Set<String> securityGroupIds = Sets.newHashSet( request.getSecurityGroups( ).getMember( ) );
    final List<SecurityGroupItemType> groups =
        EucalyptusActivityTasks.getInstance( ).describeUserSecurityGroupsById( adminUserId, null, securityGroupIds );
    if ( groups.size( ) != securityGroupIds.size( ) ) {
      throw new LoadBalancingBackendClientException( "InvalidSecurityGroup", "Invalid security group(s)" );
    }
    final Function<String, ApplySecurityGroupsEvent> updateSecurityGroups = new Function<String, ApplySecurityGroupsEvent>( ) {
      @Override
      public ApplySecurityGroupsEvent apply( final String identifier ) {
        try {
          final LoadBalancer example = LoadBalancer.namedByAccountId( accountNumber, identifier );
          final LoadBalancer loadBalancer = Entities.uniqueResult( example );
          if ( RestrictedTypes.filterPrivileged( ).apply( loadBalancer ) ) {
            final List<SecurityGroupItemType> sortedGroups =
                Ordering.natural( ).onResultOf( SecurityGroupItemType.groupId( ) ).sortedCopy( groups );
            loadBalancer.setSecurityGroupRefs( Lists.newArrayList( Iterables.transform(
                sortedGroups,
                TypeMappers.lookup( SecurityGroupItemType.class, LoadBalancerSecurityGroupRef.class ) ) ) );
            final ApplySecurityGroupsEvent evt = new ApplySecurityGroupsEvent( );
            evt.setContext( ctx );
            evt.setLoadBalancer( loadBalancer.getDisplayName( ) );
            evt.setSecurityGroupIdsToNames( CollectionUtils.putAll(
                sortedGroups,
                Maps.<String, String>newLinkedHashMap( ),
                SecurityGroupItemType.groupId( ),
                SecurityGroupItemType.groupName( ) ) );
            return evt;
          } else {
            throw new NoSuchElementException( );
          }
        } catch ( NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new AccessPointNotFoundException( ) );
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    final ApplySecurityGroupsEvent event;
    try {
      event = Entities.asTransaction( LoadBalancer.class, updateSecurityGroups ).apply( request.getLoadBalancerName( ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    try {
      ActivityManager.getInstance( ).fire( event );
    } catch( final EventFailedException e ){
      LOG.error("Failed to handle new apply security groups event", e);
      final String reason = Optional.fromNullable( e.getCause( ) ).transform( Exceptions.message( ) ).or( "internal error" );
      throw new InternalFailure400Exception(String.format("Failed to apply security groups to loadbalancer: %s", reason), e);
    }

    return reply;
  }
  
  public AttachLoadBalancerToSubnetsResponseType attachLoadBalancerToSubnets(
      final AttachLoadBalancerToSubnetsType request
  ) throws EucalyptusCloudException {
    final AttachLoadBalancerToSubnetsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName( );
    final Collection<String> requestedSubnetIds = request.getSubnets( ).getMember( );

    final String vpcId;
    final BiMap<String,String> zoneToSubnetIdMap = HashBiMap.create( );
    final LoadBalancerCoreView lb;
    try {
      final LoadBalancer lbEntity = LoadBalancers.getLoadbalancer( ctx, lbName );
      if ( !LoadBalancingMetadatas.filterPrivileged().apply( lbEntity ) ) { // IAM policy restriction
        throw new AccessPointNotFoundException();
      }
      lb = lbEntity.getCoreView();
      vpcId = lb.getVpcId();
      CollectionUtils.putAll( lbEntity.getZones(), zoneToSubnetIdMap, name(), subnetId() );
    } catch ( final LoadBalancingException e ) {
      throw e;
    } catch ( final Exception ex ){
      throw new AccessPointNotFoundException( );
    }

    if ( vpcId == null ) {
      throw new InvalidConfigurationRequestException( "Invalid subnet for load balancer" );
    }

    final List<SubnetType> subnets =
        EucalyptusActivityTasks.getInstance( ).describeSubnets( requestedSubnetIds );
    if ( subnets.size( ) != requestedSubnetIds.size( ) ) {
      throw new LoadBalancingBackendClientException( "SubnetNotFound", "Invalid subnet(s)" );
    }
    for ( final SubnetType subnetType : subnets ) {
      if ( !vpcId.equals( subnetType.getVpcId( ) ) ) {
        throw new InvalidConfigurationRequestException( "Invalid subnet for load balancer" );
      }
      final String previousSubnetId = zoneToSubnetIdMap.put( subnetType.getAvailabilityZone( ), subnetType.getSubnetId( ) );
      if ( previousSubnetId != null && !previousSubnetId.equals( subnetType.getSubnetId( ) ) ) {
        throw new InvalidConfigurationRequestException( "Multiple subnets for zone ("+subnetType.getAvailabilityZone( )+")" );
      }
    }

    final List<String> requestedZones = Lists.newArrayList(  );
    Iterables.addAll( requestedZones, Iterables.transform( requestedSubnetIds, Functions.forMap( zoneToSubnetIdMap.inverse( ) ) ) );
    try{
      final EnabledZoneEvent evt = new EnabledZoneEvent( );
      evt.setLoadBalancer( lbName );
      evt.setContext( ctx );
      evt.setZones( requestedZones );
      evt.setZoneToSubnetIdMap( zoneToSubnetIdMap );
      ActivityManager.getInstance( ).fire( evt );
    }catch(EventFailedException e){
      LOG.error("failed to handle EnabledZone event", e);
      final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to enable zones: %s", reason),e );
    }

    final Subnets replySubnets = new Subnets( );
    replySubnets.getMember().addAll( zoneToSubnetIdMap.values( ) );
    reply.getAttachLoadBalancerToSubnetsResult( ).setSubnets( replySubnets );
    return reply;
  }
  
  public DetachLoadBalancerFromSubnetsResponseType detachLoadBalancerFromSubnets(
      final DetachLoadBalancerFromSubnetsType request
  ) throws EucalyptusCloudException {
    final DetachLoadBalancerFromSubnetsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String lbName = request.getLoadBalancerName();
    final Collection<String> requestedSubnetIds = request.getSubnets( ).getMember( );

    final BiMap<String,String> zoneToSubnetIdMap = HashBiMap.create( );
    final String vpcId;
    final LoadBalancerCoreView lb;
    try {
      final LoadBalancer lbEntity = LoadBalancers.getLoadbalancer( ctx, lbName );
      if ( !LoadBalancingMetadatas.filterPrivileged( ).apply( lbEntity ) ) { // IAM policy restriction
        throw new AccessPointNotFoundException( );
      }
      lb = lbEntity.getCoreView( );
      vpcId = lb.getVpcId( );
      CollectionUtils.putAll( lbEntity.getZones(), zoneToSubnetIdMap, name(), subnetId() );
    } catch ( final LoadBalancingException e ) {
      throw e;
    } catch ( final Exception ex ){
      throw new AccessPointNotFoundException( );
    }

    if ( vpcId == null ) {
      throw new InvalidConfigurationRequestException( "Invalid subnet for load balancer" );
    }

    final List<String> zones = Lists.newArrayList(  );
    Iterables.addAll( zones, Iterables.transform( requestedSubnetIds, Functions.forMap( zoneToSubnetIdMap.inverse( ) ) ) );
    if( !zones.isEmpty( ) ){
      try{
        final DisabledZoneEvent evt = new DisabledZoneEvent( );
        evt.setLoadBalancer( lbName );
        evt.setContext( ctx );
        evt.setZones( zones );
        ActivityManager.getInstance( ).fire( evt );
      }catch(EventFailedException e){
        LOG.error("failed to handle DisabledZone event", e);
        final String reason = e.getCause()!=null && e.getCause().getMessage()!=null ? e.getCause().getMessage() : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to disable zones: %s", reason), e );
      }
    }

    List<String> availableZones = Lists.newArrayList();
    try{
      final LoadBalancer updatedLb = LoadBalancers.getLoadbalancer(ctx, lbName);
      availableZones = Lists.transform(LoadBalancers.findZonesInService(updatedLb), new Function<LoadBalancerZoneCoreView, String>(){
        @Override
        public String apply(@Nullable LoadBalancerZoneCoreView arg0) {
          return arg0.getName();
        }
      });
    }catch(Exception ex){
    }

    final Subnets replySubnets = new Subnets( );
    replySubnets.getMember().addAll( zoneToSubnetIdMap.values( ) );
    reply.getDetachLoadBalancerFromSubnetsResult( ).setSubnets( replySubnets );
    return reply;
  }

  public AddTagsResponseType addTags( final AddTagsType request ) throws EucalyptusCloudException {
    final AddTagsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount( ).getAccountNumber( );

    final Set<String> requestedNames = Sets.newHashSet( );
    if ( request.getLoadBalancerNames( ) != null ) {
      requestedNames.addAll( request.getLoadBalancerNames( ).getMember( ) );
    }
    if ( requestedNames.size( ) > 1 ) {
      throw new LoadBalancingBackendClientException( "InvalidParameterValue", "Too many load balancers" );
    }

    final Map<String,String> tags = Maps.newHashMap( );
    if ( request.getTags( ) != null ) {
      for ( final Tag tag : request.getTags( ).getMember( ) ) {
        if ( tags.put( tag.getKey( ), Strings.nullToEmpty( tag.getValue( ) ) ) != null ) {
          throw new LoadBalancingBackendClientException( "DuplicateTagKeys", "Duplicate tag key (" + tag.getKey( ) + ")" );
        }
      }
      final int reservedTags = Iterables.size( Iterables.filter( tags.keySet( ), isReservedTagPrefix() ) );
      if ( reservedTags > 0 && !Contexts.lookup( ).isPrivileged( ) ) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
    }

    final Function<String, Void> updateTags = new Function<String, Void>( ) {
      @Override
      public Void apply( final String identifier ) {
        try {
          final LoadBalancer example = LoadBalancer.namedByAccountId( accountNumber, identifier );
          final LoadBalancer loadBalancer = Entities.uniqueResult( example );
          if ( RestrictedTypes.filterPrivileged( ).apply( loadBalancer ) ) {
            final Map<String,String> lbTags = loadBalancer.getTags( );
            lbTags.putAll( tags );
            if ( Iterables.size( Iterables.filter( lbTags.keySet( ), Predicates.not( isReservedTagPrefix( ) ) ) ) > MAX_TAGS ) {
              throw Exceptions.toUndeclared( new LoadBalancingBackendClientException( "TooManyTags", "Tag limit exceeded" ) );
            }
            return null;
          } else {
            throw new NoSuchElementException( );
          }
        } catch ( NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new AccessPointNotFoundException( ) );
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    try {
      Entities.asTransaction( LoadBalancer.class, updateTags ).apply( Iterables.getOnlyElement( requestedNames ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public DescribeTagsResponseType describeTags(
      final DescribeTagsType request
  ) throws EucalyptusCloudException {
    final DescribeTagsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount( ).getAccountNumber( );
    final Set<String> requestedNames = Sets.newHashSet( );
    if ( request.getLoadBalancerNames( ) != null ) {
      requestedNames.addAll( request.getLoadBalancerNames().getMember() );
    }
    final boolean showAll = requestedNames.remove( "verbose" ) && ctx.isAdministrator();
    final Function<Set<String>, TagDescriptions> lookupAccountLBs = new Function<Set<String>, TagDescriptions>( ) {
      @Override
      public TagDescriptions apply( final Set<String> identifiers ) {
        try {
          final Predicate<? super LoadBalancer> requestedAndAccessible =
              LoadBalancingMetadatas.filteringFor( LoadBalancer.class )
                  .byId( identifiers )
                  .byPrivileges( )
                  .buildPredicate( );

          final LoadBalancer example = showAll ?
              LoadBalancer.named( null, null ) :
              LoadBalancer.namedByAccountId( accountNumber, null );
          final List<LoadBalancer> lbs = Entities.query( example, true );
          final TagDescriptions tagDescriptions = new TagDescriptions( );
          for ( final LoadBalancer loadBalancer : Iterables.filter( lbs, requestedAndAccessible ) ) {
            final TagDescription tagDescription = new TagDescription( );
            tagDescription.setLoadBalancerName( loadBalancer.getName( ) );
            final TagList tagList = new TagList( );
            tagDescription.setTags( tagList );
            for ( final Map.Entry<String,String> tagKeyAndValue : loadBalancer.getTags( ).entrySet( ) ) {
              final Tag tag = new Tag();
              tag.setKey( tagKeyAndValue.getKey( ) );
              tag.setValue( Strings.emptyToNull( tagKeyAndValue.getValue( ) ) );
              tagList.getMember().add( tag );
            }
            tagDescriptions.getMember().add( tagDescription );
          }
          return tagDescriptions;
        } catch ( EntityNotFoundException e ) {
          Entities.evictCache( LoadBalancer.class );
          throw new OptimisticLockException( "Error loading load balancers", e );
        }
      }
    };

    final TagDescriptions tagDescriptions =
        Entities.asTransaction( LoadBalancer.class, lookupAccountLBs ).apply( requestedNames );

    reply.getDescribeTagsResult( ).setTagDescriptions( tagDescriptions );

    return reply;
  }

  public RemoveTagsResponseType removeTags( final RemoveTagsType request ) throws EucalyptusCloudException {
    final RemoveTagsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount( ).getAccountNumber( );

    final Set<String> requestedNames = Sets.newHashSet( );
    if ( request.getLoadBalancerNames( ) != null ) {
      requestedNames.addAll( request.getLoadBalancerNames( ).getMember( ) );
    }
    if ( requestedNames.size( ) > 1 ) {
      throw new LoadBalancingBackendClientException( "InvalidParameterValue", "Too many load balancers" );
    }

    final Set<String> tags = Sets.newHashSet( );
    if ( request.getTags( ) != null ) {
      for ( final TagKeyOnly tag : request.getTags( ).getMember( ) ) {
        tags.add( tag.getKey( ) );
      }
      final int reservedTags = Iterables.size( Iterables.filter( tags, isReservedTagPrefix( ) ) );
      if ( reservedTags > 0 && !Contexts.lookup( ).isPrivileged( ) ) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
    }

    final Function<String, Void> removeTags = new Function<String, Void>( ) {
      @Override
      public Void apply( final String identifier ) {
        try {
          final LoadBalancer example = LoadBalancer.namedByAccountId( accountNumber, identifier );
          final LoadBalancer loadBalancer = Entities.uniqueResult( example );
          if ( RestrictedTypes.filterPrivileged( ).apply( loadBalancer ) ) {
            final Map<String,String> lbTags = loadBalancer.getTags( );
            lbTags.keySet( ).removeAll( tags );
            return null;
          } else {
            throw new NoSuchElementException( );
          }
        } catch ( NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new AccessPointNotFoundException( ) );
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    try {
      Entities.asTransaction( LoadBalancer.class, removeTags ).apply( Iterables.getOnlyElement( requestedNames ) );
    } catch ( Exception e ) {
      throw handleException( e );
    }

    return reply;
  }

  public ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(
      final ModifyLoadBalancerAttributesType request
  ) throws EucalyptusCloudException {
    final ModifyLoadBalancerAttributesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount( ).getAccountNumber( );

    final Function<String, LoadBalancerAttributes> modifyAttributes = new Function<String, LoadBalancerAttributes>( ) {
      @Override
      public LoadBalancerAttributes apply( final String identifier ) {
        try {
          final LoadBalancer example = LoadBalancer.namedByAccountId( accountNumber, identifier );
          final LoadBalancer loadBalancer = Entities.uniqueResult( example );
          if ( RestrictedTypes.filterPrivileged( ).apply( loadBalancer ) ) {
            final ConnectionSettings connectionSettings = request.getLoadBalancerAttributes( ).getConnectionSettings( );
            if ( connectionSettings != null ) {
              loadBalancer.setConnectionIdleTimeout( connectionSettings.getIdleTimeout( ) );
            }
            return TypeMappers.transform( loadBalancer, LoadBalancerAttributes.class );
          } else {
            throw new NoSuchElementException( );
          }
        } catch ( NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new AccessPointNotFoundException( ) );
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    final LoadBalancerAttributes attributes =
        Entities.asTransaction( LoadBalancer.class, modifyAttributes ).apply( request.getLoadBalancerName( ) );

    reply.getModifyLoadBalancerAttributesResult().setLoadBalancerAttributes( attributes );

    return reply;
  }

  public DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(
      final DescribeLoadBalancerAttributesType request
  ) throws EucalyptusCloudException {
    final DescribeLoadBalancerAttributesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String accountNumber = ctx.getAccount( ).getAccountNumber( );
    final Function<String, LoadBalancerAttributes> lookupAttributes = new Function<String, LoadBalancerAttributes>( ) {
      @Override
      public LoadBalancerAttributes apply( final String identifier ) {
        try {
          final LoadBalancer example = LoadBalancer.namedByAccountId( accountNumber, identifier );
          final LoadBalancer loadBalancer = Entities.uniqueResult( example );
          if ( RestrictedTypes.filterPrivileged( ).apply( loadBalancer ) ) {
            return TypeMappers.transform( loadBalancer, LoadBalancerAttributes.class );
          } else {
            throw new NoSuchElementException( );
          }
        } catch ( NoSuchElementException e ) {
          throw Exceptions.toUndeclared( new AccessPointNotFoundException( ) );
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };

    final LoadBalancerAttributes attributes =
        Entities.asTransaction( LoadBalancer.class, lookupAttributes ).apply( request.getLoadBalancerName( ) );

    reply.getDescribeLoadBalancerAttributesResult( ).setLoadBalancerAttributes( attributes );

    return reply;
  }

  private static Predicate<String> isReservedTagPrefix( ) {
    final Collection<Predicate<String>> predicates = Lists.newArrayList( );
    for ( final String prefix : reservedPrefixes ) {
      predicates.add(  com.eucalyptus.util.Strings.startsWith( prefix ) );
    }
    return Predicates.or( predicates );
  }

  private static String lookupAdmin( final Context ctx ) throws LoadBalancingException {
    try {
      return ctx.getAccount( ).lookupAdmin( ).getUserId( );
    } catch ( final AuthException e ) {
      throw new LoadBalancingException( "Error accessing account information" );
    }
  }

  private static LoadBalancingException handleException( final Exception e ) throws LoadBalancingException {
    final LoadBalancingException cause = Exceptions.findCause( e, LoadBalancingException.class );
    if ( cause != null ) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new TooManyAccessPointsException();
    }
    
    final AuthException authCause = Exceptions.findCause(e, AuthException.class);
    if(authCause != null) {
    	throw new InternalFailure400Exception(authCause.getMessage());
    }

    LOG.error( e, e );

    final InternalFailureException exception = new InternalFailureException( String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}


