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

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.base.Strings;

/**
 * @author Sang-Min Park
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_listener" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerListener extends AbstractPersistent
{
	private static Logger    LOG     = Logger.getLogger( LoadBalancerListener.class );
	public enum PROTOCOL{
		HTTP, HTTPS, TCP, SSL, NONE
	}
	@Transient
	private static final long serialVersionUID = 1L;
	private LoadBalancerListener(){}
	public static LoadBalancerListener named(final LoadBalancer lb, int lbPort){
		LoadBalancerListener newInstance = new LoadBalancerListener();
		newInstance.loadbalancer = lb;
		newInstance.loadbalancerPort = lbPort;
		newInstance.uniqueName = newInstance.createUniqueName();
		return newInstance;
	}
	
	private LoadBalancerListener(Builder builder){
		this.loadbalancer = builder.lb;
		this.instancePort = builder.instancePort;
		this.instanceProtocol = builder.instanceProtocol;
		this.loadbalancerPort = builder.loadbalancerPort;
		this.protocol = builder.protocol;
	    this.sslCertificateArn = builder.sslCertificateArn;
	}
	public static class Builder{
		public Builder(LoadBalancer lb, int instancePort, int loadbalancerPort, PROTOCOL protocol){
			this.lb = lb;
			this.instancePort = instancePort;
			this.loadbalancerPort = loadbalancerPort;
			this.protocol = protocol.name().toLowerCase();
		}
		public Builder instanceProtocol(PROTOCOL protocol){
			this.instanceProtocol= protocol.name().toLowerCase();
			return this;
		}
		public Builder withSSLCerntificate(String arn){
			this.sslCertificateArn = arn;
			return this;
		}
		public LoadBalancerListener build(){
			return new LoadBalancerListener(this);
		}
		private LoadBalancer lb = null;
		private Integer instancePort = null;
		private String instanceProtocol = null;
		private Integer loadbalancerPort = null;
		private String protocol = null;
		private String sslCertificateArn = null;
	}

    @ManyToOne
    @JoinColumn( name = "metadata_loadbalancer_fk" )
    private LoadBalancer loadbalancer = null;
	
	@Column(name="instance_port", nullable=false)
	private Integer instancePort = null;
	
	@Column(name="instance_protocol", nullable=true)
	private String instanceProtocol = null;
	
	@Column(name="loadbalancer_port", nullable=false)
	private Integer loadbalancerPort = null;
	
	@Column(name="protocol", nullable=false)
	private String protocol = null;

	@Column(name="certificate_id", nullable=true)
	private String sslCertificateArn = null;
	
	@Column(name="unique_name", nullable=false)
	private String uniqueName = null;
	
	public int getInstancePort(){
		return this.instancePort;
	}
	public PROTOCOL getInstanceProtocol(){
		if(this.instanceProtocol==null)
			return PROTOCOL.NONE;
		
		return PROTOCOL.valueOf(this.instanceProtocol.toUpperCase());
	}
	public int getLoadbalancerPort(){
		return this.loadbalancerPort;
	}
	public PROTOCOL getProtocol(){
		return PROTOCOL.valueOf(this.protocol.toUpperCase());
	}
	public String getCertificateId(){
		return this.sslCertificateArn;
	}
	
	public static boolean acceptable(Listener listener){
		try{
			if(! (listener.getInstancePort() > 0 &&
				listener.getLoadBalancerPort() > 0 &&
				!Strings.isNullOrEmpty(listener.getProtocol())))
				return false;

			PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
			if(!Strings.isNullOrEmpty(listener.getInstanceProtocol()))
				protocol = PROTOCOL.valueOf(listener.getInstanceProtocol().toUpperCase());
			return true;
		}catch(Exception e){
			return false;
		}
	}

    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("listener-%s-%s-%s", this.loadbalancer.getOwnerAccountNumber(), this.loadbalancer.getDisplayName(), this.loadbalancerPort);
    }
	@Override
	public String toString(){
		return String.format("Listener for %s: %nProtocol=%s, Port=%d, InstancePort=%d, InstanceProtocol=%s, CertId=%s", 
				this.loadbalancer.getDisplayName(), this.protocol, this.loadbalancerPort, this.instancePort, this.instanceProtocol, this.sslCertificateArn);
	}
}
