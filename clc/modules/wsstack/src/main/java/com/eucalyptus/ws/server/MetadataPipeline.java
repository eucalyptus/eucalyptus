package com.eucalyptus.ws.server;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
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
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;

@ChannelPipelineCoverage( "one" )
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
                                             "  <h1>404 - Not Found: failed to find %s</h1>\n" +
                                             "  <pre>%s</pre>\n" +
                                             " </body>\n" +
                                             "</html>\n";
  private static Logger       LOG          = Logger.getLogger( MetadataPipeline.class );
  
  public MetadataPipeline( ) {
    super( );
  }
  
  @Override
  public boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).matches( "/latest(/.*)*" ) || message.getUri( ).matches( "/\\d\\d\\d\\d-\\d\\d-\\d\\d/.*" );
  }
  
  @Override
  public String getName( ) {
    return "metadata-pipeline";
  }
  
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ( ( MessageEvent ) e ).getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest request = ( MappingHttpRequest ) ( ( MessageEvent ) e ).getMessage( );
      String newUri = null;
      String uri = request.getUri( );
      InetSocketAddress remoteAddr = ( ( InetSocketAddress ) ctx.getChannel( ).getRemoteAddress( ) );
      String remoteHost = remoteAddr.getAddress( ).getHostAddress( );//"10.1.1.2";//
      if ( uri.startsWith( "/latest/" ) )
        newUri = uri.replaceAll( "/latest/", remoteHost + ":" );
      else newUri = uri.replaceAll( "/\\d\\d\\d\\d-\\d\\d-\\d\\d/", remoteHost + ":" );
      
      HttpResponse response = null;
      LOG.trace( "Trying to get metadata: " + newUri );
      Object reply = "".getBytes( );
      Exception replyEx = null;
      try {
        if ( Bootstrap.isShuttingDown( ) ) {
          reply = "System shutting down".getBytes( );
        } else if ( !Bootstrap.isFinished( ) ) {
          reply = "System is still starting up".getBytes( );
        } else {
          reply = ServiceContext.send( "VmMetadata", newUri );
        }
      } catch ( ServiceDispatchException e1 ) {
        Logs.extreme( ).debug( e1, e1 );
        replyEx = e1;
      } catch ( Exception e1 ) {
        Logs.extreme( ).debug( e1, e1 );
        replyEx = e1;
      } finally {
        Contexts.clear( request.getCorrelationId( ) );
      }
      Logs.extreme( ).debug( "VmMetadata reply info: " + reply + " " + replyEx );
      if ( replyEx != null || reply == null || reply instanceof NullPayload ) {
        response = new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.NOT_FOUND );
        String errorMessage = String.format(
          ERROR_STRING,
          newUri.replaceAll( remoteHost + ":", "" ),
          replyEx != null && Logs.isDebug( ) ? Exceptions.string( replyEx ) : "" );
        response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain" );
        ChannelBuffer buffer = null;
        if ( replyEx != null && !( replyEx instanceof NoSuchElementException ) ) {
          buffer = ChannelBuffers.wrappedBuffer( errorMessage.getBytes( ) );
          response.setContent( buffer );
        } else {
          buffer = ChannelBuffers.wrappedBuffer( errorMessage.getBytes( ) );
          response.setContent( buffer );
        }
        response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString( buffer.readableBytes( ) ) );
      } else {
        response = new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK );
        response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain" );
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( ( byte[] ) reply );
        response.setContent( buffer );
        response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, Integer.toString( buffer.readableBytes( ) ) );
      }
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
