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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ScheduledInstanceRecurrenceRequest extends EucalyptusData {

  private String frequency;
  private Integer interval;
  private ArrayList<Integer> occurrenceDay;
  private Boolean occurrenceRelativeToEnd;
  private String occurrenceUnit;

  public String getFrequency( ) {
    return frequency;
  }

  public void setFrequency( String frequency ) {
    this.frequency = frequency;
  }

  public Integer getInterval( ) {
    return interval;
  }

  public void setInterval( Integer interval ) {
    this.interval = interval;
  }

  public ArrayList<Integer> getOccurrenceDay( ) {
    return occurrenceDay;
  }

  public void setOccurrenceDay( ArrayList<Integer> occurrenceDay ) {
    this.occurrenceDay = occurrenceDay;
  }

  public Boolean getOccurrenceRelativeToEnd( ) {
    return occurrenceRelativeToEnd;
  }

  public void setOccurrenceRelativeToEnd( Boolean occurrenceRelativeToEnd ) {
    this.occurrenceRelativeToEnd = occurrenceRelativeToEnd;
  }

  public String getOccurrenceUnit( ) {
    return occurrenceUnit;
  }

  public void setOccurrenceUnit( String occurrenceUnit ) {
    this.occurrenceUnit = occurrenceUnit;
  }
}
