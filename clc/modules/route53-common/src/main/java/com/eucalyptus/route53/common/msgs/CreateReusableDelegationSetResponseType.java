/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpHeaderMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class CreateReusableDelegationSetResponseType extends Route53Message {


  @Nonnull
  private DelegationSet delegationSet;

  @Nonnull
  @HttpHeaderMapping(header = "Location")
  @FieldRange(max = 1024)
  private String location;

  public DelegationSet getDelegationSet() {
    return delegationSet;
  }

  public void setDelegationSet(final DelegationSet delegationSet) {
    this.delegationSet = delegationSet;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

}
