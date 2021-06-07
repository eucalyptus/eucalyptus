/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import javax.annotation.Nonnull;


public class ModifyInstanceCreditSpecificationType extends ComputeMessage {

  private String clientToken;
  @Nonnull
  private InstanceCreditSpecificationListRequest instanceCreditSpecifications;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }

  public InstanceCreditSpecificationListRequest getInstanceCreditSpecifications( ) {
    return instanceCreditSpecifications;
  }

  public void setInstanceCreditSpecifications( final InstanceCreditSpecificationListRequest instanceCreditSpecifications ) {
    this.instanceCreditSpecifications = instanceCreditSpecifications;
  }

}
