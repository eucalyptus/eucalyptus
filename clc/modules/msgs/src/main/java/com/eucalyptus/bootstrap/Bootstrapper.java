/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import static com.eucalyptus.system.Ats.From;
import java.util.List;
import org.apache.log4j.Logger;
import java.util.NoSuchElementException;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Any;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Inheriting classes will be identified by the bootstrap mechanism and invoked appropriately during
 * the process of starting the system stack ({@link Bootstrap} and {@link SystemBootstrapper}). A
 * well defined Bootstrapper implementation <b>MUST</b> include at least:
 * <ol>
 * <li>The abstract {@link #load()} and {@link #start()} methods.</li>
 * <li>A {@link RunDuring} annotation declaring the {@link Bootstrap.Stage} during which the
 * bootstrapper should be executed.</li>
 * </ol>
 * 
 * @see Provides
 * @see RunDuring
 * @see DependsLocal
 * @see DependsRemote
 * @see Bootstrap.Stage
 * @see SystemBootstrapper#load()
 * @see SystemBootstrapper#start()
 */
public abstract class Bootstrapper {
  private static Logger LOG = Logger.getLogger( Bootstrapper.class );
  private List<ComponentId> dependsLocal  = getDependsLocal();
  private List<ComponentId> dependsRemote = getDependsRemote();
  
  /**
   * Perform the {@link SystemBootstrapper#load()} phase of bootstrap.
   * NOTE: The only code which can execute with uid=0 runs during the
   * {@link EmpyreanService.Stage.PrivilegedConfiguration} stage of the {@link #load()} phase.
   * 
   * @see SystemBootstrapper#load()
   * @return true on successful completion
   * @throws Exception
   */
  public abstract boolean load( ) throws Exception;
  
  /**
   * Perform the {@link SystemBootstrapper#start()} phase of bootstrap.
   * 
   * @see SystemBootstrapper#start()
   * @return true on successful completion
   * @throws Exception
   */

  public abstract boolean start( ) throws Exception;

  /**
   * Perform the enable phase of bootstrap -- this occurs when the service associated with this bootstrapper is made active and should bring the resource to an active operational state.
   * @return
   * @throws Exception
   */
  public abstract boolean enable( ) throws Exception;
  
  /**
   * Initiate a graceful shutdown 
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true on successful completion
   * @throws Exception
   */
  public abstract boolean stop( ) throws Exception;

  /**
   * Initiate a forced shutdown releasing all used resources and effectively unloading the this bootstrapper.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @throws Exception
   */
  public abstract void destroy( ) throws Exception;
  
  /**
   * Enter an idle/passive state. 
   * 
   * @return
   * @throws Exception
   */
  public abstract boolean disable( ) throws Exception;

  /**
   * Check the status of the bootstrapped resource.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true when all is clear
   * @throws Exception should contain detail any malady which may be present.
   */
  public abstract boolean check( ) throws Exception;
  
  /**
   * Get the list of {@link Component}s which must be on the local system for this bootstrapper to
   * be executable.
   * 
   * @note If the {@link DependsLocal} annotation is not specified this bootstrapper will always
   *       execute.
   * @see {@link DependsLocal}
   * @see BootstrapException#throwFatal(String)
   * @return List<Component> which must present on the local system for this bootstrapper to
   *         execute.
   */
  public List<ComponentId> getDependsLocal( ) {
    if ( dependsLocal != null ) {
      return dependsLocal;
    } else {
      if ( !From( this.getClass( ) ).has( DependsLocal.class ) ) {
        dependsLocal = Lists.newArrayListWithExpectedSize( 0 );
      } else {
        dependsLocal = Lists.newArrayList( );
        for( Class compIdClass : From( this.getClass( ) ).get( DependsLocal.class ).value( ) ) {
          if( !ComponentId.class.isAssignableFrom( compIdClass ) ) {
            LOG.error( "Ignoring specified @Depends which does not use ComponentId" );
          } else {
            try {
              dependsLocal.add( ( ComponentId ) compIdClass.newInstance( ) );
            } catch ( InstantiationException ex ) {
              LOG.error( ex , ex );
            } catch ( IllegalAccessException ex ) {
              LOG.error( ex , ex );
            }
          }
        }
      }
      return dependsLocal;
    }
  }
  
  /**
   * Get the list of {@link Component}s which must be present on a remote system for this
   * bootstrapper to be execute.
   * 
   * @note If the {@link DependsRemote} annotation is not specified this bootstrapper will always
   *       execute.
   * @see {@link DependsRemote}
   * @see BootstrapException#throwFatal(String)
   * @return List<Component> which must <b>not</b> present on the local system for this bootstrapper
   *         to execute.
   */
  public List<ComponentId> getDependsRemote( ) {
    if ( dependsRemote != null ) {
      return dependsRemote;
    } else {
      if ( !From( this.getClass( ) ).has( DependsRemote.class ) ) {
        dependsRemote = Lists.newArrayListWithExpectedSize( 0 );
      } else {
        dependsRemote = Lists.newArrayList( );
        for( Class compIdClass : From( this.getClass( ) ).get( DependsRemote.class ).value( ) ) {
          if( !ComponentId.class.isAssignableFrom( compIdClass ) ) {
            LOG.error( "Ignoring specified @Depends which does not use ComponentId" );
          } else {
            try {
              dependsRemote.add( ( ComponentId ) compIdClass.newInstance( ) );
            } catch ( InstantiationException ex ) {
              LOG.error( ex , ex );
            } catch ( IllegalAccessException ex ) {
              LOG.error( ex , ex );
            }
          }
        }
        for ( ComponentId c : dependsRemote ) {
          if ( !c.isCloudLocal( ) ) {
            BootstrapException.throwFatal( "DependsRemote specifies a component which is not cloud-local: " + this.getClass( ).getSimpleName( ) );
          }
        }
      }
      return dependsRemote;
    }
  }
  /**
   * The Bootstrap.Stage during which the bootstrapper executes.
   * 
   * @note If the {@link RunDuring} annotation is not specified on this class bootstrap will fail
   *       and the system will exit.
   * @see BootstrapException#throwFatal(String)
   * @return Bootstrap.Stage
   */
  public Bootstrap.Stage getBootstrapStage( ) {
    if ( !From( this.getClass( ) ).has( RunDuring.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify execution stage (RunDuring.value=Bootstrap.Stage): " + this.getClass( ) );
    } else {
      return From( this.getClass( ) ).get( RunDuring.class ).value( );
    }
  }
  
  /**
   * The Component to which this bootstrapper belongs and on whose behalf it executes.
   * 
   * @return Component
   */
  public <T extends ComponentId> Class<T> getProvides( ) {
    if ( !From( this.getClass( ) ).has( Provides.class ) ) {
      Exceptions.eat( "Bootstrap class does not specify the component which it @Provides.  Fine.  For now we pretend you had put @Provides(Component.any) instead of System.exit(-1): "
                      + this.getClass( ) );
      return ( Class<T> ) Any.class;
    } else {
      return ( Class<T> ) From( this.getClass( ) ).get( Provides.class ).value( );
    }
    
  }
  
  /**
   * Check that all DependsLocal components are local.
   * 
   * @return true if all local dependencies are satisfied.
   */
  public boolean checkLocal( ) {
    for ( ComponentId c : this.getDependsLocal( ) ) {
      try {
        if ( !Components.lookup( c ).isLocal( ) ) {
          return false;
        }
      } catch ( NoSuchElementException ex ) {
//        return false;
      }
    }
    return true;
  }
  
  /**
   * Check that all DependsRemote components are remote.
   * 
   * @return true if all remote dependencies are satisfied.
   */
  public boolean checkRemote( ) {
    for ( ComponentId c : this.getDependsRemote( ) ) {
      try {
        if ( Components.lookup( c ).isLocal( ) ) {
          return false;
        }
      } catch ( NoSuchElementException ex ) {
//        return false;
      }
    }
    return true;
  }
  
  @Override
  public boolean equals( Object obj ) {
    return this.getClass( ).equals( obj.getClass( ) );
  }
  
  /**
   * HARDCORE DEPRECATED: Implement {@link Bootstrapper#load()} instead. To obtain a reference to
   * the current stage of bootstrap use {@link Bootstrap#getCurrentStage()}.
   * 
   * @see Bootstrapper#load()
   * @see Bootstrap#getCurrentStage()
   */
  @Deprecated
  public boolean load( Stage current ) throws Exception {
    return this.load( );
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return a string
   */
  @Override
  public String toString( ) {
    return String.format( "Bootstrapper %s runDuring=%s dependsLocal=%s dependsRemote=%s", this.getClass( ).getSimpleName( ), this.getBootstrapStage( ), this.dependsLocal, this.dependsRemote );
  }
  
}
