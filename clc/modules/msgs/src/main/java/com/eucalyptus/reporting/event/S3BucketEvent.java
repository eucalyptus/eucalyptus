/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.reporting.event;

import javax.annotation.Nonnull;
import com.eucalyptus.util.OwnerFullName;

public class S3BucketEvent extends S3EventSupport<S3BucketEvent.S3BucketAction> {
  private static final long serialVersionUID = 1L;

  /**
   * @see #forS3BucketCreate
   * @see #forS3BucketDelete
   */
  public enum S3BucketAction {
    BUCKETCREATE, BUCKETDELETE
  }

  public static S3BucketAction forS3BucketCreate() {
    return S3BucketAction.BUCKETCREATE;
  }

  public static S3BucketAction forS3BucketDelete() {
    return S3BucketAction.BUCKETDELETE;
  }

  /**
   * @see #forS3BucketCreate
   * @see #forS3BucketDelete
   */
  public static S3BucketEvent with( @Nonnull final S3BucketAction action,
                                    @Nonnull final String s3UUID,
                                    @Nonnull final String bucketName,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nonnull final Long size ) {
    return new S3BucketEvent( action, s3UUID, bucketName, ownerFullName, size );
  }

  S3BucketEvent( @Nonnull final S3BucketAction action,
                 @Nonnull final String uuid,
                 @Nonnull final String bucketName,
                 @Nonnull final OwnerFullName ownerFullName,
                 @Nonnull final Long size) {
    super( action, uuid, bucketName, ownerFullName, size );
  }

  @Override
  public String toString() {
    return "S3BucketEvent [action=" + getAction() + ", userId="
        + getOwner().getUserId() + ", uuid=" + getUuid() + ", size="
        + getSize() + ", bucketName=" + getBucketName() + "]";
  }
}
