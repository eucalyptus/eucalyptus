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


public class DelegationSet extends EucalyptusData {

  @FieldRange(min = 1, max = 128)
  private String callerReference;

  @FieldRange(max = 32)
  private String id;

  @Nonnull
  @FieldRange(min = 1)
  private DelegationSetNameServers nameServers;

  public String getCallerReference() {
    return callerReference;
  }

  public void setCallerReference(final String callerReference) {
    this.callerReference = callerReference;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public DelegationSetNameServers getNameServers() {
    return nameServers;
  }

  public void setNameServers(final DelegationSetNameServers nameServers) {
    this.nameServers = nameServers;
  }

}
