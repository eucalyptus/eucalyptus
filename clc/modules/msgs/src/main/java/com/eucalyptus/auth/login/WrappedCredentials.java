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

package com.eucalyptus.auth.login;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

public abstract class WrappedCredentials<TYPE> implements CallbackHandler {
  private TYPE loginData;
  private String correlationId;
  public WrappedCredentials( String correlationId, TYPE loginData ) {
    super( );
    this.loginData = loginData;
    this.correlationId = correlationId;
  }

  @Override
  public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException {}

  public TYPE getLoginData( ) {
    return this.loginData;
  }

  public String getCorrelationId( ) {
    return this.correlationId;
  }
  
}
