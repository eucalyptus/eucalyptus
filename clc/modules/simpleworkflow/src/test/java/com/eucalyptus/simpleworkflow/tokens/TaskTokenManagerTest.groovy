/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.tokens

import groovy.transform.CompileStatic
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.hamcrest.Matcher
import org.junit.BeforeClass

import javax.crypto.Cipher
import java.security.Security

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.*
import org.junit.Test

import java.util.concurrent.TimeUnit

import static org.junit.Assume.assumeThat


/**
 *
 */
@CompileStatic
class TaskTokenManagerTest {

  @BeforeClass
  static void beforeClass() {
    if ( Security.getProvider( (String)BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      Security.addProvider( new BouncyCastleProvider( ) )
    }
    assumeThat( "Unlimited strength cryptography available", (Integer)Cipher.getMaxAllowedKeyLength("AES"), (Matcher<Integer>)equalTo( Integer.MAX_VALUE ) )
  }

  @Test
  void testTaskTokenRoundTrip( ) {
    TaskTokenManager manager = new TaskTokenManager( ) {
      @Override
      protected String getTokenPassword( ) {
        'Open Sesame'
      }
    }

    String accountNumber = '123456789012'
    String domainUuid = UUID.randomUUID( ).toString( )
    String runId = UUID.randomUUID( ).toString( )
    Long scheduledEventId = 1
    Long startedEventId = 3
    Long created = System.currentTimeMillis( )
    Long expires = System.currentTimeMillis( ) + TimeUnit.DAYS.toMillis( 7 )
    TaskToken token = new TaskToken( accountNumber, domainUuid, runId, scheduledEventId, startedEventId, created, expires )

    String encryptedToken = manager.encryptTaskToken( token )
    println( encryptedToken )
    String encryptedToken2 = manager.encryptTaskToken( token )

    assertNotEquals( "Token cipher text duplicated", encryptedToken, encryptedToken2 )

    TaskToken result = manager.decryptTaskToken( accountNumber, encryptedToken )
    assertEquals( "Account number", token.accountNumber, result.accountNumber )
    assertEquals( "Domain UUID", token.domainUuid, result.domainUuid )
    assertEquals( "Run ID", token.runId, result.runId )
    assertEquals( "Scheduled Event ID", token.scheduledEventId, result.scheduledEventId )
    assertEquals( "Started Event ID", token.startedEventId, result.startedEventId )
    assertEquals( "Created Timestamp", token.created, result.created )
    assertEquals( "Expires Timestamp", token.expires, result.expires )
  }

}
