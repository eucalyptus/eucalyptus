/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.loadbalancing.LoadBalancer.Scheme;
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
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.STATE;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNewListeners;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.eucalyptus.loadbalancing.backend.AccessPointNotFoundException;
import com.eucalyptus.loadbalancing.backend.CertificateNotFoundException;
import com.eucalyptus.loadbalancing.backend.DuplicateAccessPointName;
import com.eucalyptus.loadbalancing.backend.DuplicateListenerException;
import com.eucalyptus.loadbalancing.backend.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.backend.InternalFailureException;
import com.eucalyptus.loadbalancing.backend.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.backend.ListenerNotFoundException;
import com.eucalyptus.loadbalancing.backend.LoadBalancingException;
import com.eucalyptus.loadbalancing.backend.UnsupportedParameterException;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Optional;
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
	
	public static LoadBalancer getLoadbalancer(final String accountNumber, final String lbName){
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

	public static String getLoadBalancerDnsName( final LoadBalancerCoreView loadBalancer ) {
		return getLoadBalancerDnsName(
				loadBalancer.getScheme(),
				loadBalancer.getDisplayName(),
				loadBalancer.getOwnerAccountNumber()
		);
	}

	public static String getLoadBalancerDnsName( final LoadBalancer loadBalancer ) {
		return getLoadBalancerDnsName(
				loadBalancer.getScheme(),
				loadBalancer.getDisplayName(),
				loadBalancer.getOwnerAccountNumber()
		);
	}

	private static String getLoadBalancerDnsName(
			@Nullable final Scheme scheme,
			@Nonnull  final String displayName,
			@Nonnull  final String accountNumber
	) {
		return LoadBalancerDomainName.forScheme( scheme ).generate( displayName, accountNumber );
	}

	public static LoadBalancer getLoadBalancerByDnsName( final String dnsName ) throws NoSuchElementException {
		try {
			final Name hostName = Name.fromString( dnsName, Name.root ).relativize( LoadBalancerDomainName.getLoadBalancerSubdomain() );
			final Optional<LoadBalancerDomainName> domainName = LoadBalancerDomainName.findMatching( hostName );
			if ( domainName.isPresent( ) ) {
				final Pair<String, String> accountNamePair = domainName.get( ).toScopedLoadBalancerName( hostName );
				try {
					return LoadBalancers.getLoadbalancer( accountNamePair.getLeft( ), accountNamePair.getRight( ) );
				} catch ( NoSuchElementException e ) {
					if ( domainName.get( ) == LoadBalancerDomainName.INTERNAL ) { // perhaps it was an external balancer named "internal-..."
						final Pair<String, String> externalAccountNamePair = LoadBalancerDomainName.EXTERNAL.toScopedLoadBalancerName( hostName );
						return LoadBalancers.getLoadbalancer( externalAccountNamePair.getLeft( ), externalAccountNamePair.getRight( ) );
					} else {
						throw e;
					}
				}
			} else {
				throw new NoSuchElementException();
			}
		} catch ( TextParseException e ) {
			throw new NoSuchElementException( );
		}
	}

	public static Predicate<LoadBalancer> v4_2_0 = (lb) -> {
		return versionOnOrLater(lb, DeploymentVersion.v4_2_0);
	};

	public static Predicate<LoadBalancer> v4_3_0 = (lb) -> {
		return versionOnOrLater(lb, DeploymentVersion.v4_3_0);
	};

	private static boolean versionOnOrLater(final LoadBalancer lb, DeploymentVersion version) {
		if (lb.getLoadbalancerDeploymentVersion() == null) {
			return false;
		} else {
			return DeploymentVersion.getVersion(
					lb.getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(version);
		}
	}
	
	public enum DeploymentVersion {
		v4_1_0,
		v4_2_0, // the version is checked from 4.2.0
		v4_3_0;

		public static DeploymentVersion Latest = v4_3_0;

		public String toVersionString(){
			return this.name( ).substring( 1 ).replace( "_", "." );
		}

		public static DeploymentVersion getVersion(final String version) {
			if( version == null || version.length() <= 0)
				return DeploymentVersion.v4_1_0;

			return DeploymentVersion.valueOf( "v" + version.replace( ".", "_" ) );
		}

		public boolean isLaterThan(final DeploymentVersion other) {
			if(other==null)
				return false;

			String[] thisVersionDigits = this.name().substring(1).split("_");
			String[] otherVersionDigits = other.name().substring(1).split("_");

			for(int i=0; i<thisVersionDigits.length; i++){
				int thisDigit = Integer.parseInt(thisVersionDigits[i]);
				int otherDigit = 0;
				if(i < otherVersionDigits.length)
					otherDigit = Integer.parseInt(otherVersionDigits[i]);

				if(thisDigit > otherDigit)
					return true;
				else if(thisDigit < otherDigit)
					return false;
			}
			return false;
		}

		public boolean isEqualOrLaterThan(final DeploymentVersion other) {
			return this.equals(other) || this.isLaterThan(other);
		}
	}

  public static LoadBalancer addLoadbalancer(
      final UserFullName user,
      final String lbName,
      final String vpcId,
      final Scheme scheme,
      final Map<String,String> securityGroupIdsToNames,
      final Map<String,String> tags ) throws LoadBalancingException {
    
    final List<LoadBalancer> accountLbs = LoadBalancers.listLoadbalancers(user.getAccountNumber());
    for(final LoadBalancer lb : accountLbs) {
      if (lbName.toLowerCase().equals(lb.getDisplayName().toLowerCase()))
        throw new DuplicateAccessPointName( );
    }
    
    /// EC2 classic
    if (vpcId == null) {
      final String securityGroupName = 
          SecurityGroupSetup.getSecurityGroupName(user.getAccountNumber(), lbName);
      try ( final TransactionResource db = Entities.transactionFor(LoadBalancerSecurityGroup.class)) {
        try{
          final List<LoadBalancerSecurityGroup> groups =
              Entities.query(LoadBalancerSecurityGroup.withState(STATE.OutOfService));
          for(final LoadBalancerSecurityGroup group : groups) {
            if (securityGroupName.equals(group.getName())) {
              throw new InternalFailureException("Cleaning up the previous ELB with the same name. Retry in a few minutes.");
            }
          }
        }catch(final NoSuchElementException e )  {
          ;
        }
      }
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
        lb.setLoadbalancerDeploymentVersion(DeploymentVersion.Latest.toVersionString());
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
				throw new EucalyptusCloudException("The specified port(s) " + LoadBalancerListener.RESTRICTED_PORTS + ", are restricted for use as a loadbalancer port.");
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
	
	public static LoadBalancerServoInstance lookupServoInstance(final String instanceId) throws LoadBalancingException {
		try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
			LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
			final LoadBalancerServoInstance exist = Entities.uniqueResult(sample);
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
					update.setAvailabilityZone( null );
					update.setAutoScalingGroup( null );
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
	      
	  checkSSLCertificate(lb.getOwnerAccountNumber(), certArn);
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
     LoadBalancer lb;
     try{ 
       lb= LoadBalancers.getLoadbalancer(accountId, lbName);
     }catch(Exception ex){
       throw new LoadBalancingException("Failed to find the loadbalancer named " + lbName, ex);
     } 
     
     try{
         EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, oldPolicyName, lb.useSystemAccount());
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
       EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, newPolicyName, newPolicyDoc, lb.useSystemAccount());
      }catch(final Exception ex){
       throw new LoadBalancingException("Failed to add new role policy "+newPolicyName, ex);
     }
	}
	
	
	public static void checkSSLCertificate(final String accountNumber, final String certArn)
	    throws LoadBalancingException {
	  try{
      final String prefix = String.format("arn:aws:iam::%s:server-certificate", accountNumber);
      if(!certArn.startsWith(prefix))
        throw new CertificateNotFoundException();
      
      final String pathAndName = certArn.replace(prefix, "");
      final String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
      final ServerCertificateType cert = 
          EucalyptusActivityTasks.getInstance().getServerCertificate(accountNumber, certName);
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
