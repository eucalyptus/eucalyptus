package com.eucalyptus.util.fsm;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.Callables;

public class Transitions {
  private static Logger LOG = Logger.getLogger( Transitions.class );
  
  private static TransitionException exceptionOnCondition( String message, Predicate p ) {
    return new TransitionException( "Transition rejected because constraint check is false: " + message + " for class " + p.getClass( ).getCanonicalName( ) );
  }
  
  public static <P extends HasName<P>> TransitionListener<P> predicateAsListener( final Predicate<P> p ) {
    return new TransitionListener<P>( ) {
      
      @Override
      public boolean before( P parent ) {
        return true;
      }
      
      @Override
      public void leave( P parent ) {
        try {
          p.apply( parent );
        } catch ( Throwable ex ) {
          LOG.error( ex, ex );
        }
      }
      
      @Override
      public void enter( P parent ) {}
      
      @Override
      public void after( P parent ) {}
    };
  }
  
  private enum SimpleTransitions implements TransitionAction {
    NOOP {
      public void leave( HasName parent, Completion transitionCallback ) {
        transitionCallback.fire( );
      }
      
      public String toString( ) {
        return "TransitionAction.noop";
      }
      
      @Override
      public boolean before( HasName parent ) {
        return true;
      }
      
      @Override
      public void enter( HasName parent ) {}
      
      @Override
      public void after( HasName parent ) {}
    },
    OUTOFBAND {
      @Override
      public void leave( HasName parent, Completion transitionCallback ) {}
      
      public String toString( ) {
        return "TransitionAction.OUTOFBAND";
      }
      
      @Override
      public boolean before( HasName parent ) {
        return true;
      }
      
      @Override
      public void enter( HasName parent ) {}
      
      @Override
      public void after( HasName parent ) {}
      
    };
  }
  
  public static <P extends HasName<P>> TransitionAction<P> noop( ) {
    return SimpleTransitions.NOOP;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> callbackAsAction( final Callback<P> callable ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      @Override
      public void leave( P parent, Callback.Completion transitionCallback ) {
        try {
          callable.fire( parent );
          transitionCallback.fire( );
        } catch ( Throwable ex ) {
          LOG.error( ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> callableAsAction( final Callable<P> callable ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      @Override
      public void leave( P parent, Callback.Completion transitionCallback ) {
        try {
          callable.call( );
          transitionCallback.fire( );
        } catch ( Throwable ex ) {
          LOG.error( ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> runnableAsAction( final Runnable function ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      
      @Override
      public void leave( P parent, Completion transitionCallback ) {
        try {
          function.run( );
          transitionCallback.fire( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> predicateAsAction( final Predicate<P> predicate ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      @Override
      public void leave( P parent, Callback.Completion transitionCallback ) {
        try {
          if ( !predicate.apply( parent ) ) {
            transitionCallback.fireException( Transitions.exceptionOnCondition( "Transition condition failed for " + parent + " on condition ", predicate ) );
          }
          transitionCallback.fire( );
        } catch ( Throwable ex ) {
          LOG.error( ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
}
