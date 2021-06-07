/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.login;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.security.auth.Subject;
import org.junit.Test;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidSignatureAuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

/**
 * Unit tests for HMAC login modules
 */
public class HmacLoginModuleTest {

  @Test
  public void testUrlDecode() throws Exception {
    final String signature = B64.standard.encString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});
    final String decoded = loginModule().urldecode( URLEncoder.encode( signature, "UTF-8" ) );
    assertEquals( "URL decoded value", signature, decoded );
  }

  @Test
  public void testNormalize() throws Exception {
    testNormalize( "Normalized input", "A+Y=", "A+Y=" );
    testNormalize( "Truncated input", "A+Y", "A+Y=" );
    testNormalize( "Truncated input 2", "BBBBAw", "BBBBAw==" );
    testNormalize( "URL encoded", "A%2bY=", "A+Y=" );
    testNormalize( "Sanitized embedded", "=A=+=Y=", "A+Y=" );
    testNormalize( "Sanitized trailing", "A+Y=====", "A+Y=" );
  }

  private void testNormalize( final String desc, final String signature, final String expectedNormalized )  {
    final String normalized = loginModule().normalize( signature );
    assertEquals( desc, expectedNormalized, normalized );
  }

  @Test
  public void testAcceptanceHmacV2() throws Exception {
    final HmacCredentials credsV2 = creds(
        "MrFSyGZ44/Oe4nOfXQImKmq8oRABMrmNk2mJIWz1dCA=",
        Maps.newHashMap( ImmutableMap.of(
            "AWSAccessKeyId", "1234567890",
            "SignatureVersion", "2"
        ) ),
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
    assertFalse( "Module rejects credentials", loginModule.accepts() );
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
    assertTrue( "Authentication successful", hmacV2LoginModule().authenticate( creds ) );
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
  public void testBasicHmacV2HostCanonicalization() throws Exception {
    final HmacCredentials creds = creds(
        "1Y44avkQ78kH7ryAElD9ZpJmzbytRinIclJJZUNgvwE=",
        Maps.newHashMap(ImmutableMap.<String,String>builder( )
            .put( "AWSAccessKeyId", "AKIAAFOSB6X4B4FC52B3" )
            .put( "Action", "DescribeLoadBalancerPolicyTypes" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "SignatureVersion", "2" )
            .put( "Timestamp", "2015-05-20T05:17:48Z" )
            .put( "Version", "2012-06-01" )
            .build( ) ),
        "POST",
        "/services/LoadBalancing",
        "LOCALHOST:8773",
        2,
        Hmac.HmacSHA256 );
    assertTrue("Authentication successful", hmacV2LoginModule( "xBPMQG8zlU6zWHO3kvXnosnVxKdGROK2BDHOOMZw" ).authenticate(creds));
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

  @Test( expected = InvalidSignatureAuthException.class )
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
    hmacV2LoginModule().authenticate( creds );
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

  /**
   * AWS Java SDK 1.3.27
   */
  @Test
  public void testAWSJavaSDK_1_3_27_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "Xn7WHmdCX7+QqJDWD/7Fau5lIgQoV8tq5gcLq2TTyk0=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Action", "TerminateInstances" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "InstanceId.1", "i-C7D241F0" )
            .put( "AWSAccessKeyId", "M2JFNG1R6BH2WXWC4GEAE" )
            .put( "SignatureVersion", "2" )
            .put( "Version", "2012-12-01" )
            .put( "Timestamp", "2013-03-04T20:44:58.852Z" )
            .build()),
        "POST", "/services/Eucalyptus", "10.111.1.65:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule("LuB6vcOGvBFkUYTGMd7CtwTJlgIN1BprPo9yfSoe").authenticate(creds));
  }

  /**
   * Amazon CLI/AutoScaling 1.0.61.2 API 2011-01-01
   *
   * NOTE: Signature is invalid due to signed path being "/services/autoscaling"
   */
  @Test
  public void testAWSAutoScalingCLI_1_0_61_2_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "ILCkibsyRv/4nsUToJnKMZAjSrYTx++UFfuxXERyCkg=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "MaxRecords", "20" )
            .put( "Version", "2011-01-01" )
            .put( "Action", "DescribeAutoScalingGroups" )
            .put( "SignatureVersion", "2" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "Timestamp", "2013-04-04T01:47:05.199Z" )
            .put( "AWSAccessKeyId", "7C6LEIR2VTIXHPTXTCEAD" )
            .build()),
        "GET", "/services/AutoScaling/", "10.111.1.116:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule("7pG138tHUyt5YjOd4gRLZSOX6Nn77skFg29r1SQk").authenticate(creds));
  }

  /**
   * Amazon CLI/CloudWatch 1.0.13.4 API 2010-08-01
   *
   * NOTE: Signature is invalid due to signed path being "/services/cloudwatch"
   */
  @Test
  public void testAWSCloudWatchCLI_1_0_13_4_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "JEDUx0NOenXpnidyKzH/Ut+HCzqcFF3l0icDeXJ8lOw=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Version", "2010-08-01" )
            .put( "Action", "DeleteAlarms" )
            .put( "SignatureVersion", "2" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "Timestamp", "2013-04-04T01:50:57.725Z" )
            .put( "AWSAccessKeyId", "7C6LEIR2VTIXHPTXTCEAD" )
            .build()),
        "GET", "/services/CloudWatch/", "10.111.1.116:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule("7pG138tHUyt5YjOd4gRLZSOX6Nn77skFg29r1SQk").authenticate(creds));
  }

  /**
   * Amazon CLI/ElasticLoadBalancing 1.0.17.0 API 2012-06-01
   *
   * NOTE: Signature is invalid due to signed path being "/services/loadbalancing"
   */
  @Test
  public void testAWSElasticLoadBalancingCLI_1_0_17_0_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "NAbXQmUTwLxyT1cqXbOi6iMX7TQt5haL9KgrhBO+zaA=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Version", "2012-06-01" )
            .put( "Action", "DescribeLoadBalancers" )
            .put( "SignatureVersion", "2" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "Timestamp", "2013-04-04T01:54:27.366Z" )
            .put( "AWSAccessKeyId", "7C6LEIR2VTIXHPTXTCEAD" )
            .build()),
        "GET", "/services/LoadBalancing/", "10.111.1.116:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule("7pG138tHUyt5YjOd4gRLZSOX6Nn77skFg29r1SQk").authenticate(creds));
  }

  @Test
  public void testSignatureV4TestSuite() throws Exception {
    final File tempZipFile = File.createTempFile( "aws-sig-v4-test-suite", ".zip" );
    tempZipFile.deleteOnExit();
    ByteStreams.copy( Resources.asByteSource( HmacLoginModuleTest.class.getResource( "aws-sig-v4-test-suite.zip" ) ).openStream( ), new FileOutputStream( tempZipFile ) );
    final ZipFile testSuiteZip = new ZipFile( tempZipFile );

    final Set<String> testNames = Sets.newTreeSet();
    final Splitter nameSplit = Splitter.on( Pattern.compile("aws-sig-v4-test-suite/aws-sig-v4-test-suite/|\\.[a-z]{3,5}$") );
    for ( final ZipEntry entry : Collections.list( testSuiteZip.entries() ) ) {
      String test = Iterables.get( nameSplit.split( entry.getName() ), 1, "" );
      if ( !test.isEmpty() && !test.endsWith("/") ) {
        testNames.add( test );
      }
    }

    System.out.println( "Found tests: " + testNames );
    assertTrue( "No tests found!", !testNames.isEmpty() );
    
    testNames.removeAll( Lists.newArrayList(
        "get-header-value-multiline/get-header-value-multiline", // fails due to multiline headers not supported
        "get-header-value-trim/get-header-value-trim",           // fails due to spaces in header values not combined
        "get-utf8/get-utf8",                                     // fails due to unencoded utf-8 path characters
        "post-x-www-form-urlencoded-parameters/post-x-www-form-urlencoded-parameters" // incorrect test?
    ) );
    
    for ( final String testName : testNames ) {
      System.out.println( "Test: " + testName );
      final String authz = slurpTestFile( testSuiteZip, testName, ".authz" );
      final String sreq = slurpTestFile( testSuiteZip, testName, ".sreq" );
      if ( authz == null || sreq == null ) {
        System.out.println("Skipping test with missing files: [" + testName + "]");
        continue;
      }

      final Iterable<String> spaceTokenized = Splitter.on(" ").limit( 32 ).split( sreq );
      final String verb = Iterables.get(spaceTokenized, 0);
      final List<String> path = Lists.newArrayList();
      for(final String pathToken : Iterables.skip(spaceTokenized, 1)) {
        if (pathToken.startsWith("HTTP/")) {
          break;
        }
        path.add(pathToken);
      }
      final String pathWithQuery = Joiner.on(" ").join(path);
      final HmacCredentials creds = new HmacCredentials(
          "1234567890",
          HmacUtils.SignatureVariant.SignatureV4Standard,
          Maps.transformValues(
              Multimaps.index(
                  Splitter.on("&").omitEmptyStrings().split(Iterables.get(Splitter.on("?").limit(2).split(pathWithQuery), 1, "")),
                  nvp -> Iterables.get(Splitter.on("=").limit(2).split(nvp), 0)
              ).asMap(),
              values -> Lists.transform(Lists.newArrayList(values), value -> Iterables.get(Splitter.on("=").limit(2).split(value), 1, ""))),
          Maps.transformValues(
              Multimaps.index(
                  Iterables.filter(Splitter.on("\n").split(sreq), Predicates.containsPattern(":")),
                  line -> Iterables.get(Splitter.on(":").limit(2).split(line), 0).toLowerCase()
              ).asMap(),
              values -> Lists.transform(Lists.newArrayList(values), value -> Iterables.get(Splitter.on(":").limit(2).split(value), 1))),
          verb,
          Iterables.get(Splitter.on("?").limit(2).split(pathWithQuery), 0),
          body( Iterables.get(Splitter.on("\n\n").limit(2).split(sreq), 1, "" ))
      );
      assertTrue("Authentication successful " + testName, hmacV4LoginModule().authenticate(creds));
    }
    
    assertTrue( "Deleted temp zip file", tempZipFile.delete() );
    testSuiteZip.close();
  }

  /**
   * AWS Java SDK version 1.3.26, signs with path of "/"
   */
  @Test
  public void testAWSJavaSDK_1_3_26_SigV4() throws Exception {
    final HmacCredentials creds = new HmacCredentials(
        "1234567890",
        HmacUtils.SignatureVariant.SignatureV4Standard,
        Collections.<String,List<String>>emptyMap(),
        ImmutableMap.<String,List<String>>builder()
            .put( "host", Lists.newArrayList( "b-28.devtest.eucalyptus-systems.com:8773" ) )
            .put( "x-amz-date", Lists.newArrayList( "20130112T025140Z" ) )
            .put( "authorization", Lists.newArrayList( "AWS4-HMAC-SHA256 Credential=K4DM6CICEOS4Y6IORG7I5/20130112/us-east-1/iam/aws4_request, SignedHeaders=host;user-agent;x-amz-content-sha256;x-amz-date, Signature=9dd90e072ce991059ed4fefdeff3e37317abb9f4816be301573444040ff01900" ) )
            .put( "user-agent", Lists.newArrayList( "aws-sdk-java/1.3.26 Linux/3.6.10-2.fc16.x86_64 Java_HotSpot(TM)_64-Bit_Server_VM/20.6-b01" ) )
            .put( "x-amz-content-sha256", Lists.newArrayList( "5f776d91509b9c99b8cb5eb5d6d4a787a33ae41c8cd6e7b69effca69080e1e1f" ) )
            .put( "content-type", Lists.newArrayList( "application/x-www-form-urlencoded; charset=utf-8" ) )
            .put( "content-length", Lists.newArrayList( "36" ) )
            .build(),
        "POST",
        "/services/Euare/",
        body( "Action=ListGroups&Version=2010-05-08" )
    );
    assertTrue("Authentication successful", hmacV4LoginModule("ea9nMgw6353ANsJeylVkNIIzuCU0hz0xtErRbcj0").authenticate(creds));
  }

  @Test
  public void testBoto_2_27_0_SigV4() throws Exception {
    final HmacCredentials creds = new HmacCredentials(
        "1234567890",
        HmacUtils.SignatureVariant.SignatureV4Standard,
        ImmutableMap.<String,List<String>>builder()
            .put( "Action", Lists.newArrayList( "ListMetrics" ) )
            .put( "Version", Lists.newArrayList( "2010-08-01" ) )
            .build(),
        ImmutableMap.<String,List<String>>builder()
            .put( "host", Lists.newArrayList( "cloudwatch.g-12-03.autoqa.qa1.eucalyptus-systems.com:8773" ) )
            .put( "x-amz-date", Lists.newArrayList( "20140507T202657Z" ) )
            .put( "authorization", Lists.newArrayList( "AWS4-HMAC-SHA256 Credential=AKI67CCVBS1XAL7UG9KE/20140507/g-12-03/cloudwatch/aws4_request, SignedHeaders=host;x-amz-date, Signature=1a74acf1bf2b22bddad7314cc44eef6fe562a50373ca239113d2c1942a677739" ) )
            .build(),
        "GET",
        "/",
        body( "" )
    );
    assertTrue("Authentication successful", hmacV4LoginModule("vNhDy9ERZQP5WXCdmPR7ZbbzZwdlQXETeZ6wM64i").authenticate(creds));
  }

  /**
   * EUCA-4748 sig v2
   */
  @Test
  public void testSpecialCharacters_Sigv2() throws Exception {
    final HmacCredentials creds = creds(
        "J8+AGPWqWNL8n7n/rjvyb+vQlKtiF+Bl4e/1rO/bUHU=",
        Maps.newHashMap(ImmutableMap.<String,String>builder()
            .put( "Version", "2013-02-01" )
            .put( "Action", "CreateSecurityGroup" )
            .put( "GroupDescription", "!@$$%^^&&*()*&&^%{}\":?><|][~" )
            .put( "GroupName", "group1" )
            .put( "SignatureVersion", "2" )
            .put( "SignatureMethod", "HmacSHA256" )
            .put( "Timestamp", "2013-06-17T21:55:32Z" )
            .put( "AWSAccessKeyId", "YHZWRDQW0LZMFRPWWHS6F" )
            .build()),
        "POST", "/services/Eucalyptus", "10.20.10.61:8773",
        2,
        Hmac.HmacSHA256);
    assertTrue("Authentication successful", hmacV2LoginModule("K8VKG93wNXV9i3P6hgSUPMiK40CAfBMmntBCb4bs").authenticate(creds));
  }

  private TestHmacLoginModule loginModule() {
    return new TestHmacLoginModule();
  }
  
  private static class TestHmacLoginModule extends HmacLoginModuleSupport {
    private TestHmacLoginModule() {
      super(-1);
    }
    
    @Override
    public boolean authenticate( final HmacCredentials credentials ) throws Exception {
      throw new Exception("Not implemented");
    }
  }
  
  private String slurpTestFile( final ZipFile testSuiteZip, final String testPath, final String testFileExt ) throws IOException {
    final ZipEntry entry = testSuiteZip.getEntry( "aws-sig-v4-test-suite/aws-sig-v4-test-suite/" + testPath + testFileExt );
    final byte[] data =  entry == null ? null : ByteStreams.toByteArray( testSuiteZip.getInputStream( entry ) );
    return data == null ? null : new String(data, Charsets.UTF_8);
  }

  private HmacCredentials creds( final String signature,
                                 final Map<String,String> parameters,
                                 final String verb,
                                 final String servicePath,
                                 final String headerHost,
                                 final Integer signatureVersion,
                                 final Hmac hmacType ) throws AuthenticationException {
    final Map<String,List<String>> paramMap = Maps.newHashMap( Maps.transformValues(parameters, CollectionUtils.<String>listUnit()) );
    paramMap.put( "Signature", Collections.singletonList( signature ) );

    final HmacCredentials creds = new HmacCredentials(
        "1234567890", 
        signatureVersion == 1 ? HmacUtils.SignatureVariant.SignatureV1Standard : HmacUtils.SignatureVariant.SignatureV2Standard,
        paramMap, 
        Collections.singletonMap( "host", Collections.singletonList(headerHost) ), 
        verb, 
        servicePath, 
        body( "" ) );

    if ( signatureVersion == 1 || ( signatureVersion == 2 && !paramMap.containsKey( SecurityParameter.SignatureMethod.parameter() ) ) ) {
      creds.setSignatureMethod( hmacType );
    }

    return creds;
  }

  private Supplier<ByteBuffer> body( final String text ) {
    return () -> StandardCharsets.UTF_8.encode( CharBuffer.wrap( text ) );
  }

  //TODO:MOCKING
  private Hmacv2LoginModule hmacV2LoginModule() {
    return hmacV2LoginModule( "ZRvYnXG04PxhYuP228IWLmCG0o3kYIr2fPByxMlb" );
  }

  private Hmacv2LoginModule hmacV2LoginModule( final String secret ) {
    return new Hmacv2LoginModule(){
      @Override
      protected AccessKey lookupAccessKey(final HmacCredentials credentials) throws AuthException {
        return accessKey( secret );
      }
    };
  }

  private Hmacv4LoginModule hmacV4LoginModule(  ) {
    return hmacV4LoginModule( "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"  );
  }

  private Hmacv4LoginModule hmacV4LoginModule( final String secret ) {
    return new Hmacv4LoginModule(){
      @Override
      protected AccessKey lookupAccessKey( final HmacCredentials credentials ) throws AuthException {
        return accessKey( secret );
      }
    };
  }

  private AccessKey accessKey() {
    return accessKey( "ZRvYnXG04PxhYuP228IWLmCG0o3kYIr2fPByxMlb" );
  }

  private AccessKey accessKey( final String secretKey ) {
    return new AccessKey() {
      private static final long serialVersionUID = 1L;

      @Override
      public Boolean isActive() {
        return true;
      }

      @Override
      public String getAccessKey() {
        throw new IllegalStateException();
      }

      @Override
      public String getSecretKey() {
        return secretKey;
      }

      @Override
      public Date getCreateDate() {
        throw new IllegalStateException();
      }

      @Override
      public UserPrincipal getPrincipal() throws AuthException {
        return null;
      }
    };
  }
}
