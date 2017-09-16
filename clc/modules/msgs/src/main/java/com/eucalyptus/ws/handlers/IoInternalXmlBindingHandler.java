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
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.IoMessage;
import com.eucalyptus.ws.WebServicesException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 *
 */
@ChannelHandler.Sharable
public class IoInternalXmlBindingHandler extends ChannelDuplexHandler {

  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msgObj ) throws Exception {
    if ( msgObj instanceof IoMessage ) {
      final IoMessage ioMessage = ( IoMessage ) msgObj;
      final FullHttpMessage httpMessage = ioMessage.getHttpMessage( );
      String type = httpMessage.headers( ).get( "X-Euca-Type" );
      BaseMessage msg;
      try {
        //TODO:STEVE: get component by path?
        if ( type==null && httpMessage instanceof FullHttpRequest ) {
          String servicePath = ((FullHttpRequest)httpMessage).getUri( );
          String simpleType = ioMessage.getSoapEnvelope( ).getSOAPBodyFirstElementLocalName( );
          ComponentId componentId = ComponentIds.list( ).stream( ).filter( cid -> servicePath.equals( cid.getServicePath(  ) ) ).findFirst( ).get( );
          type = ComponentMessages.forComponent( componentId ).stream( ).filter( c -> simpleType.equals( c.getSimpleName( ) ) ).findFirst( ).get( ).getName( );
        }
        final Class<?> typeClass = getClass( ).getClassLoader( ).loadClass( type );
        if ( !BaseMessage.class.isAssignableFrom( typeClass ) ) {
          throw new IllegalArgumentException( "Unsupported type: " + type );
        }
        OMElement elem = ioMessage.getOmMessage( );
        msg = BaseMessages.fromOm( elem, (Class<? extends BaseMessage>) typeClass );
      } catch ( Exception e ) {
        throw new WebServicesException( new BindingException( "Failed to unmarshall type " + type + " caused by: "
            + e.getMessage( ), e ) );
      }

      // in case the base message has request ID in its correlation ID prefix,
      // we should reset the correlation ID using the request ID
      if ( ioMessage.getCorrelationId() != null &&
          msg.getCorrelationId()!=null &&
          msg.hasRequestId()) {
        try{
          final Context context = Contexts.lookup(ioMessage.getCorrelationId());
          // reset correlation ID
          msg.regardingRequestId(msg.getCorrelationId());
          ioMessage.setCorrelationId(msg.getCorrelationId());
          Contexts.update(context,  ioMessage.getCorrelationId());
        }catch(final Exception ex){
          ;
        }
      }
      msg.setCorrelationId( ioMessage.getCorrelationId( ) );
      ioMessage.setMessage( msg );
    }
    super.channelRead( ctx, msgObj );
  }

  @Override
  public void write( final ChannelHandlerContext ctx, final Object msgObj, final ChannelPromise promise ) throws Exception {
    if ( msgObj instanceof IoMessage ) {
      IoMessage ioMessage = ( IoMessage ) msgObj;
      OMElement omElem;
      String type = null;
      if ( ioMessage.getMessage( ) instanceof EucalyptusErrorMessageType || ioMessage.getMessage( ) == null ) {
        return;
      } else if ( ioMessage.getMessage( ) instanceof ExceptionResponseType ) {
        ExceptionResponseType msg = ( ExceptionResponseType ) ioMessage.getMessage( );
        String createFaultDetails = Logs.isExtrrreeeme( )
            ? Exceptions.string( msg.getException( ) )
            : msg.getException( ).getMessage( );
        omElem = Binding.createFault( msg.getRequestType( ), msg.getMessage( ), createFaultDetails );
        if ( ioMessage.getHttpMessage( ) instanceof FullHttpResponse ) {
          ( ( FullHttpResponse ) ioMessage.getHttpMessage( ) ).setStatus( HttpResponseStatus.valueOf( msg.getHttpStatusCode( ) ) );
        }
      } else {
        final Object object = ioMessage.getMessage( );
        try {
          type = ioMessage.getMessage( ).getClass( ).getName( );
          omElem = BaseMessages.toOm( (BaseMessage) object );
        } catch ( Exception e ) {
          Logs.exhaust( ).debug( e, e );
          throw new BindingException( "Failed to marshall type " + object.getClass( ).getCanonicalName( ) + " caused by: "
              + e.getMessage( ), e );
        }
      }
      if ( type != null ) {
        ioMessage.getHttpMessage( ).headers( ).add( "X-Euca-Type", type );
      }
      ioMessage.setOmMessage( omElem );
    }
    super.write( ctx, msgObj, promise );
  }
}
