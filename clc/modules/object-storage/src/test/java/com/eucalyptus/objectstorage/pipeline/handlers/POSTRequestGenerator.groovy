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

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.http.MappingHttpRequest
import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion

/**
 * Created by zhill on 4/5/14.
 */
@CompileStatic
class POSTRequestGenerator {
  static Random randGen = new Random(System.currentTimeMillis())
  static String boundary = UUID.randomUUID().toString().replace("-","")
  static String boundaryField = "--" + boundary
  static byte[] testContent = getRandomData(512)
  static String accessKey = "testaccesskey"
  static String currentB64Policy
  static String currentSignature

  static byte[] getRandomData(int length) {
    byte[] buffer = new byte[length]
    randGen.nextBytes(buffer)
    return buffer
  }

  static ChannelBuffer getContentBuffer(String accessKey, String keyName, byte[] content, String policy, String signature, String redirectUrl, String acl) {
    byte[] headers = ('--' + boundary + '\r\n' +
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
        'Content-Type: application/octet-stream\r\n\r\n').getBytes('UTF-8')
    byte[] trailer = ('\r\n' + '--' + boundary + '--\r\n').getBytes('UTF-8')
    ChannelBuffer contentBuffer = ChannelBuffers.buffer(headers.length + content.length + trailer.length)
    contentBuffer.writeBytes(headers)
    contentBuffer.writeBytes(content)
    contentBuffer.writeBytes(trailer)
    return contentBuffer
  }

  static MappingHttpRequest getPOSTRequest(String bucket, String keyName, String acl) {
    ChannelBuffer contentBuffer = getContentBuffer(accessKey, keyName, testContent, "fakepolicy", "fakesignature", "http://localhost", acl)
    currentB64Policy = "fakepolicy" // not testing actual encoding
    currentSignature = "fakesignature"
    MappingHttpRequest request = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/services/objectstorage/" + bucket)
    request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
    request.setContent(contentBuffer)
    request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, contentBuffer.readableBytes())
    return request
  }
}
