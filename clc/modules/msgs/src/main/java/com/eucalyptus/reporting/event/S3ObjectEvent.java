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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.OwnerFullName;

public class S3ObjectEvent extends S3EventSupport<S3ObjectEvent.S3ObjectAction> {
  private static final long serialVersionUID = 1L;

  /**
   * @see #forS3ObjectCreate
   * @see #forS3ObjectDelete
   */
  public enum S3ObjectAction {
    OBJECTCREATE, OBJECTDELETE
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
                                    @Nonnull  final OwnerFullName ownerFullName,
                                    @Nonnull  final Long size ) {

    return new S3ObjectEvent( action, bucketName, objectKey, version, ownerFullName, size );
  }

  S3ObjectEvent( @Nonnull  final S3ObjectAction action,
                 @Nonnull  final String bucketName,
                 @Nonnull  final String objectKey,
                 @Nullable final String version,
                 @Nonnull  final OwnerFullName ownerFullName,
                 @Nonnull  final Long size ) {
    super( action, bucketName, ownerFullName, size );
    assertThat(objectKey, not( isEmptyOrNullString() ));
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
        + ", ownerFullName=" + getOwner()
        + ", size=" + getSize() + ", bucketName=" + getBucketName()
        + ", objectKey=" + getObjectKey() + ", version=" + getVersion() + "]";
  }
}
