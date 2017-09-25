/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.autoscaling.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;

public class SetInstanceProtectionType extends AutoScalingMessage {

  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN )
  private String autoScalingGroupName;
  @Nonnull
  private InstanceIds instanceIds;
  @Nonnull
  private Boolean protectedFromScaleIn;

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public InstanceIds getInstanceIds( ) {
    return instanceIds;
  }

  public void setInstanceIds( InstanceIds instanceIds ) {
    this.instanceIds = instanceIds;
  }

  public Boolean getProtectedFromScaleIn( ) {
    return protectedFromScaleIn;
  }

  public void setProtectedFromScaleIn( Boolean protectedFromScaleIn ) {
    this.protectedFromScaleIn = protectedFromScaleIn;
  }
}
