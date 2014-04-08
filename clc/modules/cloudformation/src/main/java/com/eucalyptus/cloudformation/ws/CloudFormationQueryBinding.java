/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.ws;


import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import org.apache.log4j.Logger;

import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.ByteArrayOutputStream;


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
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream( 8192 );
        HoldMe.canHas.lock( );
        try {
          if ( httpResponse.getMessage( ) == null ) {
  /** TODO:GRZE: doing nothing here may be needed for streaming? double check... **/
  //          String response = Binding.createRestFault( this.requestType.get( ctx.getChannel( ) ), "Recieved an response from the service which has no content.", "" );
  //          byteOut.write( response.getBytes( ) );
  //          httpResponse.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
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
              ObjectMapper mapper = new ObjectMapper();
              // hack, assume type
              String className = httpResponse.getMessage().getClass().getName();
              // just get the last part
              className = className.substring(className.lastIndexOf(".") + 1);
              String messageType = className.replace("ResponseType", "Response");
              // seriously cheating here
              byteOut.write("{".getBytes());
              byteOut.write(("\"" + messageType + "\" : ").getBytes());
              mapper.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS).writeValue(byteOut, httpResponse.getMessage());
              byteOut.write("}".getBytes());
            } catch ( Exception e ) {
              LOG.debug( e );
              Logs.exhaust( ).error( e, e );
              throw e;
            }
          }
          byte[] req = byteOut.toByteArray( );
          ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(req);
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8" );
          httpResponse.setContent( buffer );
        } finally {
          HoldMe.canHas.unlock( );
        }
      }
    }
  }


}
