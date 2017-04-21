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
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class S3ApiUsageEvent extends S3EventSupport<S3Actions> {
  private static final long serialVersionUID = 1L;

  private final String usageType;
  private final Long usageValue;
  
  public static S3ApiUsageEvent with( @Nonnull  final S3Actions action,
                                    @Nonnull  final String bucketName,
                                    @Nonnull  final String usageType,
                                    @Nullable final Long usageValue,
                                    @Nonnull  final String accountNumber) {

    return new S3ApiUsageEvent( action, bucketName, usageType, usageValue, accountNumber );
  }

  S3ApiUsageEvent( @Nonnull  final S3Actions action,
                 @Nonnull  final String bucketName,
                 @Nonnull  final String usageType,
                 @Nullable final Long usageValue,
                 @Nonnull  final String accountNumber ) {
    super( action, bucketName, null, null, accountNumber, null );
    checkParam( usageType, not( isEmptyOrNullString() ) );
    this.usageType = usageType;
    this.usageValue = usageValue;
  }

  @Nonnull
  public String getUsageType() {
    return usageType;
  }

  public Long getUsageValue() {
    return usageValue;
  }

  @Override
  public String toString() {
    return "S3ApiUsageEvent [action=" + getAction()
        + ", bucketName=" + getBucketName()
        + ", usageType=" + getUsageType()
        + ", usageValue=" + getUsageValue() + "]";
  }
}
