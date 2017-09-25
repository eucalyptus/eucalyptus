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

public class TerminateInstanceInAutoScalingGroupType extends AutoScalingMessage {

  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_INSTANCE )
  private String instanceId;
  @Nonnull
  private Boolean shouldDecrementDesiredCapacity;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public Boolean getShouldDecrementDesiredCapacity( ) {
    return shouldDecrementDesiredCapacity;
  }

  public void setShouldDecrementDesiredCapacity( Boolean shouldDecrementDesiredCapacity ) {
    this.shouldDecrementDesiredCapacity = shouldDecrementDesiredCapacity;
  }
}
