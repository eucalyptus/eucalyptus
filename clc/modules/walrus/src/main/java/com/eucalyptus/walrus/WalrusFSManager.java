/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.common.fs.FileIO;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.Status;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.BucketInfo;
import com.eucalyptus.walrus.entities.MetaDataInfo;
import com.eucalyptus.walrus.entities.ObjectInfo;
import com.eucalyptus.walrus.entities.PartInfo;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.exceptions.AccessDeniedException;
import com.eucalyptus.walrus.exceptions.BucketAlreadyExistsException;
import com.eucalyptus.walrus.exceptions.BucketNotEmptyException;
import com.eucalyptus.walrus.exceptions.ContentMismatchException;
import com.eucalyptus.walrus.exceptions.HeadNoSuchBucketException;
import com.eucalyptus.walrus.exceptions.HeadNoSuchEntityException;
import com.eucalyptus.walrus.exceptions.InlineDataTooLargeException;
import com.eucalyptus.walrus.exceptions.InternalErrorException;
import com.eucalyptus.walrus.exceptions.InvalidArgumentException;
import com.eucalyptus.walrus.exceptions.InvalidPartException;
import com.eucalyptus.walrus.exceptions.InvalidRangeException;
import com.eucalyptus.walrus.exceptions.NoSuchBucketException;
import com.eucalyptus.walrus.exceptions.NoSuchEntityException;
import com.eucalyptus.walrus.exceptions.NoSuchUploadException;
import com.eucalyptus.walrus.exceptions.NotModifiedException;
import com.eucalyptus.walrus.exceptions.PreconditionFailedException;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadType;
import com.eucalyptus.walrus.msgs.CopyObjectResponseType;
import com.eucalyptus.walrus.msgs.CopyObjectType;
import com.eucalyptus.walrus.msgs.CreateBucketResponseType;
import com.eucalyptus.walrus.msgs.CreateBucketType;
import com.eucalyptus.walrus.msgs.DeleteBucketResponseType;
import com.eucalyptus.walrus.msgs.DeleteBucketType;
import com.eucalyptus.walrus.msgs.DeleteObjectResponseType;
import com.eucalyptus.walrus.msgs.DeleteObjectType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedType;
import com.eucalyptus.walrus.msgs.GetObjectResponseType;
import com.eucalyptus.walrus.msgs.GetObjectType;
import com.eucalyptus.walrus.msgs.HeadBucketResponseType;
import com.eucalyptus.walrus.msgs.HeadBucketType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsType;
import com.eucalyptus.walrus.msgs.ListBucketResponseType;
import com.eucalyptus.walrus.msgs.ListBucketType;
import com.eucalyptus.walrus.msgs.PutObjectInlineResponseType;
import com.eucalyptus.walrus.msgs.PutObjectInlineType;
import com.eucalyptus.walrus.msgs.PutObjectResponseType;
import com.eucalyptus.walrus.msgs.PutObjectType;
import com.eucalyptus.walrus.msgs.UploadPartResponseType;
import com.eucalyptus.walrus.msgs.UploadPartType;
import com.eucalyptus.walrus.pipeline.WalrusRESTBinding;
import com.eucalyptus.walrus.util.WalrusProperties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import edu.ucsb.eucalyptus.util.SystemUtil;

public class WalrusFSManager extends WalrusManager {
  private static Logger LOG = Logger.getLogger(WalrusFSManager.class);

  private StorageManager storageManager;

  public WalrusFSManager(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  @Override
  public void initialize() throws EucalyptusCloudException {
    check();
  }

  @Override
  public void check() throws EucalyptusCloudException {
    WalrusInfo walrusInfo = WalrusInfo.getWalrusInfo();
    String bukkitDir = walrusInfo.getStorageDir();
    File bukkits = new File(bukkitDir);
    if (!bukkits.exists()) {
      if (!bukkits.mkdirs()) {
        LOG.fatal("Unable to make bucket root directory: " + bukkitDir);
        throw new InternalErrorException("Invalid bucket root directory");
      }
    } else if (!bukkits.canWrite()) {
      LOG.fatal("Cannot write to bucket root directory: " + bukkitDir);
      throw new InternalErrorException("Invalid bucket root directory");
    }
    try {
      SystemUtil.setEucaReadWriteOnly(bukkitDir);
    } catch (EucalyptusCloudException ex) {
      LOG.fatal(ex);
    }
  }

  public void start() throws EucalyptusCloudException {}

  public void stop() throws EucalyptusCloudException {}

  @Override
  public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws WalrusException {
    ListAllMyBucketsResponseType reply = (ListAllMyBucketsResponseType) request.getReply();
    Context ctx = Contexts.lookup();

    try {
      BucketInfo searchBucket = new BucketInfo();
      searchBucket.setOwnerId(ctx.getAccountNumber());
      List<BucketInfo> bucketInfoList = Transactions.findAll(searchBucket);
      ListAllMyBucketsList bucketList = new ListAllMyBucketsList();

      if (bucketInfoList != null && !bucketInfoList.isEmpty()) {
        bucketList.setBuckets((ArrayList<BucketListEntry>) Lists.transform(bucketInfoList, new Function<BucketInfo, BucketListEntry>() {

          @Override
          public BucketListEntry apply(BucketInfo arg0) {
            return new BucketListEntry(arg0.getBucketName(), DateFormatter.dateToListingFormattedString(arg0.getCreationDate()));
          }
        }));
      } else { // no buckets found
        bucketList.setBuckets(new ArrayList<BucketListEntry>());
      }

      reply.setBucketList(bucketList);
      reply.setOwner(buildCanonicalUser( ctx.getUser( ) ));
    } catch (Exception e) {
      LOG.error("Failed to list buckets for account " + ctx.getAccountAlias(), e);
      throw new InternalErrorException("Failed to list buckets for account " + ctx.getAccountAlias(), e);
    }
    return reply;
  }

  /**
   * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise returns 404 if not found or 403 if no
   * accesss.
   *
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  @Override
  public HeadBucketResponseType headBucket(HeadBucketType request) throws WalrusException {
    HeadBucketResponseType reply = (HeadBucketResponseType) request.getReply();
    String bucketName = request.getBucket();
    try {
      Transactions.find(new BucketInfo(bucketName));
      return reply;
    } catch (NoSuchElementException e) {
      // Bucket not found return 404
      throw new HeadNoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Faild to look up bucket=" + bucketName, e);
      throw new HeadNoSuchBucketException("Faild to look up bucket=" + bucketName, e);
    }
  }

  @Override
  public CreateBucketResponseType createBucket(CreateBucketType request) throws WalrusException {
    CreateBucketResponseType reply = (CreateBucketResponseType) request.getReply();

    String bucketName = request.getBucket();
    String locationConstraint = request.getLocationConstraint();

    // Check if the bucket already exists
    try {
      Transactions.find(new BucketInfo(bucketName));
      throw new BucketAlreadyExistsException(bucketName);
    } catch (NoSuchElementException e) {
      // Bucket does not exist, go to next step
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Faild to look up metadata for bucket " + bucketName, e);
    }

    try {
      // create bucket and set its acl
      BucketInfo bucket = new BucketInfo(bucketName, new Date());
      bucket.setBucketSize(0L);
      if (locationConstraint != null && locationConstraint.length() > 0) {
        bucket.setLocation(locationConstraint);
      } else {
        bucket.setLocation(null);
      }
      // save the bucket to disk
      try {
        storageManager.createBucket(bucketName);
      } catch (Exception e) {
        LOG.error("Failed to create bucket " + bucketName + " on disk", e);
        throw new InternalErrorException("Failed to create bucket " + bucketName + " on disk", e);
      }
      Transactions.saveDirect(bucket); // Save the bucket to database
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to save metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to save metadata for bucket=" + bucketName, e);
    }

    reply.setBucket(bucketName);
    return reply;
  }

  @Override
  public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws WalrusException {
    DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
    String bucketName = request.getBucket();

    try (TransactionResource tr = Entities.transactionFor(BucketInfo.class)) {
      BucketInfo bucketInfo = Entities.uniqueResult(new BucketInfo(bucketName));

      ObjectInfo searchObject = new ObjectInfo();
      searchObject.setBucketName(bucketName);
      List<ObjectInfo> objects = Transactions.findAll(searchObject);

      if (objects != null && !objects.isEmpty()) {
        tr.commit();
        throw new BucketNotEmptyException(bucketName);
      } else {
        Entities.delete(bucketInfo);
        // Actually remove the bucket from the backing store
        // share threadpool with object deleter
        Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(new BucketDeleter(bucketName));
      }
      tr.commit();
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException( bucketName );
    } catch (BucketNotEmptyException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to delete bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to delete bucket=" + bucketName, e);
    }

    reply.setStatus(HttpResponseStatus.NO_CONTENT);
    reply.setStatusMessage("NO CONTENT");
    return reply;
  }

  @Override
  public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws WalrusException {
    Context ctx = Contexts.lookup();

    GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request.getReply();

    String bucketName = request.getBucket();

    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to lookup metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup metadata for bucket=" + bucketName, e);
    }

    try {
      reply.setAccessControlPolicy(getPrivateACP(buildCanonicalUser( ctx.getUser( ) )));
    } catch ( AuthException e ) {
      LOG.error("Failed to build canonical user for " + ctx.getAccountNumber(), e);
      throw new InternalErrorException("Authorization error");
    }
    return reply;
  }

  @Override
  public PutObjectResponseType putObject(PutObjectType request) throws WalrusException {
    PutObjectResponseType reply = (PutObjectResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup metadata for bucket=" + bucketName, e);
    }

    // writes are unconditional
    long size = 0;
    Date lastModified = null;
    String md5 = new String();
    String key = bucketName + "." + objectKey;
    String randomKey = request.getRandomKey();
    String prevObjectName = null;
    String tempObjectName = UUID.randomUUID().toString();
    String objectName = UUID.randomUUID().toString();
    WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();
    WalrusDataQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

    try {
      WalrusDataMessage dataMessage;
      MessageDigest digest = null;
      FileIO fileIO = null;
      while ((dataMessage = putQueue.poll(60L, TimeUnit.SECONDS)) != null) {
        if (putQueue.getInterrupted()) {
          if (WalrusDataMessage.isEOF(dataMessage)) {
            WalrusMonitor monitor = messenger.getMonitor(key);
            if (monitor.getLastModified() == null) {
              LOG.trace("Monitor wait: " + key + " random: " + randomKey);
              synchronized (monitor) {
                monitor.wait();
              }
            }
            LOG.trace("Monitor resume: " + key + " random: " + randomKey);
            lastModified = monitor.getLastModified();
            md5 = monitor.getMd5();
            // ok we are done here
            if (fileIO != null) {
              fileIO.finish();
            }
            ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, null, null);
            Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
            LOG.warn("Transfer interrupted: " + key);
            messenger.removeQueue(key, randomKey);
            break;
          }
          continue;
        }
        if (WalrusDataMessage.isStart(dataMessage)) {
          digest = Digest.MD5.get();
          try {
            fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
          } catch (Exception ex) {
            messenger.removeQueue(key, randomKey);
            throw new AccessDeniedException(ex);
          }
        } else if (WalrusDataMessage.isEOF(dataMessage)) {
          if (digest != null) {
            md5 = Hashes.bytesToHex(digest.digest());
          } else {
            WalrusMonitor monitor = messenger.getMonitor(key);
            md5 = monitor.getMd5();
            lastModified = monitor.getLastModified();
            if (md5 == null) {
              LOG.error("ETag did not match for: " + randomKey + " Computed MD5 is null");
              throw new ContentMismatchException(bucketName + "/" + objectKey);
            }
            break;
          }
          String contentMD5 = request.getContentMD5();
          if (contentMD5 != null) {
            String contentMD5AsHex = Hashes.bytesToHex(Base64.decode(contentMD5));
            if (!contentMD5AsHex.equals(md5)) {
              if (fileIO != null) {
                fileIO.finish();
              }
              cleanupTempObject(bucketName, tempObjectName);
              messenger.removeQueue(key, randomKey);
              LOG.error("ETag did not match for: " + randomKey + " Expected: " + contentMD5AsHex + " Computed: " + md5);
              throw new ContentMismatchException(bucketName + "/" + objectKey);
            }
          }

          // Fix for EUCA-2275: Moved up policy and bucket size checks on the temporary object. The temp object is committed (renamed) only after it
          // clears the checks. If any of the checks fail, temp object is cleaned up and the process errors out. If the PUT request is overwriting an
          // existing object, the object is left untouched. So the fix ensures proper clean up of temp files (no orphaned files) and does not
          // overwrite existing data when policy or bucket size checks fail

          // rename temporary object
          try {
            if (fileIO != null) {
              fileIO.finish();
            }
            storageManager.renameObject(bucketName, tempObjectName, objectName);
          } catch (IOException ex) {
            LOG.error("Failed to rename file " + tempObjectName + " to " + objectName + ". object-key=" + objectKey + ", bucket=" + bucketName);
            messenger.removeQueue(key, randomKey);
            throw new AccessDeniedException(objectKey);
          }

          lastModified = new Date();

          // Update object or create a new entity if it does not exist
          try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
            ObjectInfo objectInfo = null;
            try {
              objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
              prevObjectName = objectInfo.getObjectName();
            } catch (NoSuchElementException e) {
              objectInfo = Entities.persist(new ObjectInfo(bucketName, objectKey));
            }

            objectInfo.replaceMetaData(request.getMetaData());
            objectInfo.setEtag(md5);
            objectInfo.setSize(size);
            objectInfo.setLastModified(lastModified);
            objectInfo.setObjectName(objectName);
            objectInfo.setStorageClass("STANDARD");
            objectInfo.setContentType(request.getContentType());
            objectInfo.setContentDisposition(request.getContentDisposition());

            tr.commit();
          } catch (Exception e) {
            LOG.error("Failed to update metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
            throw new InternalErrorException("Failed to update metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
          }

          // Delete the previously uploaded object on the disk
          if (prevObjectName != null) {
            ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, prevObjectName, null, null);
            Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
          }

          // restart all interrupted puts
          WalrusMonitor monitor = messenger.getMonitor(key);
          synchronized (monitor) {
            monitor.setLastModified(lastModified);
            monitor.setMd5(md5);
            monitor.notifyAll();
          }
          // messenger.removeMonitor(key);
          messenger.clearQueues(key);
          messenger.removeQueue(key, randomKey);
          LOG.trace("Transfer complete: " + key);

          break;
        } else {
          assert (WalrusDataMessage.isData(dataMessage));
          byte[] data = dataMessage.getPayload();
          // start writing object (but do not commit yet)
          try {
            if (fileIO != null)
              fileIO.write(data);
          } catch (IOException ex) {
            LOG.error(ex);
          }
          // calculate md5 on the fly
          size += data.length;
          if (digest != null) {
            digest.update(data);
          }
        }
      }
      if (dataMessage == null) {
        messenger.removeQueue(key, randomKey);
        throw new InternalErrorException("Put timed out: " + key + "." + randomKey);
      }
    } catch (InterruptedException e) {
      LOG.error("Transfer interrupted: " + key + "." + randomKey, e);
      throw new InternalErrorException("Transfer interrupted: " + key + "." + randomKey);
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
    } finally {
      cleanupTempObject(bucketName, tempObjectName);
      messenger.removeQueue(key, randomKey);
    }

    reply.setSize(size);
    reply.setEtag(md5);
    reply.setLastModified(lastModified);
    reply.setVersionId(WalrusProperties.NULL_VERSION_ID);
    return reply;
  }

  private void cleanupTempObject(String bucketName, String tempObjectName) {
    ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, null, null);
    Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
  }

  @Override
  public PutObjectInlineResponseType putObjectInline(PutObjectInlineType request) throws WalrusException {
    PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    long size = 0;
    Date lastModified = null;
    String md5 = new String();
    String prevObjectName = null;
    String objectName = UUID.randomUUID().toString();

    try {
      // writes are unconditional
      if (request.getBase64Data().getBytes().length > WalrusProperties.MAX_INLINE_DATA_SIZE) {
        // db.rollback();
        throw new InlineDataTooLargeException(bucketName + "/" + objectKey);
      }
      byte[] base64Data = Hashes.base64decode(request.getBase64Data()).getBytes();
      size = (long) base64Data.length;

      // Fix for EUCA-2275: Moved up policy and bucket size checks on the temporary object. The object is committed (written) only after it clears the
      // checks. So the fix ensures that no files are orphaned and does not overwrite existing data when policy or bucket size checks fail

      try {
        FileIO fileIO = storageManager.prepareForWrite(bucketName, objectName);
        if (fileIO != null) {
          fileIO.write(base64Data);
          fileIO.finish();
        }
      } catch (Exception ex) {
        throw new AccessDeniedException(ex);
      }
      md5 = Hashes.getHexString(Digest.MD5.get().digest(base64Data));
      lastModified = new Date();

      // Update object or create a new entity if it does not exist
      try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
        ObjectInfo objectInfo = null;

        try {
          objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
          prevObjectName = objectInfo.getObjectName();
        } catch (NoSuchElementException e) {
          objectInfo = Entities.persist(new ObjectInfo(bucketName, objectKey));
        }

        objectInfo.setEtag(md5);
        objectInfo.setSize(size);
        objectInfo.setLastModified(lastModified);
        objectInfo.setObjectName(objectName);
        objectInfo.setStorageClass("STANDARD");
        // Add meta data if specified
        if (request.getMetaData() != null) {
          objectInfo.replaceMetaData(request.getMetaData());
        }

        tr.commit();
      } catch (Exception e) {
        LOG.error("Failed to update metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
        throw new InternalErrorException("Failed to update metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
      }

      // Delete the previously uploaded object on the disk
      if (prevObjectName != null) {
        ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, prevObjectName, null, null);
        Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
      }
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    reply.setSize(size);
    reply.setEtag(md5);
    reply.setLastModified(lastModified);
    reply.setVersionId(WalrusProperties.NULL_VERSION_ID);
    return reply;
  }

  @Override
  public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws WalrusException {
    DeleteObjectResponseType reply = (DeleteObjectResponseType) request.getReply();
    final String bucketName = request.getBucket();
    final String objectKey = request.getKey();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    // Lookup object and process deletion
    Boolean taskAdded = Boolean.FALSE;
    try {
      Entities.asTransaction(ObjectInfo.class, new Function<Boolean, String>() {

        @Override
        public String apply(Boolean arg0) {
          try {
            ObjectInfo objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
            if (!arg0) { // add task to delete only once
              ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, objectInfo.getObjectName(), objectKey, objectInfo.getUploadId());
              Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
              arg0 = Boolean.TRUE;
            }
            Entities.delete(objectInfo);
          } catch (NoSuchElementException e) {
            LOG.debug("Metadata for object-key=" + objectKey + ", bucket=" + bucketName + " not found. Nothing to delete");
          } catch (Exception e) {
            Exceptions.toUndeclared(e);
          }
          return null;
        }
      }).apply(taskAdded);
    } catch (Exception e) {
      LOG.error("Failed to delete object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to delete object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    // Always set the response to 204 NO CONTENT
    reply.setStatus(HttpResponseStatus.NO_CONTENT);
    reply.setStatusMessage("NO CONTENT");
    return reply;
  }

  private class ObjectDeleter implements Runnable {
    final String bucketName;
    final String objectName;
    final String objectKey;
    final String uploadId;

    public ObjectDeleter(String bucketName, String objectName, String objectKey, String uploadId) {
      this.bucketName = bucketName;
      this.objectName = objectName;
      this.objectKey = objectKey;
      this.uploadId = uploadId;
    }

    @Override
    public void run() {
      if (StringUtils.isNotBlank(uploadId)) {
        deleteParts(bucketName, objectKey, uploadId, false);
      } else {
        try {
          storageManager.deleteObject(bucketName, objectName);
        } catch (Exception ex) {
          LOG.error("Failed to delete file=" + objectName + ", bucket=" + bucketName, ex);
          return;
        }
      }
    }
  }

  private class BucketDeleter implements Runnable {
    final String bucketName;

    public BucketDeleter(String bucketName) {
      this.bucketName = bucketName;
    }

    @Override
    public void run() {
      try {
        deleteParts(bucketName);
        storageManager.deleteBucket(bucketName);
      } catch (Exception ex) {
        // set exception code in reply
        LOG.error(ex);
      }
    }
  }

  @Override
  public ListBucketResponseType listBucket(ListBucketType request) throws WalrusException {
    ListBucketResponseType reply = (ListBucketResponseType) request.getReply();

    Context ctx = Contexts.lookup();
    String bucketName = request.getBucket();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    String prefix = request.getPrefix();
    String delimiter = request.getDelimiter();
    String marker = request.getMarker();
    int maxKeys = -1;

    reply.setName(bucketName);
    reply.setIsTruncated(false);
    reply.setPrefix(prefix);
    reply.setMarker(marker);
    reply.setDelimiter(delimiter);

    if (request.getMaxKeys() != null) {
      maxKeys = Integer.parseInt(request.getMaxKeys());
      if (maxKeys < 0) {
        throw new InvalidArgumentException("max-keys", "Argument max-keys must be an integer between 0 and " + Integer.MAX_VALUE);
      } else if (maxKeys == 0) {
        // No keys requested, so just return
        reply.setMaxKeys(maxKeys);
        reply.setContents(new ArrayList<ListEntry>());
        reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());
        return reply;
      }
    } else {
      maxKeys = WalrusProperties.MAX_KEYS;
      reply.setMaxKeys(maxKeys);
    }

    try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {

      final int queryStrideSize = maxKeys + 1;

      ObjectInfo searchObj = new ObjectInfo();
      searchObj.setBucketName(bucketName);

      Criteria objCriteria = Entities.createCriteria(ObjectInfo.class);
      objCriteria.add(Example.create(searchObj));
      objCriteria.addOrder(Order.asc("objectKey"));
      objCriteria.setMaxResults(queryStrideSize); // add one to, hopefully, indicate truncation in one call
      objCriteria.setReadOnly(true);

      if (!Strings.isNullOrEmpty(marker)) {
        // The result set should be exclusive of the marker. marker could be a common prefix from a previous response. Look for keys that
        // lexicographically follow the marker and don't contain the marker as the prefix.
        objCriteria.add(Restrictions.gt("objectKey", marker));
      } else {
        marker = "";
      }

      if (!Strings.isNullOrEmpty(prefix)) {
        objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
      } else {
        prefix = "";
      }

      // Ensure not null.
      if (Strings.isNullOrEmpty(delimiter)) {
        delimiter = "";
      }

      List<ObjectInfo> objectInfos = null;
      int resultKeyCount = 0;
      ArrayList<ListEntry> contents = new ArrayList<ListEntry>(); // contents for reply
      String nextMarker = null;
      TreeSet<String> commonPrefixes = new TreeSet<String>();
      int firstResult = -1;
      CanonicalUser owner = buildCanonicalUser( ctx.getUser( ) ); // same owner for every object

      // Iterate over result sets of size maxkeys + 1
      do {
        // Start listing from the 0th element and increment the first element to be listed by the query size
        objCriteria.setFirstResult(queryStrideSize * (++firstResult));
        objectInfos = (List<ObjectInfo>) objCriteria.list();

        if (objectInfos.size() > 0) {
          for (ObjectInfo objectInfo : objectInfos) {
            String objectKey = objectInfo.getObjectKey();

            // Check if it will get aggregated as a commonprefix
            if (!Strings.isNullOrEmpty(delimiter)) {
              // Split the substring to a maximum of two parts as we need a result containing trailing strings. For instance
              // if "x" is delimiter and key string is also "x", then this key should be included in common prefixes.
              // "x".split("x") gives 0 strings which causes the subsequent logic to skip the key where as
              // "x".split("x", 2) gives 2 empty strings which is what the logic expects
              String[] parts = objectKey.substring(prefix.length()).split(delimiter, 2);
              if (parts.length > 1) {
                String prefixString = prefix + parts[0] + delimiter;
                if (!StringUtils.equals(prefixString, marker) && !commonPrefixes.contains(prefixString)) {
                  if (resultKeyCount == maxKeys) {
                    // This is a new record, so we know we're truncating if this is true
                    reply.setNextMarker(nextMarker);
                    reply.setIsTruncated(true);
                    resultKeyCount++;
                    break;
                  }

                  commonPrefixes.add(prefixString);
                  resultKeyCount++; // count the unique commonprefix as a single return entry

                  // If max keys have been collected, set the next-marker. It might be needed for the response if the list is truncated
                  // If the common prefixes hit the limit set by max-keys, next-marker is the last common prefix
                  if (resultKeyCount == maxKeys) {
                    nextMarker = prefixString;
                  }
                }
                continue;
              }
            }

            if (resultKeyCount == maxKeys) {
              // This is a new (non-commonprefix) record, so we know we're truncating
              reply.setNextMarker(nextMarker);
              reply.setIsTruncated(true);
              resultKeyCount++;
              break;
            }

            // Process the entry as a full key listing
            ListEntry listEntry = new ListEntry();
            listEntry.setKey(objectKey);
            listEntry.setEtag(objectInfo.getEtag());
            listEntry.setLastModified(DateFormatter.dateToListingFormattedString(objectInfo.getLastModified()));
            listEntry.setStorageClass(objectInfo.getStorageClass());
            listEntry.setSize(objectInfo.getSize());
            listEntry.setStorageClass(objectInfo.getStorageClass());
            listEntry.setOwner(owner);
            contents.add(listEntry);

            resultKeyCount++;

            // If max keys have been collected, set the next-marker. It might be needed for the response if the list is truncated
            if (resultKeyCount == maxKeys) {
              nextMarker = objectKey;
            }
          }
        }

        if (resultKeyCount <= maxKeys && objectInfos.size() <= maxKeys) {
          break;
        }
      } while (resultKeyCount <= maxKeys);

      tr.commit();

      reply.setContents(contents);

      // Prefixes are already sorted, add them to the proper data structures and populate the reply
      if (!commonPrefixes.isEmpty()) {
        ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
        for (String prefixEntry : commonPrefixes) {
          commonPrefixesList.add(new CommonPrefixesEntry(prefixEntry));
        }
        reply.setCommonPrefixesList(commonPrefixesList);
      }

      return reply;
    } catch (Exception e) {
      LOG.error("Failed to lookup metadata for objects in bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup metadata for objects in bucket=" + bucketName, e);
    }
  }

  @Override
  public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws WalrusException {
    GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();
    Context ctx = Contexts.lookup();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    // Lookup object
    try {
      Transactions.find(new ObjectInfo(bucketName, objectKey));
    } catch (NoSuchElementException e) {
      throw new NoSuchEntityException(objectKey);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    try {
      reply.setAccessControlPolicy( getPrivateACP( buildCanonicalUser( ctx.getUser( ) ) ) );
    } catch ( AuthException e ) {
      LOG.error("Failed to build canonical user for " + ctx.getAccountNumber(), e);
      throw new InternalErrorException("Authorization error");
    }
    return reply;
  }

  @Override
  public GetObjectResponseType getObject(GetObjectType request) throws WalrusException {
    GetObjectResponseType reply = (GetObjectResponseType) request.getReply();
    // Must explicitly set to true for streaming large objects.
    reply.setHasStreamingData(false);
    String bucketName = request.getBucket();
    String objectKey = request.getKey();
    Boolean deleteAfterGet = request.getDeleteAfterGet();
    if (deleteAfterGet == null) {
      deleteAfterGet = false;
    }

    Boolean getMetaData = request.getGetMetaData();
    if (getMetaData == null) {
      getMetaData = false;
    }

    Boolean getData = request.getGetData();
    if (getData == null) {
      getData = false;
    }

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      if (getData) {
        throw new NoSuchBucketException(bucketName);
      } else {
        throw new HeadNoSuchBucketException(bucketName);
      }
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    // Lookup object
    ObjectInfo objectInfo = null;
    try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
      objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
      if (objectInfo.getMetaData() != null) {
        objectInfo.getMetaData().size();
      }
      tr.commit();
    } catch (NoSuchElementException e) {
      if (getData) {
        throw new NoSuchEntityException(objectKey);
      } else {
        throw new HeadNoSuchEntityException(objectKey);
      }
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    try {
      String objectName = objectInfo.getObjectName();
      Date lastModified = objectInfo.getLastModified();
      Long size = objectInfo.getSize();
      String etag = objectInfo.getEtag();
      String contentType = objectInfo.getContentType();
      String contentDisposition = objectInfo.getContentDisposition();

      if (getMetaData && objectInfo.getMetaData() != null) {
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        for (MetaDataInfo metaDataInfo : objectInfo.getMetaData()) {
          metaData.add(new MetaDataEntry(metaDataInfo.getName(), metaDataInfo.getValue()));
        }
        reply.setMetaData(metaData);
      }

      if (request.getGetData()) {
        // check if this is a multipart object
        if (objectInfo.isMultipart()) {
          String inlineData = getMultipartData(objectInfo, request, reply);
          if (inlineData != null) {
            reply.setBase64Data(inlineData);
          }
        } else {
          if (request.getInlineData()) {
            if ((size * 4) > WalrusProperties.MAX_INLINE_DATA_SIZE) {
              throw new InlineDataTooLargeException(bucketName + "/" + objectKey);
            }
            byte[] bytes = new byte[102400];
            int bytesRead = 0, offset = 0;
            String base64Data = "";
            try {
              FileIO fileIO = storageManager.prepareForRead(bucketName, objectName);
              while ((bytesRead = fileIO.read(offset)) > 0) {
                ByteBuffer buffer = fileIO.getBuffer();
                if (buffer != null) {
                  buffer.get(bytes, 0, bytesRead);
                  base64Data += new String(bytes, 0, bytesRead);
                  offset += bytesRead;
                }
              }
              fileIO.finish();
            } catch (Exception e) {
              LOG.error(e, e);
              throw new InternalErrorException(e);
            }
            reply.setBase64Data(Hashes.base64encode(base64Data));

          } else {
            reply.setHasStreamingData(true);
            // support for large objects
            // fill in reply with useful things
            storageManager.getObject(bucketName, objectName, reply, size, request.getIsCompressed());
          }
        }
      } else {
        // could be a metadata only request, nothing to do here
      }

      reply.setEtag(etag);
      reply.setLastModified(lastModified);
      reply.setVersionId(WalrusProperties.NULL_VERSION_ID);
      reply.setSize(size);
      reply.setContentType(contentType);
      reply.setContentDisposition(contentDisposition);
      Status status = new Status();
      status.setCode(200);
      status.setDescription("OK");
      reply.setStatus(status);
      return reply;
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to perform get operation for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to perform get operation for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }
  }

  @Override
  public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws WalrusException {
    GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request.getReply();
    Date ifModifiedSince = request.getIfModifiedSince();
    Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
    String ifMatch = request.getIfMatch();
    String ifNoneMatch = request.getIfNoneMatch();
    boolean returnCompleteObjectOnFailure = request.getReturnCompleteObjectOnConditionFailure();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();

    Boolean getData = request.getGetData();
    if (getData == null) {
      getData = false;
    }

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      if (getData) {
        throw new NoSuchBucketException(bucketName);
      } else {
        throw new HeadNoSuchBucketException(bucketName);
      }
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    // Lookup object
    ObjectInfo objectInfo = null;
    try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
      objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
      objectInfo.getMetaData();
      if (objectInfo.getMetaData() != null) {
        objectInfo.getMetaData().size();
      }
      tr.commit();
    } catch (NoSuchElementException e) {
      if (getData) {
        throw new NoSuchEntityException(objectKey);
      } else {
        throw new HeadNoSuchEntityException(objectKey);
      }
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to look up metadata for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    try {
      String etag = objectInfo.getEtag();
      String objectName = objectInfo.getObjectName();
      Long byteRangeStart = request.getByteRangeStart();
      Long byteRangeEnd = request.getByteRangeEnd();

      if (byteRangeStart == null) {
        byteRangeStart = 0L;
      }
      if (byteRangeEnd == null) {
        byteRangeEnd = -1L;
      }

      if (byteRangeEnd == -1 || (byteRangeEnd + 1) > objectInfo.getSize()) {
        byteRangeEnd = objectInfo.getSize() - 1;
      }

      if ((byteRangeStart > objectInfo.getSize()) || (byteRangeStart > byteRangeEnd) || (byteRangeStart < 0 || byteRangeEnd < 0)) {
        throw new InvalidRangeException("Range: " + byteRangeStart + "-" + byteRangeEnd + "object: " + bucketName + "/" + objectKey);
      }
      if (ifMatch != null) {
        if (!ifMatch.equals(etag) && !returnCompleteObjectOnFailure) {
          throw new PreconditionFailedException(objectKey + " etag: " + etag);
        }

      }
      if (ifNoneMatch != null) {
        if (ifNoneMatch.equals(etag) && !returnCompleteObjectOnFailure) {
          throw new NotModifiedException(objectKey + " ETag: " + etag);
        }
      }
      Date lastModified = objectInfo.getLastModified();
      if (ifModifiedSince != null) {
        if ((ifModifiedSince.getTime() >= lastModified.getTime()) && !returnCompleteObjectOnFailure) {
          throw new NotModifiedException(objectKey + " LastModified: " + lastModified.toString());
        }
      }
      if (ifUnmodifiedSince != null) {
        if ((ifUnmodifiedSince.getTime() < lastModified.getTime()) && !returnCompleteObjectOnFailure) {
          throw new PreconditionFailedException(objectKey + " lastModified: " + lastModified.toString());
        }
      }

      if (request.getGetMetaData() && objectInfo.getMetaData() != null) {
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        for (MetaDataInfo metaDataInfo : objectInfo.getMetaData()) {
          metaData.add(new MetaDataEntry(metaDataInfo.getName(), metaDataInfo.getValue()));
        }
        reply.setMetaData(metaData);
      }

      Long size = byteRangeEnd - byteRangeStart + 1;
      String contentType = objectInfo.getContentType();
      String contentDisposition = objectInfo.getContentDisposition();

      if (request.getGetData()) {
        if (objectInfo.isMultipart()) {
          List<PartInfo> parts = getOrderedListOfParts(objectInfo);
          storageManager.getMultipartObject(reply, parts, request.getIsCompressed(), byteRangeStart, byteRangeEnd);
        } else {
          storageManager.getObject(bucketName, objectName, reply, byteRangeStart, byteRangeEnd + 1, request.getIsCompressed());
        }
      }
      reply.setEtag(etag);
      reply.setLastModified(lastModified);
      reply.setSize(size);
      reply.setContentType(contentType);
      reply.setContentDisposition(contentDisposition);
      reply.setByteRangeStart(byteRangeStart);
      reply.setByteRangeEnd(byteRangeEnd);
      reply.setVersionId(WalrusProperties.NULL_VERSION_ID);
      Status status = new Status();
      status.setCode(200);
      status.setDescription("OK");
      reply.setStatus(status);
      return reply;
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to perform get extended operation for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to perform get extended operation for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }
  }

  private String getMultipartData(ObjectInfo objectInfo, GetObjectType request, GetObjectResponseType response) throws WalrusException {
    // get all parts
    List<PartInfo> parts = getOrderedListOfParts(objectInfo);

    if (request.getInlineData()) {
      if ((objectInfo.getSize() * 4) > WalrusProperties.MAX_INLINE_DATA_SIZE) {
        throw new InlineDataTooLargeException(request.getBucket() + "/" + request.getKey());
      }
      String base64Data = "";
      for (PartInfo part : parts) {
        byte[] bytes = new byte[102400];
        int bytesRead = 0, offset = 0;
        try {
          FileIO fileIO = storageManager.prepareForRead(part.getBucketName(), part.getObjectName());
          while ((bytesRead = fileIO.read(offset)) > 0) {
            ByteBuffer buffer = fileIO.getBuffer();
            if (buffer != null) {
              buffer.get(bytes, 0, bytesRead);
              base64Data += new String(bytes, 0, bytesRead);
              offset += bytesRead;
            }
          }
          fileIO.finish();
        } catch (Exception e) {
          LOG.error(e, e);
          throw new InternalErrorException(e);
        }
      }
      return Hashes.base64encode(base64Data);
    } else {
      response.setHasStreamingData(true);
      // support for large objects
      storageManager.getMultipartObject(response, parts, request.getIsCompressed());
      return null;
    }
  }

  @Override
  public CopyObjectResponseType copyObject(CopyObjectType request) throws WalrusException {
    CopyObjectResponseType reply = (CopyObjectResponseType) request.getReply();
    String srcBucketName = request.getSourceBucket();
    String srcObjectKey = request.getSourceObject();
    String destBucketName = request.getDestinationBucket();
    String destObjectKey = request.getDestinationObject();
    String metadataDirective = request.getMetadataDirective();

    if (metadataDirective == null) {
      metadataDirective = "COPY";
    }

    // Lookup source bucket
    try {
      Transactions.find(new BucketInfo(srcBucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(srcBucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + srcBucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + srcBucketName, e);
    }

    // Lookup source object
    ObjectInfo srcObjectInfo = null;
    try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
      srcObjectInfo = Entities.uniqueResult(new ObjectInfo(srcBucketName, srcObjectKey));
      if (srcObjectInfo.getMetaData() != null) {
        srcObjectInfo.getMetaData().size();
      }
      tr.commit();
    } catch (NoSuchElementException e) {
      throw new NoSuchEntityException(srcObjectKey);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for object-key=" + srcObjectKey + ", bucket=" + srcBucketName, e);
      throw new InternalErrorException("Failed to look up metadata for object-key=" + srcObjectKey + ", bucket=" + srcBucketName, e);
    }

    // Lookup destination bucket if it is different than source bucket
    if (!StringUtils.equalsIgnoreCase(srcBucketName, destBucketName)) {
      try {
        Transactions.find(new BucketInfo(destBucketName));
      } catch (NoSuchElementException e) {
        throw new NoSuchBucketException(destBucketName);
      } catch (Exception e) {
        LOG.error("Failed to look up metadata for bucket=" + destBucketName, e);
        throw new InternalErrorException("Failed to lookup bucket " + destBucketName, e);
      }
    }

    String prevDestObjectName = null;
    String destinationObjectName = UUID.randomUUID().toString();
    String etag = srcObjectInfo.getEtag();
    Date lastModified = null;

    try {
      // Copy object
      if (srcObjectInfo.isMultipart()) {
        List<PartInfo> parts = getOrderedListOfParts(srcObjectInfo);
        storageManager.copyMultipartObject(parts, destBucketName, destinationObjectName);
      } else {
        storageManager.copyObject(srcBucketName, srcObjectInfo.getObjectName(), destBucketName, destinationObjectName);
      }

      lastModified = new Date();

      // Update object
      try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
        ObjectInfo destObjectInfo = null;
        try {
          destObjectInfo = Entities.uniqueResult(new ObjectInfo(destBucketName, destObjectKey));
          prevDestObjectName = destObjectInfo.getObjectName();
        } catch (NoSuchElementException e) {
          destObjectInfo = Entities.persist(new ObjectInfo(destBucketName, destObjectKey));
        }

        destObjectInfo.setObjectName(destinationObjectName);
        destObjectInfo.setSize(srcObjectInfo.getSize());
        destObjectInfo.setStorageClass(srcObjectInfo.getStorageClass());
        destObjectInfo.setContentType(srcObjectInfo.getContentType());
        destObjectInfo.setContentDisposition(srcObjectInfo.getContentDisposition());
        destObjectInfo.setEtag(etag);
        destObjectInfo.setLastModified(lastModified); // S3 updates the timestamp on copies
        if (!metadataDirective.equals("REPLACE")) {
          destObjectInfo.setMetaData(srcObjectInfo.cloneMetaData());
        } else {
          destObjectInfo.replaceMetaData(request.getMetaData());
        }
        tr.commit();
      } catch (Exception e) {
        LOG.error("Failed to update metadata for object-key=" + destObjectKey + ", bucket=" + destBucketName, e);
        throw new InternalErrorException("Failed to update metadata for object-key=" + destObjectKey + ", bucket=" + destBucketName, e);
      }

      // Delete the previously uploaded object on the disk
      if (prevDestObjectName != null) {
        ObjectDeleter objectDeleter = new ObjectDeleter(destBucketName, prevDestObjectName, null, null);
        Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
      }
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to copy source-object-key=" + srcObjectKey + ", source-bucket=" + srcBucketName + " to destination-object-key="
          + destObjectKey + ", destination-bucket=" + destBucketName);
      throw new InternalErrorException("Failed to copy source-object-key=" + srcObjectKey + ", source-bucket=" + srcBucketName
          + " to destination-object-key=" + destObjectKey + ", destination-bucket=" + destBucketName, e);
    }

    reply.setEtag(etag);
    // Last modified date in copy response is in ISO8601 format as per S3 spec
    reply.setLastModified(DateFormatter.dateToListingFormattedString(lastModified));
    // reply.setLastModified(copyObjectFormat.format(lastModified));
    reply.setCopySourceVersionId(WalrusProperties.NULL_VERSION_ID);
    reply.setVersionId(WalrusProperties.NULL_VERSION_ID);

    return reply;
  }

  public InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws WalrusException {
    InitiateMultipartUploadResponseType reply = (InitiateMultipartUploadResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    // Create a manifest object entity
    PartInfo manifest = PartInfo.generateManifest(bucketName, objectKey);
    manifest.setStorageClass("STANDARD");
    manifest.setContentEncoding(request.getContentEncoding());
    manifest.setContentType(request.getContentType());
    manifest.replaceMetaData(request.getMetaData());
    manifest.setObjectName(null); // Manifest objects don't have any data associated, hence null the objectName

    // Persist object to database
    try {
      manifest = Transactions.saveDirect(manifest);
    } catch (Exception e) {
      LOG.error("Failed to save part manifest metatdata for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to save part manifest metatdata for object-key=" + objectKey + ", bucket=" + bucketName, e);
    }

    reply.setUploadId(manifest.getUploadId());
    reply.setBucket(bucketName);
    reply.setKey(objectKey);
    return reply;
  }

  public UploadPartResponseType uploadPart(UploadPartType request) throws WalrusException {

    UploadPartResponseType reply = (UploadPartResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();
    String uploadId = request.getUploadId();
    Integer partNumber = Integer.parseInt(request.getPartNumber());

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    PartInfo manifest = null;

    // Look for manifest and the existing part. Overwrite the part if it exists in the system, create a new one if it does not
    try (TransactionResource tr = Entities.transactionFor(PartInfo.class)) {
      PartInfo search = new PartInfo(bucketName, objectKey, uploadId);

      Criteria manifestCriteria = Entities.createCriteria(PartInfo.class);
      manifestCriteria.add(Example.create(search));
      manifestCriteria.add(Restrictions.isNull("partNumber"));
      manifestCriteria.add(Restrictions.isNull("cleanup")); // indicates the multipart upload has not been finalized yet
      manifestCriteria.setReadOnly(true);
      List<PartInfo> foundManifests = manifestCriteria.list();
      if (foundManifests == null || foundManifests.size() != 1) {
        if (foundManifests == null) {
          throw new InternalErrorException("Got invalid metadata");
        } else if (foundManifests.size() < 1) {
          throw new NoSuchUploadException("Multipart upload ID is invalid. Intitiate a multipart upload request before uploading the parts");
        } else {
          throw new InternalErrorException("Multiple manifests found for same uploadId");
        }
      } else {
        manifest = foundManifests.get(0);
        if (manifest.getMetaData() != null) {
          manifest.getMetaData().size();
        }
      }

      tr.commit();
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to process metadata for part-number=" + partNumber + ", upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket="
          + bucketName, e);
      throw new InternalErrorException("Failed to process metadata for part-number=" + partNumber + ", upload-id=" + uploadId + ", object-key="
          + objectKey + ", bucket=" + bucketName, e);
    }

    // writes are unconditional
    long size = 0;
    Date lastModified = null;
    String md5 = new String();
    String key = bucketName + "." + objectKey + "." + partNumber;
    String randomKey = request.getRandomKey();
    String prevObjectName = null;
    String tempObjectName = UUID.randomUUID().toString();
    String objectName = UUID.randomUUID().toString();
    WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();
    WalrusDataQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

    try {
      WalrusDataMessage dataMessage;
      MessageDigest digest = null;
      FileIO fileIO = null;
      while ((dataMessage = putQueue.take()) != null) {
        if (putQueue.getInterrupted()) {
          if (WalrusDataMessage.isEOF(dataMessage)) {
            WalrusMonitor monitor = messenger.getMonitor(key);
            if (monitor.getLastModified() == null) {
              LOG.trace("Monitor wait: " + key + " random: " + randomKey);
              synchronized (monitor) {
                monitor.wait();
              }
            }
            LOG.trace("Monitor resume: " + key + " random: " + randomKey);
            lastModified = monitor.getLastModified();
            md5 = monitor.getMd5();
            // ok we are done here
            if (fileIO != null) {
              fileIO.finish();
            }
            ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, null, null);
            Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
            LOG.warn("Transfer interrupted: " + key);
            messenger.removeQueue(key, randomKey);
            break;
          }
          continue;
        }
        if (WalrusDataMessage.isStart(dataMessage)) {
          tempObjectName = UUID.randomUUID().toString();
          digest = Digest.MD5.get();
          try {
            fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
          } catch (Exception ex) {
            messenger.removeQueue(key, randomKey);
            throw new InternalErrorException(ex);
          }
        } else if (WalrusDataMessage.isEOF(dataMessage)) {
          if (digest != null) {
            md5 = Hashes.bytesToHex(digest.digest());
          } else {
            WalrusMonitor monitor = messenger.getMonitor(key);
            md5 = monitor.getMd5();
            lastModified = monitor.getLastModified();
            if (md5 == null) {
              LOG.error("ETag did not match for: " + randomKey + " Computed MD5 is null");
              throw new ContentMismatchException(bucketName + "/" + objectKey);
            }
            break;
          }
          String contentMD5 = request.getContentMD5();
          if (contentMD5 != null) {
            String contentMD5AsHex = Hashes.bytesToHex(Base64.decode(contentMD5));
            if (!contentMD5AsHex.equals(md5)) {
              if (fileIO != null) {
                fileIO.finish();
              }
              cleanupTempObject(bucketName, tempObjectName);
              messenger.removeQueue(key, randomKey);
              LOG.error("ETag did not match for: " + randomKey + " Expected: " + contentMD5AsHex + " Computed: " + md5);
              throw new ContentMismatchException(bucketName + "/" + objectKey);
            }
          }

          // rename part
          try {
            if (fileIO != null) {
              fileIO.finish();
            }
            storageManager.renameObject(bucketName, tempObjectName, objectName);
          } catch (IOException ex) {
            LOG.error("Failed to rename file " + tempObjectName + " to " + objectName + ". part-number=" + partNumber + ", upload-id=" + uploadId
                + ", object-key=" + objectKey + ", bucket=" + bucketName);
            messenger.removeQueue(key, randomKey);
            throw new InternalErrorException(objectKey);
          }

          lastModified = new Date();

          // Update part or create a new entity if it does not exist
          try (TransactionResource tr = Entities.transactionFor(PartInfo.class)) {
            PartInfo partInfo = null;

            try {
              partInfo = Entities.uniqueResult(new PartInfo(bucketName, objectKey, uploadId, partNumber));
              prevObjectName = partInfo.getObjectName();
            } catch (NoSuchElementException e) {
              // Part entity may not exist, create it now
              partInfo = Entities.persist(PartInfo.generatePart(bucketName, objectKey, uploadId, partNumber, objectName));
            }

            partInfo.setEtag(md5);
            partInfo.setSize(size);
            partInfo.setLastModified(lastModified);
            partInfo.setStorageClass("STANDARD");
            partInfo.setObjectName(objectName);

            tr.commit();
          } catch (Exception e) {
            LOG.error("Failed to update metadata for part-number=" + partNumber + ", upload-id=" + uploadId + ", object-key=" + objectKey
                + ", bucket=" + bucketName, e);
            throw new InternalErrorException("Failed to update metadata for part-number=" + partNumber + ", upload-id=" + uploadId + ", object-key="
                + objectKey + ", bucket=" + bucketName, e);
          }

          // Delete the previously uploaded part on the disk
          if (prevObjectName != null) {
            ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, prevObjectName, null, null);
            Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
          }

          // restart all interrupted puts
          WalrusMonitor monitor = messenger.getMonitor(key);
          synchronized (monitor) {
            monitor.setLastModified(lastModified);
            monitor.setMd5(md5);
            monitor.notifyAll();
          }
          // messenger.removeMonitor(key);
          messenger.clearQueues(key);
          messenger.removeQueue(key, randomKey);
          LOG.trace("Transfer complete: " + key + " uploadId: " + uploadId + " partNumber: " + partNumber);

          break;
        } else {
          assert (WalrusDataMessage.isData(dataMessage));
          byte[] data = dataMessage.getPayload();
          // start writing object (but do not commit yet)
          try {
            if (fileIO != null)
              fileIO.write(data);
          } catch (IOException ex) {
            LOG.error(ex);
          }
          // calculate md5 on the fly
          size += data.length;
          if (digest != null) {
            digest.update(data);
          }
        }
      }
    } catch (InterruptedException e) {
      LOG.error("Transfer interrupted: " + key + "." + randomKey, e);
      throw new InternalErrorException("Transfer interrupted: " + key + "." + randomKey);
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to perform put object for object-key=" + objectKey + ", bucket=" + bucketName, e);
    } finally {
      cleanupTempObject(bucketName, tempObjectName);
      messenger.removeQueue(key, randomKey);
    }

    reply.setSize(size);
    reply.setEtag(md5);
    reply.setLastModified(lastModified);
    return reply;
  }

  public CompleteMultipartUploadResponseType completeMultipartUpload(CompleteMultipartUploadType request) throws WalrusException {
    CompleteMultipartUploadResponseType reply = (CompleteMultipartUploadResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();
    String uploadId = request.getUploadId();
    List<Part> requestParts = request.getParts();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    if (requestParts == null || requestParts.isEmpty()) {
      throw new InvalidArgumentException("List of parts cannot be emtpy");
    }

    PartInfo manifest = null;
    List<PartInfo> foundParts = null;
    long size = 0;
    Date lastModified = null;
    String eTag = new String();
    String eTagString = new String();
    String prevObjectName = null;

    // Look for manifest and other parts
    try (TransactionResource tr = Entities.transactionFor(PartInfo.class)) {
      PartInfo search = new PartInfo(bucketName, objectKey, uploadId);

      Criteria manifestCriteria = Entities.createCriteria(PartInfo.class);
      manifestCriteria.add(Example.create(search));
      manifestCriteria.add(Restrictions.isNull("partNumber"));
      manifestCriteria.add(Restrictions.isNull("cleanup")); // indicates the multipart upload has not been finalized yet
      List<PartInfo> foundManifests = manifestCriteria.list();

      if (foundManifests == null || foundManifests.size() != 1) {
        if (foundManifests == null) {
          throw new InternalErrorException("Got invalid metadata");
        } else if (foundManifests.size() < 1) {
          throw new NoSuchUploadException("Multipart upload ID is invalid");
        } else {
          throw new InternalErrorException("Multiple manifests found for same uploadId");
        }
      } else {
        manifest = foundManifests.get(0);
        if (manifest.getMetaData() != null) {
          manifest.getMetaData().size();
        }
      }

      Criteria partCriteria = Entities.createCriteria(PartInfo.class);
      partCriteria.add(Example.create(search));
      partCriteria.add(Restrictions.isNotNull("partNumber"));
      foundParts = partCriteria.list();

      if (foundParts == null || foundParts.isEmpty()) {
        throw new InvalidPartException("No parts uploaded for upload-Id=" + request.getUploadId());
      } else {
        // keep going
      }

      if (requestParts.size() > foundParts.size()) {
        throw new InvalidArgumentException("Number of parts in the manifest is greater than the number of parts uploaded. Upload Id: "
            + request.getUploadId());
      }

      // Create a hashmap
      Map<Integer, PartInfo> partsMap = new HashMap<Integer, PartInfo>(foundParts.size());
      for (PartInfo foundPart : foundParts) {
        partsMap.put(foundPart.getPartNumber(), foundPart);
      }

      // Mark the parts in the request as valid in database
      PartInfo include = null;
      for (Part requestPart : requestParts) {
        if ((include = partsMap.remove(requestPart.getPartNumber())) != null) {
          eTagString += include.getEtag();
          size += include.getSize();
          include.setCleanup(Boolean.FALSE); // set the part cleanup to false to indicate that part should
        } else {
          throw new InvalidPartException("Part Number: " + requestPart.getPartNumber() + " upload id: " + request.getUploadId());
        }
      }

      // Mark manifest as valid
      manifest.setCleanup(Boolean.FALSE);

      // Mark the remaining parts for cleanup
      for (PartInfo excluded : partsMap.values()) {
        excluded.setCleanup(Boolean.TRUE);
      }

      tr.commit();
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to process part metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to process part metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket="
          + bucketName, e);
    }

    MessageDigest digest = Digest.MD5.get();
    digest.update(eTagString.getBytes());
    eTag = "uuid-" + Hashes.bytesToHex(digest.digest());
    lastModified = new Date();

    // Commit the object
    try (TransactionResource tr = Entities.transactionFor(ObjectInfo.class)) {
      ObjectInfo objectInfo = null;
      try {
        objectInfo = Entities.uniqueResult(new ObjectInfo(bucketName, objectKey));
        prevObjectName = objectInfo.getObjectName();
      } catch (NoSuchElementException e) {
        objectInfo = Entities.persist(new ObjectInfo(bucketName, objectKey));
      }
      objectInfo.setUploadId(uploadId);
      objectInfo.setEtag(eTag);
      objectInfo.setSize(size);
      objectInfo.setLastModified(lastModified);
      objectInfo.setStorageClass(manifest.getStorageClass());
      objectInfo.setContentDisposition(manifest.getContentDisposition());
      objectInfo.setContentType(manifest.getContentType());
      objectInfo.setMetaData(manifest.cloneMetaData());
      objectInfo.setObjectName(null); // set object name to null for mpus
      tr.commit();
    } catch (Exception e) {
      LOG.error("Failed to process metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to process metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket="
          + bucketName, e);
    }

    // fire cleanup
    firePartsCleanupTask(bucketName, objectKey, uploadId);

    // Delete the previously uploaded object on the disk
    if (prevObjectName != null) {
      ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, prevObjectName, null, null);
      Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
    }

    reply.setEtag(eTag); // TODO figure out etag correctly
    reply.setLastModified(lastModified);
    reply.setLocation("WalrusBackend" + bucketName + "/" + objectKey);
    reply.setBucket(bucketName);
    reply.setKey(objectKey);
    reply.setVersionId(WalrusProperties.NULL_VERSION_ID);
    return reply;
  }

  public AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) throws WalrusException {
    AbortMultipartUploadResponseType reply = (AbortMultipartUploadResponseType) request.getReply();

    String bucketName = request.getBucket();
    String objectKey = request.getKey();
    String uploadId = request.getUploadId();

    // Lookup bucket
    try {
      Transactions.find(new BucketInfo(bucketName));
    } catch (NoSuchElementException e) {
      throw new NoSuchBucketException(bucketName);
    } catch (Exception e) {
      LOG.error("Failed to look up metadata for bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to lookup bucket " + bucketName, e);
    }

    PartInfo manifest = null;
    List<PartInfo> foundParts = null;

    // Look for manifest and the existing parts
    try (TransactionResource tr = Entities.transactionFor(PartInfo.class)) {
      PartInfo search = new PartInfo(bucketName, objectKey, uploadId);

      Criteria manifestCriteria = Entities.createCriteria(PartInfo.class);
      manifestCriteria.add(Example.create(search));
      manifestCriteria.add(Restrictions.isNull("partNumber"));
      manifestCriteria.add(Restrictions.isNull("cleanup")); // indicates the multipart upload has not been finalized yet
      List<PartInfo> foundManifests = manifestCriteria.list();
      if (foundManifests == null || foundManifests.size() != 1) {
        if (foundManifests == null) {
          throw new InternalErrorException("Got invalid metadata");
        } else if (foundManifests.size() < 1) {
          throw new NoSuchUploadException("Multipart upload ID is invalid");
        } else {
          throw new InternalErrorException("Multiple manifests found for same uploadId");
        }
      } else {
        manifest = foundManifests.get(0);
        if (manifest.getMetaData() != null) {
          manifest.getMetaData().size();
        }
      }

      Criteria partCriteria = Entities.createCriteria(PartInfo.class);
      partCriteria.add(Example.create(search));
      partCriteria.add(Restrictions.isNotNull("partNumber"));
      foundParts = partCriteria.list();

      // Mark the parts for cleanup
      if (foundParts != null && !foundParts.isEmpty()) {
        for (PartInfo foundPart : foundParts) {
          foundPart.setCleanup(Boolean.TRUE);
        }
      } else {
        // no parts for this upload to clean up
      }

      // Mark the manifest for cleanup
      manifest.setCleanup(Boolean.TRUE);

      tr.commit();
    } catch (WalrusException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to process part metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket=" + bucketName, e);
      throw new InternalErrorException("Failed to process part metadata for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket="
          + bucketName, e);
    }

    // fire cleanup
    firePartsCleanupTask(bucketName, objectKey, uploadId);

    return reply;
  }

  private void firePartsCleanupTask(final String bucketName, final String objectKey, final String uploadId) {
    try {
      Threads.lookup(WalrusBackend.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(new Runnable() {
        public void run() {
          try {
            deleteParts(bucketName, objectKey, uploadId, true);
          } catch (final Throwable f) {
            LOG.error("Error during part cleanup for " + bucketName + "/" + objectKey + " uploadId: " + uploadId, f);
          }
        }
      });
    } catch (final Throwable f) {
      LOG.warn("Error cleaning parts for " + bucketName + "/" + objectKey + " uploadId: " + uploadId + " .", f);
    }
  }

  private void deleteParts(String bucketName, String objectKey, String uploadId, boolean cleanupFlag) {
    PartInfo searchPart = new PartInfo(bucketName, objectKey, uploadId);
    searchPart.setCleanup(cleanupFlag);

    try {
      Entities.asTransaction(PartInfo.class, new Function<PartInfo, String>() {

        @Override
        public String apply(PartInfo arg0) {
          try {
            List<PartInfo> allParts = Entities.query(arg0);

            if (allParts != null && !allParts.isEmpty()) {
              for (PartInfo part : allParts) {
                if (part.getObjectName() != null) {
                  try {
                    storageManager.deleteObject(part.getBucketName(), part.getObjectName());
                  } catch (IOException e) {
                    LOG.warn("Unable to delete file on disk for part-file=" + part.getObjectName() + ", part-number=" + part.getPartNumber()
                        + ", upload-id=" + part.getUploadId() + ", object-key=" + part.getObjectKey() + ", bucket=" + part.getBucketName() + ": "
                        + e.getMessage());
                  }
                }
                Entities.delete(part);
              }
            } else {
              LOG.debug("No parts to delete for upload=" + arg0.getUploadId() + ", object-key=" + arg0.getObjectKey() + ", bucket="
                  + arg0.getBucketName());
            }
          } catch (Exception e) {
            LOG.warn(
                "Failed to cleanup parts for upload-id=" + arg0.getUploadId() + ", object-key=" + arg0.getObjectKey() + ", bucket="
                    + arg0.getBucketName(), e);
          }
          return null;
        }
      }).apply(searchPart);
    } catch (Exception e) {
      LOG.warn("Failed to cleanup parts for upload-id=" + uploadId + ", object-key=" + objectKey + ", bucket=" + bucketName, e);
    }
  }

  /**
   * Clean up <b>ALL</b> parts in the bucket. To be called only when all parts in the bucket need to be deleted, for instance before bucket deletion
   *
   * @param bucketName
   */
  private void deleteParts(String bucketName) {
    PartInfo searchPart = new PartInfo();
    searchPart.setBucketName(bucketName);

    try {
      Entities.asTransaction(PartInfo.class, new Function<PartInfo, String>() {

        @Override
        public String apply(PartInfo arg0) {
          try {
            List<PartInfo> allParts = Entities.query(arg0);

            if (allParts != null && !allParts.isEmpty()) {
              for (PartInfo part : allParts) {
                if (part.getObjectName() != null) {
                  try {
                    storageManager.deleteObject(part.getBucketName(), part.getObjectName());
                  } catch (IOException e) {
                    LOG.warn("Unable to delete file on disk for part-file=" + part.getObjectName() + ", part-number=" + part.getPartNumber()
                        + ", upload-id=" + part.getUploadId() + ", object-key=" + part.getObjectKey() + ", bucket=" + part.getBucketName()
                        + ". cause: " + e.getMessage());
                  }
                }
                Entities.delete(part);
              }
            } else {
              LOG.debug("No parts to delete for bucket=" + arg0.getBucketName());
            }
          } catch (Exception e) {
            LOG.warn("Failed to cleanup parts for bucket=" + arg0.getBucketName(), e);
          }
          return null;
        }
      }).apply(searchPart);
    } catch (Exception e) {
      LOG.warn("Failed to cleanup parts for bucket=" + searchPart.getBucketName(), e);
    }
  }

  /**
   * Utility method for fetching ordered listing of parts that make up an object. Use this method against objects that have been previously created
   * and persisted to the database.
   *
   * @param objectInfo
   * @return
   * @throws InternalErrorException
   */
  private List<PartInfo> getOrderedListOfParts(ObjectInfo objectInfo) throws InternalErrorException {

    if (objectInfo != null && !Strings.isNullOrEmpty(objectInfo.getUploadId())) {
      PartInfo searchPart = new PartInfo(objectInfo.getBucketName(), objectInfo.getObjectKey(), objectInfo.getUploadId());
      searchPart.setCleanup(Boolean.FALSE);
      List<PartInfo> parts = null;
      try (TransactionResource tr = Entities.transactionFor(PartInfo.class)) {
        Criteria partCriteria = Entities.createCriteria(PartInfo.class);
        partCriteria.setReadOnly(true);
        partCriteria.add(Example.create(searchPart));
        partCriteria.add(Restrictions.isNotNull("partNumber"));
        partCriteria.addOrder(Order.asc("partNumber"));
        parts = partCriteria.list();
        tr.commit();

        if (parts == null || parts.isEmpty()) {
          throw new InternalErrorException("No parts found for object " + objectInfo.getObjectKey());
        }
        return parts;
      }
    } else {
      throw new InternalErrorException("Object may not be uploaded using multipart upload");
    }
  }

  private static AccessControlPolicy getPrivateACP( CanonicalUser canonicalUser ) {
    AccessControlList accessControlList = new AccessControlList();
    ArrayList<Grant> grants = getPrivateGrants( canonicalUser.getID( ), canonicalUser.getDisplayName( ) );
    accessControlList.setGrants(grants);
    return new AccessControlPolicy( canonicalUser, accessControlList);
  }

  private static ArrayList<Grant> getPrivateGrants(String canonicalId, String displayName) {
    ArrayList<Grant> grants = new ArrayList<Grant>();
    grants.add(new Grant(new Grantee(new CanonicalUser(canonicalId, displayName)), WalrusProperties.Permission.FULL_CONTROL.toString()));
    return grants;
  }

  private static CanonicalUser buildCanonicalUser( final UserPrincipal user ) throws AuthException {
    return new CanonicalUser(user.getCanonicalId(), user.getAccountAlias());
  }
}
