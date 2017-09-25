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

import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import com.google.common.collect.Lists;

public class DescribeAutoScalingInstancesType extends AutoScalingMessage {

  private InstanceIds instanceIds;
  @AutoScalingMessageValidation.FieldRange( min = 1L, max = 100L )
  private Integer maxRecords;
  private String nextToken;

  public List<String> instanceIds( ) {
    List<String> names = Lists.newArrayList( );
    if ( instanceIds != null ) {
      names = instanceIds.getMember( );
    }

    return names;
  }

  public InstanceIds getInstanceIds( ) {
    return instanceIds;
  }

  public void setInstanceIds( InstanceIds instanceIds ) {
    this.instanceIds = instanceIds;
  }

  public Integer getMaxRecords( ) {
    return maxRecords;
  }

  public void setMaxRecords( Integer maxRecords ) {
    this.maxRecords = maxRecords;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }
}
