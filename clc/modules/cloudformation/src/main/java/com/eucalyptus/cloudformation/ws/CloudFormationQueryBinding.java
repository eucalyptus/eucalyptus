/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.ws;


import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.cloudformation.common.msgs.CloudFormationErrorResponse;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.UnsafeByteArrayOutputStream;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;


public class CloudFormationQueryBinding extends BaseQueryBinding<OperationParameter> {
  // TODO: This is a best guess
  static final String CLOUDFORMATION_NAMESPACE_PATTERN = "http://cloudformation.amazonaws.com/doc/%s/";
  static final String CLOUDFORMATION_DEFAULT_VERSION = "2010-05-15";
  static final String CLOUDFORMATION_DEFAULT_NAMESPACE = String.format( CLOUDFORMATION_NAMESPACE_PATTERN, CLOUDFORMATION_DEFAULT_VERSION );
  private static final Logger LOG = Logger.getLogger(CloudFormationQueryBinding.class);
  public CloudFormationQueryBinding() {
    super( CLOUDFORMATION_NAMESPACE_PATTERN, CLOUDFORMATION_DEFAULT_VERSION, OperationParameter.Action );
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    Context context = Contexts.lookup(ctx.getChannel());
    if (context == null || context.getHttpRequest() == null ||
      context.getHttpRequest().getParameters() == null ||
      !"JSON".equals(context.getHttpRequest().getParameters().get("ContentType"))) {
      super.outgoingMessage(ctx, event);
    } else {
      if ( event.getMessage( ) instanceof MappingHttpResponse) {
        MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
        UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 8192 );
        HoldMe.canHas.lock( );
        try {
          if ( httpResponse.getMessage( ) == null ) {
  /** TODO:GRZE: doing nothing here may be needed for streaming? double check... **/
  //          String response = Binding.createRestFault( this.requestType.get( ctx.getChannel( ) ), "Recieved an response from the service which has no content.", "" );
  //          byteOut.write( response.getBytes( ) );
  //          httpResponse.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
          } else if (httpResponse.getMessage( ) instanceof CloudFormationErrorResponse) {
            jsonWriter().writeValue(byteOut, httpResponse.getMessage());
          } else if ( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType) {
            EucalyptusErrorMessageType errMsg = ( EucalyptusErrorMessageType ) httpResponse.getMessage( );
            byteOut.write( Binding.createRestFault(errMsg.getSource(), errMsg.getMessage(), errMsg.getCorrelationId()).getBytes( ) );
            httpResponse.setStatus( HttpResponseStatus.BAD_REQUEST );
          } else if ( httpResponse.getMessage( ) instanceof ExceptionResponseType) {//handle error case specially
            ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
            String detail = msg.getError( );
            if( msg.getException( ) != null ) {
              Logs.extreme().debug( msg, msg.getException( ) );
            }
            if ( msg.getException() instanceof EucalyptusWebServiceException) {
              detail = msg.getCorrelationId( );
            }
            String response = Binding.createRestFault( msg.getRequestType( ), msg.getMessage( ), detail );
            byteOut.write( response.getBytes( ) );
            httpResponse.setStatus( msg.getHttpStatus( ) );
          } else {//actually try to bind response
            try {
              // hack, assume type
              String className = httpResponse.getMessage().getClass().getName();
              // just get the last part
              className = className.substring(className.lastIndexOf(".") + 1);
              String messageType = className.replace("ResponseType", "Response");
              // seriously cheating here
              byteOut.write("{".getBytes());
              byteOut.write(("\"" + messageType + "\" : ").getBytes());
              jsonWriter().writeValue(byteOut, httpResponse.getMessage());
              byteOut.write("}".getBytes());
            } catch ( Exception e ) {
              LOG.debug( e );
              Logs.exhaust( ).error( e, e );
              throw e;
            }
          }
          ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) );
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8" );
          httpResponse.setContent( buffer );
        } finally {
          HoldMe.canHas.unlock( );
        }
      }
    }
  }

  public static ObjectWriter jsonWriter( ) {
    final ObjectMapper mapper = new ObjectMapper( );
    mapper.setPropertyNamingStrategy( PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE );
    mapper.setSerializerFactory( mapper.getSerializerFactory( ).withAdditionalSerializers(
        new SimpleSerializers( Lists.<JsonSerializer<?>>newArrayList( new EpochSecondsDateSerializer( ) ) )
    ) );
    mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
    mapper.addMixIn( EucalyptusData.class, MemberMixin.class ); // omit "member" objects when serializing lists
    return mapper.writer( ).without( SerializationFeature.FAIL_ON_EMPTY_BEANS );
  }

  private interface MemberMixin {
    @JsonValue
    ArrayList<Object> getMember( );
  }

  private static final class EpochSecondsDateSerializer extends StdSerializer<Date> {
    public EpochSecondsDateSerializer( ) {
      super( Date.class );
    }

    @Override
    public void serialize( final Date date,
                           final JsonGenerator jsonGenerator,
                           final SerializerProvider serializerProvider ) throws IOException {
      jsonGenerator.writeRawValue( String.valueOf( date.getTime( ) / 1000 ) + "." + Strings.padStart( Long.toString( date.getTime() % 1000 ), 3, '0' ) );
    }
  }
}
