/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.autoscaling.groups;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.MetricCollectionTypeMetadata;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.instances.AutoScalingInstanceCoreView;
import com.eucalyptus.autoscaling.instances.LifecycleState;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.auth.principal.OwnerFullName;
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
