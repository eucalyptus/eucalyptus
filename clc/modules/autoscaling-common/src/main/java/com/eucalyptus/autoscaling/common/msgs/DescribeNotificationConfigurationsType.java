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

import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;

public class DescribeNotificationConfigurationsType extends AutoScalingMessage {

  private AutoScalingGroupNames autoScalingGroupNames;
  private String nextToken;
  @AutoScalingMessageValidation.FieldRange( min = 1L, max = 100L )
  private Integer maxRecords;

  public AutoScalingGroupNames getAutoScalingGroupNames( ) {
    return autoScalingGroupNames;
  }

  public void setAutoScalingGroupNames( AutoScalingGroupNames autoScalingGroupNames ) {
    this.autoScalingGroupNames = autoScalingGroupNames;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }

  public Integer getMaxRecords( ) {
    return maxRecords;
  }

  public void setMaxRecords( Integer maxRecords ) {
    this.maxRecords = maxRecords;
  }
}
