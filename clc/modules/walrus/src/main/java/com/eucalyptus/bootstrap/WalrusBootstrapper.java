/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;

@Provides( Walrus.class )
@RunDuring( Bootstrap.Stage.DatabaseInit )
@DependsLocal( Walrus.class )
public class WalrusBootstrapper extends Bootstrapper {
  private static Logger             LOG = Logger.getLogger( WalrusBootstrapper.class );
  private static WalrusBootstrapper singleton;
  
  public static Bootstrapper getInstance( ) {
    synchronized ( WalrusBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new WalrusBootstrapper( );
        LOG.info( "Creating Walrus Bootstrapper instance." );
      } else {
        LOG.info( "Returning Walrus Bootstrapper instance." );
      }
    }
    return singleton;
  }
  
  @Override
  public boolean load( ) throws Exception {
    WalrusControl.checkPreconditions( );
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    WalrusControl.configure( );
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    WalrusControl.enable( );
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    WalrusControl.stop( );
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    WalrusControl.disable( );
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    //check local storage
    WalrusControl.check( );
    return true;
  }
}
