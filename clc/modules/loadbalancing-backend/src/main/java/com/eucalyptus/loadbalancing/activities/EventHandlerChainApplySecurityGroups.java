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

import static com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator.getAutoScalingGroupName;
import java.util.NoSuchElementException;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResult;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.collect.Lists;

/**
 *
 */
public class EventHandlerChainApplySecurityGroups extends EventHandlerChain<ApplySecurityGroupsEvent> {

  @Override
  public EventHandlerChain<ApplySecurityGroupsEvent> build( ) {
    this.insert( new LoadBalancerASGroupCreator( this, EventHandlerChainNew.getCapacityPerZone( ) ) );
    this.insert( new LoadBalancerSecurityGroupUpdate( this ) );
    return this;
  }

  public static class LoadBalancerSecurityGroupUpdate extends AbstractEventHandler<ApplySecurityGroupsEvent> {

    protected LoadBalancerSecurityGroupUpdate( final EventHandlerChain<? extends ApplySecurityGroupsEvent> chain ) {
      super( chain );
    }

    @Override
    public void apply( final ApplySecurityGroupsEvent evt ) throws EventHandlerException {
      final LoadBalancer.LoadBalancerCoreView lb;
      try{
        lb = LoadBalancers.getLoadbalancer( evt.getContext(), evt.getLoadBalancer() ).getCoreView();
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Failed to find the loadbalancer "+evt.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Unable to access loadbalancer metadata", ex);
      }

      final String groupName = getAutoScalingGroupName( lb.getOwnerAccountNumber(), lb.getDisplayName() );

      final DescribeAutoScalingGroupsResponseType response =
          EucalyptusActivityTasks.getInstance().describeAutoScalingGroups( Lists.newArrayList( groupName ) );

      final DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          response.getDescribeAutoScalingGroupsResult();
      if ( describeAutoScalingGroupsResult != null ) {
        final AutoScalingGroupsType autoScalingGroupsType = describeAutoScalingGroupsResult.getAutoScalingGroups( );
        if ( autoScalingGroupsType != null &&
            autoScalingGroupsType.getMember( ) != null &&
            !autoScalingGroupsType.getMember( ).isEmpty( ) &&
            autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ) != null ) {
          for ( final Instance instance : autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ).getMember( ) ) {
            EucalyptusActivityTasks.getInstance( ).modifySecurityGroups(
                instance.getInstanceId( ),
                evt.getSecurityGroupIdsToNames( ).keySet( ) );
          }
        }
      }
    }

    @Override
    public void rollback( ) throws EventHandlerException {
    }
  }
}
