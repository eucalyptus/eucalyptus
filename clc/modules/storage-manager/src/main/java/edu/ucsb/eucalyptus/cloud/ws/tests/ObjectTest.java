/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws.tests;

import edu.ucsb.eucalyptus.cloud.ws.Bukkit;
import edu.ucsb.eucalyptus.msgs.*;
import junit.framework.TestCase;

import java.util.ArrayList;

import com.eucalyptus.auth.Hashes;

public class ObjectTest extends TestCase {

	static Bukkit bukkit;
	public void testObject() throws Throwable {

		String bucketName = "halo" + Hashes.getRandom(6);
		String objectName = "key" + Hashes.getRandom(6);
		String userId = "admin";

		CreateBucketType createBucketRequest = new CreateBucketType(bucketName);
		createBucketRequest.setBucket(bucketName);
		createBucketRequest.setUserId(userId);
        createBucketRequest.setEffectiveUserId("eucalyptus");
		AccessControlListType acl = new AccessControlListType();
		createBucketRequest.setAccessControlList(acl);
		CreateBucketResponseType reply = bukkit.CreateBucket(createBucketRequest);
		System.out.println(reply);

		PutObjectInlineType putObjectRequest = new PutObjectInlineType();
		putObjectRequest.setBucket(bucketName);
		putObjectRequest.setKey(objectName);
        String data = "hi here is some data";
		putObjectRequest.setContentLength(String.valueOf(data.length()));
		putObjectRequest.setBase64Data(data);
		putObjectRequest.setUserId(userId);
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        MetaDataEntry metaDataEntry = new MetaDataEntry();
        metaDataEntry.setName("mammalType");
        metaDataEntry.setValue("walrus");
        metaData.add(metaDataEntry);
        putObjectRequest.setMetaData(metaData);
        PutObjectInlineResponseType putObjectReply = bukkit.PutObjectInline(putObjectRequest);
		System.out.println(putObjectReply);

        ListBucketType listBucketRequest = new ListBucketType();
        listBucketRequest.setBucket(bucketName);
        listBucketRequest.setUserId(userId);
        ListBucketResponseType listBucketReply = bukkit.ListBucket(listBucketRequest);
        System.out.println(listBucketReply);        

        GetObjectAccessControlPolicyType acpRequest = new GetObjectAccessControlPolicyType();
		acpRequest.setBucket(bucketName);
        acpRequest.setKey(objectName);
        acpRequest.setUserId(userId);
		GetObjectAccessControlPolicyResponseType acpResponse = bukkit.GetObjectAccessControlPolicy(acpRequest);
		System.out.println(acpResponse);

        GetObjectType getObjectRequest = new GetObjectType();
        getObjectRequest.setUserId(userId);
        getObjectRequest.setBucket(bucketName);
        getObjectRequest.setKey(objectName);
        getObjectRequest.setGetData(true);
        getObjectRequest.setGetMetaData(true);
        getObjectRequest.setInlineData(true);
        GetObjectResponseType getObjectReply = bukkit.GetObject(getObjectRequest);
        System.out.println(getObjectReply);

        DeleteObjectType deleteObjectRequest = new DeleteObjectType();
		deleteObjectRequest.setBucket(bucketName);
		deleteObjectRequest.setKey(objectName);
		deleteObjectRequest.setUserId(userId);
		DeleteObjectResponseType deleteObjectReply = bukkit.DeleteObject(deleteObjectRequest);
		System.out.println(deleteObjectReply);

		DeleteBucketType deleteBucketRequest = new DeleteBucketType();
		deleteBucketRequest.setUserId(userId);
		deleteBucketRequest.setBucket(bucketName);
		DeleteBucketResponseType deleteResponse = bukkit.DeleteBucket(deleteBucketRequest);
		System.out.println(deleteResponse);
	}

    public void setUp() {
        bukkit = new Bukkit();
   }        
}