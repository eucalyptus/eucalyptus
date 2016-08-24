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
