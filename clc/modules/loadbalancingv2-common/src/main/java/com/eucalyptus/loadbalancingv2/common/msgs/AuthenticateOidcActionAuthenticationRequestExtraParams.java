/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AuthenticateOidcActionAuthenticationRequestExtraParams extends EucalyptusData {

  private ArrayList<AuthenticateOidcActionAuthenticationRequestExtraParamsEntry> entry = new ArrayList<>();

  public ArrayList<AuthenticateOidcActionAuthenticationRequestExtraParamsEntry> getEntry() {
    return entry;
  }

  public void setEntry(final ArrayList<AuthenticateOidcActionAuthenticationRequestExtraParamsEntry> entry) {
    this.entry = entry;
  }

}
