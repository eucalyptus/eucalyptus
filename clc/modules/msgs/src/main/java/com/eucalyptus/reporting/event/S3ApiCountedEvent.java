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

public class S3ApiCountedEvent extends S3EventSupport<S3Actions> {
  private static final long serialVersionUID = 1L;

  public static S3ApiCountedEvent with( @Nonnull  final S3Actions action,
                                    @Nonnull  final String bucketName,
                                    @Nonnull  final String accountNumber) {

    return new S3ApiCountedEvent( action, bucketName, accountNumber );
  }

  S3ApiCountedEvent( @Nonnull  final S3Actions action,
                 @Nonnull  final String bucketName,
                 @Nonnull  final String accountNumber ) {
    super( action, bucketName, null, null, accountNumber, null );
  }

  @Override
  public String toString() {
    return "S3ApiCountedEvent [action=" + getAction()
        + ", bucketName=" + getBucketName()
        + ", accountNumber=" + getAccountNumber() + "]";
  }
}
