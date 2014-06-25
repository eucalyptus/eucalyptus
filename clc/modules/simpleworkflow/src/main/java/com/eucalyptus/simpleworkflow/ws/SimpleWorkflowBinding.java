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
package com.eucalyptus.simpleworkflow.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.simpleworkflow.common.model.ActivityTaskTimeoutType;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.CancelTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.CancelWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ChildPolicy;
import com.eucalyptus.simpleworkflow.common.model.CloseStatus;
import com.eucalyptus.simpleworkflow.common.model.CompleteWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ContinueAsNewWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.DecisionTaskTimeoutType;
import com.eucalyptus.simpleworkflow.common.model.DecisionType;
import com.eucalyptus.simpleworkflow.common.model.EventType;
import com.eucalyptus.simpleworkflow.common.model.ExecutionStatus;
import com.eucalyptus.simpleworkflow.common.model.FailWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RecordMarkerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RegistrationStatus;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelActivityTaskFailedCause;
import com.eucalyptus.simpleworkflow.common.model.RequestCancelExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.ScheduleActivityTaskFailedCause;
import com.eucalyptus.simpleworkflow.common.model.SignalExternalWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.simpleworkflow.common.model.StartChildWorkflowExecutionFailedCause;
import com.eucalyptus.simpleworkflow.common.model.StartTimerFailedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionCancelRequestedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTerminatedCause;
import com.eucalyptus.simpleworkflow.common.model.WorkflowExecutionTimeoutType;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

/**
 *
 */
public class SimpleWorkflowBinding extends MessageStackHandler {

  private static final ObjectMapper mapper = new ObjectMapper( );
  static {
    mapper.setDateFormat( new EpochSecondsDateFormat( ) );
    mapper.getSerializationConfig().addMixInAnnotations( SimpleWorkflowMessage.class, BindingMixIn.class);
    mapper.getDeserializationConfig().addMixInAnnotations( SimpleWorkflowMessage.class, BindingMixIn.class);
    mapper.getSerializationConfig().set( SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false );
    mapper.getSerializationConfig().setSerializationInclusion( JsonSerialize.Inclusion.NON_NULL );
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
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      try {
        BaseMessage msg = bind( httpRequest );
        httpRequest.setMessage( msg );
      } catch ( Exception e ) {
        if ( !( e instanceof BindingException ) ) {
          e = new BindingException( e );
        }
        throw e;
      }
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( 8192 );
      if ( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        //TODO:STEVE: generate an error response
        httpResponse.setStatus( HttpResponseStatus.BAD_REQUEST );
      } else if ( httpResponse.getMessage( ) instanceof ExceptionResponseType ) { //handle error case specially
        //TODO:STEVE: generate an error response
        ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
        httpResponse.setStatus( msg.getHttpStatus( ) );
      } else if ( httpResponse.getMessage( ) != null ){ //actually try to bind response
        final Object message = httpResponse.getMessage( );
        if ( message instanceof SimpleWorkflowMessage ) {
          //TODO:STEVE: verify header capitalization against SWF
          httpResponse.addHeader( "x-amzn-RequestId", ( (SimpleWorkflowMessage) message ).getCorrelationId() );
          if ( !SimpleWorkflowMessage.class.equals( message.getClass() ) ) {
            mapper.writeValue( byteOut, message );
          }
        }
      }
      byte[] req = byteOut.toByteArray( );
      ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( req );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/json" );
      httpResponse.setContent( buffer );
    }
  }

  @SuppressWarnings( "unchecked" )
  private BaseMessage bind( final MappingHttpRequest httpRequest ) throws BindingException, IOException {
    // find action
    final String target = Objects.toString( httpRequest.getHeader( "X-Amz-Target" ), "" );
    final String simpleClassName;
    if ( target.startsWith( "SimpleWorkflowService." ) ) {
      simpleClassName = target.substring( 22 ) + "Request";
    } else {
      throw new BindingException( "Unable to get action from target header: " + target );
    }

    // parse message
    final ChannelBuffer buffer = httpRequest.getContent( );
    buffer.markReaderIndex( );
    byte[] read = new byte[ buffer.readableBytes( ) ];
    buffer.readBytes( read );
    final String content = new String( read, Charsets.UTF_8 );
    buffer.resetReaderIndex( );

    final BaseMessage message;
    try {
      message = mapper.readValue( new StringReader( content ){
        @Override public String toString() { return "message"; } // overridden for better source in error message
      }, (Class<? extends BaseMessage>) Class.forName( SimpleWorkflowMessage.class.getPackage( ).getName( ) + "." + simpleClassName ) );
    } catch ( JsonProcessingException e ) {
      throw new BindingException( e.getMessage( ) );
    } catch ( ClassNotFoundException e ) {
      throw new BindingException( "Binding not found for target: " + target );
    }

    return message;
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
        return new Date( number.longValue() * 1000 );
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
