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
package com.eucalyptus.objectstorage.reporting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.amazonaws.auth.policy.actions.S3Actions;
import com.eucalyptus.objectstorage.util.S3BillingActions;
import com.eucalyptus.reporting.Counter.CountedS3;
import com.eucalyptus.reporting.event.S3ApiUsageEvent;

public class S3ApiUsageCounter {

  private static final Logger LOG = Logger.getLogger( S3ApiUsageCounter.class );

  public static void count(
      @Nonnull  final S3Actions action,
      @Nonnull  final String bucketName,
      @Nonnull  final String accountNumber,
      @Nullable final Long bytesTransferred) {
    S3ApiEventListener.count(
        S3ApiUsageEvent.with(action, bucketName, accountNumber, bytesTransferred));
  }
  
  public static CountedS3 countApiCountEvent (S3ApiUsageEvent event) {
    return new CountedS3( 
        event.getAccountNumber(),
        S3BillingActions.getBillingUsageCountName(event.getAction()),
        event.getAction(),
        event.getBucketName()
        );
  }

  public static CountedS3 countApiBytesEvent (S3ApiUsageEvent event) {
    return new CountedS3( 
        event.getAccountNumber(),
        S3BillingActions.getBillingUsageBytesName(event.getAction()),
        event.getAction(),
        event.getBucketName()
        );
  }

  public static Long countApiBytesValue (S3ApiUsageEvent event) {
    return event.getSize();
  }
}
