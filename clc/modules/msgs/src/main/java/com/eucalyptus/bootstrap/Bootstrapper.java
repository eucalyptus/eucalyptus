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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import static com.eucalyptus.system.Ats.From;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

public abstract class Bootstrapper {
  
  private static Logger LOG               = Logger.getLogger( Bootstrapper.class );
  public static String  SERVICES_PROPERTY = "euca.services";
  public static String  MODEL_PROPERTY    = "euca.model";
  public static String  VERSION_PROPERTY  = "euca.version";
  private List<Component> dependsLocal  = null;
  private List<Component> dependsRemote = null;
  
  public String getVersion( ) {
    return System.getProperty( VERSION_PROPERTY );
  }
  
  public abstract boolean load( Stage current ) throws Exception;
  
  public abstract boolean start( ) throws Exception;
  
  public boolean check( ) throws Exception {
    return true;
  }
  
  public boolean stop( ) throws Exception {
    return true;
  }
  
  public boolean destroy( ) throws Exception {
    return true;
  }
  
  public boolean checkLocal( ) {
    if ( From( this.getClass( ) ).has( DependsLocal.class ) ) {
      for ( Component c : From( this.getClass( ) ).get( DependsLocal.class ).value( ) ) {
        if ( !c.isLocal( ) ) return false;
      }
    }
    return true;
  }
  
  @SuppressWarnings( "deprecation" )
  public boolean checkRemote( ) {
    if ( From( this.getClass( ) ).has( DependsRemote.class ) ) {
      for ( Component c : From( this.getClass( ) ).get( DependsRemote.class ).value( ) ) {
        if ( c.isLocal( ) ) return false;
      }
    }
    return true;
  }
  
  @Override
  public boolean equals( Object obj ) {
    return this.getClass( ).equals( obj.getClass( ) );
  }
  
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
  public List<Component> getDependsLocal( ) {
    if ( dependsLocal != null ) {
      return dependsLocal;
    } else {
      synchronized ( this ) {
        if ( dependsLocal != null ) {
          return dependsLocal;
        } else {
          if ( !From( this.getClass( ) ).has( DependsLocal.class ) ) {
            dependsLocal = Lists.newArrayListWithExpectedSize( 0 );
          } else {
            dependsLocal = Arrays.asList( From( this.getClass( ) ).get( DependsLocal.class ).value( ) );
          }
          return dependsLocal;
        }
      }
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
  public List<Component> getDependsRemote( ) {
    if ( dependsRemote != null ) {
      return dependsRemote;
    } else {
      synchronized ( this ) {
        if ( dependsRemote != null ) {
          return dependsRemote;
        } else {
          if ( !From( this.getClass( ) ).has( DependsRemote.class ) ) {
            dependsRemote = Lists.newArrayListWithExpectedSize( 0 );
          } else {
            dependsRemote = Arrays.asList( From( this.getClass( ) ).get( DependsRemote.class ).value( ) );
            for ( Component c : dependsRemote ) {
              if ( !c.isCloudLocal( ) ) {
                BootstrapException.throwFatal( "DependsRemote specifies a component which is not cloud-local: " + this.getClass( ).getSimpleName( ) );
              }
            }
          }
          return dependsRemote;
        }
      }
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
  public Component getProvides( ) {
    if ( !From( this.getClass( ) ).has( Provides.class ) ) {
      Exceptions.eat( "Bootstrap class does not specify the component which it @Provides.  Fine.  For now we pretend you had put @Provides(Component.any) instead of System.exit(-1): "
                      + this.getClass( ) );
      return Component.any;
    } else {
      return From( this.getClass( ) ).get( Provides.class ).value( );
    }
    
  }
}
