/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

@Ignore("Manual development test")
public class ObjectTest {

    static ObjectStorageService bukkit;

    @Test
    public void testObject() throws Exception {

        String bucketName = "halo" + Hashes.getRandom(6);
        String objectName = "key" + Hashes.getRandom(6);
        String userId = "admin";

        CreateBucketType createBucketRequest = new CreateBucketType(bucketName);
        createBucketRequest.setBucket(bucketName);
        createBucketRequest.setUserId(userId);
        createBucketRequest.setEffectiveUserId("eucalyptus");
        AccessControlList acl = new AccessControlList();
        createBucketRequest.setAccessControlList(acl);
        CreateBucketResponseType reply = bukkit.createBucket(createBucketRequest);
        System.out.println(reply);

        PutObjectType putObjectRequest = new PutObjectType();
        putObjectRequest.setBucket(bucketName);
        putObjectRequest.setKey(objectName);
        String data = "hi here is some data";
        putObjectRequest.setContentLength(String.valueOf(data.length()));
        ChannelBufferStreamingInputStream cbsis = new ChannelBufferStreamingInputStream(ChannelBuffers.copiedBuffer(data.getBytes()));
        putObjectRequest.setData(cbsis);
        putObjectRequest.setUserId(userId);
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        MetaDataEntry metaDataEntry = new MetaDataEntry();
        metaDataEntry.setName("mammalType");
        metaDataEntry.setValue("walrus");
        metaData.add(metaDataEntry);
        putObjectRequest.setMetaData(metaData);
        //PutObjectResponseType putObjectReply = bukkit.putObject(putObjectRequest);
        //System.out.println(putObjectReply);

        ListBucketType listBucketRequest = new ListBucketType();
        listBucketRequest.setBucket(bucketName);
        listBucketRequest.setUserId(userId);
        ListBucketResponseType listBucketReply = bukkit.listBucket(listBucketRequest);
        System.out.println(listBucketReply);

        GetObjectAccessControlPolicyType acpRequest = new GetObjectAccessControlPolicyType();
        acpRequest.setBucket(bucketName);
        acpRequest.setKey(objectName);
        acpRequest.setUserId(userId);
        GetObjectAccessControlPolicyResponseType acpResponse = bukkit.getObjectAccessControlPolicy(acpRequest);
        System.out.println(acpResponse);

        GetObjectType getObjectRequest = new GetObjectType();
        getObjectRequest.setUserId(userId);
        getObjectRequest.setBucket(bucketName);
        getObjectRequest.setKey(objectName);
        //getObjectRequest.setGetData(true);
        getObjectRequest.setGetMetaData(true);
        getObjectRequest.setInlineData(true);
        GetObjectResponseType getObjectReply = bukkit.getObject(getObjectRequest);
        System.out.println(getObjectReply);

        DeleteObjectType deleteObjectRequest = new DeleteObjectType();
        deleteObjectRequest.setBucket(bucketName);
        deleteObjectRequest.setKey(objectName);
        deleteObjectRequest.setUserId(userId);
        DeleteObjectResponseType deleteObjectReply = bukkit.deleteObject(deleteObjectRequest);
        System.out.println(deleteObjectReply);

        DeleteBucketType deleteBucketRequest = new DeleteBucketType();
        deleteBucketRequest.setUserId(userId);
        deleteBucketRequest.setBucket(bucketName);
        DeleteBucketResponseType deleteResponse = bukkit.deleteBucket(deleteBucketRequest);
        System.out.println(deleteResponse);
    }

    @BeforeClass
    public static void setUp() {
        bukkit = new ObjectStorageGateway();
    }
}
