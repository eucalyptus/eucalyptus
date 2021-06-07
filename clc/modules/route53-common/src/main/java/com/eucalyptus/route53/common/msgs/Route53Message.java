/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import java.util.Map;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.Route53MessageValidation;
import com.eucalyptus.util.MessageValidation;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage(Route53.class)
public class Route53Message extends BaseMessage {


  public Map<String, String> validate() {
    return MessageValidation.validateRecursively(Maps.newTreeMap(), new Route53MessageValidation.Route53MessageValidationAssistant(), "", this);
  }
}
