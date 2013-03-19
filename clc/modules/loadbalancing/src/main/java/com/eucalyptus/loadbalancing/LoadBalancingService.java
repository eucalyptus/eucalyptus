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
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.EventFailedException;
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
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.activities.CreateListenerEvent;
import com.eucalyptus.loadbalancing.activities.DeleteListenerEvent;
import com.eucalyptus.loadbalancing.activities.DeleteLoadbalancerEvent;
import com.eucalyptus.loadbalancing.activities.DeregisterInstancesEvent;
import com.eucalyptus.loadbalancing.activities.DisabledZoneEvent;
import com.eucalyptus.loadbalancing.activities.EnabledZoneEvent;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.NewLoadbalancerEvent;
import com.eucalyptus.loadbalancing.activities.ActivityManager;
import com.eucalyptus.loadbalancing.activities.RegisterInstancesEvent;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 */
public class LoadBalancingService {
  private static Logger    LOG     = Logger.getLogger( LoadBalancingService.class );
  
  /// EUCA-specific, internal operations for storing instance health check and cloudwatch metrics
  public PutServoStatesResponseType putServoStates(PutServoStatesType request){
	  PutServoStatesResponseType reply = request.getReply();
	  // TODO: SPARK: authenticate/authorize using IAM roles
	  final String servoId = request.getInstanceId();
	  final Instances instances = request.getInstances();
	  LoadBalancer lb = null;
	  if(servoId!= null){
		  try{
			  LoadBalancerServoInstance servo = LoadBalancers.lookupServoInstance(servoId);
			  lb = servo.getAvailabilityZone().getLoadbalancer();
			  if(lb==null)
				  throw new Exception("Failed to find the loadbalancer");
		  }catch(NoSuchElementException ex){
			  LOG.warn("unknown servo VM id is used to query: "+servoId);
		  }catch(Exception ex){
			  LOG.warn("failed to query servo instance");
		  }
	  }
	  final Collection<LoadBalancerBackendInstance> lbInstances = lb.getBackendInstances();
	  if(lb != null && instances.getMember()!=null && instances.getMember().size()>0){
		  for(Instance instance : instances.getMember()){
			  String instanceId = instance.getInstanceId();
			  // format: instanceId:state
			  String[] parts = instanceId.split(":");
			  if(parts==null || parts.length!= 2){
				  LOG.warn("instance id is in wrong format:"+ instanceId);
				  continue;
			  }
			  instanceId = parts[0];
			  String state = parts[1];
			  
			  LoadBalancerBackendInstance found = null;
			  for(final LoadBalancerBackendInstance lbInstance : lbInstances){
				  if(instanceId.equals(lbInstance.getInstanceId())){
					  found = lbInstance;
					  break;
				  }  
			  }
			  if(found!=null){
				  final EntityTransaction db = Entities.get( LoadBalancerBackendInstance.class );
			 	  try{
			 		  found = Entities.uniqueResult(found);
			 		  if (state.equals(LoadBalancerBackendInstance.STATE.InService.name()) || 
			 				  state.equals(LoadBalancerBackendInstance.STATE.OutOfService.name())){
			 			  found.setState(Enum.valueOf(LoadBalancerBackendInstance.STATE.class, state));
			 			  Entities.persist(found);
			 		  }
			 		  db.commit();
			 	  }catch(NoSuchElementException ex){
			 		  db.rollback();
			 	  }catch(Exception ex){
			 		  db.rollback();
			 		  LOG.warn("Failed to query loadbalancer backend instance: "+instanceId, ex);
			 	  }
			  }
		  }
	  }
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
    
    final LoadBalancerDnsRecord dns = LoadBalancers.getDnsRecord(lb);
    if(dns == null || dns.getName() == null)
    	throw new LoadBalancingException("New dns name could not be created");
    
    Function<String, Boolean> rollback = new Function<String, Boolean>(){
    	@Override
    	public Boolean apply(String lbName){
    		try{
    			LoadBalancers.deleteDnsRecord(dns);
    		}catch(LoadBalancingException ex){
    			LOG.error("failed to rollback the dns records", ex);
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
    
    Collection<String> zones = request.getAvailabilityZones().getMember();
    if(zones != null && zones.size()>0){
    	try{
    	LoadBalancers.addZone(lbName, ownerFullName, zones);
    	}catch(LoadBalancingException ex){
    		rollback.apply(lbName);
    		throw ex;
    	}catch(Exception ex){
    		rollback.apply(lbName);
    		throw new LoadBalancingException("failed to create the loadbalancer", ex);
    	}
    }
    
    /// trigger new loadbalancer event 
    try{
    	NewLoadbalancerEvent evt = new NewLoadbalancerEvent();
    	evt.setLoadBalancer(lbName);
    	evt.setContext(ctx);
    	evt.setZones(zones);
    	ActivityManager.getInstance().fire(evt);
    }catch(EventFailedException e){
    	//TODO SPARK: TEST
    	LOG.error("failed to fire new loadbalancer event", e);
    	rollback.apply(lbName);
    	throw new LoadBalancingException(String.format("Faild to create the requested loadblanacer: %s", lbName), e);
    }

    Collection<Listener> listeners=request.getListeners().getMember();
    if(listeners!=null && listeners.size()>0){
    	LoadBalancers.createLoadbalancerListener(lbName,  ownerFullName, listeners);
    	try{
    		CreateListenerEvent evt = new CreateListenerEvent();
    		evt.setLoadBalancer(lbName);
    		evt.setListeners(listeners);
    		evt.setContext(ctx);
    		ActivityManager.getInstance().fire(evt);
    	}catch(EventFailedException e){
    		LOG.error("failed to fire createListener event", e);
        	rollback.apply(lbName);
        	throw new LoadBalancingException(String.format("Faild to create the requested loadblanacer: %s", lbName), e);
    	}
    }
    
    final CreateLoadBalancerResult result = new CreateLoadBalancerResult();
    if(dns==null || dns.getDnsName() == null){
    	LOG.warn("No DNS name is assigned to a loadblancer "+lbName);
    }
    
    result.setDnsName(dns.getDnsName());
    reply.setCreateLoadBalancerResult(result);
    reply.set_return(true);
    return reply;
  }

  public DescribeLoadBalancersResponseType describeLoadBalancersServo(final String instanceId, final DescribeLoadBalancersResponseType reply) throws EucalyptusCloudException {
  	// lookup servo instance Id to see which LB zone it is assigned to
	  LoadBalancerZone zone = null;
	  try{
  		final LoadBalancerServoInstance instance = LoadBalancers.lookupServoInstance(instanceId);
  		zone = instance.getAvailabilityZone();
	  }catch(NoSuchElementException ex){
  		;
  	  }catch(Exception ex){
  		LOG.warn("failed to query loadbalancer for servo instance: "+instanceId);
  	  }			  
	  
  	  final Function<LoadBalancerZone, Set<LoadBalancerDescription>> lookupLBDescriptions = new Function<LoadBalancerZone, Set<LoadBalancerDescription>> () {
		  @Override
  		  public Set<LoadBalancerDescription> apply (LoadBalancerZone zone){
	    		final Set<LoadBalancerDescription> descs = Sets.newHashSet();
	    		final LoadBalancer lb = zone.getLoadbalancer();
	    		final String lbName = lb.getDisplayName();

	    		LoadBalancerDescription desc = new LoadBalancerDescription();
	    		desc.setLoadBalancerName(lbName); /// loadbalancer name
	    		desc.setCreatedTime(lb.getCreationTimestamp());/// createdtime
	    		final LoadBalancerDnsRecord dns = lb.getDns();
	    			
	    		desc.setDnsName(dns.getDnsName());           /// dns name
	    			                                 
	    		/// instances
	    		if(zone.getBackendInstances().size()>0){
	    			desc.setInstances(new Instances());
	    			desc.getInstances().setMember(new ArrayList<Instance>(
	    		    	Collections2.transform(zone.getBackendInstances(), new Function<LoadBalancerBackendInstance, Instance>(){
    		    			@Override
    		    			public Instance apply(final LoadBalancerBackendInstance be){
    		    				Instance instance = new Instance();
    		    				instance.setInstanceId(be.getInstanceId());
    		    				return instance;
    		    			}
    		    		})));
	    		}
	    			/// availability zones
	    		desc.setAvailabilityZones(new AvailabilityZones());
	    		desc.getAvailabilityZones().setMember(Lists.newArrayList(zone.getName()));
	    			                                  /// listeners
	    		if(lb.getListeners().size()>0){
	    			desc.setListenerDescriptions(new ListenerDescriptions());
	    			desc.getListenerDescriptions().setMember(new ArrayList<ListenerDescription>(
	    					Collections2.transform(lb.getListeners(), new Function<LoadBalancerListener, ListenerDescription>(){
    							@Override
    							public ListenerDescription apply(final LoadBalancerListener input){
    								ListenerDescription desc = new ListenerDescription();
    								Listener listener = new Listener();
    								listener.setLoadBalancerPort(input.getLoadbalancerPort());
    								listener.setInstancePort(input.getInstancePort());
    								if(input.getInstanceProtocol() != PROTOCOL.NONE)
    									listener.setInstanceProtocol(input.getInstanceProtocol().name());
    								listener.setProtocol(input.getProtocol().name());
    								if(input.getCertificateId()!=null)
    									listener.setSslCertificateId(input.getCertificateId());
    								desc.setListener(listener);
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
	    		}catch(IllegalStateException ex){
	    				;
	    		}catch(Exception ex){
	    				;
	    		}
    			descs.add(desc);
	    		return descs;
	    	}
  	  };

	  Set<LoadBalancerDescription> descs = null;
  	  if(zone != null){
  		  descs= lookupLBDescriptions.apply(zone);
  	  }else
  		  descs = Sets.<LoadBalancerDescription>newHashSet();
  		  
	  DescribeLoadBalancersResult descResult = new DescribeLoadBalancersResult();
	  LoadBalancerDescriptions lbDescs = new LoadBalancerDescriptions();
	  lbDescs.setMember(new ArrayList<LoadBalancerDescription>(descs));
	  descResult.setLoadBalancerDescriptions(lbDescs);
	  reply.setDescribeLoadBalancersResult(descResult);
	  reply.set_return(true);
	    
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
	    //"servo:%s" % servo_instance_id

	    Set<String> allowedLBNames = null;
	    String marker = request.getMarker();
	    // the case that servo instances want the listeners assigned to it
	    // TODO: SPARK: authenticate/authorize using IAM roles	
	    if (marker!= null && marker.startsWith("servo")){
	  	  String instanceId = marker.replace("servo:", "");
	    	return describeLoadBalancersServo(instanceId, reply); 	
	    }else{	// normal describe-load-balancers path
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
		    allowedLBNames = Entities.asTransaction( LoadBalancer.class, lookupLBNames ).apply( requestedNames );
	    }
	    final Function<String, LoadBalancer> getLoadBalancer = new Function<String, LoadBalancer>(){
	    	@Override
	    	public LoadBalancer apply(final String lbName){
	    		try{
	    			return Entities.uniqueResult(LoadBalancer.named(ownerFullName, lbName));
	    		}catch(NoSuchElementException ex){
	    			return null;
	    		}catch(Exception ex){
	    			LOG.warn("faied to retrieve the loadbalancer-"+lbName, ex);
	    			return null;
	    		}
	    	}
	    };
	    
	    final Function<Set<String>, Set<LoadBalancerDescription>> lookupLBDescriptions = new Function<Set<String>, Set<LoadBalancerDescription>> () {
	    	public Set<LoadBalancerDescription> apply (final Set<String> input){
	    		final Set<LoadBalancerDescription> descs = Sets.newHashSet();
	    		for (String lbName : input){
	    			LoadBalancerDescription desc = new LoadBalancerDescription();
	    			final LoadBalancer lb = Entities.asTransaction(LoadBalancer.class, getLoadBalancer).apply(lbName);
	    			if(lb==null) // loadbalancer not found
	    				continue;
	    			desc.setLoadBalancerName(lbName); /// loadbalancer name
	    			desc.setCreatedTime(lb.getCreationTimestamp());/// createdtime
	    			final LoadBalancerDnsRecord dns = lb.getDns();
	    			
	    			desc.setDnsName(dns.getDnsName());           /// dns name
	    			                                  /// instances
	    			if(lb.getBackendInstances().size()>0){
	    				desc.setInstances(new Instances());
	    				desc.getInstances().setMember(new ArrayList<Instance>(
	    		    		Collections2.transform(lb.getBackendInstances(), new Function<LoadBalancerBackendInstance, Instance>(){
	    		    			@Override
	    		    			public Instance apply(final LoadBalancerBackendInstance be){
	    		    				Instance instance = new Instance();
	    		    				instance.setInstanceId(be.getInstanceId());
	    		    				return instance;
	    		    			}
	    		    		})));
	    			}
	    			/// availability zones
	    			if(lb.getZones().size()>0){
	    				desc.setAvailabilityZones(new AvailabilityZones());
	    				desc.getAvailabilityZones().setMember(new ArrayList<String>(
	    						Collections2.transform(lb.getZones(), new Function<LoadBalancerZone, String>(){
	    							@Override
	    							public String apply(final LoadBalancerZone zone){
	    								return zone.getName();
	    							}
	    						})));
	    			}
	    			                                  /// listeners
	    			if(lb.getListeners().size()>0){
	    				desc.setListenerDescriptions(new ListenerDescriptions());
	    				desc.getListenerDescriptions().setMember(new ArrayList<ListenerDescription>(
	    						Collections2.transform(lb.getListeners(), new Function<LoadBalancerListener, ListenerDescription>(){
	    							@Override
	    							public ListenerDescription apply(final LoadBalancerListener input){
	    								ListenerDescription desc = new ListenerDescription();
	    								Listener listener = new Listener();
	    								listener.setLoadBalancerPort(input.getLoadbalancerPort());
	    								listener.setInstancePort(input.getInstancePort());
	    								if(input.getInstanceProtocol() != PROTOCOL.NONE)
	    									listener.setInstanceProtocol(input.getInstanceProtocol().name());
	    								listener.setProtocol(input.getProtocol().name());
	    								if(input.getCertificateId()!=null)
	    									listener.setSslCertificateId(input.getCertificateId());
	    								desc.setListener(listener);
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
	    			}catch(IllegalStateException ex){
    				  ;
	    			}catch(Exception ex){
    				  ;
	    			}
	    			                                  /// (backend server description)
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
  
  public DeleteLoadBalancerResponseType deleteLoadBalancer(DeleteLoadBalancerType request) throws EucalyptusCloudException {
	    DeleteLoadBalancerResponseType reply = request.getReply( );
	    final String lbToDelete = request.getLoadBalancerName();
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	 	try{
		 	    
		    if(lbToDelete!=null){
		    	LoadBalancer lb = null;
		    	try{
		    		lb = LoadBalancers.getLoadbalancer(ownerFullName, lbToDelete);
		    	}catch(NoSuchElementException ex){
		    		throw new Exception("loadbalancer not found", ex);
		    	}
		    	Collection<LoadBalancerListener> listeners = lb.getListeners();
		    	final List<Integer> ports = Lists.newArrayList(Collections2.transform(listeners, new Function<LoadBalancerListener, Integer>(){
					@Override
					public Integer apply(@Nullable LoadBalancerListener arg0) {
						return arg0.getLoadbalancerPort();
					}
		    	}));
		    	
	    		try{
	    			DeleteListenerEvent evt = new DeleteListenerEvent();
	    	    	evt.setLoadBalancer(lbToDelete);
	    	    	evt.setContext(ctx);
	    	    	evt.setPorts(ports);
	    	    	ActivityManager.getInstance().fire(evt);
	    	    }catch(EventFailedException e){
	    	    	LOG.error("failed to fire DeleteListener event", e);
	    	    }
		    
	    		try{
	        		DeleteLoadbalancerEvent evt = new DeleteLoadbalancerEvent();
	        		evt.setLoadBalancer(lbToDelete);
	        		evt.setContext(ctx);
	        		ActivityManager.getInstance().fire(evt);
	        	}catch(EventFailedException e){
	        		LOG.error("failed to fire DeleteLoadbalancer event", e);
	        		throw e;
	        	}
	    		LoadBalancers.deleteLoadbalancer(ownerFullName, lbToDelete);
		    }
	    }catch(Exception e){
	    		// success if the lb is not found in the system
	    		if(!(e.getCause() instanceof NoSuchElementException)) {
	    			LOG.error("Error deleting the loadbalancer: "+e.getMessage(), e);
	    			throw new LoadBalancingException("Failed to delete the loadbalancer "+lbToDelete, e);
	    		}
	    }
	    DeleteLoadBalancerResult result = new DeleteLoadBalancerResult();
		reply.setDeleteLoadBalancerResult(result);    
	    reply.set_return(true);
	    return reply;
  }
  
  public CreateLoadBalancerListenersResponseType createLoadBalancerListeners(CreateLoadBalancerListenersType request) throws EucalyptusCloudException {
	    final CreateLoadBalancerListenersResponseType reply = request.getReply( );
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	    final String lbName = request.getLoadBalancerName();
	    final Collection<Listener> listeners = request.getListeners().getMember();
	    try{
    		CreateListenerEvent evt = new CreateListenerEvent();
    		evt.setLoadBalancer(lbName);
    		evt.setContext(ctx);
    		evt.setListeners(listeners);
    		ActivityManager.getInstance().fire(evt);
    	}catch(EventFailedException e){
    		LOG.error("failed to fire CreateListener event", e);
    		throw new LoadBalancingException("failed to create listener: internal error",e );
    	}
	    LoadBalancers.createLoadbalancerListener(lbName,  ownerFullName, listeners);
	    reply.set_return(true);
	    return reply;
  }
  
  public DeleteLoadBalancerListenersResponseType deleteLoadBalancerListeners(DeleteLoadBalancerListenersType request) throws EucalyptusCloudException {
	    final DeleteLoadBalancerListenersResponseType reply = request.getReply( );
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	  
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
	    	throw new LoadBalancingException("invalid port number", ex);
	    }
	    
	    final LoadBalancer lb;
	    try{
	  		 lb = LoadBalancers.getLoadbalancer(ownerFullName, lbName);
	    }catch(NoSuchElementException ex){
	    	throw new AccessPointNotFoundException();
	    }
	    catch(Exception ex){
	    	LOG.error("Failed to find the loadbalancer="+lbName);
	    	throw new LoadBalancingException("failed to retrieve the loadbalancer", ex);
	    }
		  	 
	   final Function<Void, Collection<Integer>> filter = new Function<Void, Collection<Integer>>(){
	    	@Override
	    	public Collection<Integer> apply(Void v){
	    		 final Collection<Integer> filtered = Sets.newHashSet();
		   	  	 for(Integer port : listenerPorts){
		   	  		 final LoadBalancerListener found = lb.findListener(port.intValue());
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
	    				final LoadBalancerListener exist = Entities.uniqueResult(LoadBalancerListener.named(lb, port.intValue()));
	    				Entities.delete(exist);
	    			}catch(NoSuchElementException ex){
	        			;
	        		}catch(Exception ex){
	        			LOG.error("Failed to delete the listener", ex);
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
    		LOG.error("failed to fire DeleteListener event", e);
    		throw new LoadBalancingException("failed to delete listener: internal error",e );
    	}
	    
	    reply.set_return(Entities.asTransaction(LoadBalancerListener.class, remover).apply(toDelete));
	    //reply.set_return(true);
	    return reply;
  }
  
  public RegisterInstancesWithLoadBalancerResponseType registerInstancesWithLoadBalancer(RegisterInstancesWithLoadBalancerType request) throws EucalyptusCloudException {
	    RegisterInstancesWithLoadBalancerResponseType reply = request.getReply( );
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	  
	    final String lbName = request.getLoadBalancerName();
	    final Collection<Instance> instances = request.getInstances().getMember();
	    final Predicate<Void> creator = new Predicate<Void>(){
	        @Override
	        public boolean apply( Void v ) {
	        	LoadBalancer lb = null;
	        	try{
	        		lb= LoadBalancers.getLoadbalancer(ownerFullName, lbName);
	        	}catch(Exception ex){
	    	    	LOG.warn("No loadbalancer is found with name="+lbName);    
	    	    	return false;
	    	    }
	        	for(Instance vm : instances){
	        		if(lb.hasBackendInstance(vm.getInstanceId()))
	        			continue;	// the vm instance is already registered
	        		final LoadBalancerBackendInstance beInstance = 
	        				LoadBalancerBackendInstance.newInstance(ownerFullName, lb, vm.getInstanceId());
	        		beInstance.setState(LoadBalancerBackendInstance.STATE.OutOfService);
	        		Entities.persist(beInstance);
	        	}
	        	return true;
	        }
	    };
	    final Function<Void, ArrayList<Instance>> finder = new Function<Void, ArrayList<Instance>>(){
	    	@Override
	    	public ArrayList<Instance> apply(Void v){
	    	  	LoadBalancer lb = null;
	    	  	try{
	    	  		lb=LoadBalancers.getLoadbalancer(ownerFullName, lbName);
	    	  	}catch(Exception ex){
	    	    	LOG.warn("No loadbalancer is found with name="+lbName);    
	    	    	return Lists.newArrayList();
	    	    }
	   	  	 	Entities.refresh(lb);
	    	    ArrayList<Instance> result = new ArrayList<Instance>(Collections2.transform(lb.getBackendInstances(), new Function<LoadBalancerBackendInstance, Instance>(){
		    		@Override
		    		public Instance apply(final LoadBalancerBackendInstance input){
		    			final Instance newInst = new Instance();
		    			newInst.setInstanceId(input.getInstanceId());
		    			return newInst;
		    		}}));
	    	    return result;
	    	}
	    };
	    
	    try{
    		RegisterInstancesEvent evt = new RegisterInstancesEvent();
    		evt.setLoadBalancer(lbName);
    		evt.setContext(ctx);
    		evt.setInstances(instances);
    		ActivityManager.getInstance().fire(evt);
    	}catch(EventFailedException e){
    		LOG.error("failed to fire RegisterInstances event", e);
    		throw new LoadBalancingException("failed to register instances: internal error",e );
    	}
	    
	    if(instances!=null){
	    	try{
	    		reply.set_return(Entities.asTransaction(LoadBalancerBackendInstance.class, creator).apply(null));
	    	}catch(Exception ex){
	    		throw new LoadBalancingException("Failed to register instances", ex);
	    	}
	    }
	    
	    RegisterInstancesWithLoadBalancerResult result = new RegisterInstancesWithLoadBalancerResult();
	    Instances returnInstances = new Instances();
	    returnInstances.setMember(Entities.asTransaction(LoadBalancer.class, finder).apply(null));
	    result.setInstances(returnInstances);
	    reply.setRegisterInstancesWithLoadBalancerResult(result);
	    return reply;
  }

  public DeregisterInstancesFromLoadBalancerResponseType deregisterInstancesFromLoadBalancer(DeregisterInstancesFromLoadBalancerType request) throws EucalyptusCloudException {
	    DeregisterInstancesFromLoadBalancerResponseType reply = request.getReply( );
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	  
	    final String lbName = request.getLoadBalancerName();
	    final Collection<Instance> instances = request.getInstances().getMember();
	    
	    final Function<Void, Collection<String>> filter = new Function<Void, Collection<String>>(){
	    	@Override
	    	public Collection<String> apply(Void v){
	    		 LoadBalancer lb = null;
		   	  	 try{
		   	  		 lb = LoadBalancers.getLoadbalancer(ownerFullName, lbName);
		   	  	 }catch(Exception ex){
	    	    	LOG.warn("No loadbalancer is found with name="+lbName);    
	    	    	return null;
	    	     }
		   	  	 Collection<String> filtered = Sets.newHashSet();
		   	  	 for(Instance inst : instances){
		   	  		 if(lb.hasBackendInstance(inst.getInstanceId()))
		   	  			 filtered.add(inst.getInstanceId());
		   	  	 }
		   	  	 return filtered;
	    	}
	    };
	    
	    final Collection<String> instancesToRemove = Entities.asTransaction(LoadBalancer.class, filter).apply(null);
	    if(instancesToRemove==null){
	    	reply.set_return(false);
	    	return reply;
	    }
	    final Predicate<Void> remover = new Predicate<Void>(){
	    	@Override
	    	public boolean apply(Void v){
	        	for(String instanceId : instancesToRemove){
	        	    LoadBalancerBackendInstance toDelete = null;
	        	    try{
	        	    	toDelete = Entities.uniqueResult(LoadBalancerBackendInstance.named(ownerFullName, instanceId));
	        	    }catch(NoSuchElementException ex){
	        	    	toDelete=null;
	        	    }catch(Exception ex){
	        	    	LOG.error("Can't query loadbalancer backend instance for "+instanceId);
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
	    	  	 LoadBalancer lb = null;
	    	  	 try{
	    	  		lb= LoadBalancers.getLoadbalancer(ownerFullName, lbName);
	    	  	 }catch(Exception ex){
	     	    	LOG.warn("No loadbalancer is found with name="+lbName);    
	     	    	return Lists.newArrayList();
	     	    }
	    	  	Entities.refresh(lb);
	    	    ArrayList<Instance> result = new ArrayList<Instance>(Collections2.transform(lb.getBackendInstances(), new Function<LoadBalancerBackendInstance, Instance>(){
		    		@Override
		    		public Instance apply(final LoadBalancerBackendInstance input){
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
    		LOG.error("failed to fire DeregisterInstances event", e);
    		throw new LoadBalancingException("failed to deregister instances: internal error",e );
    	}
	    
	    reply.set_return(Entities.asTransaction(LoadBalancerBackendInstance.class, remover).apply(null));
	    DeregisterInstancesFromLoadBalancerResult result = new DeregisterInstancesFromLoadBalancerResult();
	    Instances returnInstances = new Instances();
	    returnInstances.setMember(Entities.asTransaction(LoadBalancer.class, finder).apply(null));
	    result.setInstances(returnInstances);
	    reply.setDeregisterInstancesFromLoadBalancerResult(result);
	    return reply;
  }
  
  public EnableAvailabilityZonesForLoadBalancerResponseType enableAvailabilityZonesForLoadBalancer(EnableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
	    final EnableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
	    final Context ctx = Contexts.lookup( );
	    final UserFullName ownerFullName = ctx.getUserFullName( );
	    final String lbName = request.getLoadBalancerName();
	    final Collection<String> zones = request.getAvailabilityZones().getMember();
	    if(zones != null && zones.size()>0){
	    	try{
	    		EnabledZoneEvent evt = new EnabledZoneEvent();
	    		evt.setLoadBalancer(lbName);
	    		evt.setContext(ctx);
	    		evt.setZones(zones);
	    		ActivityManager.getInstance().fire(evt);
	    	}catch(EventFailedException e){
	    		LOG.error("failed to fire EnabledZone event", e);
	    		throw new LoadBalancingException("failed to enable zones: internal error",e );
	    	}
	    	
	    	LoadBalancers.addZone(lbName, ownerFullName, zones);
	    }
	    reply.set_return(true);
	    return reply;
  }

  public DisableAvailabilityZonesForLoadBalancerResponseType disableAvailabilityZonesForLoadBalancer(DisableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
	  final DisableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
	  final Context ctx = Contexts.lookup( );
	  final UserFullName ownerFullName = ctx.getUserFullName( );
	  final String lbName = request.getLoadBalancerName();
	  final Collection<String> zones = request.getAvailabilityZones().getMember();
	  if(zones != null && zones.size()>0){
		 try{
	    		DisabledZoneEvent evt = new DisabledZoneEvent();
	    		evt.setLoadBalancer(lbName);
	    		evt.setContext(ctx);
	    		evt.setZones(zones);
	    		ActivityManager.getInstance().fire(evt);
	    	}catch(EventFailedException e){
	    		LOG.error("failed to fire DisabledZone event", e);
	    		throw new LoadBalancingException("failed to disable zones: internal error",e );
	    }  
	  	LoadBalancers.removeZone(lbName, ownerFullName, zones);
	  }
	  reply.set_return(true);
	  return reply;
  }
  
  public ConfigureHealthCheckResponseType configureHealthCheck(ConfigureHealthCheckType request) throws EucalyptusCloudException {
    ConfigureHealthCheckResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
	final UserFullName ownerFullName = ctx.getUserFullName( );
	final String lbName = request.getLoadBalancerName();
	final HealthCheck hc = request.getHealthCheck();
	Integer healthyThreshold = hc.getHealthyThreshold();
	if (healthyThreshold == null)
		throw new LoadBalancingException("healthy tresholds must be specified");
	Integer interval = hc.getInterval();
	if(interval == null)
		throw new LoadBalancingException("interval must be specified");
	String target = hc.getTarget();
	if(target == null)
		throw new LoadBalancingException("target must be specified");
	
	Integer timeout = hc.getTimeout();
    if(timeout==null)
    	throw new LoadBalancingException("timeout must be specified");
    Integer unhealthyThreshold = hc.getUnhealthyThreshold();
    if(unhealthyThreshold == null)
    	throw new LoadBalancingException("unhealthy tresholds must be specified");
    LoadBalancer lb = null;
    try{
    	lb = LoadBalancers.getLoadbalancer(ownerFullName, lbName);
    }catch(NoSuchElementException ex){
    	throw new AccessPointNotFoundException();
    }catch(Exception ex){
    	throw new LoadBalancingException("failed to find the loadbalancer due to unknown reason");
    }

    final EntityTransaction db = Entities.get( LoadBalancer.class );
    try{
    	final LoadBalancer update = Entities.uniqueResult(lb);
    	update.setHealthCheck(healthyThreshold, interval, target, timeout, unhealthyThreshold);
		Entities.persist(update);
		db.commit();
    }catch(Exception ex){
    	db.rollback();
    	LOG.error("failed to persist health check config", ex);
    	throw new LoadBalancingException("failed to persist the health check request", ex);
    }
    ConfigureHealthCheckResult result = new ConfigureHealthCheckResult();
    result.setHealthCheck(hc);
    reply.setConfigureHealthCheckResult(result);
    return reply;
  }

  public DescribeInstanceHealthResponseType describeInstanceHealth(DescribeInstanceHealthType request) throws EucalyptusCloudException {
    DescribeInstanceHealthResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
 	final UserFullName ownerFullName = ctx.getUserFullName( );
 	final String lbName = request.getLoadBalancerName();
 	Instances instances = request.getInstances();
 	
	LoadBalancer lb = null;
	try{
		lb= LoadBalancers.getLoadbalancer(ownerFullName, lbName);
	}catch(NoSuchElementException ex){
		throw new AccessPointNotFoundException();
    }catch(Exception ex){
    	throw new LoadBalancingException("failed to query loadbalancer due to unknown reason");
    }
 	
	List<LoadBalancerBackendInstance> lbInstances = Lists.newArrayList(lb.getBackendInstances());
	List<LoadBalancerBackendInstance> instancesFound = null;
 	if(instances != null && instances.getMember()!= null && instances.getMember().size()>0){
 		instancesFound = Lists.newArrayList();
 		for(Instance inst : instances.getMember()){
 			String instId = inst.getInstanceId();
 			for(final LoadBalancerBackendInstance lbInstance : lbInstances){
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
 	for(final LoadBalancerBackendInstance instance : instancesFound){
 		InstanceState state = new InstanceState();
 		state.setInstanceId(instance.getDisplayName());
 		state.setState(instance.getState().name());
 		if(instance.getState().equals(LoadBalancerBackendInstance.STATE.OutOfService) && instance.getReasonCode()!=null)
 			state.setReasonCode(instance.getReasonCode());
 		stateList.add(state);
 	}
 	
 	final InstanceStates states = new InstanceStates();
 	states.setMember(stateList);
 	final DescribeInstanceHealthResult result = new DescribeInstanceHealthResult();
 	result.setInstanceStates(states);
    reply.setDescribeInstanceHealthResult(result);
    return reply;
  }
  
  ////////////////////////////  2nd-step operations ////////////////////////////

  public SetLoadBalancerListenerSSLCertificateResponseType setLoadBalancerListenerSSLCertificate(SetLoadBalancerListenerSSLCertificateType request) throws EucalyptusCloudException {
    SetLoadBalancerListenerSSLCertificateResponseType reply = request.getReply( );
    return reply;
  }
  
  
  ///////////////////////////   3rd-step operations ////////////////////////////
  public DescribeLoadBalancerPolicyTypesResponseType describeLoadBalancerPolicyTypes(DescribeLoadBalancerPolicyTypesType request) throws EucalyptusCloudException {
	DescribeLoadBalancerPolicyTypesResponseType reply = request.getReply( );
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
  
  public DeleteLoadBalancerPolicyResponseType deleteLoadBalancerPolicy(DeleteLoadBalancerPolicyType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerPolicyResponseType reply = request.getReply( );
 
    return reply;
  }

  public CreateLoadBalancerPolicyResponseType createLoadBalancerPolicy(CreateLoadBalancerPolicyType request) throws EucalyptusCloudException {
    CreateLoadBalancerPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public SetLoadBalancerPoliciesForBackendServerResponseType setLoadBalancerPoliciesForBackendServer(SetLoadBalancerPoliciesForBackendServerType request) throws EucalyptusCloudException {
    SetLoadBalancerPoliciesForBackendServerResponseType reply = request.getReply( );
    return reply;
  }

  public CreateLBCookieStickinessPolicyResponseType createLBCookieStickinessPolicy(CreateLBCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateLBCookieStickinessPolicyResponseType reply = request.getReply( );
    return reply;
  }

  public CreateAppCookieStickinessPolicyResponseType createAppCookieStickinessPolicy(CreateAppCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateAppCookieStickinessPolicyResponseType reply = request.getReply( );
    return reply;
  }


  //////////////////////// VPC - non support ////////////////////////////////
  public ApplySecurityGroupsToLoadBalancerResponseType applySecurityGroupsToLoadBalancer(ApplySecurityGroupsToLoadBalancerType request) throws EucalyptusCloudException {
    ApplySecurityGroupsToLoadBalancerResponseType reply = request.getReply( );
    return reply;
  }
  
/// VPC - non support
  public AttachLoadBalancerToSubnetsResponseType attachLoadBalancerToSubnets(AttachLoadBalancerToSubnetsType request) throws EucalyptusCloudException {
    AttachLoadBalancerToSubnetsResponseType reply = request.getReply( );
    return reply;
  }
  
/// VPC- non support
  public DetachLoadBalancerFromSubnetsResponseType detachLoadBalancerFromSubnets(DetachLoadBalancerFromSubnetsType request) throws EucalyptusCloudException {
    DetachLoadBalancerFromSubnetsResponseType reply = request.getReply( );
    return reply;
  }
}


