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

import com.eucalyptus.http.MappingHttpRequest

import com.eucalyptus.walrus.exceptions.MethodNotAllowedException

import groovy.transform.CompileStatic

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

/**
 * Created by zhill on 3/17/14.
 */
@CompileStatic
public class WalrusRESTBindingTest {

  @Test
  public void testGetOperation() throws Exception {
    WalrusRESTBinding binding = new WalrusRESTBinding();

    MappingHttpRequest request;

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/WalrusBackend/bucket/object");
    String path = binding.getOperationPath(request);
    assert("/bucket/object".equals(path));
  }

  @Test
  public void testGetOperationPath() throws Exception {
    WalrusRESTBinding binding = new WalrusRESTBinding();

    MappingHttpRequest request;

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/WalrusBackend/bucket/object");
    String path = binding.getOperationPath(request);
    assert("/bucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/WalrusBackend/bucket");
    path = binding.getOperationPath(request);
    assert("/bucket".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/WalrusBackend");
    path = binding.getOperationPath(request);
    assert("".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/WalrusBackendbucket/object");
    path = binding.getOperationPath(request);
    assert("/services/WalrusBackendbucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object");
    path = binding.getOperationPath(request);
    assert("/bucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    path = binding.getOperationPath(request);
    assert("/".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object/");
    path = binding.getOperationPath(request);
    assert("/bucket/object/".equals(path));
  }

  @Test(expected=MethodNotAllowedException.class)
  public void testBindHeadOperation() throws Exception {
    WalrusRESTBinding binding = new WalrusRESTBinding();
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/");
    request.setHeader(HttpHeaders.Names.HOST, "foo.bar.com");
    final Object o = binding.bind(request);
  }
}
