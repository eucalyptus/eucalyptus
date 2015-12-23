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
package com.eucalyptus.simpleworkflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.deser.DateDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.Version;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskStatus;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskTimeoutType;
import com.eucalyptus.simpleworkflow.common.model.CancelTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.CancelWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ChildPolicy;
import com.eucalyptus.simpleworkflow.common.model.CloseStatus;
import com.eucalyptus.simpleworkflow.common.model.CloseStatusFilter;
import com.eucalyptus.simpleworkflow.common.model.CompleteWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ContinueAsNewWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.ContinueAsNewWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.Decision;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskTimeoutType;
import com.eucalyptus.simpleworkflow.common.model.DecisionType;
import com.eucalyptus.simpleworkflow.common.model.EventType;
import com.eucalyptus.simpleworkflow.common.model.ExecutionStatus;
import com.eucalyptus.simpleworkflow.common.model.FailWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.LambdaFunctionTimeoutType;
import com.eucalyptus.simpleworkflow.common.model.RecordMarkerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RegistrationStatus;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelActivityTaskFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ScheduleActivityTaskFailedCause;
import com.eucalyptus.simpleworkflow.common.model.SignalExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionDecisionAttributes;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.StartTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowEventAttributes;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCancelRequestedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCount;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionInfo;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTerminatedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTimeoutType;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

/**
 *
 */
public class SwfJsonUtils {

  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    final SimpleModule module = new SimpleModule( "SwfModule", new Version(1, 0, 0, null) )
              .addSerializer( Date.class, new EpochSecondsDateSerializer( )  )
              .addDeserializer( Date.class, new EpochSecondsDateDeserializer( ) );
    mapper.registerModule( module );
    mapper.setDateFormat( new EpochSecondsDateFormat() );
    mapper.getSerializationConfig().addMixInAnnotations( SimpleWorkflowMessage.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( WorkflowEventAttributes.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( Decision.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( ContinueAsNewWorkflowExecutionDecisionAttributes.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( StartChildWorkflowExecutionDecisionAttributes.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( ActivityTaskStatus.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( CloseStatusFilter.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( WorkflowExecutionCount.class, BindingMixIn.class );
    mapper.getSerializationConfig().addMixInAnnotations( WorkflowExecutionInfo.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( SimpleWorkflowMessage.class, BindingMixIn.class);
    mapper.getDeserializationConfig().addMixInAnnotations( WorkflowEventAttributes.class, BindingMixIn.class);
    mapper.getDeserializationConfig().addMixInAnnotations( ContinueAsNewWorkflowExecutionDecisionAttributes.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( StartChildWorkflowExecutionDecisionAttributes.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( Decision.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( ActivityTaskStatus.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( CloseStatusFilter.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( WorkflowExecutionCount.class, BindingMixIn.class );
    mapper.getDeserializationConfig().addMixInAnnotations( WorkflowExecutionInfo.class, BindingMixIn.class );
    mapper.getSerializationConfig().set( SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false );
    mapper.getSerializationConfig().setSerializationInclusion( JsonSerialize.Inclusion.NON_NULL );
  }

  public static String writeObjectAsString( final Object object ) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream( 512 );
    try {
      mapper.writeValue( out, object );
    } catch ( IOException ioe ) {
      throw Exceptions.toUndeclared( ioe );
    }
    return new String( out.toByteArray( ), Charsets.UTF_8 );
  }

  public static void writeObject( final OutputStream out, final Object object ) throws IOException {
    mapper.writeValue( out, object );
  }

  public static <T> T readObject( final String in, final Class<T> type ) throws IOException {
    return mapper.readValue( new StringReader( in ){
      @Override public String toString() { return "message"; } // overridden for better source in error message
    }, type );
  }

  // TODO:STEVE: add unit test to ensure we don't start using unexpected properties from BaseMessage
  // ignore properties of BaseMessage
  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId",
      "_disabledServices", "_notreadyServices", "_stoppedServices", "_epoch", "_services", "_return",
      "callerContext" } )
  private interface BindingMixIn {
    // ignore setter with enum parameters, method with string parameter will be used
    @JsonIgnore void setCause(CancelTimerFailedCause cause);
    @JsonIgnore void setCause(CancelWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(CompleteWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(ContinueAsNewWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(FailWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(RecordMarkerFailedCause cause);
    @JsonIgnore void setCause(RequestCancelActivityTaskFailedCause cause);
    @JsonIgnore void setCause(RequestCancelExternalWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(ScheduleActivityTaskFailedCause cause);
    @JsonIgnore void setCause(SignalExternalWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(StartChildWorkflowExecutionFailedCause cause);
    @JsonIgnore void setCause(StartTimerFailedCause cause);
    @JsonIgnore void setCause(WorkflowExecutionCancelRequestedCause cause);
    @JsonIgnore void setCause(WorkflowExecutionTerminatedCause cause);
    @JsonIgnore void setChildPolicy(ChildPolicy childPolicy);
    @JsonIgnore void setDecisionType(DecisionType decisionType);
    @JsonIgnore void setDefaultChildPolicy(ChildPolicy childPolicy);
    @JsonIgnore void setEventType(EventType eventType);
    @JsonIgnore void setExecutionStatus(ExecutionStatus executionStatus);
    @JsonIgnore void setRegistrationStatus(RegistrationStatus registrationStatus);
    @JsonIgnore void setStatus(CloseStatus status);
    @JsonIgnore void setStatus(RegistrationStatus status);
    @JsonIgnore void setTimeoutType(ActivityTaskTimeoutType timeoutType);
    @JsonIgnore void setTimeoutType(DecisionTaskTimeoutType timeoutType);
    @JsonIgnore void setTimeoutType(WorkflowExecutionTimeoutType timeoutType);
    @JsonIgnore void setTimeoutType(LambdaFunctionTimeoutType timeoutType);
    @JsonIgnore Boolean isCancelRequested( );
    @JsonIgnore Boolean isTruncated( );
  }

  private static final class EpochSecondsDateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize( final JsonParser jsonParser,
                             final DeserializationContext deserializationContext ) throws IOException {
      final JsonToken token = jsonParser.getCurrentToken( );
      switch ( token ) {
        case VALUE_NUMBER_FLOAT:
          return new Date( jsonParser.getDecimalValue( ).movePointRight( 3 ).longValue( ) );
        case VALUE_NUMBER_INT:
          return new Date( jsonParser.getLongValue( ) * 1000L );
        default:
          return new DateDeserializer( ).deserialize( jsonParser, deserializationContext );
      }
    }
  }

  private static final class EpochSecondsDateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize( final Date date,
                           final JsonGenerator jsonGenerator,
                           final SerializerProvider serializerProvider ) throws IOException {
      jsonGenerator.writeRawValue( String.valueOf( date.getTime( ) / 1000 ) + "." + Strings.padStart( Long.toString( date.getTime( ) % 1000 ), 3, '0' ) );
    }
  }

  private static final class EpochSecondsDateFormat extends DateFormat implements Cloneable {
    private static final long serialVersionUID = 1L;

    @Override
    public StringBuffer format( final Date date, final StringBuffer toAppendTo, final FieldPosition fieldPosition ) {
      StringBuffer out = toAppendTo == null ? new StringBuffer( ) : toAppendTo;
      if ( date != null ) {
        out.append( date.getTime( ) / 1000 );
        out.append( '.' );
        out.append( Strings.padStart( Long.toString( date.getTime( ) % 1000 ), 3, '0' ) );
      }
      return out;
    }

    @Override
    public Date parse( final String source, final ParsePosition pos ) {
      if ( source != null ) try {
        Number number = DecimalFormat.getInstance( new Locale( "en" ) ).parse( source );
        pos.setIndex( source.length( ) ) ;
        return new Date( (long)(number.doubleValue() * 1000d) );
      } catch ( ParseException e ) {
      }
      return null;
    }

    @SuppressWarnings( "CloneDoesntCallSuperClone" )
    @Override
    public Object clone( ) {
      return this;
    }
  }
}
