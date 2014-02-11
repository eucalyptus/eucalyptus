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

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.http.MappingHttpRequest
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAddressingHeaderException
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.junit.Test

/**
 * Created by zhill on 2/5/14.
 */
class ObjectStorageAuthenticationHandlerTests {

    @Test
    public void testServicePathAddressParsing() {
        MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage")
        request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com")
        request.setUri("/services/objectstorage/bucket/object")
        request.setMethod(HttpMethod.GET)

        String addrString = ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request)
        assert(addrString.equals("/services/objectstorage/bucket/object"))

        request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com")
        request.setUri("/object")
        addrString = ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request)
        assert(addrString.equals("/bucket/object"))

        request.setHeader(HttpHeaders.Names.HOST, "bucket.objectstorage.mydomain.com:8773")
        request.setUri("/object")
        addrString = ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request)
        assert(addrString.equals("/bucket/object"))

        request.setHeader(HttpHeaders.Names.HOST, "objecstorage.mydomain.com")
        request.setUri("/")
        addrString = ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request)
        assert(addrString.equals("/"))

        request.setHeader(HttpHeaders.Names.HOST, ".objectstorage.mydomain.com")
        request.setUri("/bucket/object")
        try {
            addrString = ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request)
            fail("Should have thrown error for invalid host header")
        } catch(InvalidAddressingHeaderException e) {
        }
    }

    @Test
    public void testQueryParameterHandling() {
        MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/?AccessKeyId=123&Signature=xxxyyyyzzz&expires=date123&acl")
        request.setHeader(HttpHeaders.Names.HOST, "objectstorage.mydomain.com:8773")
        assert(request.getParameters().get("acl") == null && request.getParameters().containsKey("acl"))
        assert(request.getParameters().get("AccessKeyId") == "123")
        assert(request.getParameters().get("Signature") == "xxxyyyyzzz")
        assert(request.getParameters().get("expires") == "date123")

        assert(ObjectStorageAuthenticationHandler.S3Authentication.getS3AddressString(request) == "/services/objectstorage/bucket/?acl")
    }
}
