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
package com.eucalyptus.auth.tokens

import groovy.transform.CompileStatic

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
import java.security.cert.X509Certificate
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.Policy
import com.eucalyptus.auth.principal.Authorization

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
    SecurityTokenManager manager = manager( now, testKey )
      SecurityToken token = manager.doIssueSecurityToken(
        Principals.nobodyUser(),
        testKey,
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
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken(
        Principals.nobodyUser(),
        testKey,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    AccessKey tokenKey = manager.doLookupAccessKey( token.getAccessKeyId(), token.getToken() )
    assertThat( "Null key", tokenKey, notNullValue() )
    assertThat( "Invalid access key identifier", tokenKey.getAccessKey(), equalTo(token.getAccessKeyId()) )
    assertThat( "Invalid secret key", tokenKey.getSecretKey(), equalTo(token.getSecretKey())  )
    assertThat( "Invalid user", tokenKey.getUser().getUserId(), equalTo(Principals.nobodyUser().getUserId())  )
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

    User user = user();
    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey, user )
    SecurityToken token = manager.doIssueSecurityToken(
        user,
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
    SecurityTokenManager manager = manager( now, testKey, user )
    SecurityToken token = manager.doIssueSecurityToken(
        user,
        (int) TimeUnit.HOURS.toSeconds( desiredLifetimeHours ) )

    AccessKey tokenKey = manager.doLookupAccessKey( token.getAccessKeyId(), token.getToken() )
    assertThat( "Null key", tokenKey, notNullValue() )
    assertThat( "Invalid access key identifier", tokenKey.getAccessKey(), equalTo(token.getAccessKeyId()) )
    assertThat( "Invalid secret key", tokenKey.getSecretKey(), equalTo(token.getSecretKey())  )
    assertThat( "Invalid user", tokenKey.getUser().getUserId(), equalTo(user.getUserId())  )
    assertThat( "Invalid creation time", tokenKey.getCreateDate(), equalTo( new Date(now) ) )
    assertThat( "Invalid creation time", tokenKey.isActive(), equalTo(true) )
  }

  @Test
  void testMinimumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, TimeUnit.MINUTES.toSeconds( 15 ) as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.MINUTES.toMillis( 15 ) ) )
  }

  @Test(expected = SecurityTokenValidationException)
  void testExceedMinimumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, TimeUnit.MINUTES.toSeconds( 15 ) - 1 as Integer )
  }

  @Test
  void testMaximumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, TimeUnit.HOURS.toSeconds( 36 ) as Integer )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 36 ) ) )
  }

  @Test(expected = SecurityTokenValidationException)
  void testExceedMaximumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, TimeUnit.HOURS.toMillis( 36 ) + 1 as Integer  )
  }

  @Test
  void testMaximumTokenDurationForAdminUsers() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.systemUser(), testKey, TimeUnit.HOURS.toSeconds( 1 ) as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) )
  }

  @Test
  void testExceedMaximumTokenDurationForAdminUsers() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.systemUser(), testKey, TimeUnit.HOURS.toSeconds( 1 ) + 1 as Integer  )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) ) // duration should be truncated to 1 hour
  }

  @Test(expected=AuthException.class)
  void testUserMismatch() {
    long now = System.currentTimeMillis()
    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManager manager = manager( now, testKey )
    manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 300 )
  }

  private AccessKey accessKey( long created, User owner ) {
    new AccessKey(){
      @Override Boolean isActive() { true }
      @Override void setActive( final Boolean active) { }
      @Override String getAccessKey() { "VXCDGDDNO5L89OSHF1LHF" }
      @Override String getSecretKey() { "8jbLUrY34CsXQ8oIOMplYhYDhbrXumfrsJ4SB4aX" }
      @Override Date getCreateDate() { new Date( created ) }
      @Override User getUser() { owner }
    }
  }

  private SecurityTokenManager manager( long now, AccessKey keyForLookup, User userForLookup = null ) {
    new SecurityTokenManager() {
      @Override protected String getSecurityTokenPassword() { "password" }
      @Override protected long getCurrentTimeMillis() { now }
      @Override protected AccessKey lookupAccessKeyById( String accessKeyId ) {
        assertThat( "Correct original access key id", accessKeyId, equalTo(keyForLookup.getAccessKey()) )
        keyForLookup
      }
      @Override protected User lookupUserById( String userId ) {
        assertThat( "Correct user id", userId, equalTo(userForLookup?.getUserId()) )
        userForLookup
      }
    }
  }

  // Hey! wouldn't it be great if we had a mocking framework (spock comes with one - http://code.google.com/p/spock/)
  private User user() {
    new User() {
      @Override String getUserId() { "EXAMPLEUUFKQWVU6MBZWL" }
      @Override void setName(String name) { }
      @Override String getPath() { "" }
      @Override void setPath(String path) { }
      @Override Date getCreateDate(){ null }
      @Override User.RegistrationStatus getRegistrationStatus() { User.RegistrationStatus.CONFIRMED }
      @Override void setRegistrationStatus(User.RegistrationStatus stat) { }
      @Override Boolean isEnabled() { true }
      @Override void setEnabled(Boolean enabled) { }
      @Override String getToken() { "examplefnBk4b4TfjAjRMhjmu7eBjNmpvR8llmHPfXKzYw1s3vC8tDMCeEk02OKxuXBZJeDMbaynoo6N" }
      @Override void setToken(String token) { }
      @Override String resetToken() { getToken() }
      @Override String getConfirmationCode() { null }
      @Override void setConfirmationCode(String code) { }
      @Override void createConfirmationCode() { }
      @Override String getPassword() { "" }
      @Override void setPassword(String password) { }
      @Override Long getPasswordExpires() { null }
      @Override void setPasswordExpires(Long time) { }
      @Override String getInfo(String key) { null }
      @Override Map<String, String> getInfo() { [:] }
      @Override void setInfo(String key, String value) { }
      @Override void setInfo(Map<String, String> newInfo) { }
      @Override void removeInfo(String key) { }
      @Override List<AccessKey> getKeys() { [] }
      @Override AccessKey getKey(String keyId) { null }
      @Override void removeKey(String keyId) { }
      @Override AccessKey createKey() { null }
      @Override List<Certificate> getCertificates() { [] }
      @Override Certificate getCertificate(String certificateId) { null }
      @Override Certificate addCertificate(X509Certificate certificate) { null }
      @Override void removeCertificate(String certficateId) { }
      @Override List<Group> getGroups() { [] }
      @Override String getAccountNumber() { null }
      @Override Account getAccount() { null }
      @Override boolean isSystemAdmin() { false }
      @Override boolean isSystemUser() { false }
      @Override boolean isAccountAdmin() { false }
      @Override List<Policy> getPolicies() { [] }
      @Override Policy addPolicy(String name, String policy) { null }
      @Override Policy putPolicy(String name, String policy) { null }
      @Override void removePolicy(String name) { }
      @Override List<Authorization> lookupAuthorizations(String resourceType) { [] }
      @Override List<Authorization> lookupQuotas(String resourceType) { [] }
      @Override String getName() { "test-user-1" }
    }
  }
}
