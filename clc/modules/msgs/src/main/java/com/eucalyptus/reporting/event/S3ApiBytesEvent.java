/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.reporting.event;

import com.amazonaws.auth.policy.actions.S3Actions;
import javax.annotation.Nonnull;

public class S3ApiBytesEvent extends S3EventSupport<S3Actions> {
  private static final long serialVersionUID = 1L;

  private final Long bytesTransferred;
  
  public static S3ApiBytesEvent with( @Nonnull  final S3Actions action,
                                    @Nonnull  final String bucketName,
                                    @Nonnull final Long bytesTransferred,
                                    @Nonnull  final String accountNumber) {

    return new S3ApiBytesEvent( action, bucketName, bytesTransferred, accountNumber );
  }

  S3ApiBytesEvent( @Nonnull  final S3Actions action,
                 @Nonnull  final String bucketName,
                 @Nonnull final Long bytesTransferred,
                 @Nonnull  final String accountNumber ) {
    super( action, bucketName, null, null, accountNumber, null );
    this.bytesTransferred = bytesTransferred;
  }

  @Nonnull
  public Long getBytesTransferred() {
    return bytesTransferred;
  }

  @Override
  public String toString() {
    return "S3ApiBytesEvent [action=" + getAction()
        + ", bucketName=" + getBucketName()
        + ", bytesTransferred=" + getBytesTransferred()
        + ", accountNumber=" + getAccountNumber() + "]";

  }
}
