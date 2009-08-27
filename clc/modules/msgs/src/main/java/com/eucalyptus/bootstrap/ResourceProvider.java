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
package com.eucalyptus.bootstrap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import net.sf.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ResourceProvider {
  private static Logger LOG = Logger.getLogger( ResourceProvider.class );
  private final Resource            resource;
  private final URL                 origin;
  private final String              name;
  private final Map<String, String> properties;
  private final List<ConfigResource> configurations;
  private Component component;
  @SuppressWarnings( "unchecked" )
  public ResourceProvider( Resource resource, Properties props, URL origin ) {
    this.resource = resource;
    this.properties = Maps.newHashMap( ( Hashtable ) props );
    this.name = this.properties.remove( "name" );
    this.origin = origin;
    this.configurations = Lists.newArrayList( );
    try {
      this.component = Component.valueOf( this.name );
      component.setResourceProvider( this );
      if( System.getProperty("euca.disable." + component.name( )) == null ) {
        component.markEnabled( );
        if( System.getProperty("euca.remote." + component.name( )) == null ) {
          component.markLocal( );
        }
      }
    } catch ( Exception e ) {
      this.component = Component.any;
    }
  }
  
  public List<ConfigResource> initConfigurationResources() throws IOException {
    if( this.component == null || this.component.isEnabled( ) ) {
      for(String rscName : this.properties.keySet( )) {
        try {
          this.configurations.add( new ConfigResource( this.properties.get( rscName ) ) );
        } catch ( IOException e ) {
          LOG.error( String.format("Processing %s caused an error %s: %s", this.origin, e.getClass().getSimpleName( ), e.getMessage( )), e );
          throw e;
        } 
      }      
    } else {
      LOG.info("-X skipping " + this.component + " because it is marked disabled.");
    }
    return this.configurations;
  }
  
  public Resource getResource( ) {
    return resource;
  }

  public String getName( ) {
    return name;
  }

  public URL getOrigin( ) {
    return origin;
  }



  public List<ConfigResource> getConfigurations( ) {
    return configurations;
  }

}
