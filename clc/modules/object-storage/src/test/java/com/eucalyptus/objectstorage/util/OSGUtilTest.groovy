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

package com.eucalyptus.objectstorage.util;

import com.eucalyptus.http.MappingHttpRequest
import com.eucalyptus.objectstorage.pipeline.ObjectStorageRESTPipeline;
import com.eucalyptus.objectstorage.util.OSGUtil;
import groovy.transform.CompileStatic;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

/**
 * Created by zhill on 3/7/14.
 */
@CompileStatic
public class OSGUtilTest {

  @Test
  public void testGetOperation() throws Exception {
    MappingHttpRequest request;

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object");
    String path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket/object".equals(path));
  }

  @Test
  public void testGetOperationPath() throws Exception {
    MappingHttpRequest request;

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket/object");
    String path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/bucket");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstorage/services/objectstorage/bucket");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/services/objectstorage/bucket".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/objectstoragebucket/object");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/services/objectstoragebucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/bucket/object/");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket/object/".equals(path));

    //Test with /services/Walrus path for legacy
    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/Walrus/bucket/object");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket/object".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/Walrus/bucket");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/bucket".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/Walrus");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("".equals(path));

    request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/services/Walrusbucket/object");
    path = OSGUtil.getOperationPath(request, ObjectStorageRESTPipeline.getServicePaths());
    assert("/services/Walrusbucket/object".equals(path));


  }

  @Test
  public void testGetTarget() throws Exception {
    String[] target = OSGUtil.getTarget("/bucket/object");
    assert (target.length == 2);
    assert ("bucket".equals(target[0]));
    assert ("object".equals(target[1]));

    target = OSGUtil.getTarget("///bucket/object");
    assert (target.length == 2);
    assert ("bucket".equals(target[0]));
    assert ("object".equals(target[1]));

    target = OSGUtil.getTarget("/bucket//object");
    assert (target.length == 2);
    assert ("bucket".equals(target[0]));
    assert ("/object".equals(target[1]));

    target = OSGUtil.getTarget("//bucket/object//");
    assert (target.length == 2);
    assert ("bucket".equals(target[0]));
    assert ("object//".equals(target[1]));

    target = OSGUtil.getTarget("///bucket///object//");
    assert (target.length == 2);
    assert ("bucket".equals(target[0]));
    assert ("//object//".equals(target[1]));

    target = OSGUtil.getTarget("///bucket/");
    assert (target.length == 1);
    assert ("bucket".equals(target[0]));

    target = OSGUtil.getTarget("///");
    assert (target == null);

    target = OSGUtil.getTarget("/");
    assert (target == null);
  }
}
