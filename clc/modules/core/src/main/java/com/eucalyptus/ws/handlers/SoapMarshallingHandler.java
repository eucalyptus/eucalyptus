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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.ImmutableMap;

@ChannelHandler.Sharable
public class SoapMarshallingHandler extends MessageStackHandler implements ExceptionMarshallerHandler {
  private static Logger LOG = Logger.getLogger( SoapMarshallingHandler.class );

  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      final String content;
      final boolean request;
      if ( httpMessage instanceof MappingHttpRequest ) {
        MappingHttpRequest mappingHttpRequest = (MappingHttpRequest) httpMessage;
        content = mappingHttpRequest.getContentAsString( );
        request = true;
      } else {
        MappingHttpResponse mappingHttpResponse = (MappingHttpResponse) httpMessage;
        content = httpMessage.getContent( ).toString( StandardCharsets.UTF_8 );
        request = false;
        if ( content.isEmpty( ) ) {
          httpMessage.setMessageString( "" );
          httpMessage.setSoapEnvelope( Binding.createFault(
              mappingHttpResponse.getStatus( ).getCode( ) < 500 ? "soapenv:Client" : "soapenv:Server",
              "No content",
              mappingHttpResponse.getStatus( ).toString( ) ) );
          return;
        }
      }
      httpMessage.setMessageString( content );
      HoldMe.canHas.lock( );
      SOAPEnvelope env = null;
      try {
        StAXSOAPModelBuilder soapBuilder = null;
        try {
          SOAPFactory factory = HoldMe.getOMSOAP11Factory( );
          soapBuilder = new StAXSOAPModelBuilder( HoldMe.getXMLStreamReader( content ), factory , SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI );
        } catch ( Exception e ) {
          SOAPFactory factory = HoldMe.getOMSOAP12Factory( );
          soapBuilder = new StAXSOAPModelBuilder( HoldMe.getXMLStreamReader( content ), factory , SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI );
        }
        env = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
      } catch( Exception ex ) {
        if ( request ) {
          throw new BindingException( "Invalid request : " + ex.getMessage( ), ex );
        } else {
          env = Binding.createFault(
              "soapenv:Server",
              "Error parsing response",
              content );
        }
      } finally {
        HoldMe.canHas.unlock( );
      }
      httpMessage.setSoapEnvelope( env );
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer( 4096 );
      HoldMe.canHas.lock( );
      try ( final ChannelBufferOutputStream out = new ChannelBufferOutputStream( buffer ) ) {
        httpMessage.getSoapEnvelope( ).serialize( out );//HACK: does this need fixing for xml brokeness?
      } finally {
        HoldMe.canHas.unlock( );
      }
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpMessage.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
      httpMessage.setContent( buffer );

    }
  }

  @Nonnull
  @Override
  public ExceptionResponse marshallException( @Nonnull ChannelEvent event,
                                              @Nonnull final HttpResponseStatus status,
                                              @Nonnull final Throwable t ) throws Exception {
    final SOAPEnvelope soapEnvelope = Binding.createFault(
        status.getCode() < 500 ? "soapenv:Client" : "soapenv:Server",
        t.getMessage(),
        Logs.isExtrrreeeme() ?
            Exceptions.string( t ) :
            t.getMessage() );

    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    HoldMe.canHas.lock();
    try {
      soapEnvelope.serialize( byteOut );
    } finally {
      HoldMe.canHas.unlock();
    }

    return new ExceptionResponse(
        HttpResponseStatus.INTERNAL_SERVER_ERROR,
        ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) ),
        ImmutableMap.of( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" )
    );
  }
}
