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

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.objectstorage.exceptions.s3.MalformedPOSTRequestException;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.records.Logs;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Populates the form field map in the message based on the content body
 * Subsequent stages/handlers can use the map exclusively
 */
public class MultipartFormPartParser {
    private static Logger LOG = Logger.getLogger(MultipartFormPartParser.class);

    protected static final byte[] PART_HEADER_BOUNDARY_BYTES = new byte[] { 0x0D, 0x0A, 0x0D, 0x0A};
    protected static final byte[] PART_LINE_DELIMITER_BYTES = new byte[] { 0x0D, 0x0A };
    protected static final String PART_HEADER_BOUNDARY = "\r\n\r\n";
    protected static final String PART_LINE_DELIMITER = "\r\n";

    public static Map<String, Object> parseForm(String msgContentTypeHeader, long requestContentLength, ChannelBuffer content) throws Exception {
        Map<String, Object> formFields = Maps.newHashMap();

        //add this as it's needed for filtering the body later in the pipeline.
        String boundaryStr = getFormBoundary(msgContentTypeHeader);

        //Don't include the leading whitespace because that causes the first boundary to not be found properly when doing scans.
        byte[] boundaryBytes = (boundaryStr + PART_LINE_DELIMITER).getBytes("UTF-8");
        byte[] finalBoundaryBytes = (boundaryStr + "--"+ PART_LINE_DELIMITER).getBytes("UTF-8");
        formFields.put(ObjectStorageProperties.FormField.x_ignore_formboundary.toString(), boundaryBytes);

        processFormParts(boundaryBytes, finalBoundaryBytes, formFields, content, requestContentLength);
        return formFields;
    }

    protected static String getFormBoundary(String contentTypeHeader) throws Exception {
        //Find the boundary identifier
        if (contentTypeHeader.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA)) {
            String boundary = getFormFieldKeyName(contentTypeHeader, "boundary");
            boundary = "--" + boundary;
            return boundary;
        } else {
            throw new MalformedPOSTRequestException("Content-Type not multipart/form-data");
        }
    }

    /**
     * Populates the form fields map based on the message content buffer.
     * @param boundaryBytes the boundary byte set with no leading whitespace. E.g. --boundary\r\n
     * @param finalBoundaryBytes the expanded byte set for the last boundary with no leading whitespace. E.g. --boundary--\r\n
     * @param formFields the fields to populate with the results
     * @param buffer the message content buffer to process
     * @param fullContentLength the length of the full message, which may be different than buffer length for chunked messages
     * @throws com.eucalyptus.auth.login.AuthenticationException
     */
    protected static void processFormParts(byte[] boundaryBytes, byte[] finalBoundaryBytes, Map formFields, ChannelBuffer buffer, long fullContentLength) throws Exception {
        PartIterator iter = new PartIterator(boundaryBytes, finalBoundaryBytes, buffer);
        int offset = 0;
        while(iter.hasNext()) {
            ChannelBuffer partSlice = iter.next();
            partSlice.markReaderIndex();

            if(partSlice.readableBytes() > boundaryBytes.length) {

                int headerEnd = OSGUtil.findFirstMatchInBuffer(partSlice, 0, PART_HEADER_BOUNDARY_BYTES);
                if(headerEnd == -1) {
                    throw new MalformedPOSTRequestException("Invalid form part starting at byte offset: " + offset);
                } else {
                    //add the header boundary itself
                    headerEnd += PART_HEADER_BOUNDARY_BYTES.length;
                }

                String partHeader = getMessageString(partSlice.slice(0, headerEnd)).trim();
                Map<String, String> keyMap = parseFormPartHeaders(partHeader);
                String key = keyMap.get("name");
                if(Strings.isNullOrEmpty(key)) {
                    throw new MalformedPOSTRequestException("Invalid part name null: " + partHeader);
                }

                if (ObjectStorageProperties.FormField.file.toString().equals(key)) {
                    formFields.put(key, keyMap.get("filename")); //Add filename if found
                    String contentType = keyMap.get(HttpHeaders.Names.CONTENT_TYPE);
                    formFields.put(ObjectStorageProperties.FormField.Content_Type.toString(), contentType);
                    //Put the data into the form field with correct offsets etc.
                    getFirstChunk(formFields, partSlice, offset, fullContentLength, boundaryBytes, finalBoundaryBytes);
                } else {
                    formFields.put(key, getMessageString(partSlice.slice(headerEnd, partSlice.readableBytes() - headerEnd - boundaryBytes.length)).trim());
                }
            }

            partSlice.resetReaderIndex();
            offset += partSlice.readableBytes();
        }
    }

    static class PartIterator implements Iterator<ChannelBuffer> {
        byte[] boundary;
        byte[] finalBoundary;
        ChannelBuffer buffer;
        int currentIndex;
        int totalSize;

        public PartIterator(byte[] boundaryBytes, byte[] finalBoundaryBytes, ChannelBuffer content) {
            this.boundary = boundaryBytes;
            this.finalBoundary = finalBoundaryBytes;
            this.buffer = content;
            this.currentIndex = 0;
            this.totalSize = content.readableBytes();
        }

        @Override
        public boolean hasNext() {
            return nextPartLength() > 0;
        }

        private int nextPartLength() {
            int nextFound = OSGUtil.findFirstMatchInBuffer(buffer, this.currentIndex, boundary) + boundary.length;
            if(nextFound < boundary.length) {
                //Try the final boundary
                nextFound = OSGUtil.findFirstMatchInBuffer(buffer, this.currentIndex, finalBoundary) + finalBoundary.length;
                if(nextFound < finalBoundary.length) {
                    nextFound = -1;
                }
            }

            if(nextFound > 0) {
                return nextFound - this.currentIndex;
            } else {
                return totalSize - this.currentIndex;
            }
        }

        @Override
        public ChannelBuffer next() {
            int partLength = nextPartLength();
            if(partLength > 0) {
                ChannelBuffer slice = buffer.slice(this.currentIndex, partLength);
                this.currentIndex += partLength;
                return slice;
            } else {
                return null;
            }
        }

        /**
         * Does nothing
         */
        @Override
        public void remove() {
            return;
        }
    }


    /**
     * Gets the first data chunk in this message and sets the IGNORE_PREFIXContent-Length for the actual content length
     * based on the offset it finds
     * Expects a full form part as input e.g. 'Content-Disposition: ....\r\n--boundary--\r\n'
     * @param formFields fields to populate
     * @param buffer the data to process, assumes it is the form part itself, only boundaries should be at the end not the prefix
     * @param startingOffset the overall offset of the buffer within the total message buffer
     * @param contentLength the request-specified content-length
     * @param boundary the boundary to expect at the end of the buffer
     * @throws Exception
     */
    protected static void getFirstChunk(Map formFields, ChannelBuffer buffer, int startingOffset, long contentLength, byte[] boundary, byte[] finalBoundary) throws Exception {
        buffer.markReaderIndex();

        byte[] read = new byte[buffer.readableBytes()];
        buffer.readBytes(read);
        int index = getLastIndex(read, PART_HEADER_BOUNDARY_BYTES);
        if (index > -1) {
            int firstIndex = index + 1;
            int lastIndex;

            //The file content length is total length - offset of the start. Assume no trailing data. This is required for using HTTP on backend of OSG
            long fileContentLength = contentLength - startingOffset - firstIndex - finalBoundary.length - PART_LINE_DELIMITER_BYTES.length;
            lastIndex = (int)(firstIndex + fileContentLength);

            if(lastIndex > read.length) {
                //off the end of the buffer we have now
                //Danger from casting, but current buffer should not be longer than max int size, since chunks are 100K max in euca
                lastIndex = read.length;
            } else {
                //Do a backup check for trailing form fields.
                index = getFirstIndex(read, firstIndex, finalBoundary);
                if(index < 0) {
                    //Handle the case where it isn't the last field, but we have the whole form so we can do length properly
                    index = getFirstIndex(read, firstIndex, boundary);
                }
                if(index > firstIndex) {
                    //Found the end, take off the trailing crlf
                    lastIndex = index - PART_LINE_DELIMITER_BYTES.length;
                    fileContentLength = lastIndex - firstIndex;
                }
            }
            //ChannelBuffer firstBuffer = ChannelBuffers.copiedBuffer(read, firstIndex, (lastIndex - firstIndex));
            ChannelBuffer firstBuffer = buffer.slice(firstIndex, (lastIndex - firstIndex));
            Logs.extreme().debug("Setting first buffer chunk with size: " + firstBuffer.readableBytes());
            formFields.put(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString(), firstBuffer);
            formFields.put(ObjectStorageProperties.FormField.x_ignore_filecontentlength.toString(), fileContentLength);
        }
        buffer.resetReaderIndex();
    }

    /**
     * Gets a form key, value pair from the message string. Expects the message
     * to be the full form field from boundary to boundary (not including any boundaries themselves)
     * e.g. message="Content-Disposition: form-data; name=\"key\"\r\nContent-Type: text/plain\r\n\r\nValue";
     * @param message
     * @return
     */
    protected static Map<String, String> getFormField(String message, String key) throws Exception {
        Map<String, String> keymap = new HashMap<>();
        String[] parts = message.split(";");
        if (parts.length >= 2) {
            if (parts[1].contains(key)) {
                String keystring = parts[1].substring(parts[1].indexOf('=') + 1);
                if (parts.length == 2) {
                    String[] keyparts = keystring.split("\r\n\r\n");
                    String keyName = keyparts[0];
                    keyName = keyName.replaceAll("\"", "");
                    String value = keyparts[1].replaceAll("\r\n", "");
                    keymap.put(keyName, value);
                } else {
                    String keyName = keystring.trim();
                    keyName = keyName.replaceAll("\"", "");
                    String valuestring = parts[2].substring(parts[2].indexOf('=') + 1, parts[2].indexOf("\r\n")).trim();
                    String value = valuestring.replaceAll("\"", "");
                    keymap.put(keyName, value);
                }
            }
        }
        return keymap;
    }

    /**
     * Parses the form field header line, from 'Content-Disposition' to the double-newline.
     * e.g. 'Content-Disposition: form-data; name=\"key\"\r\nContent-Type: text/plain'
     * @param fieldHeaderLine
     * @return
     */
    protected static Map<String, String> parseFormPartHeaders(String fieldHeaderLine) throws MalformedPOSTRequestException {
        if(!fieldHeaderLine.startsWith("Content-Disposition")) {
            throw new MalformedPOSTRequestException("Invalid form encoding on line: " + fieldHeaderLine);
        }

        Map<String, String> headers = Maps.newHashMap();
        String[] lines = fieldHeaderLine.split(PART_LINE_DELIMITER);
        if(lines.length > 0) {
            for(String line : lines) {
                line = line.trim();
                //Split on the value params
                String[] values = line.split(";");
                for(String value : values) {
                    value = value.trim();
                    //Is it a K/V
                    String[] params  = value.split("=");
                    if(params.length == 2) {
                        headers.put(params[0].trim(), params[1].trim().replaceAll("\"","")); //trim surrounding quotes
                    } else {
                        //Try split on ':', must be a header value. e.g. Content-Type
                        params = value.split(":");
                        if(params.length == 2) {
                            //Using header style.
                            headers.put(params[0].trim(), params[1].trim());
                        } else {
                            throw new MalformedPOSTRequestException("Unexpected form field content: " + value);
                        }
                    }
                }
            }
        }
        return headers;
    }

    /**
     * Get the name of the form field.
     * Expects input of the form:
     * 'Content-Disposition: form-data; name="fieldname"; somekey="somevalue"\r\nOptionalHeader: optionalvalue\r\n\r\nACTUALCONTENT\r\n\--boundary'
     * @param message
     * @param key
     * @return
     */
    protected static String getFormFieldKeyName(String message, String key) throws Exception {
        String[] parts = message.split(";");
        if (parts.length > 1) {
            //The key is in the 2nd part, may be more, but not important
            if (parts[1].contains(key + "=")) {
                String[] keyparts = parts[1].split("=", 2);
                if(keyparts.length < 2) {
                    //error
                    throw new MalformedPOSTRequestException("Invalid form field entry: " + parts[1].substring(0, Math.min(128, parts[1].length())));
                }
                return keyparts[1].replaceAll("\"", "").trim();
            }
        }

        //Bad form content, only return a limited error message size
        throw new MalformedPOSTRequestException("Invalid form field entry: " + message.substring(0, Math.min(128, message.length())));
    }

    protected static String getMessageString(ChannelBuffer buffer) throws UnsupportedEncodingException {
        buffer.markReaderIndex();
        byte[] read = new byte[buffer.readableBytes()];
        buffer.readBytes(read);
        buffer.resetReaderIndex();
        return new String(read, "UTF-8");
    }

    protected static int getFirstIndex(byte[] bytes, int sourceIndex, byte[] bytesToCompare) {
        int firstIndex = -1;
        if ((bytes.length - sourceIndex) < bytesToCompare.length)
            return firstIndex;
        for (int i = sourceIndex; i < bytes.length; ++i) {
            for (int j = 0; j < bytesToCompare.length && ((i + j) < bytes.length); ++j) {
                if (bytes[i + j] == bytesToCompare[j]) {
                    firstIndex = i;
                } else {
                    firstIndex = -1;
                    break;
                }
            }
            if (firstIndex != -1)
                return firstIndex;
        }
        return firstIndex;
    }

    protected static int getLastIndex(byte[] bytes, byte[] bytesToCompare) {
        int lastIndex = -1;
        if (bytes.length < bytesToCompare.length)
            return lastIndex;
        for (int i = 0; i < bytes.length; ++i) {
            for (int j = 0; j < bytesToCompare.length && ((i + j) < bytes.length); ++j) {
                if (bytes[i + j] == bytesToCompare[j]) {
                    lastIndex = i + j;
                } else {
                    lastIndex = -1;
                    break;
                }
            }
            if (lastIndex != -1)
                return lastIndex;
        }
        return lastIndex;
    }
}
