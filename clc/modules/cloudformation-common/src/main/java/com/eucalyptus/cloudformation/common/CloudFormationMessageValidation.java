/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common;

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
public class CloudFormationMessageValidation {

  public enum FieldRegexValue {
    // Generic
    STRING_128("(?s).{1,128}"),
    STRING_256("(?s).{1,256}"),

    // Enums
    ENUM_ACCOUNTGATESTATUS("SUCCEEDED|FAILED|SKIPPED"),
    ENUM_CAPABILITY("CAPABILITY_IAM|CAPABILITY_NAMED_IAM|CAPABILITY_AUTO_EXPAND"),
    ENUM_CHANGEACTION("Add|Modify|Remove|Import"),
    ENUM_CHANGESETSTATUS("CREATE_PENDING|CREATE_IN_PROGRESS|CREATE_COMPLETE|DELETE_COMPLETE|FAILED"),
    ENUM_CHANGESETTYPE("CREATE|UPDATE|IMPORT"),
    ENUM_CHANGESOURCE("ResourceReference|ParameterReference|ResourceAttribute|DirectModification|Automatic"),
    ENUM_CHANGETYPE("Resource"),
    ENUM_DEPRECATEDSTATUS("LIVE|DEPRECATED"),
    ENUM_DIFFERENCETYPE("ADD|REMOVE|NOT_EQUAL"),
    ENUM_EVALUATIONTYPE("Static|Dynamic"),
    ENUM_EXECUTIONSTATUS("UNAVAILABLE|AVAILABLE|EXECUTE_IN_PROGRESS|EXECUTE_COMPLETE|EXECUTE_FAILED|OBSOLETE"),
    ENUM_HANDLERERRORCODE("NotUpdatable|InvalidRequest|AccessDenied|InvalidCredentials|AlreadyExists|NotFound|ResourceConflict|Throttling|ServiceLimitExceeded|NotStabilized|GeneralServiceException|ServiceInternalError|NetworkFailure|InternalFailure"),
    ENUM_ONFAILURE("DO_NOTHING|ROLLBACK|DELETE"),
    ENUM_OPERATIONSTATUS("PENDING|IN_PROGRESS|SUCCESS|FAILED"),
    ENUM_PROVISIONINGTYPE("NON_PROVISIONABLE|IMMUTABLE|FULLY_MUTABLE"),
    ENUM_REGISTRATIONSTATUS("COMPLETE|IN_PROGRESS|FAILED"),
    ENUM_REGISTRYTYPE("RESOURCE"),
    ENUM_REPLACEMENT("True|False|Conditional"),
    ENUM_REQUIRESRECREATION("Never|Conditionally|Always"),
    ENUM_RESOURCEATTRIBUTE("Properties|Metadata|CreationPolicy|UpdatePolicy|DeletionPolicy|Tags"),
    ENUM_RESOURCESIGNALSTATUS("SUCCESS|FAILURE"),
    ENUM_RESOURCESTATUS("CREATE_IN_PROGRESS|CREATE_FAILED|CREATE_COMPLETE|DELETE_IN_PROGRESS|DELETE_FAILED|DELETE_COMPLETE|DELETE_SKIPPED|UPDATE_IN_PROGRESS|UPDATE_FAILED|UPDATE_COMPLETE|IMPORT_FAILED|IMPORT_COMPLETE|IMPORT_IN_PROGRESS|IMPORT_ROLLBACK_IN_PROGRESS|IMPORT_ROLLBACK_FAILED|IMPORT_ROLLBACK_COMPLETE"),
    ENUM_STACKDRIFTDETECTIONSTATUS("DETECTION_IN_PROGRESS|DETECTION_FAILED|DETECTION_COMPLETE"),
    ENUM_STACKDRIFTSTATUS("DRIFTED|IN_SYNC|UNKNOWN|NOT_CHECKED"),
    ENUM_STACKINSTANCESTATUS("CURRENT|OUTDATED|INOPERABLE"),
    ENUM_STACKRESOURCEDRIFTSTATUS("IN_SYNC|MODIFIED|DELETED|NOT_CHECKED"),
    ENUM_STACKSETDRIFTDETECTIONSTATUS("COMPLETED|FAILED|PARTIAL_SUCCESS|IN_PROGRESS|STOPPED"),
    ENUM_STACKSETDRIFTSTATUS("DRIFTED|IN_SYNC|NOT_CHECKED"),
    ENUM_STACKSETOPERATIONACTION("CREATE|UPDATE|DELETE|DETECT_DRIFT"),
    ENUM_STACKSETOPERATIONRESULTSTATUS("PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED"),
    ENUM_STACKSETOPERATIONSTATUS("RUNNING|SUCCEEDED|FAILED|STOPPING|STOPPED"),
    ENUM_STACKSETSTATUS("ACTIVE|DELETED"),
    ENUM_STACKSTATUS("CREATE_IN_PROGRESS|CREATE_FAILED|CREATE_COMPLETE|ROLLBACK_IN_PROGRESS|ROLLBACK_FAILED|ROLLBACK_COMPLETE|DELETE_IN_PROGRESS|DELETE_FAILED|DELETE_COMPLETE|UPDATE_IN_PROGRESS|UPDATE_COMPLETE_CLEANUP_IN_PROGRESS|UPDATE_COMPLETE|UPDATE_ROLLBACK_IN_PROGRESS|UPDATE_ROLLBACK_FAILED|UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS|UPDATE_ROLLBACK_COMPLETE|REVIEW_IN_PROGRESS|IMPORT_IN_PROGRESS|IMPORT_COMPLETE|IMPORT_ROLLBACK_IN_PROGRESS|IMPORT_ROLLBACK_FAILED|IMPORT_ROLLBACK_COMPLETE"),
    ENUM_TEMPLATESTAGE("Original|Processed"),
    ENUM_VISIBILITY("PUBLIC|PRIVATE"),
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

  public static class CloudFormationMessageValidationAssistant implements MessageValidation.ValidationAssistant {

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
