/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * <p/>
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare.identity;

import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 *
 */
public class IdentityServiceException extends EucalyptusWebServiceException {
  private static final long serialVersionUID = 1L;

  public IdentityServiceException( final String code, final Role role, final String message ) {
    super( code, role, message );
  }
}
