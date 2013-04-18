package com.eucalyptus.bootstrap;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import com.eucalyptus.loadbalancing.activities.LoadBalancerASGroupCreator;
import org.apache.log4j.Logger;
import com.eucalyptus.loadbalancing.LoadBalancing;

@Provides(LoadBalancing.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(LoadBalancing.class)
public class LoadBalancingBootstrapper extends Bootstrapper {

  private static Logger LOG = Logger.getLogger( LoadBalancingBootstrapper.class );

  private static LoadBalancingBootstrapper singleton;

  public static Bootstrapper getInstance( ) {
    synchronized ( LoadBalancingBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new LoadBalancingBootstrapper( );
        LOG.info( "Creating Load Balancing Bootstrapper instance." );
      } else {
        LOG.info( "Returning Load Balancing Bootstrapper instance." );
      }
    }
    return singleton;
  }

  @Override
  public boolean load() throws Exception {
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    if ( LoadBalancerASGroupCreator.LOADBALANCER_EMI != null
        && LoadBalancerASGroupCreator.LOADBALANCER_EMI.startsWith("emi-") )
      return true;

    return false;
  }
}
