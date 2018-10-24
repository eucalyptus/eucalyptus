/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.compute.metadata;

import java.net.InetSocketAddress;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

@ComponentPart( Eucalyptus.class )
public class MetadataPipeline extends FilteredPipeline implements ChannelUpstreamHandler {
  private static final String ERROR_STRING = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                                             "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
                                             "         \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                                             "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
                                             " <head>\n" +
                                             "  <title>404 - Not Found</title>\n" +
                                             " </head>\n" +
                                             " <body>\n" +
                                             "  <h1>404 - Not Found</h1>\n" +
                                             " </body>\n" +
                                             "</html>\n";
  /**
   * Metadata versions are not necessarily API versions
   */
  private static final Set<String> VERSION = ImmutableSet.of( "1.0", "2007-01-19", "2007-03-01", "2007-08-29",
                                               "2007-10-10", "2007-12-15", "2008-02-01", "2008-09-01", "2009-04-04",
                                               "2011-01-01", "2011-05-01", "2012-01-12", "2014-02-25", "latest" );

  private static final String INSTANCE_ID_HEADER = "Euca-Instance-Id";

  private static Logger       LOG          = Logger.getLogger( MetadataPipeline.class );
  
  public MetadataPipeline( ) {
    super( );
  }
  
  @Override
  public boolean checkAccepts( HttpRequest message ) {
    return
        message.getUri( ).matches( "/latest(/.*)*|/\\d\\d\\d\\d-\\d\\d-\\d\\d/.*|/1.0/.*" ) ||
        ("/".equals( message.getUri( ) ) && "169.254.169.254".equals( message.getHeader( HttpHeaders.Names.HOST ) ) );
  }
  
  @Override
  public String getName( ) {
    return "metadata-pipeline";
  }
  
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ( ( MessageEvent ) e ).getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest request = ( MappingHttpRequest ) ( ( MessageEvent ) e ).getMessage( );
      final String uri = request.getUri( );
      final InetSocketAddress remoteAddr = ( ( InetSocketAddress ) ctx.getChannel( ).getRemoteAddress( ) );
      String remoteHostOrInstanceId = remoteAddr.getAddress( ).getHostAddress( );
      if ( "127.0.0.1".equals( remoteHostOrInstanceId ) &&
          request.containsHeader( INSTANCE_ID_HEADER ) &&
          request.getHeader( INSTANCE_ID_HEADER ).matches( "i-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ) ) {
        remoteHostOrInstanceId = request.getHeader( INSTANCE_ID_HEADER );
      }
      final String newUri;
      if ( uri.startsWith( "/latest/" ) ) {
        newUri = uri.replaceAll( "/latest[/]+", remoteHostOrInstanceId + ":" );
      } else if ( uri.startsWith( "/1.0/" ) ) {
        newUri = uri.replaceAll( "/1.0[/]+", remoteHostOrInstanceId + ":" );
      } else {
        newUri = uri.replaceAll( "/\\d\\d\\d\\d-\\d\\d-\\d\\d[/]+", remoteHostOrInstanceId + ":" );
      } 

      LOG.trace( "Trying to get metadata: " + newUri );
      Object reply = "".getBytes( );
      Exception replyEx = null;
      if ( uri.equals( "/" ) || uri.isEmpty( ) ){
        reply = Joiner.on('\n').join( VERSION).getBytes( Charsets.UTF_8 );
      } else {
        try {
          if ( Bootstrap.isShuttingDown( ) ) {
            reply = "System shutting down".getBytes( );
          } else if ( !Bootstrap.isFinished( ) ) {
            reply = "System is still starting up".getBytes( );
          } else {
            reply = ServiceContext.send( "eucalyptus-vmmetadata-request", newUri ).get( );
          }
        } catch ( Exception e1 ) {
          Logs.extreme( ).debug( e1, e1 );
          replyEx = e1;
        } finally {
          Contexts.clear( request.getCorrelationId( ) );
        }
      }

      if ( Logs.extreme( ).isDebugEnabled( ) ) {
        Logs.extreme( ).debug( "VmMetadata reply info: " + reply + " " + replyEx );
      }
      HttpResponse response;
      if ( replyEx == null && reply instanceof byte[] ) {
        response = new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK );
        response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain" );
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( ( byte[] ) reply );
        response.setContent( buffer );
        response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString( buffer.readableBytes( ) ) );
      } else {
        response = new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.NOT_FOUND );
        response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain" );
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( ERROR_STRING.getBytes( ) );
        response.setContent( buffer );
        response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString( buffer.readableBytes( ) ) );
      }
      response.setHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
      ctx.getChannel( ).write( response ).addListener( ChannelFutureListener.CLOSE );
    } else {
      ctx.sendUpstream( e );
    }
  }
  
  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    pipeline.addLast( "instance-metadata", this );
    return pipeline;
  }
  
}
