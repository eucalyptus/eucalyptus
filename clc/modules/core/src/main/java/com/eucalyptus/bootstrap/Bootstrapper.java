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

package com.eucalyptus.bootstrap;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceDependencyException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.EmpyreanService;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

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
public abstract class Bootstrapper implements Comparable<Bootstrapper>, CanBootstrap {
  public static abstract class Simple extends Bootstrapper {
    
    @Override
    public boolean check( ) throws Exception {
      return true;
    }
    
    @Override
    public void destroy( ) throws Exception {}
    
    @Override
    public boolean disable( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean enable( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean load( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean start( ) throws Exception {
      return true;
    }
    
    @Override
    public boolean stop( ) throws Exception {
      return true;
    }
    
  }
  
  private static Logger     LOG           = Logger.getLogger( Bootstrapper.class );
  private List<ComponentId> dependsLocal  = this.getDependsLocal( );
  
  private List<ComponentId> dependsRemote = this.getDependsRemote( );
  
  /**
   * Check the status of the bootstrapped resource.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true when all is clear
   * @throws Exception should contain detail any malady which may be present.
   */
  @Override
  public abstract boolean check( ) throws Exception;
  
  /**
   * Check that all DependsLocal components are local.
   * 
   * @return true if all local dependencies are satisfied.
   */
  public boolean checkLocal( ) {
    for ( final ComponentId c : this.getDependsLocal( ) ) {
      try {
        if ( !Components.lookup( c ).hasLocalService( ) ) {
          return false;
        }
      } catch ( final NoSuchElementException ex ) {
        return false;
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
    for ( final ComponentId c : this.getDependsRemote( ) ) {
      try {
        if ( Components.lookup( c ).hasLocalService( ) ) {
          return false;
        }
      } catch ( final NoSuchElementException ex ) {}
    }
    return true;
  }
  
  /**
   * Initiate a forced shutdown releasing all used resources and effectively unloading the this
   * bootstrapper.
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @throws Exception
   */
  @Override
  public abstract void destroy( ) throws Exception;
  
  /**
   * Enter an idle/passive state.
   * 
   * @return
   * @throws Exception
   */
  @Override
  public abstract boolean disable( ) throws Exception;
  
  /**
   * Perform the enable phase of bootstrap -- this occurs when the service associated with this
   * bootstrapper is made active and should bring the resource to an active operational state.
   * 
   * @return
   * @throws Exception
   */
  @Override
  public abstract boolean enable( ) throws Exception;
  
  @Override
  public boolean equals( final Object obj ) {
    if ( obj == null ) {
      return false;
    } else {
      return this.getClass( ).equals( obj.getClass( ) );
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
    if ( !Ats.from( this.getClass( ) ).has( RunDuring.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify execution stage (RunDuring.value=Bootstrap.Stage): " + this.getClass( ) );
    } else {
      return Ats.from( this.getClass( ) ).get( RunDuring.class ).value( );
    }
  }
  
  /**
   * Get the list of {@link ComponentId}s which must be on the local system for this bootstrapper to
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
    if ( this.dependsLocal != null ) {
      return this.dependsLocal;
    } else {
      if ( !Ats.from( this.getClass( ) ).has( DependsLocal.class ) ) {
        this.dependsLocal = Lists.newArrayListWithExpectedSize( 0 );
      } else {
        this.dependsLocal = Lists.newArrayList( );
        for ( final Class<?> compIdClass : Ats.from( this.getClass( ) ).get( DependsLocal.class ).value( ) ) {
          if ( !ComponentId.class.isAssignableFrom( compIdClass ) ) {
            LOG.error( "Ignoring specified @Depends which does not extend ComponentId: " + compIdClass );
          } else {
            try {
              LOG.trace( "Adding @Depends to " + this.getClass( ) + ": " + compIdClass );
              this.dependsLocal.add( ( ComponentId ) compIdClass.newInstance( ) );
            } catch ( final InstantiationException ex ) {
              LOG.error( ex, ex );
            } catch ( final IllegalAccessException ex ) {
              LOG.error( ex, ex );
            }
          }
        }
      }
      return this.dependsLocal;
    }
  }
  
  /**
   * Get the list of {@link ComponentId}s which must be present on a remote system for this
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
    if ( this.dependsRemote != null ) {
      return this.dependsRemote;
    } else {
      if ( !Ats.from( this.getClass( ) ).has( DependsRemote.class ) ) {
        this.dependsRemote = Lists.newArrayListWithExpectedSize( 0 );
      } else {
        this.dependsRemote = Lists.newArrayList( );
        for ( final Class<?> compIdClass : Ats.from( this.getClass( ) ).get( DependsRemote.class ).value( ) ) {
          if ( !ComponentId.class.isAssignableFrom( compIdClass ) ) {
            LOG.error( "Ignoring specified @Depends which does not use ComponentId" );
          } else {
            try {
              this.dependsRemote.add( ( ComponentId ) compIdClass.newInstance( ) );
            } catch ( final InstantiationException ex ) {
              LOG.error( ex, ex );
            } catch ( final IllegalAccessException ex ) {
              LOG.error( ex, ex );
            }
          }
        }
      }
      return this.dependsRemote;
    }
  }
  
  /**
   * The Component to which this bootstrapper belongs and on whose behalf it executes.
   * 
   * @return Component
   */
  @SuppressWarnings( "unchecked" )
  public <T extends ComponentId> Class<T> getProvides( ) {
    if ( !Ats.from( this.getClass( ) ).has( Provides.class ) ) {
      Exceptions.trace( "Bootstrap class does not specify the component which it @Provides.  Fine.  For now we pretend you had put @Provides(ComponentId.class): "
                      + this.getClass( ) );
      return ( Class<T> ) ComponentId.class;
    } else {
      return Ats.from( this.getClass( ) ).get( Provides.class ).value( );
    }
    
  }

  protected void throwIfNotEnabled(
      final Class<? extends ComponentId> componentIdClass
  ) throws ServiceDependencyException {
    if ( Components.services( componentIdClass ).find( ServiceConfigurations.filterEnabled( ) ).isEmpty( ) ) {
      throw new ServiceDependencyException( "The "
          + ComponentIds.lookup( getProvides( ) ).name( )
          + " service depends upon an ENABLED "
          + ComponentIds.lookup( componentIdClass ).name( ) );
    }
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.getClass( ).hashCode( );
    return result;
  }
  
  /**
   * Perform the {@link SystemBootstrapper#load()} phase of bootstrap.
   * NOTE: The only code which can execute with uid=0 runs during the
   * {@link EmpyreanService.Stage.PrivilegedConfiguration} stage of the {@link #load()} phase.
   * 
   * @see SystemBootstrapper#load()
   * @return true on successful completion
   * @throws Exception
   */
  @Override
  public abstract boolean load( ) throws Exception;
  
  /**
   * Perform the {@link SystemBootstrapper#start()} phase of bootstrap.
   * 
   * @see SystemBootstrapper#start()
   * @return true on successful completion
   * @throws Exception
   */
  
  @Override
  public abstract boolean start( ) throws Exception;
  
  /**
   * Initiate a graceful shutdown
   * 
   * @note Intended for future use. May become {@code abstract}.
   * @return true on successful completion
   * @throws Exception
   */
  @Override
  public abstract boolean stop( ) throws Exception;
  
  /**
   * @see java.lang.Object#toString()
   * @return a string
   */
  @Override
  public String toString( ) {
    return String.format( "Bootstrapper %s runDuring=%s dependsLocal=%s dependsRemote=%s", this.getClass( ).getSimpleName( ), this.getBootstrapStage( ),
                          this.dependsLocal, this.dependsRemote );
  }
  
  @Override
  public int compareTo( Bootstrapper o ) {
    return this.getClass( ).toString( ).compareTo( o.getClass( ).toString( ) );
  }
}
