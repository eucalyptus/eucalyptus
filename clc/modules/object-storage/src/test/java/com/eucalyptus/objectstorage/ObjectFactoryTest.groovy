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

package com.eucalyptus.objectstorage

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.principal.User
import com.eucalyptus.auth.principal.UserPrincipal
import com.eucalyptus.objectstorage.entities.Bucket
import com.eucalyptus.objectstorage.entities.ObjectEntity
import com.eucalyptus.objectstorage.entities.PartEntity
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException
import com.eucalyptus.objectstorage.msgs.CopyObjectType
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType
import com.eucalyptus.objectstorage.msgs.GetObjectType
import com.eucalyptus.objectstorage.providers.InMemoryProvider
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient
import com.eucalyptus.objectstorage.util.AclUtils
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.s3.AccessControlList
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.MetaDataEntry
import com.eucalyptus.storage.msgs.s3.Part
import com.eucalyptus.util.EucalyptusCloudException
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.fail

@CompileStatic
public class ObjectFactoryTest {
  private static final Logger LOG = Logger.getLogger(ObjectFactoryTest.class);
  static ObjectStorageProviderClient provider = new InMemoryProvider()

  @BeforeClass
  public static void beforeClass() throws Exception {
    UnitTestSupport.setupAuthPersistenceContext()
    UnitTestSupport.setupOsgPersistenceContext()
    UnitTestSupport.initializeAuth(2, 2)
  }

  @AfterClass
  public static void afterClass() throws Exception {
    UnitTestSupport.tearDownOsgPersistenceContext()
    UnitTestSupport.tearDownAuthPersistenceContext()
  }

  @Before
  public void setUp() throws Exception {
    provider.start()
    UnitTestSupport.flushObjects()
    UnitTestSupport.flushBuckets()
  }

  @After
  public void tearDown() throws Exception {
    provider.stop()
    UnitTestSupport.flushObjects()
    UnitTestSupport.flushBuckets()
  }

  /**
   * Expect creation to complete and result in 'extant' object
   * @throws Exception
   */
  @Test
  public void testCreateObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()
    def key = 'testkey'

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, content.length, user)
    objectEntity.setAcl(acp)
    ArrayList<MetaDataEntry> metadata = new ArrayList<>();
    metadata.add(new MetaDataEntry("key1", "value1"))
    metadata.add(new MetaDataEntry("key2", "value2"))
    metadata.add(new MetaDataEntry("key3", "value3"))

    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), metadata, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.extant))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
    assert (fetched.geteTag().equals(resultEntity.geteTag()))

    GetObjectType request = new GetObjectType()
    request.setUser(user)
    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    GetObjectResponseType response = provider.getObject(request)
    assert (response.getEtag().equals(resultEntity.geteTag()))
    assert (response.getSize().equals(resultEntity.getSize()))
    byte[] buffer = new byte[content.length]
    response.getDataInputStream().read(buffer)
    for (int i = 0; i < buffer.size(); i++) {
      assert (buffer[i] == content[i])
    }

    //Check metadata
    assert (response.metaData.size() == metadata.size())
    assert (response.metaData.containsAll(metadata))

  }

  /**
   * Expect creation to complete and result in 'extant' object
   * @throws Exception
   */
  @Test
  public void testCreateObjectOverwrite() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert(bucket != null)
    assert(bucket.getState().equals(BucketState.extant))
    def key = 'testkey'

    for(int j = 0 ; j < 5 ; j++) {
      byte[] content = ('fakecontent123' + String.valueOf(j)).getBytes()
      ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, content.length, user)
      objectEntity.setAcl(acp)
      ArrayList<MetaDataEntry> metadata = new ArrayList<>();
      metadata.add(new MetaDataEntry("key1", "value1"))
      metadata.add(new MetaDataEntry("key2", "value2"))
      metadata.add(new MetaDataEntry("key3", "value3"))
      metadata.add(new MetaDataEntry("itrvalue", String.valueOf(j)))

      ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), metadata, user)

      assert(resultEntity != null)
      assert(resultEntity.getState().equals(ObjectState.extant))
      ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
      assert(fetched.geteTag().equals(resultEntity.geteTag()))

      GetObjectType request = new GetObjectType()
      request.setUser(user)
      request.setKey(resultEntity.getObjectUuid())
      request.setBucket(bucket.getBucketUuid())
      GetObjectResponseType response = provider.getObject(request)
      assert(response.getEtag().equals(resultEntity.geteTag()))
      assert(response.getSize().equals(resultEntity.getSize()))
      byte[] buffer = new byte[content.length]
      response.getDataInputStream().read(buffer)
      for(int i = 0; i < buffer.size(); i++) {
        assert(buffer[i] == content[i])
      }

      //Check metadata
      assert(response.metaData.size() == metadata.size())
      assert(response.metaData.containsAll(metadata))

      //Verify listing
      PaginatedResult<ObjectEntity> objs = ObjectMetadataManagers.getInstance().listPaginated(bucket, 1000, null, null, null)
      assert(objs != null && objs.getEntityList().size() == 1)
      assert(objs != null && objs.getEntityList().get(0).getObjectUuid() == resultEntity.getObjectUuid())
    }
  }



  /**
   * Expect creation to complete and result in 'extant' object
   * 2nd creation should overwrite the first, lookup should return
   * the 2nd object.
   * @throws Exception
   */
  @Test
  public void testCreateDuplicateObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()
    def key = 'testkey'

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, content.length, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), null, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.extant))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
    assert (fetched.geteTag().equals(resultEntity.geteTag()))

    GetObjectType request = new GetObjectType()
    request.setUser(user)
    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    GetObjectResponseType response = provider.getObject(request)
    assert (response.getEtag().equals(resultEntity.geteTag()))
    assert (response.getSize().equals(resultEntity.getSize()))
    byte[] buffer = new byte[content.length]
    response.getDataInputStream().read(buffer)
    for (int i = 0; i < buffer.size(); i++) {
      assert (buffer[i] == content[i])
    }

    byte[] content2 = 'fakecontent123fakecontent123morecontent'.getBytes()

    ObjectEntity objectEntity2 = ObjectEntity.newInitializedForCreate(bucket, key, content2.length, user)
    objectEntity2.setAcl(acp)
    ObjectEntity resultEntity2 = OsgObjectFactory.getFactory().createObject(provider, objectEntity2, new ByteArrayInputStream(content2), null, user)

    assert (resultEntity2 != null)
    assert (resultEntity2.getState().equals(ObjectState.extant))
    ObjectEntity fetched2 = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
    assert (fetched2.geteTag().equals(resultEntity2.geteTag()))
    assert (!fetched2.geteTag().equals(fetched.geteTag()))
    assert (fetched2.getObjectUuid().equals(resultEntity2.getObjectUuid()))
    assert (!fetched2.getObjectUuid().equals(resultEntity.getObjectUuid()))
  }

  /**
   * Expect deletion to fully remove the object on the backend and
   * in metadata.
   * @throws Exception
   */
  @Test
  public void testFullDeleteObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, "testket", content.length, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), null, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.extant))

    //Do the delete logically
    OsgObjectFactory.getFactory().logicallyDeleteObject(provider, objectEntity, user)

    try {
      ObjectEntity found = ObjectMetadataManagers.getInstance().lookupObject(bucket, "testkey", null)
      fail('Should have gotten no-key exception on metadata lookup')
    } catch (NoSuchElementException e) {
      //Correctly caught this exception
    }

    //Lookup record directly
    List<ObjectEntity> objs = ObjectMetadataManagers.getInstance().lookupObjectsInState(objectEntity.getBucket(), objectEntity.getObjectKey(), null, ObjectState.deleting)

    //Actually do the delete
    try {
      OsgObjectFactory.getFactory().actuallyDeleteObject(provider, objectEntity, user)
    } catch(NoSuchEntityException e) {
      //Correctly caught. Logical delete will do actual delete unless bucket versioning is enabled.
    }

    GetObjectType request = new GetObjectType()
    request.setUser(user)
    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    try {
      GetObjectResponseType response = provider.getObject(request)
      fail('Should not find key on backend: ' + response.getEtag())
    } catch (EucalyptusCloudException e) {
      //Correctly caught the exception as expected
    }
  }

  /**
   * Expect deletion to fully delete the object regardless of its 'deleting'
   * state
   * @throws Exception
   */
  @Test
  public void testDeleteAlreadyDeletingObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, "testket", content.length, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), null, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.extant))

    //Do the delete
    OsgObjectFactory.getFactory().actuallyDeleteObject(provider, objectEntity, user)

    try {
      ObjectEntity found = ObjectMetadataManagers.getInstance().lookupObject(bucket, "testkey", null)
      fail('Should have gotten no-key exception on metadata lookup')
    } catch (NoSuchElementException e) {
      //Correctly caught this exception
    }

    GetObjectType request = new GetObjectType()
    request.setUser(user)
    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    try {
      GetObjectResponseType response = provider.getObject(request)
      fail('Should not find key on backend')
    } catch (EucalyptusCloudException e) {
      //Correctly caught the exception as expected
    }
  }

  /**
   * Expect deletion to throw an exception for not found object.
   * @throws Exception
   */
  @Test
  public void testDeleteNonExistentObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))

    def objects = TestUtils.createNObjects(ObjectMetadataManagers.getInstance(), 1, bucket, "testkey", 100, user)

    OsgObjectFactory.getFactory().actuallyDeleteObject(provider, objects.first(), user)

    try {
      //Do the delete
      ObjectEntity nonObject = new ObjectEntity().withBucket(bucket).withKey("nokey")
      OsgObjectFactory.getFactory().actuallyDeleteObject(provider, objects.first(), user)
      fail('DeleteObject should throw exception for nonexistent object')
    } catch (Exception e) {
      LOG.info("Caught expection as expected", e)
    }

  }

  /**
   * @throws Exception
   */
  @Test
  public void testMultipartUpload() throws Exception {
    //Set the min part size to 1 to allow small tests
    ObjectStorageProperties.MPU_PART_MIN_SIZE = 1;

    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()
    def key = 'testkey'

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, 0, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createMultipartUpload(provider, objectEntity, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.mpu_pending))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupUpload(bucket, key, resultEntity.getUploadId());
    assert (fetched.getState() == ObjectState.mpu_pending)
    assert (fetched.getSize() == 0)
    assert (fetched.getUploadId() == resultEntity.getUploadId())

    List<Part> partList = new ArrayList<Part>(10);
    List<PartEntity> partEntities = new ArrayList<PartEntity>();

    for (int i = 1; i <= 10; i++) {
      PartEntity tmp = PartEntity.newInitializedForCreate(bucket, key, resultEntity.getUploadId(), i, content.length, user)
      partEntities.add(OsgObjectFactory.getFactory().createObjectPart(provider, fetched, tmp, new ByteArrayInputStream(content), user))

      PaginatedResult<PartEntity> partsList1 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 0, 1000)
      assert (partsList1.getEntityList().size() == i)
      for (int j = 0; j < i; j++) {
        assert (partsList1.getEntityList().get(j).geteTag() == partEntities[j].geteTag())
      }
      partList.add(new Part(i, partEntities.last().geteTag()))
    }

    GetObjectType request = new GetObjectType()
    request.setUser(user)

    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    try {
      GetObjectResponseType response = provider.getObject(request)
      fail("Should have failed to get key on incomplete MPU")
    } catch (NoSuchKeyException e) {
      //correct, not committed
    }

    //Complete the upload
    ObjectEntity finalObject = OsgObjectFactory.getFactory().completeMultipartUpload(provider, resultEntity, partList, user)
    assert (finalObject != null)
    assert (finalObject.geteTag() != null)
    assert (finalObject.getSize() == 10 * content.length)
    assert (finalObject.getState() == ObjectState.extant)

    ObjectEntity foundObj = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
    assert (foundObj.getSize() == finalObject.getSize())
    assert (foundObj.geteTag() == finalObject.geteTag())
    assert (foundObj.getObjectUuid() == finalObject.getObjectUuid())

    GetObjectResponseType response = provider.getObject(request)
    assert (response.getEtag().equals(resultEntity.geteTag()))
    assert (response.getSize().equals(resultEntity.getSize()))
    byte[] buffer = new byte[content.length]
    response.getDataInputStream().read(buffer)
    for (int i = 0; i < buffer.size(); i++) {
      assert (buffer[i] == content[i])
    }
  }

  /**
   * Tests MPU with a part uploaded ontop of itself to test the part-overwrite handling
   */
  @Test
  public void testMultipartUploadPartOverwrite() throws Exception {
    //Set the min part size to 1 to allow small tests
    ObjectStorageProperties.MPU_PART_MIN_SIZE = 1;

    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert(bucket != null)
    assert(bucket.getState().equals(BucketState.extant))
    byte[] content
    def key = 'testkey'

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, 0, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createMultipartUpload(provider, objectEntity, user)

    assert(resultEntity != null)
    assert(resultEntity.getState().equals(ObjectState.mpu_pending))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupUpload(bucket, key, resultEntity.getUploadId());
    assert(fetched.getState() == ObjectState.mpu_pending)
    assert(fetched.getSize() == 0)
    assert(fetched.getUploadId() == resultEntity.getUploadId())

    List<Part> partList = new ArrayList<Part>(10);
    List<PartEntity> partEntities = new ArrayList<PartEntity>();

    //Don't change this, this tests overwriting the same part
    def partNumber = 1
    for(int i = 1; i <= 10; i++) {
      partEntities.clear();
      partList.clear();
      content = ('fakecontent123' + String.valueOf(i)).getBytes("UTF-8")
      PartEntity tmp = PartEntity.newInitializedForCreate(bucket, key, resultEntity.getUploadId(), partNumber, content.length, user)
      partEntities.add(partNumber - 1, OsgObjectFactory.getFactory().createObjectPart(provider, fetched, tmp, new ByteArrayInputStream(content), user))

      PaginatedResult<PartEntity> partsList1 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 0, 1000)
      assert(partsList1.getEntityList().size() == 1)
      assert(partsList1.getEntityList().first().geteTag() == partEntities.get(partNumber - 1).geteTag())
      partList.add(partNumber - 1, new Part(partNumber, partEntities.last().geteTag()))
    }

    //Add the rest of the parts
    for(int i = partNumber+1; i <= 10; i++) {
      content = ('fakecontent123' + String.valueOf(i)).getBytes("UTF-8")
      PartEntity tmp = PartEntity.newInitializedForCreate(bucket, key, resultEntity.getUploadId(), i, content.length, user)
      partEntities.add(OsgObjectFactory.getFactory().createObjectPart(provider, fetched, tmp, new ByteArrayInputStream(content), user))

      PaginatedResult<PartEntity> partsList1 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 0, 1000)
      assert(partsList1.getEntityList().size() == i)
      for(int j = 0 ; j < i; j++) {
        assert(partsList1.getEntityList().get(j).geteTag() == partEntities[j].geteTag())
      }
      partList.add(new Part(i, partEntities.last().geteTag()))
    }

    Long summedSize = 0
    partEntities.each { summedSize += ((PartEntity)it).getSize() }

    GetObjectType request = new GetObjectType()
    request.setUser(user)

    request.setKey(resultEntity.getObjectUuid())
    request.setBucket(bucket.getBucketUuid())
    try {
      GetObjectResponseType response = provider.getObject(request)
      fail("Should have failed to get key on incomplete MPU")
    } catch(NoSuchKeyException e) {
      //correct, not committed
    }

    //Complete the upload
    ObjectEntity finalObject = OsgObjectFactory.getFactory().completeMultipartUpload(provider, resultEntity, partList, user)
    assert(finalObject != null)
    assert(finalObject.geteTag() != null)
    assert(finalObject.getSize() == summedSize)
    assert(finalObject.getState() == ObjectState.extant)

    ObjectEntity foundObj = ObjectMetadataManagers.getInstance().lookupObject(bucket, key, null)
    assert(foundObj.getSize() == finalObject.getSize())
    assert(foundObj.geteTag() == finalObject.geteTag())
    assert(foundObj.getObjectUuid() == finalObject.getObjectUuid())

    GetObjectResponseType response = provider.getObject(request)
    assert(response.getEtag().equals(resultEntity.geteTag()))
    assert(response.getSize().equals(resultEntity.getSize()))
    byte[] buffer = new byte[content.length]
    response.getDataInputStream().read(buffer)
    for(int i = 0; i < buffer.size(); i++) {
      assert(buffer[i] == content[i])
    }
  }

  @Test
  public void testListParts() throws Exception {
    //Set the min part size to 1 to allow small tests
    ObjectStorageProperties.MPU_PART_MIN_SIZE = 1;

    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()
    def key = 'testkey'

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, key, 0, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createMultipartUpload(provider, objectEntity, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.mpu_pending))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupUpload(bucket, key, resultEntity.getUploadId());
    assert (fetched.getState() == ObjectState.mpu_pending)
    assert (fetched.getSize() == 0)
    assert (fetched.getUploadId() == resultEntity.getUploadId())

    List<Part> partList = new ArrayList<Part>()
    List<PartEntity> partEntities = new ArrayList<PartEntity>();

    for (int i = 1; i <= 10; i++) {
      PartEntity tmp = PartEntity.newInitializedForCreate(bucket, key, resultEntity.getUploadId(), i, content.length, user)
      partEntities.add(OsgObjectFactory.getFactory().createObjectPart(provider, fetched, tmp, new ByteArrayInputStream(content), user))
      partList.add(new Part(i, partEntities.last().geteTag()))
    }

    PaginatedResult<PartEntity> partsList1 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 0, 1000)
    assert (partsList1.getEntityList().size() == 10)
    assert (!partsList1.getIsTruncated())
    for (int j = 0; j < 10; j++) {
      assert (partsList1.getEntityList().get(j).geteTag() == partEntities[j].geteTag())
    }

    PaginatedResult<PartEntity> partsList2 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 1, 5)
    assert (partsList2.getEntityList().size() == 5)
    assert (partsList2.getIsTruncated())
    for (int j = 0; j < 5 ; j++) {
      assert (partsList2.getEntityList().get(j).geteTag() == partEntities[1 + j].geteTag())
    }

    PaginatedResult<PartEntity> partsList3 = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, key, resultEntity.getUploadId(), 3, 20)
    assert (partsList3.getEntityList().size() == 7)
    assert (!partsList3.getIsTruncated())
    for (int j = 0; j < 7; j++) {
      assert (partsList3.getEntityList().get(j).geteTag() == partEntities[3 + j].geteTag())
    }

    //Complete the upload
    ObjectEntity finalObject = OsgObjectFactory.getFactory().completeMultipartUpload(provider, resultEntity, partList, user)
  }


  /**
   * Expect copy to complete and result in 'extant' object
   * @throws Exception
   */
  @Test
  public void testCopyObject() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    Bucket sourceBucket = Bucket.getInitializedBucket("test-source-bucket", user.getUserId(), acp, null)
    sourceBucket = OsgBucketFactory.getFactory().createBucket(provider, sourceBucket, UUID.randomUUID().toString(), user)

    Bucket destBucket = Bucket.getInitializedBucket("test-dest-bucket", user.getUserId(), acp, null)
    destBucket = OsgBucketFactory.getFactory().createBucket(provider, destBucket, UUID.randomUUID().toString(), user)

    assert (sourceBucket != null && destBucket != null)
    assert (sourceBucket.getState().equals(BucketState.extant) && destBucket.getState().equals(BucketState.extant))
    byte[] content = 'fakecontent123'.getBytes()
    def key = 'testkey'

    ObjectEntity srcObjectEntity = ObjectEntity.newInitializedForCreate(sourceBucket, key, content.length, user)
    srcObjectEntity.setAcl(acp)
    ObjectEntity resultSrcEntity = OsgObjectFactory.getFactory().createObject(provider, srcObjectEntity, new ByteArrayInputStream(content), null, user)

    assert (resultSrcEntity != null)
    assert (resultSrcEntity.getState().equals(ObjectState.extant))
    ObjectEntity fetched = ObjectMetadataManagers.getInstance().lookupObject(sourceBucket, key, null)
    assert (fetched.geteTag().equals(resultSrcEntity.geteTag()))

    ObjectEntity destObjectEntity = ObjectEntity.newInitializedForCreate(destBucket, key, content.length, user)
    CopyObjectType outgoing = new CopyObjectType()
    outgoing.setSourceBucket(sourceBucket.bucketUuid)
    outgoing.setSourceObject(resultSrcEntity.objectUuid)
    outgoing.setDestinationBucket(destBucket.bucketUuid)
    outgoing.setDestinationObject(destObjectEntity.objectUuid)
    ObjectEntity resultDestEntity = OsgObjectFactory.getFactory().copyObject(provider, destObjectEntity, outgoing, user, "COPY");
    assert (resultDestEntity != null)

    GetObjectType request = new GetObjectType()
    request.setUser(user)
    request.setKey(resultDestEntity.getObjectUuid())
    request.setBucket(destBucket.getBucketUuid())
    GetObjectResponseType response = provider.getObject(request)
    assert (response.getEtag().equals(resultDestEntity.geteTag()))
    assert (response.getSize().equals(resultDestEntity.getSize()))
    byte[] buffer = new byte[content.length]
    response.getDataInputStream().read(buffer)
    for (int i = 0; i < buffer.size(); i++) {
      assert (buffer[i] == content[i])
    }
  }

  /**
   * 
   * @throws Exception
   */
  @Test
  public void testDeleteMarker() throws Exception {
    UserPrincipal user = Accounts.lookupPrincipalByUserId(UnitTestSupport.getUsersByAccountName(UnitTestSupport.getTestAccounts().first()).first())
    String canonicalId = user.getCanonicalId()
    AccessControlPolicy acp = new AccessControlPolicy()
    acp.setAccessControlList(new AccessControlList())
    acp = AclUtils.processNewResourcePolicy(user, acp, canonicalId)

    // Create bucket
    Bucket bucket = Bucket.getInitializedBucket("testbucket", user.getUserId(), acp, null)
    bucket = OsgBucketFactory.getFactory().createBucket(provider, bucket, UUID.randomUUID().toString(), user)

    assert (bucket != null)
    assert (bucket.getState().equals(BucketState.extant))

    // Enable versioning
    ObjectStorageProperties.VersioningStatus versionStatus = ObjectStorageProperties.VersioningStatus.Enabled
    BucketMetadataManagers.getInstance().setVersioning(bucket, versionStatus)

    bucket = BucketMetadataManagers.getInstance().lookupExtantBucket("testbucket")

    assert (bucket != null)
    assert (bucket.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Enabled))

    byte[] content = 'fakecontent123'.getBytes()

    ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket, "testkey", content.length, user)
    objectEntity.setAcl(acp)
    ObjectEntity resultEntity = OsgObjectFactory.getFactory().createObject(provider, objectEntity, new ByteArrayInputStream(content), null, user)

    assert (resultEntity != null)
    assert (resultEntity.getState().equals(ObjectState.extant))

    //Do the delete logically
    ObjectEntity deleteMarker1 = OsgObjectFactory.getFactory().logicallyDeleteObject(provider, resultEntity, user)

    assert (deleteMarker1 != null)
    assert (deleteMarker1.getVersionId() != null)

    //Do the delete logically
    ObjectEntity deleteMarker2 = OsgObjectFactory.getFactory().logicallyDeleteObject(provider, resultEntity, user)

    assert (deleteMarker2 != null)
    assert (deleteMarker2.getVersionId() != null)
    assert (!deleteMarker1.getVersionId().equals(deleteMarker2.getVersionId()))

    // Suspend versioning
    versionStatus = ObjectStorageProperties.VersioningStatus.Suspended
    BucketMetadataManagers.getInstance().setVersioning(bucket, versionStatus)

    bucket = BucketMetadataManagers.getInstance().lookupExtantBucket("testbucket")

    assert (bucket != null)
    assert (bucket.getVersioning().equals(ObjectStorageProperties.VersioningStatus.Suspended))

    resultEntity = ObjectMetadataManagers.getInstance().lookupObject(bucket, "testkey", null)
    assert (resultEntity != null)

    //Do the delete logically
    deleteMarker1 = OsgObjectFactory.getFactory().logicallyDeleteObject(provider, resultEntity, user)

    assert (deleteMarker1 != null)
    assert (ObjectStorageProperties.NULL_VERSION_ID.equals(deleteMarker1.getVersionId()))

    //Do the delete logically
    deleteMarker2 = OsgObjectFactory.getFactory().logicallyDeleteObject(provider, resultEntity, user)

    assert (deleteMarker2 != null)
    assert (ObjectStorageProperties.NULL_VERSION_ID.equals(deleteMarker2.getVersionId()))
    assert (!deleteMarker1.getObjectModifiedTimestamp().equals(deleteMarker2.getObjectModifiedTimestamp()))
  }

}
