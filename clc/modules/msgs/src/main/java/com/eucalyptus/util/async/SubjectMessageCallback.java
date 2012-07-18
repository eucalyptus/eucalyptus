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

public abstract class SubjectMessageCallback<P, Q extends BaseMessage, R extends BaseMessage> extends MessageCallback<Q, R> {
  private P subject;
  
  protected SubjectMessageCallback( ) {
    super( );
  }

  protected SubjectMessageCallback( P subject ) {
    this.subject = subject;
  }

  protected SubjectMessageCallback( Q request ) {
    super( request );
  }

  protected SubjectMessageCallback( P subject, Q request ) {
    super( request );
    this.subject = subject;
  }

  /**
   * @return the subject
   */
  public P getSubject( ) {
    if ( this.subject == null ) {
      throw new IllegalStateException( "The subject associated with the callback has not been set." );
    }
    return this.subject;
  }
  
  /**
   * @param subject the subject to set
   */
  public void setSubject( P subject ) {
    this.subject = subject;
  }
  
}
