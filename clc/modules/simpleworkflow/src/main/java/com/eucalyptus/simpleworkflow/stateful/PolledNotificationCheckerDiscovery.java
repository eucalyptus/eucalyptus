/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.stateful;

import java.lang.reflect.Modifier;
import java.util.concurrent.CopyOnWriteArrayList;

import com.eucalyptus.simpleworkflow.common.stateful.PolledNotificationChecker;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

/**
 *
 */
public class PolledNotificationCheckerDiscovery extends ServiceJarDiscovery {

  private static final Logger logger = Logger.getLogger( PolledNotificationCheckerDiscovery.class );

  private static final CopyOnWriteArrayList<PolledNotificationChecker> checkers = new CopyOnWriteArrayList<>( );

  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( PolledNotificationChecker.class.isAssignableFrom( candidate ) &&
        !Modifier.isAbstract( candidate.getModifiers() ) &&
        !Modifier.isInterface( candidate.getModifiers( ) ) &&
        !candidate.isLocalClass( ) &&
        !candidate.isAnonymousClass( ) ) {
      try {
        checkers.add( (PolledNotificationChecker) candidate.newInstance() );
        logger.debug( "Discovered polled notification checker: " + candidate.getName( ) );
      } catch ( final Throwable ex ) {
        logger.error( "Error in discovery for " + candidate.getName( ), ex );
      }
      return true;
    } else {
      return false;
    }
  }

  static Supplier<Iterable<PolledNotificationChecker>> supplier( ) {
    return new Supplier<Iterable<PolledNotificationChecker>>() {
      @Override
      public Iterable<PolledNotificationChecker> get() {
        return Iterables.unmodifiableIterable( checkers );
      }
    };
  }

  @Override
  public Double getPriority( ) {
    return 0.3d;
  }
}
