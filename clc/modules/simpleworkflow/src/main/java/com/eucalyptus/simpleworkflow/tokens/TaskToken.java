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
