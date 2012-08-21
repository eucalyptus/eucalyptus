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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import javax.annotation.Nonnull;
import com.eucalyptus.util.OwnerFullName;

public class S3ObjectEvent extends S3EventSupport<S3ObjectEvent.S3ObjectAction> {
  private static final long serialVersionUID = 1L;

  /**
   * @see #forS3ObjectCreate
   * @see #forS3ObjectDelete
   * @see #forS3ObjectGet
   */
  public enum S3ObjectAction {
    OBJECTGET, OBJECTCREATE, OBJECTDELETE
  }

  private final String objectName;

  public static S3ObjectAction forS3ObjectCreate() {
    return S3ObjectAction.OBJECTCREATE;
  }

  public static S3ObjectAction forS3ObjectDelete() {
    return S3ObjectAction.OBJECTDELETE;
  }

  public static S3ObjectAction forS3ObjectGet() {
    return S3ObjectAction.OBJECTGET;
  }

  /**
   * @see #forS3ObjectCreate
   * @see #forS3ObjectDelete
   * @see #forS3ObjectGet
   */
  public static S3ObjectEvent with( @Nonnull final S3ObjectAction action,
                                    @Nonnull final String s3UUID,
                                    @Nonnull final String bucketName,
                                    @Nonnull final String objectName,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nonnull final Long size ) {

    return new S3ObjectEvent( action, s3UUID, bucketName, objectName, ownerFullName, size );
  }

  S3ObjectEvent( @Nonnull final S3ObjectAction action,
                 @Nonnull final String uuid,
                 @Nonnull final String bucketName,
                 @Nonnull final String objectName,
                 @Nonnull final OwnerFullName ownerFullName,
                 @Nonnull final Long size ) {
    super( action, uuid, bucketName, ownerFullName, size );
    assertThat(objectName, notNullValue());
    this.objectName = objectName;
  }

  @Nonnull
  public String getObjectName() {
    return objectName;
  }

  @Override
  public String toString() {
    return "S3ObjectEvent [action=" + getAction()
        + ", ownerFullName=" + getOwner() + ", uuid=" + getUuid()
        + ", size=" + getSize() + ", bucketName=" + getBucketName()
        + ", objectName=" + getObjectName() + "]";
  }
}
