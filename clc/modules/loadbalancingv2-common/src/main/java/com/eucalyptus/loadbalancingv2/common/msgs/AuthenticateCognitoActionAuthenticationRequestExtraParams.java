/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AuthenticateCognitoActionAuthenticationRequestExtraParams extends EucalyptusData {

  private ArrayList<AuthenticateCognitoActionAuthenticationRequestExtraParamsEntry> entry = new ArrayList<>();

  public ArrayList<AuthenticateCognitoActionAuthenticationRequestExtraParamsEntry> getEntry() {
    return entry;
  }

  public void setEntry(final ArrayList<AuthenticateCognitoActionAuthenticationRequestExtraParamsEntry> entry) {
    this.entry = entry;
  }

}
