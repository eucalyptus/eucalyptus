/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import static com.eucalyptus.loadbalancing.LoadBalancer.Scheme;
import static com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import static com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.loadbalancing.activities.LoadBalancerVersionException;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.context.Context;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.STATE;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstance;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstances;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescription;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.loadbalancing.service.AccessPointNotFoundException;
import com.eucalyptus.loadbalancing.service.CertificateNotFoundException;
import com.eucalyptus.loadbalancing.service.DuplicateAccessPointName;
import com.eucalyptus.loadbalancing.service.DuplicateListenerException;
import com.eucalyptus.loadbalancing.service.InternalFailure400Exception;
import com.eucalyptus.loadbalancing.service.InternalFailureException;
import com.eucalyptus.loadbalancing.service.InvalidConfigurationRequestException;
import com.eucalyptus.loadbalancing.service.ListenerNotFoundException;
import com.eucalyptus.loadbalancing.service.LoadBalancingException;
import com.eucalyptus.loadbalancing.service.UnsupportedParameterException;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingActivitiesImpl;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

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

	public static LoadBalancer getLoadbalancer(final String accountNumber, final String lbName) {
		LoadBalancer lb = null;
		try (final TransactionResource db = Entities.transactionFor(LoadBalancer.class)) {
			lb = Entities.uniqueResult(LoadBalancer.namedByAccountId(accountNumber, lbName));
			db.commit();
			return lb;
		} catch (NoSuchElementException ex) {
			throw ex;
		} catch (Exception ex) {
			if (lb != null)
				return lb;
			else
				throw Exceptions.toUndeclared(ex);
		}
	}

	public static LoadBalancer getLoadbalancerCaseInsensitive(final String accountNumber, final String lbName) {
		for (final LoadBalancer lb : listLoadbalancers(accountNumber) ) {
			if (lb.getDisplayName().toLowerCase().equals(lbName.toLowerCase())) {
				return lb;
			}
		}
		throw new NoSuchElementException();
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

	public static void checkVersion(final LoadBalancer lb, DeploymentVersion minVersion) throws LoadBalancerVersionException {
		if(lb.getLoadbalancerDeploymentVersion() == null ||
				! DeploymentVersion.getVersion(
						lb.getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(minVersion)) {
			throw new LoadBalancerVersionException(minVersion);
		}
	}

	public static Predicate<LoadBalancer> v4_2_0 = (lb) -> {
		return versionOnOrLater(lb, DeploymentVersion.v4_2_0);
	};

	public static Predicate<LoadBalancer> v4_3_0 = (lb) -> {
		return versionOnOrLater(lb, DeploymentVersion.v4_3_0);
	};

	public static Predicate<LoadBalancer> v4_4_0 = (lb) -> {
		return versionOnOrLater(lb, DeploymentVersion.v4_4_0);
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
		v4_3_0,
		v4_4_0;

		public static DeploymentVersion Latest = v4_4_0;

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
      ///FIXME: not a sane reference
      final String securityGroupName = 
          LoadBalancingActivitiesImpl.getSecurityGroupName(user.getAccountNumber(), lbName);
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

     ////FIXME: not a sound reference
     final String roleName = String.format("%s-%s-%s", LoadBalancingActivitiesImpl.ROLE_NAME_PREFIX, 
        accountId, lbName);
     final String oldPolicyName = String.format("%s-%s-%s-%s", 
	      LoadBalancingActivitiesImpl.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
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
         LoadBalancingActivitiesImpl.SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
         accountId, lbName, newCertName); 
     final String newPolicyDoc = LoadBalancingActivitiesImpl.ROLE_SERVER_CERT_POLICY_DOCUMENT
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

	//// WARNING: this method is database intensive call
	//// Do not invoke too frequently!
	public static LoadBalancerServoDescription getServoDescription(
	    final String accountId, final String lbName, final String zone) 
	        throws LoadBalancingException {
	  final LoadBalancer lb = getLoadbalancer(accountId, lbName);
	  final LoadBalancerZone lbZone;
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
      try {
        final LoadBalancerZone sample = LoadBalancerZone.named( lb, zone );
        lbZone = Entities.uniqueResult( sample );
      }catch(final Exception ex) {
        throw new LoadBalancingException("No such availability zone is found in database");
      }
    }
	  List<LoadBalancerListener> lbListeners = Lists.newArrayList();
	  List<LoadBalancerBackendServerDescription> backendServers = Lists.newArrayList();
	  List<LoadBalancerPolicyDescription> lbPolicies = Lists.newArrayList();
	  try{
	   for(final LoadBalancerListenerCoreView listenerView: lb.getListeners()){
	     lbListeners.add(LoadBalancerListenerEntityTransform.INSTANCE.apply(listenerView));
	   }
	   for(final LoadBalancerBackendServerDescriptionCoreView backendView : lb.getBackendServers()) {
	     backendServers.add(LoadBalancerBackendServerDescriptionEntityTransform.INSTANCE.apply(backendView));
	   }
	 }catch(final Exception ex) {
	   throw new LoadBalancingException("Unexpected error while preparing loadbalancer description", ex);
	 }
	  return getServoDescription(lb, lbZone, lbListeners, backendServers, lbPolicies);
	}
	
	public static LoadBalancerServoDescription getServoDescription(
	    final LoadBalancer lb, 
	    final LoadBalancerZone zone, 
	    final List<LoadBalancerListener> lbListeners,
	    final List<LoadBalancerBackendServerDescription> backendServers, 
	    final List<LoadBalancerPolicyDescription> lbPolicies) {

		final String lbName = lb.getDisplayName();
		final LoadBalancerServoDescription desc = new LoadBalancerServoDescription();
		desc.setLoadBalancerName(lbName); /// loadbalancer name
		desc.setCreatedTime(lb.getCreationTimestamp());/// createdtime

		/// dns name
		desc.setDnsName(LoadBalancers.getLoadBalancerDnsName(lb));

		// attributes
		desc.setLoadBalancerAttributes(TypeMappers.transform(lb, LoadBalancerAttributes.class));

		/// backend instances in the same zone
		Collection<LoadBalancerBackendInstanceCoreView> backendInstancesInSameZone =
				Collections2.filter(zone.getBackendInstances(), new Predicate<LoadBalancerBackendInstanceCoreView>() {
					@Override
					public boolean apply(LoadBalancerBackendInstanceCoreView arg0) {
						return !LoadBalancerBackendInstance.STATE.Error.equals(arg0.getBackendState()) &&
								!(arg0.getIpAddress() == null || arg0.getIpAddress().length() <= 0);
					}
				});

		final boolean zoneHasAvailableInstance =
						backendInstancesInSameZone.stream().anyMatch(inst ->
										LoadBalancerBackendInstance.STATE.InService.equals(inst.getBackendState()) &&
														!(inst.getIpAddress() == null || inst.getIpAddress().length() <= 0)
						);

		// backend instances in cross-zone
		Collection<LoadBalancerBackendInstanceCoreView> crossZoneBackendInstances =
				Lists.newArrayList();
		if (!zoneHasAvailableInstance
						|| desc.getLoadBalancerAttributes().getCrossZoneLoadBalancing().getEnabled()) {
			// EUCA-13233: when a zone contains no available instance, cross-zone instances are always included
			crossZoneBackendInstances = Collections2.filter(lb.getBackendInstances(), new Predicate<LoadBalancerBackendInstanceCoreView>() {
				@Override
				public boolean apply(LoadBalancerBackendInstanceCoreView arg0) {
					// Instance's service state can only be determined in the same zone. Cross-zone instances are included only when InService
					final boolean inService = LoadBalancerBackendInstance.STATE.InService.equals(arg0.getBackendState()) &&
							!(arg0.getIpAddress() == null || arg0.getIpAddress().length() <= 0);
					return inService &&
							!zone.getName().equals(arg0.getPartition()); // different zone
				}
			});
		}

		if (!backendInstancesInSameZone.isEmpty()) {
			desc.setBackendInstances(new BackendInstances());
			desc.getBackendInstances().getMember().addAll(
					Collections2.transform(backendInstancesInSameZone, new Function<LoadBalancerBackendInstanceCoreView, BackendInstance>() {
						@Override
						public BackendInstance apply(final LoadBalancerBackendInstanceCoreView be) {
							final BackendInstance instance = new BackendInstance();
							instance.setInstanceId(be.getInstanceId());
							instance.setInstanceIpAddress(be.getIpAddress());
							instance.setReportHealthCheck(true);
							return instance;
						}
					}));
		}

		if (!crossZoneBackendInstances.isEmpty()) {
			if (desc.getBackendInstances() == null)
				desc.setBackendInstances(new BackendInstances());
			desc.getBackendInstances().getMember().addAll(
					Collections2.transform(crossZoneBackendInstances, new Function<LoadBalancerBackendInstanceCoreView, BackendInstance>() {
						@Override
						public BackendInstance apply(final LoadBalancerBackendInstanceCoreView be) {
							final BackendInstance instance = new BackendInstance();
							instance.setInstanceId(be.getInstanceId());
							instance.setInstanceIpAddress(be.getIpAddress());
							// if the servo's zone != backend instance's, it does not report health check
							// only the servo in the same zone will change the instance's state
							instance.setReportHealthCheck(false);
							return instance;
						}
					}));
		}

		/// availability zones
		desc.setAvailabilityZones(new AvailabilityZones());
		desc.getAvailabilityZones().getMember().add(zone.getName());

		final Set<String> policiesOfListener = Sets.newHashSet();
		final Set<String> policiesForBackendServer = Sets.newHashSet();

		/// listeners
		if (lbListeners.size() > 0) {
			desc.setListenerDescriptions(new ListenerDescriptions());
			desc.getListenerDescriptions().setMember(new ArrayList<>(
					Collections2.transform(lbListeners, new Function<LoadBalancerListener, ListenerDescription>() {
						@Override
						public ListenerDescription apply(final LoadBalancerListener lbListener) {
							ListenerDescription desc = new ListenerDescription();
							Listener listener = new Listener();
							listener.setLoadBalancerPort(lbListener.getLoadbalancerPort());
							listener.setInstancePort(lbListener.getInstancePort());
							if (lbListener.getInstanceProtocol() != PROTOCOL.NONE)
								listener.setInstanceProtocol(lbListener.getInstanceProtocol().name());
							listener.setProtocol(lbListener.getProtocol().name());
							if (lbListener.getCertificateId() != null)
								listener.setSSLCertificateId(lbListener.getCertificateId());
							desc.setListener(listener);
							final PolicyNames pnames = new PolicyNames();
							pnames.setMember(new ArrayList<>(Lists.transform(lbListener.getPolicies(), new Function<LoadBalancerPolicyDescriptionCoreView, String>() {
								@Override
								public String apply(
										LoadBalancerPolicyDescriptionCoreView arg0) {
									try {
										return arg0.getPolicyName(); // No other policy types are supported
									} catch (final Exception ex) {
										return ""; // No other policy types are supported
									}
								}
							})));
							policiesOfListener.addAll(pnames.getMember());
							desc.setPolicyNames(pnames);
							return desc;
						}
					})));
		}

		/// backend server descriptions
		try {
			if (backendServers.size() > 0) {
				desc.setBackendServerDescriptions(new BackendServerDescriptions());
				desc.getBackendServerDescriptions().setMember(new ArrayList<>(
						Collections2.transform(backendServers, new Function<LoadBalancerBackendServerDescription, BackendServerDescription>() {
							@Override
							public BackendServerDescription apply(
									LoadBalancerBackendServerDescription backend) {
								final BackendServerDescription desc = new BackendServerDescription();
								desc.setInstancePort(backend.getInstancePort());
								desc.setPolicyNames(new PolicyNames());
								desc.getPolicyNames().setMember(new ArrayList<>(
										Collections2.transform(backend.getPolicyDescriptions(), new Function<LoadBalancerPolicyDescriptionCoreView, String>() {
											@Override
											public String apply(
													LoadBalancerPolicyDescriptionCoreView arg0) {
												return arg0.getPolicyName();
											}
										})
								));
								policiesForBackendServer.addAll(desc.getPolicyNames().getMember());
								return desc;
							}
						})
				));
			}
		} catch (final Exception ex) {
			;
		}

		/// health check
		try {
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
		} catch (Exception ex) {
		}

		// policies (EUCA-specific)
		final List<PolicyDescription> policies = Lists.newArrayList();
		for (final LoadBalancerPolicyDescription lbPolicy : lbPolicies) {
			// for efficiency, add policies only if they are set for listeners
			// PublicKey policies should always be included bc it's referenced from BackendAuthenticationPolicyType
			if (policiesOfListener.contains(lbPolicy.getPolicyName())
					|| policiesForBackendServer.contains(lbPolicy.getPolicyName())
					|| "PublicKeyPolicyType".equals(lbPolicy.getPolicyTypeName()))
				policies.add(LoadBalancerPolicies.AsPolicyDescription.INSTANCE.apply(lbPolicy));
		}
		final PolicyDescriptions policyDescs = new PolicyDescriptions();
		policyDescs.setMember((ArrayList<PolicyDescription>) policies);
		desc.setPolicyDescriptions(policyDescs);

		return desc;
	}

	public static void checkWorkerCertificateExpiration(final LoadBalancer lb) throws LoadBalancingException{
		try{
			for (final LoadBalancerZoneCoreView lbZoneView : lb.getZones()) {
				final LoadBalancerZone lbZone = LoadBalancerZoneEntityTransform.INSTANCE.apply(lbZoneView);
				for(final LoadBalancerServoInstanceCoreView instance : lbZone.getServoInstances()) {
					if(LoadBalancerServoInstance.STATE.InService.equals(instance.getState()))  {
						boolean expired = false;
						try { // Upgrade case: add expiration date to instance's launch time
							if (instance.getCertificateExpirationDate() == null) {
								final List<RunningInstancesItemType> instances =
										EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(instance.getInstanceId()), true);
								final Date launchDate = instances.get(0).getLaunchTime();
								final Calendar cal = Calendar.getInstance();
								cal.setTime(launchDate);
								cal.add(Calendar.DATE, Integer.parseInt(LoadBalancingWorkerProperties.EXPIRATION_DAYS));

								try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
									final LoadBalancerServoInstance entity = Entities.uniqueResult(LoadBalancerServoInstance.named(instance.getInstanceId()));
									entity.setCertificateExpiration(cal.getTime());
									expired = entity.isCertificateExpired();
									Entities.persist(entity);
									db.commit();
								}
							}
						}catch(final Exception ex) {
							LOG.warn("Failed to update ELB worker's certificate expiration date", ex);
						}
						if (expired || instance.isCertificateExpired()) {
							throw new InternalFailureException(String.format("LoadBalancing worker(%s)'s certificate has expired. Contact Cloud Administrator.",
									instance.getInstanceId()));
						}
					}
				}
			}
		}catch(final LoadBalancingException ex) {
			throw ex;
		}catch(final Exception ex) {
			throw new LoadBalancingException("Error while checking loadbalancing worker's certificate expiration", ex);
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
