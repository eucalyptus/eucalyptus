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

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
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
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreViewTransform;
import com.eucalyptus.loadbalancing.common.backend.msgs.Listener;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomains;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
@SuppressWarnings("deprecation")
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling loadbalancing")
@Entity
@PersistenceContext( name = "eucalyptus_loadbalancing" )
@Table( name = "metadata_listener" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LoadBalancerListener extends AbstractPersistent
{
	private static Logger    LOG     = Logger.getLogger( LoadBalancerListener.class );
	
	public static class ELBPortRestrictionChangeListener implements PropertyChangeListener {
		   @Override
		   public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			    try {
			      if ( newValue instanceof String ) {
			    	  final Set<Integer> range = PortRangeMapper.apply((String)newValue);
			      }
			    } catch ( final Exception e ) {
					throw new ConfigurablePropertyException("Malformed port: value should be [port(, port)] or [port-port]");
			    }
		   }
	}
	
	private static Function<String, Set<Integer>> PortRangeMapper = new Function<String, Set<Integer>>(){

		@SuppressWarnings("deprecation")
		@Override
		@Nullable
		public Set<Integer> apply(@Nullable String input) {
			try{
				if(input.contains("-")){
					if(StringUtils.countMatches(input, "-") != 1)
						throw new Exception("malformed range");
					final String[] tokens = input.split("-");
					if(tokens.length!=2){
						throw new Exception("invalid range");
					}
					final int beginPort = Integer.parseInt(tokens[0]);
					final int endPort = Integer.parseInt(tokens[1]);
					if(beginPort < 1 || endPort > 65535 || beginPort > endPort)
						throw new Exception("invald range");
					return ContiguousSet.create(Range.closed(beginPort, endPort), DiscreteDomains.integers());
				}else if(input.contains(",")){
					final String[] tokens = input.split(",");
					if(tokens.length != StringUtils.countMatches(input, ",")+1)
						throw new Exception("malformed list");
						
					final Set<Integer> ports = Sets.newHashSet();
					for(final String token : tokens){
						final int portNum = Integer.parseInt(token);
						if(token.isEmpty()|| portNum < 1 || portNum > 65535)
							throw new Exception("invald port number");
						ports.add(portNum);
					}
					return ports;
				}else{
					final int portNum = Integer.parseInt(input);
					if(input.isEmpty()|| portNum < 1 || portNum > 65535)
						throw new Exception("invald port number");
					return Sets.newHashSet(portNum);
				}
			}catch(final Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	};
	
	private final static String DEFAULT_PORT_RESTRICTION = "22";
	@ConfigurableField( displayName = "loadbalancer_restricted_ports",
			description = "The ports restricted for use as a loadbalancer port. Format should be port(, port) or [port-port]",
			initial = DEFAULT_PORT_RESTRICTION,
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE,
			changeListener = ELBPortRestrictionChangeListener.class
			)
	public static String LOADBALANCER_RESTRICTED_PORTS = DEFAULT_PORT_RESTRICTION;
	
	public enum PROTOCOL{
		HTTP, HTTPS, TCP, SSL, NONE
	}
	
	@Transient
	private static final long serialVersionUID = 1L;
	
	@Transient
	private LoadBalancerListenerRelationView view = null;
	
	@PostLoad
	private void onLoad(){
		if(this.view==null)
			this.view = new LoadBalancerListenerRelationView(this);
	}
	
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
	
	public void setSSLCertificateId(final String certArn){
	  this.sslCertificateArn = certArn;
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
	
	@Column(name="unique_name", nullable=false, unique=true)
	private String uniqueName = null;
	
	
  @ManyToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE )
  @JoinTable( name = "metadata_policy_has_listeners", joinColumns = { @JoinColumn( name = "metadata_listener_fk" ) }, inverseJoinColumns = @JoinColumn( name = "metadata_policy_fk" ) )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<LoadBalancerPolicyDescription> policies = null;
	
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
	
	public void addPolicy(final LoadBalancerPolicyDescription policy){
	  if(this.policies==null){
	    this.policies = Lists.newArrayList();
	  }
	  if(!this.policies.contains(policy))
	    this.policies.add(policy);
	}
	
	public void removePolicy(final String policyName){
	  if(this.policies==null || policyName==null)
	    return;
	  LoadBalancerPolicyDescription toDelete = null;
	  for(final LoadBalancerPolicyDescription pol : this.policies){
	    if(policyName.equals(pol.getPolicyName()))
	      toDelete = pol;
	  }
	  if(toDelete!=null)
	    this.policies.remove(toDelete);
	}
	
	public void removePolicy(final LoadBalancerPolicyDescription policy){
	  if(this.policies==null || policy==null)
      return;
	  this.policies.remove(policy);
	}
	
	public void resetPolicies(){
	  if(this.policies == null)
	    return;
	  this.policies.clear();
	}
	
	public List<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
	  return this.view.getPolicies();
	}
	
	public static boolean protocolSupported(Listener listener){
		try{
			final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
			if(PROTOCOL.HTTP.equals(protocol) || PROTOCOL.TCP.equals(protocol) || PROTOCOL.HTTPS.equals(protocol) || PROTOCOL.SSL.equals(protocol))
				return true;
			else
				return false;
		}catch(Exception e){
			return false;
		}
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
	
	public static boolean validRange(Listener listener){
		try{
			if(! (listener.getInstancePort() > 0 &&
				listener.getLoadBalancerPort() > 0 &&
				!Strings.isNullOrEmpty(listener.getProtocol())))
				return false;
			
			int lbPort =listener.getLoadBalancerPort();
			int instancePort = listener.getInstancePort();
			
			if (! (lbPort >= 1 && lbPort <= 65535))
				return false;
			
			if (! (instancePort >= 1 && instancePort <= 65535))
				return false;
			
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	// port 22: used as sshd by servo instances
	public static boolean portAvailable(Listener listener){
		try{
			if(! (listener.getInstancePort() > 0 &&
				listener.getLoadBalancerPort() > 0 &&
				!Strings.isNullOrEmpty(listener.getProtocol())))
				return false;
			
			int lbPort =listener.getLoadBalancerPort();
			int instancePort = listener.getInstancePort();
			return ! PortRangeMapper.apply(LOADBALANCER_RESTRICTED_PORTS).contains(lbPort);
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
	
	public static class LoadBalancerListenerCoreView {
		private LoadBalancerListener listener = null;
		
		LoadBalancerListenerCoreView(LoadBalancerListener listener){
			this.listener = listener;
		}
		
		public int getInstancePort(){
			return this.listener.getInstancePort();
		}
		public PROTOCOL getInstanceProtocol(){
			return this.listener.getInstanceProtocol();
		}
		public int getLoadbalancerPort(){
			return this.listener.getLoadbalancerPort();
		}
		public PROTOCOL getProtocol(){
			return this.listener.getProtocol();
		}
		public String getCertificateId(){
			return this.listener.getCertificateId();
		}
	}
	
	@TypeMapper
	public enum LoadBalancerListenerCoreViewTransform implements Function<LoadBalancerListener, LoadBalancerListenerCoreView> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerListenerCoreView apply(
				@Nullable LoadBalancerListener arg0) {
			return new LoadBalancerListenerCoreView(arg0);
		}	
	}
	
	public enum LoadBalancerListenerEntityTransform implements Function<LoadBalancerListenerCoreView, LoadBalancerListener> {
		INSTANCE;

		@Override
		@Nullable
		public LoadBalancerListener apply(
				@Nullable LoadBalancerListenerCoreView arg0) {
			final EntityTransaction db = Entities.get(LoadBalancerListener.class);
			try{
				final LoadBalancerListener listener = Entities.uniqueResult(arg0.listener);
				db.commit();
				return listener;
			}catch(final Exception ex){
				db.rollback();
				throw Exceptions.toUndeclared(ex);
			}finally{
				if(db.isActive())
					db.rollback();
			}
			
		}
	}
	
	public static class LoadBalancerListenerRelationView {
		private LoadBalancerCoreView loadbalancer = null;
		private ImmutableList<LoadBalancerPolicyDescriptionCoreView> policies = null;
		private LoadBalancerListenerRelationView(final LoadBalancerListener listener){
			if(listener.loadbalancer!=null)
				this.loadbalancer = TypeMappers.transform(listener.loadbalancer, LoadBalancerCoreView.class);
			if(listener.policies!=null){
			   this.policies = ImmutableList.copyOf(Collections2.transform(listener.policies,
	            LoadBalancerPolicyDescriptionCoreViewTransform.INSTANCE)); 
			}
		}
		
		public LoadBalancerCoreView getLoadBalancer(){
			return this.loadbalancer;
		}
		
		public ImmutableList<LoadBalancerPolicyDescriptionCoreView> getPolicies(){
		  return this.policies;
		}
	}
}
