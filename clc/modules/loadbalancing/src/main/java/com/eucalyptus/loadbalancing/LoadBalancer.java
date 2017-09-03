/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import static com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import static com.eucalyptus.util.Strings.isPrefixOf;
import static com.eucalyptus.util.Strings.trimPrefix;

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

import org.hibernate.annotations.Parent;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreViewTransform;
import com.eucalyptus.loadbalancing.LoadBalancers.DeploymentVersion;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreViewTransform;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionDraining;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.auth.principal.OwnerFullName;
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
	
	@Column( name = "loadbalancer_cross_zone_loadbalancing")
	private Boolean crossZoneLoadbalancing;
	
	@Column( name = "loadbalancer_accesslog_enabled", nullable=true)
	private Boolean accessLogEnabled;

  @Column( name = "loadbalancer_accesslog_emit_interval", nullable=true)
  private Integer accessLogEmitInterval;
  
	@Column( name ="loadbalancer_accesslog_s3bucket_name", nullable=true)
	private String accessLogS3BucketName;
	
	@Column( name = "loadbalancer_accesslog_s3bucket_prefix", nullable=true)
	private String accessLogS3BucketPrefix;
	
	@Column( name = "loadbalancer_deployment_version", nullable=true)
	private String loadbalancerDeploymentVersion;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
	private Collection<LoadBalancerListener> listeners = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	private Collection<LoadBalancerZone> zones = null;
	
	@OneToOne(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "loadbalancer")
	private LoadBalancerSecurityGroup group = null;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	private Collection<LoadBalancerAutoScalingGroup> autoscale_groups = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
	private Collection<LoadBalancerPolicyDescription> policies = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
  private Collection<LoadBalancerBackendServerDescription> backendServers = null;
	
	@ElementCollection
	@CollectionTable( name = "metadata_loadbalancer_security_groups" )
	@OrderColumn( name = "metadata_security_group_ordinal")
	private List<LoadBalancerSecurityGroupRef> securityGroupRefs = Lists.newArrayList( );

	@ElementCollection
	@CollectionTable( name = "metadata_loadbalancer_tag" )
	@MapKeyColumn( name = "metadata_key" )
	@Column( name = "metadata_value" )
	@JoinColumn( name = "metadata_loadbalancer_id" )
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
	
	public Boolean getCrossZoneLoadbalancingEnabled() {
	  return this.crossZoneLoadbalancing;
	}
	
	public void setCrossZoneLoadbalancingEnabled(final boolean enabled) {
	  this.crossZoneLoadbalancing = enabled;
	}
	
	public Boolean getAccessLogEnabled() {
	  return this.accessLogEnabled;
	}
	
	public void setAccessLogEnabled(final boolean enabled){
	  this.accessLogEnabled = enabled;
	}
	
	public Integer getAccessLogEmitInterval(){
	  return this.accessLogEmitInterval;
	}
	
	public void setAccessLogEmitInterval(final Integer interval){
	  this.accessLogEmitInterval = interval;
	}
	
	public String getAccessLogS3BucketName(){
	  return this.accessLogS3BucketName;
	}
	
	public void setAccessLogS3BucketName(final String bucketName){
	  this.accessLogS3BucketName = bucketName;
	}
	
	public String getAccessLogS3BucketPrefix(){
	  return this.accessLogS3BucketPrefix;
	}
	
	public void setLoadbalancerDeploymentVersion(final String version){
	  this.loadbalancerDeploymentVersion = version;
	}
	
	public String getLoadbalancerDeploymentVersion(){
	  return this.loadbalancerDeploymentVersion;
	}
	
	public void setAccessLogS3BucketPrefix(final String bucketPrefix){
	  this.accessLogS3BucketPrefix = bucketPrefix;
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
	
	public Collection<LoadBalancerListenerCoreView> getListeners(){
		return this.relationView.getListeners();
	}
	
	public Collection<LoadBalancerZoneCoreView> getZones(){
		return this.relationView.getZones();
	}
	
	public LoadBalancerSecurityGroupCoreView getGroup(){
		return this.relationView.getGroup();
	}
	
	public Collection<LoadBalancerAutoScalingGroupCoreView> getAutoScaleGroups(){
		return this.relationView.getAutoScaleGroups();
	}
	
	public boolean useSystemAccount(){
    return this.getLoadbalancerDeploymentVersion() != null &&
        DeploymentVersion.getVersion(this.getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(DeploymentVersion.v4_2_0);
	}

	public LoadBalancerCoreView getCoreView( ) {
		return coreView;
	}

	public Collection<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
	  return this.relationView.getPolicies();
	}
	
	public Collection<LoadBalancerBackendServerDescriptionCoreView> getBackendServers(){
	  return this.relationView.getBackendServers();
	}
	
	public void setHealthCheck(int healthyThreshold, int interval, String target, int timeout, int unhealthyThreshold)
		throws IllegalArgumentException
	{
		final String[] targetParts = target.split( ":", 2 );
		final String canonicalizedTarget = targetParts.length == 2 ?
				targetParts[0].toUpperCase( ) + ":" + targetParts[1] :
				target;
		// check the validity of the health check param
		if(healthyThreshold < 0)
			throw new IllegalArgumentException("healthyThreshold must be > 0");
		if(interval < 0)
			throw new IllegalArgumentException("interval must be > 0");
		if(timeout < 0)
			throw new IllegalArgumentException("timeout must be > 0");
		if(unhealthyThreshold < 0)
			throw new IllegalArgumentException("unhealthyThreshold must be > 0");
		
		if( !Iterables.any( Lists.newArrayList( "HTTP", "HTTPS",  "TCP", "SSL"), isPrefixOf( canonicalizedTarget ) ) )
			throw new IllegalArgumentException("target must starts with one of HTTP, HTTPS, TCP, SSL");
		
		if(canonicalizedTarget.startsWith("HTTP") || canonicalizedTarget.startsWith("HTTPS")){
			int idxPort = canonicalizedTarget.indexOf(":");
			int idxPath = canonicalizedTarget.indexOf("/");
			if(idxPort < 0 || idxPath <0 || (idxPath-idxPort) <= 1)
				throw new IllegalArgumentException("Port and Path must be specified for HTTP");
			String port = canonicalizedTarget.substring(idxPort+1, idxPath);
			try{
				int portNum = Integer.parseInt(port);
				if(!(portNum > 0 && portNum < 65536))
					throw new Exception("invalid port number");
			}catch(Exception ex){
				throw new IllegalArgumentException("Invalid target specified", ex);
			}
		}else if (canonicalizedTarget.startsWith("TCP") || canonicalizedTarget.startsWith("SSL")){
			String portStr = trimPrefix( ":", canonicalizedTarget.substring( 3 ) );
			try{
				int portNum = Integer.parseInt( portStr );
				if(!(portNum > 0 && portNum < 65536))
					throw new Exception("invalid port number");
			}catch(Exception ex){
				throw new IllegalArgumentException("Invalid target specified", ex);
			}
		}
	   
		this.healthConfig = new LoadBalancerHealthCheckConfig(healthyThreshold, interval, canonicalizedTarget, timeout,unhealthyThreshold);
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
	public static class LoadBalancerHealthCheckConfig {
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
		private ImmutableList<LoadBalancerAutoScalingGroupCoreView> autoscale_groups = null;
		private ImmutableList<LoadBalancerBackendInstanceCoreView> backendInstances = null;
		private ImmutableList<LoadBalancerListenerCoreView> listeners = null;
		private ImmutableList<LoadBalancerZoneCoreView> zones = null;
		private ImmutableList<LoadBalancerPolicyDescriptionCoreView> policies = null;
		private ImmutableList<LoadBalancerBackendServerDescriptionCoreView> backendServers = null;
		LoadBalancerRelationView(final LoadBalancer lb){
			this.loadbalancer = lb;

			if(lb.group!=null)
				this.group = TypeMappers.transform(lb.group, LoadBalancerSecurityGroupCoreView.class);
			if(lb.autoscale_groups!=null)
				this.autoscale_groups = ImmutableList.copyOf(Collections2.transform(lb.autoscale_groups, LoadBalancerAutoScalingGroupCoreViewTransform.INSTANCE));
			if(lb.backendInstances!=null)
				this.backendInstances = ImmutableList.copyOf(Collections2.transform(lb.backendInstances, LoadBalancerBackendInstanceCoreViewTransform.INSTANCE));
			if(lb.listeners!=null)
				this.listeners = ImmutableList.copyOf(Collections2.transform(lb.listeners, LoadBalancerListenerCoreViewTransform.INSTANCE));
			if(lb.zones!=null)
				this.zones = ImmutableList.copyOf(Collections2.transform(lb.zones,  LoadBalancerZoneCoreViewTransform.INSTANCE));
			if(lb.policies!=null)
			  this.policies = ImmutableList.copyOf(Collections2.transform(lb.policies, LoadBalancerPolicyDescriptionCoreViewTransform.INSTANCE));
			if(lb.backendServers!=null)
			  this.backendServers =  ImmutableList.copyOf(Collections2.transform(lb.backendServers, LoadBalancerBackendServerDescriptionCoreViewTransform.INSTANCE));
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

		public Collection<LoadBalancerListenerCoreView> getListeners(){
			return this.listeners;
		}
		
		public Collection<LoadBalancerZoneCoreView> getZones(){
			return this.zones;
		}
		
		public LoadBalancerSecurityGroupCoreView getGroup(){
			return this.group;
		}
		
		public Collection<LoadBalancerAutoScalingGroupCoreView> getAutoScaleGroups(){
			return this.autoscale_groups;
		}
		
		public Collection<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
		  return this.policies;
		}
		
		public Collection<LoadBalancerBackendServerDescriptionCoreView> getBackendServers(){
		  return this.backendServers;
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
		
		public Boolean getCrossZoneLoadbalancingEnabled( ) {
		  return this.loadbalancer.getCrossZoneLoadbalancingEnabled();
		}
		
		public Boolean getAccessLogEnabled( ) {
		  return this.loadbalancer.getAccessLogEnabled();
		}
		
		public String getAccessLogS3BucketName( ) {
		  return this.loadbalancer.getAccessLogS3BucketName();
		}
		
		public String getAccessLogS3BucketPrefix( ) {
		  return this.loadbalancer.getAccessLogS3BucketPrefix();
		}
		
		public Integer getAccessLogEmitInterval( ) {
		  return this.loadbalancer.getAccessLogEmitInterval();
		}
		
		public String getLoadbalancerDeploymentVersion() {
		  return this.loadbalancer.getLoadbalancerDeploymentVersion();
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
		
		public boolean useSystemAccount(){
		  return this.loadbalancer.useSystemAccount();
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

					final ConnectionDraining connectionDraining = new ConnectionDraining( );
				connectionDraining.setEnabled( false );
				attributes.setConnectionDraining( connectionDraining );

				final ConnectionSettings connectionSettings = new ConnectionSettings( );
				connectionSettings.setIdleTimeout(
						Objects.firstNonNull( loadBalancer.getConnectionIdleTimeout( ), 60 ) );
				attributes.setConnectionSettings( connectionSettings );

				final CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing( );
				crossZoneLoadBalancing.setEnabled( 
				    Objects.firstNonNull(loadBalancer.getCrossZoneLoadbalancingEnabled(), false) );
				attributes.setCrossZoneLoadBalancing( crossZoneLoadBalancing );
				
				final AccessLog accessLog = new AccessLog();
				accessLog.setEnabled(Objects.firstNonNull(loadBalancer.getAccessLogEnabled(), false));
				accessLog.setEmitInterval(loadBalancer.getAccessLogEmitInterval());
				accessLog.setS3BucketName(loadBalancer.getAccessLogS3BucketName());
				accessLog.setS3BucketPrefix(loadBalancer.getAccessLogS3BucketPrefix());
				attributes.setAccessLog( accessLog );
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
