/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
