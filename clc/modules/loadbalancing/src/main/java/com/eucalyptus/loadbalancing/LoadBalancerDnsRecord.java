package com.eucalyptus.loadbalancing;

import java.util.Collection;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.net.HostSpecifier;
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
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_dns" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerDnsRecord extends AbstractPersistent {

	public static class ELBDnsChangeListener implements PropertyChangeListener {
	   @Override
	   public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
		    try {
		      if ( newValue instanceof String ) {
				if(!HostSpecifier.isValid(String.format("%s.com", (String) newValue)))
					throw new ConfigurablePropertyException("Malformed domain name");
		      }
		    } catch ( final Exception e ) {
				throw new ConfigurablePropertyException("Malformed domain name");
		    }
		}
	}
	
	public static class ELBDnsTtlChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      try{
        final int ttl = Integer.parseInt((String)newValue);
      }catch(final Exception ex){
       throw new ConfigurablePropertyException("Malformed ttl value"); 
      }
    }
	}

	private static Logger    LOG     = Logger.getLogger( LoadBalancerDnsRecord.class );
	@ConfigurableField( displayName = "loadbalancer_dns_subdomain",
			description = "loadbalancer dns subdomain",
			initial = "lb",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ELBDnsChangeListener.class
			)
	public static String LOADBALANCER_DNS_SUBDOMAIN = "lb";
	
	@ConfigurableField( displayName = "loadbalancer_dns_ttl",
	      description = "loadbalancer dns ttl value",
	      initial = "60",
	      readonly = false,
	      type = ConfigurableFieldType.KEYVALUE,
	      changeListener = ELBDnsTtlChangeListener.class
	      )
	public static String LOADBALANCER_DNS_TTL = "60";
	public static int getLoadbalancerTTL(){
	  return Integer.parseInt(LOADBALANCER_DNS_TTL);
	}
	
	@Transient
	private static final long serialVersionUID = 1L;
	
	@Transient
	private LoadBalancerDnsRecordRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerDnsRecordRelationView(this);
	}

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
	
	public LoadBalancerCoreView getLoadbalancer(){
		return this.view.getLoadBalancer();
	}
	
	public String getName(){
		return String.format("%s.%s", this.dnsName, this.dnsZone);
	}
	
	public String getDnsName(){
		return String.format("%s.%s.%s", this.dnsName, this.dnsZone, 
				SystemConfiguration.getSystemConfiguration().getDnsDomain());
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
	

	public static class LoadBalancerDnsRecordCoreView {
		private LoadBalancerDnsRecord dns = null;
		LoadBalancerDnsRecordCoreView(final LoadBalancerDnsRecord dns){
			this.dns = dns;
		}
		
		public String getZone(){
			return this.dns.getZone();
		}
		
		public String getName(){
			return this.dns.getName();
		}
		
		public String getDnsName(){
			return this.dns.getDnsName();
		}
	}
	
	@TypeMapper
	public enum LoadBalancerDnsRecordCoreViewTransform implements Function<LoadBalancerDnsRecord, LoadBalancerDnsRecordCoreView>{
		INSTANCE;
		
		@Override
		@Nullable
		public LoadBalancerDnsRecordCoreView apply(
				@Nullable LoadBalancerDnsRecord arg0) {
			return new LoadBalancerDnsRecordCoreView(arg0);
		}
	}
	
	public enum LoadBalancerDnsRecordEntityTransform implements Function<LoadBalancerDnsRecordCoreView, LoadBalancerDnsRecord>{
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerDnsRecord apply(
				@Nullable LoadBalancerDnsRecordCoreView arg0) {
			final EntityTransaction db = Entities.get(LoadBalancerDnsRecord.class);
			try{
				final LoadBalancerDnsRecord dns = Entities.uniqueResult(arg0.dns);
				db.commit();
				return dns;
			}catch(final Exception ex){
				db.rollback();
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
		}
	}
	
	public static class LoadBalancerDnsRecordRelationView {
		private LoadBalancerDnsRecord dns = null;
		private LoadBalancerCoreView loadbalancer = null;
		LoadBalancerDnsRecordRelationView(LoadBalancerDnsRecord dns){
			this.dns = dns;
			if(dns.loadbalancer!=null)
				this.loadbalancer = TypeMappers.transform(dns.loadbalancer, LoadBalancerCoreView.class);
		}
		public LoadBalancerCoreView getLoadBalancer(){
			return this.loadbalancer;
		}
	}
}