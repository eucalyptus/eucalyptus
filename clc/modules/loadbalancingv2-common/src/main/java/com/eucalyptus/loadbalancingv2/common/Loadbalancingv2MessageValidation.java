/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.MessageValidation;
import com.eucalyptus.util.Pair;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/**
 *
 */
public class Loadbalancingv2MessageValidation {

  public enum FieldRegexValue {
    // Generic
    STRING_128("(?s).{1,128}"),
    STRING_256("(?s).{1,256}"),

    // Enums
    ENUM_ACTIONTYPEENUM("forward|authenticate-oidc|authenticate-cognito|redirect|fixed-response"),
    ENUM_AUTHENTICATECOGNITOACTIONCONDITIONALBEHAVIORENUM("deny|allow|authenticate"),
    ENUM_AUTHENTICATEOIDCACTIONCONDITIONALBEHAVIORENUM("deny|allow|authenticate"),
    ENUM_IPADDRESSTYPE("ipv4|dualstack"),
    ENUM_LOADBALANCERSCHEMEENUM("internet-facing|internal"),
    ENUM_LOADBALANCERSTATEENUM("active|provisioning|active_impaired|failed"),
    ENUM_LOADBALANCERTYPEENUM("application|network"),
    ENUM_PROTOCOLENUM("HTTP|HTTPS|TCP|TLS|UDP|TCP_UDP"),
    ENUM_PROTOCOLVERSIONENUM("GRPC|HTTP1|HTTP2"),
    ENUM_REDIRECTACTIONSTATUSCODEENUM("HTTP_301|HTTP_302"),
    ENUM_TARGETHEALTHREASONENUM("Elb.RegistrationInProgress|Elb.InitialHealthChecking|Target.ResponseCodeMismatch|Target.Timeout|Target.FailedHealthChecks|Target.NotRegistered|Target.NotInUse|Target.DeregistrationInProgress|Target.InvalidState|Target.IpUnusable|Target.HealthCheckDisabled|Elb.InternalError"),
    ENUM_TARGETHEALTHSTATEENUM("initial|healthy|unhealthy|unused|draining|unavailable"),
    ENUM_TARGETTYPEENUM("instance|ip|lambda"),
    ;

    private final Pattern pattern;

    private FieldRegexValue(final String regex) {
      this.pattern = Pattern.compile(regex);
    }

    public Pattern pattern() {
      return pattern;
    }
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FieldRegex {

    FieldRegexValue value();
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FieldRange {

    long max() default Long.MAX_VALUE;

    long min() default 0;
  }

  public static class Loadbalancingv2MessageValidationAssistant implements MessageValidation.ValidationAssistant {

    @Override
    public Pair<Long, Long> range(final Ats ats) {
      final FieldRange range = ats.get(FieldRange.class);
      return range == null ?
          null :
          Pair.pair(range.min(), range.max());
    }

    @Override
    public Pattern regex(final Ats ats) {
      final FieldRegex regex = ats.get(FieldRegex.class);
      return regex == null ?
          null :
          regex.value().pattern();
    }

    @Override
    public boolean validate(final Object object) {
      return object instanceof EucalyptusData;
    }
  }
}
