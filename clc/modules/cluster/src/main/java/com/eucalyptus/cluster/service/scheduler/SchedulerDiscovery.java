/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.scheduler;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class SchedulerDiscovery extends ServiceJarDiscovery {
  private static final Logger logger = Logger.getLogger( SchedulerDiscovery.class );

  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( Scheduler.class.isAssignableFrom( candidate ) &&
        Modifier.isPublic( candidate.getModifiers( ) ) &&
        !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        final Scheduler scheduler = (Scheduler) candidate.newInstance( );
        final String name = scheduler.name( );
        logger.info( "Registering scheduler : " + name + " " + candidate  );
        Schedulers.register( name, (Class<? extends Scheduler>)candidate );
        return true;
      } catch ( final Exception ex ) {
        logger.error( "Error in scheduler discovery for " + candidate, ex );
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
}
