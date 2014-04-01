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

import com.eucalyptus.ws.server.FilteredPipeline
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

/**
 * Tests for acceptance of various request types for each pipeline to ensure proper routing
 */
@CompileStatic
class ObjectStorageRESTPipelineAcceptanceTest extends GroovyTestCase {
    static final ObjectStorageDELETEPipeline deletePipeline = new ObjectStorageDELETEPipeline();
    static final ObjectStorageGETPipeline getPipeline = new ObjectStorageGETPipeline();
    static final ObjectStoragePUTPipeline putPipeline = new ObjectStoragePUTPipeline();
    static final ObjectStorageFormPOSTPipeline formPostPipeline = new ObjectStorageFormPOSTPipeline();
    static final ObjectStorageHEADPipeline headPipeline = new ObjectStorageHEADPipeline();
    static final ObjectStorageOPTIONSPipeline optionsPipeline = new ObjectStorageOPTIONSPipeline();

    static final pipelines = [ deletePipeline, getPipeline, putPipeline, formPostPipeline, headPipeline, optionsPipeline ]

    @Test
    public void testGETRequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(getPipeline))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(getPipeline))

    }

    @Test
    public void testPUTRequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, '/services/objectstorage/bucket/object')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(putPipeline))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(putPipeline))
    }

    @Test
    public void testHEADRequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, '/services/objectstorage/bucket/object')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(headPipeline))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(headPipeline))

    }

    @Test
    public void testPOSTRequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/objectstorage/bucket/object')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(putPipeline))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(formPostPipeline))
    }

    @Test
    public void testDELETERequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, '/services/objectstorage/bucket/object')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(deletePipeline))

        request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, '/services/objectstorage/bucket/object')
        request.addHeader(HttpHeaders.Names.CONTENT_TYPE, 'multipart/form-data; boundary=xxxx')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(deletePipeline))
    }

    @Test
    public void testOPTIONSRequests() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, '/services/objectstorage/bucket/object')
        assert(pipelines.findAll { return ((FilteredPipeline)it).checkAccepts(request) } == Lists.asList(optionsPipeline))
    }
}
