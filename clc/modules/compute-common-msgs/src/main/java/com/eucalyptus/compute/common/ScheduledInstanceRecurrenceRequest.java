/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
