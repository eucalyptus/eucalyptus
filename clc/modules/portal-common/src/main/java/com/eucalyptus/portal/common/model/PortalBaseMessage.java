/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
