/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

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
public class RdsMessageValidation {

  public enum FieldRegexValue {
    // Generic
    STRING_128("(?s).{1,128}"),
    STRING_256("(?s).{1,256}"),

    // Enums
    ENUM_ACTIVITYSTREAMMODE("sync|async"),
    ENUM_ACTIVITYSTREAMSTATUS("stopped|starting|started|stopping"),
    ENUM_APPLYMETHOD("immediate|pending-reboot"),
    ENUM_AUTHSCHEME("SECRETS"),
    ENUM_DBPROXYSTATUS("available|modifying|incompatible-network|insufficient-resource-limits|creating|deleting"),
    ENUM_ENGINEFAMILY("MYSQL"),
    ENUM_IAMAUTHMODE("DISABLED|REQUIRED"),
    ENUM_SOURCETYPE("db-instance|db-parameter-group|db-security-group|db-snapshot|db-cluster|db-cluster-snapshot"),
    ENUM_TARGETTYPE("RDS_INSTANCE|RDS_SERVERLESS_ENDPOINT|TRACKED_CLUSTER"),
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

  public static class RdsMessageValidationAssistant implements MessageValidation.ValidationAssistant {

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
