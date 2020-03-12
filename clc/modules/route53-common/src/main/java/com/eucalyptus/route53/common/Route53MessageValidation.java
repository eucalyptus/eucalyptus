/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common;

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
public class Route53MessageValidation {

  public enum FieldRegexValue {
    // Generic
    STRING_128("(?s).{1,128}"),
    STRING_256("(?s).{1,256}"),

    // Enums
    ENUM_ACCOUNTLIMITTYPE("MAX_HEALTH_CHECKS_BY_OWNER|MAX_HOSTED_ZONES_BY_OWNER|MAX_TRAFFIC_POLICY_INSTANCES_BY_OWNER|MAX_REUSABLE_DELEGATION_SETS_BY_OWNER|MAX_TRAFFIC_POLICIES_BY_OWNER"),
    ENUM_CHANGEACTION("CREATE|DELETE|UPSERT"),
    ENUM_CHANGESTATUS("PENDING|INSYNC"),
    ENUM_COMPARISONOPERATOR("GreaterThanOrEqualToThreshold|GreaterThanThreshold|LessThanThreshold|LessThanOrEqualToThreshold"),
    ENUM_HEALTHCHECKTYPE("HTTP|HTTPS|HTTP_STR_MATCH|HTTPS_STR_MATCH|TCP|CALCULATED|CLOUDWATCH_METRIC"),
    ENUM_HOSTEDZONELIMITTYPE("MAX_RRSETS_BY_ZONE|MAX_VPCS_ASSOCIATED_BY_ZONE"),
    ENUM_INSUFFICIENTDATAHEALTHSTATUS("Healthy|Unhealthy|LastKnownStatus"),
    ENUM_RRTYPE("SOA|A|TXT|NS|CNAME|MX|NAPTR|PTR|SRV|SPF|AAAA|CAA"),
    ENUM_RESETTABLEELEMENTNAME("FullyQualifiedDomainName|Regions|ResourcePath|ChildHealthChecks"),
    ENUM_RESOURCERECORDSETFAILOVER("PRIMARY|SECONDARY"),
    ENUM_REUSABLEDELEGATIONSETLIMITTYPE("MAX_ZONES_BY_REUSABLE_DELEGATION_SET"),
    ENUM_STATISTIC("Average|Sum|SampleCount|Maximum|Minimum"),
    ENUM_TAGRESOURCETYPE("healthcheck|hostedzone"),
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

  public static class Route53MessageValidationAssistant implements MessageValidation.ValidationAssistant {

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
