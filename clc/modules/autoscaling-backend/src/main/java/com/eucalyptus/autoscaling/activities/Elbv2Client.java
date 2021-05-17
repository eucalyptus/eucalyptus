/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.autoscaling.activities;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.common.msgs.Loadbalancingv2Message;
import com.eucalyptus.util.DispatchingClient;

public class Elbv2Client extends DispatchingClient<Loadbalancingv2Message, Loadbalancingv2> {

  Elbv2Client( final AccountFullName accountFullName ) {
    super( accountFullName, Loadbalancingv2.class );
  }
}