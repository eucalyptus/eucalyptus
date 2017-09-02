/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.Checker;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.LocationInfo;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.exceptions.AccessDeniedException;
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
import com.eucalyptus.walrus.storage.FileSystemStorageManager;
import com.eucalyptus.walrus.util.WalrusProperties;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import edu.ucsb.eucalyptus.util.SystemUtil;

@ComponentNamed
public class WalrusControl implements WalrusService {

  private static Logger LOG = Logger.getLogger(WalrusControl.class);

  private static StorageManager storageManager;
  private static WalrusManager walrusManager;

  public static void checkPreconditions() throws EucalyptusCloudException {
  }

  public static void configure() {
    WalrusInfo walrusInfo = WalrusInfo.getWalrusInfo();
    try {
      storageManager = new FileSystemStorageManager();
    } catch (Exception ex) {
      LOG.error(ex);
    }
    walrusManager = new WalrusFSManager(storageManager);
    WalrusManager.configure();
    try {
      walrusManager.check();
    } catch (EucalyptusCloudException ex) {
      LOG.error("Error initializing walrus", ex);
      SystemUtil.shutdownWithError(ex.getMessage());
    }

    try {
      if (storageManager != null) {
        storageManager.start();
      }
    } catch (EucalyptusCloudException ex) {
      LOG.error("Error starting storage backend: " + ex);
    }

    // Implementation for EUCA-3583. Check for available space in Walrus bukkits directory and throw a fault when less than 10% of total space is
    // available
    try {
      ScheduledFuture<?> future =
          DiskResourceCheck.start(new Checker(new LocationInfo(new File(WalrusInfo.getWalrusInfo().getStorageDir()), 10.0), WalrusBackend.class,
              (long) 300000));
    } catch (Exception ex) {
      LOG.error("Error starting disk space check for WalrusBackend storage directory.", ex);
    }
  }

  public WalrusControl() {}

  public static void enable() throws EucalyptusCloudException {
    storageManager.enable();
  }

  public static void disable() throws EucalyptusCloudException {
    storageManager.disable();
  }

  public static void check() throws EucalyptusCloudException {
    storageManager.check();
  }

  public static void stop() throws EucalyptusCloudException {
    storageManager.stop();
    storageManager = null;
    walrusManager = null;
  }

  /**
   * Ensure that only admin can perform action. Walrus is internal-only.
   * 
   * @throws AccessDeniedException
   */
  protected void checkPermissions() throws AccessDeniedException {
    final Context context = Contexts.lookup( );
    if ( !context.hasAdministrativePrivileges( ) &&
        !AccountIdentifiers.OBJECT_STORAGE_WALRUS_ACCOUNT.equals( context.getAccountAlias( ) )  ) {
      throw new AccessDeniedException("Service", "WalrusBackend");
    }
  }

  @Override
  public HeadBucketResponseType HeadBucket( HeadBucketType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.headBucket(request);
  }

  @Override
  public CreateBucketResponseType CreateBucket( CreateBucketType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.createBucket(request);
  }

  @Override
  public DeleteBucketResponseType DeleteBucket( DeleteBucketType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.deleteBucket(request);
  }

  @Override
  public ListAllMyBucketsResponseType ListAllMyBuckets( ListAllMyBucketsType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.listAllMyBuckets(request);
  }

  @Override
  public PutObjectResponseType PutObject( PutObjectType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.putObject(request);
  }

  @Override
  public PutObjectInlineResponseType PutObjectInline( PutObjectInlineType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.putObjectInline(request);
  }

  @Override
  public DeleteObjectResponseType DeleteObject( DeleteObjectType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.deleteObject(request);
  }

  @Override
  public ListBucketResponseType ListBucket( ListBucketType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.listBucket(request);
  }

  @Override
  public GetObjectAccessControlPolicyResponseType GetObjectAccessControlPolicy( GetObjectAccessControlPolicyType request )
      throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.getObjectAccessControlPolicy(request);
  }

  @Override
  public GetObjectResponseType GetObject( GetObjectType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.getObject(request);
  }

  @Override
  public GetObjectExtendedResponseType GetObjectExtended( GetObjectExtendedType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.getObjectExtended(request);
  }

  @Override
  public CopyObjectResponseType CopyObject( CopyObjectType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.copyObject(request);
  }

  @Override
  public InitiateMultipartUploadResponseType InitiateMultipartUpload( InitiateMultipartUploadType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.initiateMultipartUpload(request);
  }

  @Override
  public UploadPartResponseType UploadPart( UploadPartType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.uploadPart(request);
  }

  @Override
  public CompleteMultipartUploadResponseType CompleteMultipartUpload( CompleteMultipartUploadType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.completeMultipartUpload(request);
  }

  @Override
  public AbortMultipartUploadResponseType AbortMultipartUpload( AbortMultipartUploadType request ) throws EucalyptusCloudException {
    checkPermissions();
    return walrusManager.abortMultipartUpload(request);
  }
}
