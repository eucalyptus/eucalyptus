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
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class S3ObjectEvent extends S3EventSupport<S3ObjectEvent.S3ObjectAction> {
  private static final long serialVersionUID = 1L;

  /**
   * @see #forS3ObjectCreate
   * @see #forS3ObjectDelete
   */
  public enum S3ObjectAction {
    OBJECTCREATE, OBJECTDELETE, OBJECTUSAGE
  }

  private final String objectKey;
  private final String version;

  public static S3ObjectAction forS3ObjectCreate() {
    return S3ObjectAction.OBJECTCREATE;
  }

  public static S3ObjectAction forS3ObjectDelete() {
    return S3ObjectAction.OBJECTDELETE;
  }

  /**
   * @see #forS3ObjectCreate
   * @see #forS3ObjectDelete
   */
  public static S3ObjectEvent with( @Nonnull  final S3ObjectAction action,
                                    @Nonnull  final String bucketName,
                                    @Nonnull  final String objectKey,
                                    @Nullable final String version,
                                    @Nonnull  final String userId,
                                    @Nonnull  final String userName,
                                    @Nonnull  final String accountNumber,
                                    @Nonnull  final Long size ) {

    return new S3ObjectEvent( action, bucketName, objectKey, version, userId, userName, accountNumber, size );
  }

  S3ObjectEvent( @Nonnull  final S3ObjectAction action,
                 @Nonnull  final String bucketName,
                 @Nonnull  final String objectKey,
                 @Nullable final String version,
                 @Nonnull  final String userId,
                 @Nonnull  final String userName,
                 @Nonnull  final String accountNumber,
                 @Nonnull  final Long size ) {
    super( action, bucketName, userId, userName, accountNumber, size );
    checkParam( objectKey, not( isEmptyOrNullString() ) );
    this.objectKey = objectKey;
    this.version = version;
  }

  @Nonnull
  public String getObjectKey() {
    return objectKey;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "S3ObjectEvent [action=" + getAction()
        + ", userId=" + getUserId()
        + ", size=" + getSize() + ", bucketName=" + getBucketName()
        + ", objectKey=" + getObjectKey() + ", version=" + getVersion() + "]";
  }
}
