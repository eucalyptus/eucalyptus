package com.eucalyptus.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import com.eucalyptus.util.HasName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class StatefulNamedRegistry<T extends HasName<T>, E extends Enum<E>> {
  private static Logger                             LOG = Logger.getLogger( StatefulNamedRegistry.class );
  private Map<E, ConcurrentNavigableMap<String, T>> stateMaps;
  private ConcurrentNavigableMap<String, T>         activeMap;
  private E[]                                       states;
  private ReadWriteLock                             canHas;
  
  public StatefulNamedRegistry( E... states ) {
    super( );
    this.activeMap = new ConcurrentSkipListMap<String, T>( );
    this.states = states;// hack.
    this.canHas = new ReentrantReadWriteLock( );
    if ( this.states.length > 1 ) {
      this.stateMaps = new HashMap<E, ConcurrentNavigableMap<String, T>>( );
      for ( int i = 0; i < this.states.length; i++ ) {
        this.stateMaps.put( ( E ) states[i], new ConcurrentSkipListMap<String, T>( ) );
      }
    }
  }
  
  public boolean isRegistered( String name ) {
    this.canHas.readLock( ).lock( );
    try {
      for ( Map<String, T> m : this.stateMaps.values( ) )
        if ( this.activeMap.containsKey( name ) ) return true;
      return false;
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public T remove( String name ) {
    T oldValue = null;
    this.canHas.writeLock( ).lock( );
    try {
      for ( Map<String, T> m : this.stateMaps.values( ) ) {
        oldValue = m.remove( name );
        if ( oldValue != null ) return oldValue;
      }
      throw new NoSuchElementException(
        "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
    } finally {
      this.canHas.writeLock( ).unlock( );
      try {
        ListenerRegistry.getInstance( ).fireEvent( new StateEvent<T, E>( this.states[0], oldValue ) );
      } catch ( EventVetoedException e ) {
        LOG.warn( "Registry change was vetoed: " + e, e );
      }
    }
  }
  
  public T deregister( String key ) {
    return this.remove( key );
  }
  
  public T register( T obj, E initialState ) {
    T oldValue = null;
    if ( obj == null ) {
      throw new IllegalArgumentException( "Value cannot be null: " + obj );
    }
    this.canHas.writeLock( ).lock( );
    try {
      oldValue = this.lookup( obj.getName( ) );
      E oldState = this.getState( oldValue.getName( ) );
      this.stateMaps.get( oldState ).remove( obj.getName( ) );
    } catch ( NoSuchElementException e ) {
      this.stateMaps.get( initialState ).put( obj.getName( ), obj );
    } finally {
      this.canHas.writeLock( ).unlock( );
      try {
        ListenerRegistry.getInstance( ).fireEvent( new StateEvent<T, E>( initialState, obj ) );
      } catch ( EventVetoedException e ) {
        LOG.warn( "Registry change was vetoed: " + e, e );
      }
    }
    return oldValue;
  }
  
  public T registerIfAbsent( T obj, E initialState ) {
    T oldValue = null;
    if ( obj == null ) {
      throw new IllegalArgumentException( "Value cannot be null: " + obj );
    }
    this.canHas.writeLock( ).lock( );
    try {
      return this.lookup( obj.getName( ) );
    } catch ( NoSuchElementException e ) {
      this.stateMaps.get( initialState ).putIfAbsent( obj.getName( ), obj );
    } finally {
      this.canHas.writeLock( ).unlock( );
      try {
        ListenerRegistry.getInstance( ).fireEvent( new StateEvent<T, E>( initialState, obj ) );
      } catch ( EventVetoedException e ) {
        LOG.warn( "Registry change was vetoed: " + e, e );
      }
    }
    return oldValue;
  }
  
  private E getState( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      for ( Entry<E, ConcurrentNavigableMap<String, T>> e : this.stateMaps.entrySet( ) ) {
        if ( e.getValue( ).containsKey( name ) ) {
          return e.getKey( );
        }
      }
      throw new NoSuchElementException(
        "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public T lookup( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      for ( Map<String, T> m : this.stateMaps.values( ) ) {
        if ( m.containsKey( name ) ) {
//          LOG.debug( m.get( name ) );
          return m.get( name );
        }
      }
      throw new NoSuchElementException(
        "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public void setState( String name, E newState ) throws NoSuchElementException {
    T value = null;
    this.canHas.writeLock( ).lock( );
    try {
      value = this.remove( name );
      this.stateMaps.get( newState ).put( name, value );
    } finally {
      this.canHas.writeLock( ).unlock( );
      try {
        ListenerRegistry.getInstance( ).fireEvent( new StateEvent<T, E>( newState, value ) );
      } catch ( EventVetoedException e ) {
        LOG.warn( "Registry change was vetoed: " + e, e );
      }
    }
  }
  
  public boolean contains( String name ) {
    this.canHas.readLock( ).lock( );
    try {
      for ( Map m : this.stateMaps.values( ) ) {
        if ( m.containsKey( name ) ) {
          return true;
        }
      }
      return false;
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public ImmutableMap<String, T> getMap( E state ) {
    return ImmutableMap.copyOf( this.stateMaps.get( state ) );
  }
  
  public ImmutableList<String> listKeys( ) {
    List<String> keyList = Lists.newArrayList( );
    for ( Map<String, T> m : this.stateMaps.values( ) ) {
      keyList.addAll( m.keySet( ) );
    }
    return ImmutableList.copyOf( keyList );
  }
  
  public ImmutableList<T> listValues( ) {
    List<T> valueList = Lists.newArrayList( );
    for ( Map<String, T> m : this.stateMaps.values( ) ) {
      valueList.addAll( m.values( ) );
    }
    return ImmutableList.copyOf( valueList );
  }
  
  public ImmutableList<String> listKeys( E state ) {
    return ImmutableList.copyOf( this.stateMaps.get( state ).keySet( ) );
  }
  
  public ImmutableList<T> listValues( E state ) {
    return ImmutableList.copyOf( this.stateMaps.get( state ).values( ) );
  }
}
