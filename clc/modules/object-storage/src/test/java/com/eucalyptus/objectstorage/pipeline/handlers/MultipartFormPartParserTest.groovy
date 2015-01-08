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

/**
 * Created by zhill on 4/3/14.
 */
@CompileStatic
class MultipartFormPartParserTest {

    @Test
    public void testParseForm() {
        MappingHttpRequest request = POSTRequestGenerator.getPOSTRequest("testbucket", "testkey", "private")
        Map<String, Object> formFields;

        long contentLength = Long.parseLong(request.getHeader(HttpHeaders.Names.CONTENT_LENGTH))
        String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
        formFields = MultipartFormPartParser.parseForm(contentType, contentLength, request.getContent())

        assert(formFields.get("key") == "testkey")
        assert(formFields.get(HttpHeaders.Names.CONTENT_TYPE) == "application/octet-stream") //the value for the whole request
        assert(formFields.get("acl") == "aws-exec-read")
        assert(formFields.get("AWSAccessKeyId") == POSTRequestGenerator.accessKey)
        assert(formFields.get("Policy") == POSTRequestGenerator.currentB64Policy)
        assert(formFields.get("Signature") == POSTRequestGenerator.currentSignature)
        byte[] outputBuffer = new byte[((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readableBytes()]
        ((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readBytes(outputBuffer)
        assert(outputBuffer == POSTRequestGenerator.testContent)
    }

    @Test
    void testGetFormBoundary() {
        MappingHttpRequest request = POSTRequestGenerator.getPOSTRequest("testbucket", "testkey", "private")
        String boundaryFound = MultipartFormPartParser.getFormBoundary(request.getHeader(HttpHeaders.Names.CONTENT_TYPE))
        assert(boundaryFound != null && boundaryFound == POSTRequestGenerator.boundaryField)
    }

    @Test
    void testGetFirstChunkComplete() {
        String boundary = "--boundary"
        byte[] boundaryBytes = (boundary + "\r\n").getBytes("UTF-8")
        byte[] finalBoundaryBytes = (boundary + "--\r\n").getBytes("UTF-8")
        byte[] content = POSTRequestGenerator.getRandomData(64)
        content[10] = '\r'.getBytes('UTF-8')[0]
        content[11] = '\n'.getBytes('UTF-8')[0]
        content[25] = '\r'.getBytes('UTF-8')[0]
        content[26] = '\n'.getBytes('UTF-8')[0]

        //Would normally use a ChannelBuffer.wrappedBuffer here, but Groovy barfs on the method def
        byte[] headers = ("Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n").getBytes("UTF-8")

        ChannelBuffer data = ChannelBuffers.buffer(headers.length + content.length + 2 + finalBoundaryBytes.length)
        data.writeBytes(headers)
        data.writeBytes(content)
        data.writeBytes("\r\n".getBytes("UTF-8"))
        data.writeBytes(finalBoundaryBytes)
        Map<String, Object> formFields = Maps.newHashMap()
        MultipartFormPartParser.getFirstChunk(formFields, data, 0, data.readableBytes(), boundaryBytes, finalBoundaryBytes)
        byte[] output = new byte[((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readableBytes()]
        ((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).getBytes(0, output)
        assert(output == content)
        assert(formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()) == content.length)

        //Test with normal, non-final boundary
        data = ChannelBuffers.buffer(headers.length + content.length + 2 +boundaryBytes.length)
        data.writeBytes(headers)
        data.writeBytes(content)
        data.writeBytes("\r\n".getBytes("UTF-8"))
        data.writeBytes(boundaryBytes)

        formFields = Maps.newHashMap()
        MultipartFormPartParser.getFirstChunk(formFields, data, 0, data.readableBytes(), boundaryBytes, finalBoundaryBytes)
        output = new byte[((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readableBytes()]
        ((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).getBytes(0, output)
        assert(output == content)
        assert(formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()) == content.length)
    }


    @Test
    void testGetFirstChunkOfMany() {
        String boundary = "--boundary"
        byte[] boundaryBytes = (boundary + "\r\n").getBytes("UTF-8")
        byte[] finalBoundaryBytes = (boundary + "--\r\n").getBytes("UTF-8")

        byte[] content = POSTRequestGenerator.getRandomData(128)
        content[100] = '\r'.getBytes('UTF-8')[0]
        content[101] = '\n'.getBytes('UTF-8')[0]

        byte[] headers = (boundary + "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n\r\n").getBytes("UTF-8")
        byte[] trailer = ('\r\n' + boundary + '--\r\n').getBytes("UTF-8")

        ChannelBuffer testData = ChannelBuffers.buffer(headers.length + content.length + trailer.length)
        testData.writeBytes(headers)
        testData.writeBytes(content)
        testData.writeBytes(trailer)

        Map<String, Object> formFields = Maps.newHashMap()
        MultipartFormPartParser.getFirstChunk(formFields, testData, 0, testData.readableBytes(), boundaryBytes, finalBoundaryBytes)
        byte[] output = new byte[((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readableBytes()]
        ((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).getBytes(0, output)
        assert(output == content)
        assert(formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()) == content.length)


        testData = ChannelBuffers.buffer(headers.length + content.length)
        testData.writeBytes(headers)
        testData.writeBytes(content)
        int boundarySize = finalBoundaryBytes.length
        int fakeSize = testData.readableBytes() + 1000 + boundarySize + 2 //to simulate the rest of the content size
        int fakeContentLength = content.length + 1000
        println 'Fake size = ' + fakeSize + ' , fake content length = ' + fakeContentLength
        formFields = Maps.newHashMap()
        MultipartFormPartParser.getFirstChunk(formFields, testData, 0, fakeSize, boundaryBytes, finalBoundaryBytes)

        output = new byte[((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).readableBytes()]
        ((ChannelBuffer)formFields.get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString())).getBytes(0, output)
        assert(output == content)
        assert(formFields.get(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString()) == fakeContentLength)
    }


    @Test
    void testGetMessageString() {
        String source = "testdata123\r\n\r\nhelloworld;\"blah\"blah\r\n"
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(source.getBytes("UTF-8"))
        String messageString = MultipartFormPartParser.getMessageString(buffer)
        assert(messageString == source)
    }

    @Test
    void testParseFormPart() {
        String headerLine = "Content-Disposition: form-data; name=\"file\"; filename=\"somefile.html\"\r\n" +
                "Content-Type: application/text\r\n" +
                "Content-Length: somelength";
        Map<String, String> headers = MultipartFormPartParser.parseFormPartHeaders(headerLine);
        assert(headers.get("Content-Disposition") == "form-data")
        assert(headers.get("Content-Type") == "application/text")
        assert(headers.get("Content-Length") == "somelength")
        assert(headers.get("name") == "file")
        assert(headers.get("filename") == "somefile.html")
    }

    @Test
    void testGetFirstIndex() {
        byte[] testSource = "hello\r\nworld\nblah1blah2blah3".getBytes("UTF-8")
        int start = MultipartFormPartParser.getFirstIndex(testSource, 0 , "world".getBytes("UTF-8"))
        assert(start == 7)

        start = MultipartFormPartParser.getFirstIndex(testSource, 0 , "\r\n".getBytes("UTF-8"))
        assert(start == 5)

        start = MultipartFormPartParser.getFirstIndex(testSource, 0 , "hello".getBytes("UTF-8"))
        assert(start == 0)

        start = MultipartFormPartParser.getFirstIndex(testSource, 0 , "3".getBytes("UTF-8"))
        assert(start == testSource.length - 1)

        start = MultipartFormPartParser.getFirstIndex(testSource, 0 , "barf".getBytes("UTF-8"))
        assert(start == -1)
    }

    @Test
    void testGetLastIndex() {
        byte[] testSource = "hello\r\nworld\nblah1blah2blah3".getBytes("UTF-8")
        int start = MultipartFormPartParser.getLastIndex(testSource, "world".getBytes("UTF-8"))
        assert(start == 11)

        start = MultipartFormPartParser.getLastIndex(testSource, "\r\n".getBytes("UTF-8"))
        assert(start == 6)

        start = MultipartFormPartParser.getLastIndex(testSource, "hello".getBytes("UTF-8"))
        assert(start == 4)

        start = MultipartFormPartParser.getLastIndex(testSource, "3".getBytes("UTF-8"))
        assert(start == testSource.length - 1)

        start = MultipartFormPartParser.getLastIndex(testSource, "barf".getBytes("UTF-8"))
        assert(start == -1)
    }
}
