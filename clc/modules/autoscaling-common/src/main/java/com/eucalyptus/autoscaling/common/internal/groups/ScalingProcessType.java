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
package com.eucalyptus.autoscaling.common.internal.groups;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingProcessTypeMetadata;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.OwnerFullName;
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
