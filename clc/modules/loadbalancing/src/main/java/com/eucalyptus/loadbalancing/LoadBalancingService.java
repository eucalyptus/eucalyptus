package com.eucalyptus.loadbalancing;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.ApplySecurityGroupsToLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.ApplySecurityGroupsToLoadBalancerType;
import com.eucalyptus.loadbalancing.AttachLoadBalancerToSubnetsResponseType;
import com.eucalyptus.loadbalancing.AttachLoadBalancerToSubnetsType;
import com.eucalyptus.loadbalancing.ConfigureHealthCheckResponseType;
import com.eucalyptus.loadbalancing.ConfigureHealthCheckType;
import com.eucalyptus.loadbalancing.CreateAppCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.CreateAppCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.CreateLBCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.CreateLBCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.CreateLoadBalancerType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerType;
import com.eucalyptus.loadbalancing.DescribeInstanceHealthResponseType;
import com.eucalyptus.loadbalancing.DescribeInstanceHealthType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancerPoliciesResponseType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancerPoliciesType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancerPolicyTypesResponseType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancerPolicyTypesType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.DetachLoadBalancerFromSubnetsResponseType;
import com.eucalyptus.loadbalancing.DetachLoadBalancerFromSubnetsType;
import com.eucalyptus.loadbalancing.DisableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.DisableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.EnableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.EnableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.loadbalancing.SetLoadBalancerListenerSSLCertificateResponseType;
import com.eucalyptus.loadbalancing.SetLoadBalancerListenerSSLCertificateType;
import com.eucalyptus.loadbalancing.SetLoadBalancerPoliciesForBackendServerResponseType;
import com.eucalyptus.loadbalancing.SetLoadBalancerPoliciesForBackendServerType;
import com.eucalyptus.loadbalancing.SetLoadBalancerPoliciesOfListenerResponseType;
import com.eucalyptus.loadbalancing.SetLoadBalancerPoliciesOfListenerType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class LoadBalancingService {
  private static Logger    LOG     = Logger.getLogger( LoadBalancingService.class );
  public DescribeLoadBalancerPolicyTypesResponseType describeLoadBalancerPolicyTypes(DescribeLoadBalancerPolicyTypesType request) throws EucalyptusCloudException {
    DescribeLoadBalancerPolicyTypesResponseType reply = request.getReply( );
    return reply;
  }

  public ConfigureHealthCheckResponseType configureHealthCheck(ConfigureHealthCheckType request) throws EucalyptusCloudException {
    ConfigureHealthCheckResponseType reply = request.getReply( );
    return reply;
  }

  public DetachLoadBalancerFromSubnetsResponseType detachLoadBalancerFromSubnets(DetachLoadBalancerFromSubnetsType request) throws EucalyptusCloudException {
    DetachLoadBalancerFromSubnetsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeLoadBalancerPoliciesResponseType describeLoadBalancerPolicies(DescribeLoadBalancerPoliciesType request) throws EucalyptusCloudException {
    DescribeLoadBalancerPoliciesResponseType reply = request.getReply( );
    return reply;
  }

  public SetLoadBalancerPoliciesOfListenerResponseType setLoadBalancerPoliciesOfListener(SetLoadBalancerPoliciesOfListenerType request) throws EucalyptusCloudException {
    SetLoadBalancerPoliciesOfListenerResponseType reply = request.getReply( );
    return reply;
  }

  public DisableAvailabilityZonesForLoadBalancerResponseType disableAvailabilityZonesForLoadBalancer(DisableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
    DisableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeInstanceHealthResponseType describeInstanceHealth(DescribeInstanceHealthType request) throws EucalyptusCloudException {
    DescribeInstanceHealthResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteLoadBalancerPolicyResponseType deleteLoadBalancerPolicy(DeleteLoadBalancerPolicyType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerPolicyResponseType reply = request.getReply( );
 
    return reply;
  }

  public CreateLoadBalancerPolicyResponseType createLoadBalancerPolicy(CreateLoadBalancerPolicyType request) throws EucalyptusCloudException {
    CreateLoadBalancerPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public EnableAvailabilityZonesForLoadBalancerResponseType enableAvailabilityZonesForLoadBalancer(EnableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
    EnableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }

  public CreateLoadBalancerListenersResponseType createLoadBalancerListeners(CreateLoadBalancerListenersType request) throws EucalyptusCloudException {
    CreateLoadBalancerListenersResponseType reply = request.getReply( );
    return reply;
  }

  public CreateLoadBalancerResponseType createLoadBalancer(CreateLoadBalancerType request) throws EucalyptusCloudException {
    final CreateLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName();
    final LoadBalancer lb = LoadBalancers.addLoadbalancer(ownerFullName, lbName);
    if(lb == null)
    	throw new LoadBalancingException(String.format("Requested loadbalancer %s cannot be created", lbName));
    /// TODO: SPARK we should add zones, listeners, etc., if present in the request
    final CreateLoadBalancerResult result = new CreateLoadBalancerResult();
    if(lb!=null && lb.getDnsAddress()==null){
    	LOG.error("No DNS name is assigned to a loadblancer");
    }
    result.setDnsName(lb != null ? lb.getDnsAddress() : null);
    reply.setCreateLoadBalancerResult(result);
    reply.set_return(true);
    return reply;
  }

  public DeleteLoadBalancerResponseType deleteLoadBalancer(DeleteLoadBalancerType request) throws EucalyptusCloudException {
    DeleteLoadBalancerResponseType reply = request.getReply( );
    final String lbToDelete = request.getLoadBalancerName();
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    
    if(lbToDelete!=null){
    	try{
    		LoadBalancers.deleteLoadbalancer(ownerFullName, lbToDelete);
    	}catch(Exception e){
    		// success if the lb is not found in the system
    		if(!(e.getCause() instanceof NoSuchElementException)) {
    			LOG.error("Error deleting the loadbalancer: "+e.getMessage(), e);
    			throw new LoadBalancingException("Failed to delete the loadbalancer "+lbToDelete, e);
    		}
    	}
    }
    DeleteLoadBalancerResult result = new DeleteLoadBalancerResult();
	reply.setDeleteLoadBalancerResult(result);    
    reply.set_return(true);
    return reply;
  }

  public SetLoadBalancerPoliciesForBackendServerResponseType setLoadBalancerPoliciesForBackendServer(SetLoadBalancerPoliciesForBackendServerType request) throws EucalyptusCloudException {
    SetLoadBalancerPoliciesForBackendServerResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteLoadBalancerListenersResponseType deleteLoadBalancerListeners(DeleteLoadBalancerListenersType request) throws EucalyptusCloudException {
    DeleteLoadBalancerListenersResponseType reply = request.getReply( );
    return reply;
  }

  public DeregisterInstancesFromLoadBalancerResponseType deregisterInstancesFromLoadBalancer(DeregisterInstancesFromLoadBalancerType request) throws EucalyptusCloudException {
    DeregisterInstancesFromLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }

  public SetLoadBalancerListenerSSLCertificateResponseType setLoadBalancerListenerSSLCertificate(SetLoadBalancerListenerSSLCertificateType request) throws EucalyptusCloudException {
    SetLoadBalancerListenerSSLCertificateResponseType reply = request.getReply( );
    return reply;
  }

  public CreateLBCookieStickinessPolicyResponseType createLBCookieStickinessPolicy(CreateLBCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateLBCookieStickinessPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public AttachLoadBalancerToSubnetsResponseType attachLoadBalancerToSubnets(AttachLoadBalancerToSubnetsType request) throws EucalyptusCloudException {
    AttachLoadBalancerToSubnetsResponseType reply = request.getReply( );
    return reply;
  }

  public CreateAppCookieStickinessPolicyResponseType createAppCookieStickinessPolicy(CreateAppCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateAppCookieStickinessPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public RegisterInstancesWithLoadBalancerResponseType registerInstancesWithLoadBalancer(RegisterInstancesWithLoadBalancerType request) throws EucalyptusCloudException {
    RegisterInstancesWithLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }

  public ApplySecurityGroupsToLoadBalancerResponseType applySecurityGroupsToLoadBalancer(ApplySecurityGroupsToLoadBalancerType request) throws EucalyptusCloudException {
    ApplySecurityGroupsToLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeLoadBalancersResponseType describeLoadBalancers(DescribeLoadBalancersType request) throws EucalyptusCloudException {
    DescribeLoadBalancersResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final AccountFullName ownerFullName = ctx.getUserFullName( ).asAccountFullName( );
    final Set<String> requestedNames = Sets.newHashSet( );
    if ( !request.getLoadBalancerNames().getMember().isEmpty()) {
    	requestedNames.addAll( request.getLoadBalancerNames().getMember() );
    }
    
   final Function<Set<String>, Set<String>> lookupLBNames = new Function<Set<String>, Set<String>>( ) {
        public Set<String> apply( final Set<String> input ) {
    	  final Predicate<? super LoadBalancer> requestedAndAccessible = CloudMetadatas.filteringFor( LoadBalancer.class )
  	       .byId(input )
  	       .byPrivileges()
           .buildPredicate();
        	
    	  final List<LoadBalancer> lbs = Entities.query( LoadBalancer.named( ownerFullName, null ), true);
          Set<String> res = Sets.newHashSet( );
          for ( final LoadBalancer foundLB : Iterables.filter(lbs, requestedAndAccessible ))
            res.add( foundLB.getDisplayName( ) );
          return res;
        }
    }; 
    Set<String> allowedLBNames = Entities.asTransaction( LoadBalancer.class, lookupLBNames ).apply( requestedNames );
    
    final Function<Set<String>, Set<LoadBalancerDescription>> lookupLBDescriptions = new Function<Set<String>, Set<LoadBalancerDescription>> () {
    	public Set<LoadBalancerDescription> apply (final Set<String> input){
    		final Set<LoadBalancerDescription> descs = Sets.newHashSet();
    		for (String lbName : input){
    			LoadBalancerDescription desc = new LoadBalancerDescription();
    			desc.setLoadBalancerName(lbName);
    			descs.add(desc);
    		}
    		return descs;
    	}
    };
    Set<LoadBalancerDescription> descs = lookupLBDescriptions.apply(allowedLBNames);
    
    DescribeLoadBalancersResult descResult = new DescribeLoadBalancersResult();
    LoadBalancerDescriptions lbDescs = new LoadBalancerDescriptions();
    lbDescs.setMember(new ArrayList<LoadBalancerDescription>(descs));
    descResult.setLoadBalancerDescriptions(lbDescs);
    reply.setDescribeLoadBalancersResult(descResult);
    reply.set_return(true);
    return reply;
  }
}
