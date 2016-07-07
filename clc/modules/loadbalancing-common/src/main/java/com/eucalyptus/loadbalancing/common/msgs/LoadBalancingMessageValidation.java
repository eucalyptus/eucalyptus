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
package com.eucalyptus.loadbalancing.common.msgs;

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
public class LoadBalancingMessageValidation {

  public static class LoadBalancingMessageValidationAssistant implements MessageValidation.ValidationAssistant {
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
    STRING_255( "(?s).{1,255}" ),

    // ELB
    LOAD_BALANCER_NAME( "[a-zA-Z0-9-]{1,32}" ),
    LOAD_BALANCER_SCHEME( "internal|internet-facing" ),
    LOAD_BALANCER_TAG_KEY( "[\\p{L}\\p{Z}\\p{N}_.:/=+@-]{1,128}" ),
    LOAD_BALANCER_TAG_VALUE( "[\\p{L}\\p{Z}\\p{N}_.:/=+@-]{0,256}" ),
    LOAD_BALANCER_INSTANCE_ID_OPTIONAL_STATUS( "i-[a-fA-F0-9]{8}(?:[0-9a-fA-F]{9})?(?::(?:InService|OutOfService|Error))?" ),

    // EC2
    EC2_INSTANCE_ID( "i-[a-fA-F0-9]{8}(?:[0-9a-fA-F]{9})?" ),
    EC2_SECURITY_GROUP_ID( "sg-[a-fA-F0-9]{8}" ),
    EC2_SUBNET_ID( "subnet-[a-fA-F0-9]{8}" ),
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
