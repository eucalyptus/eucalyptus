/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
