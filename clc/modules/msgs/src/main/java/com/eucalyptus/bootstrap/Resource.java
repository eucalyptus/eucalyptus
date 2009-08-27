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
package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import com.google.common.collect.Lists;

public enum Resource {
  PrivilegedContext( ),
  SystemCredentials( ),
  RemoteConfiguration( ),
  Database( ),
  PersistenceContext( ),
  ClusterCredentials( ),
  RemoteServices( ),
  UserCredentials( ),
  CloudService( ),
  SpringService( ),
  Nothing( );
  private String                 resourceName;
  private static Logger          LOG = Logger.getLogger( Resource.class );
  private List<Bootstrapper>     bootstrappers;
  private List<ResourceProvider> providers;

  private Resource( ) {
    this.resourceName = String.format( "com.eucalyptus.%sProvider", this.name( ) );
    this.bootstrappers = Lists.newArrayList( );
  }

  public List<ResourceProvider> getProviders( ) {
    synchronized ( this ) {
      if ( this.providers == null ) {
        this.providers = Lists.newArrayList( );
        Enumeration<URL> p1;
        try {
          p1 = Thread.currentThread( ).getContextClassLoader( ).getResources( this.resourceName );
          try {
            URL u = null;
            while ( p1.hasMoreElements( ) ) {
              u = p1.nextElement( );
              LOG.debug( "Found resource provider: " + u );
              Properties props = new Properties( );
              props.load( u.openStream( ) );
              ResourceProvider p = new ResourceProvider( this, props, u );
              providers.add( p );
            }
          } catch ( IOException e ) {
            LOG.error( e, e );
          }
        } catch ( IOException e1 ) {
          LOG.error( e1, e1 );
        }
      }
      return providers;
    }
  }

  public boolean providedBy( Class clazz ) {
    for ( Annotation a : clazz.getAnnotations( ) ) {
      if ( a instanceof Provides && this.equals( ( ( Provides ) a ).resource( ) ) ) { return true; }
    }
    return false;
  }

  public boolean satisfiesDependency( Class clazz ) {
    Depends d = ( Depends ) clazz.getAnnotation( Depends.class );//TODO: lame AST parser complains about this and requires cast...
    if( d != null && Lists.newArrayList( d.resources( ) ).contains( this ) ) {
      return true;
    }
    return false;
  }

  public boolean add( Bootstrapper b ) {
    return this.bootstrappers.add( b );
  }

  public List<Bootstrapper> getBootstrappers( ) {
    return this.bootstrappers;
  }

  public boolean add( ResourceProvider p ) {
    return this.providers.add( p );
  }

  public List<ResourceProvider> initProviders( ) throws IOException {
    for ( ResourceProvider p : this.providers ) {
      p.initConfigurationResources( );
    }
    return this.providers;
  }

}
