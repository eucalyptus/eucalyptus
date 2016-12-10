/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.principal;

import javax.annotation.Nonnull;
import com.google.common.base.Function;

/**
 *
 */
public interface AccountIdentifiers {
  String NOBODY_ACCOUNT = "nobody";
  Long NOBODY_ACCOUNT_ID = 1l;
  String NOBODY_CANONICAL_ID = "65a011a29cdf8ec533ec3d1ccaae921c"; // Matches AWS magic number

  /**
   * <h2>NOTE:GRZE:</h2> there will <b>always</b> be an account named <tt>eucalyptus</tt>. The name is used
   * in a variety of ways as an input and identifier during system bootstrap. That is, not local
   * host bootstrap. So, this is before any other identifier information is created. To support any
   * simplifications to install, let alone unattended installs, this value MUST be hardcoded -- it
   * is the account which all system services use to bootstrap, including initial configuration.
   */
  String SYSTEM_ACCOUNT = "eucalyptus";
  String SYSTEM_ACCOUNT_PREFIX = "(eucalyptus)";
  Long SYSTEM_ACCOUNT_ID = 0l;
  String SYSTEM_CANONICAL_ID = "0"; // Should never be used as a lookup key;
  
  //EUCA-9376 - Workaround to avoid multiple admin users in the blockstorage account due to EUCA-9635
  String BLOCKSTORAGE_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "blockstorage";

  //EUCA-9644 - CloudFormation account for buckets and user to launch SWF workflows
  String CLOUDFORMATION_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "cloudformation";

  //EUCA-9533 - System account for pre-signed urls in download manifests
  String AWS_EXEC_READ_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "aws-exec-read";

  // EUCA-8667 - System account for osg <--> walrus
  String OBJECT_STORAGE_WALRUS_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "objectstorage";
  
  // EUCA-10790 - System account for elb
  String ELB_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "loadbalancing";

  // EUCA-10102
  String IMAGING_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "imaging";
  String DATABASE_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "database";

  // EUCA-10471 - System account for sqs, for cloudwatch metrics
  String SIMPLEQUEUE_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "simplequeue";

  String getAccountNumber( );
  String getAccountAlias( );
  String getCanonicalId( );

  static class Properties {
    public static Function<AccountIdentifiers,String> accountNumber( ) {
      return StringProperties.NUMBER;
    }

    public static Function<AccountIdentifiers,String> accountCanonicalId( ) {
      return StringProperties.CANONICAL_ID;
    }

    private enum StringProperties implements Function<AccountIdentifiers,String> {
      NUMBER {
        @Nonnull
        @Override
        public String apply( final AccountIdentifiers accountIdentifiers ) {
          return accountIdentifiers.getAccountNumber( );
        }
      },
      CANONICAL_ID {
        @Nonnull
        @Override
        public String apply( final AccountIdentifiers accountIdentifiers ) {
          return accountIdentifiers.getCanonicalId( );
        }
      }
    }
  }
}
