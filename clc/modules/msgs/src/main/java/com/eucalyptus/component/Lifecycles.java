package com.eucalyptus.component;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Committor;
import com.eucalyptus.util.Transition;

/**
 * State definitions
 * 
 * @author decker
 */
public class Lifecycles {
  private static Logger LOG = Logger.getLogger( Lifecycles.class );
  
  /**
   * The states possible in a component's lifecycle <br/>
   * TODO:TODO:TODO:TODO:TODO:TODO:TODO: <br/>
   * Valid transitions are: <br/>
   * - UNLOADED -> INITIALIZED <br/>
   * - INITIALIZED -> LOADED <br/>
   * - LOADED -> STARTED <br/>
   * - STARTED <-> STOPPED <br/>
   * - STOPPED -> UNLOADED (not implemented at this time) <br/>
   * 
   * @author decker
   */
  public enum State {
    DISABLED, PRIMORDIAL, INITIALIZED, LOADED, STARTED, STOPPED, PAUSED;
    
    public <A> Transition<A, Lifecycles.State> to( final Lifecycles.State s, final Committor<A> c ) throws Exception {
      return ( Transition<A, Lifecycles.State> ) Transition.anonymous( this, s, new Committor<A>( ) {
        @Override
        public void commit( A object ) throws Exception {
          c.commit( object );
          if ( object instanceof Component ) {
            ( ( Component ) object ).getLifecycle( ).setState( s );
          }
        }
      } );
    }
    
    public <A> Transition<A, Lifecycles.State> to( final Lifecycles.State s ) throws Exception {
      return ( Transition<A, Lifecycles.State> ) Transition.anonymous( this, s, new Committor<A>( ) {
        @Override
        public void commit( A object ) throws Exception {
          if ( object instanceof Component ) {
            ( ( Component ) object ).getLifecycle( ).setState( s );
          }
        }
      } );
    }
    
  }
  
  public static Lifecycle lookup( String componentName ) throws NoSuchElementException {
    return Components.lookup( Lifecycle.class, componentName );
  }
  
  public static Lifecycle lookup( com.eucalyptus.bootstrap.Component component ) throws NoSuchElementException {
    return Components.lookup( Lifecycle.class, component.name( ) );
  }
  
  public static boolean contains( String componentName ) {
    return Components.contains( Lifecycle.class, componentName );
  }
  
  public static boolean contains( com.eucalyptus.bootstrap.Component component ) {
    return Components.contains( Lifecycle.class, component.name( ) );
  }
  
}
