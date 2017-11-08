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
