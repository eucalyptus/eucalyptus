/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.ws.client;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.crypto.util.SslSetup;
import com.eucalyptus.ws.IoHandlers;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

@ComponentPart( ComponentId.class )
public class InternalClientChannelInitializer extends MonitoredSocketChannelInitializer {

  @Override
  protected void initChannel( final SocketChannel channel ) throws Exception {
    super.initChannel( channel );
    final ChannelPipeline pipeline = channel.pipeline( );
    if ( MoreObjects.firstNonNull( SslSetup.CLIENT_HTTPS_ENABLED, true ) ) {
      pipeline.addLast( "https", IoHandlers.internalHttpsHandler( ) );
    }
    pipeline.addLast( "decoder", IoHandlers.httpResponseDecoder( ) );
    pipeline.addLast( "aggregator", IoHandlers.newHttpChunkAggregator( ) );
    pipeline.addLast( "encoder", IoHandlers.httpRequestEncoder( ) );
    pipeline.addLast( "wrapper", IoHandlers.ioMessageWrappingHandler( ) );
    if ( MoreObjects.firstNonNull( StackConfiguration.CLIENT_INTERNAL_HMAC_SIGNATURE_ENABLED, false ) ) {
      pipeline.addLast( "hmac", IoHandlers.getInternalHmacHandler( ) );
      pipeline.addLast( "serializer", IoHandlers.soapMarshalling( ) );
    } else {
      pipeline.addLast( "serializer", IoHandlers.soapMarshalling( ) );
      pipeline.addLast( "wssec", IoHandlers.internalWsSecHandler( ) );
    }
    pipeline.addLast( "addressing", IoHandlers.addressingHandler( ) );
    pipeline.addLast( "soap", IoHandlers.soapHandler( ) );
    pipeline.addLast( "binding", IoHandlers.bindingHandler( ) );
  }
}
