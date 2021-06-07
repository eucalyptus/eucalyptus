/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import java.lang.reflect.Method;
import java.util.Map;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2MessageValidation;
import com.eucalyptus.util.MessageValidation;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage(Loadbalancingv2.class)
public class Loadbalancingv2Message extends BaseMessage {


  public static ResponseMetadata getResponseMetadata(final BaseMessage message) {
    try {
      Method responseMetadataMethod = message.getClass().getMethod("getResponseMetadata");
      return ((ResponseMetadata) responseMetadataMethod.invoke(message));
    } catch (Exception e) {
    }

    return null;
  }

  @Override
  public <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply();
    final ResponseMetadata responseMetadata = getResponseMetadata(type);
    if (responseMetadata != null) {
      responseMetadata.setRequestId(type.getCorrelationId());
    }
    return type;
  }

  public Map<String, String> validate() {
    return MessageValidation.validateRecursively(Maps.newTreeMap(), new Loadbalancingv2MessageValidation.Loadbalancingv2MessageValidationAssistant(), "", this);
  }
}
