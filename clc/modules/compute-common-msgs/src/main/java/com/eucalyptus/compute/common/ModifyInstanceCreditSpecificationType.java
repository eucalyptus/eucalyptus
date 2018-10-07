/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
