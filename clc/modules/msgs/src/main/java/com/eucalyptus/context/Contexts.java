package com.eucalyptus.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Assertions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.HasRequest;

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
  
  public static Context lookup( String correlationId ) throws NoSuchContextException {
    Assertions.assertNotNull( correlationId );
    if ( !uuidContexts.containsKey( correlationId ) ) {
      throw new NoSuchContextException( "Found correlation id " + correlationId + " but no corresponding context." );
    } else {
      Context ctx = uuidContexts.get( correlationId );
      ctx.setMuleEvent( RequestContext.getEvent( ) );
      return ctx;
    }
  }
  
  public static Context lookup( ) throws IllegalContextAccessException {
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
        LOG.error( e, e );
        throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
      }
    } else if ( o != null && o instanceof HasRequest ) {
        try {
          return Contexts.lookup( ( ( HasRequest ) o ).getRequest( ).getCorrelationId( ) );
        } catch ( NoSuchContextException e ) {
          LOG.error( e, e );
          throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request.", e );
        }
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request." );
    }
  }
  
  public static void clear( String corrId ) {
    Assertions.assertNotNull( corrId );
    Context ctx = uuidContexts.remove( corrId );
    Channel channel = null;
    if ( ctx != null && ( channel = ctx.getChannel( ) ) != null ) {
      channelContexts.remove( channel );
    } else {
      LOG.debug( "Context.clear() failed for correlationId=" + corrId, new RuntimeException( "Missing reference to channel for the request." ) );
    }
    ctx.clear( );
  }

  public static void clear( Context context ) {
    if( context != null ) {
      clear( context.getCorrelationId( ) );
    }
  }

  public static Context createWrapped( String dest, final BaseMessage msg ) {
    if( uuidContexts.containsKey( msg.getCorrelationId( ) ) ) {
      return null;
    } else {
      Context ctx = new Context( dest, msg );
      uuidContexts.put( ctx.getCorrelationId( ), ctx );    
      return ctx;
    }
  }
  
}
