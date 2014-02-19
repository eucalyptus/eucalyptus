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
package com.eucalyptus.imaging;

import java.lang.reflect.InvocationTargetException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class ImagingServiceException extends EucalyptusWebServiceException {
  public static final Role DEFAULT_ROLE = Role.Sender;
  public static final String DEFAULT_CODE = "400"; 
  
  public static final String INTERNAL_SERVER_ERROR = "500";
  
  protected ImagingServiceException( 
                               final String code, 
                               final Role role, 
                               final String message ) {
    super( code, role, message );
  }

  public ImagingServiceException(final String code, final String message){
    this( code, DEFAULT_ROLE, message);
  }
  
  public ImagingServiceException(final String code, final String message, final Throwable inner){
    this(code, DEFAULT_ROLE, message);
    this.initCause(inner);
  }
  
  public ImagingServiceException(final String message){
    this(DEFAULT_CODE, DEFAULT_ROLE, message);
  }
  public ImagingServiceException(final String message, Throwable inner){
    this(DEFAULT_CODE, DEFAULT_ROLE, message);
    this.initCause(inner);
  }
  public static <T extends ImagingServiceException> T rethrow( Class<T> type, String message ) {
    try {
      return type.getConstructor( new Class[] { String.class } ).newInstance( new Object[] { message } );
    } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
}