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

/**
 *
 */
public interface AccountIdentifiers {
  String NOBODY_ACCOUNT = "nobody";
  Long NOBODY_ACCOUNT_ID = 1l;

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
  //EUCA-9376 - Workaround to avoid multiple admin users in the blockstorage account due to EUCA-9635
  String BLOCKSTORAGE_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "blockstorage";

  //EUCA-9644 - CloudFormation account for buckets and user to launch SWF workflows
  String CLOUDFORMATION_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "cloudformation";

  //EUCA-9533 - System account for pre-signed urls in download manifests
  String AWS_EXEC_READ_SYSTEM_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "aws-exec-read";

  // EUCA-8667 - System account for osg <--> walrus
  String OBJECT_STORAGE_WALRUS_ACCOUNT = SYSTEM_ACCOUNT_PREFIX + "objectstorage";

  String getAccountNumber( );
  String getAccountAlias( );
  String getCanonicalId( );
}
