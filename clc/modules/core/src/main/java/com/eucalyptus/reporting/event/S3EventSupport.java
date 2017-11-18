/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting.event;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import com.eucalyptus.event.Event;

/**
 * Support class for S3 events
 */
class S3EventSupport<E extends Enum<E>> implements Event {
  private static final long serialVersionUID = 1L;

  private final E action;
  private final String userId;
  private final String userName;
  private final String accountNumber;
  private final Long size;
  private final String bucketName;

  S3EventSupport( @Nonnull final E action,
                  @Nonnull final String bucketName,
                  @Nonnull final String userId,
                  @Nonnull final String userName,
                  @Nonnull final String accountNumber,
                  @Nonnull final Long size ) {
    checkParam( action, notNullValue() );
    checkParam( bucketName, not( isEmptyOrNullString() ) );
    checkParam( userId, not( isEmptyOrNullString() ) );
    checkParam( userName, not( isEmptyOrNullString() ) );
    checkParam( accountNumber, not( isEmptyOrNullString() ) );
    checkParam( size, notNullValue() );

    this.action = action;
    this.userId = userId;
    this.userName = userName;
    this.accountNumber = accountNumber;
    this.size = size;
    this.bucketName = bucketName;
  }

  @Nonnull
  public E getAction() {
    return action;
  }

  @Nonnull
  public String getUserId() {
    return userId;
  }

  @Nonnull
  public String getUserName() {
    return userName;
  }

  @Nonnull
  public String getAccountNumber() {
    return accountNumber;
  }

  @Nonnull
  public Long getSize() {
    return size;
  }

  @Nonnull
  public String getBucketName() {
    return bucketName;
  }
}
