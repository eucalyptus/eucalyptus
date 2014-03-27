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



package com.eucalyptus.walrus.pipeline

import groovy.transform.CompileStatic
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

/**
 * Created by zhill on 3/17/2014
 */
@CompileStatic
class WalrusRESTPipelineTest extends GroovyTestCase {
    static final WalrusRESTPipeline pipeline = new WalrusRESTPipeline() {
        @Override
        String getName() {
            return "test pipeline"
        }

        //For test don't do a real pipeline.
        @Override
        ChannelPipeline addHandlers(ChannelPipeline pipeline) {
            return null
        }
    }

    @Test
    void testCheckAccepts() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend/')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackends')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackendBucket/Object/')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackendBuckets/')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackends/')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend/')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend/bucket/object')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend/bucket/object')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, '/services/WalrusBackend.bucket/object')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, '/services/WalrusBackend/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, '/services/WalrusBackend/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/WalrusBackend')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/WalrusBackend')
        request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data")
        assert(!pipeline.checkAccepts(request))

    }
}
