/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class VPC extends EucalyptusData {

  @FieldRange(max = 1024)
  private String vPCId;

  @FieldRange(min = 1, max = 64)
  @FieldRegex(FieldRegexValue.ENUM_VPCREGION)
  private String vPCRegion;

  public String getVPCId() {
    return vPCId;
  }

  public void setVPCId(final String vPCId) {
    this.vPCId = vPCId;
  }

  public String getVPCRegion() {
    return vPCRegion;
  }

  public void setVPCRegion(final String vPCRegion) {
    this.vPCRegion = vPCRegion;
  }

}
