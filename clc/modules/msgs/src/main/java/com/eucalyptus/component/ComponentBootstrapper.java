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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.util.List;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.CheckedFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.fsm.TransitionException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class ComponentBootstrapper {
  private static Logger                                 LOG                   = Logger.getLogger( ComponentBootstrapper.class );
  private final Multimap<Bootstrap.Stage, Bootstrapper> bootstrappers         = ArrayListMultimap.create( );
  private final Multimap<Bootstrap.Stage, Bootstrapper> disabledBootstrappers = ArrayListMultimap.create( );
  private final Component                               component;
  
  ComponentBootstrapper( Component component ) {
    super( );
    this.component = component;
  }
  
  public void addBootstrapper( Bootstrapper bootstrapper ) {
    if ( Stage.PrivilegedConfiguration.equals( bootstrapper.getBootstrapStage( ) ) ) {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, "stage:" + bootstrapper.getBootstrapStage( ).toString( ),
                        this.component.getComponentId( ).name( ),
                        bootstrapper.getClass( ).getName( ), 
                        "component=" + this.component.getComponentId( ).name( ) ).info( );
    } else {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, "stage:" + bootstrapper.getBootstrapStage( ).toString( ),
                        this.component.getComponentId( ).name( ),
                        bootstrapper.getClass( ).getName( ), 
                        "component=" + this.component.getComponentId( ).name( ) ).info( );
      this.bootstrappers.put( bootstrapper.getBootstrapStage( ), bootstrapper );
    }
  }
  
  private void updateBootstrapDependencies( ) {
    try {
      for ( Entry<Stage, Bootstrapper> entry : Iterables.concat( Lists.newArrayList( this.bootstrappers.entries( ) ), Lists.newArrayList( this.disabledBootstrappers.entries( ) ) ) ) {
        Bootstrap.Stage stage = entry.getKey( );
        Bootstrapper bootstrapper = entry.getValue( );
        if ( entry.getValue( ).checkLocal( ) && entry.getValue( ).checkRemote( ) ) {
          this.enableBootstrapper( stage, bootstrapper );
        } else {
          this.disableBootstrapper( stage, bootstrapper );
        }
      }
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  private void enableBootstrapper( Bootstrap.Stage stage, Bootstrapper bootstrapper ) {
    EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_ENABLED, "stage:", stage.toString( ), this.component.getComponentId( ).name( ),
                      bootstrapper.getClass( ).getName( ), "component=" + this.component.getComponentId( ).name( ) ).info( );
    this.disabledBootstrappers.remove( stage, bootstrapper );
    this.bootstrappers.put( stage, bootstrapper );
  }
  
  private void disableBootstrapper( Bootstrap.Stage stage, Bootstrapper bootstrapper ) {
    EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_MARK_DISABLED, "stage:" + stage.toString( ), this.component.getComponentId( ).name( ),
                      bootstrapper.getClass( ).getName( ), "component=" + this.component.getComponentId( ).name( ) ).info( );
    this.bootstrappers.remove( stage, bootstrapper );
    this.disabledBootstrappers.put( stage, bootstrapper );
  }
  
  private boolean doTransition( EventType transition, CheckedFunction<Bootstrapper, Boolean> checkedFunction ) {
    String name = transition.name( ).replaceAll( ".*_", "" ).toLowerCase( );
    this.updateBootstrapDependencies( );
    for ( Stage s : Bootstrap.Stage.values( ) ) {
      for ( Bootstrapper b : this.bootstrappers.get( s ) ) {
        EventRecord.here( this.component.getClass( ), transition, "stage", s.name( ), b.getClass( ).getCanonicalName( ) ).debug( );
        try {
          boolean result = checkedFunction.apply( b );
          if ( !result ) {
            throw Exceptions.error( new TransitionException( b.getClass( ).getSimpleName( ) + " returned 'false' from " + name
                                                             + "( ): terminating bootstrap for component: " + this.component.getName( ) ) );
          }
        } catch ( Throwable e ) {
          throw Exceptions.error( new TransitionException( b.getClass( ).getSimpleName( ) + " returned '" + e.getMessage( ) + "' from " + name
                                                           + "( ): terminating bootstrap for component: " + this.component.getName( ), e ) );
        }
      }
    }
    return true;
    
  }
  
  public boolean load( ) {
    this.doTransition( EventType.BOOTSTRAPPER_LOAD, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.load( );
      }
    } );
    return true;
  }
  
  public boolean start( ) {
    this.doTransition( EventType.BOOTSTRAPPER_START, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.start( );
      }
    } );
    return true;
  }
  
  public boolean enable( ) {
    this.doTransition( EventType.BOOTSTRAPPER_ENABLE, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.enable( );
      }
    } );
    return true;
  }
  
  public boolean stop( ) {
    this.doTransition( EventType.BOOTSTRAPPER_STOP, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.stop( );
      }
    } );
    return true;
  }
  
  public void destroy( ) {
    this.doTransition( EventType.BOOTSTRAPPER_DESTROY, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        arg0.destroy( );
        return true;
      }
    } );
  }
  
  public boolean disable( ) {
    this.doTransition( EventType.BOOTSTRAPPER_DISABLE, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.disable( );
      }
    } );
    
    return true;
  }
  
  public boolean check( ) {
    this.doTransition( EventType.BOOTSTRAPPER_CHECK, new CheckedFunction<Bootstrapper, Boolean>( ) {
      @Override
      public Boolean apply( Bootstrapper arg0 ) throws Exception {
        return arg0.check( );
      }
    } );
    return true;
  }
  
  public List<Bootstrapper> getBootstrappers( ) {
    return Lists.newArrayList( this.bootstrappers.values( ) );
  }
  
}
