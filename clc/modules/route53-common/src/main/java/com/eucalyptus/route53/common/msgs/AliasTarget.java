/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class AliasTarget extends EucalyptusData {

  @Nonnull
  @FieldRange(max = 1024)
  private String dNSName;

  @Nonnull
  private Boolean evaluateTargetHealth;

  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  public String getDNSName() {
    return dNSName;
  }

  public void setDNSName(final String dNSName) {
    this.dNSName = dNSName;
  }

  public Boolean getEvaluateTargetHealth() {
    return evaluateTargetHealth;
  }

  public void setEvaluateTargetHealth(final Boolean evaluateTargetHealth) {
    this.evaluateTargetHealth = evaluateTargetHealth;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

}
