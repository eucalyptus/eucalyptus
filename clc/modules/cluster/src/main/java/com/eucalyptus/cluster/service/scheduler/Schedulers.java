/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cluster.service.scheduler;

import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Assert;
import com.google.common.collect.Maps;
import groovy.lang.Closure;

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

  public static <R> R withContext( final Closure<R> closure ) {
    try ( final ScheduleResource context = context( ) ) {
      return closure.call( context );
    }
  }

  public static <R> R withAutoCommitContext( final Closure<R> closure ) {
    try ( final ScheduleResource context = context( ) ) {
      R r = closure.call( context );
      context.commit( );
      return r;
    }
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
