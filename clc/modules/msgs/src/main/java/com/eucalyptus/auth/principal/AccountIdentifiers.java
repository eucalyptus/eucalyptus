/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
