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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Lists;

public class DescribeScheduledInstanceAvailabilityType extends ScheduledInstanceComputeMessage {

  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = Lists.newArrayList( );
  private SlotDateTimeRangeRequest firstSlotStartTimeRange;
  private Integer minSlotDurationInHours;
  private Integer maxSlotDurationInHours;
  private ScheduledInstanceRecurrenceRequest recurrence;
  private Integer maxResults;
  private String nextToken;

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }

  public SlotDateTimeRangeRequest getFirstSlotStartTimeRange( ) {
    return firstSlotStartTimeRange;
  }

  public void setFirstSlotStartTimeRange( SlotDateTimeRangeRequest firstSlotStartTimeRange ) {
    this.firstSlotStartTimeRange = firstSlotStartTimeRange;
  }

  public Integer getMinSlotDurationInHours( ) {
    return minSlotDurationInHours;
  }

  public void setMinSlotDurationInHours( Integer minSlotDurationInHours ) {
    this.minSlotDurationInHours = minSlotDurationInHours;
  }

  public Integer getMaxSlotDurationInHours( ) {
    return maxSlotDurationInHours;
  }

  public void setMaxSlotDurationInHours( Integer maxSlotDurationInHours ) {
    this.maxSlotDurationInHours = maxSlotDurationInHours;
  }

  public ScheduledInstanceRecurrenceRequest getRecurrence( ) {
    return recurrence;
  }

  public void setRecurrence( ScheduledInstanceRecurrenceRequest recurrence ) {
    this.recurrence = recurrence;
  }

  public Integer getMaxResults( ) {
    return maxResults;
  }

  public void setMaxResults( Integer maxResults ) {
    this.maxResults = maxResults;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }
}
