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
package com.eucalyptus.autoscaling.groups;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.MetricCollectionTypeMetadata;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.instances.AutoScalingInstanceCoreView;
import com.eucalyptus.autoscaling.instances.LifecycleState;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Iterables;

/**
 *
 */
public enum MetricCollectionType implements MetricCollectionTypeMetadata {

  GroupDesiredCapacity {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return (double) autoScalingGroup.getDesiredCapacity();
    }
  },

  GroupInServiceInstances {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return countInstancesInState( autoScalingInstances, LifecycleState.InService );
    }
  },

  GroupMaxSize {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return (double) autoScalingGroup.getMaxSize();
    }
  },

  GroupMinSize {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return (double) autoScalingGroup.getMinSize();
    }
  },

  GroupPendingInstances {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return countInstancesInState( autoScalingInstances, LifecycleState.Pending );
    }
  },

  GroupTerminatingInstances {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return countInstancesInState( autoScalingInstances, LifecycleState.Terminating );
    }
  },

  GroupTotalInstances {
    @Override
    public Double getValue( final AutoScalingGroupMinimumView autoScalingGroup,
                            final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances ) {
      return (double) Iterables.size( autoScalingInstances );
    }
  },

  ;

  @Override
  public String getDisplayName() {
    return name();
  }

  @Override
  public OwnerFullName getOwner() {
    return Principals.systemFullName();
  }

  public abstract Double getValue( AutoScalingGroupMinimumView autoScalingGroup,
                                   Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances );

  private static Double countInstancesInState( final Iterable<? extends AutoScalingInstanceCoreView> autoScalingInstances,
                                               final LifecycleState state ) {
    return (double) CollectionUtils.reduce( autoScalingInstances, 0, CollectionUtils.count( state.forView() ) );
  }
}
