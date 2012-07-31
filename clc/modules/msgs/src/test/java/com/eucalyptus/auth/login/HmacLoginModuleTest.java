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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.login;

import static org.junit.Assert.*;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.security.auth.Subject;
import org.junit.Test;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.B64;
import com.google.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

/**
 * Unit tests for HMAC login modules
 */
public class HmacLoginModuleTest {

  @Test
  public void testUrlDecode() throws Exception {
    final String signature = B64.standard.encString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});
    final String decoded = HmacLoginModuleSupport.urldecode( URLEncoder.encode( signature, "UTF-8" ) );
    assertEquals( "URL decoded value", signature, decoded );
  }

  @Test
  public void testNormalize() throws Exception {
    testNormalize( "Normalized input", "A+Y=", "A+Y=" );
    testNormalize( "Truncated input", "A+Y", "A+Y=" );
    testNormalize( "Truncated input 2", "BBBBAw", "BBBBAw==" );
    testNormalize( "URL encoded", "A%2bY=", "A+Y=" );
    testNormalize( "Sanitized embedded", "=A=+=Y=", "A+Y=" );
    testNormalize( "Sanitized trailing", "A+Y=====", "A+Y=");
  }

  private void testNormalize( final String desc, final String signature, final String expectedNormalized )  {
    final String normalized = HmacLoginModuleSupport.normalize( signature );
    assertEquals(desc, expectedNormalized, normalized);
  }

  @Test
  public void testAcceptanceHmacV1() throws Exception {
    final HmacCredentials credsV1 = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET",
        "/path",
        "localhost:8773",
        1,
        Hmac.HmacSHA256);
    final Hmacv1LoginModule loginModule = hmacV1LoginModule();
    loginModule.initialize( new Subject(), credsV1, Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap() );
    assertTrue("Module accepts credentials", loginModule.accepts());

    final HmacCredentials credsV2 = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    loginModule.reset();
    loginModule.initialize( new Subject(), credsV2, Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap() );
    assertFalse("Module rejects credentials", loginModule.accepts());
  }

  @Test
  public void testBasicHmacV1() throws Exception {
    final HmacCredentials creds = creds(
        "6Yjg3XTjomQWCuCRNa+96CTI+EdY1Pu56xRAgijk/DM=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1LexicographicalOrdering() throws Exception {
    final HmacCredentials creds = creds(
        "hqd08+ge/qtPDpRIFYqnjMv1+sNbmrs/zTg9DvmZMJE=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "1" );
          put( "R", "1" );
          put( "A", "1" );
        }},
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1NoValue() throws Exception {
    final HmacCredentials creds = creds(
        "Ul8W6d1ppeWI7A51Sdq3LYr/1vPtl5ppb/BMY08vA/Y=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "1" );
          put( "NP", null );
        }},
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1ExtraEquals() throws Exception {
    final HmacCredentials creds = creds(
        "6Yjg3XTjomQWCuCRNa+96CTI+EdY1Pu56xRAgijk/DM=====",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1UrlEncoded() throws Exception {
    final HmacCredentials creds = creds(
        "6Yjg3XTjomQWCuCRNa%2b96CTI%2bEdY1Pu56xRAgijk%2fDM%3d",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1Sha1() throws Exception {
    final HmacCredentials creds = creds(
        "fOwwQ5cRgzn13ZrEhif2OLGetaA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA1);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV1Invalid() throws Exception {
    final HmacCredentials creds = creds(
        "fOwwQ5cRgzn13ZrEhif2OLGeWaA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET", "/path", "localhost:8773",
        1,
        Hmac.HmacSHA1);
    assertFalse("Authentication failed", hmacV1LoginModule().authenticate(creds));
  }

  @Test
  public void testAcceptanceHmacV2() throws Exception {
    final HmacCredentials credsV2 = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    final Hmacv2LoginModule loginModule = hmacV2LoginModule();
    loginModule.initialize( new Subject(), credsV2, Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap() );
    assertTrue("Module accepts credentials", loginModule.accepts());

    final HmacCredentials credsV1 = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1"
        )),
        "GET",
        "/path",
        "localhost:8773",
        1,
        Hmac.HmacSHA256 );
    loginModule.reset();
    loginModule.initialize( new Subject(), credsV1, Collections.<String,String>emptyMap(), Collections.<String,String>emptyMap() );
    assertFalse("Module rejects credentials", loginModule.accepts());
  }

  @Test
  public void testBasicHmacV2() throws Exception {
    final HmacCredentials creds = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV2LexicographicalOrdering() throws Exception {
    final HmacCredentials creds1 = creds(
        "ZB1nE9O2Nun8iCcCQXiKAasZZ/ZSl0yqoQ/+Ux70f2U=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "2" );
          put( "R", "1" );
          put( "A", "1" );
        }},
        "GET", "/path", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds1));
  }

  @Test
  public void testHmacV2LexicographicalByteOrderingWithCase() throws Exception {
    final HmacCredentials creds1 = creds(
        "Yxtxh7wgZgI98Ps9cQa9hQ5A6WlwDMrlz10PjVk6xgY=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "2" );
          put( "a", "1" );
          put( "A", "1" );
        }},
        "GET", "/path", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds1));

    final HmacCredentials creds2 = creds(
        "Yxtxh7wgZgI98Ps9cQa9hQ5A6WlwDMrlz10PjVk6xgY=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "2" );
          put( "A", "1" );
          put( "a", "1" );
        }},
        "GET", "/path", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds2));
  }

  @Test
  public void testHmacV2NoValue() throws Exception {
    final HmacCredentials creds1 = creds(
        "hU6YeOTMqVgax+hLoDQTqjVyi09XaadIUrp9PHNOXug=",
        new LinkedHashMap<String,String>(){{
          put( "AWSAccessKeyId", "1234567890" );
          put( "SignatureVersion", "2" );
          put( "NP", null );
        }},
        "GET", "/path", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds1));
  }

  @Test
  public void testHmacV2ExtraEquals() throws Exception {
    final HmacCredentials creds = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=====",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV2NoPort() throws Exception {
    final HmacCredentials creds = creds(
        "J0TPfG6W7ZFd7Di+0oeXPdIKUjw00dKR+HUB7nZAMV4=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testBasicHmacV2PlusAsEncodedPlusCanonicalization() throws Exception {
    final HmacCredentials creds = creds(
        "HcHSowrJLwxbym2DFAhVhkXR8mFZ2UcDtKg9hh1Qbyc=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path+plus",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testBasicHmacV2PlusAsEncodedSpaceCanonicalization() throws Exception {
    final HmacCredentials creds = creds(
        "FOoBqskkKoiCj0duRQmjetxJCUgrAx8vwW neYG2WJs=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path+plus",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testBasicHmacV2StarEncodedCanonicalization() throws Exception {
    final HmacCredentials creds = creds(
        "03E5iuj6n3jg9h6lYiR5IPtsvwwZX0dhsQeGy kZeZY=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890*",
            "SignatureVersion", "2",
            "Star", "*"
        )),
        "GET",
        "/path*star",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV2UrlEncoded() throws Exception {
    final HmacCredentials creds = creds(
        "MrFSyGZ44%2fOe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA%3d",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV2WithMethod() throws Exception {
    final HmacCredentials creds = creds(
        "6FoWdyXxxjCgqKYVsjpvbY6ol6ddfzUbJgwi7SRuyIA=",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "DELETE",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  @Test
  public void testHmacV2Invalid() throws Exception {
    final HmacCredentials creds = creds(
        "MrFSyGZ44%2fOe4nOfXQImKmR8oRABMrmNk2mJIWz1dCA%3d",
        Maps.newHashMap(ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        )),
        "GET",
        "/path",
        "localhost:8773",
        2,
        Hmac.HmacSHA256 );
    assertFalse("Authentication failed", hmacV2LoginModule().authenticate(creds));
  }

  /**
   * Elasticfox version 1.7 build 000116 (EC2 API 2010-06-15)
   */
  @Test
  public void testElasticfox_1_7_000116() throws Exception {
    final HmacCredentials creds = creds(
        "u4jJ7NM5Vt2oMCuHea2BQeAfNDI=",
        Maps.newHashMap(ImmutableMap.of(
            "Action", "DescribeAvailabilityZones",
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1",
            "Timestamp", "2012-05-09T22:35:14Z",
            "Version", "2010-06-15"
            )),
        "POST", "/services/Eucalyptus/", "localhost:8773",
        1,
        Hmac.HmacSHA1);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  /**
   * Hybridfox version 1.7 build 000177 (EC2 API 2011-12-01)
   */
  @Test
  public void testHybridfox_1_7_000177_Sigv1() throws Exception {
    final HmacCredentials creds = creds(
        "eCijXehApkoj7VwTDSMf7V6pCM8=",
        Maps.newHashMap(ImmutableMap.of(
            "Action", "DescribeVpcs",
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "1",
            "Timestamp", "2012-05-09T23:01:39Z",
            "Version", "2011-12-01"
        )),
        "POST", "/services/Eucalyptus/", "localhost:8773",
        1,
        Hmac.HmacSHA1);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }

  /**
   * Hybridfox version 1.7 build 000177 (EC2 API 2011-12-01)
   */
  @Test
  public void testHybridfox_1_7_000177_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "L9mcIT2K/SOnOg09t01DWCKtadNOoIEGo/PkpW/DL08=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Action", "DescribeRegions" )
            .put( "AWSAccessKeyId", "1234567890" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "SignatureVersion", "2" )
            .put( "Timestamp", "2012-05-09T23:10:15" )
            .put( "Version", "2011-12-01" )
            .build()),
        "POST", "/service/Eucalyptus/", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  /**
   * Euca2ools / Boto
   * euca2ools-1.3.1-12.fc16.noarch
   * Version: 1.2 (BSD)
   * User-Agent: Boto/2.0 (linux2)
   */
  @Test
  public void testEuca2oolsBoto_1_3_1() throws Exception {
    final HmacCredentials creds = creds(
        "JjRHXf60U4eatEOpKhf0enPNimNzVKLy99f0+/lUfzc=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "AWSAccessKeyId", "1234567890" )
            .put( "Action", "DescribeInstances" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "SignatureVersion", "2" )
            .put( "Timestamp", "2012-05-10T18:00:00Z" )
            .put( "Version", "2011-01-01" )
            .build()),
        "POST", "/services/Eucalyptus/", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  /**
   * Typica 1.7.2
   *
   * Note: Typica does not include the port in the host header - "Host: localhost"
   */
  @Test
  public void testTypica_1_7_2_Sigv1() throws Exception {
    final HmacCredentials creds = creds(
        "bb3F2grNDpz1cwkOT4xtc2Hk5eM=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Action", "DescribeImages" )
            .put( "AWSAccessKeyId", "1234567890" )
            .put( "SignatureVersion", "1" )
            .put( "Timestamp", "2012-05-10T19:08:00Z" )
            .put( "Version", "2010-06-15" )
            .build()),
        "GET", "/services/Eucalyptus", "localhost:8773",
        1,
        Hmac.HmacSHA1);
    assertTrue("Authentication successful", hmacV1LoginModule().authenticate(creds));
  }


  /**
   * Typica 1.7.2
   *
   * Note: Typica does not include the port in the host header - "Host: localhost"
   */
  @Test
  public void testTypica_1_7_2_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "PotQrmC/3ZTCGaRIZnlg0cX9VFUgiB+aguCJorntDhI=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "AWSAccessKeyId", "1234567890" )
            .put( "Action", "DescribeImages" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "SignatureVersion", "2" )
            .put( "Timestamp", "2012-05-10T18:53:00Z" )
            .put( "Version", "2010-06-15" )
            .build()),
        "GET", "/services/Eucalyptus", "localhost:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule().authenticate(creds));
  }

  private HmacCredentials creds( final String signature,
                                 final Map<String,String> parameters,
                                 final String verb,
                                 final String servicePath,
                                 final String headerHost,
                                 final Integer signatureVersion,
                                 final Hmac hmacType ) {
    return new HmacCredentials( "1234567890", signature, parameters, verb, servicePath, headerHost, signatureVersion, hmacType );
  }

  //TODO:MOCKING
  private Hmacv1LoginModule hmacV1LoginModule() {
    return new Hmacv1LoginModule(){
      @Override
      protected AccessKey lookupAccessKey(final HmacCredentials credentials) throws AuthException {
        return accessKey();
      }

      @Override
      protected void checkForReplay( final String signature ) throws AuthenticationException {
      }
    };
  }

  private Hmacv2LoginModule hmacV2LoginModule() {
    return new Hmacv2LoginModule(){
      @Override
      protected AccessKey lookupAccessKey(final HmacCredentials credentials) throws AuthException {
        return accessKey();
      }

      @Override
      protected void checkForReplay( final String signature ) throws AuthenticationException {
      }
    };
  }

  private AccessKey accessKey() {
    return new AccessKey() {
      @Override
      public Boolean isActive() {
        return true;
      }

      @Override
      public void setActive(final Boolean active) throws AuthException {
        throw new IllegalStateException();
      }

      @Override
      public String getAccessKey() {
        throw new IllegalStateException();
      }

      @Override
      public String getSecretKey() {
        return "ZRvYnXG04PxhYuP228IWLmCG0o3kYIr2fPByxMlb";
      }

      @Override
      public Date getCreateDate() {
        throw new IllegalStateException();
      }

      @Override
      public void setCreateDate(final Date createDate) throws AuthException {
        throw new IllegalStateException();
      }

      @Override
      public User getUser() throws AuthException {
        return null;
      }
    };
  }
}
