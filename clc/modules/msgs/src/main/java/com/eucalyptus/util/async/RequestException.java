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

public class RequestException extends RuntimeException {
  final BaseMessage msg;

  public RequestException( BaseMessage msg ) {
    super( msg.toSimpleString( ) );
    this.msg = msg;
  }

  public RequestException( String message, Throwable ex, BaseMessage msg ) {
    super( message + ":" + msg.toSimpleString( ), ex );
    this.msg = msg;
  }

  public RequestException( String message, BaseMessage msg ) {
    super( message + ":" + msg.toSimpleString( ) );
    this.msg = msg;
  }

  public RequestException( Throwable ex, BaseMessage msg ) {
    super( ex.getMessage( ) + ":" + msg.toSimpleString( ),  ex );
    this.msg = msg;
  }

  public <T extends BaseMessage> T getRequest( ) {
    return ( T ) this.msg;
  }

}
