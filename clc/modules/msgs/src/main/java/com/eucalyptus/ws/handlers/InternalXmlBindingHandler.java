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
package com.eucalyptus.ws.handlers;

import org.apache.axiom.om.OMElement;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.WebServicesException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

/**
 *
 */
public class InternalXmlBindingHandler extends MessageStackHandler {

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = (MappingHttpMessage) event.getMessage( );
      OMElement omElem;
      String type = null;
      if ( httpMessage.getMessage( ) instanceof EucalyptusErrorMessageType || httpMessage.getMessage( ) == null ) {
        return;
      } else if ( httpMessage.getMessage( ) instanceof ExceptionResponseType ) {
        ExceptionResponseType msg = ( ExceptionResponseType ) httpMessage.getMessage( );
        String createFaultDetails = Logs.isExtrrreeeme( )
            ? Exceptions.string( msg.getException( ) )
            : msg.getException( ).getMessage( );
        omElem = Binding.createFault( msg.getRequestType( ), msg.getMessage( ), createFaultDetails );
        if ( httpMessage instanceof MappingHttpResponse ) {
          ( ( MappingHttpResponse ) httpMessage ).setStatus( msg.getHttpStatus( ) );
        }
      } else {
        final Object object = httpMessage.getMessage( );
        try {
          type = httpMessage.getMessage( ).getClass( ).getName( );
          omElem = BaseMessages.toOm( (BaseMessage) object );
        } catch ( Exception e ) {
          Logs.exhaust( ).debug( e, e );
          throw new BindingException( "Failed to marshall type " + object.getClass( ).getCanonicalName( ) + " caused by: "
              + e.getMessage( ), e );
        }
      }
      if ( type != null ) {
        httpMessage.addHeader( "X-Euca-Type", type );
      }
      httpMessage.setOmMessage( omElem );
    }
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpMessage = (MappingHttpMessage) event.getMessage( );
      String type = httpMessage.getHeader( "X-Euca-Type" );
      BaseMessage msg;
      try {
        //TODO:STEVE: get component by path?
        if ( type==null && httpMessage instanceof MappingHttpRequest ) {
          String servicePath = ((MappingHttpRequest)httpMessage).getServicePath( );
          String simpleType = httpMessage.getSoapEnvelope( ).getSOAPBodyFirstElementLocalName( );
          ComponentId componentId = ComponentIds.list( ).stream( ).filter( cid -> servicePath.equals( cid.getServicePath(  ) ) ).findFirst( ).get( );
          type = ComponentMessages.forComponent( componentId ).stream( ).filter( c -> simpleType.equals( c.getSimpleName( ) ) ).findFirst( ).get( ).getName( );
        }
        final Class<?> typeClass = getClass( ).getClassLoader( ).loadClass( type );
        if ( !BaseMessage.class.isAssignableFrom( typeClass ) ) {
          throw new IllegalArgumentException( "Unsupported type: " + type );
        }
        OMElement elem = httpMessage.getOmMessage( );
        msg = BaseMessages.fromOm( elem, (Class<? extends BaseMessage>) typeClass );
      } catch ( Exception e ) {
        throw new WebServicesException( new BindingException( "Failed to unmarshall type " + type + " caused by: "
            + e.getMessage( ), e ) );
      }

      // in case the base message has request ID in its correlation ID prefix,
      // we should reset the correlation ID using the request ID
      if ( httpMessage.getCorrelationId() != null &&
          msg.getCorrelationId()!=null &&
          msg.hasRequestId()) {
        try{
          final Context context = Contexts.lookup(httpMessage.getCorrelationId());
          // reset correlation ID
          msg.regardingRequestId(msg.getCorrelationId());
          httpMessage.setCorrelationId(msg.getCorrelationId());
          Contexts.update(context,  httpMessage.getCorrelationId());
        }catch(final Exception ex){
          ;
        }
      }
      msg.setCorrelationId( httpMessage.getCorrelationId( ) );
      httpMessage.setMessage( msg );
    }
  }
}
