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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.bootstrap.ServiceBootstrapper;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.ws.client.ServiceProxy;

@Provides(resource=Resource.RemoteServices)
public class ServiceProxyBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceProxyBootstrapper.class );
  
  @Override
  public boolean load( Resource current ) throws Exception {
    if( !Component.walrus.isLocal( ) ) {
      List<WalrusConfiguration> walri = Configuration.getWalrusConfigurations( );
      for( WalrusConfiguration w : walri ) {
        Component.walrus.setHostAddress( w.getHostName( ) );
      }
    }
    if( !Component.eucalyptus.isLocal( ) ) {
      Component.eucalyptus.setHostAddress( Component.db.getHostAddress( ) );
      Component.jetty.setHostAddress( Component.db.getHostAddress( ) );
      Component.clusters.setHostAddress( Component.db.getHostAddress( ) );
    }
    return false;
  }


  @Provides(resource=Resource.SpringService)
  public static class LateBindingBootstrapper extends Bootstrapper {
  
    @Override
    public boolean start( ) throws Exception {
      List<WalrusConfiguration> walri = Configuration.getWalrusConfigurations( );
      for( WalrusConfiguration w : walri ) {
        try {
          registerComponent( Component.walrus, w );
        } catch ( Exception e ) {
          LOG.error( "Failed to create walrus service proxy: " + e );
        }
      }
      if( Component.walrus.isLocal( ) ) {
        registerLocalComponent( Component.walrus );
      }
      List<StorageControllerConfiguration> scs = Configuration.getStorageControllerConfigurations( );
      for( StorageControllerConfiguration sc : scs ) {
        try {
          registerComponent( Component.storage, sc );
        } catch ( Exception e ) {
          LOG.error( "Failed to create storage controller "+sc.getName( )+" service proxy: " + e );
        }
      }
      if( Component.storage.isLocal( ) ) {
        registerLocalComponent( Component.storage );
      }
      if( Component.dns.isLocal( ) ) {
        registerLocalComponent( Component.dns );
      }
      if( Component.clusters.isLocal( ) ) {
        registerLocalComponent( Component.clusters );
      }
      if( Component.jetty.isLocal( ) ) {
        registerLocalComponent( Component.jetty );
      }
      return true;
    }
    private void registerLocalComponent( Component component ) throws RegistrationException {
      Registry registry = ServiceBootstrapper.getRegistry( );
      URI uri = component.getUri( );
      String keyPrefix = component.name( ) + "/";
      registry.registerObject( keyPrefix + component.name( ), new ServiceProxy( component, component.name( ), uri ) );
    }
    private void registerComponent( Component component, ComponentConfiguration componentConfiguration ) throws Exception {
      Registry registry = ServiceBootstrapper.getRegistry( );
      URI uri = new URI( "http://"+ componentConfiguration.getHostName( ) + ":" + componentConfiguration.getPort( ) + componentConfiguration.getServicePath( ) );
      String keyPrefix = component.name( ) + "/";
      registry.registerObject( keyPrefix + componentConfiguration.getName( ), new ServiceProxy( component, componentConfiguration.getName( ), uri ) );
      LOG.info( "Registering service proxy for " + componentConfiguration.getName( ) + " at " + uri.toASCIIString( ) );
    }

    @Override
    public boolean load( Resource current ) throws Exception {
      return true;
    }
  }


  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
