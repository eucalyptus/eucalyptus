package com.eucalyptus.event;

import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Maps;
import com.eucalyptus.records.EventRecord;

public class ListenerRegistry {
  private static Logger                                     LOG       = Logger.getLogger( ListenerRegistry.class );
  private static ListenerRegistry                           singleton = getInstance( );
  private Map<Class, ReentrantListenerRegistry>             registryMap;
  private ReentrantListenerRegistry<Class<? extends Event>> eventMap;

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
    this.registryMap.put( Component.class, new ReentrantListenerRegistry<Component>( ) );
    this.registryMap.put( String.class, new ReentrantListenerRegistry<String>( ) );
  }

  @SuppressWarnings( "unchecked" )
  public void register( Object type, EventListener listener ) {
    if ( type instanceof Class && Event.class.isAssignableFrom( (Class)type ) ) {
      this.eventMap.register( (Class) type , listener );
    } else {
      if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
        this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
      }
      this.registryMap.get( type.getClass( ) ).register( type, listener );
    }
  }

  @SuppressWarnings( "unchecked" )
  public void deregister( Object type, EventListener listener ) {
    if ( type instanceof Class && Event.class.isAssignableFrom( (Class)type ) ) {
      this.eventMap.deregister( (Class) type, listener );
    } else {
      if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
        this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
      }
      this.registryMap.get( type.getClass( ) ).deregister( type, listener );
    }
  }

  @SuppressWarnings( "unchecked" )
  public void destroy( Object type ) {
    if ( type instanceof Class && Event.class.isAssignableFrom( (Class) type ) ) {
      this.eventMap.destroy( (Class) type );
    } else {
      if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
        this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
      }
      this.registryMap.get( type.getClass( ) ).destroy( type );
    }
  }

  public void fireEvent( Event e ) throws EventVetoedException {
    this.eventMap.fireEvent( e.getClass( ), e );
  }
  @SuppressWarnings( "unchecked" )
  public void fireEvent( Object type, Event e ) throws EventVetoedException {
    if ( !this.registryMap.containsKey( type.getClass( ) ) ) {
      this.registryMap.put( type.getClass( ), new ReentrantListenerRegistry( ) );
    }
    this.registryMap.get( type.getClass( ) ).fireEvent( type, e );
  }

}
