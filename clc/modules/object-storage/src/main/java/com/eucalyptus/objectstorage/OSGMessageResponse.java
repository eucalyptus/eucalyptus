/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.objectstorage;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class OSGMessageResponse {
    private HttpResponseStatus httpResponseStatus;
    private String httpResponseBody;

    public String getHttpResponseBody() {
        return httpResponseBody;
    }

    public void setHttpResponseBody(String httpResponseBody) {
        this.httpResponseBody = httpResponseBody;
    }
    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public void setHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public OSGMessageResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
        //no response body
    }

    public OSGMessageResponse(HttpResponseStatus httpResponseStatus, String httpResponseBody) {
        this.httpResponseStatus = httpResponseStatus;
        this.httpResponseBody = httpResponseBody;
    }

    public static final OSGMessageResponse Continue = new OSGMessageResponse(HttpResponseStatus.CONTINUE);
    public static final OSGMessageResponse Whitespace = new OSGMessageResponse(null, "");
}
