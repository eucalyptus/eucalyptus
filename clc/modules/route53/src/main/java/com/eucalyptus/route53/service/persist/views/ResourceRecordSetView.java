/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.views;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.route53.service.persist.entities.ResourceRecordSet.Type;

/**
 *
 */
@Immutable
public interface ResourceRecordSetView {

  String getOwnerAccountNumber();

  String getName();

  Type getType();

  Integer getTtl();

  @Nullable
  String getAliasDnsName();

  @Nullable
  String getAliasHostedZoneId();

  List<String> getValues();
}
