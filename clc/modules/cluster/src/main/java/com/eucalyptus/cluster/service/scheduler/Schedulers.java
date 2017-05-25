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

import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.util.Assert;
import com.google.common.collect.Maps;

/**
 *
 */
public class Schedulers {

  private static final Logger logger = Logger.getLogger( Schedulers.class );
  private static final ConcurrentMap<String,Class<? extends Scheduler>> schedulerTypesByName = Maps.newConcurrentMap( );

  /**
   * Get the scheduler with the given name or the default
   * if none found.
   *
   * @return A scheduler, perhaps the one you asked for.
   */
  @Nonnull
  public static Scheduler forName( final String name ) {
    try {
      return schedulerTypesByName.getOrDefault( name, RoundRobinScheduler.class ).newInstance( );
    } catch ( final Exception e ) {
      logger.error(
          "Error creating scheduler for " + name + ": " + e.getMessage( ),
          logger.isDebugEnabled( ) ? e : null );
      return new RoundRobinScheduler( );
    }
  }

  /**
   * Resource for scheduling scope.
   */
  public static ScheduleResource context( ) {
    return new ScheduleResource( );
  }

  static void register( final String name, final Class<? extends Scheduler> scheduler ) {;
    if ( schedulerTypesByName.putIfAbsent(
        Assert.notNull( name, "name" ),
        Assert.notNull( scheduler, "scheduler" ) ) != null ) {
      throw new IllegalStateException( "Duplicate scheduler registration : " + name + " " + scheduler +
          "(registered type "+schedulerTypesByName.get( name )+")" );
    }
  }

}
