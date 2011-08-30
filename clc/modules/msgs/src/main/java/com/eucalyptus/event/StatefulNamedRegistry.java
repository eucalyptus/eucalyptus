package com.eucalyptus.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import com.eucalyptus.util.HasName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class StatefulNamedRegistry<T extends HasName, E extends Enum<E>> {
  private static Logger LOG = Logger.getLogger( StatefulNamedRegistry.class );
  
  public static class StateEvent<T, E extends Enum<E>> extends GenericEvent<T> {
    private final E state;
    private final E previousState;
    private final T value;
    
    public StateEvent( T value, E state, E previousState ) {
      super( );
      this.value = value;
      this.state = state;
      this.previousState = previousState;
    }
    
    public StateEvent( T value, E state, E previousState, T message ) {
      super( message );
      this.value = value;
      this.state = state;
      this.previousState = previousState;
    }
    
    public E getState( ) {
      return this.state;
    }
    
    public E getPreviousState( ) {
      return this.previousState;
    }
    
  }
  
  private class StatefulValue {
    private final E state;
    private final T value;
    
    public StatefulValue( E state, T value ) {
      super( );
      this.state = state;
      this.value = value;
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.value == null )
        ? 0
        : this.value.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) {
        return true;
      }
      if ( obj == null ) {
        return false;
      }
      if ( getClass( ) != obj.getClass( ) ) {
        return false;
      }
      StatefulValue other = ( StatefulValue ) obj;
      if ( this.value == null ) {
        if ( other.value != null ) {
          return false;
        }
      } else if ( !this.value.equals( other.value ) ) {
        return false;
      }
      return true;
    }
    
    public E getState( ) {
      return this.state;
    }
    
    public T getValue( ) {
      return this.value;
    }
  }
  
  private final ConcurrentNavigableMap<String, StatefulValue> stateMap = new ConcurrentSkipListMap<String, StatefulValue>( );
  private E[]                                                 states;
  private ReadWriteLock                                       canHas;
  
  protected StatefulNamedRegistry( E... states ) {
    super( );
    this.states = states;// hack.
    this.canHas = new ReentrantReadWriteLock( );
  }
  
  public boolean isRegistered( String name ) {
    this.canHas.readLock( ).lock( );
    try {
      return this.stateMap.containsKey( name );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public T deregister( String key ) {
    StatefulValue oldValue = null;
    this.canHas.writeLock( ).lock( );
    try {
      oldValue = this.stateMap.remove( key );
      if ( oldValue != null ) {
        return oldValue.getValue( );
      } else {
        throw new NoSuchElementException( "Can't find registered object: " + key + " in " + this.getClass( ).getSimpleName( ) );
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
      if ( oldValue != null ) {
        this.fireStateChange( oldValue, this.states[0] );
      }
    }
  }
  
  /**
   * @param obj
   * @param nextState
   * @return reference to registered version of obj
   */
  public T register( T obj, E nextState ) {
    StatefulValue oldValue = null;
    StatefulValue newValue = null;
    assertThat( obj, notNullValue( ) );
    this.canHas.writeLock( ).lock( );
    try {
      newValue = new StatefulValue( nextState, obj );
      oldValue = this.stateMap.putIfAbsent( obj.getName( ), newValue );
      if ( oldValue != null ) {
        newValue = new StatefulValue( nextState, oldValue.getValue( ) );
        if ( this.stateMap.replace( obj.getName( ), oldValue, newValue ) ) {
          return newValue.getValue( );
        } else {
          return ( newValue = oldValue ).getValue( );
        }
      } else {
        return newValue.getValue( );
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
      this.fireStateChange( oldValue, nextState );
    }
  }
  
  private E getState( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      if ( this.stateMap.containsKey( name ) ) {
        return this.stateMap.get( name ).getState( );
      } else {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      }
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  /**
   * Returns the reference (see {@link StatefulValue#getValue()}) of the currently associated
   * {@link StatefulValue} for the key {@code name}.
   * 
   * {@inheritDoc StatefulNamedRegistry#lookupEntry(String)}
   * 
   * @param name
   * @return
   * @throws NoSuchElementException
   */
  public T lookup( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      return this.lookupEntry( name ).getValue( );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  /**
   * Returns the reference (see {@link StatefulValue#getValue()}) of the currently associated
   * {@link StatefulValue} for the key determined by {@code obj.getName()} (see
   * {@link HasName#getName()}).
   * 
   * {@inheritDoc StatefulNamedRegistry#lookupEntry(String)}
   * 
   * @see HasName#getName()
   * @see StatefulNamedRegistry#lookupEntry(String)
   * @see StatefulValue#getValue()
   * @param obj
   * @return
   * @throws NoSuchElementException
   */
  public T lookup( T obj ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      return this.lookupEntry( obj.getName( ) ).getValue( );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public E lookupState( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      this.lookup( "hi" );
      return this.lookupEntry( name ).getState( );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  /**
   * Try to lookup the {@link StatefulValue} instance associated with the key {@code name}. If
   * found, the registered reference is returned. If not, a {@link NoSuchElementException} is
   * thrown.
   * 
   * @see StatefulValue
   * @param name
   * @return
   * @throws NoSuchElementException
   */
  public StatefulValue lookupEntry( String name ) throws NoSuchElementException {
    this.canHas.readLock( ).lock( );
    try {
      StatefulValue currValue = this.stateMap.get( name );
      if ( currValue != null ) {
        return currValue;
      } else {
        throw new NoSuchElementException( "Can't find registered object: " + name + " in " + this.getClass( ).getSimpleName( ) );
      }
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public boolean setState( String name, E newState ) throws NoSuchElementException {
    StatefulValue oldValue = null;
    this.canHas.writeLock( ).lock( );
    try {
      oldValue = this.lookupEntry( name );
      if ( oldValue.getState( ).equals( newState ) ) {
        return true;
      } else {
        return this.stateMap.replace( name, oldValue, new StatefulValue( newState, oldValue.getValue( ) ) );
      }
    } finally {
      this.canHas.writeLock( ).unlock( );
      if ( oldValue != null && !oldValue.getState( ).equals( newState ) ) {
        this.fireStateChange( oldValue, newState );
      }
    }
  }
  
  private void fireStateChange( StatefulValue oldValue, E newState ) {
    try {
      ListenerRegistry.getInstance( ).fireEvent( new StateEvent<T, E>( oldValue.getValue( ), newState, oldValue.getState( ) ) );
    } catch ( EventFailedException e ) {
      LOG.warn( "Registry change was vetoed: " + e, e );
    }
  }
  
  public boolean contains( String name ) {
    this.canHas.readLock( ).lock( );
    try {
      return this.stateMap.containsKey( name );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public boolean contains( String name, E state ) {
    this.canHas.readLock( ).lock( );
    try {
      return this.stateMap.containsKey( name ) && this.stateMap.get( state ).getState( ).equals( state );
    } finally {
      this.canHas.readLock( ).unlock( );
    }
  }
  
  public List<T> listValues( ) {
    List<T> valueList = Lists.newArrayList( );
    for ( StatefulValue m : this.stateMap.values( ) ) {
      valueList.add( m.getValue( ) );
    }
    return ImmutableList.copyOf( valueList );
  }
  
  public ImmutableList<String> listKeys( E state ) {
    return ImmutableList.copyOf( this.stateMap.keySet( ) );
  }
  
  public ImmutableList<T> listStateValues( E state ) {
    List<T> valueList = Lists.newArrayList( );
    for ( StatefulValue m : this.stateMap.values( ) ) {
      if ( m.getState( ).equals( state ) ) {
        valueList.add( m.getValue( ) );
      }
    }
    return ImmutableList.copyOf( valueList );
  }
}
