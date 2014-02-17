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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingProcessTypeMetadata;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public enum ScalingProcessType implements Predicate<AutoScalingGroup>, ScalingProcessTypeMetadata {

  AZRebalance,
  AddToLoadBalancer,
  AlarmNotification,
  HealthCheck,
  Launch,
  ReplaceUnhealthy,
  ScheduledActions,
  Terminate,
  ;

  @Override
  public boolean apply( @Nullable final AutoScalingGroup group ) {
    return group == null || isEnabled( this, group.getSuspendedProcesses() );
  }

  public Predicate<AutoScalingGroupCoreView> forView() {
    return new Predicate<AutoScalingGroupCoreView>() {
      @Override
      public boolean apply( final AutoScalingGroupCoreView group ) {
        return group == null || isEnabled( ScalingProcessType.this, group.getSuspendedProcesses() );
      }
    };
  }

  @Override
  public String getDisplayName() {
    return name();
  }

  @Override
  public OwnerFullName getOwner() {
    return Principals.systemFullName();
  }

  private static boolean isEnabled( final ScalingProcessType scalingProcessType,
                                    final Iterable<SuspendedProcess> suspendedProcesses ) {
    boolean enabled = true;

    for ( final SuspendedProcess suspendedProcess : suspendedProcesses ) {
      if ( suspendedProcess.getScalingProcessType() == scalingProcessType ) {
        enabled = false;
        break;
      }
    }

    return enabled;  }
}
