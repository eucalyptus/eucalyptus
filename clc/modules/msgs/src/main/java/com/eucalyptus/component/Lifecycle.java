package com.eucalyptus.component;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Lifecycles.State;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Transition;
import com.eucalyptus.records.EventRecord;

/**
 * @author decker
 */
public class Lifecycle implements ComponentInformation {
  private static Logger                         LOG         = Logger.getLogger( Lifecycle.class );
  private final NavigableMap<Transition, Class> transitions = new ConcurrentSkipListMap<Transition, Class>( );
  private final Component                       parent;
  private Lifecycles.State                      state;
  
  public Lifecycle( Component parent ) {
    this.parent = parent;
    this.state = Lifecycles.State.INITIALIZED;
    for ( DefaultTransitionList t : DefaultTransitionList.values( ) ) {
      this.addTransition( t.newInstance( ) );
    }
  }
  
  public Boolean isInitialized( ) {
    return State.STARTED.equals( this.state );
  }
  
  public void setState( State newState ) {
    this.state = newState;
  }
  
  public Lifecycles.State getState( ) {
    return this.state;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Lifecycle [" );
    if ( this.state != null ) {
      builder.append( "state=" ).append( this.state ).append( ", " );
    }
    builder.append( "]" );
    return builder.toString( );
  }
  
  public void addTransition( Transition transition ) {
    Class old = this.transitions.put( transition, transition.getClass( ) );
    if ( old != null ) {
      EventRecord.caller( Lifecycle.class, EventType.LIFECYCLE_TRANSITION_DEREGISTERED, transition.toString( ), old.getCanonicalName( ) );
    }
    EventRecord.caller( Lifecycle.class, EventType.LIFECYCLE_TRANSITION_REGISTERED, transition.toString( ) );
  }
  
  @Override
  public String getName( ) {
    return this.parent.getName( );
  }
}
