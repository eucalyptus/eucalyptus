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
package com.eucalyptus.tokens

import com.eucalyptus.auth.euare.UserPrincipalImpl
import com.eucalyptus.auth.principal.PolicyVersion
import com.eucalyptus.auth.principal.SecurityTokenContent
import com.eucalyptus.auth.principal.UserPrincipal
import com.eucalyptus.auth.tokens.SecurityToken
import com.eucalyptus.auth.tokens.SecurityTokenValidationException
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.crypto.Cipher

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString
import static org.junit.Assert.*
import org.hamcrest.Matcher
import org.junit.Test
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.User
import java.util.concurrent.TimeUnit

import org.junit.BeforeClass
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.principal.Certificate
import com.google.common.base.Optional as GOptional

import static org.junit.Assume.assumeThat

/**
 * Unit tests for security token manager
 */
@CompileStatic
class SecurityTokenManagerTest {

  @BeforeClass
  static void beforeClass() {
    if ( Security.getProvider( (String)BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      Security.addProvider( new BouncyCastleProvider( ) )
    }
    assumeThat( "Unlimited strength cryptography available", (Integer)Cipher.getMaxAllowedKeyLength("AES"), (Matcher<Integer>)equalTo( Integer.MAX_VALUE ) )
  }

  @Test
  void testIssueTokenWithAccessKey() {
    long now = System.currentTimeMillis()
    long desiredLifetimeHours = 6

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
      SecurityToken token = manager.doIssueSecurityToken(
        Principals.nobodyUser(),
        testKey,
          0,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Invalid access key identifier", token.getAccessKeyId(), not(isEmptyOrNullString()) )
    assertThat( "Invalid secret key", token.getSecretKey(), not(isEmptyOrNullString())  )
    assertThat( "Duplicate access key identifier", token.getAccessKeyId(), not(equalTo(testKey.getAccessKey())) )
    assertThat( "Duplicate secret key", token.getSecretKey(), not(equalTo(testKey.getSecretKey())) )
    assertThat( "Security token empty", token.getToken(), not(isEmptyOrNullString()) )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( desiredLifetimeHours ) ) )
  }

  @Test
  void testLookupTokenWithAccessKey() {
    long now = System.currentTimeMillis()
    long desiredLifetimeHours = 6

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey, Principals.nobodyUser() )
    SecurityToken token = manager.doIssueSecurityToken(
        Principals.nobodyUser(),
        testKey,
        0,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    AccessKey tokenKey = manager.doLookupAccessKey( token.getAccessKeyId(), token.getToken() )
    assertThat( "Null key", tokenKey, notNullValue() )
    assertThat( "Invalid access key identifier", tokenKey.getAccessKey(), equalTo(token.getAccessKeyId()) )
    assertThat( "Invalid secret key", tokenKey.getSecretKey(), equalTo(token.getSecretKey())  )
    assertThat( "Invalid user", tokenKey.getPrincipal().getUserId(), equalTo(Principals.nobodyUser().getUserId())  )
    assertThat( "Invalid creation time", tokenKey.getCreateDate(), equalTo( new Date(now) ) )
    assertThat( "Invalid creation time", tokenKey.isActive(), equalTo(true) )
  }

  /**
   * WithPassword tests are for console authentication case
   */
  @Test
  void testIssueTokenWithPassword() {
    long now = System.currentTimeMillis()
    long desiredLifetimeHours = 6

    UserPrincipal user = user();
    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey, user )
    SecurityToken token = manager.doIssueSecurityToken(
        user,
        0,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Invalid access key identifier", token.getAccessKeyId(), not(isEmptyOrNullString()) )
    assertThat( "Invalid secret key", token.getSecretKey(), not(isEmptyOrNullString())  )
    assertThat( "Duplicate access key identifier", token.getAccessKeyId(), not(equalTo(testKey.getAccessKey())) )
    assertThat( "Duplicate secret key", token.getSecretKey(), not(equalTo(testKey.getSecretKey())) )
    assertThat( "Security token empty", token.getToken(), not(isEmptyOrNullString()) )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( desiredLifetimeHours ) ) )
  }

  /**
   * WithPassword tests are for console authentication case
   */
  @Test
  void testLookupTokenWithPassword() {
    long now = System.currentTimeMillis()
    long desiredLifetimeHours = 6

    User user = user();
    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey, user )
    SecurityToken token = manager.doIssueSecurityToken(
        user,
        0,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    AccessKey tokenKey = manager.doLookupAccessKey( token.getAccessKeyId(), token.getToken() )
    assertThat( "Null key", tokenKey, notNullValue() )
    assertThat( "Invalid access key identifier", tokenKey.getAccessKey(), equalTo(token.getAccessKeyId()) )
    assertThat( "Invalid secret key", tokenKey.getSecretKey(), equalTo(token.getSecretKey())  )
    assertThat( "Invalid user", tokenKey.getPrincipal().getUserId(), equalTo(user.getUserId())  )
    assertThat( "Invalid creation time", tokenKey.getCreateDate(), equalTo( new Date(now) ) )
    assertThat( "Invalid creation time", tokenKey.isActive(), equalTo(true) )
  }

  @Test
  void testMinimumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 0, TimeUnit.MINUTES.toSeconds( 15 ) as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.MINUTES.toMillis( 15 ) ) )
  }

  @Test(expected = SecurityTokenValidationException)
  void testExceedMinimumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 0, TimeUnit.MINUTES.toSeconds( 15 ) - 1 as Integer )
  }

  @Test
  void testMaximumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 0, TimeUnit.HOURS.toSeconds( 36 ) as Integer )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 36 ) ) )
  }

  @Test(expected = SecurityTokenValidationException)
  void testExceedMaximumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 0, TimeUnit.HOURS.toMillis( 36 ) + 1 as Integer  )
  }

  @Test
  void testMaximumTokenDurationForAdminUsers() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.systemUser(), testKey, 0, TimeUnit.HOURS.toSeconds( 1 ) as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) )
  }

  @Test
  void testExceedMaximumTokenDurationForAdminUsers() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.systemUser(), testKey, TimeUnit.HOURS.toSeconds( 1 ) as Integer, TimeUnit.HOURS.toSeconds( 1 ) + 1 as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) ) // duration should be truncated to 1 hour
  }

  @Test(expected=AuthException.class)
  void testUserMismatch() {
    long now = System.currentTimeMillis()
    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManagerImpl manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 0, 300 )
  }

  private static AccessKey accessKey( long created, UserPrincipal owner ) {
    accessKey( created, owner, 'VXCDGDDNO5L89OSHF1LHF', '8jbLUrY34CsXQ8oIOMplYhYDhbrXumfrsJ4SB4aX' )
  }

  private static AccessKey accessKey( long created, UserPrincipal owner, String accessKey, String secretKey ) {
    new AccessKey(){
      @Override Boolean isActive() { true }
      @Override String getAccessKey() { accessKey }
      @Override String getSecretKey() { secretKey }
      @Override Date getCreateDate() { new Date( created ) }
      @Override UserPrincipal getPrincipal() { owner }
    }
  }

  private SecurityTokenManagerImpl manager( long now, AccessKey keyForLookup, UserPrincipal userForLookup = null ) {
    new SecurityTokenManagerImpl() {
      @Override protected String getSecurityTokenPassword() { "password" }
      @Override protected long getCurrentTimeMillis() { now }
      @Override protected UserPrincipal lookupByUserById(final String userId, final String nonce) throws AuthException {
        assertThat( "Correct user id", userId, equalTo(userForLookup?.getUserId()) )
        String secret = doGenerateSecret( nonce, userForLookup.getToken( ) )
        final Collection<AccessKey> keys = Collections.singleton( accessKey( 0, null, null, secret ) )
        new UserPrincipalImpl( userForLookup, keys )
      }
      @Override protected UserPrincipal lookupByRoleById(final String roleId, final GOptional<String> sessionName, final String nonce) throws AuthException {
        new UserPrincipalImpl( userForLookup, Collections.singleton( keyForLookup ) )
      }
      @Override protected UserPrincipal lookupByAccessKeyId(final String accessKeyId, final String nonce) throws AuthException {
        assertThat( "Correct original access key id", accessKeyId, equalTo(keyForLookup.getAccessKey()) )
        String secret = doGenerateSecret( nonce, keyForLookup.getSecretKey( ) )
        final Collection<AccessKey> keys = Collections.singleton( accessKey( 0, null, null, secret ) )
        new UserPrincipalImpl( userForLookup, keys )
      }
      @Override protected SecurityTokenContent doDispatchingDecode(final String accessKeyId, final String token) throws AuthException {
        doDecode( accessKeyId, token )
      }
    }
  }

  private UserPrincipal user() {
    new UserPrincipal() {
      @Nonnull
      @Override String getAuthenticatedId() { userId }
      @Nonnull
      @Override String getAccountAlias( ) { 'alias' }
      @Nonnull
      @Override String getCanonicalId() { 'iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiid' }
      @Nonnull
      @Override String getUserId() { "EXAMPLEUUFKQWVU6MBZWL" }
      @Nonnull
      @Override String getPath() { "" }
      @Override boolean isEnabled() { true }
      @Override String getToken() { "examplefnBk4b4TfjAjRMhjmu7eBjNmpvR8llmHPfXKzYw1s3vC8tDMCeEk02OKxuXBZJeDMbaynoo6N" }
      @Override String getPassword() { null }
      @Override Long getPasswordExpires() { null }
      @Nonnull
      @Override List<AccessKey> getKeys() { [] }
      @Nonnull
      @Override List<Certificate> getCertificates() { [] }
      @Nonnull
      @Override String getAccountNumber() { null }
      @Override boolean isAccountAdmin() { false }
      @Override boolean isSystemAdmin() { false }
      @Override boolean isSystemUser() { false }
      @Nonnull
      @Override List<PolicyVersion> getPrincipalPolicies() { [] }
      @Nonnull
      @Override String getName() { "test-user-1" }
      @Override String getPTag() { null }
    }
  }
}
