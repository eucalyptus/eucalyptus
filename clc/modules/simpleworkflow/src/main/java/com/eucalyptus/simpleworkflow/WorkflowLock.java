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
