/**
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.BindingReplace;
import com.eucalyptus.storage.msgs.s3.AccelerateConfiguration;


public class GetBucketAccelerateConfigurationResponseType extends ObjectStorageResponseType implements BindingReplace<AccelerateConfiguration> {
  private AccelerateConfiguration accelerateConfiguration;

  @Nonnull
  @Override
  public AccelerateConfiguration bindingReplace( ) {
    return getAccelerateConfiguration( );
  }

  public AccelerateConfiguration getAccelerateConfiguration( ) {
    return accelerateConfiguration;
  }

  public void setAccelerateConfiguration( final AccelerateConfiguration accelerateConfiguration ) {
    this.accelerateConfiguration = accelerateConfiguration;
  }
}
