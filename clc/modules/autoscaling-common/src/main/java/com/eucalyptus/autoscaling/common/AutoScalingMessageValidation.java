/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.autoscaling.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.regex.Pattern;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.system.Ats;
import com.google.common.base.CaseFormat;

/**
 *
 */
public class AutoScalingMessageValidation {

  @Target( ElementType.FIELD)
  @Retention( RetentionPolicy.RUNTIME)
  public @interface FieldRegex {
    FieldRegexValue value();
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FieldRange {
    long min() default 0;
    long max() default Long.MAX_VALUE;
  }

  public enum FieldRegexValue {
    // Generic
    STRING_128( "(?s).{1,128}" ),
    STRING_256( "(?s).{1,256}" ),
    UUID_VERBOSE( "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|verbose" ),

    // Auto scaling
    ADJUSTMENT( "ChangeInCapacity|ExactCapacity|PercentChangeInCapacity" ),
    NAME( "[\u0020-\u0039\u003B-\uD7FF\uE000-\uFFFD\uD800\uDC00-\uDBFF\uDFFF\r\n\t]{1,255}" ),
    ARN( "arn:aws:autoscaling:[\u0020-\uD7FF\uE000-\uFFFD\uD800\uDC00-\uDBFF\uDFFF\r\n\t]{1,1580}" ),
    NAME_OR_ARN( "[\u0020-\u0039\u003B-\uD7FF\uE000-\uFFFD\uD800\uDC00-\uDBFF\uDFFF\r\n\t]{1,255}|arn:aws:autoscaling:[\u0020-\uD7FF\uE000-\uFFFD\uD800\uDC00-\uDBFF\uDFFF\r\n\t]{1,1580}" ),
    HEALTH_CHECK( "ELB|EC2" ),
    HEALTH_STATUS( "Healthy|Unhealthy" ),
    METRIC( "GroupMinSize|GroupMaxSize|GroupDesiredCapacity|GroupInServiceInstances|GroupPendingInstances|GroupTerminatingInstances|GroupTotalInstances" ),
    METRIC_GRANULARITY( "1Minute" ),
    SCALING_PROCESS( "Launch|Terminate|HealthCheck|ReplaceUnhealthy|AZRebalance|AlarmNotification|ScheduledActions|AddToLoadBalancer" ),
    TAG_FILTER( "auto-scaling-group|key|value|propagate-at-launch" ),
    TAG_RESOURCE( "auto-scaling-group" ),
    TERMINATION_POLICY( "OldestInstance|NewestInstance|OldestLaunchConfiguration|ClosestToNextInstanceHour|Default" ),

    // EC2
    EC2_NAME( "(?s).{1,255}" ),
    EC2_MACHINE_IMAGE( "[ae]mi-[0-9a-fA-F]{8}" ),
    EC2_KERNEL_IMAGE( "[ae]ki-[0-9a-fA-F]{8}" ),
    EC2_RAMDISK_IMAGE( "[ae]ri-[0-9a-fA-F]{8}" ),
    EC2_SNAPSHOT( "snap-[0-9a-fA-F]{8}" ),
    EC2_INSTANCE( "i-[0-9a-fA-F]{8}" ),
    EC2_INSTANCE_VERBOSE( "i-[0-9a-fA-F]{8}|verbose" ),
    EC2_USERDATA( "(?s).{0,90000}" ), // Enough for 64KiB Base64 encoded with some formatting
    EC2_SPOT_PRICE( "[0-9]{1,4}\\.[0-9]{2,4}" ),

    // ELB
    ELB_NAME( "(?s).{1,255}" ),

    // IAM
    IAM_NAME_OR_ARN( "[a-zA-Z0-9+=,.@-]{1,128}|arn:aws:iam:[!-~]{1,1588}" ),

    ;

    private final Pattern pattern;

    private FieldRegexValue( final String regex ) {
      this.pattern = Pattern.compile( regex );
    }

    public Pattern pattern() {
      return pattern;
    }
  }

  public static String displayName( Field field ) {
    HttpParameterMapping httpParameterMapping = Ats.from( field ).get( HttpParameterMapping.class );
    return httpParameterMapping != null ?
        httpParameterMapping.parameter()[0] :
        CaseFormat.LOWER_CAMEL.to( CaseFormat.UPPER_CAMEL, field.getName() );
  }


}
