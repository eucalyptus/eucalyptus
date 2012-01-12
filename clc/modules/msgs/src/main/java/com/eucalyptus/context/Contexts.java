package com.eucalyptus.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import com.eucalyptus.BaseException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.ws.util.ReplyQueue;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.HasRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class Contexts {
  private static Logger                          LOG             = Logger.getLogger( Contexts.class );
  private static int                             MAX             = 8192;
  private static int                             CONCUR          = MAX / ( Runtime.getRuntime( ).availableProcessors( ) * 2 + 1 );
  private static float                           THRESHOLD       = 1.0f;
  private static ConcurrentMap<String, Context>  uuidContexts    = new ConcurrentHashMap<String, Context>( MAX, THRESHOLD, CONCUR );
  private static ConcurrentMap<Channel, Context> channelContexts = new ConcurrentHashMap<Channel, Context>( MAX, THRESHOLD, CONCUR );
  
  static boolean hasOutstandingRequests( ) {
    return uuidContexts.keySet( ).size( ) > 0;
  }
  
  public static Context create( MappingHttpRequest request, Channel channel ) {
    Context ctx = new Context( request, channel );
    request.setCorrelationId( ctx.getCorrelationId( ) );
    uuidContexts.put( ctx.getCorrelationId( ), ctx );
    channelContexts.put( channel, ctx );
    return ctx;
  }
  
  public static boolean exists( ) {
    try {
      lookup( );
      return true;
    } catch ( IllegalContextAccessException ex ) {
      return false;
    }
  }
  public static boolean exists( Channel channel ) {
    return channelContexts.containsKey( channel );
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
  
  public static boolean exists( String correlationId ) {
    return correlationId != null && uuidContexts.containsKey( correlationId );
  }
  
  private static ThreadLocal<Context> tlContext = new ThreadLocal<Context>( );
  
  public static void threadLocal( Context ctx ) {//GRZE: really unhappy these are public.
    tlContext.set( ctx );
  }
  
  public static void removeThreadLocal( ) {//GRZE: really unhappy these are public.
    tlContext.remove( );
  }
  
  public static Context lookup( String correlationId ) throws NoSuchContextException {
    assertThat( "BUG: correlationId is null.", correlationId, notNullValue( ) );
    if ( !uuidContexts.containsKey( correlationId ) ) {
      throw new NoSuchContextException( "Found correlation id " + correlationId + " but no corresponding context." );
    } else {
      Context ctx = uuidContexts.get( correlationId );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return ctx;
    }
  }
  
  public static final Context lookup( ) throws IllegalContextAccessException {
    Context ctx;
    if ( ( ctx = tlContext.get( ) ) != null ) {
      return ctx;
    }
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
      try {
        return Contexts.lookup( ( ( BaseMessage ) o ).getCorrelationId( ) );
      } catch ( NoSuchContextException e ) {
        Logs.exhaust( ).error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else if ( o != null && o instanceof HasRequest ) {
      try {
        return Contexts.lookup( ( ( HasRequest ) o ).getRequest( ).getCorrelationId( ) );
      } catch ( NoSuchContextException e ) {
        Logs.exhaust( ).error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request." );
    }
  }
  
  public static void clear( String corrId ) {
    assertThat( "BUG: correlationId is null.", corrId, notNullValue( ) );
    Context ctx = uuidContexts.remove( corrId );
    Channel channel = null;
    if ( ctx != null && ( channel = ctx.getChannel( ) ) != null ) {
      channelContexts.remove( channel );
    } else {
      LOG.debug( "Context.clear() failed for correlationId=" + corrId, new RuntimeException( "Missing reference to channel for the request." ) );
    }
    if ( ctx != null ) {
      ctx.clear( );
    }
  }
  
  public static void clear( Context context ) {
    if ( context != null ) {
      clear( context.getCorrelationId( ) );
    }
  }
  
  public static Context createWrapped( String dest, final BaseMessage msg ) {
    if ( uuidContexts.containsKey( msg.getCorrelationId( ) ) ) {
      return null;
    } else {
      Context ctx = new Context( dest, msg );
      uuidContexts.put( ctx.getCorrelationId( ), ctx );
      return ctx;
    }
  }

  @SuppressWarnings( "unchecked" )
  public static void response( BaseMessage responseMessage ) {
    if ( responseMessage instanceof ExceptionResponseType ) {
      Logs.exhaust( ).trace( responseMessage );
    }
    String corrId = responseMessage.getCorrelationId( );
    try {
      Context ctx = lookup( corrId );
      EventRecord.here( ServiceContext.class, EventType.MSG_REPLY, responseMessage.getCorrelationId( ), responseMessage.getClass( ).getSimpleName( ),
                        String.format( "%.3f ms", ( System.nanoTime( ) - ctx.getCreationTime( ) ) / 1000000.0 ) ).trace( );
      Channel channel = ctx.getChannel( );
      Channels.write( channel, responseMessage );
      clear( ctx );
    } catch ( NoSuchContextException e ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message: " + e.getMessage( ) );
      Logs.extreme( ).debug( responseMessage, e );
    } catch ( Exception e ) {
      LOG.warn( "Error occurred while handling reply: " + responseMessage );
      Logs.extreme( ).debug( responseMessage, e );
    }
  }

  public static void responseError( Throwable cause ) {
    Contexts.responseError( lookup( ).getCorrelationId( ), cause );
  }

  public static void responseError( String corrId, Throwable cause ) {
    try {
      Context ctx = lookup( corrId );
      EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, cause.getClass( ).getCanonicalName( ), cause.getMessage( ),
                        String.format( "%.3f ms", ( System.nanoTime( ) - ctx.getCreationTime( ) ) / 1000000.0 ) ).trace( );
      Channels.fireExceptionCaught( ctx.getChannel( ), cause );
      if ( !( cause instanceof BaseException ) ) {
        clear( ctx );
      }
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( cause, cause );
    }
  }
  
}
