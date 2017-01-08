/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.auth.euare.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.PolicyScope;

/**
 *
 */
public abstract class EuareQuotaKey extends QuotaKey {

  protected String unsupportedValue( final PolicyScope scope ) throws AuthException {
    switch ( scope ) {
      case Group:
        return NOT_SUPPORTED;
      case User:
        return NOT_SUPPORTED;
    }
    throw new AuthException( "Invalid scope" );
  }
}
