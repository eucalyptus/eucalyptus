/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws;

import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.route53.common.msgs.Route53Message;
import com.eucalyptus.route53.service.Route53ClientException;
import com.eucalyptus.route53.service.Route53Exception;

/**
 *
 */
@ComponentNamed
public class Route53MessageValidator extends ServiceAdvice {

  @Override
  protected void beforeService(@Nonnull final Object object) throws Route53Exception {
    // validate message
    if (object instanceof Route53Message) {
      final Route53Message request = (Route53Message) object;
      final Map<String, String> validationErrorsByField = request.validate();
      if (!validationErrorsByField.isEmpty()) {
        throw new Route53ClientException("ValidationError", validationErrorsByField.values().iterator().next());
      }
    }
  }
}
