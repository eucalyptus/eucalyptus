/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.simpleworkflow.tokens;

import com.google.common.base.Preconditions;

/**
 *
 */
public class TaskToken {
  private final String accountNumber;
  private final String domainUuid;
  private final String runId;
  private final Long scheduledEventId;
  private final Long startedEventId;
  private final Long created;
  private final Long expires;

  public TaskToken( final String accountNumber,
                    final String domainUuid,
                    final String runId,
                    final Long scheduledEventId,
                    final Long startedEventId,
                    final Long created,
                    final Long expires ) {
    Preconditions.checkNotNull( accountNumber, "accountNumber is required" );
    Preconditions.checkNotNull( domainUuid, "domainUuid is required" );
    Preconditions.checkNotNull( runId, "runId is required" );
    Preconditions.checkNotNull( scheduledEventId, "scheduledEventId is required" );
    Preconditions.checkNotNull( startedEventId, "startedEventId is required" );
    this.accountNumber = accountNumber;
    this.domainUuid = domainUuid;
    this.runId = runId;
    this.scheduledEventId = scheduledEventId;
    this.startedEventId = startedEventId;
    this.created = created;
    this.expires = expires;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getDomainUuid() {
    return domainUuid;
  }

  public String getRunId() {
    return runId;
  }

  public Long getScheduledEventId() {
    return scheduledEventId;
  }

  public Long getStartedEventId() {
    return startedEventId;
  }

  public Long getCreated() {
    return created;
  }

  public Long getExpires() {
    return expires;
  }
}
