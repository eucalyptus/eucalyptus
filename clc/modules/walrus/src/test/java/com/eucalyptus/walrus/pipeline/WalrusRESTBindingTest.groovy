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

import com.eucalyptus.http.MappingHttpRequest
import groovy.transform.CompileStatic
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
}
