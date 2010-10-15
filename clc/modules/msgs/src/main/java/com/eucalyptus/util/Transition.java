package com.eucalyptus.util;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

/**
 * @author decker
 */
public abstract class Transition<O, T extends Comparable> implements Comparable<Transition> {
  
  private static Logger LOG = Logger.getLogger( Transition.class );
  private final T       oldState;
  private final T       newState;
  private final String  name;
  
  /**
   * @param oldState
   * @param newState
   */
  public Transition( String name, T oldState, T newState ) {
    this.name = name;
    this.oldState = oldState;
    this.newState = newState;
  }
  
  public static <A, B extends Comparable> Transition<A, B> anonymous( Class<? extends Transition<A, B>> r ) throws Exception {
    return ( Transition<A, B> ) new Transition.anonymously<A, B>( r );
  }
  
  public static <A, B extends Comparable, L extends Iterable<A>> Transition<A, B> anonymous( L i, Class<? extends Transition<A, B>> r ) throws Throwable {
    Transition anon = anonymous( r );
    for ( A a : i ) {
      anon.transition( a );
    }
    return ( Transition<A, B> ) anon;
  }
  
  public static <A, B extends Comparable> Transition<A, B> anonymous( B from, B to, Committor<A> r ) throws Exception {
    return ( Transition<A, B> ) new Transition.anonymously<A, B>( from, to, r );
  }
  
  public static <A, B extends Comparable, L extends Iterable<A>> Transition<A, B> anonymous( B from, B to, L i, Committor<A> r ) throws Throwable {
    Transition<A, B> anon = anonymous( from, to, r );
    for ( A a : i ) {
      anon.transition( a );
    }
    return ( Transition<A, B> ) anon;
  }
  
  private static class anonymously<O, T extends Comparable> extends Transition<O, T> {
    private Committor<O> committor;
    
    public anonymously( T from, T to, Committor<O> committor ) {
      super( "anonymous", from, to );
      this.committor = committor;
    }
    
    public anonymously( final Class<? extends Transition<O, T>> c ) throws Exception {
      this( c.newInstance( ) );
    }
    
    public anonymously( final Transition<O, T> transition ) {
      super( transition.getName( ), transition.getOldState( ), transition.getNewState( ) );
      this.committor = new Committor<O>( ) {
        @Override
        public void commit( O object ) throws Exception {
          transition.commit( object );
        }
      };
    }
    
    @Override
    protected void commit( O component ) throws Exception {
      this.committor.commit( component );
    }
    
    @Override
    protected void post( O component ) throws Exception {}
    
    @Override
    protected void prepare( O component ) throws Exception {}
    
    @Override
    protected void rollback( O component ) {}
  }
  
  /**
   * Check preconditions and prepare for the transition
   * 
   * @throws Exception
   */
  protected abstract void prepare( O component ) throws Exception;
  
  /**
   * Perform component specific operations needed to accomplish the state transition. Note that the
   * component's lifecycle state will only be updated when and if
   * this operations completes.
   * 
   * @throws Exception
   */
  protected abstract void commit( O component ) throws Exception;
  
  /**
   * Check postconditions to ensure that the new component state has been entered cleanly.
   * 
   * @throws Exception
   */
  protected abstract void post( O component ) throws Exception;
  
  /**
   * In the case any operation throws an exception this method is invoked.
   */
  protected abstract void rollback( O component );
  
  /**
   * @see com.eucalyptus.util.Transition#transition(java.lang.Object)
   * @param object
   * @throws Throwable
   */
  public final Transition<O, T> transition( O object ) throws Throwable {
    if ( object instanceof Iterable ) {
      for ( O o : ( Iterable<O> ) object )
        this.doTransition( o );
    } else {
      this.doTransition( object );
    }
    return this;
  }
  
  private void doTransition( O object ) throws Exception {
    try {
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_PREPARE, this.toString( ), object.getClass( ).getCanonicalName( ) ).info( );
      this.prepare( object );
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_COMMIT, this.toString( ), object.getClass( ).getCanonicalName( ) ).info( );
      this.commit( object );
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_POST, this.toString( ), object.getClass( ).getCanonicalName( ) ).info( );
      this.post( object );
    } catch ( Exception e ) {
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_ROLLBACK, this.toString( ), object.getClass( ).getCanonicalName( ) ).error( );
      this.rollback( object );
      throw e;
    }
    EventRecord.caller( this.getClass( ), EventType.TRANSITION_FINISHED, this.toString( ) );
  }
  
  public final Transition<O, T> transition( Iterable<O> list ) throws Throwable {
    for ( O o : list )
      this.doTransition( o );
    return this;
  }
  
  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * @param that
   * @return
   */
  @Override
  public int compareTo( Transition that ) {
    int ret = 0;
    if ( ( ret = this.oldState.compareTo( that.oldState ) ) == 0 ) {
      return this.newState.compareTo( that.newState );
    } else {
      return ret;
    }
  }
  
  /**
   * @see java.lang.Object#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.newState == null )
      ? 0
      : this.newState.hashCode( ) );
    result = prime * result + ( ( this.oldState == null )
      ? 0
      : this.oldState.hashCode( ) );
    return result;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Transition other = ( Transition ) obj;
    if ( this.newState == null ) {
      if ( other.newState != null ) return false;
    } else if ( !this.newState.equals( other.newState ) ) return false;
    if ( this.oldState == null ) {
      if ( other.oldState != null ) return false;
    } else if ( !this.oldState.equals( other.oldState ) ) return false;
    return true;
  }
  
  /**
   * @return the oldState
   */
  public final T getOldState( ) {
    return this.oldState;
  }
  
  /**
   * @return the newState
   */
  public final T getNewState( ) {
    return this.newState;
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return
   */
  @Override
  public String toString( ) {
    return String.format( "Transition:from:%s:to:%s", this.oldState, this.newState );
  }
  
  /**
   * @return the name
   */
  protected final String getName( ) {
    return this.name;
  }
  
  protected Transition<O, T> newInstance( ) throws Exception {
    return this.getClass( ).newInstance( );
  }
  
}
