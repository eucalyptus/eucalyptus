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

import com.eucalyptus.simpleworkflow.common.stateful.PolledNotificationChecker;
import org.apache.log4j.Logger;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 *
 */
abstract class AbstractTaskPolledNotificationChecker implements PolledNotificationChecker {

  private static final Logger logger = Logger.getLogger( AbstractTaskPolledNotificationChecker.class );

  private final String type;

  protected AbstractTaskPolledNotificationChecker( final String type ) {
    this.type = type;
  }

  @Override
  public boolean apply( final String channel ) {
    final Iterable<String> channelName = Splitter.on(':').limit( 4 ).split( channel );
    if ( Iterables.size( channelName ) == 4 ) {
      final String accountNumber = Iterables.get( channelName, 0 );
      final String type = Iterables.get( channelName, 1 );
      final String domain = Iterables.get( channelName, 2 );
      final String taskList = Iterables.get( channelName, 3 );
      if ( this.type.equals( type ) ) try {
        return hasTasks( accountNumber, domain, taskList );
      } catch ( final Exception e ) {
        logger.error( "Error checking for pending tasks", e );
      }
    }
    return false;
  }

  abstract boolean hasTasks( String accountNumber, String domain, String taskList );
}
