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
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.junit.Test

import java.nio.charset.Charset

/**
 * Created by zhill on 4/3/14.
 */
@CompileStatic
class MultipartFormFieldParserTest {

    @Test
    public void testParseForm() {
        MappingHttpRequest request = POSTRequestGenerator.getPOSTRequest("testbucket", "testkey", "private")
        Map<String, Object> formFields;

        long contentLength = Long.parseLong(request.getHeader(HttpHeaders.Names.CONTENT_LENGTH))
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
        formFields = MultipartFormFieldParser.parseForm(contentType, contentLength, request.getContent())

        assert(formFields.get("key") == "testkey")
        assert(formFields.get(HttpHeaders.Names.CONTENT_TYPE) == "application/octet-stream") //the value for the whole request
        assert(formFields.get("acl") == "aws-exec-read")
        assert(formFields.get("AWSAccessKeyId") == POSTRequestGenerator.accessKey)
        assert(formFields.get("Policy") == POSTRequestGenerator.currentB64Policy)
        assert(formFields.get("Signature") == POSTRequestGenerator.currentSignature)
        assert(((ChannelBuffer)formFields.get(ObjectStorageProperties.IGNORE_PREFIX + "FirstDataChunk")).toString(Charset.forName("UTF-8")) == POSTRequestGenerator.testContent)
    }

    @Test
    void testGetFormBoundary() {
        MappingHttpRequest request = POSTRequestGenerator.getPOSTRequest("testbucket", "testkey", "private")
        String boundaryFound = MultipartFormFieldParser.getFormBoundary(request.getHeader(HttpHeaders.Names.CONTENT_TYPE))
        assert(boundaryFound != null && boundaryFound == POSTRequestGenerator.boundaryField)
    }

    @Test
    void testGetFirstChunkComplete() {
        String boundary = "--boundary"
        String content = "blah-blah-blah-blahcontentishere"
        String testData = boundary + "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n" +
                content + "\r\n" + boundary + "--"
        Map<String, Object> formFields = Maps.newHashMap()
        MultipartFormFieldParser.getFirstChunk(formFields, ChannelBuffers.wrappedBuffer(testData.getBytes("UTF-8")), 0, testData.getBytes("UTF-8").length, boundary)
        print 'Got content: "' + ((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) + '"'
        assert(((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) == content)
        assert(formFields.get(ObjectStorageProperties.UPLOAD_LENGTH_FIELD) == content.getBytes("UTF-8").length)


        testData = "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n" +
                content +  "\r\n" + boundary + "\r\nContent-Disposition: form-data; name=\"trailer\"\r\n\r\nacl\r\n"
        formFields = Maps.newHashMap()
        MultipartFormFieldParser.getFirstChunk(formFields, ChannelBuffers.wrappedBuffer(testData.getBytes("UTF-8")), 0, testData.getBytes("UTF-8").length, boundary)
        print 'Got content: "' + ((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) + '"'
        assert(((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) == content)
        assert(formFields.get(ObjectStorageProperties.UPLOAD_LENGTH_FIELD) == content.getBytes("UTF-8").length)
    }


    @Test
    void testGetFirstChunkOfMany() {
        String boundary = "--boundary"
        String content = "blah-blah-blah-blahcontentishere"
        String testData = boundary + "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n" +
                content + '\r\n' + boundary + '--\r\n'
        Map<String, Object> formFields = Maps.newHashMap()
        MultipartFormFieldParser.getFirstChunk(formFields, ChannelBuffers.wrappedBuffer(testData.getBytes("UTF-8")), 0, testData.getBytes("UTF-8").length, boundary)
        println 'Got content: "' + ((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) + '"'
        assert(((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) == content)
        assert(formFields.get(ObjectStorageProperties.UPLOAD_LENGTH_FIELD) == content.getBytes("UTF-8").length)



        testData = "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n" + content
        int boundarySize = boundary.getBytes(("UTF-8")).length
        int fakeSize = testData.getBytes("UTF-8").length + 1000 + boundarySize + 6 //to simulate the rest of the content size
        int fakeContentLength = content.getBytes("UTF-8").length + 1000
        println 'Fake size = ' + fakeSize + ' , fake content length = ' + fakeContentLength
        formFields = Maps.newHashMap()
        MultipartFormFieldParser.getFirstChunk(formFields, ChannelBuffers.wrappedBuffer(testData.getBytes("UTF-8")), 0, fakeSize, boundary)
        println 'Got content: "' + ((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) + '"'
        assert(((ChannelBuffer)formFields.get(ObjectStorageProperties.FIRST_CHUNK_FIELD)).toString(Charset.forName("UTF-8")) == content)
        assert(formFields.get(ObjectStorageProperties.UPLOAD_LENGTH_FIELD) == fakeContentLength)
    }


    @Test
    void testGetMessageString() {
        String source = "testdata123\r\n\r\nhelloworld;\"blah\"blah\r\n"
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(source.getBytes("UTF-8"))
        String messageString = MultipartFormFieldParser.getMessageString(buffer)
        assert(messageString == source)
    }

    @Test
    void testParseFormPart() {
        String headerLine = "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n" +
                "Content-Length: somelength";
        Map<String, String> headers = MultipartFormFieldParser.parseFormPartHeaders(headerLine);
        assert(headers.get("Content-Disposition") == "form-data")
        assert(headers.get("Content-Type") == "application/text")
        assert(headers.get("Content-Length") == "somelength")
        assert(headers.get("name") == "file")
        assert(headers.get("filename") == "somefile.html")
    }

    @Test
    void testGetFirstIndex() {
        byte[] testSource = "hello\r\nworld\nblah1blah2blah3".getBytes("UTF-8")
        int start = MultipartFormFieldParser.getFirstIndex(testSource, 0 , "world".getBytes("UTF-8"))
        assert(start == 7)

        start = MultipartFormFieldParser.getFirstIndex(testSource, 0 , "\r\n".getBytes("UTF-8"))
        assert(start == 5)

        start = MultipartFormFieldParser.getFirstIndex(testSource, 0 , "hello".getBytes("UTF-8"))
        assert(start == 0)

        start = MultipartFormFieldParser.getFirstIndex(testSource, 0 , "3".getBytes("UTF-8"))
        assert(start == testSource.length - 1)

        start = MultipartFormFieldParser.getFirstIndex(testSource, 0 , "barf".getBytes("UTF-8"))
        assert(start == -1)
    }

    @Test
    void testGetLastIndex() {
        byte[] testSource = "hello\r\nworld\nblah1blah2blah3".getBytes("UTF-8")
        int start = MultipartFormFieldParser.getLastIndex(testSource, "world".getBytes("UTF-8"))
        assert(start == 11)

        start = MultipartFormFieldParser.getLastIndex(testSource, "\r\n".getBytes("UTF-8"))
        assert(start == 6)

        start = MultipartFormFieldParser.getLastIndex(testSource, "hello".getBytes("UTF-8"))
        assert(start == 4)

        start = MultipartFormFieldParser.getLastIndex(testSource, "3".getBytes("UTF-8"))
        assert(start == testSource.length - 1)

        start = MultipartFormFieldParser.getLastIndex(testSource, "barf".getBytes("UTF-8"))
        assert(start == -1)
    }
}
