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
package com.eucalyptus.loadbalancing.activities;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.common.backend.msgs.Listener;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancers;
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
      ;
    }
	}

	public static class AuthorizeIngressRule extends AbstractEventHandler<CreateListenerEvent> {
		private CreateListenerEvent event = null;
		
		protected AuthorizeIngressRule(
				EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt) throws EventHandlerException {
			this.event = evt;
			final Collection<Listener> listeners = evt.getListeners();
			LoadBalancer lb = null;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
				final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
			}catch(Exception ex){
				throw new EventHandlerException("could not find the loadbalancer", ex);
			}
			if(groupName == null)
				throw new EventHandlerException("Group name is not found");
			
			for(Listener listener : listeners){
				int port = listener.getLoadBalancerPort();
				String protocol = "tcp"; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
				try{
					EucalyptusActivityTasks.getInstance().authorizeSecurityGroup(groupName, protocol, port);
				}catch(Exception ex){
					throw new EventHandlerException(String.format("failed to authorize %s, %s, %d", groupName, protocol, port), ex);
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
				;
			}
			if(groupName == null)
				return;
			
			for(Listener listener : listeners){
				int port = listener.getLoadBalancerPort();
				String protocol = listener.getProtocol();
				protocol = protocol.toLowerCase();
				
				try{
					EucalyptusActivityTasks.getInstance().revokeSecurityGroup(groupName, protocol, port);
				}catch(Exception ex){
					;
				}
			}
		}
	}	
	
	public static class AuthorizeSSLCertificate extends AbstractEventHandler<CreateListenerEvent> {
	  private String roleName = null;
	  private List<String> policyNames = Lists.newArrayList();
    protected AuthorizeSSLCertificate(
        EventHandlerChain<CreateListenerEvent> chain) {
      super(chain);
    }

    public static final String SERVER_CERT_ROLE_POLICY_NAME_PREFIX = "loadbalancer-iam-policy";
    public static final String ROLE_SERVER_CERT_POLICY_DOCUMENT=
        "{\"Statement\":[{\"Action\": [\"iam:DownloadServerCertificate\"],\"Effect\": \"Allow\",\"Resource\": \"CERT_ARN_PLACEHOLDER\"}]}";

    @Override
    public void apply(CreateListenerEvent evt) throws EventHandlerException {
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
          EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc);
          policyNames.add(policyName);
        }catch(final Exception ex){
          throw new EventHandlerException("failed to authorize server certificate for SSL listener", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if(roleName!=null && policyNames.size()>0){
        for(final String policyName : policyNames){
          try{
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName);
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
		private static final int DEFAULT_UNHEALTHY_THRESHOLD = 2;
		
		protected UpdateHealthCheckConfig(
				EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb = null;
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
				Listener firstListener = null;
				if(evt.getListeners()==null || evt.getListeners().size()<=0)
					throw new EventHandlerException("No listener requested");
				
				final List<Listener> listeners = Lists.newArrayList(evt.getListeners());
				firstListener = listeners.get(0);
				final String target = String.format("TCP:%d", firstListener.getInstancePort());
				final EntityTransaction db = Entities.get( LoadBalancer.class );
				try{
					final LoadBalancer update = Entities.uniqueResult(lb);
			    	update.setHealthCheck(DEFAULT_HEALTHY_THRESHOLD, DEFAULT_INTERVAL, target, DEFAULT_TIMEOUT, DEFAULT_UNHEALTHY_THRESHOLD);
					Entities.persist(update);
					db.commit();
				}catch(final NoSuchElementException exx){
					db.rollback();
					LOG.warn("Loadbalancer not found in the database");
				}catch(final Exception exx){
					db.rollback();
					LOG.warn("Unable to query the loadbalancer", ex);
				}finally {
					if(db.isActive())
						db.rollback();
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
}
