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
    IP_ADDRESS( "(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])" ),

    // ELB
    LOADBALANCING_ARN("arn:aws:elasticloadbalancing:[!-~]{1,2019}"),
    LOADBALANCING_NAME("[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,30}[a-zA-Z0-9])?"),
    SECURITY_POLICY("ELBSecurityPolicy-[a-zA-Z0-9-]{1,128}"),
    HTTP_STATUSCODE("[245][0-9][0-9]"),
    REDIRECT_PORT("[1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]|#\\{port\\}"),
    REDIRECT_PROTOCOL("HTTPS?|#\\{protocol\\}"),
    CODE_VALUES_OR_RANGE("0|[1-9][0-9]{0,2}|(?:(?:0|[1-9][0-9]{0,2}),)+(?:0|[1-9][0-9]{0,2})|(?:0|[1-9][0-9]{0,2})-(?:0|[1-9][0-9]{0,2})"),

    // ELB Enums
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

    // EC2
    EC2_EIPALLOC( "eipalloc-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_SECURITYGROUP( "sg-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_SUBNET( "subnet-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_VPC( "vpc-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),


    // IAM
    IAM_ARN( "arn:aws:iam:[!-~]{1,1588}" ),

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
