/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.providers

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.apache.commons.codec.digest.DigestUtils
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

import com.eucalyptus.objectstorage.UnitTestSupport
import com.eucalyptus.objectstorage.entities.S3ProviderConfiguration
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchUploadException
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType
import com.eucalyptus.objectstorage.msgs.CreateBucketType
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType
import com.eucalyptus.objectstorage.msgs.DeleteBucketType
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType
import com.eucalyptus.objectstorage.msgs.DeleteObjectType
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType
import com.eucalyptus.objectstorage.msgs.GetObjectType
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType
import com.eucalyptus.objectstorage.msgs.HeadBucketType
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType
import com.eucalyptus.objectstorage.msgs.HeadObjectType
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType
import com.eucalyptus.objectstorage.msgs.ListBucketType
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsResponseType
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsType
import com.eucalyptus.objectstorage.msgs.ListPartsResponseType
import com.eucalyptus.objectstorage.msgs.ListPartsType
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType
import com.eucalyptus.objectstorage.msgs.PutObjectType
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType
import com.eucalyptus.objectstorage.msgs.UploadPartType
import com.eucalyptus.objectstorage.providers.s3.S3ProviderClient
import com.eucalyptus.objectstorage.providers.walrus.WalrusProviderClient
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.BucketListEntry
import com.eucalyptus.storage.msgs.s3.ListEntry
import com.eucalyptus.storage.msgs.s3.Part
import com.eucalyptus.util.EucalyptusCloudException
import com.google.common.base.Strings

/**
 * Intended for testing the InMemoryProvider. Will need
 * additional work to support real-backends that use persistent storage
 */
@CompileStatic
class ObjectStorageProviderClientTest {
  static ObjectStorageProviderClient provider
  static String configValue
  static final List<String> TEST_BUCKET_NAMES = [
    'bucket1',
    'bucket2',
    'bucket3',
    'bucket4'
  ]
  static final String TEST_ACCESS_KEY = 'testaccesskey123'


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UnitTestSupport.setupOsgPersistenceContext()
    UnitTestSupport.setupAuthPersistenceContext()
    UnitTestSupport.initializeAuth(2, 2)
    configValue = System.getProperty("provider", "mem")
    println 'Using provider ' + configValue

    S3ProviderConfiguration.getS3ProviderConfiguration().S3AccessKey = System.getProperty("accessKey")
    S3ProviderConfiguration.getS3ProviderConfiguration().S3SecretKey = System.getProperty("secretKey")
    S3ProviderConfiguration.getS3ProviderConfiguration().S3Endpoint = System.getProperty("endpoint")

    switch (configValue) {
      case 's3':
        assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.getS3ProviderConfiguration().S3Endpoint))
        assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.getS3ProviderConfiguration().S3AccessKey))
        assert (!Strings.isNullOrEmpty(S3ProviderConfiguration.getS3ProviderConfiguration().S3SecretKey))
        println 'Using endpoint ' + S3ProviderConfiguration.getS3ProviderConfiguration().S3Endpoint

        provider = new S3ProviderClient()
        break
      case 'walrus':
        provider = new WalrusProviderClient();
        break
      case 'mem':
        provider = new InMemoryProvider()
        break
      default:
        throw new RuntimeException('Unknown provider specified: ' + configValue)
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    if (provider != null) provider.stop()
  }

  @Before
  public void setUp() throws Exception {
    if ("mem".equals(configValue)) {
      provider = new InMemoryProvider()
    } else {
      provider = ObjectStorageProviders.getInstance(configValue)
    }
  }

  //Delete all buckets and objects
  static flushResources() {
    //TODO: this is basically a test-case for listBuckets, listObjects, actuallyDeleteObject, deleteBucket

    ListAllMyBucketsType listRequest = new ListAllMyBucketsType()
    listRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    ListAllMyBucketsResponseType bucketListing = provider.listAllMyBuckets(listRequest)

    ListBucketType listReq = new ListBucketType()
    listReq.setEffectiveUserId(TEST_ACCESS_KEY)
    ListBucketResponseType objListing = null
    DeleteObjectType delObjReq = new DeleteObjectType();
    delObjReq.setEffectiveUserId(TEST_ACCESS_KEY)

    DeleteObjectResponseType delObjResp = null
    DeleteBucketType delBucketReq = null
    delBucketReq = new DeleteBucketType()
    delBucketReq.setEffectiveUserId(TEST_ACCESS_KEY)

    bucketListing.getBucketList().getBuckets().each { b ->
      BucketListEntry bucket = (BucketListEntry) b
      listReq.setBucket(bucket.getName())
      println 'Flushing bucket ' + bucket.getName()
      objListing = provider.listBucket(listReq)

      objListing.getContents().each { key ->
        ListEntry content = (ListEntry) key
        println 'Flushing key ' + content.getKey()
        delObjReq.setBucket(bucket.getName())
        delObjReq.setKey(content.getKey())
        provider.deleteObject(delObjReq)
      }
      objListing = null

      delBucketReq.setBucket(bucket.getName())
      provider.deleteBucket(delBucketReq)
    }

  }

  @After
  public void tearDown() throws Exception {
    flushResources()
  }

  static void populateBuckets(List<String> bucketNames, String accessKey) {
    bucketNames.each { String i ->
      def req = new CreateBucketType((String)i)
      req.setEffectiveUserId(accessKey)
      provider.createBucket(req)
      println 'Created bucket for testing: ' + i
    }
  }

  static PutObjectResponseType putObject(String bucket, String key, String content) {
    //add some objects.
    PutObjectType putObj = new PutObjectType()
    putObj.setKey(key)
    putObj.setBucket(bucket)
    putObj.setEffectiveUserId(TEST_ACCESS_KEY)
    putObj.setContentLength(content.getBytes().length.toString())

    //For now, skip the acl stuff. Our providers set it to 'private' anyway
    return provider.putObject(putObj, new ByteArrayInputStream(content.getBytes()))
  }

  static HeadObjectResponseType headObject(String bucket, String key) {
    HeadObjectType headObjRequest = new HeadObjectType()
    headObjRequest.setBucket(bucket)
    headObjRequest.setKey(key)
    headObjRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.headObject(headObjRequest)
  }

  static HeadBucketResponseType headBucket(String bucket) {
    HeadBucketType headRequest = new HeadBucketType()
    headRequest.setBucket(bucket)
    headRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.headBucket(headRequest)
  }

  static DeleteBucketResponseType deleteBucket(String bucket) {
    DeleteBucketType delBucketReq = null
    delBucketReq = new DeleteBucketType()
    delBucketReq.setBucket(bucket)
    delBucketReq.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.deleteBucket(delBucketReq)
  }

  static DeleteObjectResponseType deleteObject(String bucket, String key) {
    DeleteObjectType delReq = new DeleteObjectType()
    delReq = new DeleteObjectType()
    delReq.setBucket(bucket)
    delReq.setKey(key)
    delReq.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.deleteObject(delReq)
  }

  static ListPartsResponseType listParts(String bucket, String key, String uploadId) {
    ListPartsType initRequest = new ListPartsType()
    initRequest.setBucket(bucket)
    initRequest.setKey(key)
    initRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    initRequest.setUploadId(uploadId)
    return provider.listParts(initRequest)
  }

  static ListMultipartUploadsResponseType listUploads(String bucket) {
    ListMultipartUploadsType initRequest = new ListMultipartUploadsType()
    initRequest.setBucket(bucket)
    initRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.listMultipartUploads(initRequest)
  }

  static InitiateMultipartUploadResponseType initMPU(String bucket, String key) {
    InitiateMultipartUploadType initRequest = new InitiateMultipartUploadType()
    initRequest.setBucket(bucket)
    initRequest.setKey(key)
    initRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.initiateMultipartUpload(initRequest)
  }

  static AbortMultipartUploadResponseType abortMPU(String bucket, String key, String uploadId) {
    AbortMultipartUploadType abortRequest = new AbortMultipartUploadType()
    abortRequest.setBucket(bucket)
    abortRequest.setKey(key)
    abortRequest.setUploadId(uploadId)
    abortRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.abortMultipartUpload(abortRequest)
  }

  static CompleteMultipartUploadResponseType completeMPU(String bucket, String key, String uploadId, ArrayList<Part> parts) {
    CompleteMultipartUploadType completeRequest = new CompleteMultipartUploadType()
    completeRequest.setBucket(bucket)
    completeRequest.setKey(key)
    completeRequest.setUploadId(uploadId)
    completeRequest.setParts(parts)
    completeRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    return provider.completeMultipartUpload(completeRequest);
  }

  static UploadPartResponseType uploadPart(String bucket, String key, String uploadId, int partNumber, String content) {
    //add some objects.
    UploadPartType upload = new UploadPartType()
    upload.setKey(key)
    upload.setBucket(bucket)
    upload.setUploadId(uploadId)
    upload.setPartNumber(partNumber.toString())
    upload.setEffectiveUserId(TEST_ACCESS_KEY)
    upload.setContentLength(content.getBytes().length.toString())

    //For now, skip the acl stuff. Our providers set it to 'private' anyway
    return provider.uploadPart(upload, new ByteArrayInputStream(content.getBytes()))
  }


  static boolean objectExists(String bucket, String key) {
    try {
      return headObject(bucket, key).getEtag() != null
    } catch (EucalyptusCloudException e) {
      return false
    }
  }

  static boolean bucketExists(String bucket) {
    try {
      return headBucket(bucket).getBucket().equals(bucket)
    } catch (EucalyptusCloudException e) {
      return false
    }
  }

  @Test
  public void testListAllMyBuckets() throws Exception {
    populateBuckets(TEST_BUCKET_NAMES, TEST_ACCESS_KEY)

    ListAllMyBucketsType listRequest = new ListAllMyBucketsType()
    listRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    ListAllMyBucketsResponseType bucketListing = provider.listAllMyBuckets(listRequest)

    assert (bucketListing != null)
    assert (bucketListing.getOwner().getID() == TEST_ACCESS_KEY)
    assert (bucketListing.getBucketList() != null && bucketListing.getBucketList().getBuckets() != null &&
    bucketListing.getBucketList().getBuckets().size() == TEST_BUCKET_NAMES.size())

    //construct in order
    def returnedNames = bucketListing.getBucketList().getBuckets().collect { b ->
      ((BucketListEntry)b).getName()
    }
    assert (returnedNames.equals(TEST_BUCKET_NAMES))
  }

  @Test
  public void testHeadBucket() throws Exception {
    def headBucket = 'headtestbucket'
    populateBuckets([headBucket], TEST_ACCESS_KEY)

    HeadBucketType headRequest = new HeadBucketType()
    headRequest.setBucket(headBucket)
    headRequest.setEffectiveUserId(TEST_ACCESS_KEY)

    HeadBucketResponseType response = provider.headBucket(headRequest)
    assert (response != null)
    assert (response.getBucket() == headBucket)

    headRequest.setBucket('non-existent-bucket')
    try {
      response = provider.headBucket(headRequest)
      fail('HEAD request on non-existent bucket should throw exception')
    } catch (EucalyptusCloudException e) {
      println 'Caught exception as expected: ' + e.getMessage()
    }
  }

  @Test
  public void testCreateBucket() throws Exception {
    def createBucket = 'create-testbucket'
    populateBuckets([createBucket], TEST_ACCESS_KEY)

    assert (bucketExists(createBucket))
  }

  @Test
  public void testDeleteBucket() throws Exception {
    def deleteBucket = 'delete-testbucket'
    populateBuckets([deleteBucket], TEST_ACCESS_KEY)

    assert (bucketExists(deleteBucket))

    DeleteBucketType deleteRequest = new DeleteBucketType()
    deleteRequest.setBucket(deleteBucket)
    deleteRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    DeleteBucketResponseType deleteResponse = provider.deleteBucket(deleteRequest)
    assert (deleteResponse != null)
    assert (deleteResponse.getStatus() == HttpResponseStatus.NO_CONTENT)

    //test on delete for already gone bucket
    deleteResponse = provider.deleteBucket(deleteRequest)
    assert (deleteResponse != null)
    assert (deleteResponse.getStatus() == HttpResponseStatus.NO_CONTENT)
  }

  @Test
  public void testGetBucketAccessControlPolicy() throws Exception {
    def bucket = 'getacp-testbucket'
    populateBuckets([bucket], TEST_ACCESS_KEY)

    GetBucketAccessControlPolicyType getAclRequest = new GetBucketAccessControlPolicyType()
    getAclRequest.setBucket(bucket)
    getAclRequest.setEffectiveUserId(TEST_ACCESS_KEY)

    GetBucketAccessControlPolicyResponseType response = provider.getBucketAccessControlPolicy(getAclRequest)
    assert (response != null)
    assert (response.getAccessControlPolicy().getOwner().getID() != null)
    assert (response.getAccessControlPolicy().getAccessControlList().getGrants().size() == 1)
    assert (response.getAccessControlPolicy().getAccessControlList().getGrants().get(0).getGrantee().getCanonicalUser().getID().equals(response.getAccessControlPolicy().getOwner().getID()))
    assert (response.getAccessControlPolicy().getAccessControlList().getGrants().get(0).getPermission().equals(ObjectStorageProperties.Permission.FULL_CONTROL.toString()))
  }

  @Test
  public void testListBucket() throws Exception {
    def bucket = 'list-testbucket'
    populateBuckets([bucket], TEST_ACCESS_KEY)


    ListBucketType listReq = new ListBucketType()
    listReq.setEffectiveUserId(TEST_ACCESS_KEY)
    listReq.setBucket(bucket)
    ListBucketResponseType objListing = null

    objListing = provider.listBucket(listReq)
    assert (objListing != null)
    assert (objListing.getName().equals(bucket))
    assert (objListing.getContents().size() == 0)

    //Put some objs
    def testContent = 'testingcontent123...blahblahblah'
    def contentLength = testContent.getBytes().length

    putObject(bucket, 'obj0', testContent)
    putObject(bucket, 'obj1', testContent)
    putObject(bucket, 'obj2', testContent)
    putObject(bucket, 'obj3', testContent)
    putObject(bucket, 'obj4', testContent)

    //Get listing again
    objListing = provider.listBucket(listReq)
    assert (objListing != null)
    assert (objListing.getName().equals(bucket))
    assert (objListing.getContents().size() == 5)

    assert (objListing.getContents().get(0).getKey().equals('obj0'))
    assert (objListing.getContents().get(0).getSize() == contentLength)

    assert (objListing.getContents().get(1).getKey().equals('obj1'))
    assert (objListing.getContents().get(1).getEtag() != null)
    assert (objListing.getContents().get(1).getSize() == contentLength)

    assert (objListing.getContents().get(2).getKey().equals('obj2'))
    assert (objListing.getContents().get(2).getEtag() != null)
    assert (objListing.getContents().get(2).getSize() == contentLength)


    assert (objListing.getContents().get(3).getKey().equals('obj3'))
    assert (objListing.getContents().get(3).getEtag() != null)
    assert (objListing.getContents().get(3).getSize() == contentLength)

    assert (objListing.getContents().get(4).getKey().equals('obj4'))
    assert (objListing.getContents().get(4).getEtag() != null)
    assert (objListing.getContents().get(4).getSize() == contentLength)
  }

  @Test
  public void testPutGetObject() throws Exception {
    def bucket = 'putobj-testbucket'
    populateBuckets([bucket], TEST_ACCESS_KEY)

    assert (bucketExists(bucket))

    def testContent = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea'
    putObject(bucket, 'obj0', testContent)

    def md5 = DigestUtils.md5Hex(testContent)
    assert (objectExists(bucket, 'obj0'))
    GetObjectType getRequest = new GetObjectType()
    getRequest.setBucket(bucket)
    getRequest.setKey('obj0')
    getRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    GetObjectResponseType getResponse = provider.getObject(getRequest)

    byte[] buffer = new byte[testContent.getBytes().length]
    getResponse.getDataInputStream().read(buffer)
    assert (buffer == testContent.getBytes())
    assert (getResponse.getEtag().equals(md5))

  }

  @Test
  public void testDeleteObject() throws Exception {
    def bucket = 'deleteobj-testbucket'
    populateBuckets([bucket], TEST_ACCESS_KEY)

    assert (bucketExists(bucket))

    def testContent = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea'
    putObject(bucket, 'obj0', testContent)

    assert (objectExists(bucket, 'obj0'))
    deleteObject(bucket, 'obj0')
    assert (!objectExists(bucket, 'obj0'))
  }

  @Ignore
  @Test
  public void testGetObjectExtended() throws Exception {
    fail('not yet implemented')
  }

  @Test
  public void testHeadObject() throws Exception {
    def headObjBucket = 'headobj-testbucket'
    populateBuckets([headObjBucket], TEST_ACCESS_KEY)

    HeadBucketType headRequest = new HeadBucketType()
    headRequest.setBucket(headObjBucket)
    headRequest.setEffectiveUserId(TEST_ACCESS_KEY)

    HeadBucketResponseType response = provider.headBucket(headRequest)
    assert (response != null)
    assert (response.getBucket() == headObjBucket)

    def testContent = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea'
    putObject(headObjBucket, 'obj0', testContent)

    HeadObjectType headObjRequest = new HeadObjectType()
    headObjRequest.setBucket(headObjBucket)
    headObjRequest.setKey('obj0')
    headObjRequest.setEffectiveUserId(TEST_ACCESS_KEY)

    HeadObjectResponseType objResponse = provider.headObject(headObjRequest)
    assert (objResponse != null)
    assert (objResponse.getSize() == testContent.getBytes().length)
    assert (objResponse.getEtag() != null)

    try {
      headObjRequest.setKey('nonexistentkey')
      objResponse = provider.headObject(headObjRequest)
      fail('Should have thrown exception on HEAD of fake object: ' + objResponse)
    } catch (EucalyptusCloudException e) {
      println 'Correctly caught exception on HEAD of non-existent object ' + e

    }
  }

  @Test
  public void testMultipartUploadWithComplete() throws Exception {
    def bucket = 'mpu-testbucket'
    def mpuKey = 'mputestkey'
    populateBuckets([bucket], TEST_ACCESS_KEY)

    assert (bucketExists(bucket))

    //Initiate
    InitiateMultipartUploadResponseType response = initMPU(bucket, mpuKey)
    assert (response != null && response.getUploadId() != null)
    def uploadId = response.getUploadId()

    //List uploads
    ListMultipartUploadsResponseType listUploadsResponse = listUploads(bucket)
    assert (listUploadsResponse.getBucket() == bucket)
    assert (listUploadsResponse.getUploads().size() == 1)
    assert (listUploadsResponse.getUploads().get(0).getKey() == mpuKey)
    assert (listUploadsResponse.getUploads().get(0).getUploadId() == uploadId)

    //Upload parts
    def testContent1 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea11111111111111'
    def testContent2 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea22222222222222'
    def testContent3 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea33333333333333'
    def part1Response = uploadPart(bucket, mpuKey, uploadId, 1, testContent1)
    assert (part1Response != null && part1Response.getEtag() != null)

    def part2Response = uploadPart(bucket, mpuKey, uploadId, 2, testContent2)
    assert (part2Response != null && part2Response.getEtag() != null)

    def part3Response = uploadPart(bucket, mpuKey, uploadId, 3, testContent3)
    assert (part3Response != null && part3Response.getEtag() != null)

    def part4Response = uploadPart(bucket, mpuKey, uploadId, 4, testContent1)
    assert (part4Response != null && part4Response.getEtag() != null)

    def part5Response = uploadPart(bucket, mpuKey, uploadId, 5, testContent2)
    assert (part5Response != null && part5Response.getEtag() != null)


    List<Part> parts = new ArrayList<Part>(5);
    parts.add(new Part(1, part1Response.getEtag()))
    parts.add(new Part(2, part2Response.getEtag()))
    parts.add(new Part(3, part3Response.getEtag()))
    parts.add(new Part(4, part4Response.getEtag()))
    parts.add(new Part(5, part5Response.getEtag()))

    //List parts
    ListPartsResponseType listResponse = listParts(bucket, mpuKey, uploadId)
    assert (listResponse != null)
    assert (listResponse.getBucket() == bucket)
    assert (listResponse.getKey() == mpuKey)
    assert (listResponse.getUploadId() == uploadId)
    assert (listResponse.getParts().size() == 5)
    assert (listResponse.getParts().get(0).getPartNumber() == 1)
    assert (listResponse.getParts().get(0).getEtag() == part1Response.getEtag())

    assert (listResponse.getParts().get(1).getPartNumber() == 2)
    assert (listResponse.getParts().get(1).getEtag() == part2Response.getEtag())

    assert (listResponse.getParts().get(2).getPartNumber() == 3)
    assert (listResponse.getParts().get(2).getEtag() == part3Response.getEtag())

    assert (listResponse.getParts().get(3).getPartNumber() == 4)
    assert (listResponse.getParts().get(3).getEtag() == part4Response.getEtag())

    assert (listResponse.getParts().get(4).getPartNumber() == 5)
    assert (listResponse.getParts().get(4).getEtag() == part5Response.getEtag())

    def fullContent = testContent1 + testContent2 + testContent3 + testContent1 + testContent2
    def md5 = DigestUtils.md5Hex(fullContent)
    assert (!objectExists(bucket, mpuKey))

    //complete
    def completionResponse = completeMPU(bucket, mpuKey, uploadId, parts)
    assert (completionResponse != null)
    assert (completionResponse.getEtag() == md5)
    assert (completionResponse.getKey() == mpuKey)

    assert (objectExists(bucket, mpuKey))

    GetObjectType getRequest = new GetObjectType()
    getRequest.setBucket(bucket)
    getRequest.setKey(mpuKey)
    getRequest.setEffectiveUserId(TEST_ACCESS_KEY)
    GetObjectResponseType getResponse = provider.getObject(getRequest)

    byte[] buffer = new byte[fullContent.length()]
    getResponse.getDataInputStream().read(buffer)
    assert (buffer == fullContent.getBytes())
    assert (getResponse.getEtag().equals(md5))

    //Comfirm upload is gone
    listUploadsResponse = listUploads(bucket)
    assert (listUploadsResponse.getBucket() == bucket)
    assert (listUploadsResponse.getUploads().size() == 0)

    //Confirm parts are gone
    try {
      listResponse = listParts(bucket, mpuKey, uploadId)
      fail('Should have gotten no-upload exception for upload id ' + uploadId)
    } catch (NoSuchUploadException e) {
      //correct.
    }
  }

  @Test
  public void testMultipartUploadWithAbort() throws Exception {
    def bucket = 'mpu-testbucket'
    def mpuKey = 'mputestkey'
    populateBuckets([bucket], TEST_ACCESS_KEY)

    assert (bucketExists(bucket))

    //Initiate
    InitiateMultipartUploadResponseType response = initMPU(bucket, mpuKey)
    assert (response != null && response.getUploadId() != null)
    def uploadId = response.getUploadId()

    //List uploads
    ListMultipartUploadsResponseType listUploadsResponse = listUploads(bucket)
    assert (listUploadsResponse.getBucket() == bucket)
    assert (listUploadsResponse.getUploads().size() == 1)
    assert (listUploadsResponse.getUploads().get(0).getKey() == mpuKey)
    assert (listUploadsResponse.getUploads().get(0).getUploadId() == uploadId)

    //Upload parts
    def testContent1 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea11111111111111'
    def testContent2 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea22222222222222'
    def testContent3 = 'FakeObjectContenthere...adfasdgoaiganoge awogia goias gojafasdfjawea33333333333333'
    def part1Response = uploadPart(bucket, mpuKey, uploadId, 1, testContent1)
    assert (part1Response != null && part1Response.getEtag() != null)

    def part2Response = uploadPart(bucket, mpuKey, uploadId, 2, testContent2)
    assert (part2Response != null && part2Response.getEtag() != null)

    def part3Response = uploadPart(bucket, mpuKey, uploadId, 3, testContent3)
    assert (part3Response != null && part3Response.getEtag() != null)

    def part4Response = uploadPart(bucket, mpuKey, uploadId, 4, testContent1)
    assert (part4Response != null && part4Response.getEtag() != null)

    def part5Response = uploadPart(bucket, mpuKey, uploadId, 5, testContent2)
    assert (part5Response != null && part5Response.getEtag() != null)


    List<Part> parts = new ArrayList<Part>(5);
    parts.add(new Part(1, part1Response.getEtag()))
    parts.add(new Part(2, part2Response.getEtag()))
    parts.add(new Part(3, part3Response.getEtag()))
    parts.add(new Part(4, part4Response.getEtag()))
    parts.add(new Part(5, part5Response.getEtag()))

    //List parts
    ListPartsResponseType listResponse = listParts(bucket, mpuKey, uploadId)
    assert (listResponse != null)
    assert (listResponse.getBucket() == bucket)
    assert (listResponse.getKey() == mpuKey)
    assert (listResponse.getUploadId() == uploadId)
    assert (listResponse.getParts().size() == 5)
    assert (listResponse.getParts().get(0).getPartNumber() == 1)
    assert (listResponse.getParts().get(0).getEtag() == part1Response.getEtag())

    assert (listResponse.getParts().get(1).getPartNumber() == 2)
    assert (listResponse.getParts().get(1).getEtag() == part2Response.getEtag())

    assert (listResponse.getParts().get(2).getPartNumber() == 3)
    assert (listResponse.getParts().get(2).getEtag() == part3Response.getEtag())

    assert (listResponse.getParts().get(3).getPartNumber() == 4)
    assert (listResponse.getParts().get(3).getEtag() == part4Response.getEtag())

    assert (listResponse.getParts().get(4).getPartNumber() == 5)
    assert (listResponse.getParts().get(4).getEtag() == part5Response.getEtag())

    assert (!objectExists(bucket, mpuKey))

    //complete
    def abortResponse = abortMPU(bucket, mpuKey, uploadId)
    assert (abortResponse != null)

    assert (!objectExists(bucket, mpuKey))

    //Confirm upload is gone
    listUploadsResponse = listUploads(bucket)
    assert (listUploadsResponse.getBucket() == bucket)
    assert (listUploadsResponse.getUploads().size() == 0)

    //Confirm parts are gone
    try {
      listResponse = listParts(bucket, mpuKey, uploadId)
      fail('Should have gotten no-upload exception for upload id ' + uploadId)
    } catch (NoSuchUploadException e) {
      //correct.
    }
  }
}
