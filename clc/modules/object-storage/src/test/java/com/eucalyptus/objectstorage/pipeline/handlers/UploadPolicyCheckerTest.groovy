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
