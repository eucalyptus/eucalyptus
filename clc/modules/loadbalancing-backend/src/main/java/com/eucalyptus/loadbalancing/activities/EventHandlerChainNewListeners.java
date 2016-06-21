/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.activities;

import static com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup.generateDefaultVPCSecurityGroupName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.eucalyptus.compute.common.SecurityGroupItemType;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerListener;
import com.eucalyptus.loadbalancing.LoadBalancerPolicies;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainNewListeners extends EventHandlerChain<CreateListenerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainNewListeners.class );

	@Override
	public EventHandlerChain<CreateListenerEvent> build() {
	  this.insert(new CheckSSLCertificateId(this));
	  this.insert(new AuthorizeSSLCertificate(this));
		this.insert(new AuthorizeIngressRule(this));
		this.insert(new UpdateHealthCheckConfig(this));
		this.insert(new DefaultSSLPolicy(this));
		return this;
	}
	
	public static class CheckSSLCertificateId extends AbstractEventHandler<CreateListenerEvent> {

    protected CheckSSLCertificateId(EventHandlerChain<CreateListenerEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(CreateListenerEvent evt) throws EventHandlerException {
      final Collection<Listener> listeners = evt.getListeners();
      final String acctNumber = evt.getContext().getAccount().getAccountNumber();
      LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
      }catch(Exception ex){
        throw new EventHandlerException("could not find the loadbalancer", ex);
      }
      
      for(Listener listener : listeners){
        final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
        if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
          final String certArn = listener.getSSLCertificateId();
          if(certArn == null || certArn.length()<=0)
            throw new EventHandlerException("No SSLCertificateId is specified");
          //    ("arn:aws:iam::%s:server-certificate%s%s", this.owningAccount.getAccountNumber(), path, this.certName);
          final String prefix = String.format("arn:aws:iam::%s:server-certificate", acctNumber);
          if(!certArn.startsWith(prefix))
            throw new EventHandlerException("SSLCertificateId is not ARN format");
          try{
            final String pathAndName = certArn.replace(prefix, "");
            final String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
            
            final ServerCertificateType cert = EucalyptusActivityTasks.getInstance().getServerCertificate(evt.getContext().getUser().getUserId(), certName);
            if(cert==null)
              throw new EventHandlerException("No SSL certificate is found with the ARN"); 
            if(!certArn.equals(cert.getServerCertificateMetadata().getArn()))
              throw new EventHandlerException("Returned certificate's ARN doesn't match the request");
          }catch(final EventHandlerException ex){
            throw ex;
          }catch(final Exception ex){
              throw new EventHandlerException("Failed to get SSL server certificate", ex);
          }
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
		}
	}

	public static class AuthorizeIngressRule extends AbstractEventHandler<CreateListenerEvent> {
		private CreateListenerEvent event = null;
		
		protected AuthorizeIngressRule(EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt) throws EventHandlerException {
			this.event = evt;
			final Collection<Listener> listeners = evt.getListeners();
			LoadBalancer lb;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
				final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
			}catch(Exception ex){
				throw new EventHandlerException("could not find the loadbalancer", ex);
			}

         	final Map<String,String> securityGroupIdsToNames = lb.getCoreView( ).getSecurityGroupIdsToNames( );
			String protocol = "tcp"; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
			if ( lb.getVpcId( ) == null ) {
				if ( groupName == null )
					throw new EventHandlerException( "Group name is not found" );

				for ( Listener listener : listeners ) {
					int port = listener.getLoadBalancerPort();
					try {
					  EucalyptusActivityTasks.getInstance().authorizeSystemSecurityGroup( groupName, protocol, port, lb.useSystemAccount() );
					} catch ( Exception ex ) {
						throw new EventHandlerException( String.format( "failed to authorize %s, %s, %d", groupName, protocol, port ), ex );
					}
				}
			} else if ( securityGroupIdsToNames.size( ) == 1 ) {
				if ( securityGroupIdsToNames.values( ).contains( generateDefaultVPCSecurityGroupName( lb.getVpcId( ) ) ) ) {
                    boolean isRuleEmpty = false;
                    try {
                        final SecurityGroupItemType defaultGroup =
                                EucalyptusActivityTasks.getInstance()
                                        .describeUserSecurityGroupsByName( evt.getContext().getAccount(),
                                                lb.getVpcId( ), securityGroupIdsToNames.values().stream().findAny().get() )
                                        .stream().findAny().get();
                        if (defaultGroup.getIpPermissions() == null ||
                                defaultGroup.getIpPermissions().isEmpty()) {
                            isRuleEmpty = true;
                        }
                    }catch(final Exception ex) {
                        isRuleEmpty = false;
                    }

                    if (isRuleEmpty) { // the rule is created only for the first time the group is created
                        final String groupId = Iterables.getOnlyElement(securityGroupIdsToNames.keySet());
                        for (Listener listener : listeners) {
                            int port = listener.getLoadBalancerPort();
                            try {
                                EucalyptusActivityTasks.getInstance().authorizeSystemSecurityGroup(groupId, protocol, port, lb.useSystemAccount());
                            } catch (Exception ex) {
                                throw new EventHandlerException(String.format("failed to authorize %s, %s, %d", groupId, protocol, port), ex);
                            }
                        }
                    }
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.event == null)
				return;
			
			final Collection<Listener> listeners = this.event.getListeners();
			LoadBalancer lb = null;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(this.event.getContext(), this.event.getLoadBalancer());
				final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
			}catch(Exception ex){
			}
			if(groupName == null)
				return;
			
			for(Listener listener : listeners){
				int port = listener.getLoadBalancerPort();
				String protocol = listener.getProtocol();
				protocol = protocol.toLowerCase();
				
				try{
				  EucalyptusActivityTasks.getInstance().revokeSystemSecurityGroup( groupName, protocol, port, lb.useSystemAccount() );
       }catch(Exception ex){
				}
			}
		}
	}	
	
	public static class AuthorizeSSLCertificate extends AbstractEventHandler<CreateListenerEvent> {
	  private String roleName = null;
	  private List<String> policyNames = Lists.newArrayList();
	   private CreateListenerEvent event = null;
    protected AuthorizeSSLCertificate(
        EventHandlerChain<CreateListenerEvent> chain) {
      super(chain);
    }

    public static final String SERVER_CERT_ROLE_POLICY_NAME_PREFIX = "loadbalancer-iam-policy";
    public static final String ROLE_SERVER_CERT_POLICY_DOCUMENT=
        "{\"Statement\":[{\"Action\": [\"iam:DownloadServerCertificate\"],\"Effect\": \"Allow\",\"Resource\": \"CERT_ARN_PLACEHOLDER\"}]}";

    @Override
    public void apply(CreateListenerEvent evt) throws EventHandlerException {
      this.event = evt;
      final Collection<Listener> listeners = evt.getListeners();
      final Set<String> certArns = Sets.newHashSet();
      
      for(final Listener listener : listeners){
        final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
        if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
          certArns.add(listener.getSSLCertificateId());
        }
      }
      if(certArns.size() <= 0)
        return;
      
      LoadBalancer lb = null;
      try{
        lb = LoadBalancers.getLoadbalancer(event.getContext(), event.getLoadBalancer());
      }catch(Exception ex){
        throw new EventHandlerException("could not find the loadbalancer", ex);
      }
      
      roleName = String.format("%s-%s-%s", EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, 
          evt.getContext().getAccount().getAccountNumber(), evt.getLoadBalancer());
      final String prefix = 
          String.format("arn:aws:iam::%s:server-certificate", evt.getContext().getAccount().getAccountNumber());
    
      for (final String arn : certArns){
        if(!arn.startsWith(prefix))
          continue;
        String pathAndName = arn.replace(prefix, "");
        String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
        String policyName = String.format("%s-%s-%s-%s", SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
            evt.getContext().getAccount().getAccountNumber(), evt.getLoadBalancer(), certName);
        final String rolePolicyDoc = ROLE_SERVER_CERT_POLICY_DOCUMENT.replace("CERT_ARN_PLACEHOLDER", arn);
        try{
          EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc, lb.useSystemAccount());
          policyNames.add(policyName);
        }catch(final Exception ex){
          throw new EventHandlerException("failed to authorize server certificate for SSL listener", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if (this.event == null)
        return;

      if(roleName!=null && policyNames.size()>0){
        LoadBalancer lb = null;
        try{
          lb = LoadBalancers.getLoadbalancer(event.getContext(), event.getLoadBalancer());
        }catch(Exception ex){
          return;
        }

        for(final String policyName : policyNames){
          try{
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName, lb.useSystemAccount());
          }catch(final Exception ex){
            LOG.warn("Failed to delete role policy during listener creation rollback", ex);
          }
        }
      }
    }
	}

	static class UpdateHealthCheckConfig extends AbstractEventHandler<CreateListenerEvent> {
		private static final int DEFAULT_HEALTHY_THRESHOLD = 3;
		private static final int DEFAULT_INTERVAL = 30;
		private static final int DEFAULT_TIMEOUT = 5;
		private static final int DEFAULT_UNHEALTHY_THRESHOLD = 3;
		
		protected UpdateHealthCheckConfig(
				EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			/* default setting in AWS
			"HealthyThreshold": 10, 
			"Interval": 30, 
			"Target": "TCP:8000", 
			"Timeout": 5, 
			"UnhealthyThreshold": 2 */
			try{
				lb.getHealthCheckTarget();
				lb.getHealthCheckInterval();
				lb.getHealthCheckTimeout();
				lb.getHealthCheckUnhealthyThreshold();
				lb.getHealthyThreshold();
			}catch(final IllegalStateException ex){ /// only when the health check is not previously configured
				Listener firstListener;
				if(evt.getListeners()==null || evt.getListeners().size()<=0)
					throw new EventHandlerException("No listener requested");
				
				final List<Listener> listeners = Lists.newArrayList(evt.getListeners());
				firstListener = listeners.get(0);
				final String target = String.format( "TCP:%d", firstListener.getInstancePort() );
				try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
					final LoadBalancer update = Entities.uniqueResult(lb);
					update.setHealthCheck( DEFAULT_HEALTHY_THRESHOLD, DEFAULT_INTERVAL, target, DEFAULT_TIMEOUT, DEFAULT_UNHEALTHY_THRESHOLD );
					db.commit();
				}catch(final NoSuchElementException exx){
					LOG.warn("Loadbalancer not found in the database");
				}catch(final Exception exx){
					LOG.warn("Unable to query the loadbalancer", ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
		}
	}
	
	 public static class DefaultSSLPolicy extends AbstractEventHandler<CreateListenerEvent> {
    protected DefaultSSLPolicy(
        EventHandlerChain<? extends CreateListenerEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(CreateListenerEvent evt) throws EventHandlerException {
      LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
      }
      final List<Listener> listeners = Lists.newArrayList(evt.getListeners());
      
      boolean sslListener = false;
      for(final Listener l : listeners) {
        final String protocol = l.getProtocol().toLowerCase();
        if("https".equals(protocol) || "ssl".equals(protocol)) {
          sslListener = true;
          break;
        }
      }
      if(!sslListener)
        return;
      
      try{
        /// this will load the sample policies into memory
        if(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME == null) {
          LoadBalancerPolicies.getSamplePolicyDescription();
          if(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME == null)
            throw new Exception("Latest security policy is not found");
        }
        
        boolean policyCreated = false;
        final Collection<LoadBalancerPolicyDescriptionCoreView> policies = lb.getPolicies();
        if(policies != null) {
          for (final LoadBalancerPolicyDescriptionCoreView view : policies ) {
            if ("SSLNegotiationPolicyType".equals(view.getPolicyTypeName()) &&
                LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME.equals(view.getPolicyName())) {
              policyCreated = true;
              break;
            }
          }
        }
        if(! policyCreated) {
          final PolicyAttribute attr = new PolicyAttribute();
          attr.setAttributeName("Reference-Security-Policy");
          attr.setAttributeValue(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);
          LoadBalancerPolicies.addLoadBalancerPolicy(lb, LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME, "SSLNegotiationPolicyType", 
              Lists.newArrayList(attr));
          try{ // reload with the newly created policy
            lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
          }catch(NoSuchElementException ex){
            throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
          }catch(Exception ex){
            throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
          }
        }
      }catch (final Exception ex) {
        LOG.warn("Failed to create default security policy for https/ssl listeners", ex);
        return;
      }
      
      try{
        final LoadBalancerPolicyDescription policy = 
            LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb, LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);
        if(policy==null)
          throw new Exception("No such policy is found: "+LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);

        final Collection<LoadBalancerListenerCoreView> lbListeners = lb.getListeners();
        for(final Listener l : listeners) {
          final String protocol = l.getProtocol().toLowerCase();
          if("https".equals(protocol) || "ssl".equals(protocol)) {
            LoadBalancerListener listener = null;
            for(final LoadBalancerListenerCoreView view : lbListeners){
              if(view.getLoadbalancerPort() == l.getLoadBalancerPort()){
                listener = LoadBalancerListenerEntityTransform.INSTANCE.apply(view);
                break;
              }
            }
            if(listener == null)
              throw new Exception("No such listener is found");
            boolean policyAttached=false;
            final List<LoadBalancerPolicyDescriptionCoreView> listenerPolicies = listener.getPolicies();
            if(listenerPolicies!=null) {
              for(final LoadBalancerPolicyDescriptionCoreView listenerPolicy : listenerPolicies ) {
                if( "SSLNegotiationPolicyType".equals(listenerPolicy.getPolicyTypeName()) &&
                    LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME.equals(listenerPolicy.getPolicyName()))
                {
                  policyAttached = true;
                  break;
                }
              }
            }
            
            if(!policyAttached && listener!=null && policy!=null) {
              LoadBalancerPolicies.addPoliciesToListener(listener, Lists.newArrayList(policy));
            }
          }
        }
      }catch(final Exception ex) {
        LOG.warn("Failed to set default security policy to https/ssl listeners", ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
	 }
}
