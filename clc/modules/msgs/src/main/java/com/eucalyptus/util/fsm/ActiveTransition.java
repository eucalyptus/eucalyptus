package com.eucalyptus.util.fsm;

import java.util.concurrent.atomic.AtomicReference;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback;

public class ActiveTransition<P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> implements HasName<ActiveTransition>, TransitionListener<P> {
  private final Long                                 id;
  private final String                               name;
  private final Long                                 startTime;
  private final Transition<P, S, T>                  transition;
  private final AtomicMarkedState<P, S, T>           state;
  private final AtomicReference<Callback.Completion> callback;
  
  public ActiveTransition( Long id, Transition<P, S, T> transition, AtomicMarkedState<P, S, T> state ) {
    this.id = id;
    this.startTime = System.nanoTime( );
    this.state = state;
    this.name = state.getName( ) + "-" + transition.getName( ) + "-" + id;
    this.transition = transition;
    this.callback = new AtomicReference<Callback.Completion>( null );
  }
  
  /**
   * @return the id
   */
  public final Long getId( ) {
    return this.id;
  }
  
  /**
   * @return the rule
   */
  public final Transition<P, S, T> getTransition( ) {
    return this.transition;
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  @Override
  public int compareTo( ActiveTransition that ) {
    return this.id.compareTo( that.id );
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return
   */
  @Override
  public String toString( ) {
    return String.format( "TransitionId:name=%s:id=%s:startTime=%s:state=%s", this.name, this.id, this.startTime );
  }
  
  /**
   * @return the callback
   */
  public Callback.Completion getCallback( ) throws IllegalStateException {
    if ( this.callback.get( ) != null ) {
      return this.callback.get( );
    } else {
      throw new IllegalStateException( "BUG: No callback set for active transition." );
    }
  }

  void setCallback( Callback.Completion callback ) {
    if ( this.callback.get( ) != null ) {
      throw new IllegalStateException( "BUG: A callback has already been created." );
    } else {
      this.callback.set( callback );
    }
  }
  
  /**
   * @param parent
   * @return
   * @see com.eucalyptus.util.fsm.Transition#before(com.eucalyptus.util.HasName)
   */
  @Override
  public boolean before( P parent ) {
    return this.transition.before( parent );
  }
  
  /**
   * @param parent
   * @see com.eucalyptus.util.fsm.Transition#leave(com.eucalyptus.util.HasName)
   */
  @Override
  public void leave( P parent, Callback.Completion callback ) {
    this.callback.compareAndSet( null, callback );
    this.transition.leave( parent, callback );
  }
  
  /**
   * @param parent
   * @see com.eucalyptus.util.fsm.Transition#enter(com.eucalyptus.util.HasName)
   */
  @Override
  public void enter( P parent ) {
    this.transition.enter( parent );
  }
  
  /**
   * @param parent
   * @see com.eucalyptus.util.fsm.Transition#after(com.eucalyptus.util.HasName)
   */
  @Override
  public void after( P parent ) {
    this.transition.after( parent );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.Transition#getFromStateMark()
   */
  public Boolean getFromStateMark( ) {
    return this.transition.getFromStateMark( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.Transition#getFromState()
   */
  public S getFromState( ) {
    return this.transition.getFromState( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.Transition#getToStateMark()
   */
  public Boolean getToStateMark( ) {
    return this.transition.getToStateMark( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.Transition#getToState()
   */
  public S getToState( ) {
    return this.transition.getToState( );
  }
  
}
