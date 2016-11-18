/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.tokens.common.policy

import com.eucalyptus.auth.policy.ern.Ern
import org.junit.BeforeClass
import org.junit.Test

/**
 *
 */
class StsErnTest {

  @BeforeClass
  static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new StsErnBuilder( ) )
  }

  @Test
  void testParseValid( ) {
    List<String> arns = [
        'arn:aws:sts::123456789012:federated-user/Bob',
        'arn:aws:sts::123456789012:assumed-role/Accounting-Role/Mary',
    ]
    for ( String arn : arns ) {
      Ern.parse( arn )
    }
  }
}
