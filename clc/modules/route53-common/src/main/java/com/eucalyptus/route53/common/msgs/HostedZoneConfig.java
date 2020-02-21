/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HostedZoneConfig extends EucalyptusData {

  @FieldRange(max = 256)
  private String comment;

  private Boolean privateZone;

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public Boolean getPrivateZone() {
    return privateZone;
  }

  public void setPrivateZone(final Boolean privateZone) {
    this.privateZone = privateZone;
  }

}
