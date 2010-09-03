package com.eucalyptus.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import com.eucalyptus.http.MappingHttpRequest;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Contexts {
  private static Logger LOG = Logger.getLogger( Contexts.class );
  private static int                             MAX             = 8192;
  private static int                             CONCUR          = MAX / ( Runtime.getRuntime( ).availableProcessors( ) * 2 + 1 );
  private static float                           THRESHOLD       = 1.0f;
  private static ConcurrentMap<String, Context>  uuidContexts    = new ConcurrentHashMap<String, Context>( MAX, THRESHOLD, CONCUR );
  private static ConcurrentMap<Channel, Context> channelContexts = new ConcurrentHashMap<Channel, Context>( MAX, THRESHOLD, CONCUR );

  public static Context create( MappingHttpRequest request, Channel channel ) {
    Context ctx = new Context( request, channel );
    request.setCorrelationId( ctx.getCorrelationId( ) );
    uuidContexts.put( ctx.getCorrelationId( ), ctx );
    channelContexts.put( channel, ctx );
    return ctx;
  }

  public static Context lookup( Channel channel ) throws NoSuchContextException {
    if ( !channelContexts.containsKey( channel ) ) {
      throw new NoSuchContextException( "Found channel context " + channel + " but no corresponding context." );
    } else {
      Context ctx = channelContexts.get( channel );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return ctx;
    }
  }
  
  public static Context lookup( String correlationId ) throws NoSuchContextException {
    if ( !uuidContexts.containsKey( correlationId ) ) {
      throw new NoSuchContextException( "Found correlation id " + correlationId + " but no corresponding context." );
    } else {
      Context ctx = uuidContexts.get( correlationId );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return ctx;
    }
  }

  public static Context lookup( ) {
    BaseMessage parent = null;
    MuleMessage muleMsg = null;
    if ( RequestContext.getEvent( ) != null && RequestContext.getEvent( ).getMessage( ) != null ) {
      muleMsg = RequestContext.getEvent( ).getMessage( );
    } else if ( RequestContext.getEventContext( ) != null && RequestContext.getEventContext( ).getMessage( ) != null ) {
      muleMsg = RequestContext.getEventContext( ).getMessage( );
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) outside of a service." );
    }
    Object o = muleMsg.getPayload( );
    if ( o != null && o instanceof BaseMessage ) {
      String correlationId = ( ( BaseMessage ) o ).getCorrelationId( );
      try {
        return Contexts.lookup( correlationId );
      } catch ( NoSuchContextException e ) {
        LOG.error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request." );
    }
  }

  public static void clear( Context context ) {
    Context ctx = uuidContexts.remove( context.getCorrelationId( ) );
    Channel channel = null;
    if ( ctx != null && ( channel = ctx.getChannel( ) ) != null ) {
      channelContexts.remove( channel );
    } else {
      throw new RuntimeException( "Missing reference to channel for the request." );
    }
    ctx.clear( );
  }

  public static void clear( String correlationId ) {
    try {
      clear( lookup( correlationId ) );
    } catch ( NoSuchContextException e ) {
      LOG.error( e, e );
    }
  }   
}
