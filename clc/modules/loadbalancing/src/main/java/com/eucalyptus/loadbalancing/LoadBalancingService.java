package com.eucalyptus.loadbalancing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
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
import com.eucalyptus.tags.Tag;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 */
public class LoadBalancingService {
  private static Logger    LOG     = Logger.getLogger( LoadBalancingService.class );
  
  public PutServoStatesResponseType putServoStates(PutServoStatesType request){
	  PutServoStatesResponseType reply = request.getReply();
	  return reply;
  }
  
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
    final DisableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName();
    final Collection<String> zones = request.getAvailabilityZones().getMember();
    if(zones != null && zones.size()>0){
    	LoadBalancers.removeZone(lbName, ownerFullName, zones);
    }
    reply.set_return(true);
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
    final EnableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName();
    final Collection<String> zones = request.getAvailabilityZones().getMember();
    if(zones != null && zones.size()>0){
    	LoadBalancers.addZone(lbName, ownerFullName, zones);
    }
    reply.set_return(true);
    return reply;
  }

  public CreateLoadBalancerListenersResponseType createLoadBalancerListeners(CreateLoadBalancerListenersType request) throws EucalyptusCloudException {
    final CreateLoadBalancerListenersResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserFullName ownerFullName = ctx.getUserFullName( );
    final String lbName = request.getLoadBalancerName();
    final Collection<Listener> listeners = request.getListeners().getMember();
    LoadBalancers.createLoadbalancerListener(lbName,  ownerFullName, listeners);
    reply.set_return(true);
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
    
    Collection<Listener> listeners=request.getListeners().getMember();
    if(listeners!=null && listeners.size()>0){
    	LoadBalancers.createLoadbalancerListener(lbName,  ownerFullName, listeners);
    }
    Collection<String> zones = request.getAvailabilityZones().getMember();
    if(zones != null && zones.size()>0){
    	LoadBalancers.addZone(lbName, ownerFullName, zones);
    }
    /// TODO: SPARK we should add zones, listeners, etc., if present in the request
    final CreateLoadBalancerResult result = new CreateLoadBalancerResult();
    if(lb!=null && lb.getDnsAddress()==null){
    	LOG.warn("No DNS name is assigned to a loadblancer "+lbName);
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
    reply.set_return(Entities.asTransaction(LoadBalancerListener.class, remover).apply(toDelete));
    //reply.set_return(true);
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
    
    reply.set_return(Entities.asTransaction(LoadBalancerBackendInstance.class, remover).apply(null));
    DeregisterInstancesFromLoadBalancerResult result = new DeregisterInstancesFromLoadBalancerResult();
    Instances returnInstances = new Instances();
    returnInstances.setMember(Entities.asTransaction(LoadBalancer.class, finder).apply(null));
    result.setInstances(returnInstances);
    reply.setDeregisterInstancesFromLoadBalancerResult(result);
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
        	
    if(instances!=null){
		reply.set_return(Entities.asTransaction(LoadBalancerBackendInstance.class, creator).apply(null));
    }
    
    RegisterInstancesWithLoadBalancerResult result = new RegisterInstancesWithLoadBalancerResult();
    Instances returnInstances = new Instances();
    returnInstances.setMember(Entities.asTransaction(LoadBalancer.class, finder).apply(null));
    result.setInstances(returnInstances);
    reply.setRegisterInstancesWithLoadBalancerResult(result);
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
    			desc.setDnsName(lb.getDnsAddress());           /// dns name
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
}
