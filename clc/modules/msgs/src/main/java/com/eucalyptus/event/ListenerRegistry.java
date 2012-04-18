package com.eucalyptus.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.records.Record;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

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
  public void register( Object type, final EventListener listener ) {
    final List<Class> lookupTypes = Classes.genericsToClasses( listener );
    lookupTypes.remove( Event.class );
    /**
     * GRZE: event type is not specified by the generic type of listeners EventListener decl or is
     * <Event>
     **/
    boolean illegal = ( type == null && lookupTypes.isEmpty( ) );
    /** GRZE: explicit event type does conform to generic type **/
    for ( Class<?> c : lookupTypes ) {
      if ( type != null && c.isAssignableFrom( ( type instanceof Class )
        ? ( Class ) type
        : type.getClass( ) ) ) {
        illegal = false;
        break;
      }
    }
    if ( illegal ) {
      throw Exceptions.error( new IllegalArgumentException( "Failed to register listener " + listener.getClass( ).getCanonicalName( )
                                                            + " because the declared generic type " + lookupTypes
                                                            + " is not assignable from the provided event type: " + ( type != null
                                                              ? type.getClass( ).getCanonicalName( )
                                                              : "null" ) ) );
    } else {
      Class key = ( type == null
        ? lookupTypes.get( 0 )
        : ( type instanceof Class
          ? ( Class ) type
          : type.getClass( ) ) );
      if ( Event.class.isAssignableFrom( key ) ) {
        this.eventMap.register( key, listener );
      } else {
        if ( !this.registryMap.containsKey( key ) ) {
          this.registryMap.put( key, new ReentrantListenerRegistry( ) );
        }
        this.registryMap.get( key ).register( type, listener );
      }
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public void deregister( final Object type, final EventListener listener ) {
    final List<Class> lookupTypes = Classes.genericsToClasses( listener );
    lookupTypes.remove( Event.class );
    /**
     * GRZE: event type is not specified by the generic type of listeners EventListener decl or is
     * <Event>
     **/
    boolean illegal = ( type == null && lookupTypes.isEmpty( ) );
    for ( Class<?> c : lookupTypes ) {
      if ( type != null && c.isAssignableFrom( ( type instanceof Class )
        ? ( Class ) type
        : type.getClass( ) ) ) {
        illegal = false;
        break;
      }
    }
    if ( illegal ) {
      throw Exceptions.error( new IllegalArgumentException( "Failed to register listener " + listener.getClass( ).getCanonicalName( )
                                                            + " because the declared generic type " + lookupTypes
                                                            + " is not assignable from the provided event type: " + ( type != null
                                                              ? type.getClass( ).getCanonicalName( )
                                                              : "null" ) ) );
    } else {
      if ( ( type instanceof Class ) && Event.class.isAssignableFrom( ( Class ) type ) ) {
        this.eventMap.deregister( ( Class ) type, listener );
      } else {
        if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
          this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
        }
        this.registryMap.get( type.getClass( ) ).deregister( type, listener );
      }
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
  
  public Future<Event> fireEventAsync( final Object type, final Event e ) {
    return Threads.enqueue( ServiceConfigurations.createEphemeral( Empyrean.INSTANCE ), 32, new Callable<Event>( ) {
      
      @Override
      public Event call( ) throws Exception {
        try {
          ListenerRegistry.this.fireEvent( type, e );
          return e;
        } catch ( final Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          throw ex;
        }
      }
    } );
  }
  
  public static class ReentrantListenerRegistry<T> {
    private Multimap<T, EventListener> listenerMap;
    private Lock                       modificationLock;
    
    public ReentrantListenerRegistry( ) {
      super( );
      this.listenerMap = ArrayListMultimap.create( );
      this.modificationLock = new ReentrantLock( );
    }
    
    public void register( T type, EventListener listener ) {
      if ( type instanceof Enum ) {
        EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_REGISTERED, type.getClass( ).getSimpleName( ), ( ( Enum ) type ).name( ),
                            listener.getClass( ).getSimpleName( ) ).trace( );
      } else {
        EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_REGISTERED, type.getClass( ).getSimpleName( ),
                            listener.getClass( ).getSimpleName( ) ).trace( );
      }
      this.modificationLock.lock( );
      try {
        if ( !this.listenerMap.containsEntry( type, listener ) ) {
          this.listenerMap.put( type, listener );
        }
      } finally {
        this.modificationLock.unlock( );
      }
    }
    
    public void deregister( T type, EventListener listener ) {
      if ( type instanceof Enum ) {
        EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DEREGISTERED, type.getClass( ).getSimpleName( ), ( ( Enum ) type ).name( ),
                            listener.getClass( ).getSimpleName( ) ).trace( );
      } else {
        EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DEREGISTERED, type.getClass( ).getSimpleName( ),
                            listener.getClass( ).getSimpleName( ) ).trace( );
      }
      this.modificationLock.lock( );
      try {
        this.listenerMap.remove( type, listener );
      } finally {
        this.modificationLock.unlock( );
      }
    }
    
    public void destroy( T type ) {
      this.modificationLock.lock( );
      for ( EventListener e : this.listenerMap.get( type ) ) {
        EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DESTROY_ALL, type.getClass( ).getSimpleName( ),
                            e.getClass( ).getCanonicalName( ) ).trace( );
      }
      try {
        this.listenerMap.removeAll( type );
      } finally {
        this.modificationLock.unlock( );
      }
    }
    
    public void fireEvent( T type, Event e ) throws EventFailedException {
      List<EventListener> listeners;
      this.modificationLock.lock( );
      try {
        listeners = Lists.newArrayList( this.listenerMap.get( type ) );
      } finally {
        this.modificationLock.unlock( );
      }
      this.fireEvent( e, listeners );
    }
    
    private void fireEvent( Event e, List<EventListener> listeners ) throws EventFailedException {
      List<Throwable> errors = Lists.newArrayList( );
      for ( EventListener ce : listeners ) {
        EventRecord.here( ReentrantListenerRegistry.class, EventType.LISTENER_EVENT_FIRED, ce.getClass( ).getSimpleName( ), e.toString( ) ).trace( );
        try {
          ce.fireEvent( e );
        } catch ( Exception ex ) {
          EventFailedException eventEx = new EventFailedException( "Failed to fire event: listener=" + ce.getClass( ).getCanonicalName( ) + " event="
                                                                   + e.toString( ) + " because of: "
                                                                   + ex.getMessage( ), Exceptions.filterStackTrace( ex ) );
          errors.add( eventEx );
        }
      }
      for ( Throwable ex : errors ) {
        Logs.extreme( ).error( ex, ex );
        LOG.error( ex );
      }
    }
    
  }
}
