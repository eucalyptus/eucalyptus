/**
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.BindingReplace;
import com.eucalyptus.storage.msgs.s3.NotificationConfiguration;


public class GetBucketNotificationConfigurationResponseType extends ObjectStorageResponseType implements BindingReplace<NotificationConfiguration> {
  private NotificationConfiguration notificationConfiguration;

  @Nonnull
  @Override
  public NotificationConfiguration bindingReplace( ) {
    return getNotificationConfiguration( );
  }

  public NotificationConfiguration getNotificationConfiguration( ) {
    return notificationConfiguration;
  }

  public void setNotificationConfiguration( final NotificationConfiguration notificationConfiguration ) {
    this.notificationConfiguration = notificationConfiguration;
  }
}