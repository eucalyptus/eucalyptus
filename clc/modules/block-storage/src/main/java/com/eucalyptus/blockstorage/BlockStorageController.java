/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2017 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.org.apache.tools.ant.util.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.async.ExpiredSnapshotCleaner;
import com.eucalyptus.blockstorage.async.ExpiredVolumeCleaner;
import com.eucalyptus.blockstorage.async.FailedSnapshotCleaner;
import com.eucalyptus.blockstorage.async.FailedVolumeCleaner;
import com.eucalyptus.blockstorage.async.SnapshotCreator;
import com.eucalyptus.blockstorage.async.SnapshotDeleter;
import com.eucalyptus.blockstorage.async.SnapshotTransferCleaner;
import com.eucalyptus.blockstorage.async.ThreadPoolSizeUpdater;
import com.eucalyptus.blockstorage.async.VolumeCreator;
import com.eucalyptus.blockstorage.async.VolumeDeleter;
import com.eucalyptus.blockstorage.async.VolumeStateChecker;
import com.eucalyptus.blockstorage.async.VolumesConvertor;
import com.eucalyptus.blockstorage.entities.BlockStorageGlobalConfiguration;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.entities.VolumeExportRecord;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.entities.VolumeToken;
import com.eucalyptus.blockstorage.exceptions.AccessDeniedException;
import com.eucalyptus.blockstorage.exceptions.SnapshotNotFoundException;
import com.eucalyptus.blockstorage.exceptions.SnapshotTooLargeException;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.msgs.StorageSnapshot;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationType;
import com.eucalyptus.blockstorage.threadpool.CheckerThreadPool;
import com.eucalyptus.blockstorage.threadpool.SnapshotThreadPool;
import com.eucalyptus.blockstorage.threadpool.SnapshotTransferThreadPool;
import com.eucalyptus.blockstorage.threadpool.VolumeThreadPool;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import edu.ucsb.eucalyptus.cloud.InvalidParameterValueException;
import edu.ucsb.eucalyptus.cloud.NoSuchVolumeException;
import edu.ucsb.eucalyptus.cloud.SnapshotInUseException;
import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.VolumeNotReadyException;
import edu.ucsb.eucalyptus.cloud.VolumeSizeExceededException;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

@ComponentNamed
public class BlockStorageController implements BlockStorageService {
  private static Logger LOG = Logger.getLogger(BlockStorageController.class);

  static LogicalStorageManager blockManager;

  static final int SNAPSHOT_POINT_DEFAULT_CONCURRENCY = 3;

  static Supplier<Semaphore> snapshotPointSemaphoreAllocator = new Supplier<Semaphore>() {
    @Override
    public Semaphore get( ) {
      Integer maxConcurrentSnapshots = StorageInfo.getStorageInfo().getMaxConcurrentSnapshots();
      int snapshotPointSemaphorePermits = 
          (maxConcurrentSnapshots == null ? SNAPSHOT_POINT_DEFAULT_CONCURRENCY : maxConcurrentSnapshots);
      LOG.debug("Snapshot point semaphore created with " + snapshotPointSemaphorePermits + " permits.");
      return new Semaphore(snapshotPointSemaphorePermits);
    }
  };
  
  static Supplier<Semaphore> snapshotPointSemaphoreSupplier = Suppliers.memoize(snapshotPointSemaphoreAllocator);
  
  
  private static Function<String, SnapshotInfo> SNAPSHOT_FAILED = new Function<String, SnapshotInfo>() {

    @Override
    public SnapshotInfo apply(String arg0) {
      SnapshotInfo snap;
      try {
        snap = Entities.uniqueResult(new SnapshotInfo(arg0));
        snap.setStatus(StorageProperties.Status.failed.toString());
        snap.setProgress("0");
        LOG.debug("Snapshot " + arg0 + " set to 'failed' state");
        return snap;
      } catch (TransactionException | NoSuchElementException e) {
        LOG.warn("Failed to retrieve snapshot entity from DB for " + arg0, e);
      }
      return null;
    }
  };

  // TODO: zhill, this can be added later for snapshot abort capabilities
  // static ConcurrentHashMap<String,HttpTransfer> httpTransferMap; //To keep track of current transfers to support aborting

  // Introduced for testing EUCA-9297 fix: allows artificial capacity changes of backend
  static boolean setUseTestingDelegateManager(boolean enableDelegate) {
    if (enableDelegate && !(blockManager instanceof StorageManagerTestingProxy)) {
      LOG.info("Switching to use delegating storage manager for testing");
      blockManager = new StorageManagerTestingProxy(blockManager);
    } else if (!enableDelegate && (blockManager instanceof StorageManagerTestingProxy)) {
      LOG.info("Switching to NOT use delegating storage manager anymore");
      blockManager = ((StorageManagerTestingProxy) blockManager).getDelegateStorageManager();
    }
    return enableDelegate;
  }

  public static void configure() throws EucalyptusCloudException {
    BlockStorageGlobalConfiguration.getInstance();
    StorageProperties.updateWalrusUrl();
    StorageProperties.updateName();
    StorageProperties.updateStorageHost();

    try {
      blockManager = StorageManagers.getInstance();
      if (blockManager != null) {
        blockManager.initialize();
      } else {
        throw new EucalyptusCloudException("Got null block manager. Cannot configure.");
      }
    } catch (Exception e) {
      throw new EucalyptusCloudException(e);
    }
  }

  public BlockStorageController() {}

  // for unit testing with a mock implementation
  public BlockStorageController(LogicalStorageManager blockManager) {
    this.blockManager = blockManager;
  }

  public static void check() throws EucalyptusCloudException {
    blockManager.checkReady();
  }

  public static void stop() throws EucalyptusCloudException {
    // Shutdown volume, snapshot, snapshot transfer and checker thread pools
    VolumeThreadPool.shutdown();
    SnapshotThreadPool.shutdown();
    SnapshotTransferThreadPool.shutdown();
    CheckerThreadPool.shutdown();

    if (blockManager != null) {
      LOG.info("Stopping blockmanager");
      blockManager.stop();
    }

    // clean all state.
    blockManager = null;

    StorageProperties.enableSnapshots = false; // swathi: what does this mean?
  }

  public static void enable() throws EucalyptusCloudException {
    blockManager.configure();
    // blockManager.initialize();
    blockManager.enable();

    // run startup checks
    // changing this from async to sync execution so it does not accidentally pick up any new volumes/snapshots
    runStartUpChecks();

    // Initialize volume, snapshot, snapshot transfer and checker thread pools
    StorageInfo info = StorageInfo.getStorageInfo();
    VolumeThreadPool.initialize(info.getMaxConcurrentVolumes());
    SnapshotThreadPool.initialize(info.getMaxConcurrentSnapshots());
    SnapshotTransferThreadPool.initialize(info.getMaxConcurrentSnapshotTransfers());
    CheckerThreadPool.initialize();

    // Add checkers for volume and snapshot maintenance
    CheckerThreadPool.add(new VolumeDeleter(blockManager));
    CheckerThreadPool.add(new FailedVolumeCleaner(blockManager));
    CheckerThreadPool.add(new ExpiredVolumeCleaner());
    CheckerThreadPool.add(new VolumeStateChecker(blockManager)); // TODO What is the point of this checker?
    CheckerThreadPool.add(new SnapshotDeleter(blockManager));
    CheckerThreadPool.add(new FailedSnapshotCleaner(blockManager));
    CheckerThreadPool.add(new ExpiredSnapshotCleaner());
    CheckerThreadPool.add(new SnapshotTransferCleaner());
    CheckerThreadPool.add(new ThreadPoolSizeUpdater());
    // add any block manager checkers
    List<CheckerTask> backendCheckers = null;
    if ((backendCheckers = blockManager.getCheckers()) != null && !backendCheckers.isEmpty()) {
      for (CheckerTask checker : backendCheckers) {
        CheckerThreadPool.add(checker);
      }
    }

    // TODO ask neil what this means
    StorageProperties.enableSnapshots = StorageProperties.enableStorage = true;
  }

  public static void disable() throws EucalyptusCloudException {
    // Shutdown volume, snapshot, snapshot transfer and checker thread pools
    VolumeThreadPool.shutdown();
    SnapshotThreadPool.shutdown();
    SnapshotTransferThreadPool.shutdown();
    CheckerThreadPool.shutdown();

    blockManager.disable();
  }

  private static void runStartUpChecks() {
    try {
      LOG.info("Initiating startup checks for block storage");
      updateStuckVolumes();
      updateStuckSnapshots();
    } catch (Exception e) {
      LOG.error("Startup cleanup failed", e);
    }

    try {
      blockManager.startupChecks();
    } catch (EucalyptusCloudException e) {
      LOG.error("Startup checks failed");
    }
  }

  public static void updateStuckVolumes() {
    LOG.info("Initiating clean up of stuck EBS volumes");
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo volumeInfo = new VolumeInfo();
      volumeInfo.setStatus(StorageProperties.Status.creating.toString());
      List<VolumeInfo> volumesInCreating = Entities.query(volumeInfo, false);
      if (volumesInCreating != null && !volumesInCreating.isEmpty()) {
        for (VolumeInfo volInfo : volumesInCreating) {
          // Mark them as failed so that it gets reflected in the CLC
          // and the clean up routine picks them up later
          volInfo.setStatus(StorageProperties.Status.failed.toString());
        }
      } else {
        LOG.info("No stuck EBS volumes found. No clean up needed");
      }
      tran.commit();
    } catch (Throwable e) {
      LOG.warn("Failed to clean up stuck EBS volumes", e);
    }
  }

  public static void updateStuckSnapshots() {
    LOG.info("Initiating clean up of stuck EBS snapshots");
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo snapshotInfo = new SnapshotInfo();
      snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
      List<SnapshotInfo> snapshotsInCreating = Entities.query(snapshotInfo, false);
      if (snapshotsInCreating != null && !snapshotsInCreating.isEmpty()) {
        for (SnapshotInfo snapInfo : snapshotsInCreating) {
          // Mark them as failed so that it gets reflected in the CLC
          // and the clean up routine picks them up later
          snapInfo.setStatus(StorageProperties.Status.failed.toString());
          snapInfo.setProgress("0");
        }
      } else {
        LOG.info("No stuck EBS snapshots found. No clean up needed");
      }
      tran.commit();
    } catch (Throwable e) {
      LOG.warn("Failed to clean up stuck EBS snapshots", e);
    }
  }

  @Override
  public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) throws EucalyptusCloudException {
    UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
    if (ComponentIds.lookup(Eucalyptus.class).name().equals(request.getEffectiveUserId()))
      throw new AccessDeniedException("Only admin can change walrus properties.");
    // test connection to ObjectStorage
    StorageProperties.updateWalrusUrl();
    try {
      blockManager.checkPreconditions();
      StorageProperties.enableStorage = true;
    } catch (Exception ex) {
      StorageProperties.enableStorage = false;
      LOG.error(ex);
    }
    if (request.getStorageParams() != null) {
      for (ComponentProperty param : request.getStorageParams()) {
        LOG.debug("Storage Param: " + param.getDisplayName() + " Qname: " + param.getQualifiedName() + " Value: " + param.getValue());
      }
      blockManager.setStorageProps(request.getStorageParams());
    }
    return reply;
  }

  @Override
  public GetStorageConfigurationResponseType GetStorageConfiguration(GetStorageConfigurationType request) throws EucalyptusCloudException {
    GetStorageConfigurationResponseType reply = (GetStorageConfigurationResponseType) request.getReply();
    StorageProperties.updateName();
    if (ComponentIds.lookup(Eucalyptus.class).name().equals(request.getEffectiveUserId()))
      throw new AccessDeniedException("Only admin can change walrus properties.");
    if (StorageProperties.NAME.equals(request.getName())) {
      reply.setName(StorageProperties.NAME);
      ArrayList<ComponentProperty> storageParams = blockManager.getStorageProps();
      reply.setStorageParams(storageParams);
    }
    return reply;
  }

  @Override
  public GetVolumeTokenResponseType GetVolumeToken(GetVolumeTokenType request) throws EucalyptusCloudException {
    GetVolumeTokenResponseType reply = (GetVolumeTokenResponseType) request.getReply();
    String volumeId = request.getVolumeId();
    LOG.info("Processing GetVolumeToken request for volume " + volumeId);

    if (null == volumeId) {
      LOG.error("Cannot get token for a null-valued volumeId");
      throw new EucalyptusCloudException("No volumeId specified in token request");
    }

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo vol = Entities.uniqueResult(new VolumeInfo(volumeId));
      VolumeToken token = vol.getOrCreateAttachmentToken();

      // Encrypt the token with the NC's private key
      String encryptedToken = BlockStorageUtil.encryptForNode(token.getToken(), BlockStorageUtil.getPartitionForLocalService(Storage.class));
      reply.setToken(encryptedToken);
      reply.setVolumeId(volumeId);
      tran.commit();
      LOG.debug(reply.toSimpleString());
      return reply;
    } catch (NoSuchElementException e) {
      throw new EucalyptusCloudException("Volume " + request.getVolumeId() + " not found", e);
    } catch (Exception e) {
      LOG.error("Failed to get volume token: " + e.getMessage());
      throw new EucalyptusCloudException("Could not get volume token for volume " + request.getVolumeId(), e);
    }
  }

  /**
   * Removes connection authorization for the specified iqn/ip pair in the request using the specified token. Only performs the operation if the token
   * is valid for the specified volume.
   *
   * Invalidates the token upon successful de-authorization.
   * 
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  @Override
  public UnexportVolumeResponseType UnexportVolume(UnexportVolumeType request) throws EucalyptusCloudException {
    final long startTime = System.currentTimeMillis();
    UnexportVolumeResponseType reply = request.getReply();
    final String token = request.getToken();
    final String volumeId = request.getVolumeId();
    final String nodeIqn = request.getIqn();
    final String nodeIp = request.getIp();

    LOG.info("Processing UnexportVolume request for volume " + volumeId + " from node " + nodeIp + " with iqn " + nodeIqn);

    VolumeInfo volumeEntity = null;
    VolumeToken validToken = null;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo foundVolume = Entities.uniqueResult(new VolumeInfo(volumeId));
      volumeEntity = Entities.merge(foundVolume);

      try {
        validToken = volumeEntity.getAttachmentTokenIfValid(token);
        // volumeEntity.invalidateExport(tokenValue, nodeIp, nodeIqn);
        // Entities.flush(volumeEntity); //Sync state -- not needed.... same transaction
      } catch (Exception e) {
        LOG.error("Invalid token in request for volume " + volumeId + ". Encrypted token: " + token);
        throw new EucalyptusCloudException(e);
      }

      try {
        blockManager.unexportVolume(volumeEntity.getVolumeId(), nodeIqn);
      } catch (UnsupportedOperationException e) { // The backend doesn't support unexport to just one host
        // Check to see if the volume is exported to any other hosts
        if (validToken.hasOnlyExport(nodeIp, nodeIqn)) {
          // Either volume is exported to this host only or has no active exports , so unexport all.
          blockManager.unexportVolumeFromAll(volumeId);
        } else {
          // Volume may be exported to other hosts... this is a noop.
          LOG.info("Volume " + volumeId + ": UnexportVolume for single host not supported by backend. Treating as no-op and continuing normally.");
        }
      } catch (Exception e) {
        LOG.error("Could not detach volume: " + volumeId, e);
        throw e;
      }

      // Do the actual invalidation. Handle retries, but only on the DB part.
      if (!Entities.asTransaction(VolumeInfo.class, new Function<VolumeInfo, Boolean>() {
        @Override
        public Boolean apply(VolumeInfo vol) {
          VolumeInfo entity = Entities.merge(vol);
          try {
            entity.invalidateExport(token, nodeIp, nodeIqn);
            return true;
          } catch (Exception e) {
            LOG.error("Error invalidating export: " + e.getMessage());
            return false;
          }
        }
      }).apply(volumeEntity)) {
        // Transaction failed after retries...
        LOG.error("Error invalidating the export record in the DB for volume " + volumeId);
      }

      tran.commit();
      reply.set_return(true);
    } catch (NoSuchElementException e) {
      LOG.error("Volume " + volumeId + " not found in DB", e);
      throw new EucalyptusCloudException("Volume " + volumeId + " not found");
    } catch (Exception e) {
      LOG.error("Failed UnexportVolume due to: " + e.getMessage(), e);
      throw new EucalyptusCloudException(e);
    }
    ThruputMetrics.addDataPoint(MonitoredAction.UNEXPORT_VOLUME, System.currentTimeMillis() - startTime);
    return reply;
  }

  /**
   * Perform a volume export validated by the token presented in the request. Upon completion of the Export operation, the identified host (by ip and
   * iqn) will have access to connect to the requested volume. No connection is made, just the authorization.
   *
   * If a valid export record exists for the given token and host information, then the connectionString for that record is returned rather than
   * creating a new record.
   * 
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  @Override
  public ExportVolumeResponseType ExportVolume(ExportVolumeType request) throws EucalyptusCloudException {
    final long startTime = System.currentTimeMillis();
    final ExportVolumeResponseType reply = (ExportVolumeResponseType) request.getReply();
    final String volumeId = request.getVolumeId();
    final String token = request.getToken();
    final String ip = request.getIp();
    final String iqn = request.getIqn();
    reply.setVolumeId(volumeId);

    LOG.info("Processing ExportVolume request for volume " + volumeId);

    final Function<VolumeInfo, String> exportAndAttach = new Function<VolumeInfo, String>() {
      @Override
      public String apply(VolumeInfo volume) {
        int tokenRetry = 3;
        VolumeToken tokenInfo = null;
        VolumeInfo volEntity = Entities.merge(volume);
        for (int i = 0; i < tokenRetry; i++) {
          try {
            tokenInfo = volEntity.getAttachmentTokenIfValid(token);
            if (tokenInfo != null) {
              break;
            }
          } catch (Exception e) {
            LOG.warn("Could not check for valid token. Will retry. ", e);
            tokenInfo = null;
          }
          try {
            Thread.sleep(100); // sleep 100ms to make retry useful.
          } catch (InterruptedException e) {
            throw new RuntimeException("Token check backoff sleep interrupted", e);
          }
        }

        if (tokenInfo == null) {
          throw new RuntimeException("Cannot export, due to invalid token");
        }

        VolumeExportRecord export = null;
        try {
          export = tokenInfo.getValidExport(ip, iqn);
        } catch (EucalyptusCloudException e2) {
          LOG.error("Failed when checking/getting valid export for " + ip + " - " + iqn);
          return null;
        }

        if (export == null) {
          String connectionString = null;
          try {
            // attachVolume must be idempotent.
            connectionString = blockManager.exportVolume(volumeId, iqn);
          } catch (Exception e) {
            LOG.error("Could not attach volume: " + e.getMessage());
            LOG.trace("Failed volume attach", e);
            return null;
          }

          try {
            // addExport must be idempotent, if one exists a new is not added with same data
            tokenInfo.addExport(ip, iqn, connectionString);
            return connectionString;
          } catch (Exception e) {
            LOG.error("Could not export volume " + volumeId + " failed to add export record");
            try {
              LOG.info("Unexporting volume " + volumeId + " to " + iqn + " for export failure cleanup");
              blockManager.unexportVolume(volumeId, iqn);
            } catch (EucalyptusCloudException e1) {
              LOG.error("Failed to detach volume during invalidation failure", e);
            }
            return null;
          }
        } else {
          LOG.debug("Found extant valid export for " + ip + " and " + iqn + " returning connection information for that export");
          return export.getConnectionString();
        }
      }
    };

    VolumeInfo searchVol = new VolumeInfo(volumeId);
    VolumeInfo vol = null;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      vol = Entities.uniqueResult(searchVol);
      tran.commit();
    } catch (NoSuchElementException e) {
      LOG.error("No volume db record found for " + volumeId, e);
      throw new EucalyptusCloudException("Volume not found " + volumeId);
    } catch (TransactionException e) {
      LOG.error("Failed to Export due to db error", e);
      throw new EucalyptusCloudException("Could not export volume", e);
    }

    // Do the export
    try {
      String connectionString = Entities.asTransaction(VolumeInfo.class, exportAndAttach).apply(vol);
      if (connectionString != null) {
        reply.setConnectionString(connectionString);
      } else {
        throw new Exception("Got null record result. Cannot set connection string");
      }
    } catch (Exception e) {
      LOG.error("Failed ExportVolume transaction due to: " + e.getMessage(), e);
      throw new EucalyptusCloudException("Failed to add export", e);
    }
    ThruputMetrics.addDataPoint(MonitoredAction.EXPORT_VOLUME, System.currentTimeMillis() - startTime);
    return reply;
  }

  @Override
  public GetStorageVolumeResponseType GetStorageVolume(GetStorageVolumeType request) throws EucalyptusCloudException {
    GetStorageVolumeResponseType reply = (GetStorageVolumeResponseType) request.getReply();
    if (!StorageProperties.enableStorage) {
      LOG.error("BlockStorage has been disabled. Please check your setup");
      return reply;
    }

    String volumeId = request.getVolumeId();
    LOG.info("Processing GetStorageVolume request for volume " + volumeId);

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo volumeInfo = new VolumeInfo();
      volumeInfo.setVolumeId(volumeId);
      List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
      if (volumeInfos.size() > 0) {
        VolumeInfo foundVolumeInfo = volumeInfos.get(0);
        String deviceName = blockManager.getVolumeConnectionString(volumeId);
        reply.setVolumeId(foundVolumeInfo.getVolumeId());
        reply.setSize(foundVolumeInfo.getSize().toString());
        reply.setStatus(foundVolumeInfo.getStatus());
        reply.setSnapshotId(foundVolumeInfo.getSnapshotId());
        if (deviceName != null)
          reply.setActualDeviceName(deviceName);
        else
          reply.setActualDeviceName("invalid");
      } else {
        throw new NoSuchVolumeException(volumeId);
      }
      tran.commit();
    }
    return reply;
  }

  @Override
  public DeleteStorageVolumeResponseType DeleteStorageVolume(DeleteStorageVolumeType request) throws EucalyptusCloudException {
    final long startTime = System.currentTimeMillis();
    DeleteStorageVolumeResponseType reply = (DeleteStorageVolumeResponseType) request.getReply();
    if (!StorageProperties.enableStorage) {
      LOG.error("BlockStorage has been disabled. Please check your setup");
      return reply;
    }
    String volumeId = request.getVolumeId();
    LOG.info("Processing DeleteStorageVolume request for volume " + volumeId);

    VolumeInfo volumeInfo = new VolumeInfo();
    volumeInfo.setVolumeId(volumeId);
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo foundVolume = Entities.uniqueResult(volumeInfo);
      // check its status
      String status = foundVolume.getStatus();
      if (status == null) {
        throw new EucalyptusCloudException("Invalid volume status: null");
      } else if (status.equals(StorageProperties.Status.available.toString())) {
        // Set status, for cleanup thread to find.
        LOG.trace("Marking volume " + volumeId + " for deletion");
        ThruputMetrics.startOperation(MonitoredAction.DELETE_VOLUME, volumeId, startTime);
        foundVolume.setStatus(StorageProperties.Status.deleting.toString());
      } else if (status.equals(StorageProperties.Status.deleting.toString()) || status.equals(StorageProperties.Status.deleted.toString())
          || status.equals(StorageProperties.Status.failed.toString())) {
        LOG.debug("Volume " + volumeId + " already in deleting/deleted/failed. No-op for delete request.");
      } else {
        throw new EucalyptusCloudException("Cannot delete volume in state: " + status + ". Please retry later");
      }
      // Delete operation should be idempotent as multiple attempts can be made to delete the same volume
      // Set the response element to true if the volume entity is found. EUCA-6093
      reply.set_return(Boolean.TRUE);
      tran.commit();
    } catch (NoSuchElementException e) {
      // Set the response element to false if the volume entity does not exist in the SC database
      // if record is not found, delete is idempotent
      LOG.warn("Got delete request, but unable to find volume in SC database: " + volumeId);
      reply.set_return(Boolean.TRUE);
    } catch (EucalyptusCloudException e) {
      LOG.error("Error marking volume " + volumeId + " for deletion: " + e.getMessage());
      throw e;
    } catch (final Throwable e) {
      LOG.error("Exception looking up volume: " + volumeId, e);
      throw new EucalyptusCloudException(e);
    }
    return reply;
  }

  /**
   * Checks to see if a new snapshot of size volSize will exceed the quota
   * 
   * @param volSize
   * @param maxSize
   * @return
   */
  private boolean totalSnapshotSizeLimitExceeded(String snapshotId, int volSize, int sizeLimitGB) throws EucalyptusCloudException {

    int totalSnapshotSize = 0;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Criteria query = Entities.createCriteria(SnapshotInfo.class);
      query.setReadOnly(true);

      // Only look for snaps that are not failed nor deleted
      ImmutableSet<String> excludedStates = ImmutableSet.of(StorageProperties.Status.failed.toString(), 
          StorageProperties.Status.deleted.toString(), StorageProperties.Status.deletedfromebs.toString());

      query.add(Restrictions.not(Restrictions.in("status", excludedStates)));

      // The listing may include duplicates (for snapshots cached on multiple clusters), this set ensures each unique snap id is counted only once.
      HashSet<String> idSet = new HashSet<String>();
      List<SnapshotInfo> snapshots = (List<SnapshotInfo>) query.list();
      tran.commit();
      for (SnapshotInfo snap : snapshots) {
        totalSnapshotSize += (snap.getSizeGb() != null && idSet.add(snap.getSnapshotId()) ? snap.getSizeGb() : 0);
      }
      LOG.debug("Snapshot " + snapshotId + " checking snapshot total size of  " + totalSnapshotSize + " against limit of " + sizeLimitGB);
      return (totalSnapshotSize + volSize) > sizeLimitGB;
    } catch (final Throwable e) {
      LOG.error("Error finding total snapshot used size " + e.getMessage());
      throw new EucalyptusCloudException("Failed to check snapshot total size limit", e);
    }
  }

  @Override
  public CreateStorageSnapshotResponseType CreateStorageSnapshot(CreateStorageSnapshotType request) throws EucalyptusCloudException {
    final long actionStart = System.currentTimeMillis();
    CreateStorageSnapshotResponseType reply = (CreateStorageSnapshotResponseType) request.getReply();

    StorageProperties.updateWalrusUrl();
    if (!StorageProperties.enableSnapshots) {
      LOG.error("Snapshots have been disabled. Please check connection to ObjectStorage.");
      return reply;
    }

    String volumeId = request.getVolumeId();
    LOG.info("Processing CreateStorageSnapshot request for volume " + volumeId);

    String snapshotId = request.getSnapshotId();
    VolumeInfo sourceVolumeInfo = null;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo volumeInfo = new VolumeInfo(volumeId);
      sourceVolumeInfo = Entities.uniqueResult(volumeInfo);
      tran.commit();
    } catch (NoSuchElementException e) {
      LOG.debug("Volume " + volumeId + " not found in db");
      throw new NoSuchVolumeException(volumeId);
    } catch (final Throwable e) {
      LOG.warn("Volume " + volumeId + " error getting info from db. May not exist. " + e.getMessage());
      throw new EucalyptusCloudException("Could not get volume information for volume " + volumeId, e);
    }

    if (sourceVolumeInfo == null) {
      // Another check to be sure that we have the source volume
      throw new NoSuchVolumeException(volumeId);
    } else {
      // check status
      if (!sourceVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
        throw new VolumeNotReadyException(volumeId);
      } else {
        ThruputMetrics.startOperation(MonitoredAction.CREATE_SNAPSHOT, snapshotId, actionStart);
        // create snapshot
        if (StorageProperties.shouldEnforceUsageLimits) {
          int maxSize = -1;
          try {
            maxSize = BlockStorageGlobalConfiguration.getInstance().getGlobal_total_snapshot_size_limit_gb();
          } catch (Exception e) {
            LOG.error("Could not determine max global snapshot limit. Aborting snapshot creation", e);
            throw new EucalyptusCloudException("Total size limit not found.", e);
          }
          if (maxSize <= 0) {
            LOG.warn("Total global snapshot size limit is less than or equal to 0");
            throw new EucalyptusCloudException("Total snapshot size limit is less than or equal to 0");
          }
          if (totalSnapshotSizeLimitExceeded(snapshotId, sourceVolumeInfo.getSize(), maxSize)) {
            LOG.info("Snapshot " + snapshotId + " exceeds global total snapshot size limit of " + maxSize
                + "GB (see storage.global_total_snapshot_size_limit_gb configurable property)");
            throw new SnapshotTooLargeException(maxSize);
          }
        }

        SnapshotCreator snapshotter = null;
        SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
        try {
          snapshotInfo.setUserName(sourceVolumeInfo.getUserName());
          snapshotInfo.setVolumeId(volumeId);
          snapshotInfo.setProgress("0");
          snapshotInfo.setSizeGb(sourceVolumeInfo.getSize());
          snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
          LOG.debug("Snapshot " + snapshotId + " set to 'creating' state");
          snapshotInfo.setIsOrigin(Boolean.TRUE);

          /* Change to support sync snap consistency point set on CLC round-trip */
          /*
           * Always do this operation. On backends that don't support it they will return null. In that case it is effectively a no-op and we continue
           * normal async snapshot.
           * 
           * If the snap point is set, then we update the DB properly.
           */
          String snapPointId = null;
          try {
            // Only allow 'n' snapshot point creation operations concurrently, where 'n' is the
            // eucalyptus property [ZONE].storage.maxconcurrentsnapshots. If 'n' are already running,
            // block until one frees.

            // If this property is changed during runtime, then eucalyptus-cloud must be restarted 
            // for the change to take effect.
            
            // Note this is not the entire snapshot process, only taking the snapshot on the back end,
            // which should normally be fast, so OK to block in this synchronous data path.

            try {
              snapshotPointSemaphoreSupplier.get().acquire();
              LOG.trace("Acquired semaphore for BlockStorageController createSnapshotPoint. Remaining permits = " + 
                  snapshotPointSemaphoreSupplier.get().availablePermits());
            } catch (InterruptedException ex) {
              throw new EucalyptusCloudException("Failed to create snapshot point " + snapshotId + " on volume " + volumeId +
                  " as the semaphore could not be acquired");
            }
            try {
              // This will be a no-op if the backend doesn't support it. Will return null.
              snapPointId = blockManager.createSnapshotPoint(volumeId, snapshotId);
            } finally {
              LOG.trace("Releasing semaphore for BlockStorageController createSnapshotPoint");
              snapshotPointSemaphoreSupplier.get().release();
            }
            // Start time is the time of snapshot point creation
            snapshotInfo.setStartTime(new Date());
            if (snapPointId == null) {
              LOG.debug("Synchronous snap point not supported for this backend. Cleanly skipped.");
            } else {
              snapshotInfo.setSnapPointId(snapPointId);
            }
            // Do a commit here because the snapshotter expects to find db entry.
            snapshotInfo.setStatus(StorageProperties.Status.creating.toString());

            // Persist the snapshot metadata to db
            try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
              Entities.persist(snapshotInfo);
              tran.commit();
            } catch (Exception e) {
              LOG.warn("Unable to persist metadata for snapshot " + snapshotId, e);
              throw e;
            }

            Context ctx = null;
            try {
              ctx = Contexts.lookup(request.getCorrelationId());
              if (!ctx.getChannel().isOpen()) {
                throw new NoSuchContextException("Channel is closed");
              }
            } catch (NoSuchContextException e) {
              if (snapPointId != null) {
                // Other end hung up, mark this as failed since this is a sync operation
                throw new EucalyptusCloudException("Channel closed, aborting snapshot.");
              }
            }

          } catch (EucalyptusCloudException e) {
            // If the snapshot was done but took too long then delete the snap and fail the op.
            try {
              blockManager.deleteSnapshotPoint(volumeId, snapshotId, snapPointId);
            } catch (Exception ex) {
              LOG.error("Snapshot " + snapshotId + " exception on snap point cleanup after failure: " + e.getMessage());
            }
            LOG.error("Snapshot " + snapshotId + " failed to create snap point successfully: " + e.getMessage());
            throw e;
          }

          /* Resume old code path and finish the snapshot process if already started */
          // snapshot asynchronously
          snapshotter = new SnapshotCreator(volumeId, snapshotId, snapPointId, blockManager);

          reply.setSnapshotId(snapshotId);
          reply.setVolumeId(volumeId);
          reply.setStartTime(DateUtils.format(snapshotInfo.getStartTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
          reply.setProgress(snapshotInfo.getProgress());
        } catch (EucalyptusCloudException cloudEx) {
          snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
          markSnapshotFailed(snapshotId);
          LOG.error("Snapshot " + snapshotId + " creation failed with exception ", cloudEx);
          throw cloudEx;
        } catch (final Throwable e) {
          snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
          markSnapshotFailed(snapshotId);
          LOG.error("Snapshot " + snapshotId + " Error committing state update to failed", e);
          throw new EucalyptusCloudException("Snapshot " + snapshotId + " unexpected throwable exception caught", e);
        }
        reply.setStatus(snapshotInfo.getStatus());

        if (snapshotter != null) { // Kick off the snapshotter task after persisting snapshot to database
          try {
            SnapshotThreadPool.add(snapshotter);
          } catch (Exception e) {
            LOG.warn("Failed to add creation task for " + snapshotId + " to asynchronous thread pool", e);
            // Mark the snapshot as failed
            markSnapshotFailed(snapshotId);
            throw new EucalyptusCloudException("Failed to add creation task for " + snapshotId + " to asynchronous thread pool", e);
          }
        }
      }
    }

    return reply;

  }

  private void markSnapshotFailed(String snapshotId) {
    try {
      Entities.asTransaction(SnapshotInfo.class, SNAPSHOT_FAILED).apply(snapshotId);
    } catch (Throwable t) {
      LOG.warn("Unable to update status to failed for " + snapshotId, t);
    }
  }

  // returns snapshots in progress or at the SC
  @Override
  public DescribeStorageSnapshotsResponseType DescribeStorageSnapshots(DescribeStorageSnapshotsType request) throws EucalyptusCloudException {
    DescribeStorageSnapshotsResponseType reply = (DescribeStorageSnapshotsResponseType) request.getReply();
    // checker.transferPendingSnapshots();
    List<String> snapshotSet = request.getSnapshotSet();
    ArrayList<SnapshotInfo> snapshotInfos = new ArrayList<SnapshotInfo>();
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      if ((snapshotSet != null) && !snapshotSet.isEmpty()) {
        for (String snapshotSetEntry : snapshotSet) {
          SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotSetEntry);
          List<SnapshotInfo> foundSnapshotInfos = Entities.query(snapshotInfo);
          if (foundSnapshotInfos.size() > 0) {
            snapshotInfos.add(foundSnapshotInfos.get(0));
          }
        }
      } else {
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        List<SnapshotInfo> foundSnapshotInfos = Entities.query(snapshotInfo);
        for (SnapshotInfo snapInfo : foundSnapshotInfos) {
          snapshotInfos.add(snapInfo);
        }
      }

      ArrayList<StorageSnapshot> snapshots = reply.getSnapshotSet();
      for (SnapshotInfo snapshotInfo : snapshotInfos) {
        snapshots.add(convertSnapshotInfo(snapshotInfo));
        // Getting rid of failed snapshot cleanup - EUCA-10803
        // if (snapshotInfo.getStatus().equals(StorageProperties.Status.failed.toString()))
        // checker.cleanFailedSnapshot(snapshotInfo.getSnapshotId());
      }
      tran.commit();
    }
    return reply;
  }

  /**
   * Delete snapshot in idempotent way. Multiple requests for same snapshotId should return true. Only return false if the snapsnot *cannot* be
   * deleted but does exist
   * 
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  @Override
  public DeleteStorageSnapshotResponseType DeleteStorageSnapshot(DeleteStorageSnapshotType request) throws EucalyptusCloudException {
    final long startTime = System.currentTimeMillis();
    DeleteStorageSnapshotResponseType reply = (DeleteStorageSnapshotResponseType) request.getReply();

    StorageProperties.updateWalrusUrl();
    if (!StorageProperties.enableSnapshots) {
      LOG.error("Snapshots have been disabled. Please check connection to ObjectStorage.");
      return reply;
    }

    String snapshotId = request.getSnapshotId();
    LOG.info("Processing DeleteStorageSnapshot request for snapshot " + snapshotId);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo snapshotInfo = Entities.uniqueResult(new SnapshotInfo(snapshotId));
      String status = snapshotInfo.getStatus();
      if (status.equals(StorageProperties.Status.available.toString())) {
        snapshotInfo.setStatus(StorageProperties.Status.deleting.toString());
        LOG.debug("Snapshot " + snapshotId + " set to 'deleting' state");
        ThruputMetrics.startOperation(MonitoredAction.DELETE_SNAPSHOT, snapshotId, startTime);
      } else if (status.equals(StorageProperties.Status.deleting.toString()) || status.equals(StorageProperties.Status.deleted.toString())
          || status.equals(StorageProperties.Status.deletedfromebs.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
        LOG.debug("Snapshot " + snapshotId + " already in deleting/deleted/failed. No-op for delete request.");
      } else {
        // snapshot is still in progress.
        throw new SnapshotInUseException("Cannot delete snapshot in state: " + status + ". Please retry later");
      }
      reply.set_return(Boolean.TRUE);
      tran.commit();
    } catch (NoSuchElementException e) {
      // the SC knows nothing about this snapshot, either never existed or was deleted
      // For idempotent behavior, tell backend to delete and return true
      LOG.info("Got delete request, but unable to find snapshot in database: " + snapshotId +
          ". May have already been deleted in another zone.");
      reply.set_return(Boolean.TRUE);
    } catch (TransactionException e) {
      LOG.error("Exception looking up snapshot: " + snapshotId, e);
      throw new EucalyptusCloudException(e);
    }
    return reply;
  }

  /*
   * TODO: zhill, removed this since it isn't necessary, but can be added-back later when we have time for full dev and testing public
   * AbortStorageSnapshotResponseType AbortSnapshotPoint( AbortStorageSnapshotType request ) throws EucalyptusCloudException {
   * AbortStorageSnapshotResponseType reply = ( AbortStorageSnapshotResponseType ) request.getReply(); String snapshotId = request.getSnapshotId();
   * reply.set_return(true);
   * 
   * try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) { SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(new
   * SnapshotInfo(snapshotId)); String status = foundSnapshotInfo.getStatus(); if(status.equals(StorageProperties.Status.available.toString()) ||
   * status.equals(StorageProperties.Status.failed.toString())) { foundSnapshotInfo.setStatus(StorageProperties.Status.deleting.toString());
   * tr.commit(); } else { //snapshot is still in progress. foundSnapshotInfo.setStatus(StorageProperties.Status.failed.toString()); tr.commit();
   * checker.cleanFailedSnapshot(snapshotId); } } catch (NoSuchElementException e) { //the SC knows nothing about this snapshot. LOG.debug("Snapshot "
   * + snapshotId + " not found"); } catch (Exception e) { LOG.error("Failed to abort snapshot " + snapshotId, e); throw new
   * EucalyptusCloudException("Failed to abort snapshot " + snapshotId, e) }
   * 
   * return reply; }
   */
  @Override
  public CreateStorageVolumeResponseType CreateStorageVolume(CreateStorageVolumeType request) throws EucalyptusCloudException {
    final long actionStart = System.currentTimeMillis();
    CreateStorageVolumeResponseType reply = (CreateStorageVolumeResponseType) request.getReply();

    if (!StorageProperties.enableStorage) {
      LOG.error("BlockStorage has been disabled. Please check your setup");
      return reply;
    }
    String snapshotId = request.getSnapshotId();
    String parentVolumeId = request.getParentVolumeId();
    String userId = request.getUserId();
    String volumeId = request.getVolumeId();
    LOG.info("Processing CreateStorageVolume request for volume " + volumeId);

    // in GB
    String size = request.getSize();
    int sizeAsInt = (size != null) ? Integer.parseInt(size) : 0;
    if (size != null && sizeAsInt <= 0) {
      throw new InvalidParameterValueException("The parameter size (" + sizeAsInt + ") must be greater than zero.");
    }
    if (StorageProperties.shouldEnforceUsageLimits) {
      if (size != null) {
        int totalVolumeSize = 0;
        VolumeInfo volInfo = new VolumeInfo();
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
          List<VolumeInfo> volInfos = Entities.query(volInfo);
          for (VolumeInfo vInfo : volInfos) {
            if (!vInfo.getStatus().equals(StorageProperties.Status.failed.toString())
                && !vInfo.getStatus().equals(StorageProperties.Status.deleted.toString())) {
              totalVolumeSize += vInfo.getSize();
            }
          }
          tran.commit();
        }
        if (((totalVolumeSize + sizeAsInt) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb())) {
          throw new VolumeSizeExceededException(volumeId, "Total Volume Size Limit Exceeded");
        }
        if (sizeAsInt > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB()) {
          throw new VolumeSizeExceededException(volumeId, "Max Volume Size Limit Exceeded");
        }
      }
    }

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo volumeInfo = new VolumeInfo(volumeId);
      List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
      if (volumeInfos.size() > 0) {
        throw new VolumeAlreadyExistsException(volumeId);
      }
      if (snapshotId != null) {
        SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
        snapInfo.setScName(null);
        snapInfo.setStatus(StorageProperties.Status.available.toString());
        List<SnapshotInfo> snapInfos = Entities.query(snapInfo);
        if (snapInfos.size() == 0) {
          throw new SnapshotNotFoundException("Snapshot " + snapshotId + " does not exist or is unavailable");
        }
        volumeInfo.setSnapshotId(snapshotId);
        reply.setSnapshotId(snapshotId);
      }
      volumeInfo.setUserName(userId);
      volumeInfo.setSize(sizeAsInt);
      volumeInfo.setStatus(StorageProperties.Status.creating.toString());
      LOG.debug("Volume " + volumeId + " set to 'creating' state");
      Date creationDate = new Date();
      volumeInfo.setCreateTime(creationDate);
      Entities.persist(volumeInfo);
      reply.setVolumeId(volumeId);
      reply.setCreateTime(DateUtils.format(creationDate.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
      reply.setSize(size);
      reply.setStatus(volumeInfo.getStatus());
      tran.commit();
    }

    try {
      // create volume asynchronously
      VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, parentVolumeId, sizeAsInt, blockManager);
      ThruputMetrics.startOperation(snapshotId != null ? MonitoredAction.CREATE_VOLUME_FROM_SNAPSHOT : MonitoredAction.CREATE_VOLUME, volumeId,
          actionStart);
      VolumeThreadPool.add(volumeCreator);
    } catch (Exception e) {
      LOG.warn("Failed to add creation task for " + volumeId + " to asynchronous thread pool", e);
      // Mark the volume as failed
      try {
        Function<String, VolumeInfo> updateFunction = new Function<String, VolumeInfo>() {

          @Override
          public VolumeInfo apply(String arg0) {
            VolumeInfo vol;
            try {
              vol = Entities.uniqueResult(new VolumeInfo(arg0));
              vol.setStatus(StorageProperties.Status.failed.toString());
              LOG.debug("Volume " + arg0 + " set to 'failed' state");
              return vol;
            } catch (TransactionException | NoSuchElementException e) {
              LOG.warn("Failed to retrieve DB entity for " + arg0, e);
            }
            return null;
          }
        };

        Entities.asTransaction(VolumeInfo.class, updateFunction).apply(volumeId);
      } catch (Exception e1) {
        LOG.warn("Unable to update status for " + volumeId + " after failure to add creation task to asynchronous thread pool", e);
      }

      throw new EucalyptusCloudException("Failed to add creation task for " + volumeId + " to asynchronous thread pool", e);
    }

    return reply;
  }

  @Override
  public DescribeStorageVolumesResponseType DescribeStorageVolumes(DescribeStorageVolumesType request) throws EucalyptusCloudException {
    DescribeStorageVolumesResponseType reply = (DescribeStorageVolumesResponseType) request.getReply();

    List<String> volumeSet = request.getVolumeSet();
    ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      if ((volumeSet != null) && !volumeSet.isEmpty()) {
        for (String volumeSetEntry : volumeSet) {
          VolumeInfo volumeInfo = new VolumeInfo(volumeSetEntry);
          List<VolumeInfo> foundVolumeInfos = Entities.query(volumeInfo);
          if (foundVolumeInfos.size() > 0) {
            volumeInfos.add(foundVolumeInfos.get(0));
          }
        }
      } else {
        VolumeInfo volumeInfo = new VolumeInfo();
        List<VolumeInfo> foundVolumeInfos = Entities.query(volumeInfo);
        for (VolumeInfo volInfo : foundVolumeInfos) {
          volumeInfos.add(volInfo);
        }
      }

      ArrayList<StorageVolume> volumes = reply.getVolumeSet();
      for (VolumeInfo volumeInfo : volumeInfos) {
        volumes.add(convertVolumeInfo(volumeInfo));
        // Getting rid of failed volume cleanup - EUCA-10803
        // if (volumeInfo.getStatus().equals(StorageProperties.Status.failed.toString())
        // && (System.currentTimeMillis() - volumeInfo.getLastUpdateTimestamp().getTime() > StorageProperties.FAILED_STATE_CLEANUP_THRESHOLD_MS)) {
        // LOG.warn("Failed volume, cleaning it: " + volumeInfo.getVolumeId());
        // checker.cleanFailedVolume(volumeInfo.getVolumeId());
        // }
      }
      tran.commit();
    }
    return reply;

  }

  /**
   * This should no longer be called/invoked directly...
   * 
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  @Override
  public AttachStorageVolumeResponseType attachVolume(AttachStorageVolumeType request) throws EucalyptusCloudException {
    throw new EucalyptusCloudException("Operation not supported");
  }

  @Override
  public DetachStorageVolumeResponseType detachVolume(DetachStorageVolumeType request) throws EucalyptusCloudException {
    DetachStorageVolumeResponseType reply = request.getReply();
    String volumeId = request.getVolumeId();
    LOG.info("Processing DetachVolume request for volume " + volumeId);

    // Do the work.
    try {
      LOG.info("Detaching volume " + volumeId + " from all hosts");
      Entities.asTransaction(VolumeInfo.class, invalidateAndDetachAll()).apply(volumeId);
    } catch (final Exception e) {
      LOG.error("Failed to fully detach volume " + volumeId);
      reply.set_return(false);
    }
    return reply;
  }

  private static Function<String, VolumeInfo> invalidateAndDetachAll() {

    final Predicate<VolumeToken> invalidateExports = new Predicate<VolumeToken>() {
      @Override
      public boolean apply(VolumeToken volToken) {
        VolumeToken tokenEntity = Entities.merge(volToken);
        try {
          tokenEntity.invalidateAllExportsAndToken();
          return true;
        } catch (Exception e) {
          LOG.error("Failed invalidating exports for token " + tokenEntity.getToken());
          return false;
        }
      }
    };

    // Could save cycles by statically setting all of these functions that don't require closures so they are not
    // constructed for each request.
    return new Function<String, VolumeInfo>() {
      @Override
      public VolumeInfo apply(String volumeId) {
        try {
          VolumeInfo volumeEntity = Entities.uniqueResult(new VolumeInfo(volumeId));
          try {
            LOG.debug("Invalidating all tokens and all exports for " + volumeId);
            // Invalidate all tokens and exports and forcibly detach.
            if (!Iterables.all(volumeEntity.getAttachmentTokens(), invalidateExports)) {
              // At least one failed.
              LOG.error("Failed to invalidate all tokens and exports");
            }
          } catch (Exception e) {
            LOG.error("Error invalidating tokens", e);
          }

          try {
            LOG.debug("Unexporting volume " + volumeId + " from all hosts");
            blockManager.unexportVolumeFromAll(volumeId);
          } catch (EucalyptusCloudException ex) {
            LOG.error("Detaching volume " + volumeId + " from all hosts failed", ex);
          }
        } catch (NoSuchElementException e) {
          LOG.error("Cannot force detach of volume " + volumeId + " because it is not found in database");
          return null;
        } catch (TransactionException e) {
          LOG.error("Failed to lookup volume " + volumeId);
        }

        return null;
      }
    };
  }

  private StorageVolume convertVolumeInfo(VolumeInfo volInfo) throws EucalyptusCloudException {
    StorageVolume volume = new StorageVolume();
    String volumeId = volInfo.getVolumeId();
    volume.setVolumeId(volumeId);
    volume.setStatus(volInfo.getStatus());
    volume.setCreateTime(DateUtils.format(volInfo.getCreateTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
    volume.setSize(String.valueOf(volInfo.getSize()));
    volume.setSnapshotId(volInfo.getSnapshotId());
    VolumeToken tok = volInfo.getCurrentValidToken();
    if (tok != null) {
      volume.setActualDeviceName(BlockStorageUtil.encryptForNode(tok.getToken(), BlockStorageUtil.getPartitionForLocalService(Storage.class)));
    } else {
      // use 'invalid' to indicate no export? invalid seems okay since there is no valid device unless a token is valid
      volume.setActualDeviceName("invalid");
    }
    return volume;
  }

  private StorageSnapshot convertSnapshotInfo(SnapshotInfo snapInfo) {
    StorageSnapshot snapshot = new StorageSnapshot();
    snapshot.setVolumeId(snapInfo.getVolumeId());
    snapshot.setStatus(StorageProperties.Status.deletedfromebs.toString().equals(snapInfo.getStatus()) ? StorageProperties.Status.deleted.toString()
        : snapInfo.getStatus());
    snapshot.setSnapshotId(snapInfo.getSnapshotId());
    String progress = snapInfo.getProgress();
    progress = progress != null ? progress + "%" : progress;
    snapshot.setProgress(progress);
    snapshot.setStartTime(DateUtils.format(snapInfo.getStartTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
    return snapshot;
  }

  @Override
  public CloneVolumeResponseType CloneVolume(CloneVolumeType request) throws EucalyptusCloudException {
    CloneVolumeResponseType reply = request.getReply();
    CreateStorageVolumeType createStorageVolume = new CreateStorageVolumeType();
    createStorageVolume.setParentVolumeId(request.getVolumeId());
    CreateStorageVolume(createStorageVolume);
    return reply;
  }
}
