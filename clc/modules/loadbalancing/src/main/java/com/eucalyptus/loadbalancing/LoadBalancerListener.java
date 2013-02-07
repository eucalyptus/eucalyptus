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
		HTTP, HTTPS, TCP, SSL
	}
	@Transient
	private static final long serialVersionUID = 1L;
	private LoadBalancerListener(){}
	public static LoadBalancerListener named(final LoadBalancer lb, int lbPort){
		LoadBalancerListener newInstance = new LoadBalancerListener();
		newInstance.loadbalancer = lb;
		newInstance.loadbalancerPort = lbPort;
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
	
    @Column(name="metadata_unique_name", nullable=false, unique=true)
    private String uniqueName = null;
    
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
	
	public int getInstancePort(){
		return this.instancePort;
	}
	public PROTOCOL getInstanceProtocol(){
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
			PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol());
			if(!Strings.isNullOrEmpty(listener.getInstanceProtocol()))
				protocol = PROTOCOL.valueOf(listener.getInstanceProtocol());
			return true;
		}catch(Exception e){
			return false;
		}
	}

	@PrePersist
	private void generateOnCommit( ) {
		this.uniqueName = createUniqueName( );
	}

	private String createUniqueName(){
		return String.format("%s-listener-port-%d", this.loadbalancer.getDisplayName(), this.getLoadbalancerPort());
	}
	
	@Override
	public int hashCode( ) {
	    final int prime = 31;
	    int result = 0;
	    result = prime * result + ( ( this.uniqueName == null )
	      ? 0
	      : this.uniqueName.hashCode( ) );
	    return result;
	}
	  
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass( ) != obj.getClass( ) ) {
			return false;
		}
		LoadBalancerListener other = ( LoadBalancerListener ) obj;
		if ( this.uniqueName == null ) {
			if ( other.uniqueName != null ) {
				return false;
			}
		} else if ( !this.uniqueName.equals( other.uniqueName ) ) {
			return false;
		}
		return true;
  }
}
