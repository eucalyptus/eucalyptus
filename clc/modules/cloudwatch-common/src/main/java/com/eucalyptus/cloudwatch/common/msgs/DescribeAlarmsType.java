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
package com.eucalyptus.cloudwatch.common.msgs;

import java.util.List;
import com.google.common.collect.Lists;

public class DescribeAlarmsType extends CloudWatchMessage {

  private AlarmNames alarmNames;
  private String alarmNamePrefix;
  private String stateValue;
  private String actionPrefix;
  private Integer maxRecords;
  private String nextToken;

  public List<String> getAlarms( ) {
    List<String> names = Lists.newArrayList( );
    if ( alarmNames != null ) {
      names = alarmNames.getMember( );
    }

    return names;
  }

  public AlarmNames getAlarmNames( ) {
    return alarmNames;
  }

  public void setAlarmNames( AlarmNames alarmNames ) {
    this.alarmNames = alarmNames;
  }

  public String getAlarmNamePrefix( ) {
    return alarmNamePrefix;
  }

  public void setAlarmNamePrefix( String alarmNamePrefix ) {
    this.alarmNamePrefix = alarmNamePrefix;
  }

  public String getStateValue( ) {
    return stateValue;
  }

  public void setStateValue( String stateValue ) {
    this.stateValue = stateValue;
  }

  public String getActionPrefix( ) {
    return actionPrefix;
  }

  public void setActionPrefix( String actionPrefix ) {
    this.actionPrefix = actionPrefix;
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
