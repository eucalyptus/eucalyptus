/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Change extends EucalyptusData {

  private ResourceChange resourceChange;

  @FieldRegex(FieldRegexValue.ENUM_CHANGETYPE)
  private String type;

  public ResourceChange getResourceChange() {
    return resourceChange;
  }

  public void setResourceChange(final ResourceChange resourceChange) {
    this.resourceChange = resourceChange;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
