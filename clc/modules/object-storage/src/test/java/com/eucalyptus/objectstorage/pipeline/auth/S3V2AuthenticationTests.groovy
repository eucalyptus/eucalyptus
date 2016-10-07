/*
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
 */

package com.eucalyptus.objectstorage.pipeline.auth

import com.eucalyptus.http.MappingHttpRequest
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

import static org.junit.Assert.fail

/**
 * Tests {@link S3V2Authentication}.
 *
 * Note: These tests are not run by default since they do not match the test class naming convention.
 *
 * @author zhill on 2/5/14
 */
class S3V2AuthenticationTests {
  @Test
  public void testInvalidDnsParsing() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/key")
    request.setHeader(HttpHeaders.Names.HOST, ".objectstorage.mydomain.com")
    request.setMethod(HttpMethod.GET)

    try {
      S3V2Authentication.buildCanonicalResource(request, true)
      fail("Should have thrown exception trying to parse invalid request")
    } catch (S3Exception e) {
      //expected
    }
  }

  @Test
  public void testServiceUrlParsing() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")
    request.setMethod(HttpMethod.GET)

    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/"))

    request.setUri("/services/objectstorage")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage"))

    request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")
    request.setUri("/services/objectstorage")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage"))
  }

  @Test
  public void testUrlEncodingParsing() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object?uploadId=a%20b")
    request.setHeader(HttpHeaders.Names.HOST, "domain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage/bucket/object?uploadId=a%20b"))
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?uploadId=a%20b"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/object?uploadId=a%20b")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.domain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object?uploadId=a%20b"))
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?uploadId=a%20b"))
  }

  @Test
  public void testPathStyleAddressParsing() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object")
    request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage/bucket/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object"))

    request.setUri("/bucket/")
    request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/"))

    request.setUri("/bucket/object")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy")
    request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy")
    request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))


    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy&acl")
    request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?acl&" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objectstorage/bucket/object?acl&" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object?" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy&acl")
    request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object?acl&" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object?acl&" + ObjectStorageProperties.SubResource.versionId.toString() + "=xy"))

    for (ObjectStorageProperties.SubResource resource : ObjectStorageProperties.SubResource.values()) {
      request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/key?" + resource.toString())
      request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")

      assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/key?" + resource.toString()))
      assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/objecttorage/bucket/key?" + resource.toString()))
    }

    for (ObjectStorageProperties.SubResource resource : ObjectStorageProperties.SubResource.values()) {
      request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/key?" + resource.toString())
      request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")

      assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/key?" + resource.toString()))
      assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/key?" + resource.toString()))
    }
  }

  @Test
  public void testPathStyleAddressParsingLegacyWalrus() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/Walrus/bucket/object")
    request.setHeader(HttpHeaders.Names.HOST, "mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/services/Walrus/bucket/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object"))

    request.setUri("/bucket/")
    request.setHeader(HttpHeaders.Names.HOST, "walrus.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/"))

    request.setUri("/bucket/object")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object"))
  }

  @Test
  public void testDnsStyleAddressParsing() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/object")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com")

    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/services/objectstorage/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/services/objectstorage/object"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/object%20for%20me+letsgo")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object%20for%20me+letsgo"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object%20for%20me+letsgo"))

    for (ObjectStorageProperties.SubResource resource : ObjectStorageProperties.SubResource.values()) {
      request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/key?" + resource.toString())
      request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com")

      assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/key?" + resource.toString()))
      assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/key?" + resource.toString()))
    }

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/object")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com")

    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/services/objectstorage/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/services/objectstorage/object"))
  }

  @Test
  public void testDnsStyleAddressParsingLegacyWalrus() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/object")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.walrus.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/services/objectstorage/object"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/services/objectstorage/object"))

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/object%20for%20me+letsgo")
    request.setHeader(HttpHeaders.Names.HOST, "bucket.walrus.mydomain.com")
    assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/object%20for%20me+letsgo"))
    assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/object%20for%20me+letsgo"))

    for (ObjectStorageProperties.SubResource resource : ObjectStorageProperties.SubResource.values()) {
      request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/key?" + resource.toString())
      request.setHeader(HttpHeaders.Names.HOST, "bucket.walrus.mydomain.com")

      assert (S3V2Authentication.buildCanonicalResource(request, true).equals("/bucket/key?" + resource.toString()))
      assert (S3V2Authentication.buildCanonicalResource(request, false).equals("/bucket/key?" + resource.toString()))
    }
  }

  @Test
  public void testQueryParameterHandling() {
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/?AWSAccessKeyId=123&Signature=xxxyyyyzzz&expires=date123&acl&x-amz-security-token=tokenvalue123")
    request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com:8773")
    assert (request.getParameters().get("acl") == null && request.getParameters().containsKey("acl"))
    assert (request.getParameters().get("AWSAccessKeyId") == "123")
    assert (request.getParameters().get("Signature") == "xxxyyyyzzz")
    assert (request.getParameters().get("expires") == "date123")
    assert (request.getParameters().get("x-amz-security-token") == "tokenvalue123")

    assert (S3V2Authentication.buildCanonicalResource(request, false) == "/services/objectstorage/bucket/?acl")
    assert (S3V2Authentication.buildCanonicalResource(request, true) == "/bucket/?acl")
  }

  @Test
  public void testV2SigningCanonicalization() {
    //TODO: zhill - test string-to-sign for regular REST v2 auth

  }
}
