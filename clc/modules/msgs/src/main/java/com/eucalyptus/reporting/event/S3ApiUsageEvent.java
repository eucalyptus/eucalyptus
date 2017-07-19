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
import javax.annotation.Nullable;

public class S3ApiUsageEvent extends S3EventSupport<S3Actions> {
  private static final long serialVersionUID = 1L;

  // For API usage events, the "size" inherited from S3EventSupport holds
  // the number of bytes transferred (for operations that transfer data)
  public static S3ApiUsageEvent with( 
      @Nonnull  final S3Actions action,
      @Nonnull  final String bucketName,
      @Nonnull  final String accountNumber,
      @Nullable final Long bytesTransferred) {

    return new S3ApiUsageEvent( action, bucketName, accountNumber, bytesTransferred );
  }

  private S3ApiUsageEvent( 
      @Nonnull  final S3Actions action,
      @Nonnull  final String bucketName,
      @Nonnull  final String accountNumber,
      @Nullable final Long bytesTransferred) {
    super( action, bucketName, null, null, accountNumber, bytesTransferred );
  }

  @Override
  public String toString() {
    return "S3ApiUsageEvent "
        + "[action=" + getAction()
        + ", bucketName=" + getBucketName()
        + ", accountNumber=" + getAccountNumber()
        + ", bytesTransferred=" + getSize()
        + "]";
  }
}
