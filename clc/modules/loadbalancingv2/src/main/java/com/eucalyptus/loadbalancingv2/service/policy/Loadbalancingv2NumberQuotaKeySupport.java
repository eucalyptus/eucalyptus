/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadatas;
import com.eucalyptus.loadbalancingv2.common.policy.Loadbalancingv2PolicySpec;
import net.sf.json.JSONException;


public class Loadbalancingv2NumberQuotaKeySupport<T extends Loadbalancingv2Metadata>
    extends QuotaKey {

  private final String key;
  private final String action;
  private final Class<T> metadataClass;

  Loadbalancingv2NumberQuotaKeySupport(final String key,
      final String action,
      final Class<T> metadataClass) {
    this.key = key;
    this.action = action;
    this.metadataClass = metadataClass;
  }

  @Override
  public final void validateValueType(String value) throws JSONException {
    KeyUtils.validateIntegerValue(value, key);
  }

  @Override
  public final boolean canApply(final String action) {
    return PolicySpec.qualifiedName(
        Loadbalancingv2PolicySpec.VENDOR_LOADBALANCINGV2,
        this.action).equals(action);
  }

  @Override
  public final String value(final PolicyScope scope,
      final String id,
      final String resource,
      final Long quantity
  ) throws AuthException {
    switch (scope) {
      case Account:
        return Long.toString(Loadbalancingv2Metadatas.quantityMetricFunction(metadataClass)
            .apply(AccountFullName.getInstance(id)) + 1);
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString(Loadbalancingv2Metadatas.quantityMetricFunction(metadataClass)
            .apply(UserFullName.getInstance(id)) + 1);
    }
    throw new AuthException("Invalid scope");
  }
}

