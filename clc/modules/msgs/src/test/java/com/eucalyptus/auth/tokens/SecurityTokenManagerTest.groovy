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
package com.eucalyptus.auth.tokens

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString
import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.User
import java.util.concurrent.TimeUnit

import org.junit.BeforeClass
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.eucalyptus.auth.AuthException

/**
 * Unit tests for security token manager
 */
class SecurityTokenManagerTest {

  @BeforeClass
  static void beforeClass() {
    if ( Security.getProvider( BouncyCastleProvider.PROVIDER_NAME ) == null ) {
      Security.addProvider( new BouncyCastleProvider( ) )
    }
  }

  @Test
  void testIssueToken() {
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
  void testLookupToken() {
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

  @Test
  void testMinimumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, 1 )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) )
  }

  @Test
  void testMaximumTokenDuration() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.nobodyUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.nobodyUser(), testKey, Integer.MAX_VALUE )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 36 ) ) )
  }

  @Test
  void testMaximumTokenDurationForAdminUsers() {
    long now = System.currentTimeMillis()

    AccessKey testKey = accessKey( now - TimeUnit.HOURS.toMillis( 24 ), Principals.systemUser() )
    SecurityTokenManager manager = manager( now, testKey )
    SecurityToken token = manager.doIssueSecurityToken( Principals.systemUser(), testKey, Integer.MAX_VALUE )

    assertThat( "Null token issued", token, notNullValue() )
    assertThat( "Token expiry", token.getExpires(), equalTo( now + TimeUnit.HOURS.toMillis( 1 ) ) )
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
      @Override void setCreateDate(final Date createDate) { }
      @Override User getUser() { owner }
    }
  }

  private SecurityTokenManager manager( long now, AccessKey keyForLookup ) {
    new SecurityTokenManager() {
      @Override protected String getSecurityTokenPassword() { "password" }
      @Override protected long getCurrentTimeMillis() { now }
      @Override protected AccessKey lookupAccessKeyById( String accessKeyId ) {
        assertThat( "Correct original access key id", accessKeyId, equalTo(keyForLookup.getAccessKey()) )
        keyForLookup
      }
    }
  }
}
