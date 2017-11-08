/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
