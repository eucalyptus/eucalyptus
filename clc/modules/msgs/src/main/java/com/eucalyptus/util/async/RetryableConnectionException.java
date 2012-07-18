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

package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * Exceptions of this type indicate that a connection error occured before the request was written.
 */
public class RetryableConnectionException extends ConnectionException {

  public RetryableConnectionException( BaseMessage msg ) {
    super( msg );
  }

  public RetryableConnectionException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public RetryableConnectionException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public RetryableConnectionException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }

}
