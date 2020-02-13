/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;


public class DescribeTypeRegistrationType extends CloudFormationMessage {

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String registrationToken;

  public String getRegistrationToken() {
    return registrationToken;
  }

  public void setRegistrationToken(final String registrationToken) {
    this.registrationToken = registrationToken;
  }

}
