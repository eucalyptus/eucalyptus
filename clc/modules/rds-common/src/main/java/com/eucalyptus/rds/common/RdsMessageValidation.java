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
    STRING_64("(?s).{1,64}"),
    STRING_128("(?s).{1,128}"),
    STRING_255("(?s).{1,255}"),
    STRING_256("(?s).{1,256}"),

    ESTRING_1024("(?s).{0,1024}"),

    // RDS

    RDS_DB_CLUSTER_ID("[a-zA-Z](?:([a-zA-Z0-9]|[-](?=[a-zA-Z0-9])){0,62})?"),
    RDS_DB_ENGINE("[a-z0-9.-]{1,128}"),
    RDS_DB_ENGINE_VERION("[a-zA-Z0-9.-]{1,128}"),
    RDS_DB_INSTANCE_ID("[a-zA-Z](?:([a-zA-Z0-9]|[-](?=[a-zA-Z0-9])){0,62})?"),
    RDS_DB_MASTERPASSWORD("(?:[ !]|[#-.]|[0-?]|[A-~]){8,128}"),
    RDS_DB_MASTERUSERNAME("[a-zA-Z0-9]{1,128}"),
    RDS_DB_OPTION_GROUP_NAME("[a-z0-9._ -]{1,255}"),
    RDS_DB_SNAPSHOT_ID("[a-zA-Z](?:([a-zA-Z0-9]|[-](?=[a-zA-Z0-9])){0,254})?"),
    RDS_DB_SECURITY_GROUP_NAME("[a-z0-9._ -]{1,255}"),
    RDS_DB_SUBNET_GROUP_NAME("[a-z0-9._ -]{1,255}"),
    RDS_DB_PARAMETER_GROUP_NAME("[a-zA-Z](?:([a-zA-Z0-9]|[-](?=[a-zA-Z0-9])){0,254})?"),

    // RDS Enums
    ENUM_ACTIVITYSTREAMMODE("sync|async"),
    ENUM_ACTIVITYSTREAMSTATUS("stopped|starting|started|stopping"),
    ENUM_APPLYMETHOD("immediate|pending-reboot"),
    ENUM_AUTHSCHEME("SECRETS"),
    ENUM_DBPROXYSTATUS("available|modifying|incompatible-network|insufficient-resource-limits|creating|deleting"),
    ENUM_ENGINEFAMILY("MYSQL"),
    ENUM_IAMAUTHMODE("DISABLED|REQUIRED"),
    ENUM_LICENSEMODEL("license-included|bring-your-own-license|general-public-license"),
    ENUM_SOURCETYPE("db-instance|db-parameter-group|db-security-group|db-snapshot|db-cluster|db-cluster-snapshot"),
    ENUM_STORAGETYPE("standard|gp2|io1"),
    ENUM_TARGETTYPE("RDS_INSTANCE|RDS_SERVERLESS_ENDPOINT|TRACKED_CLUSTER"),

    // EC2
    EC2_SECURITYGROUP_ID( "sg-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_SUBNET_ID( "subnet-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),

    // IAM
    IAM_NAME_OR_ARN( "[a-zA-Z0-9+=,.@-]{1,128}|arn:aws:iam:[!-~]{1,1588}" ),

    // KMS
    KMS_NAME_OR_ARN( "[a-zA-Z0-9+=,.@-]{1,128}|arn:aws:kms:[!-~]{1,1588}" ),
    ;

    private final Pattern pattern;

    FieldRegexValue(final String regex) {
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
