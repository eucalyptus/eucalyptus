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
package com.eucalyptus.simpleworkflow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.util.Pair;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * The workflow lock ensures that locally we are not attempting to update
 * a workflow concurrently. Other hosts can attempt concurrent updates and
 * will cause optimistic locking errors. When this occurs the local lock
 * ensures the cache eviction of stale data occurs prior to the next update
 * attempt, preventing lost updates.
 */
public final class WorkflowLock implements AutoCloseable {
  private static final Interner<WorkflowLock> workflowLockInterner = Interners.newWeakInterner();

  private final ReentrantLock lock = new ReentrantLock( );
  private final String accountNumber;
  private final String domainUuid;
  private final String runId;


  public static WorkflowLock lock( final AccountFullName accountFullName, final Domain domain, final String runId ) {
    return lock( accountFullName.getAccountNumber( ), domain.getNaturalId( ), runId );
  }

  public static WorkflowLock lock( final AccountFullName accountFullName, final Pair<String,String> domainUuidRunIdPair ) {
    return lock( accountFullName.getAccountNumber( ), domainUuidRunIdPair.getLeft( ), domainUuidRunIdPair.getRight( ) );
  }

  public static WorkflowLock lock( final AccountFullName accountFullName, final String domainUuid, final String runId ) {
    return lock( accountFullName.getAccountNumber( ), domainUuid, runId );
  }

  public static WorkflowLock lock( final String accountNumber, final String domainUuid, final String runId ) {
    return workflowLockInterner.intern( new WorkflowLock( accountNumber, domainUuid, runId ) ).lock( );
  }

  public static WorkflowLock tryLock( final AccountFullName accountFullName, final String domainUuid, final String runId ) {
    return tryLock( accountFullName.getAccountNumber( ), domainUuid, runId );
  }

  public static WorkflowLock tryLock( final String accountNumber, final String domainUuid, final String runId ) {
    return workflowLockInterner.intern( new WorkflowLock( accountNumber, domainUuid, runId ) ).tryLock( );
  }

  WorkflowLock( final String accountNumber, final String domainUuid, final String runId ) {
    this.accountNumber = accountNumber;
    this.domainUuid = domainUuid;
    this.runId = runId;
  }

  public WorkflowLock lock( ) {
    lock.lock( );
    return this;
  }

  public WorkflowLock tryLock( ) {
    lock.tryLock( );
    return this;
  }

  public Boolean isHeldByCurrentThread( ) {
    return lock.isHeldByCurrentThread( );
  }

  @Override
  public void close( ) {
    if ( isHeldByCurrentThread( ) ) {
      lock.unlock( );
    }
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final WorkflowLock that = (WorkflowLock) o;

    if ( !accountNumber.equals( that.accountNumber ) ) return false;
    if ( !domainUuid.equals( that.domainUuid ) ) return false;
    if ( !runId.equals( that.runId ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = accountNumber.hashCode();
    result = 31 * result + domainUuid.hashCode();
    result = 31 * result + runId.hashCode();
    return result;
  }
}
