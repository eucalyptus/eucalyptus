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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.google.common.collect.Lists;

public class BootstrapperDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( BootstrapperDiscovery.class );
  private static List<Class>         bootstrappers = Lists.newArrayList( );

  public BootstrapperDiscovery() {}
  
  /**
   * Find 
   * @see com.eucalyptus.bootstrap.ServiceJarDiscovery#processClass(java.lang.Class)
   * @param candidate
   * @return
   * @throws Exception
   */
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    String bc = candidate.getCanonicalName( );
    Class bootstrapper = this.getBootstrapper( candidate );
    if ( !Ats.from( candidate ).has( RunDuring.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify execution stage (RunDuring.value=Bootstrap.Stage): " + bc );
    } else if ( !Ats.from( candidate ).has( Provides.class ) ) {
      throw BootstrapException.throwFatal( "Bootstrap class does not specify provided component (Provides.value=Component): " + bc );
    }
    this.bootstrappers.add( bootstrapper );
    return true;
  }

  @SuppressWarnings( "unchecked" )
  public static List<Bootstrapper> getBootstrappers( ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : bootstrappers ) {
      if ( c.equals( SystemBootstrapper.class ) ) continue;
      try {
        EventRecord.here( BootstrapperDiscovery.class, EventType.BOOTSTRAPPER_INIT,"<init>()V", c.getCanonicalName( ) ).info( );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          EventRecord.here( BootstrapperDiscovery.class, EventType.BOOTSTRAPPER_INIT,"getInstance()L", c.getCanonicalName( ) ).info( );
          try {
            Method m = c.getDeclaredMethod( "getInstance", new Class[] {} );
            ret.add( ( Bootstrapper ) m.invoke( null, new Object[] {} ) );
          } catch ( NoSuchMethodException ex ) {
            throw BootstrapException.throwFatal( "Error in <init>()V in bootstrapper: " + c.getCanonicalName( ), e );
          }
        }
      } catch ( Exception e ) {
        throw BootstrapException.throwFatal( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ), e );
      }
    }
    return ret;
  }

  @SuppressWarnings( "unchecked" )
  private Class getBootstrapper( Class candidate ) throws Exception {
    if ( Modifier.isAbstract( candidate.getModifiers( ) ) ) throw new InstantiationException( candidate.getName( ) + " is abstract." );
    if ( !Bootstrapper.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + Bootstrapper.class );
    LOG.trace( "Candidate bootstrapper: " + candidate.getName( ) );
    if ( !Modifier.isPublic( candidate.getDeclaredConstructor( new Class[] {} ).getModifiers( ) ) ) {
      Method factory = candidate.getDeclaredMethod( "getInstance", new Class[] {} );
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) {
        throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" );
      }
    }
    EventRecord.here( ServiceJarDiscovery.class, EventType.BOOTSTRAPPER_ADDED, candidate.getName() ).info( );
    return candidate;
  }

  @Override
  public Double getPriority( ) {
    return 0.0d;
  }
  
}
