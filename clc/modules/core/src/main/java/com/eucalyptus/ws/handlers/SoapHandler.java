/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import io.vavr.control.Option;

@ChannelHandler.Sharable
public class SoapHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( SoapHandler.class );
  
  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage message = ( MappingHttpMessage ) event.getMessage( );
      final SOAPEnvelope env = message.getSoapEnvelope( );
      if ( !env.hasFault( ) ) {
        message.setOmMessage( env.getBody( ).getFirstElement( ) );
      } else {
        final Supplier<Integer> statusCodeSupplier = () -> getStatus( message );
        IoSoapHandler.perhapsFault( env, statusCodeSupplier );
      }
    }
  }
  
  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      final Option<Pair<SOAPEnvelope,Integer>> soapEnvelopeOption =
          IoSoapHandler.perhapsBuildFault( httpMessage.getMessage( ) );
      if ( soapEnvelopeOption.isDefined( ) ) {
        httpMessage.setSoapEnvelope( soapEnvelopeOption.get( ).getLeft( ) );
        if ( httpMessage instanceof MappingHttpResponse ) {
          ( ( MappingHttpResponse ) httpMessage ).setStatus( HttpResponseStatus.valueOf( soapEnvelopeOption.get( ).getRight( ) ) );
        }
      } else {
        httpMessage.setSoapEnvelope( IoSoapHandler.buildSoapEnvelope( httpMessage.getOmMessage() ) );
      }
    }
  }

  @Nullable
  private static Integer getStatus( final MappingHttpMessage message ) {
    return message instanceof MappingHttpResponse ?
        ((MappingHttpResponse) message).getStatus( ).getCode( ) :
        null;
  }
}
