/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class GetStackPolicyResult extends EucalyptusData {

  @FieldRange(min = 1, max = 16384)
  private String stackPolicyBody;

  public String getStackPolicyBody() {
    return stackPolicyBody;
  }

  public void setStackPolicyBody(final String stackPolicyBody) {
    this.stackPolicyBody = stackPolicyBody;
  }

}
