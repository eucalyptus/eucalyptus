/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.common.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.MessageValidation;
import com.eucalyptus.util.Pair;
import com.google.common.collect.ImmutableSet;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
public class PortalBaseMessage extends BaseMessage {

  protected static class PortalBaseMessageValidationAssistant implements MessageValidation.ValidationAssistant {
    private final Set<Class<?>> simpleTypes = ImmutableSet.<Class<?>>of(
        Boolean.class,
        Date.class,
        Integer.class,
        Long.class,
        String.class
    );

    @Override
    public boolean validate( final Object object ) {
      return object != null &&
          !simpleTypes.contains( object.getClass( ) ) &&
          !Iterable.class.isAssignableFrom( object.getClass( ) );
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
    // General
    STRING_128( "(?s).{1,128}" ),
    STRING_256( "(?s).{1,256}" ),
    STRING_1024( "(?s).{1,1024}" ),
    STRING_1224( "(?s).{1,1224}" ),

    OPT_STRING_128( "(?s).{0,128}" ),
    OPT_STRING_256( "(?s).{0,256}" ),
    OPT_STRING_1024( "(?s).{0,1024}" ),
    OPT_STRING_1728( "(?s).{0,1728}" ),
    OPT_STRING_2048( "(?s).{0,2048}" ),
    OPT_STRING_32768( "(?s).{0,32768}" ),

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
