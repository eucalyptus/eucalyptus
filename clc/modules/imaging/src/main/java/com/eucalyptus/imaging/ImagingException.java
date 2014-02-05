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
import com.eucalyptus.compute.conversion.ImageManifestException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class ImagingException extends EucalyptusWebServiceException {
  
  protected ImagingException( 
                               final String code, 
                               final Role role, 
                               final String message ) {
    super( code, role, message );
  }

  public static <T extends ImagingException> T rethrow( Class<T> type, String message ) {
    try {
      return type.getConstructor( new Class[] { String.class } ).newInstance( new Object[] { message } );
    } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
}