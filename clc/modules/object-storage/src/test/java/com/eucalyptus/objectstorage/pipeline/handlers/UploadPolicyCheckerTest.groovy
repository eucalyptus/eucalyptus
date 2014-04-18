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

import com.eucalyptus.crypto.util.B64
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.common.DateFormatter
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.apache.commons.codec.binary.Base64
import org.junit.Assert
import org.junit.Test

/**
 * Created by zhill on 4/17/14.
 */
@CompileStatic
class UploadPolicyCheckerTest {
    @Test
    void testCheckPolicy() {
        String acl = 'private'
        String expiration = DateFormatter.dateToListingFormattedString(new Date(System.currentTimeMillis() + 10000));
        String bucket = 'bucket'
        String key = 'key'
        String contentType = "application/octet-stream"
        String policy = String.format('{"expiration": "%s" , "conditions": [{ "acl": "%s"},{"bucket": "%s"}, ["starts-with","$key", "ke"]]}', expiration, acl, bucket, key)
        Map<String, String> formFields = Maps.newHashMap()
        formFields.put(ObjectStorageProperties.FormField.acl.toString(), acl)
        formFields.put(ObjectStorageProperties.FormField.key.toString(), key)
        formFields.put(ObjectStorageProperties.FormField.bucket.toString(), bucket)
        formFields.put(ObjectStorageProperties.FormField.Content_Type.toString(), contentType)
        formFields.put(ObjectStorageProperties.FormField.Policy.toString(), B64.standard.encString(policy))

        try {
            UploadPolicyChecker.checkPolicy(formFields)
        } catch(Exception e) {
            Assert.fail('Unexpected exception')
        }
    }

    @Test
    void testCheckPolicyRejection() {
        String acl = 'private'
        String expiration = DateFormatter.dateToListingFormattedString(new Date(System.currentTimeMillis() + 10000));
        String bucket = 'bucket'
        String key = 'key'
        String contentType = "application/octet-stream"
        String policy = String.format('{"expiration": "%s" , "conditions": [{ "acl": "%s"},{"bucket": "%s"}, ["starts-with","$key", "mismatchthiskey"]]}', expiration, acl, bucket, key)
        Map<String, String> formFields = Maps.newHashMap()
        formFields.put(ObjectStorageProperties.FormField.acl.toString(), acl)
        formFields.put(ObjectStorageProperties.FormField.key.toString(), key)
        formFields.put(ObjectStorageProperties.FormField.bucket.toString(), bucket)
        formFields.put(ObjectStorageProperties.FormField.Content_Type.toString(), contentType)
        formFields.put(ObjectStorageProperties.FormField.Policy.toString(), B64.standard.encString(policy))

        try {
            UploadPolicyChecker.checkPolicy(formFields)
            Assert.fail('Expected a failure here, should not have passed with the given policy: ' + policy)
        } catch(Exception e) {
            //Good.
        }
    }

}
