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


public class HealthCheckObservation extends EucalyptusData {

  @FieldRange(max = 45)
  private String iPAddress;

  @FieldRange(min = 1, max = 64)
  @FieldRegex(FieldRegexValue.ENUM_HEALTHCHECKREGION)
  private String region;

  private StatusReport statusReport;

  public String getIPAddress() {
    return iPAddress;
  }

  public void setIPAddress(final String iPAddress) {
    this.iPAddress = iPAddress;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(final String region) {
    this.region = region;
  }

  public StatusReport getStatusReport() {
    return statusReport;
  }

  public void setStatusReport(final StatusReport statusReport) {
    this.statusReport = statusReport;
  }

}
