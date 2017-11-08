/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.fsm;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Callback.Completion;
import com.eucalyptus.util.async.Callbacks;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.Callables;

public class Transitions {
  private static Logger LOG = Logger.getLogger( Transitions.class );
  
  private static TransitionException exceptionOnCondition( String message, Predicate p ) {
    return new TransitionException( "Transition rejected because constraint check is false: " + message + " for class " + p.getClass( ) );
  }
  
  public static <P extends HasName<P>> TransitionListener<P> callbackAsListener( final Callback<P> p ) {
    return new TransitionListener<P>( ) {
      
      @Override
      public boolean before( P parent ) {
        return true;
      }
      
      @Override
      public void leave( P parent ) {
        try {
          p.fire( parent );
        } catch ( Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
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
  
  public static <P extends HasName<P>> TransitionListener<P> predicateAsBeforeListener( final Predicate<P> predicate ) {
    TransitionListener<P> listener = new TransitionListener<P>( ) {
      @Override
      public boolean before( P parent ) {
        return predicate.apply( parent );
      }
      
      @Override
      public void leave( P parent ) {}
      
      @Override
      public void enter( P parent ) {}
      
      @Override
      public void after( P parent ) {}
    };
    return listener;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> callbackAsAction( final Callback<P> callback ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      @Override
      public void leave( P parent, Callback.Completion transitionCallback ) {
        try {
          callback.fire( parent );
          transitionCallback.fire( );
        } catch ( RuntimeException ex ) {
          LOG.error( ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
  
  public static <P extends HasName<P>> TransitionAction<P> callableAsAction( final Callable<P> callable ) {
    return callbackAsAction( Callbacks.forCallable( callable ) );
  }
  
  public static <P extends HasName<P>> TransitionAction<P> runnableAsAction( final Runnable runnable ) {
    return callbackAsAction( Callbacks.forRunnable( runnable ) );
  }
  
  public static <P extends HasName<P>> TransitionAction<P> predicateAsAction( final Predicate<P> predicate ) {
    TransitionAction<P> action = new AbstractTransitionAction<P>( ) {
      @Override
      public void leave( P parent, Callback.Completion transitionCallback ) {
        try {
          if ( !predicate.apply( parent ) ) {
            transitionCallback.fireException( Transitions.exceptionOnCondition( "Transition condition failed for " + parent + " on condition ", predicate ) );
          } else {
            transitionCallback.fire( );
          }
        } catch ( RuntimeException ex ) {
          Logs.extreme( ).error( ex, ex );
          transitionCallback.fireException( ex );
        }
      }
    };
    return action;
  }
}
