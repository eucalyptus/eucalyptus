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

import static com.eucalyptus.loadbalancing.LoadBalancingMetadata.LoadBalancerMetadata;
import static com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;

/**
 * @author Sang-Min Park
 */
public class LoadBalancers {
	private static Logger    LOG     = Logger.getLogger( LoadBalancers.class );
	public static LoadBalancer addLoadbalancer(final UserFullName owner, final String lbName) throws LoadBalancingException
	{
		return LoadBalancers.addLoadbalancer(owner,  lbName, null);
	}
	
	// a loadbalancer is per-account resource; per-user access is governed by IAM policy
	public static LoadBalancer getLoadbalancer(final Context ctx, final String lbName){
		final LoadBalancer lb= LoadBalancers.getLoadbalancer(ctx.getAccount().getName(), lbName);
		return lb;
	}
	
	private static LoadBalancer getLoadbalancer(final String accountName, final String lbName){
		 final EntityTransaction db = Entities.get( LoadBalancer.class );
		 try {
			 final LoadBalancer lb = Entities.uniqueResult( LoadBalancer.namedByAccount(accountName, lbName)); 
			 db.commit();
			 return lb;
		 }catch(NoSuchElementException ex){
			 db.rollback();
			 throw ex;
		 }catch(Exception ex){
			 db.rollback( );
			 LOG.error("failed to get the loadbalancer="+lbName, ex);
			 throw Exceptions.toUndeclared(ex);
		 }
	}
	
	///
	public static LoadBalancer getLoadBalancerByName(final String lbName) throws LoadBalancingException{
		 final EntityTransaction db = Entities.get( LoadBalancer.class );
		 try {
			 final List<LoadBalancer> lbs = Entities.query( LoadBalancer.named( null, lbName )); 
			 db.commit();
			 if(lbs==null || lbs.size()<=0)
				 throw new NoSuchElementException();
			 if(lbs.size()>1)
				 throw new LoadBalancingException("More than one loadbalancer with the same name found");
			 return lbs.get(0);
		 }catch(LoadBalancingException ex){
			 throw ex;
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}
	
	///
	public static LoadBalancer getLoadBalancerByDnsName(final String dnsName) throws NoSuchElementException{
		 final EntityTransaction db = Entities.get( LoadBalancerDnsRecord.class );
		 try{
			 final List<LoadBalancerDnsRecord> dnsList = Entities.query(LoadBalancerDnsRecord.named());
			 db.commit();
			 LoadBalancer lb = null;
			 for(final LoadBalancerDnsRecord dns : dnsList){
				 if(dns.getDnsName()!=null && dns.getDnsName().equals(dnsName))
					 lb= dns.getLoadBalancer();
			 }
			 if(lb!=null)
				 return lb;
			 else
				 throw new NoSuchElementException();
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }finally{
			 if(db.isActive())
				 db.rollback();
		 }
	}
	
	public static LoadBalancer addLoadbalancer(UserFullName user, String lbName, String scheme) throws LoadBalancingException {
		 final EntityTransaction db = Entities.get( LoadBalancer.class );
		 try {
		        try {
		        	if(Entities.uniqueResult( LoadBalancer.namedByAccount( user.getAccountName(), lbName )) != null)
		        		throw new DuplicateAccessPointName();
		        } catch ( NoSuchElementException e ) {
		        	final LoadBalancer lb = LoadBalancer.newInstance(user, lbName);
		        	if(scheme!=null)
		        		lb.setScheme(scheme);
		        	Entities.persist( lb );
		          	db.commit( );
		          	return lb;
		        }
		    }catch(LoadBalancingException ex){
		    	throw ex;
		    }catch ( Exception ex ) {
		    	db.rollback( );
		    	LOG.error("failed to persist a new loadbalancer", ex);
		    	throw new LoadBalancingException("Failed to persist a new load-balancer because of: " + ex.getMessage(), ex);
		  }
		  throw new LoadBalancingException("Failed to create a new load-balancer instance");
	}
	
	public static void deleteLoadbalancer(final UserFullName user, final String lbName) throws LoadBalancingException {
		Predicate<Void> delete = new Predicate<Void>(){
			@Override
			public boolean apply(@Nullable Void arg0) {
				try{
					final LoadBalancer toDelete =  Entities.uniqueResult( LoadBalancer.named(user, lbName));	
					Entities.delete(toDelete);
				}catch(final Exception ex){
					return false;
				}
				return true;
			}
		};
		Entities.asTransaction(LoadBalancer.class, delete).apply(null);
	}
	
	public static void validateListener(final List<Listener> listeners) 
				throws LoadBalancingException{
		validateListener(null, listeners);
	}
	
	public static void validateListener(final LoadBalancer lb, final List<Listener> listeners) 
				throws LoadBalancingException{
		for(Listener listener : listeners){
			if(!LoadBalancerListener.protocolSupported(listener))
				throw new UnsupportedParameterException("The requested protocol is not supported");
			if(!LoadBalancerListener.acceptable(listener))
				throw new InvalidConfigurationRequestException("Invalid listener format");
			if(!LoadBalancerListener.validRange(listener))
				throw new InvalidConfigurationRequestException("Invalid port range");

    		// check the listener 
			if(lb!=null && lb.hasListener(listener.getLoadBalancerPort().intValue())){
				LoadBalancerListener existing = lb.findListener(listener.getLoadBalancerPort().intValue());
				if(existing.getInstancePort() == listener.getInstancePort().intValue() &&
						existing.getProtocol().name().toLowerCase().equals(listener.getProtocol().toLowerCase()) &&
						(existing.getCertificateId()!=null && existing.getCertificateId().equals(listener.getSslCertificateId())))
					;
				else
					throw new DuplicateListenerException();
			}
		}
	}
	
	
	public static void createLoadbalancerListener(final String lbName, final Context ctx , final List<Listener> listeners) throws LoadBalancingException {
	    LoadBalancer lb = null;
    	try{
    		lb= LoadBalancers.getLoadbalancer(ctx, lbName);
    	}catch(Exception ex){
    		throw new InternalFailure400Exception("unable to find the loadbalancer");
	    }
    	
    	validateListener(lb, listeners);
    	
		final Predicate<LoadBalancer> creator = new Predicate<LoadBalancer>(){
	        @Override
	        public boolean apply( LoadBalancer lb ) {
	        	for(Listener listener : listeners){
	        		// check the listener 
	    			try{	
	        			if(!lb.hasListener(listener.getLoadBalancerPort().intValue())){
	        				LoadBalancerListener.Builder builder = new LoadBalancerListener.Builder(lb, listener.getInstancePort().intValue(), 
	            					listener.getLoadBalancerPort().intValue(), LoadBalancerListener.PROTOCOL.valueOf(listener.getProtocol().toUpperCase()));
	            			if(!Strings.isNullOrEmpty(listener.getInstanceProtocol()))
	            				builder.instanceProtocol(PROTOCOL.valueOf(listener.getInstanceProtocol()));
	            			
	            			if(!Strings.isNullOrEmpty(listener.getSslCertificateId()))
	            				builder.withSSLCerntificate(listener.getSslCertificateId());
	            			Entities.persist(builder.build());
	        			}
	    			}catch(Exception ex){
	    				LOG.warn("failed to create the listener object", ex);
	    			}
	        	}
	        	return true;
	        }
	    };
	    Entities.asTransaction(LoadBalancerListener.class, creator).apply(lb);
	}
	
	public static void addZone(final String lbName, final Context ctx, final Collection<String> zones) throws LoadBalancingException{
		List<ClusterInfoType> clusters = null;
		try{
			clusters = EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
		}catch(Exception ex){
			throw new InternalFailure400Exception("Unable to verify the requested zones");
		}
		for(String zone : zones){
			boolean found = false;
			for(ClusterInfoType cluster: clusters){	 // assume that describe-availability-zones return only enabled clusters
				if(zone.equals(cluster.getZoneName())){
					found = true;
					break;
				}
			}
			if(!found)
				throw new InvalidConfigurationRequestException("No cluster named "+zone+" is available");
		}
		
	   	LoadBalancer lb = null;
    	try{
    		lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    	}catch(Exception ex){
	    	throw new AccessPointNotFoundException();
	    }
    	try{
			for(String zone : zones){
				final EntityTransaction db = Entities.get( LoadBalancerZone.class );
				// check the listener 
				try{
					final LoadBalancerZone sample = LoadBalancerZone.named(lb, zone);
					final LoadBalancerZone exist = Entities.uniqueResult(sample);
					exist.setState(LoadBalancerZone.STATE.InService);
					Entities.persist(exist);
					db.commit();
				}catch(NoSuchElementException ex){
					final LoadBalancerZone newZone = LoadBalancerZone.named(lb, zone);
					newZone.setState(LoadBalancerZone.STATE.InService);
					Entities.persist(newZone);
					db.commit();
				}catch(Exception ex){
					db.rollback();
					LOG.error("failed to persist the zone "+zone, ex);
					throw ex;
				}
			}
    	}catch(Exception ex){
    		throw new InternalFailure400Exception("Failed to persist the zone");
    	}
	}
	
	public static void removeZone(final String lbName, final Context ctx, final Collection<String> zones) throws LoadBalancingException{
	 	LoadBalancer lb = null;
    	try{
    		lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    	}catch(Exception ex){
	    	throw new AccessPointNotFoundException();
	    }
		final EntityTransaction db = Entities.get( LoadBalancerZone.class );
		for(String zone : zones){
			try{
				final LoadBalancerZone exist = Entities.uniqueResult(LoadBalancerZone.named(lb, zone));
				Entities.delete(exist);
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
				LOG.debug(String.format("zone %s not found for %s", zone, lbName));
			}catch(Exception ex){
				db.rollback();
				LOG.error("failed to delete the zone "+zone, ex);
			}
		}
	}
	
	public static LoadBalancerZone findZone(final LoadBalancer lb, final String zoneName){
		final EntityTransaction db = Entities.get(LoadBalancerZone.class);
		try{
			final LoadBalancerZone exist = Entities.uniqueResult(LoadBalancerZone.named(lb, zoneName));
			db.commit();
			return exist;
		}catch(NoSuchElementException ex){
			db.rollback();
			throw ex;
		}catch(Exception ex){
			db.rollback();
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public static List<LoadBalancerZone> findZonesInService(final LoadBalancer lb){
		final List<LoadBalancerZone> inService = Lists.newArrayList();
		for(final LoadBalancerZone zone : lb.getZones()){
			if(zone.getState().equals(LoadBalancerZone.STATE.InService))
				inService.add(zone);
		}
		return inService;
	}
	
	public static LoadBalancerDnsRecord getDnsRecord(final LoadBalancer lb) throws LoadBalancingException{
		/// create the next dns record
		final EntityTransaction db = Entities.get( LoadBalancerDnsRecord.class );
		try{
			LoadBalancerDnsRecord exist = Entities.uniqueResult(LoadBalancerDnsRecord.named(lb));
			db.commit();
			return exist;
		}catch(NoSuchElementException ex){
			final LoadBalancerDnsRecord newRec = 
					LoadBalancerDnsRecord.named(lb);
			Entities.persist(newRec);
			db.commit();
			return newRec;
		}catch(Exception ex){
			db.rollback();
			throw new LoadBalancingException("failed to query dns record", ex);
		}
	}
	
	public static void deleteDnsRecord(final LoadBalancerDnsRecord dns) throws LoadBalancingException{
		final EntityTransaction db = Entities.get( LoadBalancerDnsRecord.class );
		try{
			LoadBalancerDnsRecord exist = Entities.uniqueResult(dns);
			Entities.delete(exist);
			db.commit();
		}catch(NoSuchElementException ex){
			db.rollback();
		}catch(Exception ex){
			db.rollback();
			throw new LoadBalancingException("failed to delete dns record", ex);
		}
	}
	
	public static LoadBalancerServoInstance lookupServoInstance(final String instanceId) throws LoadBalancingException {
		final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
		try{
			LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
			final LoadBalancerServoInstance exist = Entities.uniqueResult(sample);
			db.commit();
			return exist;
		}catch(NoSuchElementException ex){
			db.rollback();
			throw ex;
		}catch(Exception ex){
			db.rollback();
			throw new LoadBalancingException("failed to query servo instances");
		}
	}
	
	public static LoadBalancerBackendInstance lookupBackendInstance(final String instanceId) {
		final EntityTransaction db = Entities.get( LoadBalancerBackendInstance.class ) ;
		try{
			final LoadBalancerBackendInstance found = Entities.uniqueResult(LoadBalancerBackendInstance.named(instanceId));
			db.commit();
			return found;
		}catch(final NoSuchElementException ex){
			db.rollback();
			throw ex;
		}catch(final Exception ex){
			db.rollback();
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public static void unsetForeignKeys(final Context ctx, final String loadbalancer){
		Predicate<LoadBalancerServoInstance> unsetServoInstanceKey = new Predicate<LoadBalancerServoInstance>(){
			@Override
			public boolean apply(@Nullable LoadBalancerServoInstance arg0) {
				try{
					final LoadBalancerServoInstance update = Entities.uniqueResult(arg0);
					//update.setSecurityGroup(null);
					update.setAvailabilityZone(null);
					update.setAutoScalingGroup(null);
					update.setDns(null);
					return true;
				}catch(final Exception ex){
					return false;
				}
			}
		};
		
		LoadBalancer lb = null;
		try{
			lb = getLoadbalancer(ctx, loadbalancer);
		}catch(Exception ex){
			return;
		}
		if(lb!=null){
			if(lb.getZones()!=null){
				for(final LoadBalancerZone zone : lb.getZones()){
					for(final LoadBalancerServoInstance servo : zone.getServoInstances()){
						try{
							Entities.asTransaction(LoadBalancerServoInstance.class, unsetServoInstanceKey).apply(servo);
						}catch(Exception ex){
							;
						}
					}
				}
			}
		}
	}

  @QuantityMetricFunction( LoadBalancerMetadata.class )
  public enum CountLoadBalancers implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( LoadBalancer.class );
      try {
        return Entities.count( LoadBalancer.named( input, null ) );
      } finally {
        db.rollback( );
      }
    }
  }
}
