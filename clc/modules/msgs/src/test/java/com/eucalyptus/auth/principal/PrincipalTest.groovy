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
package com.eucalyptus.auth.principal

import com.eucalyptus.auth.policy.ern.Ern
import com.eucalyptus.auth.policy.ern.EuareErnBuilder
import org.junit.BeforeClass

import static org.junit.Assert.*
import org.junit.Test

import static com.eucalyptus.auth.principal.Principal.PrincipalType.AWS

/**
 * 
 */
class PrincipalTest {

  @BeforeClass
  static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new EuareErnBuilder( ) )
  }
  
  @Test
  void testPrincipalMatcherConversion() {
    [
        "*":                                       "*",
        "arn:aws:iam::123456789012:root":          "arn:aws:iam::123456789012:root",
        "123456789012":                            "arn:aws:iam::123456789012:root",
        "arn:aws:iam::123456789012:user/Username": "arn:aws:iam::123456789012:user/Username",
        "arn:aws:iam::123456789012:user/*":        "arn:aws:iam::123456789012:user/*",
        "arn:aws:iam::123456789012:role/p/MyRole": "arn:aws:iam::123456789012:role/p/MyRole",
        "arn:aws:iam::123456789012:role/*":        "arn:aws:iam::123456789012:role/*",
    ].each { input, expected ->
      assertEquals( "Match conversion for " + input, expected, AWS.convertForUserMatching( input ) )
    }
  }
}
