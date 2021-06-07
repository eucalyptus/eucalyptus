/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.loadbalancing.workflow;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;

import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import org.apache.log4j.Logger;

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescriptions;
import com.eucalyptus.util.Exceptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 */
public class VmWorkflowMarshaller {
  private static final String LOADBALANCING_BINDING_NAME =
      "elasticloadbalancing_amazonaws_com_doc_2012_06_01";
  private static Logger LOG =
      Logger.getLogger(VmWorkflowMarshaller.class);

  public static String marshalPolicy(final PolicyDescription policy) {
    final LoadBalancerServoDescriptions descriptions = new LoadBalancerServoDescriptions();
    final LoadBalancerServoDescription desc = new LoadBalancerServoDescription();

    final PolicyDescriptions policies = new PolicyDescriptions();
    policies.setMember(new ArrayList<PolicyDescription>());
    policies.getMember().add(policy);
    desc.setPolicyDescriptions(policies);

    descriptions.setMember(new ArrayList<LoadBalancerServoDescription>());
    descriptions.getMember().add(desc);
    return marshalLoadBalancer(descriptions);
  }

  public static String marshalLoadBalancer(final LoadBalancerServoDescriptions desc) {
    final Binding binding =
        BindingManager.getBinding(LOADBALANCING_BINDING_NAME);
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      binding.toStream(stream, desc);
    } catch (final BindingException ex) {
      LOG.error("Marshalling failed", ex);
      throw Exceptions.toUndeclared(ex);
    }
    final String outString = new String(stream.toByteArray());
    return outString;
  }

  public static Map<String, String> unmarshalInstances(final String encodedStatus) {
    return decodeJsonStringMap(encodedStatus);
  }

  // only fill metric name and value;
  // dimension and unit is filled by caller
  public static MetricData unmarshalMetrics(final String metrics) {
    final Map<String, String> metricMap = decodeJsonStringMap(metrics);
    final MetricData data = new MetricData();

    data.setMember(Lists.<MetricDatum>newArrayList());
    for (final String name : metricMap.keySet()) {
      final MetricDatum datum = new MetricDatum();
      datum.setMetricName(name);
      try {
        datum.setValue(Double.valueOf(metricMap.get(name)));
      } catch (final NumberFormatException ex) {
        LOG.warn("Failed to parse metric value: " + metricMap.get(name));
      }
      data.getMember().add(datum);
    }
    return data;
  }

  public static Map<String, String> decodeJsonStringMap(final String encodedMap) {
    final ObjectMapper mapper = new ObjectMapper();
    final TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
    };
    try {
      final Map<String, String> decodedResult = mapper.readValue(encodedMap, typeRef);
      return decodedResult;
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared("Failed to decode String map in json", ex);
    }
  }
}
