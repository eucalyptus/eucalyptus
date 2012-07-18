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

package com.eucalyptus.cloud.run;

import static com.eucalyptus.cloud.run.Allocations.Allocation;
import java.util.Date;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.context.Context;
import com.google.common.base.Predicate;

/**
 * Applies contextual contract information to an allocation
 */
public class ContractEnforcement {

  public static Predicate<Allocation> run( ) {
    return RunContractEnforcement.INSTANCE;
  }

  enum RunContractEnforcement implements Predicate<Allocation> {
    INSTANCE;

    @Override
    public boolean apply( final Allocation allocInfo ) {
      final Context context = allocInfo.getContext();

      if ( !context.hasAdministrativePrivileges() ) {
        final Contract<Date> expiry = context.getContracts( ).get( Contract.Type.EXPIRATION );
        if ( expiry != null ) {
          allocInfo .setExpiration( expiry.getValue() );
        }
      }

      return true;
    }
  }
}
