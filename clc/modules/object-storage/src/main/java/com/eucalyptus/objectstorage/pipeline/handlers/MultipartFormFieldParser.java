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
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Populates the form field map in the message based on the content body
 * Subsequent stages/handlers can use the map exclusively
 */
public class MultipartFormFieldParser {
    private static Logger LOG = Logger.getLogger(MultipartFormFieldParser.class);

    private static final String PART_HEADER_BOUNDARY = "\r\n\r\n"; //part headers and content separated by double-newline
    private static final String PART_LINE_DELIMITER = "\r\n";
    private static final String CR = "\r";

    public static Map<String, Object> parseForm(String msgContentTypeHeader, long requestContentLength, ChannelBuffer content) throws Exception {
        Map<String, Object> formFields = Maps.newHashMap();
        //add this as it's needed for filtering the body later in the pipeline.
        String boundary = getFormBoundary(msgContentTypeHeader);
        formFields.put(ObjectStorageProperties.FORM_BOUNDARY_FIELD, boundary);

        processFormParts(boundary, formFields, content, requestContentLength);
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
     * Populates the form fields map based on the message content buffer
     * @param boundary
     * @param formFields
     * @param buffer
     * @throws com.eucalyptus.auth.login.AuthenticationException
     */
    protected static void processFormParts(String boundary, Map formFields, ChannelBuffer buffer, long fullContentLength) throws Exception {
        String message = getMessageString(buffer);
        String[] parts = message.split(boundary + PART_LINE_DELIMITER); //Split on the boundary plus trailing crlf, won't match last part, but that's okay
        int currentOffset = 0;
        int boundarySize = boundary.length() + PART_LINE_DELIMITER.length();
        for (String part : parts) {
            if(part.length() == 0) {
                //Leading newline for form body, skip it.
                currentOffset += part.length() + boundarySize;
                continue;
            }
            String[] partPieces = part.split(PART_HEADER_BOUNDARY);
            if(partPieces.length != 2) {
                throw new MalformedPOSTRequestException("Invalid form part: " + part);
            }
            String partHeader = partPieces[0];
            String partContent = partPieces[1];
            Map<String, String> keyMap = parseFormPartHeaders(partHeader);
            String key = keyMap.get("name");
            if(Strings.isNullOrEmpty(key)) {
                throw new MalformedPOSTRequestException("Invalid part name null: " + partHeader);
            } else if(Strings.isNullOrEmpty(partContent)) {
                throw new MalformedPOSTRequestException("Empty part content");
            }

            if (ObjectStorageProperties.FormField.file.toString().equals(key)) {
                formFields.put(key, keyMap.get("filename")); //Add filename if found
                String contentType = keyMap.get(HttpHeaders.Names.CONTENT_TYPE);
                formFields.put(HttpHeaders.Names.CONTENT_TYPE, contentType);
                //Put the data into the form field with correct offsets etc.
                getFirstChunk(formFields, ChannelBuffers.wrappedBuffer(part.getBytes("UTF-8")) , currentOffset, fullContentLength, boundary);
            } else {
                formFields.put(key, partContent.trim());
            }
            currentOffset += part.length() + boundarySize;
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
    protected static void getFirstChunk(Map formFields, ChannelBuffer buffer, int startingOffset, long contentLength, String boundary) throws Exception {
        buffer.markReaderIndex();

        byte[] read = new byte[buffer.readableBytes()];
        buffer.readBytes(read);
        int index = getLastIndex(read, PART_HEADER_BOUNDARY.getBytes("UTF-8"));
        if (index > -1) {
            int firstIndex = index + 1;
            int lastIndex = read.length;
            boundary = PART_LINE_DELIMITER + boundary;
            //Find the next boundary...end of the data chunk
            //index = getFirstIndex(read, firstIndex, boundary.getBytes("UTF-8"));
            index = getFirstIndex(read, firstIndex, CR.getBytes("UTF-8"));
            if (index > -1) {
                lastIndex = index;
            }

            ChannelBuffer firstBuffer = ChannelBuffers.copiedBuffer(read, firstIndex, (lastIndex - firstIndex));
            formFields.put(ObjectStorageProperties.FIRST_CHUNK_FIELD, firstBuffer);

            //The file content length is total length - offset of the start. Assume no trailing data.
            // Need to look at how to determine if trailing data is there and adjust. Account for trailing boundary + '--\r\n'
            long fileContentLength = contentLength - startingOffset - firstIndex - boundary.getBytes("UTF-8").length - 4;
            if(lastIndex < read.length) {
                //The form ends before the end of the message chunk, and we know about it now, so use that size directly.
                fileContentLength = lastIndex - firstIndex;
            }
            formFields.put(ObjectStorageProperties.UPLOAD_LENGTH_FIELD, fileContentLength);
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
