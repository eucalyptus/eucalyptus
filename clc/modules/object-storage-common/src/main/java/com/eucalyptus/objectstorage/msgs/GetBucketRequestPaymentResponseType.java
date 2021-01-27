/**
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import com.eucalyptus.binding.BindingReplace;
import com.eucalyptus.storage.msgs.s3.RequestPaymentConfiguration;


public class GetBucketRequestPaymentResponseType extends ObjectStorageResponseType implements BindingReplace<RequestPaymentConfiguration> {
  private RequestPaymentConfiguration paymentConfiguration;

  @Override
  public RequestPaymentConfiguration bindingReplace( ) {
    return getPaymentConfiguration( );
  }

  public RequestPaymentConfiguration getPaymentConfiguration( ) {
    return paymentConfiguration;
  }

  public void setPaymentConfiguration( final RequestPaymentConfiguration paymentConfiguration ) {
    this.paymentConfiguration = paymentConfiguration;
  }
}