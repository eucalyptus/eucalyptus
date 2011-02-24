package com.eucalyptus.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Assertions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Contexts {
  private static Logger                          LOG             = Logger.getLogger( Contexts.class );
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
    clear( context.getCorrelationId( ) );
  }

  public static void child( String parentId, String childId ) {
    Context ctx;
    if( ( ctx = uuidContexts.get( parentId ) ) != null ) {
      final Context parentCtxRef = ctx;
      uuidContexts.put( childId, new Context( parentCtxRef.getHttpRequest( ), parentCtxRef.getChannel( ) ) {
        private final Context pctx = parentCtxRef; 

      @Override
      void clear( ) {
        if( uuidContexts.containsKey( parentCtxRef.getCorrelationId( ) ) ) {
          try {
            Contexts.clear( parentCtxRef.getCorrelationId( ) );
          } catch ( Exception ex ) {
            LOG.error( ex );
          }
        }
        super.clear( );
      }

      public int hashCode( ) {
        return this.pctx.hashCode( );
      }

      public Channel getChannel( ) {
        return this.pctx.getChannel( );
      }

      public MappingHttpRequest getHttpRequest( ) {
        return this.pctx.getHttpRequest( );
      }

      public String getCorrelationId( ) {
        return this.pctx.getCorrelationId( );
      }

      public Long getCreationTime( ) {
        return this.pctx.getCreationTime( );
      }

      public void setRequest( BaseMessage msg ) {
        this.pctx.setRequest( msg );
      }

      public BaseMessage getRequest( ) {
        return this.pctx.getRequest( );
      }

      public void setUser( User user ) {
        this.pctx.setUser( user );
      }

      public UserFullName getUserFullName( ) {
        return this.pctx.getUserFullName( );
      }

      public UserFullName getEffectiveUserFullName( ) {
        return this.pctx.getEffectiveUserFullName( );
      }

      public boolean equals( Object obj ) {
        return this.pctx.equals( obj );
      }

      public boolean hasAdministrativePrivileges( ) {
        return this.pctx.hasAdministrativePrivileges( );
      }

      public User getUser( ) {
        return this.pctx.getUser( );
      }

      public String getServiceName( ) {
        return this.pctx.getServiceName( );
      }

      public Subject getSubject( ) {
        return this.pctx.getSubject( );
      }

      public void setSubject( Subject subject ) {
        this.pctx.setSubject( subject );
      }

      public Map<String, Contract> getContracts( ) {
        return this.pctx.getContracts( );
      }

      public Account getAccount( ) {
        return this.pctx.getAccount( );
      }

      public String toString( ) {
        return this.pctx.toString( );
      }} );
    } else {
      throw new IllegalContextAccessException( "Cannot access context implicitly using lookup(V) when not handling a request." );
    }
  }
  
}
