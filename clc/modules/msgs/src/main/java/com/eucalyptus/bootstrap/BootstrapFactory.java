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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import com.eucalyptus.util.BaseDirectory;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtils;
import com.eucalyptus.util.ServiceJarFile;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class BootstrapFactory {
  private static Logger                               LOG           = Logger.getLogger( BootstrapFactory.class );
  private static Multimap<Resource, ResourceProvider> resources     = Multimaps.newArrayListMultimap( );
  private static Multimap<Resource, Bootstrapper>     bootstrappers = Multimaps.newArrayListMultimap( );

  public static void initResourceProviders( ) {
    for ( Resource r : Resource.values( ) ) {
      for ( ResourceProvider p : r.getProviders( ) ) {
        LOG.info( "Loaded " + LogUtils.dumpObject( p ) );
      }
    }
  }

  public static void initConfigurationResources( ) throws IOException {
    for ( Resource r : Resource.values( ) ) {
      for ( ResourceProvider p : r.initProviders( ) ) {
        LOG.info( "Loading resource provider:" + p.getName( ) + " -- " + p.getOrigin( ) );
        for ( ConfigResource cfg : p.getConfigurations( ) ) {
          LOG.info( "-> " + cfg.getUrl( ) );
        }
      }
    }
  }

  public static void initBootstrappers( ) {
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( Component.eucalyptus.name() ) && f.getName( ).endsWith( ".jar" ) && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        ServiceJarFile jar;
        try {
          jar = new ServiceJarFile( f );
        } catch ( IOException e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
        List<Bootstrapper> bsList = jar.getBootstrappers( );
        for ( Bootstrapper bootstrap : bsList ) {
          for ( Resource r : Resource.values( ) ) {
            if ( r.providedBy( bootstrap.getClass( ) ) || Resource.Nothing.equals( r ) ) {
              Provides provides = bootstrap.getClass( ).getAnnotation( Provides.class );
              if( provides == null ) {
                LOG.info( "-X Skipping bootstrapper " + bootstrap.getClass( ).getSimpleName( ) + " since Provides is not specified." );
              } else {
                Component component = provides.component( );
                if ( component != null && !component.isEnabled( ) ) {
                  LOG.info( "-X Skipping bootstrapper " + bootstrap.getClass( ).getSimpleName( ) + " since Provides.component=" + component.toString( ) + " is disabled." );
                  break;
                }
                if ( checkDepends( bootstrap ) ) {
                  r.add( bootstrap );
                  LOG.info( "-> Associated bootstrapper " + bootstrap.getClass( ).getSimpleName( ) + " with resource " + r.toString( ) + "." );
                  break;
                }
              }
            }
          }
        }
      }
    }
  }

  private static boolean checkDepends( Bootstrapper bootstrap ) {
    Depends depends = bootstrap.getClass( ).getAnnotation( Depends.class );
    if( depends == null ) return true;
    for ( Component c : depends.local( ) ) {
      if ( !c.isLocal( ) ) {
        LOG.info( "-X Skipping bootstrapper " + bootstrap.getClass( ).getSimpleName( ) + " since Depends.local=" + c.toString( ) + " is remote." );
        return false;
      }
    }
    for ( Component c : depends.remote( ) ) {
      if ( c.isLocal( ) ) {
        LOG.info( "-X Skipping bootstrapper " + bootstrap.getClass( ).getSimpleName( ) + " since Depends.remote=" + c.toString( ) + " is local." );
        return false;
      }
    }
    return true;
  }

}
