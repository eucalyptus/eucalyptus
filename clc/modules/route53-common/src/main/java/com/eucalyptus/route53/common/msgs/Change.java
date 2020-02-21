/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class Change extends EucalyptusData {

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_CHANGEACTION)
  private String action;

  @Nonnull
  private ResourceRecordSet resourceRecordSet;

  public String getAction() {
    return action;
  }

  public void setAction(final String action) {
    this.action = action;
  }

  public ResourceRecordSet getResourceRecordSet() {
    return resourceRecordSet;
  }

  public void setResourceRecordSet(final ResourceRecordSet resourceRecordSet) {
    this.resourceRecordSet = resourceRecordSet;
  }

}
