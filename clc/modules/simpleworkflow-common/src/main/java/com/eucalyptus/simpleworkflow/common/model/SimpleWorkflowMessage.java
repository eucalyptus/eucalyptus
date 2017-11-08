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
package com.eucalyptus.simpleworkflow.common.model;

import static com.eucalyptus.util.MessageValidation.validateRecursively;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.MessageValidation;
import com.eucalyptus.util.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@ComponentMessage( SimpleWorkflow.class )
public class SimpleWorkflowMessage extends BaseMessage {
  public <TYPE extends SimpleWorkflowMessage> TYPE reply( final TYPE response ) {
    return super.reply( response );
  }

  public Map<String,String> validate( ) {
    return validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new SimpleWorkflowMessageValidationAssistant( ),
        "",
        this );
  }

  private static class SimpleWorkflowMessageValidationAssistant implements MessageValidation.ValidationAssistant {
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

    // SWF

    DURATION_8( "[0-9]|[1-9][0-9]{1,7}" ),

    DURATION_8_NONE( "NONE|[0-9]|[1-9][0-9]{1,7}" ),
    
    INT_MIN_MAX("-?[0-9]|[1-9][0-9]{1,10}"),

    NAME_64( "(?U)[^\\s:/|\u0000-\u001F\u007F-\u009F]{1,2}|(?!.*arn)[^\\s:/|\u0000-\u001F\u007F-\u009F][^:/|\u0000-\u001f\u007f-\u009f]{1,62}[^\\s:/|\u0000-\u001F\u007F-\u009F]" ),

    NAME_256( "(?U)[^\\s:/|\u0000-\u001F\u007F-\u009F]{1,2}|(?!.*arn)[^\\s:/|\u0000-\u001F\u007F-\u009F][^:/|\u0000-\u001f\u007f-\u009f]{1,254}[^\\s:/|\u0000-\u001F\u007F-\u009F]" ),

    CHILD_POLICY( "TERMINATE|REQUEST_CANCEL|ABANDON" ),

    CLOSE_STATUS( "COMPLETED|FAILED|CANCELED|TERMINATED|CONTINUED_AS_NEW|TIMED_OUT" ),

    DECISION( "ScheduleActivityTask|RequestCancelActivityTask|CompleteWorkflowExecution|FailWorkflowExecution|CancelWorkflowExecution|ContinueAsNewWorkflowExecution|RecordMarker|StartTimer|CancelTimer|SignalExternalWorkflowExecution|RequestCancelExternalWorkflowExecution|StartChildWorkflowExecution" ),

    REGISTRATION_STATUS( "REGISTERED|DEPRECATED" ),
    LAMBDA_FUNCTION_FAILURE_CAUSE( "OPERATION_NOT_PERMITTED|ID_ALREADY_IN_USE|OPEN_LAMBDA_FUNCTIONS_LIMIT_EXCEEDED|LAMBDA_FUNCTION_CREATION_RATE_EXCEEDED|LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION")
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
