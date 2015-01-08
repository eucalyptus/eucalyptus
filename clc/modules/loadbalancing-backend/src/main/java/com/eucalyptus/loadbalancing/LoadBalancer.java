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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreViewTransform;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionDraining;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.CaseFormat;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_loadbalancer" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancer extends UserMetadata<LoadBalancer.STATE> implements LoadBalancerMetadata {

	public enum Scheme {
		Internal,
		InternetFacing;

		@Override
		public String toString() {
			return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_HYPHEN, name( ) );
		}

		public static Optional<Scheme> fromString( final String scheme ) {
			return Enums.getIfPresent( Scheme.class, CaseFormat.LOWER_HYPHEN.to( CaseFormat.UPPER_CAMEL, Strings.nullToEmpty( scheme ) ) );
		}
	}

	@Transient
	private transient LoadBalancerCoreView coreView = null;
	@Transient
	private transient LoadBalancerRelationView relationView = null;
	
	@PostLoad
	private void onLoad(){
		if(this.coreView==null)
			this.coreView = new LoadBalancerCoreView(this);
		if(this.relationView==null)
			this.relationView = new LoadBalancerRelationView(this);
	}
	
	@Transient
	private static final long serialVersionUID = 1L;
	
	public enum STATE {
		pending, available, failed
	} // TODO: SPARK: what's the state for loadbalancer?
	
	private LoadBalancer(){
		super(null, null);
	}
	
	private LoadBalancer(final OwnerFullName userFullName, final String lbName){
		super(userFullName, lbName);
	}
	
	static LoadBalancer newInstance(final OwnerFullName userFullName, final String lbName){
		final LoadBalancer instance= new LoadBalancer(userFullName, lbName);
		if(userFullName!=null)
			instance.setOwnerAccountNumber(userFullName.getAccountNumber());
		return instance;
	}
	
	static LoadBalancer named(){
		return new LoadBalancer();
	}
	
	public static LoadBalancer named(final OwnerFullName userFullName, final String lbName){
		final LoadBalancer instance= new LoadBalancer(null, lbName);
		if(userFullName!=null)
			instance.setOwnerAccountNumber(userFullName.getAccountNumber());
		return instance;
	}
	
	public static LoadBalancer ownedByAccount(final String accountNumber) {
	  final LoadBalancer instance = new LoadBalancer();
	  instance.setOwnerAccountNumber(accountNumber);
	  return instance;
	}
	
	public static LoadBalancer namedByAccountId(final String accountId, final String lbName){
		final LoadBalancer instance = new LoadBalancer(null, lbName);
		instance.setOwnerAccountNumber(accountId);
		return instance;
	}

	@Column( name = "loadbalancer_vpc_id", nullable=true, updatable = false )
	private String vpcId; // only available for LoadBalancers attached to an Amazon VPC

	@Column( name = "loadbalancer_scheme", nullable=true)
	@Enumerated( EnumType.STRING )
	private Scheme scheme; // only available for LoadBalancers attached to an Amazon VPC

	@Column( name = "loadbalancer_connection_idle_timeout" )
	private Integer connectionIdleTimeout;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerListener> listeners = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerZone> zones = null;
	
	@OneToOne(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private LoadBalancerSecurityGroup group = null;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private LoadBalancerAutoScalingGroup autoscale_group = null;
	
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private LoadBalancerDnsRecord dns = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	@Cache( usage= CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerPolicyDescription> policies = null;

	@ElementCollection
	@CollectionTable( name = "metadata_loadbalancer_security_groups" )
	@JoinColumn( name = "metadata_loadbalancer_id" )
	@OrderColumn( name = "metadata_security_group_ordinal")
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private List<LoadBalancerSecurityGroupRef> securityGroupRefs = Lists.newArrayList( );

	@ElementCollection
	@CollectionTable( name = "metadata_loadbalancer_tag" )
	@MapKeyColumn( name = "metadata_key" )
	@Column( name = "metadata_value" )
	@JoinColumn( name = "metadata_loadbalancer_id" )
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Map<String,String> tags;

	public String getVpcId( ) {
		return vpcId;
	}

	public void setVpcId( final String vpcId ) {
		this.vpcId = vpcId;
	}

	public void setScheme(Scheme scheme){
		this.scheme = scheme;
	}
	
	public Scheme getScheme(){
		return this.scheme;
	}

	public Integer getConnectionIdleTimeout( ) {
		return connectionIdleTimeout;
	}

	public void setConnectionIdleTimeout( final Integer connectionIdleTimeout ) {
		this.connectionIdleTimeout = connectionIdleTimeout;
	}

	public List<LoadBalancerSecurityGroupRef> getSecurityGroupRefs() {
    return securityGroupRefs;
  }

	public void setSecurityGroupRefs( final List<LoadBalancerSecurityGroupRef> securityGroupRefs ) {
		this.securityGroupRefs = securityGroupRefs;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags( final Map<String, String> tags ) {
    this.tags = tags;
  }

	public LoadBalancerBackendInstanceCoreView findBackendInstance(final String instanceId){
		return this.relationView.findBackendInstance(instanceId);
	}
	
	public boolean hasBackendInstance(final String instanceId){
		return this.relationView.hasBackendInstance(instanceId);
	}
	
	public Collection<LoadBalancerBackendInstanceCoreView> getBackendInstances(){
		return this.relationView.getBackendInstances();
	}
	
	public LoadBalancerListenerCoreView findListener(final int lbPort){
		return this.relationView.findListener(lbPort);
	}
	
	public boolean hasListener(final int lbPort){
		return this.relationView.hasListener(lbPort);
	}
	
	public void setDns(final LoadBalancerDnsRecord dns){
		this.dns = dns;
	}
	
	public LoadBalancerDnsRecordCoreView getDns(){
		return this.relationView.getDns();
	}
	public Collection<LoadBalancerListenerCoreView> getListeners(){
		return this.relationView.getListeners();
	}
	
	public Collection<LoadBalancerZoneCoreView> getZones(){
		return this.relationView.getZones();
	}
	
	public LoadBalancerSecurityGroupCoreView getGroup(){
		return this.relationView.getGroup();
	}
	
	public LoadBalancerAutoScalingGroupCoreView getAutoScaleGroup(){
		return this.relationView.getAutoScaleGroup();
	}
	
	public void addPolicyDescription(final LoadBalancerPolicyDescription desc){
	  if (this.policies ==null)
	    this.policies = Lists.newArrayList();
	  this.policies.remove(desc);
	  this.policies.add(desc);
	}
	
	public void removePolicyDescription(final LoadBalancerPolicyDescription desc){
	  if(this.policies ==null)
	    return;
	  this.policies.remove(desc);
	}

	public LoadBalancerCoreView getCoreView( ) {
		return coreView;
	}

	public Collection<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
	  return this.relationView.getPolicies();
	}
	
	public void setHealthCheck(int healthyThreshold, int interval, String target, int timeout, int unhealthyThreshold)
		throws IllegalArgumentException
	{
		// check the validity of the health check param
		if(healthyThreshold < 0)
			throw new IllegalArgumentException("healthyThreshold must be > 0");
		if(interval < 0)
			throw new IllegalArgumentException("interval must be > 0");
		if(timeout < 0)
			throw new IllegalArgumentException("timeout must be > 0");
		if(unhealthyThreshold < 0)
			throw new IllegalArgumentException("unhealthyThreshold must be > 0");
		
		if(!(target.startsWith("HTTP") || target.startsWith("HTTPS") || 
				target.startsWith("TCP") ||target.startsWith("SSL")))
			throw new IllegalArgumentException("target must starts with one of HTTP, HTTPS, TCP, SSL");
		
		if(target.startsWith("HTTP") || target.startsWith("HTTPS")){
			int idxPort = target.indexOf(":");
			int idxPath = target.indexOf("/");
			if(idxPort < 0 || idxPath <0 || (idxPath-idxPort) <= 1)
				throw new IllegalArgumentException("Port and Path must be specified for HTTP");
			String port = target.substring(idxPort+1, idxPath);
			try{
				int portNum = Integer.parseInt(port);
				if(!(portNum > 0 && portNum < 65536))
					throw new Exception("invalid port number");
			}catch(Exception ex){
				throw new IllegalArgumentException("Invalid target specified", ex);
			}
		}else if (target.startsWith("TCP") || target.startsWith("SSL")){
			String copy = target;
			copy = copy.replace("TCP:","").replace("SSL:", "");
			try{
				int portNum = Integer.parseInt(copy);
				if(!(portNum > 0 && portNum < 65536))
					throw new Exception("invalid port number");
			}catch(Exception ex){
				throw new IllegalArgumentException("Invalid target specified", ex);
			}
		}
	   
		this.healthConfig = new LoadBalancerHealthCheckConfig(healthyThreshold, interval, target, timeout,unhealthyThreshold);
		this.healthConfig.setLoadBalancer(this);
	}
	
	public boolean isHealthcheckConfigured(){
		if (this.healthConfig==null)
			return false;
		else
			return true;
	}
	
	public int getHealthyThreshold() {
		if(this.healthConfig!=null)
			return this.healthConfig.HealthyThreshold;
		else
			throw new IllegalStateException("health check is not configured");
	}
	
	public int getHealthCheckInterval(){
		if(this.healthConfig!=null)
			return this.healthConfig.Interval;
		else
			throw new IllegalStateException("health check is not configured");
	}
	public String getHealthCheckTarget(){
		if(this.healthConfig!=null)
			return this.healthConfig.Target;
		else
			throw new IllegalStateException("health check is not configured");
		
	}
	public int getHealthCheckTimeout(){
		if(this.healthConfig!=null)
			return this.healthConfig.Timeout;
		else
			throw new IllegalStateException("health check is not configured");
	}
	public int getHealthCheckUnhealthyThreshold(){
		if(this.healthConfig!=null)
			return this.healthConfig.UnhealthyThreshold;
		else
			throw new IllegalStateException("health check is not configured");
	}
	@Override 
	public String toString(){
		return String.format("loadbalancer %s",  this.displayName);
	} 
	
	@Embedded
	private LoadBalancerHealthCheckConfig healthConfig = null;
	
	@Embeddable
	private static class LoadBalancerHealthCheckConfig {
		@Parent
		private LoadBalancer loadBalancer = null;
		
		@Column( name = "loadbalancer_healthy_threshold" , nullable = true)
		private Integer HealthyThreshold = null; //Specifies the number of consecutive health probe successes required before moving the instance to the Healthy state.

		@Column( name = "loadbalancer_healthcheck_interval", nullable = true)
		private Integer Interval = null; //Specifies the approximate interval, in seconds, between health checks of an individual instance.
		
		@Column( name = "loadbalancer_healthcheck_target", nullable = true)
		private String Target = null; //Specifies the instance being checked. The protocol is either TCP, HTTP, HTTPS, or SSL. The range of valid ports is one (1) through 65535.
		
		@Column( name = "loadbalancer_healthcheck_timeout", nullable = true)
		private Integer Timeout = null; //Specifies the amount of time, in seconds, during which no response means a failed health probe.
		
		@Column( name = "loadbalancer_unhealthy_threshold", nullable = true)
		private Integer UnhealthyThreshold = null; //Specifies the number of consecutive health probe failures required before moving the instance to the 
		
		private LoadBalancerHealthCheckConfig(){}
		private LoadBalancerHealthCheckConfig(int healthyThreshold, int interval, String target, int timeout, int unhealthyThreshold){
			this.HealthyThreshold = new Integer(healthyThreshold);
			this.Interval = new Integer(interval);
			this.Target = target;
			this.Timeout = new Integer(timeout);
			this.UnhealthyThreshold = new Integer(unhealthyThreshold);
		}
		
		void setLoadBalancer(LoadBalancer balancer){
			this.loadBalancer=balancer;
		}
		LoadBalancer getLoadBalancer(){
			return this.loadBalancer;
		}
	}
	
	@Override
	public String getPartition( ) {
		return ComponentIds.lookup( Eucalyptus.class ).name( );
	}
	  
	@Override
	public FullName getFullName( ) {
		return FullName.create.vendor( "euca" )
				.region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
				.namespace( this.getOwnerAccountNumber( ) )
				.relativeId( "loadbalancer", this.getDisplayName( ) );
	} 
	
	public static class LoadBalancerRelationView {
		private LoadBalancer loadbalancer = null;
		private LoadBalancerSecurityGroupCoreView group = null;
		private LoadBalancerAutoScalingGroupCoreView autoscale_group = null;
		private LoadBalancerDnsRecordCoreView dns = null;

		private ImmutableList<LoadBalancerBackendInstanceCoreView> backendInstances = null;
		private ImmutableList<LoadBalancerListenerCoreView> listeners = null;
		private ImmutableList<LoadBalancerZoneCoreView> zones = null;
		private ImmutableList<LoadBalancerPolicyDescriptionCoreView> policies = null;
		LoadBalancerRelationView(final LoadBalancer lb){
			this.loadbalancer = lb;

			if(lb.group!=null)
				this.group = TypeMappers.transform(lb.group, LoadBalancerSecurityGroupCoreView.class);
			if(lb.autoscale_group!=null)
				this.autoscale_group = TypeMappers.transform(lb.autoscale_group, LoadBalancerAutoScalingGroupCoreView.class);
			if(lb.dns != null)
				this.dns = TypeMappers.transform(lb.dns,  LoadBalancerDnsRecordCoreView.class);
			if(lb.backendInstances!=null)
				this.backendInstances = ImmutableList.copyOf(Collections2.transform(lb.backendInstances, LoadBalancerBackendInstanceCoreViewTransform.INSTANCE));
			if(lb.listeners!=null)
				this.listeners = ImmutableList.copyOf(Collections2.transform(lb.listeners, LoadBalancerListenerCoreViewTransform.INSTANCE));
			if(lb.zones!=null)
				this.zones = ImmutableList.copyOf(Collections2.transform(lb.zones,  LoadBalancerZoneCoreViewTransform.INSTANCE));
			if(lb.policies!=null)
			  this.policies = ImmutableList.copyOf(Collections2.transform(lb.policies, LoadBalancerPolicyDescriptionCoreViewTransform.INSTANCE));
		}
		

		public LoadBalancerBackendInstanceCoreView findBackendInstance(final String instanceId){
			  if(this.backendInstances!=null){
				  try{
					  return Iterables.find(this.backendInstances, new Predicate<LoadBalancerBackendInstanceCoreView>(){
					  @Override
					  public boolean apply(final LoadBalancerBackendInstanceCoreView input){
						  return input.getInstanceId().contentEquals(instanceId);
				 	  }
				  	});
				  }catch(NoSuchElementException ex){
					  return null;
					  }
				  }
			  return null;

		}
		
		public boolean hasBackendInstance(final String instanceId){
			 return this.findBackendInstance(instanceId) != null;
		}
		
		public Collection<LoadBalancerBackendInstanceCoreView> getBackendInstances(){
			return this.backendInstances;
		}
		
		public LoadBalancerListenerCoreView findListener(final int lbPort){
			if(this.listeners!=null){
				try{
					return Iterables.find(this.listeners, new Predicate<LoadBalancerListenerCoreView>(){
				  @Override
				  	public boolean apply(final LoadBalancerListenerCoreView input){
					  return input.getLoadbalancerPort() == lbPort;
				  	}
				  });
				}catch(NoSuchElementException ex){
				 	return null;
				}
			}
			return null;
		}
		
		public boolean hasListener(final int lbPort){
			return this.findListener(lbPort)!=null;
		}
		
		public LoadBalancerDnsRecordCoreView getDns(){
			return this.dns;
		}
		public Collection<LoadBalancerListenerCoreView> getListeners(){
			return this.listeners;
		}
		
		public Collection<LoadBalancerZoneCoreView> getZones(){
			return this.zones;
		}
		
		public LoadBalancerSecurityGroupCoreView getGroup(){
			return this.group;
		}
		
		public LoadBalancerAutoScalingGroupCoreView getAutoScaleGroup(){
			return this.autoscale_group;
		}
		
		public Collection<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
		  return this.policies;
		}
	}
	
	public static class LoadBalancerCoreView {
		private LoadBalancer loadbalancer;
		private ImmutableMap<String,String> securityGroupIdsToNames;
		LoadBalancerCoreView(final LoadBalancer lb){
			this.loadbalancer = lb;
			this.securityGroupIdsToNames = ImmutableMap.copyOf( CollectionUtils.putAll(
					lb.getSecurityGroupRefs( ),
					Maps.<String,String>newLinkedHashMap( ),
					LoadBalancerSecurityGroupRef.groupId( ),
					LoadBalancerSecurityGroupRef.groupName( ) ) );
		}
		
		public String getDisplayName(){
			return this.loadbalancer.getDisplayName();
		}
		
		public String getOwnerUserId(){
			return this.loadbalancer.getOwnerUserId();
		}
		
		public String getOwnerUserName(){
			return this.loadbalancer.getOwnerUserName();
		}

		public String getOwnerAccountNumber() {
			return this.loadbalancer.getOwnerAccountNumber();
		}

		public Date getCreationTimestamp(){
			return this.loadbalancer.getCreationTimestamp();
		}

		public String getVpcId() {
			return this.loadbalancer.getVpcId(  );
		}

		public Scheme getScheme(){
			return this.loadbalancer.getScheme();
		}

		public Integer getConnectionIdleTimeout( ) {
			return this.loadbalancer.getConnectionIdleTimeout( );
		}

		public Map<String,String> getSecurityGroupIdsToNames( ) {
			return this.securityGroupIdsToNames;
		}

		public boolean isHealthcheckConfigured(){
			return this.loadbalancer.isHealthcheckConfigured();
		}
		
		public int getHealthyThreshold() {	
			return this.loadbalancer.getHealthyThreshold();
		}
		
		public int getHealthCheckInterval(){
			return this.loadbalancer.getHealthCheckInterval();
		}
		
		public String getHealthCheckTarget(){
			return this.loadbalancer.getHealthCheckTarget();
		}
		
		public int getHealthCheckTimeout(){
			return this.loadbalancer.getHealthCheckTimeout();
		}
		
		public int getHealthCheckUnhealthyThreshold(){
			return this.loadbalancer.getHealthCheckUnhealthyThreshold();
		}
		
	}

	@TypeMapper
	public enum LoadBalancerCoreViewToLoadBalancerAttributesTransform
			implements Function<LoadBalancer,LoadBalancerAttributes> {
		INSTANCE;

		@Nullable
		@Override
		public LoadBalancerAttributes apply( @Nullable final LoadBalancer loadBalancer ) {
			LoadBalancerAttributes attributes = null;
			if ( loadBalancer != null ) {
				attributes = new LoadBalancerAttributes( );

				final AccessLog accessLog = new AccessLog( );
				accessLog.setEnabled( false );
				attributes.setAccessLog( accessLog );

				final ConnectionDraining connectionDraining = new ConnectionDraining( );
				connectionDraining.setEnabled( false );
				attributes.setConnectionDraining( connectionDraining );

				final ConnectionSettings connectionSettings = new ConnectionSettings( );
				connectionSettings.setIdleTimeout(
						Objects.firstNonNull( loadBalancer.getConnectionIdleTimeout( ), 60 ) );
				attributes.setConnectionSettings( connectionSettings );

				final CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing( );
				crossZoneLoadBalancing.setEnabled( false );
				attributes.setCrossZoneLoadBalancing( crossZoneLoadBalancing );
			}
			return attributes;
		}
	}

	public enum LoadBalancerEntityTransform implements NonNullFunction<LoadBalancerCoreView, LoadBalancer> {
		INSTANCE;
		@Nonnull
		@Override
		public LoadBalancer apply( LoadBalancerCoreView arg0) {
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
				return Entities.uniqueResult(arg0.loadbalancer);
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
}
