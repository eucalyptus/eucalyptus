/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResult;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemType;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

/**
 *
 */
public class EventHandlerChainApplySecurityGroups extends EventHandlerChain<ApplySecurityGroupsEvent> {
  private static Logger LOG  = Logger.getLogger( EventHandlerChainApplySecurityGroups.class );

  @Override
  public EventHandlerChain<ApplySecurityGroupsEvent> build( ) {
    this.insert( new LoadBalancerASGroupCreator( this ){
      @Override
      public void rollback() throws EventHandlerException {
        // do not rollback on failures, leave any existing autoscaling resources in place
      }
    } );
    this.insert( new LoadBalancerSecurityGroupUpdate( this ) );
    return this;
  }

  public static class LoadBalancerSecurityGroupUpdate extends AbstractEventHandler<ApplySecurityGroupsEvent> {

    protected LoadBalancerSecurityGroupUpdate( final EventHandlerChain<? extends ApplySecurityGroupsEvent> chain ) {
      super( chain );
    }

    @Override
    public void checkVersion(ApplySecurityGroupsEvent evt) throws EventHandlerException {
      LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
      }

      if(!LoadBalancers.v4_3_0.apply(lb) && lb.getVpcId()!= null)
        throw new LoadBalancerVersionException(LoadBalancers.DeploymentVersion.v4_3_0);
    }

    @Override
    public void apply( final ApplySecurityGroupsEvent evt ) throws EventHandlerException {
      final LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer( evt.getContext(), evt.getLoadBalancer() );
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Failed to find the loadbalancer "+evt.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Unable to access loadbalancer metadata", ex);
      }

      for(final LoadBalancerAutoScalingGroupCoreView group : lb.getAutoScaleGroups()) {
        final String groupName = group.getName();
        final DescribeAutoScalingGroupsResponseType response = 
            EucalyptusActivityTasks.getInstance().describeAutoScalingGroups( Lists.newArrayList( groupName ), lb.useSystemAccount() );
        final DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
            response.getDescribeAutoScalingGroupsResult();
        if ( describeAutoScalingGroupsResult != null ) {
          final AutoScalingGroupsType autoScalingGroupsType = describeAutoScalingGroupsResult.getAutoScalingGroups( );
          if ( autoScalingGroupsType != null &&
              autoScalingGroupsType.getMember( ) != null &&
              !autoScalingGroupsType.getMember( ).isEmpty( ) &&
              autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ) != null ) {
            for ( final Instance instance : autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ).getMember( ) ) {
              final String userVpcEni = lookupSecondaryNetworkInterface(instance.getInstanceId());
              if (userVpcEni == null) {
                throw new EventHandlerException("Failed to lookup user VPC network interface");
              }
              try {
                final List<String> sgroupIds = Lists.newArrayList(evt.getSecurityGroupIdsToNames().keySet());
                EucalyptusActivityTasks.getInstance().modifyNetworkInterfaceSecurityGroups(userVpcEni, sgroupIds);
              }catch(final Exception ex) {
                throw new EventHandlerException("Failed to set security groups to network interface", ex);
              }
            }
          }
        }
      }
    }

    private String lookupSecondaryNetworkInterface(final String instanceId) {
      try{
        final Optional<InstanceNetworkInterfaceSetItemType> optEni =
                LoadBalancingSystemVpcs.getUserVpcInterface(instanceId);
        if(optEni.isPresent()) {
          return optEni.get().getNetworkInterfaceId();
        }
        return null;
      }catch(final Exception ex) {
        LOG.error("Failed to lookup secondary network interface for instance: " + instanceId);
        return null;
      }
    }

    @Override
    public void rollback( ) throws EventHandlerException {
    }
  }
}
