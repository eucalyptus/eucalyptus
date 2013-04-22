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
import java.util.Collection;
import java.util.NoSuchElementException;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.Parent;

import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * @author Sang-Min Park
 *
 */

@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_loadbalancer" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancer extends UserMetadata<LoadBalancer.STATE> implements LoadBalancerMetadata {
	private static Logger    LOG     = Logger.getLogger( LoadBalancer.class );

	@Transient
	private static final long serialVersionUID = 1L;
	
	public enum STATE {
		pending, available, failed
	} // TODO: SPARK: what's the state for loadbalancer?
	
	private LoadBalancer(){
		super(null, null);
	}
	
	private LoadBalancer(final String lbName){
		super(null, lbName);
	}
	
	private LoadBalancer(final OwnerFullName userFullName, final String lbName){
		super(userFullName, lbName);
	}
	
	static LoadBalancer newInstance(final OwnerFullName userFullName, final String lbName){
		final LoadBalancer instance= new LoadBalancer(userFullName, lbName);
		instance.setOwnerAccountName(userFullName.getAccountName());
		return instance;
	}
	
	static LoadBalancer named(final OwnerFullName userFullName, final String lbName){
		final LoadBalancer instance= new LoadBalancer(userFullName, lbName);
		instance.setOwnerAccountName(userFullName.getAccountName());
		return instance;
	}
	
	@Column( name = "loadbalancer_scheme", nullable=true)
	private String scheme = null; // only available for LoadBalancers attached to an Amazon VPC
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerBackendInstance> backendInstances = null;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
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
	
	public void setScheme(String scheme){
		this.scheme = scheme;
	}
	
	public String getScheme(){
		return this.scheme;
	}
	
	public LoadBalancerBackendInstance findBackendInstance(final String instanceId){
		if(this.backendInstances!=null){
			try{
				return Iterables.find(this.backendInstances, new Predicate<LoadBalancerBackendInstance>(){
					  @Override
					  public boolean apply(final LoadBalancerBackendInstance input){
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
	
	public Collection<LoadBalancerBackendInstance> getBackendInstances(){
		return this.backendInstances;
	}
	
	public LoadBalancerListener findListener(final int lbPort){
		if(this.listeners!=null){
			try{
				return Iterables.find(this.listeners, new Predicate<LoadBalancerListener>(){
					@Override
					public boolean apply(final LoadBalancerListener input){
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
	
	public void setDns(final LoadBalancerDnsRecord dns){
		this.dns = dns;
	}
	
	public LoadBalancerDnsRecord getDns(){
		return this.dns;
	}
	public Collection<LoadBalancerListener> getListeners(){
		return this.listeners;
	}
	
	public Collection<LoadBalancerZone> getZones(){
		return this.zones;
	}
	
	public LoadBalancerSecurityGroup getGroup(){
		return this.group;
	}
	
	public LoadBalancerAutoScalingGroup getAutoScaleGroup(){
		return this.autoscale_group;
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
}
