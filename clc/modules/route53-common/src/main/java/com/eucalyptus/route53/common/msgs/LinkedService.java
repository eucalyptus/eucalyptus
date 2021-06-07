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


public class LinkedService extends EucalyptusData {

  @FieldRange(max = 256)
  private String description;

  @FieldRange(max = 128)
  private String servicePrincipal;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getServicePrincipal() {
    return servicePrincipal;
  }

  public void setServicePrincipal(final String servicePrincipal) {
    this.servicePrincipal = servicePrincipal;
  }

}
