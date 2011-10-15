package com.eucalyptus.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;

public class ListenerRegistry {
  private static Logger                                           LOG       = Logger.getLogger( ListenerRegistry.class );
  private static ListenerRegistry                                 singleton = getInstance( );
  private final Map<Class, ReentrantListenerRegistry>             registryMap;
  private final ReentrantListenerRegistry<Class<? extends Event>> eventMap;
  
  public static ListenerRegistry getInstance( ) {
    synchronized ( ListenerRegistry.class ) {
      if ( singleton == null ) singleton = new ListenerRegistry( );
    }
    return singleton;
  }
  
  @SuppressWarnings( "unchecked" )
  public ListenerRegistry( ) {
    super( );
    this.registryMap = Maps.newHashMap( );
    this.eventMap = new ReentrantListenerRegistry<Class<? extends Event>>( );
    this.registryMap.put( ComponentId.class, new ReentrantListenerRegistry<ComponentId>( ) );
    this.registryMap.put( String.class, new ReentrantListenerRegistry<String>( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  public void register( final Object type, final EventListener listener ) {
    final List<Class<?>> lookupTypes = Classes.genericsToClasses( listener );
    lookupTypes.remove( Event.class );
    /** GRZE: event type is not specified by the generic type of listeners EventListener decl or is <Event> **/
    boolean illegal = ( type == null && lookupTypes.isEmpty( ) );
    /** GRZE: explicit event type does conform to generic type **/
    illegal |= ( type != null
                 && !lookupTypes.contains( Event.class )
                 && !lookupTypes.get( 0 ).isAssignableFrom( Classes.typeOf( type ) ) );
    if ( illegal ) {
      throw Exceptions.fatal( new IllegalArgumentException( "Failed to register listener " + listener.getClass( ).getCanonicalName( )
                                                            + " because the declared generic type " + lookupTypes
                                                            + " is not assignable from the provided event type: " + ( type != null
                                                              ? type.getClass( ).getCanonicalName( )
                                                              : "null" ) ) );
    } else {
      if ( ( type instanceof Class ) && Event.class.isAssignableFrom( ( Class ) type ) ) {
        this.eventMap.register( ( Class ) type, listener );
      } else {
        if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
          this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
        }
        this.registryMap.get( type.getClass( ) ).register( type, listener );
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public void deregister( final Object type, final EventListener listener ) {
    if ( ( type instanceof Class ) && Event.class.isAssignableFrom( ( Class ) type ) ) {
      this.eventMap.deregister( ( Class ) type, listener );
    } else {
      if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
        this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
      }
      this.registryMap.get( type.getClass( ) ).deregister( type, listener );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public void destroy( final Object type ) {
    if ( ( type instanceof Class ) && Event.class.isAssignableFrom( ( Class ) type ) ) {
      this.eventMap.destroy( ( Class ) type );
    } else {
      if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
        this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
      }
      this.registryMap.get( type.getClass( ) ).destroy( type );
    }
  }
  
  public void fireEvent( final Event e ) throws EventFailedException {
    this.eventMap.fireEvent( e.getClass( ), e );
  }
  
  @SuppressWarnings( "unchecked" )
  public void fireEvent( final Object type, final Event e ) throws EventFailedException {
    if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
      this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
    }
    this.registryMap.get( type.getClass( ) ).fireEvent( type, e );
  }
  
  public Future<Throwable> fireEventAsync( final Object type, final Event e ) {
    return Threads.lookup( Empyrean.class, ListenerRegistry.class, type.getClass( ).getCanonicalName( ) ).submit( new Callable<Throwable>( ) {
      
      @Override
      public Throwable call( ) throws Exception {
        try {
          ListenerRegistry.this.fireEvent( type, e );
          return null;
        } catch ( final Exception ex ) {
          LOG.error( ex );
          return ex;
        }
      }
    } );
  }
  
}
