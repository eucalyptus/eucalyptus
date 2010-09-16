package com.eucalyptus.ws.server;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.transport.NullPayload;

import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.util.Messaging;

@ChannelPipelineCoverage( "one" )
public class MetadataPipeline extends FilteredPipeline implements UnrollableStage, ChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( MetadataPipeline.class );
  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( this );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).matches("/latest(/.*)*") || message.getUri( ).matches("/\\d\\d\\d\\d-\\d\\d-\\d\\d/.*");
  }

  @Override
  public String getPipelineName( ) {
    return "instance-metadata";
  }

  @Override
  public String getStageName( ) {
    return "instance-metadata";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "instance-metadata", this );
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if( e instanceof MessageEvent && ( (MessageEvent ) e).getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest request = ( MappingHttpRequest ) ((MessageEvent) e).getMessage( );
      String newUri = null;
      String uri = request.getUri( );
      InetSocketAddress remoteAddr = ((InetSocketAddress) ctx.getChannel( ).getRemoteAddress( ) );
      String remoteHost = remoteAddr.getAddress( ).getHostAddress( );//"10.1.1.2";//
      if( uri.startsWith( "/latest/" ) )
        newUri = uri.replaceAll( "/latest/", remoteHost + ":" );
      else
        newUri = uri.replaceAll( "/\\d\\d\\d\\d-\\d\\d-\\d\\d/", remoteHost + ":" );

      HttpResponse response = null;
      LOG.info( "Trying to get metadata: " + newUri );
      Object reply = null;
      try {
        reply = ServiceContext.send( "VmMetadata", newUri );
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
      } finally {
        Contexts.clear( request.getCorrelationId( ) );
      }
      if ( reply != null && !( reply instanceof NullPayload ) ) {
        response = new DefaultHttpResponse(request.getProtocolVersion( ),HttpResponseStatus.OK);
        response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/html" );
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( ( byte[] ) reply );
        response.setContent( buffer );
        response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString( buffer.readableBytes( ) ) );
      }
      else
        response = new DefaultHttpResponse(request.getProtocolVersion( ),HttpResponseStatus.NOT_FOUND);
      ctx.getChannel( ).write( response ).addListener( ChannelFutureListener.CLOSE );
    } else {
      ctx.sendUpstream( e );
    }
  }

}
