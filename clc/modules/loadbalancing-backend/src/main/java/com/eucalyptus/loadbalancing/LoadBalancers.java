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

import static com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import static com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNewListeners;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.eucalyptus.loadbalancing.backend.AccessPointNotFoundException;
import com.eucalyptus.loadbalancing.backend.CertificateNotFoundException;
import com.eucalyptus.loadbalancing.backend.DuplicateAccessPointName;
import com.eucalyptus.loadbalancing.backend.DuplicateListenerException;
import com.eucalyptus.loadbalancing.backend.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.backend.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.backend.ListenerNotFoundException;
import com.eucalyptus.loadbalancing.backend.LoadBalancingException;
import com.eucalyptus.loadbalancing.backend.UnsupportedParameterException;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * @author Sang-Min Park
 */
public class LoadBalancers {
	private static Logger    LOG     = Logger.getLogger( LoadBalancers.class );

	public static List<LoadBalancer> listLoadbalancers(){
		 try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
			 return Entities.query(LoadBalancer.named());
		 }catch(final NoSuchElementException ex){
			 return Lists.newArrayList();
		 }catch(final Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }
	}
	
	public static List<LoadBalancer> listLoadbalancers(final String accountNumber) {
	  try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
      return Entities.query(LoadBalancer.ownedByAccount(accountNumber));
    }catch(final NoSuchElementException ex){
      return Lists.newArrayList();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
	}
	
	// a loadbalancer is per-account resource; per-user access is governed by IAM policy
	@Nonnull
	public static LoadBalancer getLoadbalancer(final Context ctx, final String lbName){
		return LoadBalancers.getLoadbalancer( ctx.getAccount().getAccountNumber(), lbName );
	}
	
	private static LoadBalancer getLoadbalancer(final String accountNumber, final String lbName){
		 LoadBalancer lb = null;
		 try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
			 lb = Entities.uniqueResult( LoadBalancer.namedByAccountId(accountNumber, lbName)); 
			 db.commit();
			 return lb;
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 if(lb!=null)
				 return lb;
			 else
				 throw Exceptions.toUndeclared(ex);
		 }
	}
	
	///
	public static LoadBalancer getLoadBalancerByDnsName(final String dnsName) throws NoSuchElementException{
		 try ( final TransactionResource db = Entities.transactionFor( LoadBalancerDnsRecord.class ) ) {
			 final List<LoadBalancerDnsRecord> dnsList = Entities.query(LoadBalancerDnsRecord.named());
			 db.commit();
			 LoadBalancerCoreView lbView = null;
			 for(final LoadBalancerDnsRecord dns : dnsList){
				 if(dns.getDnsName()!=null && dns.getDnsName().equals(dnsName))
					 lbView= dns.getLoadbalancer();
			 }
			 if(lbView == null)
				 throw new NoSuchElementException();
			 return LoadBalancerEntityTransform.INSTANCE.apply(lbView);
		 }catch(NoSuchElementException ex){
			 throw ex;
		 }catch(Exception ex){
			 throw Exceptions.toUndeclared(ex);
		 }
	}
	
  public static LoadBalancer addLoadbalancer(
      final UserFullName user,
      final String lbName,
      final String vpcId,
      final LoadBalancer.Scheme scheme,
      final Map<String,String> securityGroupIdsToNames,
      final Map<String,String> tags ) throws LoadBalancingException {
    
    final List<LoadBalancer> accountLbs = LoadBalancers.listLoadbalancers(user.getAccountNumber());
    for(final LoadBalancer lb : accountLbs) {
      if (lbName.toLowerCase().equals(lb.getDisplayName().toLowerCase()))
        throw new DuplicateAccessPointName( );
    }
    
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
      try {
        if( Entities.uniqueResult( LoadBalancer.namedByAccountId( user.getAccountNumber(), lbName ) ) != null )
          throw new DuplicateAccessPointName( );
      } catch ( final NoSuchElementException e ) {
        final List<LoadBalancerSecurityGroupRef> refs = Lists.newArrayList( );
        for ( final Map.Entry<String,String> groupIdToNameEntry : securityGroupIdsToNames.entrySet( ) ) {
          refs.add( new LoadBalancerSecurityGroupRef( groupIdToNameEntry.getKey( ), groupIdToNameEntry.getValue( ) ) );
        }
        Collections.sort( refs, Ordering.natural( ).onResultOf( LoadBalancerSecurityGroupRef.groupId( ) ) );

        final LoadBalancer lb = LoadBalancer.newInstance(user, lbName );
        lb.setVpcId( vpcId );
        lb.setScheme( scheme );
        lb.setSecurityGroupRefs( refs );
        lb.setTags( tags );
        Entities.persist( lb );
        db.commit( );
        return lb;
      }
    }catch(LoadBalancingException ex){
      throw ex;
    }catch ( Exception ex ) {
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
				throws LoadBalancingException, EucalyptusCloudException{
		validateListener(null, listeners);
	}
	
	public static void validateListener(final LoadBalancer lb, final List<Listener> listeners) 
				throws LoadBalancingException, EucalyptusCloudException{
		for(Listener listener : listeners){
			if(!LoadBalancerListener.protocolSupported(listener))
				throw new UnsupportedParameterException("The requested protocol is not supported");
			if(!LoadBalancerListener.acceptable(listener))
				throw new InvalidConfigurationRequestException("Invalid listener format");
			if(!LoadBalancerListener.validRange(listener))
				throw new InvalidConfigurationRequestException("Invalid port range");
			if(!LoadBalancerListener.portAvailable(listener))
				throw new EucalyptusCloudException("The specified port is restricted for use as a loadbalancer port");
			final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
			  if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
			  final String sslId = listener.getSSLCertificateId();
			  if(sslId==null || sslId.length()<=0)
			    throw new InvalidConfigurationRequestException("SSLCertificateId is required for HTTPS or SSL protocol");
			}
			
    		// check the listener 
			if(lb!=null && lb.hasListener( listener.getLoadBalancerPort() )){
				final LoadBalancerListenerCoreView existing = lb.findListener( listener.getLoadBalancerPort() );
				if ( existing.getInstancePort() != listener.getInstancePort() ||
						!existing.getProtocol().name().toLowerCase().equals( listener.getProtocol().toLowerCase() ) ||
						( ( existing.getCertificateId() == null || !existing.getCertificateId().equals( listener.getSSLCertificateId() ) ) ) ) {
					throw new DuplicateListenerException();
				}
			}
		}
	}
	
	
	public static void createLoadbalancerListener(final String lbName, final Context ctx , final List<Listener> listeners) 
			throws LoadBalancingException, EucalyptusCloudException {
	    LoadBalancer lb;
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
	        			if(!lb.hasListener( listener.getLoadBalancerPort() )){
	        				LoadBalancerListener.Builder builder = new LoadBalancerListener.Builder(lb, listener.getInstancePort(),
											listener.getLoadBalancerPort(), LoadBalancerListener.PROTOCOL.valueOf(listener.getProtocol().toUpperCase()));
	            			if(!Strings.isNullOrEmpty(listener.getInstanceProtocol()))
	            				builder.instanceProtocol(PROTOCOL.valueOf(listener.getInstanceProtocol()));
	            			
	            			if(!Strings.isNullOrEmpty(listener.getSSLCertificateId()))
	            				builder.withSSLCerntificate(listener.getSSLCertificateId());
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
	
	public static void addZone(
		final String lbName,
		final Context ctx,
		final Collection<String> zones,
		final Map<String,String> zoneToSubnetIdMap
	) throws LoadBalancingException {
		LoadBalancer lb;
		try{
			lb = LoadBalancers.getLoadbalancer(ctx, lbName);
		}catch(Exception ex){
			throw new AccessPointNotFoundException();
		}
		try{
			for( final String zone : zones ){
				// check the listener
				try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
					try {
						final LoadBalancerZone sample = LoadBalancerZone.named( lb, zone );
						final LoadBalancerZone exist = Entities.uniqueResult( sample );
						exist.setState( LoadBalancerZone.STATE.InService );
					} catch( final NoSuchElementException ex ) {
						final LoadBalancerZone newZone = LoadBalancerZone.create( lb, zone, zoneToSubnetIdMap.get( zone ) );
						newZone.setState( LoadBalancerZone.STATE.InService );
						Entities.persist( newZone );
					}
					db.commit();
				} catch( final Exception ex ){
					LOG.error("failed to persist the zone "+zone, ex);
					throw ex;
				}
			}
		}catch(Exception ex){
			throw new InternalFailure400Exception("Failed to persist the zone");
		}
	}
	
	public static void removeZone(final String lbName, final Context ctx, final Collection<String> zones) throws LoadBalancingException{
	 	LoadBalancer lb;
    	try{
    		lb = LoadBalancers.getLoadbalancer(ctx, lbName);
    	}catch(Exception ex){
	    	throw new AccessPointNotFoundException();
	    }
		for(String zone : zones){
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
				final LoadBalancerZone exist = Entities.uniqueResult(LoadBalancerZone.named(lb, zone));
				Entities.delete(exist);
				db.commit();
			}catch(NoSuchElementException ex){
				LOG.debug(String.format("zone %s not found for %s", zone, lbName));
			}catch(Exception ex){
				LOG.error("failed to delete the zone "+zone, ex);
			}
		}
	}
	
	public static LoadBalancerZone findZone(final LoadBalancer lb, final String zoneName){
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
			final LoadBalancerZone exist = Entities.uniqueResult(LoadBalancerZone.named(lb, zoneName));
			db.commit();
			return exist;
		}catch(NoSuchElementException ex){
			throw ex;
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public static List<LoadBalancerZoneCoreView> findZonesInService(final LoadBalancer lb){
		final List<LoadBalancerZoneCoreView> inService = Lists.newArrayList();
		for(final LoadBalancerZoneCoreView zone : lb.getZones()){
			if(zone.getState().equals(LoadBalancerZone.STATE.InService))
				inService.add(zone);
		}
		return inService;
	}
	
	public static LoadBalancerDnsRecord getDnsRecord(final LoadBalancer lb) throws LoadBalancingException{
		/// create the next dns record
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerDnsRecord.class ) ) {
			try {
				return Entities.uniqueResult( LoadBalancerDnsRecord.named( lb ) );
			} catch( NoSuchElementException ex ) {
				final LoadBalancerDnsRecord newRec = LoadBalancerDnsRecord.named( lb );
				Entities.persist( newRec );
				db.commit();
				return newRec;
			}
		}catch(Exception ex){
			throw new LoadBalancingException("failed to query dns record", ex);
		}
	}
	
	public static void deleteDnsRecord(final LoadBalancerDnsRecord dns) throws LoadBalancingException{
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerDnsRecord.class ) ) {
			LoadBalancerDnsRecord exist = Entities.uniqueResult(dns);
			Entities.delete(exist);
			db.commit();
		}catch(NoSuchElementException ex){
			// nothing to delete
		}catch(Exception ex){
			throw new LoadBalancingException("failed to delete dns record", ex);
		}
	}
	
	public static LoadBalancerServoInstance lookupServoInstance(final String instanceId) throws LoadBalancingException {
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
			LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
			final LoadBalancerServoInstance exist = Entities.uniqueResult(sample);
			db.commit();
			return exist;
		}catch(NoSuchElementException ex){
			throw ex;
		}catch(Exception ex){
			throw new LoadBalancingException("failed to query servo instances", ex);
		}
	}
	
	public static LoadBalancerBackendInstance lookupBackendInstance(final LoadBalancer lb, final String instanceId) {
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
			final LoadBalancerBackendInstance found = Entities.uniqueResult(LoadBalancerBackendInstance.named(lb, instanceId));
			db.commit();
			return found;
		}catch(final NoSuchElementException ex){
			throw ex;
		}catch(final Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public static void deleteBackendInstance(final LoadBalancer lb, final String instanceId) {
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
			final LoadBalancerBackendInstance toDelete = Entities.uniqueResult(LoadBalancerBackendInstance.named(lb, instanceId));
		    Entities.delete(toDelete);
		    db.commit();
		}catch(final NoSuchElementException ex){
			throw ex;
		}catch(final Exception ex){
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
		
		LoadBalancer lb;
		try{
			lb = getLoadbalancer(ctx, loadbalancer);
		}catch(Exception ex){
			return;
		}
		if(lb!=null){
			if(lb.getZones()!=null){
				for(final LoadBalancerZoneCoreView zoneView : lb.getZones()){
					LoadBalancerZone zone;
					try{
						zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
					}catch(final Exception ex){
						continue;
					}
					
					for(LoadBalancerServoInstanceCoreView servo : zone.getServoInstances()){
						try{
							final LoadBalancerServoInstance instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(servo);
							Entities.asTransaction(LoadBalancerServoInstance.class, unsetServoInstanceKey).apply(instance);
						}catch(Exception ex){
						}
					}
				}
			}
		}
	}
	
	public static void setLoadBalancerListenerSSLCertificate(final LoadBalancer lb, final int lbPort, final String certArn)
	    throws LoadBalancingException {
	  final Collection<LoadBalancerListenerCoreView> listeners = lb.getListeners();
	  LoadBalancerListenerCoreView listener = null;
	  for(final LoadBalancerListenerCoreView l : listeners){
	    if(l.getLoadbalancerPort() == lbPort){
	      listener = l;
	      break;
	    }
	  }
	  if(listener == null)
	    throw new ListenerNotFoundException();
	  if(!(PROTOCOL.HTTPS.equals(listener.getProtocol()) || PROTOCOL.SSL.equals(listener.getProtocol())))
	    throw new InvalidConfigurationRequestException("Listener's protocol is not HTTPS or SSL");
	      
	  checkSSLCertificate(lb.getOwnerUserId(), certArn);
	  updateIAMRolePolicy(lb.getOwnerAccountNumber(), lb.getDisplayName(), listener.getCertificateId(), certArn);
	  try ( final TransactionResource db = Entities.transactionFor( LoadBalancerListener.class ) ) {
	    final LoadBalancerListener update = Entities.uniqueResult(LoadBalancerListener.named(lb, lbPort));
	    update.setSSLCertificateId( certArn );
	    Entities.persist(update);
	    db.commit();
	  }catch(final NoSuchElementException ex){
	    throw new ListenerNotFoundException();
	  }catch(final Exception ex){
	    throw Exceptions.toUndeclared(ex);
	  }
	}
	
	private static void updateIAMRolePolicy(final String accountId, final String lbName, 
	    final String oldCertArn, final String newCertArn) throws LoadBalancingException{
	   final String prefix = 
         String.format("arn:aws:iam::%s:server-certificate", accountId);
     final String oldCertName = oldCertArn.replace(prefix, "")
         .substring(oldCertArn.replace(prefix, "").lastIndexOf("/")+1);
     final String newCertName = newCertArn.replace(prefix, "")
         .substring(newCertArn.replace(prefix, "").lastIndexOf("/")+1);
	  
     final String roleName = String.format("%s-%s-%s", EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, 
        accountId, lbName);
     final String oldPolicyName = String.format("%s-%s-%s-%s", 
	      EventHandlerChainNewListeners.AuthorizeSSLCertificate.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
          accountId, lbName, oldCertName);  
     try{
         EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, oldPolicyName);
      }catch(final Exception ex){
        throw new LoadBalancingException("Failed to delete old role policy "+oldPolicyName, ex);
     }
     final String newPolicyName = String.format("%s-%s-%s-%s", 
         EventHandlerChainNewListeners.AuthorizeSSLCertificate.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
         accountId, lbName, newCertName); 
     final String newPolicyDoc = EventHandlerChainNewListeners
         .AuthorizeSSLCertificate.ROLE_SERVER_CERT_POLICY_DOCUMENT
         .replace("CERT_ARN_PLACEHOLDER", newCertArn);
     try{
       EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, newPolicyName, newPolicyDoc);
      }catch(final Exception ex){
       throw new LoadBalancingException("Failed to add new role policy "+newPolicyName, ex);
     }
	}
	
	
	public static void checkSSLCertificate(final String userId, final String certArn)  
	    throws LoadBalancingException {
	  String acctNumber;
	  try{
  	  User user = Accounts.lookupUserById(userId);
  	  acctNumber = user.getAccountNumber();
	  }catch(final Exception ex){
	    throw new InternalFailure400Exception("Unable to check ssl certificate Id", ex);
	  }
	  try{
      final String prefix = String.format("arn:aws:iam::%s:server-certificate", acctNumber);
      if(!certArn.startsWith(prefix))
        throw new CertificateNotFoundException();
      
      final String pathAndName = certArn.replace(prefix, "");
      final String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
      final ServerCertificateType cert = 
          EucalyptusActivityTasks.getInstance().getServerCertificate(userId, certName);
      if(cert==null)
        throw new CertificateNotFoundException();
      if(!certArn.equals(cert.getServerCertificateMetadata().getArn()))
        throw new CertificateNotFoundException();
    }catch(final Exception ex){
      throw new CertificateNotFoundException();
    }
	}

  @QuantityMetricFunction( LoadBalancerMetadata.class )
  public enum CountLoadBalancers implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
        return Entities.count( LoadBalancer.named( input, null ) );
      }
    }
  }
}
