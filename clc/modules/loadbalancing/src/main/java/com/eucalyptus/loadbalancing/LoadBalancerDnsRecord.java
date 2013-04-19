package com.eucalyptus.loadbalancing;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.util.Exceptions;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

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

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling loadbalancing")
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_dns" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerDnsRecord extends AbstractPersistent {
	private static Logger    LOG     = Logger.getLogger( LoadBalancerDnsRecord.class );
	@ConfigurableField( displayName = "loadbalancer_dns_subdomain",
			description = "loadbalancer dns subdomain",
			initial = "lb",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE
			)
	
	public static String LOADBALANCER_DNS_SUBDOMAIN = "lb";
	
	@Transient
	private static final long serialVersionUID = 1L;

	private LoadBalancerDnsRecord(){}
	
	private LoadBalancerDnsRecord(final LoadBalancer lb){
		this.loadbalancer = lb;
	}
	
	public static LoadBalancerDnsRecord named(){
		return new LoadBalancerDnsRecord(); // query all
	}

	public static LoadBalancerDnsRecord named(final LoadBalancer lb){
		final LoadBalancerDnsRecord instance = new LoadBalancerDnsRecord(lb);
		
		String dnsPrefix = String.format("%s-%s", lb.getDisplayName(), lb.getOwnerAccountNumber());
		dnsPrefix = dnsPrefix.replace(".", "_");
		
		final int maxPrefixLength = 253 - 
				String.format(".%s.%s", LOADBALANCER_DNS_SUBDOMAIN, 
						SystemConfiguration.getSystemConfiguration().getDnsDomain()).length();
		if(maxPrefixLength < 0 )
			throw Exceptions.toUndeclared("invalid dns name length");
		if(dnsPrefix.length() > maxPrefixLength)
			dnsPrefix = dnsPrefix.substring(0, maxPrefixLength);
				
		instance.dnsName = dnsPrefix;
		instance.dnsZone = LOADBALANCER_DNS_SUBDOMAIN;
		instance.uniqueName = instance.createUniqueName();
		return instance;
	}
	
    @OneToOne
    @JoinColumn( name = "metadata_loadbalancer_fk", nullable=false)
    private LoadBalancer loadbalancer = null;

	@Column(name="dns_name", nullable=false, unique=true)
    private String dnsName = null;
	
	@Column(name="dns_zone", nullable=false)
	private String dnsZone = null;

	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;

    
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "dns")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<LoadBalancerServoInstance> servoInstances = null;

	
	public String getZone(){
		return this.dnsZone;
	}
	
	public void setLoadbalancer(final LoadBalancer lb){
		this.loadbalancer = lb;
	}
	
	public String getName(){
		return String.format("%s.%s", this.dnsName, this.dnsZone);
	}
	
	public String getDnsName(){
		return String.format("%s.%s.%s", this.dnsName, this.dnsZone, 
				SystemConfiguration.getSystemConfiguration().getDnsDomain());
	}
	
	public Collection<LoadBalancerServoInstance> getServoInstances(){
		return this.servoInstances;
	}
	
	public LoadBalancer getLoadBalancer(){
		return this.loadbalancer;
	}


    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("dns-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.getDnsName());
    }
		
	@Override
	public String toString(){
		String name = "unassigned";
		if(this.dnsName!=null && this.dnsZone!=null)
			name = String.format("Loadbalancer DNS record - %s",getDnsName());
		return name;
	}
}
