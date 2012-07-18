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

package com.eucalyptus.auth.api;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class NoSuchCertificateException extends BaseSecurityException {

  public NoSuchCertificateException( ) {
    super( );
  }

  public NoSuchCertificateException( HttpResponseStatus status ) {
    super( status );
  }

  public NoSuchCertificateException( String message, HttpResponseStatus status ) {
    super( message, status );
  }

  public NoSuchCertificateException( String message, Throwable cause, HttpResponseStatus status ) {
    super( message, cause, status );
  }

  public NoSuchCertificateException( String message, Throwable cause ) {
    super( message, cause );
  }

  public NoSuchCertificateException( String message ) {
    super( message );
  }

  public NoSuchCertificateException( Throwable cause, HttpResponseStatus status ) {
    super( cause, status );
  }

  public NoSuchCertificateException( Throwable cause ) {
    super( cause );
  }

}
