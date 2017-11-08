/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
