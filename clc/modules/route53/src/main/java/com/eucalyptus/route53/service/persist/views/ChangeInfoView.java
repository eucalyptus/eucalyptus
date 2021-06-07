/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.views;

import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.route53.service.persist.entities.ChangeInfo;

/**
 *
 */
@Immutable
public interface ChangeInfoView {

  /**
   * Get the change info id
   */
  String getDisplayName();

  String getOwnerAccountNumber();

  ChangeInfo.Status getState();

  @Nullable
  String getComment();

  Date getCreationTimestamp();
}
