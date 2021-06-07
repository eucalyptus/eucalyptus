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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.cloudwatch.common.internal.domain.alarms;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch_backend")
@Table(name="alarm_history")
public class AlarmHistory extends AbstractPersistent {

  @Override
  public String toString() {
    return "AlarmHistory [accountId=" + accountId + ", alarmName=" + alarmName
        + ", historyData=" + historyData + ", historyItemType="
        + historyItemType + ", historySummary=" + historySummary
        + ", timestamp=" + timestamp + "]";
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAlarmName() {
    return alarmName;
  }

  public void setAlarmName(String alarmName) {
    this.alarmName = alarmName;
  }

  public String getHistoryData() {
    return historyData;
  }

  public void setHistoryData(String historyData) {
    this.historyData = historyData;
  }

  public HistoryItemType getHistoryItemType() {
    return historyItemType;
  }

  public void setHistoryItemType(HistoryItemType historyItemType) {
    this.historyItemType = historyItemType;
  }

  public String getHistorySummary() {
    return historySummary;
  }

  public void setHistorySummary(String historySummary) {
    this.historySummary = historySummary;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public static Logger getLog() {
    return LOG;
  }

  private static final Logger LOG = Logger.getLogger(AlarmHistory.class);
  public AlarmHistory() {
    super();
  }

  @Column( name = "account_id" , nullable = false)
  private String accountId;
  @Column( name = "alarm_name" , nullable = false)
  private String alarmName; 
  @Column( name = "history_data", length = 4095 )
  private String historyData;

  @Column(name = "history_item_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private HistoryItemType historyItemType;

  @Column( name = "history_summary" , nullable = false)
  private String historySummary;
  @Column( name = "timestamp" , nullable = false)
  private Date timestamp;
  
  public enum HistoryItemType {
    ConfigurationUpdate, StateUpdate, Action
  }
}
