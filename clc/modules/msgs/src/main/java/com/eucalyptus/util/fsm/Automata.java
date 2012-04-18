/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util.fsm;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

public class Automata {
  public interface EnumMappable {
    Mapper asEnum = new Mapper( );
    
    class Mapper {
      public static <E> E[] getEnumConstants( final E input ) {
        if ( Enum.class.isAssignableFrom( input.getClass( ) ) ) {
          return ( E[] ) ( ( Enum ) input ).getDeclaringClass( ).getEnumConstants( );
        } else {
          throw new RuntimeException( "Failed to produce Enum constants because underlying class does not extend Enum:  "
                                                                  + input.getClass( ) );
        }
        
      }
    }
  }
  
  public interface Transition<T extends Enum<T>> extends EnumMappable, Comparable<T> {}
  
  public interface State<S extends Enum<S>> extends EnumMappable, Comparable<S> {}
  
  public static Logger LOG = Logger.getLogger( Automata.class );
  
  public static <S extends Automata.State, P extends HasFullName<P>> Callable<CheckedListenableFuture<P>> sequenceTransitions( final HasStateMachine<P, S, ?> hasFsm, final S... toStates ) {
    assertThat( toStates, not( emptyArray( ) ) );
    //TODO:GRZE: enforce that the sequence of states denotes a valid transition path
    S currentState = hasFsm.getStateMachine( ).getState( );
    int index = Lists.newArrayList( toStates ).indexOf( currentState );
    S[] actualStates = toStates;
    if ( index >= 0 && index < toStates.length ) {
      actualStates = Arrays.copyOfRange( toStates, index + 1, toStates.length );
    }
    Logs.exhaust( ).debug( "Preparing callback for " + hasFsm.getFullName( )
                           + " from state "
                           + currentState
                           + " followed by transition sequence: "
                           + Joiner.on( "->" ).join( actualStates ) );
    final List<Callable<CheckedListenableFuture<P>>> callables = makeTransitionCallables( hasFsm, actualStates );
    return Futures.sequence( callables.toArray( new Callable[] {} ) );
  }
  
  private static <S extends Automata.State, P extends HasFullName<P>> List<Callable<CheckedListenableFuture<P>>> makeTransitionCallables( final HasStateMachine<P, S, ?> hasFsm, final S... toStates ) {
    final List<Callable<CheckedListenableFuture<P>>> callables = Lists.newArrayList( );
    final StateMachine<P, S, ?> fsm = hasFsm.getStateMachine( );
    if ( toStates.length > 0 ) {
      for ( final S toState : toStates ) {
        callables.add( new Callable<CheckedListenableFuture<P>>( ) {
          @Override
          public String toString( ) {
            return Automata.class.getSimpleName( ) + ":"
                   + hasFsm.getFullName( )
                   + ":"
                   + fsm.getState( )
                   + "->"
                   + toState;
          }
          
          @Override
          public CheckedListenableFuture<P> call( ) {
            S fromState = fsm.getState( );
            try {
              CheckedListenableFuture<P> res = fsm.transition( toState );
              try {
                res.get( );
                Logs.extreme( ).debug( fsm.toString( ) + " transitioned from "
                                       + fromState
                                       + "->"
                                       + toState );
                return res;
              } catch ( Exception ex ) {
                return res;
              }
            } catch ( final Exception ex ) {
              Logs.extreme( ).debug( fsm.toString( ) + " failed transitioned from "
                                     + fromState
                                     + "->"
                                     + toState );
              Exceptions.maybeInterrupted( ex );
              Logs.extreme( ).error( ex, ex );
              return Futures.predestinedFailedFuture( ex );
//              throw Exceptions.toUndeclared( ex );
            }
          }
        } );
      }
    } else {
      callables.add( new Callable<CheckedListenableFuture<P>>( ) {
        @Override
        public String toString( ) {
          return Automata.class.getSimpleName( ) + ":"
                 + hasFsm.getFullName( )
                 + ":"
                 + fsm.getState( );
        }
        
        @Override
        public CheckedListenableFuture<P> call( ) {
          CheckedListenableFuture<P> ret = Futures.predestinedFuture( hasFsm.getStateMachine( ).getParent( ) );
          return ret;
        }
      } );
    }
    return callables;
  }
}
