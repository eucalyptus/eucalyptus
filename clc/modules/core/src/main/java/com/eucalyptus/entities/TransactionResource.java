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
package com.eucalyptus.entities;

import javax.persistence.EntityTransaction;

/**
 * AutoCloseable transaction suitable for use in try with resources:
 *
 * <pre>
 * try ( TransactionResource transaction = ... ) {
 *   ...
 *   transaction.commit( );
 * }
 * </pre>
 *
 * If not committed the transaction will rollback.
 */
public class TransactionResource implements AutoCloseable, EntityTransaction {

  private final EntityTransaction entityTransaction;

  TransactionResource( final EntityTransaction entityTransaction ) {
    this.entityTransaction = entityTransaction;
  }

  @Override
  public void close( ) {
    if ( isActive( ) ) rollback( );
  }

  @Override
  public void begin( ) {
    entityTransaction.begin( );
  }

  @Override
  public void commit( ) {
    entityTransaction.commit( );
  }

  @Override
  public void rollback( ) {
    entityTransaction.rollback( );
  }

  @Override
  public void setRollbackOnly( ) {
    entityTransaction.setRollbackOnly( );
  }

  @Override
  public boolean getRollbackOnly( ) {
    return entityTransaction.getRollbackOnly( );
  }

  @Override
  public boolean isActive( ) {
    return entityTransaction.isActive( );
  }
}
