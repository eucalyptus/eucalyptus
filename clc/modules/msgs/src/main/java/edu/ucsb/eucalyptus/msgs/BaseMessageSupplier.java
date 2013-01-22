/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package edu.ucsb.eucalyptus.msgs;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.google.common.base.Supplier;

/**
 * A wrapping for BaseMessage with additional binding specific details.
 */
public class BaseMessageSupplier implements Supplier<BaseMessage>{

  private final BaseMessage baseMessage;
  private final HttpResponseStatus status;
  
  public BaseMessageSupplier( final BaseMessage baseMessage,
                              final HttpResponseStatus status ) {
    this.baseMessage = baseMessage;
    this.status = status;
  }

  public BaseMessage getBaseMessage( ) {
    return baseMessage;
  }

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  @Override
  public BaseMessage get( ) {
    return getBaseMessage( );
  }
}
