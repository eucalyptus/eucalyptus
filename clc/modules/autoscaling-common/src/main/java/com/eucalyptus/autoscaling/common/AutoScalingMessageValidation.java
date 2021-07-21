/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common;

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
public class AutoScalingMessageValidation {

  public static class AutoScalingMessageValidationAssistant implements MessageValidation.ValidationAssistant {
    @Override
    public boolean validate( final Object object ) {
      return object instanceof EucalyptusData;
    }

    @Override
    public Pair<Long, Long> range( final Ats ats ) {
      final FieldRange range = ats.get( FieldRange.class );
      return range == null ?
          null :
          Pair.pair( range.min( ), range.max( ) );
    }

    @Override
    public Pattern regex( final Ats ats ) {
      final FieldRegex regex = ats.get( FieldRegex.class );
      return regex == null ?
          null :
          regex.value( ).pattern( );
    }
  }

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
    ESTRING_256( "(?s).{0,256}" ),
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
    VPC_ZONE_IDENTIFIER( "subnet-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?(?: *, *subnet-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?)*" ),

    // EC2
    EC2_NAME( "(?s).{1,255}" ),
    EC2_MACHINE_IMAGE( "[ae]mi-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_KERNEL_IMAGE( "[ae]ki-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_RAMDISK_IMAGE( "[ae]ri-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_SNAPSHOT( "snap-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_INSTANCE( "i-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_INSTANCE_VERBOSE( "i-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?|verbose" ),
    EC2_USERDATA( "(?s).{0,90000}" ), // Enough for 64KiB Base64 encoded with some formatting
    EC2_SPOT_PRICE( "[0-9]{1,4}\\.[0-9]{2,4}" ),
    EC2_PLACEMENT_TENANCY( "default|dedicated" ),

    // ELB
    ELB_NAME( "(?s).{1,255}" ),
    ELB_TARGETGROUPARN( "arn:aws:elasticloadbalancing:[!-~]{1,483}" ),

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




}
