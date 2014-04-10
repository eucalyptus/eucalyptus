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

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.http.MappingHttpRequest
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion

/**
 * Created by zhill on 4/5/14.
 */
class POSTRequestGenerator {
    static String boundary = "9431149156168"
    static String boundaryField = "--" + boundary
    static String testContent = "testingdatacontent1231231231231321231231231231232123123123132123"
    static String accessKey = "testaccesskey"
    static String currentB64Policy
    static String currentSignature

    protected static String getContentString(String accessKey, String keyName, String content, String policy, String signature, String redirectUrl, String acl) {
        return '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="Policy"\r\n' +
        'Content-Type: text/plain\r\n\r\n' +
        policy + '\r\n' +
        '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="AWSAccessKeyId"\r\n' +
        'Content-Type: text/plain\r\n\r\n' +
        accessKey + '\r\n' +
        '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="key"\r\n' +
        'Content-Type: text/plain\r\n\r\n' +
        keyName + '\r\n' +
        '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="Signature"\r\n' +
        'Content-Type: text/plain\r\n\r\n' +
        signature + '\r\n' +
        '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="acl"\r\n' +
        'Content-Type: text/plain\r\n\r\n' +
        'aws-exec-read\r\n' +
        '--' + boundary + '\r\n' +
        'Content-Disposition: form-data; name="file"; filename="/tmp/euca-bundle-FnZatV/myinstance0.part.00"\r\n' +
        'Content-Type: application/octet-stream\r\n\r\n' +
        content + '\r\n' +
        '--' + boundary + '--\r\n'

        /*return "--" + boundary +
                "\r\nContent-Disposition: form-data; name=\"key\"\r\n\r\n" +
                keyName + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"acl\"\r\n\r\n" +
                acl + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"success_action_redirect\"\r\n\r\n" +
                redirectUrl + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"x-amz-meta-tag\"\r\n\r\n" +
                "Some test metadata\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"AWSAccessKeyId\"\r\n\r\n" +
                accessKey + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"Policy\"\r\n\r\n" +
                policy + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"Signature\"\r\n\r\n" +
                signature + "\r\n" +
                boundaryField + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n" +
                content + "\r\n" +
                boundaryField + "--" + "\r\n"
                */
        //"Content-Disposition: form-data; name=\"submit\"\r\n\r\n" +
        //"Upload to Amazon S3\r\n" +
        //boundaryField
    }

    static MappingHttpRequest getPOSTRequest(String bucket, String keyName, String acl) {
        String body = getContentString(accessKey, keyName, testContent, "fakepolicy", "fakesignature", "http://localhost", acl)
        currentB64Policy = "fakepolicy" // not testing actual encoding
        currentSignature = "fakesignature"
        ChannelBuffer contentBuffer = ChannelBuffers.wrappedBuffer(body.getBytes("UTF-8"))
        MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/services/objectstorage/" + bucket)
        request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
        request.setContent(contentBuffer)
        request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, body.getBytes("UTF-8").length)
        return request
    }
}
