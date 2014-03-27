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

package com.eucalyptus.objectstorage.pipeline

import groovy.transform.CompileStatic
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

/**
 * Created by zhill on 3/7/14.
 */
@CompileStatic
class ObjectStorageRESTPipelineTest extends GroovyTestCase {
    static final ObjectStorageRESTPipeline pipeline = new ObjectStorageRESTPipeline(){

        @Override
        String getName() {
            return "test pipeline"
        }

        @Override
        ChannelPipeline addHandlers(ChannelPipeline pipeline) {
            return null
        }
    }

    @Test
    public void testCheckAccepts() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBackend')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/WalrusBucket/Object/')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage/')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus/bucket/object')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/Walrus')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage/bucket/object')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, '/services/Walrus.bucket/object')
        assert(!pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, '/services/objectstorage/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, '/services/objectstorage/bucket')
        assert(pipeline.checkAccepts(request))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/objectstorage')
        assert(pipeline.checkAccepts(request))

        //This is expected to be accepted since the class being tested is a base class. All HTTP verbs are accepted for this OSG class.
        //Specific extensions filter further on HTTP verb.
        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/objectstorage')
        request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data")
        assert(pipeline.checkAccepts(request))

    }
}
