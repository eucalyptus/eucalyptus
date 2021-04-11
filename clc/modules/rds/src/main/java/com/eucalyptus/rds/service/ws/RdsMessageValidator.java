/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws;

import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.rds.common.msgs.RdsMessage;
import com.eucalyptus.rds.service.RdsClientException;
import com.eucalyptus.rds.service.RdsException;

/**
 *
 */
@ComponentNamed
public class RdsMessageValidator extends ServiceAdvice {

  @Override
  protected void beforeService(@Nonnull final Object object) throws RdsException {
    // validate message
    if (object instanceof RdsMessage) {
      final RdsMessage request = (RdsMessage) object;
      final Map<String, String> validationErrorsByField = request.validate();
      if (!validationErrorsByField.isEmpty()) {
        throw new RdsClientException("ValidationError", validationErrorsByField.values().iterator().next());
      }
    }
  }
}
